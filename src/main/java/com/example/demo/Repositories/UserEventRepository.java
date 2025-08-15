package com.example.demo.Repositories;

import com.example.demo.Entities.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent,Long> {

    @Query(
        """
            SELECT e FROM UserEvent e WHERE e.eventTime >= :recentTime
        """)
    List<UserEvent> findRecentEvents(@Param("recentTime") LocalDateTime recentTime);
}
