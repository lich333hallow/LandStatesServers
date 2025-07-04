package ru.lich333hallow.LandStatesServer.states;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.lich333hallow.LandStatesServer.models.PlayerModel;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LobbyState {
    private String type;
    private String lobbyId;
    private List<PlayerModel> players;
}
