package dev.chatree.weatherapi.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.chatree.weatherapi.entity.WeatherHistoryEntity;
import dev.chatree.weatherapi.model.kafka.SensorUpdate;
import dev.chatree.weatherapi.repository.WeatherHistoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

@Log4j2
@Service
public class WeatherSensorConsumerService extends AbstractConsumerSeekAware {

    @Value("${kafka.in.topics.weatherstation-sensor.key.data}")
    private String weatherStationSensorDataKey;

    @Resource
    private WeatherHistoryEntity weatherHistoryEntityCache;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @KafkaListener(
            topics = "${kafka.in.topics.weatherstation-sensor.topic}",
            groupId = "${kafka.consumer.group-id-prefix}-weatherapi${kafka.config.environment}")
    public void consumeWeatherStationSensorMessage(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                                   @Header("__TypeId__") String type,
                                                   @Header(KafkaHeaders.OFFSET) Long offset,
                                                   @Header(KafkaHeaders.RECEIVED_TIMESTAMP) Long timestamp,
                                                   @Payload String value) {
        if (key.equalsIgnoreCase(weatherStationSensorDataKey)) {
            OffsetDateTime offsetDateTime;

            if (type.equals("SensorUpdate")) {
                log.info("type = {}", type);
                log.info("key = {}", key);
                log.info("offset = {}", offset);
                offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());
                log.info("timestamp = {}", offsetDateTime);
                log.info("value = {}", value);

                try {
                    SensorUpdate sensorUpdate = gson.fromJson(value, SensorUpdate.class);
                    weatherHistoryEntityCache.setDate(offsetDateTime.toLocalDate());
                    weatherHistoryEntityCache.setTime(offsetDateTime.toLocalTime());
                    weatherHistoryEntityCache.setTimestamp(offsetDateTime.toString());
                    weatherHistoryEntityCache.setSource(sensorUpdate.getSource().split("\\.")[0]);

                    if (sensorUpdate.getType().equals("temperature")) {
                        weatherHistoryEntityCache.setTemperature(sensorUpdate.getValue());
                    }
                    if (sensorUpdate.getType().equals("humidity")) {
                        weatherHistoryEntityCache.setHumidity(sensorUpdate.getValue());
                    }
                    if (sensorUpdate.getType().equals("pressure")) {
                        weatherHistoryEntityCache.setPressure(sensorUpdate.getValue());
                    }
                    if (sensorUpdate.getType().equals("pm25")) {
                        weatherHistoryEntityCache.setPm25(sensorUpdate.getValue().intValue());
                    }
                } catch (Exception e) {
                    log.error("Consume SensorUpdate error: ", e);
                }
            }
        }
    }
}
