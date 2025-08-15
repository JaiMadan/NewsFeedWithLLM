package com.example.demo.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${newsFeed.llm.geoService}")
    private String apiKey;

    public List<Double> getCoordinates(String locationName) {
        String url = UriComponentsBuilder.fromHttpUrl("https://api.opencagedata.com/geocode/v1/json")
            .queryParam("q", locationName)
            .queryParam("key", apiKey)
            .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
            if (!results.isEmpty()) {
                Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
                double lat = (double) geometry.get("lat");
                double lng = (double) geometry.get("lng");

                List<Double> coordinates = new ArrayList<>();
                coordinates.add(lat);
                coordinates.add(lng);
                return coordinates;
            }
        }

        throw new RuntimeException("Location not found or API call failed.");
    }
}

