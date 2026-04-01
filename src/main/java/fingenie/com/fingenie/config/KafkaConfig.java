package fingenie.com.fingenie.config;

import fingenie.com.fingenie.kafka.event.ModelPredictionEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for AI Event Pipeline
 * 
 * Topics:
 * - transaction-events: Transaction CRUD events from Spring Boot
 * - user-events: User activity events (logins, profile updates)
 * - survey-events: Financial survey/feedback responses
 * - model-predictions: AI predictions from Python AI Service
 * - feature-updates: Feature store update notifications
 * 
 * Consumer Groups:
 * - fingenie-ai-consumers: AI Service consumers
 * - fingenie-feature-updaters: Feature store update consumers
 * - fingenie-audit-loggers: Audit logging consumers
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:fingenie-spring-consumers}")
    private String consumerGroupId;

    // ============================================
    // Topic Configuration
    // ============================================
    
    public static final String TOPIC_TRANSACTION_EVENTS = "transaction-events";
    public static final String TOPIC_USER_EVENTS = "user-events";
    public static final String TOPIC_SURVEY_EVENTS = "survey-events";
    public static final String TOPIC_MODEL_PREDICTIONS = "model-predictions";
    public static final String TOPIC_FEATURE_UPDATES = "feature-updates";
    public static final String TOPIC_DEAD_LETTER = "fingenie-dlq";

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTION_EVENTS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(TOPIC_USER_EVENTS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic surveyEventsTopic() {
        return TopicBuilder.name(TOPIC_SURVEY_EVENTS)
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic modelPredictionsTopic() {
        return TopicBuilder.name(TOPIC_MODEL_PREDICTIONS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic featureUpdatesTopic() {
        return TopicBuilder.name(TOPIC_FEATURE_UPDATES)
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "86400000") // 1 day
                .build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(TOPIC_DEAD_LETTER)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .build();
    }

    // ============================================
    // Producer Configuration
    // ============================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Performance settings
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ============================================
    // Consumer Configuration
    // ============================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        
        // Reliability settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        // Trust packages for JSON deserialization
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "fingenie.com.fingenie.kafka.event");
        // Python AI producer does not set Spring type headers.
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ModelPredictionEvent.class.getName());
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
