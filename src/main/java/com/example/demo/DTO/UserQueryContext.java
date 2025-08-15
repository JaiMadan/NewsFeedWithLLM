package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserQueryContext {
    private List<String> intent;
    private List<String> keywords;
    private Double relevanceScoreThreshold;
    private List<String> location;
    private List<String> entities;

    public boolean hasIntent(String type) {
        return intent != null && intent.contains(type);
    }

}
