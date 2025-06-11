package com.totalo.smile.pojo.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostRequest {
    public Long userId;
    public String content;
    public String topic;
    public LocalDateTime time;
}
