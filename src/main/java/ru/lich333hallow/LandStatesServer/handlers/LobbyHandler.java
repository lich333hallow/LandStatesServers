package ru.lich333hallow.LandStatesServer.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.lich333hallow.LandStatesServer.models.LobbyModel;
import ru.lich333hallow.LandStatesServer.models.PlayerModel;
import ru.lich333hallow.LandStatesServer.states.LobbyState;
import ru.lich333hallow.LandStatesServer.models.WebSocketMessageLobby;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbyHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final List<LobbyModel> lobbies = new ArrayList<>();
    private final ObjectMapper objectMapper;

    public LobbyHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sendLobbyState(session, "LOBBY_STATE");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        sendLobbyState(session, "LOBBY_STATE");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebSocketMessageLobby wsMessage;


        try {
            wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessageLobby.class);
        } catch (IOException e) {
            session.sendMessage(new TextMessage("{\"error\":\"Invalid JSON format\"}"));
            return;
        }

        if (wsMessage.getType() == null) {
            session.sendMessage(new TextMessage("{\"error\":\"Message type is required\"}"));
            return;
        }

        try {
            switch (wsMessage.getType()) {
                case "JOIN":
                    handleJoinMessage(session, wsMessage, false);
                    break;

                case "READY":
                    handleReadyMessage(session, wsMessage);
                    break;

                case "CREATE":
                    handleJoinMessage(session, wsMessage, true);
                    break;

                case "DISCONNECTED":
                    handleDisconnectedMessage(session, wsMessage);
                    break;

                default:
                    session.sendMessage(new TextMessage(
                            String.format("{\"error\":\"Unknown message type: %s\"}", wsMessage.getType())
                    ));
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage(
                    String.format("{\"error\":\"Processing error: %s\"}", e.getMessage())
            ));
            e.printStackTrace();
        }
    }

    private void handleJoinMessage(WebSocketSession session, WebSocketMessageLobby message, boolean create) throws IOException {
        if (create){
            lobbies.add(new LobbyModel(message.getLobbyId(), Integer.parseInt(message.getNumberOfPlayers()), new ArrayList<>()));
        }

        PlayerModel player = new PlayerModel(session.getId(), message.getPlayerName(), message.getColor());
        player.setName(message.getPlayerName());
        lobbies.stream().filter(l -> l.getLobbyId().equals(message.getLobbyId())).findFirst().ifPresent(l -> l.getPlayers().add(player));
        broadcastLobbyState("JOIN", message.getLobbyId());
    }

    private void handleReadyMessage(WebSocketSession session, WebSocketMessageLobby message) throws IOException {
        lobbies.stream()
                .filter(l -> l.getLobbyId().equals(message.getLobbyId()))
                .findFirst().flatMap(l -> l.getPlayers().stream()
                        .filter(p -> p.getName().equals(message.getPlayerName()))
                        .findFirst()).ifPresent(p -> {
                    p.setReady(message.getReady());
                    try {
                        checkAllReady();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        broadcastLobbyState("LOBBY_STATE", message.getLobbyId());
    }

    private void handleDisconnectedMessage(WebSocketSession session, WebSocketMessageLobby message) throws IOException {
        lobbies.stream()
                .filter(l -> l.getLobbyId().equals(message.getLobbyId()))
                .findFirst()
                .ifPresent(lobby -> {
                    List<PlayerModel> playersToRemove = lobby.getPlayers().stream()
                            .filter(player -> message.getPlayerName().equals(player.getName()))
                            .toList();

                    lobby.getPlayers().removeAll(playersToRemove);

                    if (lobby.getPlayers().isEmpty()) {
                        lobbies.remove(lobby);
                    }
                    try {
                        broadcastLobbyState("DISCONNECTED", lobby.getLobbyId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    private void checkAllReady() throws IOException {
        for (LobbyModel lobby : lobbies) {
            if (lobby.getPlayers().size() == lobby.getNumberOfPlayers() &&
                    lobby.getPlayers().stream().allMatch(PlayerModel::isReady)) {

                String startGameMessage = "{\"type\":\"START_GAME\"}";

                for (PlayerModel player : lobby.getPlayers()) {
                    sessions.stream()
                            .filter(s -> s.getId().equals(player.getSessionId()))
                            .findFirst()
                            .ifPresent(s -> {
                                try {
                                    s.sendMessage(new TextMessage(startGameMessage));
                                } catch (IOException e) {
                                    System.err.println("Error sending START_GAME to session " + s.getId());
                                }
                            });
                }
            }
        }
    }

    private void sendLobbyState(WebSocketSession session, String type) throws IOException {
        try {
            LobbyModel targetLobby = lobbies.stream()
                    .filter(l -> l.getPlayers().stream()
                            .anyMatch(p -> p.getSessionId().equals(session.getId())))
                    .findFirst()
                    .orElse(null);

            if (targetLobby != null) {
                String lobbyState = objectMapper.writeValueAsString(
                        new LobbyState(type, targetLobby.getLobbyId(), targetLobby.getPlayers())
                );
                session.sendMessage(new TextMessage(lobbyState));
            }
        } catch (Exception e) {
            System.err.println("Error sending lobby state to session " + session.getId());
            throw e;
        }
    }

    private void broadcastLobbyState(String type, String id) throws IOException {
        for (LobbyModel lobby : lobbies) {
            if(!lobby.getLobbyId().equals(id)){
                continue;
            }
            String lobbyState = objectMapper.writeValueAsString(
                    new LobbyState(type, lobby.getLobbyId(), lobby.getPlayers())
            );

            for (PlayerModel player : lobby.getPlayers()) {
                sessions.stream()
                        .filter(s -> s.getId().equals(player.getSessionId()))
                        .findFirst()
                        .ifPresent(s -> {
                            try {
                                s.sendMessage(new TextMessage(lobbyState));
                            } catch (IOException e) {
                                System.err.println("Error broadcasting to session " + s.getId());
                            }
                        });
            }
        }
    }
}
