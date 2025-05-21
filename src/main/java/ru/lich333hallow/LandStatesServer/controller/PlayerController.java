package ru.lich333hallow.LandStatesServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.lich333hallow.LandStatesServer.dto.PlayerDTO;
import ru.lich333hallow.LandStatesServer.service.PlayerService;

@Controller
@RequiredArgsConstructor
public class PlayerController {
    public final PlayerService playerService;

    @PostMapping("/api/createPlayer")
    public ResponseEntity<PlayerDTO> createNewPlayer(@RequestBody PlayerDTO playerDTO){
        return ResponseEntity.ok(playerService.createPlayer(playerDTO));
    }
}
