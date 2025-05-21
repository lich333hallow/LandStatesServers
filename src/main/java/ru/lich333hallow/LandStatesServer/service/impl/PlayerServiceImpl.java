package ru.lich333hallow.LandStatesServer.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.lich333hallow.LandStatesServer.dto.PlayerDTO;
import ru.lich333hallow.LandStatesServer.entity.Player;
import ru.lich333hallow.LandStatesServer.repository.PlayerRepository;
import ru.lich333hallow.LandStatesServer.service.PlayerService;
import ru.lich333hallow.LandStatesServer.utils.PlayerMapper;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    @Override
    public Player findPlayerById(String id) {
        return playerRepository.findPlayerByPlayerId(id);
    }

    @Override
    public PlayerDTO createPlayer(PlayerDTO playerDTO) {

        Player player = new Player();

        player.setPlayerId(playerDTO.getPlayerId());
        player.setName(playerDTO.getName());

        return PlayerMapper.convertToPlayer(playerRepository.save(player));
    }
}
