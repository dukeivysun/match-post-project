package com.totalo.smile.matcher;

import com.totalo.smile.pojo.dto.PostWithVector;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SemanticMatcher {

    // 分段阈值
    private static final double PRECISE_THRESHOLD = 0.85;
    private static final double SUGGEST_THRESHOLD = 0.7;

    // 向量缓存（可用 Caffeine、Guava 或 Redis 替代）
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    /**
     * 获取（缓存后）的语义向量
     */
    public float[] getCachedVector(String text, @NotNull VectorProvider provider) {
        return embeddingCache.computeIfAbsent(text, provider::getVector);
    }

    /**
     * 进行分级匹配
     */
    public MatchResult match(PostWithVector current, @NotNull List<PostWithVector> pool) {
        List<ScoredPost> scored = pool.stream()
                .filter(p -> !p.getUserId().equals(current.getUserId()))
                .filter(p -> Math.abs(Duration.between(current.getTime(), p.getTime()).toMinutes()) <= 90)
                .map(p -> {
                    double sim = cosineSimilarity(current.getVector(), p.getVector());
                    log.info("与用户 {} 相似度：{}", p.getUserId(), sim);
                    return new ScoredPost(p, sim);
                })
                .sorted(Comparator.comparingDouble(ScoredPost::similarity).reversed())
                .toList();

        List<PostWithVector> precise = scored.stream()
                .filter(s -> s.similarity >= PRECISE_THRESHOLD)
                .map(s -> s.post)
                .toList();

        List<PostWithVector> suggestions = scored.stream()
                .filter(s -> s.similarity >= SUGGEST_THRESHOLD && s.similarity < PRECISE_THRESHOLD)
                .map(s -> s.post)
                .toList();

        return new MatchResult(precise, suggestions);
    }

    /**
     * 计算余弦相似度
     */
    public double cosineSimilarity(@NotNull float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 提供向量的函数式接口（用于注入外部 EmbeddingService）
     */
    public interface VectorProvider {
        float[] getVector(String text);
    }

    /**
     * 匹配结果分为 精准匹配 和 推荐匹配
     */
    public record MatchResult(List<PostWithVector> preciseMatches, List<PostWithVector> recommendedMatches) {
        public boolean hasMatches() {
            return !preciseMatches.isEmpty() || !recommendedMatches.isEmpty();
        }

        public int totalMatchCount() {
            return preciseMatches.size() + recommendedMatches.size();
        }
    }

    /**
     * 带相似度评分的中间匹配对象，用于排序和筛选
     */
    public record ScoredPost(
            PostWithVector post,
            double similarity
    ) implements Comparable<ScoredPost> {

        @Contract(pure = true)
        @Override
        public int compareTo(@NotNull ScoredPost other) {
            return Double.compare(other.similarity, this.similarity); // 降序排序
        }
    }

}
