package ru.lich333hallow.LandStatesServer.utils;

import lombok.experimental.UtilityClass;
import ru.lich333hallow.LandStatesServer.dto.PlayerDTO;
import ru.lich333hallow.LandStatesServer.entity.Player;

@UtilityClass
public class PlayerMapper {
    public PlayerDTO convertToPlayer(Player player){
        PlayerDTO playerDTO = new PlayerDTO();

        playerDTO.setName(player.getName());
        playerDTO.setPlayerId(player.getPlayerId());

        return playerDTO;
    }
}
