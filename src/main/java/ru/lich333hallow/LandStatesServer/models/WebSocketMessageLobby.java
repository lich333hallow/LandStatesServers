package ru.lich333hallow.LandStatesServer.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessageLobby {
    private String type;
    private String playerName;
    private String numberOfPlayers;
    private Boolean ready;
    private String lobbyId;
}

