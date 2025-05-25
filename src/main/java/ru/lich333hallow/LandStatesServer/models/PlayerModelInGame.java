package ru.lich333hallow.LandStatesServer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerModelInGame {
    private String sessionId;
    private String name;
    private String number;
    private int balance;
    private int miners;
    private int defenders;
    private int bases;
    private int warriors;
}
