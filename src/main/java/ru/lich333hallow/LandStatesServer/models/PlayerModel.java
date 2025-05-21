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
    private boolean ready;

    public PlayerModel(String sessionId, String name){
        this.name = name;
        this.sessionId = sessionId;
    }

    public PlayerModel(String name, boolean ready){
        this.name = name;
        this.ready = ready;
    }
}
