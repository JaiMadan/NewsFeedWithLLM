package com.example.demo.JsonToRDBMS;

import com.example.demo.Entities.NewsArticle;
import com.example.demo.Repositories.NewsArticleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class JsonReaderAndDbInsertion implements CommandLineRunner {

    @Autowired
    private NewsArticleRepository newsRepo;

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        File jsonFile = new File("src/main/resources/news_data.json");

        TypeReference<List<NewsArticle>> typeRef = new TypeReference<>() {};
        List<NewsArticle> articles = mapper.readValue(jsonFile, typeRef);

        newsRepo.saveAll(articles);
    }
}
