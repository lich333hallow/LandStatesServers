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

        Lobby lobbyModel = new Lobby();

        lobbyModel.setLobbyName(registerDTO.getLobbyName());
        lobbyModel.setLobbyId(UUID.randomUUID().toString());
        lobbyModel.setPlayerDTOS(registerDTO.getPlayerDTOS());
        lobbyModel.setTimeInSeconds(registerDTO.getTimeInSeconds());
        lobbyModel.setNumberOfPlayers(registerDTO.getNumberOfPlayers());
        lobbyModel.setActive(true);
        lobbyModel.setHostId(registerDTO.getHostId());
        lobbyModel.setHostName(playerRepository.findPlayerByPlayerId(lobbyModel.getHostId()).getName());
        lobbyModel.setNowPlayers(1);

        return LobbyMapper.convertToLobby(lobbyRepository.save(lobbyModel));
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
        Lobby lobbyModel = lobbyRepository.findByLobbyId(id).orElseThrow(() -> new LobbyExceptionNotFound("Lobby not found!"));

        lobbyModel.setLobbyName(lobbyDTO.getLobbyName());
        lobbyModel.setTimeInSeconds(lobbyDTO.getTimeInSeconds());
        lobbyModel.setHostId(lobbyDTO.getHostId());
        lobbyModel.setPlayerDTOS(lobbyDTO.getPlayerDTOS());
        lobbyModel.setNumberOfPlayers(lobbyDTO.getNumberOfPlayers());
        lobbyModel.setHostName(lobbyDTO.getHostName());

        lobbyModel.setNowPlayers(lobbyDTO.getNowPlayers());
        lobbyModel.setActive(lobbyModel.getNowPlayers() != lobbyModel.getNumberOfPlayers() || lobbyModel.getNowPlayers() == 0);

        return LobbyMapper.convertToLobby(lobbyRepository.save(lobbyModel));
    }
}
