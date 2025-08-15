package com.example.demo.DTO;


import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NewsFeedResponse {
    private List<NewsResponse> articles;
}
