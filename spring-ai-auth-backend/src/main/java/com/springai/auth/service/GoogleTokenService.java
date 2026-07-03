package com.springai.auth.service;

import com.springai.auth.model.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service to exchange the OAuth2 authorization code for Google tokens
 * and retrieve/validate the user's profile.
 */
@Service
public class GoogleTokenService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    /**
     * Exchanges the authorization code for a Google id_token, validates it, and returns the UserProfile.
     *
     * @param code The authorization code received from the Angular frontend.
     * @return UserProfile extracted from the Google id_token.
     */
    public UserProfile exchangeCodeAndGetProfile(String code, String clientRedirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", code);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("redirect_uri", clientRedirectUri != null && !clientRedirectUri.trim().isEmpty() ? clientRedirectUri : redirectUri);
        requestBody.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String idToken = (String) body.get("id_token");
                if (idToken == null) {
                    throw new RuntimeException("No id_token found in Google token response");
                }
                return verifyIdTokenAndGetProfile(idToken);
            } else {
                throw new RuntimeException("Failed to exchange code: Google returned status " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during Google OAuth code exchange: " + e.getMessage(), e);
        }
    }

    /**
     * Calls Google's Token Info endpoint to safely parse and validate the ID token.
     */
    private UserProfile verifyIdTokenAndGetProfile(String idToken) {
        String verificationUrl = GOOGLE_TOKEN_INFO_URL + "?id_token=" + idToken;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(verificationUrl, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> claims = response.getBody();

                String aud = (String) claims.get("aud");
                if (aud == null || !aud.equals(clientId)) {
                    throw new SecurityException("Token audience does not match Client ID!");
                }

                return new UserProfile(
                    (String) claims.get("sub"),
                    (String) claims.get("email"),
                    (String) claims.get("name"),
                    (String) claims.get("picture"),
                    Boolean.parseBoolean(String.valueOf(claims.get("email_verified"))),
                    false
                );
            } else {
                throw new RuntimeException("Failed to verify ID token: status " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error verifying Google ID token: " + e.getMessage(), e);
        }
    }
}
