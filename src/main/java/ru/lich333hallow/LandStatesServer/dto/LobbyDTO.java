package ru.lich333hallow.LandStatesServer.dto;

import lombok.Data;

import java.util.List;

@Data
public class LobbyDTO {
    private String lobbyId;
    private String lobbyName;
    private String hostId;
    private String hostName;
    private int numberOfPlayers;
    private int timeInSeconds;
    private List<PlayerDTO> playerDTOS;
    private int nowPlayers;
}
