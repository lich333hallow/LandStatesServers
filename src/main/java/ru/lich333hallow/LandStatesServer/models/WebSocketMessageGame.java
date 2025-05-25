package ru.lich333hallow.LandStatesServer.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessageGame {
    private String type;
    private String lobbyId;
    private String name;
    private String target;
    private String balance;
    private String miners;
    private String defenders;
    private String bases;
}
