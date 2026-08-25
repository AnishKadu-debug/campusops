package com.campusops.incident.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic incidentCreatedTopic(
            @Value("${campusops.kafka.topics.incident-created}") String topicName) {
        return new NewTopic(topicName, 1, (short) 1);
    }

    @Bean
    public NewTopic incidentAssignedTopic(
            @Value("${campusops.kafka.topics.incident-assigned}") String topicName) {
        return new NewTopic(topicName, 1, (short) 1);
    }

    @Bean
    public NewTopic slaBreachedTopic(
            @Value("${campusops.kafka.topics.sla-breached}") String topicName) {
        return new NewTopic(topicName, 1, (short) 1);
    }
}
