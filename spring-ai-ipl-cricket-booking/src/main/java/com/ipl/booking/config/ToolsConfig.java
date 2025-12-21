package com.ipl.booking.config;

import com.ipl.booking.service.CricketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class ToolsConfig {

    @Bean
    @Description("Get a list of all available IPL cricket matches including match ID, teams, date, and venue")
    public Function<Void, List<CricketService.Match>> getMatches(CricketService cricketService) {
        return request -> cricketService.getMatches();
    }

    @Bean
    @Description("Book tickets for a cricket match. Requires match ID, customer name and ticket count")
    public Function<CricketService.BookingRequest, CricketService.BookingResponse> bookTicket(
            CricketService cricketService) {
        return cricketService::bookTicket;
    }
}
