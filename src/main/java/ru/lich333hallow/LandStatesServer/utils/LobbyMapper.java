package ru.lich333hallow.LandStatesServer.utils;

import lombok.experimental.UtilityClass;
import ru.lich333hallow.LandStatesServer.dto.LobbyDTO;
import ru.lich333hallow.LandStatesServer.entity.Lobby;

@UtilityClass
public class LobbyMapper {

    public LobbyDTO convertToLobby(Lobby lobbyModel){
        LobbyDTO lobbyDTO = new LobbyDTO();

        lobbyDTO.setHostId(lobbyModel.getHostId());
        lobbyDTO.setLobbyId(lobbyModel.getLobbyId());
        lobbyDTO.setLobbyName(lobbyModel.getLobbyName());
        lobbyDTO.setPlayerDTOS(lobbyModel.getPlayerDTOS());
        lobbyDTO.setTimeInSeconds(lobbyModel.getTimeInSeconds());
        lobbyDTO.setNumberOfPlayers(lobbyModel.getNumberOfPlayers());
        lobbyDTO.setNowPlayers(lobbyModel.getNowPlayers());
        lobbyDTO.setHostName(lobbyModel.getHostName());

        return lobbyDTO;
    }
}
