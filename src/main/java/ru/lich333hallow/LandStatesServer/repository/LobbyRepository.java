package ru.lich333hallow.LandStatesServer.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.lich333hallow.LandStatesServer.entity.Lobby;

import java.util.List;
import java.util.Optional;

public interface LobbyRepository extends MongoRepository<Lobby, String> {

    Optional<Lobby> findByLobbyId(String id);

    Optional<Lobby> findByHostId(String hostId);

    List<Lobby> findByActive(boolean status);

    Optional<Lobby> findByActiveAndHostId(boolean status, String hostId);
}
