package com.example.demo.Services;

import com.example.demo.Entities.NewsArticle;
import com.example.demo.Entities.UserEvent;
import com.example.demo.Repositories.NewsArticleRepository;
import com.example.demo.Repositories.UserEventRepository;
import com.example.demo.Utils.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrendingFeedService {

    @Autowired
    NewsArticleRepository newsArticleRepository;

    @Autowired
    UserEventRepository userEventRepository;

    @Autowired
    NormalizingService normalizingService;

    @Cacheable(value = "trendingCache", key = "#lat + '_' + #lon")
    public List<NewsArticle> getTrendingArticles(double lat, double lon, int limit) {
        List<UserEvent> recentEvents = userEventRepository.findRecentEvents(LocalDateTime.now().minusDays(3));

        // Group by articleId
        Map<String, List<UserEvent>> grouped = recentEvents.stream()
            .collect(Collectors.groupingBy(UserEvent::getArticleId));

        List<ArticleScore> scores = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            String articleId = entry.getKey();
            List<UserEvent> events = entry.getValue();

            long views = events.stream().filter(e -> e.getEventType().equals("VIEW")).count();
            long clicks = events.stream().filter(e -> e.getEventType().equals("CLICK")).count();
            double recency = 1.0 - Math.min(1.0, Duration.between(
                events.stream().map(UserEvent::getEventTime).max(LocalDateTime::compareTo).orElse(LocalDateTime.now()),
                LocalDateTime.now()
            ).toHours() / 48.0);

            double avgLat = events.stream().mapToDouble(UserEvent::getLatitude);
            double avgLon = events.stream().mapToDouble(UserEvent::getLongitude).average().orElse(lon);
            double proximity = 1.0 - Math.min(1.0, GeoUtils.haversineDistance(lat, lon, avgLat, avgLon) / 15000.0);

            {
                for(int i=0;i<events.size();i++){
                    normalizingService.gaveLocationScore(newsArticleRepository.findById(events.get(i).getArticleId()),lat,lon)
                }
                normalizingService.getDistanceScore(newsArticleRepository.findById())
            }

            double score = (clicks * 2 + views) * recency * proximity;
            scores.add(new ArticleScore(articleId, score));
        }

        return scores.stream()
            .sorted(Comparator.comparingDouble(ArticleScore::score).reversed())
            .limit(limit)
            .map(s -> newsArticleRepository.findById(s.articleId()).orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    record ArticleScore(String articleId, double score) {}

}
