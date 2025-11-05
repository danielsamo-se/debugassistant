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

        int comments = issue.comments() != null ? issue.comments() : 0;
        int reactions = 0;

        if (issue.reactions() != null && issue.reactions().totalCount() != null) {
            reactions = issue.reactions().totalCount();
        }

        // reactions are worth more than comments
        double score = (reactions * 2.0) + comments;

        // closed issue likely contains a solution
        if ("closed".equalsIgnoreCase(issue.state())) {
            score += 5.0;
        }

        return score;
    }
}