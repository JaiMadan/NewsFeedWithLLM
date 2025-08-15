package com.example.demo.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class NewsResponse {
    private String title;
    private String description;
    private String url;

    private LocalDateTime publication_date;
    private String source_name;
    private List<String> category;
    private double relevance_score;

    private String llm_summary;
    private double latitude;
    private double longitude;
}
