package ru.lich333hallow.LandStatesServer.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import ru.lich333hallow.LandStatesServer.dto.PlayerDTO;

@Controller
public class WebSocketController {

    @MessageMapping("/player")
    @SendTo("/topic/playerInfo")
    public PlayerDTO player(PlayerDTO playerDTO){
        return new PlayerDTO(playerDTO.getPlayerId(), playerDTO.getName());
    }
}
