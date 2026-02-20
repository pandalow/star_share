package com.star.share.counter.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableKafka
public class CounterConfig {
    @Bean
    public ProducerFactory<String, String> stringProducerFactory(KafkaProperties properties, SslBundles sslBundles) {
        var props = properties.buildProducerProperties(sslBundles);
        return new DefaultKafkaProducerFactory<>(props); // Using the default StringSerializer for both key and value
    }

    @Bean
    public KafkaTemplate<String, String> stringkafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
