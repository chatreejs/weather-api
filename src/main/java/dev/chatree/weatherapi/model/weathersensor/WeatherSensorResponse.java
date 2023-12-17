package dev.chatree.weatherapi.model.weathersensor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherSensorResponse {
    private String timestamp;
    private Double temperature;
    private Double humidity;
    private Double pressure;
    private Integer pm25;
}
