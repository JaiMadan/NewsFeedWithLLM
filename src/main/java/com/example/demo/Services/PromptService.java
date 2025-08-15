package com.example.demo.Services;

import com.example.demo.DTO.UserQueryContext;
import com.example.demo.Entities.NewsArticle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class PromptService {


    private final RestTemplate restTemplate;
    @Value("${newsFeed.llm.key}")
    private String apiKey;
    private final String COHERE_URL = "https://api.cohere.ai/v1/chat";

    @Autowired
    private GeocodingService geocodingService;

    public PromptService(RestTemplateBuilder restTemplate) {
        this.restTemplate = restTemplate.build();
    }

    private String buildPrompt(String query) {
        return """
           You are an intelligent assistant helping to interpret news search queries.

            Given a natural language query from a user, extract the following:

            1. Intent – One or more of: category, score, source, search, nearby as List.
            2. Entities – Names of people, places, organizations, or events mentioned as List.
            3. Location – Specific locations mentioned (like "Palo Alto") in List.
            4. Keywords – Key words used for matching titles/descriptions as List.
            5. relevanceScoreThreshold – A value between 0.0 and 1.0 indicating how relevant the articles should be (include this only query uses terms like "top", "important", "very relevant").

            Format your response strictly as a JSON object with the fields: intent, entities, location, keywords, relevanceScoreThreshold.
    ---
    Query: "%s"
    """.formatted(query);
    }

    public UserQueryContext callLLM(String userQuery) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
            "message", buildPrompt(userQuery),
            "model", "command-r-plus", // or "command-light" for smaller model
            "temperature", 0.3,
            "max_tokens", 500,
            "chat_history", List.of() // optional, can be used for multi-turn context
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(COHERE_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                String jsonText = (String) responseBody.get("text");

                int start = jsonText.indexOf('{');
                int end = jsonText.lastIndexOf('}');

                if (start >= 0 && end >= 0 && end > start) {
                    String cleanJson = jsonText.substring(start, end + 1);
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(cleanJson, UserQueryContext.class);
                    //return objectMapper.readValue(cleanJson, UserQueryContext.class);
                } else {
                    throw new RuntimeException("No valid JSON found in LLM response: " + jsonText);
                }

            } else {
                throw new RuntimeException("Cohere call failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error calling Cohere LLM: " + e.getMessage(), e);
        }
    }


    public List<NewsArticle> filterArticles(List<NewsArticle> allArticles, UserQueryContext ctx){
        Stream<NewsArticle> filtered = allArticles.stream();

        // Filter by category
        if (ctx.hasIntent("category") && (ctx.getKeywords() != null || ctx.getEntities() != null)) {
            filtered = filtered.filter(a -> {
                if (a.getCategory() == null) return false;

                boolean keywordMatch = false;
                boolean entityMatch = false;

                if (ctx.getKeywords() != null) {
                    keywordMatch = a.getCategory().stream().anyMatch(cat ->
                        ctx.getKeywords().stream().anyMatch(userCat ->
                            userCat.equalsIgnoreCase(cat)));
                }

                if (ctx.getEntities() != null) {
                    entityMatch = a.getCategory().stream().anyMatch(cat ->
                        ctx.getEntities().stream().anyMatch(userCat ->
                            userCat.equalsIgnoreCase(cat)));
                }

                return keywordMatch || entityMatch;
            });
        }






        // Filter by score threshold
        if (ctx.hasIntent("score") && ctx.getRelevanceScoreThreshold() != null) {
            filtered = filtered.filter(a -> a.getRelevanceScore() != null &&
                a.getRelevanceScore() >= ctx.getRelevanceScoreThreshold());
        }




        // Filter by search keywords in title/description
        if (ctx.hasIntent("search") && ctx.getKeywords() != null) {
            filtered = filtered.filter(a -> {
                String title = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
                String desc = a.getDescription() != null ? a.getDescription().toLowerCase() : "";
                return ctx.getKeywords().stream().anyMatch(k -> title.contains(k.toLowerCase()) || desc.contains(k.toLowerCase()));
            }
            );
        }

        return filtered.toList();
    }



    public String buildBatchSummaryPrompt(List<NewsArticle> topArticles) {
        StringBuilder sb = new StringBuilder("""
        You are an intelligent assistant helping summarize news articles.

        Given the following list of news articles with their titles and descriptions, generate a short 1–2 sentence summary for each.

        Return the result as a JSON array of summaries, in the **same order** as input.

        Format:
        [
          "Summary for article 1",
          "Summary for article 2",
          ...
        ]

        Articles:
    """);

        for (int i = 0; i < topArticles.size(); i++) {
            NewsArticle article = topArticles.get(i);
            sb.append("""
            Article %d:
            Title: %s
            Description: %s

        """.formatted(i + 1, article.getTitle(), article.getDescription()));
        }

        return sb.toString();
    }



    public String callLLMForEnrichment(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
            "message", prompt,
            "model", "command-r-plus",
            "temperature", 0.3,
            "max_tokens", 500
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(COHERE_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                return (String) body.get("text");
            } else {
                throw new RuntimeException("Cohere call failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error calling Cohere LLM: " + e.getMessage(), e);
        }
    }

}
