package ru.lich333hallow.LandStatesServer.states;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.lich333hallow.LandStatesServer.models.PlayerModelInGame;

import java.util.List;

@Data
@AllArgsConstructor
public class WinnerState {
    private String type;
    private List<PlayerModelInGame> winners;
}
