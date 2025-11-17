package com.debugassistant.backend.repository;

import com.debugassistant.backend.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId);
}