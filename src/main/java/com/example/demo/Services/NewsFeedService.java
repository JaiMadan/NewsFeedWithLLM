package com.example.demo.Services;

import com.example.demo.DTO.NewsResponse;
import com.example.demo.Entities.NewsArticle;
import com.example.demo.Repositories.NewsArticleRepository;
import com.example.demo.Utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsFeedService {

    @Autowired
    NewsArticleRepository newsArticleRepository;

    @Autowired
    PromptService promptService;

    public List<NewsArticle> findBySource(String source) throws Exception{
        Pageable top5 = PageRequest.of(0, 5);
        return newsArticleRepository.findBySource(source,top5);
    }

    public List<NewsArticle> findByCategory(String category) {
        Pageable top5 = PageRequest.of(0, 5);
        return newsArticleRepository.findByCategory(category,top5);
    }

    public List<NewsArticle> findByScore(Double score) {
        Pageable top5 = PageRequest.of(0, 5);
        return newsArticleRepository.findByScore(score,top5);
    }

    public List<NewsArticle> findBySearch(String query) {
        List<String> tokenKeywords = tokenizeQuery(query);
        List<NewsArticle> newsArticles = newsArticleRepository.findAll();
        for(NewsArticle newsArticle: newsArticles){
            double totalScore = computeNormalizedMatchScore(newsArticle,tokenKeywords)+(newsArticle.getRelevanceScore()!=null?newsArticle.getRelevanceScore():0.0);
            newsArticle.setFinalScore(totalScore);
        }

        return newsArticles.stream().sorted(Comparator.comparingDouble(NewsArticle::getFinalScore).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    public double computeNormalizedMatchScore(NewsArticle article, List<String> keywords) {
        if (keywords.isEmpty()) return 0.0;

        String title = article.getTitle().toLowerCase();
        String description = article.getDescription().toLowerCase();

        int matched = 0;
        for (String keyword : keywords) {
            if (title.contains(keyword) || description.contains(keyword)) {
                matched++;
            }
        }

        return (double) matched / keywords.size();
    }

    public List<String> tokenizeQuery(String query) {
        if (query == null || query.isEmpty()) return Collections.emptyList();


        return Arrays.stream(query
                .toLowerCase()
                .replaceAll("[^a-z0-9]", " ")
                .split("\\s+"))
            .filter(word -> word.length() > 1)
            .collect(Collectors.toList());
    }


    public List<NewsArticle> findNearestToUser(Double lat, Double lon) {
        List<NewsArticle> newsArticles = newsArticleRepository.findAll();

        // Filter out articles without valid lat/lon
        List<NewsArticle> validArticles = newsArticles.stream()
            .filter(article -> article.getLatitude() != null && article.getLongitude() != null)
            .collect(Collectors.toList());

        for (NewsArticle article : validArticles) {
            double distance = GeoUtils.haversineDistance(
                lat, lon,
                article.getLatitude(), article.getLongitude()
            );
            article.setDistanceToUser(distance);
        }


        return validArticles.stream()
            .sorted(Comparator.comparingDouble(NewsArticle::getDistanceToUser))
            .limit(5)
            .collect(Collectors.toList());

    }

    public List<NewsArticle> findAll() {
        return newsArticleRepository.findAll();
    }

    public List<NewsResponse> convertDataToExport(List<NewsArticle> articles) throws JsonProcessingException {
        List<NewsResponse> newsResponses = new ArrayList<>();
        String prompt = promptService.buildBatchSummaryPrompt(articles);
        String llmResponse = promptService.callLLMForEnrichment(prompt);

        ObjectMapper mapper = new ObjectMapper();
        List<String> summaries = mapper.readValue(llmResponse, new TypeReference<List<String>>(){});
        for (int i = 0; i < articles.size(); i++) {
            articles.get(i).setLlmSummary(summaries.get(i));
        }

        for (NewsArticle newsArticle : articles) {
            newsResponses.add(NewsResponse
                .builder()
                .url(newsArticle.getUrl())
                .title(newsArticle.getTitle())
                .source_name(newsArticle.getSourceName())
                .latitude(newsArticle.getLatitude())
                .longitude(newsArticle.getLongitude())
                .publication_date(newsArticle.getPublicationDate())
                .relevance_score(newsArticle.getRelevanceScore())
                .description(newsArticle.getDescription())
                .title(newsArticle.getTitle())
                .category(newsArticle.getCategory())
                .llm_summary(newsArticle.getLlmSummary())
                .build()
            );
        }

        return newsResponses;
    }
}
