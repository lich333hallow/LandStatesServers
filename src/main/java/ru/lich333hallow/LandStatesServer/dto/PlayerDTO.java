package ru.lich333hallow.LandStatesServer.dto;

import lombok.Data;


@Data
public class PlayerDTO {
    private String playerId;
    private String name;

    public PlayerDTO(){

    }

    public PlayerDTO(String playerId, String name){
        this.playerId = playerId;
        this.name = name;
    }
}
