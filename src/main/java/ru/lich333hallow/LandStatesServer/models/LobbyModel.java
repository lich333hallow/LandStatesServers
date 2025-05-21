package ru.lich333hallow.LandStatesServer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LobbyModel {
    private String lobbyId;
    private int numberOfPlayers;
    private List<PlayerModel> players;
}
