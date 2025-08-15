package com.example.demo.Controller;


import com.example.demo.DTO.NewsFeedResponse;
import com.example.demo.DTO.NewsResponse;
import com.example.demo.DTO.QueryInput;
import com.example.demo.DTO.UserQueryContext;
import com.example.demo.Entities.NewsArticle;
import com.example.demo.Services.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
public class NewsFeedController {


    @Autowired
    NewsFeedService newsFeedService;

    @Autowired
    NormalizingService normalizingService;

    @Autowired
    PromptService promptService;

    @Autowired
    TrendingFeedService trendingFeedService;




    @GetMapping("/query")
    public ResponseEntity<?> getByLlmQuery(@RequestBody QueryInput queryInput) throws JsonProcessingException {


            List<NewsArticle> newsArticles = newsFeedService.findAll();
            // get all types of intent from query by user
            // using LLM model to extract
            // all entities, intent, threshold relevance score, location, search parameters
            UserQueryContext userQueryContext = promptService.callLLM(queryInput.getQuery());

            // filtering articles based on types of intent and their parameters
            List<NewsArticle> filteredArticles = promptService.filterArticles(newsArticles,userQueryContext);

            // ranking the filtered articles by normalizing
            // the final score of all types of intent
            // and using individual weightage of each intent with parameters of it
            List<NewsResponse> newsResponses = new ArrayList<>();
            for (NewsArticle newsArticle : filteredArticles) {
                double finalScore = normalizingService.calculateCompositeScore(newsArticle,userQueryContext.getIntent(),queryInput.getQuery(),userQueryContext.getLocation());
                newsArticle.setFinalScore(finalScore);
            }
            newsResponses = newsFeedService.convertDataToExport(filteredArticles.stream()
                .sorted(Comparator.comparingDouble(NewsArticle::getFinalScore)
                .reversed())
                .limit(5)
                .toList()
            );
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(NewsFeedResponse
                    .builder()
                    .articles(newsResponses)
                    .build()
                );
    }


     @GetMapping("/source")
     public ResponseEntity<?> getBySource(@RequestParam String source) throws Exception {

             List<NewsArticle> newsArticles = newsFeedService.findBySource(source);
             List<NewsResponse> newsResponses = newsFeedService.convertDataToExport(newsArticles);

             return ResponseEntity
                 .status(HttpStatus.OK)
                 .body(NewsFeedResponse
                     .builder()
                     .articles(newsResponses)
                     .build()
                 );
     }


    @GetMapping("/category")
    public ResponseEntity<?> getByCategory(@RequestParam String category) throws JsonProcessingException {

            List<NewsArticle> newsArticles = newsFeedService.findByCategory(category);
            List<NewsResponse> newsResponses = newsFeedService.convertDataToExport(newsArticles);
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(NewsFeedResponse
                    .builder()
                    .articles(newsResponses)
                    .build()
                );
    }


    @GetMapping("/score")
    public ResponseEntity<?> getByScore(@RequestParam Double score) throws JsonProcessingException {

            List<NewsArticle> newsArticles = newsFeedService.findByScore(score);
            List<NewsResponse> newsResponses = newsFeedService.convertDataToExport(newsArticles);
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(NewsFeedResponse
                    .builder()
                    .articles(newsResponses)
                    .build()
                );
    }

    @GetMapping("/search")
    public ResponseEntity<?> getBySearch(@RequestParam String search) throws JsonProcessingException {

            List<NewsArticle> newsArticles = newsFeedService.findBySearch(search);
            List<NewsResponse> newsResponses = newsFeedService.convertDataToExport(newsArticles);
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(NewsFeedResponse
                    .builder()
                    .articles(newsResponses)
                    .build()
                );
    }


    @GetMapping("/nearby")
    public ResponseEntity<?> getNearestArticles(@RequestParam Double lat, @RequestParam Double lon)
        throws JsonProcessingException {

            List<NewsArticle> newsArticles = newsFeedService.findNearestToUser(lat,lon);
            List<NewsResponse> newsResponses = newsFeedService.convertDataToExport(newsArticles);
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(NewsFeedResponse
                    .builder()
                    .articles(newsResponses)
                    .build()
                );
    }

    @GetMapping("/trending")
    public ResponseEntity<?> getTrending(
        @RequestParam double lat,
        @RequestParam double lon,
        @RequestParam(defaultValue = "5") int limit
    ) {
            return ResponseEntity.ok(trendingFeedService.getTrendingArticles(lat, lon, limit));
    }


}
