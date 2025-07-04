package ru.lich333hallow.LandStatesServer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameModel {
    private String id;
    private List<PlayerModelInGame> players;
    private int gameTime;
}
