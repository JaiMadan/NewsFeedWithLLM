package com.example.demo.Entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "news_articles")
@Data
@NoArgsConstructor
public class NewsArticle {

    @Id
    private String id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 4000)
    private String url;

    @JsonProperty("publication_date")
    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @JsonProperty("source_name")
    @Column(name = "source_name")
    private String sourceName;

    @JsonProperty("relevance_score")
    private Double relevanceScore;
    private Double latitude;
    private Double longitude;


    @Transient
    private String llmSummary;

    @Transient
    private double finalScore;

    @Transient
    private double distanceToUser;


    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> category;

    // Getters and setters
}
