package ru.lich333hallow.LandStatesServer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "players")
public class Player {
    @Id
    private String id;
    @Indexed(name = "playerId")
    private String playerId;
    @Indexed(name = "name")
    private String name;
    @Indexed(name = "color")
    private String color;
}
