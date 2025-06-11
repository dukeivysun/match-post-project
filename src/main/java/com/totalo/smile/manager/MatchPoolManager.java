package com.totalo.smile.manager;

import com.totalo.smile.pojo.dto.PostWithVector;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class MatchPoolManager {
    private final ConcurrentMap<Long, PostWithVector> postPool = new ConcurrentHashMap<>();

    /**
     * 添加帖子
     *
     * @param post
     * @param ttl
     */
    public void addPost(@org.jetbrains.annotations.NotNull PostWithVector post, Duration ttl) {
        post.setExpireTime(LocalDateTime.now().plus(ttl)); // 添加失效时间字段
        postPool.put(post.getUserId(), post);
    }

    /**
     * 获取当前有效候选
     *
     * @param topic
     * @return
     */
    public List<PostWithVector> getCandidates(String topic) {
        LocalDateTime now = LocalDateTime.now();
        return postPool.values().stream()
                .filter(p -> p.getTopic().equals(topic))
                .filter(p -> p.getExpireTime() == null || p.getExpireTime().isAfter(now))
                .collect(Collectors.toList());
    }

    /**
     * 移除（匹配成功或主动退出）
     *
     * @param userId
     */
    public void remove(Long userId) {
        postPool.remove(userId);
    }
}
