package ru.lich333hallow.LandStatesServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.lich333hallow.LandStatesServer.dto.LobbyDTO;
import ru.lich333hallow.LandStatesServer.dto.LobbyRegisterDTO;
import ru.lich333hallow.LandStatesServer.service.LobbyService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LobbyController {
    public final LobbyService lobbyService;

    @GetMapping("/api/getAllLobbies")
    public ResponseEntity<List<LobbyDTO>> getLobbies(){
        return ResponseEntity.ok(lobbyService.getAllLobbies());
    }

    @GetMapping("/api/getLobbyById/{id}")
    public ResponseEntity<LobbyDTO> getLobbyById(@PathVariable String id){
        return ResponseEntity.ok(lobbyService.getLobbyById(id));
    }

    @PostMapping("/api/createLobby")
    public ResponseEntity<LobbyDTO> createLobby(@RequestBody LobbyRegisterDTO lobby){
        return ResponseEntity.ok(lobbyService.createLobby(lobby));
    }

    @GetMapping("/api/getLobbyByHost/{id}/{status}")
    public ResponseEntity<LobbyDTO> getLobbyByHost(@PathVariable String id, @PathVariable boolean status){
        return ResponseEntity.ok(lobbyService.getLobbyByHost(status, id));
    }

    @GetMapping("/api/getActiveLobby")
    public ResponseEntity<List<LobbyDTO>> getActiveLobby(){
        return ResponseEntity.ok(lobbyService.getLobbyActive());
    }

    @PutMapping("/api/updateLobby/{id}")
    public ResponseEntity<LobbyDTO> updateLobbyDTO(@PathVariable String id, @RequestBody LobbyDTO lobbyDTO){
        return ResponseEntity.ok(lobbyService.updateLobby(id, lobbyDTO));
    }
}
