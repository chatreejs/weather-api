package dev.chatree.weatherapi.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.chatree.weatherapi.entity.WeatherHistoryEntity;
import dev.chatree.weatherapi.model.kafka.SensorUpdate;
import dev.chatree.weatherapi.model.weathersensor.WeatherSensorResponse;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.TimeZone;

@Log4j2
@Service
public class WeatherSensorConsumerService extends AbstractConsumerSeekAware {

    @Value("${kafka.in.topics.weatherstation-sensor.key.data}")
    private String weatherStationSensorDataKey;

    @Resource
    private WeatherSensorResponse weatherSensorResponseCache;

    @Resource
    private WeatherHistoryEntity weatherHistoryEntityCache;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private final SimpMessagingTemplate simpMessagingTemplate;

    public WeatherSensorConsumerService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

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
                    weatherSensorResponseCache.setTimestamp(offsetDateTime.toString());

                    weatherHistoryEntityCache.setDate(offsetDateTime.toLocalDate());
                    weatherHistoryEntityCache.setTime(offsetDateTime.toLocalTime());
                    weatherHistoryEntityCache.setTimestamp(offsetDateTime.toString());
                    weatherHistoryEntityCache.setSource(sensorUpdate.getSource().split("\\.")[0]);

                    if (sensorUpdate.getType().equals("temperature")) {
                        weatherSensorResponseCache.setTemperature(sensorUpdate.getValue());
                        weatherHistoryEntityCache.setTemperature(sensorUpdate.getValue());
                    }
                    if (sensorUpdate.getType().equals("humidity")) {
                        weatherSensorResponseCache.setHumidity(sensorUpdate.getValue());
                        weatherHistoryEntityCache.setHumidity(sensorUpdate.getValue());
                    }
                    if (sensorUpdate.getType().equals("pressure")) {
                        weatherSensorResponseCache.setPressure(sensorUpdate.getValue());
                        weatherHistoryEntityCache.setPressure(sensorUpdate.getValue());
                    }
                    if (sensorUpdate.getType().equals("pm25")) {
                        weatherSensorResponseCache.setPm25(sensorUpdate.getValue().intValue());
                        weatherHistoryEntityCache.setPm25(sensorUpdate.getValue().intValue());
                    }

                    simpMessagingTemplate.convertAndSend("/topic/weather-sensor", weatherSensorResponseCache);
                } catch (Exception e) {
                    log.error("Consume SensorUpdate error: ", e);
                }
            }
        }
    }
}
