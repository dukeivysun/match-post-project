package com.totalo.smile.pojo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostWithVector {
    private Long userId;
    private String topic;
    private String content;
    private float[] vector;
    private LocalDateTime time;
    private LocalDateTime expireTime;
}
