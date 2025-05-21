package ru.lich333hallow.LandStatesServer.utils;

import lombok.experimental.UtilityClass;
import ru.lich333hallow.LandStatesServer.dto.LobbyDTO;
import ru.lich333hallow.LandStatesServer.entity.Lobby;

@UtilityClass
public class LobbyMapper {

    public LobbyDTO convertToLobby(Lobby lobby){
        LobbyDTO lobbyDTO = new LobbyDTO();

        lobbyDTO.setHostId(lobby.getHostId());
        lobbyDTO.setLobbyId(lobby.getLobbyId());
        lobbyDTO.setLobbyName(lobby.getLobbyName());
        lobbyDTO.setPlayerDTOS(lobby.getPlayerDTOS());
        lobbyDTO.setTimeInSeconds(lobby.getTimeInSeconds());
        lobbyDTO.setNumberOfPlayers(lobby.getNumberOfPlayers());
        lobbyDTO.setNowPlayers(lobby.getNowPlayers());
        lobbyDTO.setHostName(lobby.getHostName());

        return lobbyDTO;
    }
}
