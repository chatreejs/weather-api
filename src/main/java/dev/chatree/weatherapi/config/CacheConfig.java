package dev.chatree.weatherapi.config;

import dev.chatree.weatherapi.entity.WeatherHistoryEntity;
import dev.chatree.weatherapi.model.kafka.SensorUpdate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    @Bean
    public WeatherHistoryEntity weatherHistoryEntityCache() {
        return new WeatherHistoryEntity();
    }
}
