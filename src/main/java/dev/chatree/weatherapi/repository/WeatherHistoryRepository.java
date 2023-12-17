package dev.chatree.weatherapi.repository;

import dev.chatree.weatherapi.entity.WeatherHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface WeatherHistoryRepository extends JpaRepository<WeatherHistoryEntity, Long> {

    WeatherHistoryEntity findTopByDateAndTime(LocalDate localDate, LocalTime localTime);
}
