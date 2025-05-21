package ru.lich333hallow.LandStatesServer.service;

import ru.lich333hallow.LandStatesServer.dto.LobbyDTO;
import ru.lich333hallow.LandStatesServer.dto.LobbyRegisterDTO;

import java.util.List;

public interface LobbyService {

    List<LobbyDTO> getAllLobbies();

    LobbyDTO getLobbyById(String id);

    LobbyDTO createLobby(LobbyRegisterDTO lobbyDTO);

    LobbyDTO getLobbyByHost(boolean status, String hostId);

    List<LobbyDTO> getLobbyActive();

    LobbyDTO updateLobby(String id, LobbyDTO lobbyDTO);

}
