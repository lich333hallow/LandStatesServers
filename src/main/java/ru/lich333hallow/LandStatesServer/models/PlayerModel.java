package ru.lich333hallow.LandStatesServer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerModel {
    private String sessionId;
    private String name;
    private String color;
    private boolean ready;

    public PlayerModel(String sessionId, String name, String color){
        this.name = name;
        this.sessionId = sessionId;
        this.color = color;
    }
}
