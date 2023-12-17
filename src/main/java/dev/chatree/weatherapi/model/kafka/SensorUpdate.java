package dev.chatree.weatherapi.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorUpdate {
    private String source;
    private String type;
    private Double value;
    private String timeOfEvent;
}
