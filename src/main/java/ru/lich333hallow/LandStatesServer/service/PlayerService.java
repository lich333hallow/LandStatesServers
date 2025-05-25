package ru.lich333hallow.LandStatesServer.service;

import ru.lich333hallow.LandStatesServer.dto.PlayerDTO;
import ru.lich333hallow.LandStatesServer.entity.Player;

public interface PlayerService {

    Player findPlayerById(String id);

    PlayerDTO createPlayer(PlayerDTO playerDTO);
}
