package com.example.demo.Repositories;

import com.example.demo.Entities.NewsArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle,String> {

    @Query(
        """
          Select article from NewsArticle article
          where LOWER(article.sourceName) like LOWER(CONCAT('%', :source, '%'))
          order by article.publicationDate desc
        """
    )
    List<NewsArticle> findBySource(@Param("source") String source, Pageable pageable);


    @Query("""
    SELECT DISTINCT article
    FROM NewsArticle article
    JOIN article.category c
    WHERE LOWER(c) LIKE LOWER(CONCAT('%', :category, '%'))
    ORDER BY article.publicationDate DESC
    """)
    List<NewsArticle> findByCategory(@Param("category") String category, Pageable pageable);

    @Query(
        """
          Select article from NewsArticle article
          where article.relevanceScore >=:score
          order by article.relevanceScore desc
        """
    )
    List<NewsArticle> findByScore(@Param("score") Double category, Pageable pageable);

}
