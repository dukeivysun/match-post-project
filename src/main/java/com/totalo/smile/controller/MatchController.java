package com.totalo.smile.controller;

import com.totalo.smile.pojo.request.PostRequest;
import com.totalo.smile.manager.MatchPoolManager;
import com.totalo.smile.matcher.SemanticMatcher;
import com.totalo.smile.service.EmbeddingService;
import com.totalo.smile.pojo.dto.PostWithVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/match")
public class MatchController {
    private final EmbeddingService embeddingService;
    private final MatchPoolManager matchPool;
    private final SemanticMatcher semanticMatcher;

    @Autowired
    public MatchController(EmbeddingService embeddingService, MatchPoolManager matchPool,
                           SemanticMatcher semanticMatcher) {
        this.embeddingService = embeddingService;
        this.matchPool = matchPool;
        this.semanticMatcher = semanticMatcher;
    }

    // 使用 semanticMatcher 内建的缓存机制
    @PostMapping
    public MatchResponse post(@RequestBody PostRequest req) {
        float[] vector = semanticMatcher.getCachedVector(req.getContent(), embeddingService::getEmbedding);
        PostWithVector currentPost = new PostWithVector();
        currentPost.setUserId(req.getUserId());
        currentPost.setContent(req.getContent());
        currentPost.setTopic(req.getTopic());
        currentPost.setTime(req.getTime());
        currentPost.setVector(vector);

        matchPool.addPost(currentPost, Duration.ofMinutes(30));
        List<PostWithVector> pool = matchPool.getCandidates(req.topic);
        SemanticMatcher.MatchResult result = semanticMatcher.match(currentPost, pool);
        return new MatchResponse(result.preciseMatches(), result.recommendedMatches());
    }

    /**
     * 返回对象
     *
     * @param preciseMatches
     * @param recommendedMatches
     */
    public record MatchResponse(
            List<PostWithVector> preciseMatches,
            List<PostWithVector> recommendedMatches
    ) {
    }

}
