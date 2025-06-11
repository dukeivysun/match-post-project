package com.totalo.smile.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取文本的语义向量（使用 Ollama 本地 bge-m3 模型）
     *
     * @param text
     * @return
     */
    public float[] getEmbedding(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("");  // 设置 Authorization: Bearer xxx

        EmbeddingRequest requestBody = new EmbeddingRequest("bge-m3", text);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<OllamaEmbeddingResponse> response = restTemplate.exchange(
                "http://localhost:11434/api/embeddings",
                HttpMethod.POST,
                entity,
                OllamaEmbeddingResponse.class
        );

        assert response.getBody() != null;
        List<Double> embedding = response.getBody().getEmbedding();
        if (CollectionUtils.isEmpty(embedding)) {
            throw new RuntimeException("Ollama 返回空向量，确认模型是否正常运行");
        }

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }

        return result;
    }

    /**
     * 请求体：必须使用 input 字段（不是 prompt）
     */
    @Setter
    @Getter
    public static class EmbeddingRequest {
        private String model;
        private String prompt;

        public EmbeddingRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

    }

    /**
     * 响应体（Ollama 会返回 embedding 一维向量）
     */
    @Setter
    @Getter
    public static class OllamaEmbeddingResponse {
        private List<Double> embedding;
    }
}
