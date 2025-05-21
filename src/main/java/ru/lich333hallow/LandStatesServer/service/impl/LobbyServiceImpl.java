package ru.lich333hallow.LandStatesServer.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.lich333hallow.LandStatesServer.dto.LobbyDTO;
import ru.lich333hallow.LandStatesServer.dto.LobbyRegisterDTO;
import ru.lich333hallow.LandStatesServer.entity.Lobby;
import ru.lich333hallow.LandStatesServer.exceptions.LobbyExceptionNotFound;
import ru.lich333hallow.LandStatesServer.repository.LobbyRepository;
import ru.lich333hallow.LandStatesServer.repository.PlayerRepository;
import ru.lich333hallow.LandStatesServer.service.LobbyService;
import ru.lich333hallow.LandStatesServer.utils.LobbyMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LobbyServiceImpl implements LobbyService {

    @Getter
    public final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;

    @Override
    public List<LobbyDTO> getAllLobbies() {
        return lobbyRepository.findAll().stream()
                .map(LobbyMapper::convertToLobby)
                .collect(Collectors.toList());
    }

    @Override
    public LobbyDTO getLobbyById(String id) {
        Optional<Lobby> lobby = lobbyRepository.findByLobbyId(id);
        if(lobby.isEmpty()){
            throw new LobbyExceptionNotFound("Lobby not found");
        }
        return LobbyMapper.convertToLobby(lobby.get());
    }

    @Override
    public LobbyDTO createLobby(LobbyRegisterDTO registerDTO) {

        Lobby lobby = new Lobby();

        lobby.setLobbyName(registerDTO.getLobbyName());
        lobby.setLobbyId(UUID.randomUUID().toString());
        lobby.setPlayerDTOS(registerDTO.getPlayerDTOS());
        lobby.setTimeInSeconds(registerDTO.getTimeInSeconds());
        lobby.setNumberOfPlayers(registerDTO.getNumberOfPlayers());
        lobby.setActive(true);
        lobby.setHostId(registerDTO.getHostId());
        lobby.setHostName(playerRepository.findPlayerByPlayerId(lobby.getHostId()).getName());
        lobby.setNowPlayers(1);

        return LobbyMapper.convertToLobby(lobbyRepository.save(lobby));
    }

    @Override
    public LobbyDTO getLobbyByHost(boolean status, String hostId) {
        Optional<Lobby> lobby = lobbyRepository.findByActiveAndHostId(status, hostId);

        if(lobby.isEmpty()){
            throw new LobbyExceptionNotFound("Lobby with this id doesn't exists");
        }

        return LobbyMapper.convertToLobby(lobby.get());
    }

    @Override
    public List<LobbyDTO> getLobbyActive() {
        return lobbyRepository.findByActive(true).stream()
                .map(LobbyMapper::convertToLobby)
                .collect(Collectors.toList());
    }

    @Override
    public LobbyDTO updateLobby(String id, LobbyDTO lobbyDTO) {
        Lobby lobby = lobbyRepository.findByLobbyId(id).orElseThrow(() -> new LobbyExceptionNotFound("Lobby not found!"));

        lobby.setLobbyName(lobbyDTO.getLobbyName());
        lobby.setTimeInSeconds(lobbyDTO.getTimeInSeconds());
        lobby.setHostId(lobbyDTO.getHostId());
        lobby.setPlayerDTOS(lobbyDTO.getPlayerDTOS());
        lobby.setNumberOfPlayers(lobbyDTO.getNumberOfPlayers());
        lobby.setHostName(lobbyDTO.getHostName());

        lobby.setNowPlayers(lobbyDTO.getNowPlayers());
        lobby.setActive(lobby.getNowPlayers() != lobby.getNumberOfPlayers() || lobby.getNowPlayers() == 0);

        return LobbyMapper.convertToLobby(lobbyRepository.save(lobby));
    }
}
