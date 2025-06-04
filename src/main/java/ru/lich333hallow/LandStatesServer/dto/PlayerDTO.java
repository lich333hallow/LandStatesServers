package ru.lich333hallow.LandStatesServer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDTO {
    private String playerId;
    private String name;
    private String color;

}
