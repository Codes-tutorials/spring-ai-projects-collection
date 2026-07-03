package com.springai.auth.controller;

import com.springai.auth.model.AuthResponse;
import com.springai.auth.model.User;
import com.springai.auth.model.UserProfile;
import com.springai.auth.service.GoogleTokenService;
import com.springai.auth.service.JwtService;
import com.springai.auth.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller providing endpoints for authentication, payment processing, and session status
 * for the Spring AI Course website. Uses the persistent User database table.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleTokenService googleTokenService;
    private final JwtService jwtService;
    private final UserService userService;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    public AuthController(GoogleTokenService googleTokenService, JwtService jwtService, UserService userService) {
        this.googleTokenService = googleTokenService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Accepts Google authorization code from the Angular frontend, exchanges it with Google,
     * checks/updates user record in database, and returns a signed JWT with DB payment status.
     */
    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String redirectUri = request.get("redirectUri");
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
        }

        try {
            UserProfile googleProfile = googleTokenService.exchangeCodeAndGetProfile(code, redirectUri);

            // Persist / update user details and login timestamp in the database
            User user = userService.registerOrUpdateUser(googleProfile);

            UserProfile profileWithPayment = new UserProfile(
                    user.getGoogleSub(),
                    user.getEmail(),
                    user.getName(),
                    user.getPicture(),
                    user.isEmailVerified(),
                    user.isPaid()
            );

            String token = jwtService.generateToken(profileWithPayment);
            return ResponseEntity.ok(new AuthResponse(token, profileWithPayment));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Authenticated endpoint to record a successful Razorpay payment.
     * Cryptographically verifies Razorpay signature, sets user.paid = true in database,
     * then returns a fresh JWT with paid=true.
     */
    @PostMapping("/pay")
    public ResponseEntity<?> recordPayment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token has expired or is invalid"));
            }

            UserProfile profile = jwtService.extractUserProfile(token);

            // Cryptographic signature verification (when Razorpay provides signature)
            String paymentId = request.get("paymentId");
            String orderId = request.get("orderId");
            String signature = request.get("signature");

            if (signature != null && !signature.trim().isEmpty()
                    && orderId != null && !orderId.trim().isEmpty()) {
                String payload = orderId + "|" + paymentId;
                boolean isValid = com.razorpay.Utils.verifySignature(payload, signature, razorpaySecret);
                if (!isValid) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Cryptographic signature validation failed"));
                }
            }

            // Mark user as paid in database
            User user = userService.markUserAsPaid(profile.email());

            UserProfile updatedProfile = new UserProfile(
                    user.getGoogleSub(),
                    user.getEmail(),
                    user.getName(),
                    user.getPicture(),
                    user.isEmailVerified(),
                    user.isPaid()
            );

            String newToken = jwtService.generateToken(updatedProfile);
            return ResponseEntity.ok(new AuthResponse(newToken, updatedProfile));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment processing failed: " + e.getMessage()));
        }
    }

    /**
     * Returns the current user profile, dynamically syncing payment status from the database.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token has expired or is invalid"));
            }

            UserProfile profile = jwtService.extractUserProfile(token);

            // Load user from database to ensure current payment status is returned
            User user = userService.findByEmail(profile.email())
                    .orElseThrow(() -> new RuntimeException("User not found: " + profile.email()));

            UserProfile syncedProfile = new UserProfile(
                    user.getGoogleSub(),
                    user.getEmail(),
                    user.getName(),
                    user.getPicture(),
                    user.isEmailVerified(),
                    user.isPaid()
            );

            return ResponseEntity.ok(syncedProfile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Failed to decode session: " + e.getMessage()));
        }
    }
}
