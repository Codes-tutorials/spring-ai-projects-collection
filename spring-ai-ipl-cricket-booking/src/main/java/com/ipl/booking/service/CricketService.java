package com.ipl.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CricketService {

    private static final Logger logger = LoggerFactory.getLogger(CricketService.class);

    // Mock Data
    private static final List<Match> MATCHES = List.of(
            new Match("M001", "CSK vs RCB", "2024-03-22", "Chennai", 2500.0),
            new Match("M002", "MI vs GT", "2024-03-24", "Mumbai", 3000.0),
            new Match("M003", "SRH vs KKR", "2024-03-23", "Hyderabad", 1500.0));

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    // Records
    public record Match(String id, String teams, String date, String venue, double price) {
    }

    public record BookingRequest(String matchId, String customerName, int count) {
    }

    public record BookingResponse(String bookingId, String status, String message, double totalAmount) {
    }

    // Service Methods
    public List<Match> getMatches() {
        logger.info("Fetching all matches");
        return MATCHES;
    }

    public BookingResponse bookTicket(BookingRequest request) {
        logger.info("Booking ticket for request: {}", request);

        var match = MATCHES.stream()
                .filter(m -> m.id().equalsIgnoreCase(request.matchId()))
                .findFirst();

        if (match.isEmpty()) {
            return new BookingResponse(null, "FAILED", "Invalid Match ID", 0.0);
        }

        String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        double totalAmount = match.get().price() * request.count();

        Booking booking = new Booking(bookingId, request, totalAmount);
        bookings.put(bookingId, booking);

        return new BookingResponse(bookingId, "CONFIRMED",
                "Booked " + request.count() + " tickets for " + match.get().teams(), totalAmount);
    }

    private record Booking(String id, BookingRequest request, double amount) {
    }
}
