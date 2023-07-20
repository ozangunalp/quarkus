package io.quarkus.kafka.client.runtime.ui.model.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties("action")
public class KafkaMessageCreateRequest {

    private String topic;
    private String type;
    private Integer partition;
    private String value;
    private String key;
    private Map<String, String> headers;

    public KafkaMessageCreateRequest() {
    }

    public KafkaMessageCreateRequest(String topic, String type, Integer partition, String value, String key,
            Map<String, String> headers) {
        this.topic = topic;
        this.type = type;
        this.partition = partition;
        this.value = value;
        this.key = key;
        this.headers = headers;
    }

    public String getTopic() {
        return topic;
    }

    public String getType() {
        return type;
    }

    public Integer getPartition() {
        return partition;
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
