package com.example.demo.Services;

import com.example.demo.Entities.NewsArticle;
import com.example.demo.Utils.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class NormalizingService {

    @Autowired
    NewsFeedService newsFeedService;

    @Autowired
    GeocodingService geocodingService;

    public double calculateCompositeScore(NewsArticle article, List<String> ctx, String query, List<String> Locations) {
        double score = 0.0;
        ctx = ctx.stream().map(String::toLowerCase).collect(Collectors.toList());
        if (ctx.contains("score")) {
            score += 0.5 * normalizeRelevanceScore(article.getRelevanceScore());
        }
        if (ctx.contains("category") || ctx.contains("source")) {
            score += 0.3 * getRecencyScore(article.getPublicationDate());
        }
        if (ctx.contains("search")) {

            score += 0.2 * newsFeedService.computeNormalizedMatchScore(article,newsFeedService.tokenizeQuery(query));
        }
        if (ctx.contains("nearby")) {
            for (String loc : Locations) {
                List<Double> coordinates = geocodingService.getCoordinates(loc);
                if (coordinates.size() == 2 && coordinates.get(0) != null && coordinates.get(1) != null) {
                    score += 0.2 * getDistanceScore(article, coordinates.get(0), coordinates.get(1));
                }
            }
        }
        return score;
    }

    private double normalizeRelevanceScore(Double score) {
        if (score == null) return 0.0;
        return Math.min(1.0, Math.max(0.0, score));
    }

    private double getRecencyScore(LocalDateTime publicationDate) {
        if (publicationDate == null) return 0.0;

        LocalDateTime now = LocalDateTime.now();
        long ageInDays = ChronoUnit.DAYS.between(publicationDate, now);

        // Decay factor tuned for ~100 years = ~36,500 days
        double lambda = Math.log(2) / 36500.0; // Half-life of 100 years

        // Exponential decay formula
        double score = Math.exp(-lambda * ageInDays);

        return Math.max(0.0, Math.min(1.0, score));
    }

    private double getDistanceScore(NewsArticle article, Double lat, Double lon) {
        if (article.getLatitude() == null || article.getLongitude() == null) return 0.0;

        double dist = GeoUtils.haversineDistance(article.getLatitude(), article.getLongitude(),
            lat, lon);

        double EARTH_RADIUS_KM = 6371.0;
        return 1.0 - Math.min(1.0, dist / EARTH_RADIUS_KM); // closer = closer to 1
    }

    public double gaveLocationScore(NewsArticle article, Double lat, Double lon) {


        double dist = GeoUtils.haversineDistance(article.getLatitude(), article.getLongitude(),
            lat, lon);



        return 1.0-(dist/(2.0*6531.0));
    }



}
