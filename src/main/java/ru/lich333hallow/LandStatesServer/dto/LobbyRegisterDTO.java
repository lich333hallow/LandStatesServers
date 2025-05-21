package ru.lich333hallow.LandStatesServer.dto;

import lombok.Data;

import java.util.List;

@Data
public class LobbyRegisterDTO {
    private String hostId;
    private String LobbyName;
    private int numberOfPlayers;
    private int timeInSeconds;
    private List<PlayerDTO> playerDTOS;
}
