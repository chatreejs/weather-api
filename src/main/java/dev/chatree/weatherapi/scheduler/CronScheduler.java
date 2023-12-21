package dev.chatree.weatherapi.scheduler;

import dev.chatree.weatherapi.entity.WeatherHistoryEntity;
import dev.chatree.weatherapi.repository.WeatherHistoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Log4j2
@Component
public class CronScheduler {

    @Resource
    private WeatherHistoryEntity weatherHistoryEntityCache;

    private final WeatherHistoryRepository weatherHistoryRepository;

    public CronScheduler(WeatherHistoryRepository weatherHistoryRepository) {
        this.weatherHistoryRepository = weatherHistoryRepository;
    }

    @Scheduled(cron = "${cron.update-weather-history}")
    public void updateWeatherHistory() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (now.getSecond() != 0 ) {
            now = now.minusSeconds(now.getSecond());
        }
        log.info("Update weather history data at {}", now.format(DateTimeFormatter.ISO_DATE_TIME));

        WeatherHistoryEntity weatherHistoryEntity = new WeatherHistoryEntity();
        weatherHistoryEntity.setDate(now.toLocalDate());
        weatherHistoryEntity.setTime(now.toLocalTime().truncatedTo(ChronoUnit.SECONDS));
        weatherHistoryEntity.setSource(weatherHistoryEntityCache.getSource());
        weatherHistoryEntity.setTemperature(weatherHistoryEntityCache.getTemperature());
        weatherHistoryEntity.setHumidity(weatherHistoryEntityCache.getHumidity());
        weatherHistoryEntity.setPressure(weatherHistoryEntityCache.getPressure());
        weatherHistoryEntity.setPm25(weatherHistoryEntityCache.getPm25());

        log.info("weatherHistoryEntity = {}", weatherHistoryEntity);

        weatherHistoryRepository.save(weatherHistoryEntity);
    }
}
