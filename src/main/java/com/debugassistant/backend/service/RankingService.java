package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import org.springframework.stereotype.Service;

/**
 * Calculates a relevance score to sort GitHub issues.
 */
@Service
public class RankingService {

    public double calculateScore(GitHubIssue issue) {
        if (issue == null) {
            return 0.0;
        }

        // safety check for null values
        int comments = issue.comments() != null ? issue.comments() : 0;
        int reactions = 0;

        if (issue.reactions() != null && issue.reactions().totalCount() != null) {
            reactions = issue.reactions().totalCount();
        }

        // reactions count double because they indicate a helpful solution
        return (reactions * 2.0) + comments;
    }
}