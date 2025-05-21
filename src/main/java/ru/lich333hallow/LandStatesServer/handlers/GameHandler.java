package ru.lich333hallow.LandStatesServer.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.lich333hallow.LandStatesServer.models.GameModel;
import ru.lich333hallow.LandStatesServer.models.LobbyModel;
import ru.lich333hallow.LandStatesServer.states.GameState;
import ru.lich333hallow.LandStatesServer.states.LobbyState;
import ru.lich333hallow.LandStatesServer.models.PlayerModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameHandler extends TextWebSocketHandler {
    private ObjectMapper objectMapper;
    private List<WebSocketSession> sessions = new ArrayList<>();
    private List<GameModel> games = new ArrayList<>();

    public GameHandler(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    private void broadcastGameState(String type) throws IOException {
        for (GameModel game : games) {
            String gameState = objectMapper.writeValueAsString(
                    new GameState()
            );

            for (PlayerModel player : game.getPlayers()) {
                sessions.stream()
                        .filter(s -> s.getId().equals(player.getSessionId()))
                        .findFirst()
                        .ifPresent(s -> {
                            try {
                                s.sendMessage(new TextMessage(gameState));
                            } catch (IOException e) {
                                System.err.println("Error broadcasting to session " + s.getId());
                            }
                        });
            }
        }
    }

}
