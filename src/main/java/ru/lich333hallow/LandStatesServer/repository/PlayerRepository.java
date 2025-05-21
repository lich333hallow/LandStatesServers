package ru.lich333hallow.LandStatesServer.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.lich333hallow.LandStatesServer.entity.Player;

public interface PlayerRepository extends MongoRepository<Player, String> {
    Player findPlayerByPlayerId(String playerId);
}
