package ru.lich333hallow.LandStatesServer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.lich333hallow.LandStatesServer.dto.PlayerDTO;

import java.util.List;

@Data
@Document(collection = "lobbies")
public class Lobby {
    @Id
    private String id;
    @Indexed(unique = true, name = "lobbyId")
    private String lobbyId;
    @Indexed(name = "lobbyName")
    private String LobbyName;
    @Indexed(name = "numberOfPlayers")
    private int numberOfPlayers;
    @Indexed(name = "timeInSeconds")
    private int timeInSeconds;
    @Indexed(name = "lobbyId")
    private List<PlayerDTO> playerDTOS;
    @Indexed(name = "hostId")
    private String hostId;
    @Indexed(name = "active")
    private boolean active;
    @Indexed(name = "nowPlayers")
    private int nowPlayers;
    @Indexed(name = "hostName")
    private String hostName;
}
