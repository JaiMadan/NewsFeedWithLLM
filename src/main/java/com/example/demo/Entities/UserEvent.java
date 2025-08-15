package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class UserEvent {
    @Id
    @GeneratedValue
    private Long id;

    private String userId;
    private String articleId;
    private String eventType; // e.g., view, click
    private LocalDateTime eventTime;
    private Double latitude;
    private Double longitude;
}

