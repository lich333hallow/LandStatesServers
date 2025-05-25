package ru.lich333hallow.LandStatesServer.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.lang.NonNullApi;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.lich333hallow.LandStatesServer.models.*;
import ru.lich333hallow.LandStatesServer.states.GameState;
import ru.lich333hallow.LandStatesServer.states.LobbyState;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GameHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final List<GameModel> games = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final Map<String, ScheduledExecutorService> playerSchedulers = new ConcurrentHashMap<>();

    public GameHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        games.forEach(g -> {
            g.getPlayers().removeIf(p -> {
                if (p.getSessionId().equals(session.getId())) {
                    stopPlayerScheduler(p.getSessionId());
                    return true;
                }
                return false;
            });
        });

        games.removeIf(g -> g.getPlayers().isEmpty());
        sessions.remove(session);
    }

    private void stopPlayerScheduler(String sessionId) {
        ScheduledExecutorService scheduler = playerSchedulers.remove(sessionId);
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void startPlayerScheduler(PlayerModelInGame player, String gameId) {
        stopPlayerScheduler(player.getSessionId());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        playerSchedulers.put(player.getSessionId(), scheduler);

        scheduler.scheduleAtFixedRate(() -> {
            increasePlayerWarriors(player, gameId);
        }, 1, 3, TimeUnit.SECONDS);
    }

    private void increasePlayerWarriors(PlayerModelInGame player, String gameId) {
        int newWarriors = player.getWarriors() + player.getBases();
        player.setWarriors(newWarriors);

        try {
            broadcastGameState("WARRIORS_INCREASED", gameId, player.getName());
        } catch (IOException e) {
            System.err.println("Error broadcasting warriors increase for player " + player.getName() + ": " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebSocketMessageGame wsMessage;

        try {
            wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessageGame.class);
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
                    handleJoinMessage(session, wsMessage);
                    break;
                case "FINISH_GAME":
                    handleFinishGame(session, wsMessage);
                    break;
                case "ATTACK":
                    handleAttack(session, wsMessage);
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
        }
    }

    private void handleNeutral(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        if(gameModel.isPresent()){
            GameModel gameModel1 = gameModel.get();
            NeutralBaseModel neutralBaseModel = new NeutralBaseModel(20);
            PlayerModelInGame player = gameModel1.getPlayers().stream()
                    .filter(p -> p.getName().equals(message.getName()))
                    .findFirst().get();

            System.out.println(player.getWarriors());
            System.out.println(neutralBaseModel.getWarriors());
            System.out.println(player.getWarriors() - neutralBaseModel.getWarriors() > 0);

            if(player.getWarriors() - neutralBaseModel.getWarriors() > 0){
                player.setWarriors(player.getWarriors() - neutralBaseModel.getWarriors());
                player.setBases(player.getBases() + 1);

                System.out.println(player);

                broadcastGameState("CAPTURED", lobbyId, player.getName());
            }
            broadcastGameState("FAILED", lobbyId, player.getName());
        }
    }

    private void handleJoinMessage(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();

        if (gameModel.isEmpty()) {
            GameModel gameModel1 = new GameModel();
            gameModel1.setId(lobbyId);
            List<PlayerModelInGame> playerModelInGames = new ArrayList<>();

            PlayerModelInGame newPlayer = new PlayerModelInGame(
                    session.getId(),
                    message.getName(),
                    "1",
                    Integer.parseInt(message.getBalance()),
                    Integer.parseInt(message.getMiners()),
                    Integer.parseInt(message.getDefenders()),
                    Integer.parseInt(message.getBases()),
                    100
            );

            playerModelInGames.add(newPlayer);
            gameModel1.setPlayers(playerModelInGames);
            games.add(gameModel1);

            startPlayerScheduler(newPlayer, lobbyId);
        } else {
            GameModel gameModel1 = gameModel.get();
            List<PlayerModelInGame> playerModelInGames = gameModel1.getPlayers();
            int number = playerModelInGames.size() + 1;

            PlayerModelInGame newPlayer = new PlayerModelInGame(
                    session.getId(),
                    message.getName(),
                    String.valueOf(number),
                    Integer.parseInt(message.getBalance()),
                    Integer.parseInt(message.getMiners()),
                    Integer.parseInt(message.getDefenders()),
                    Integer.parseInt(message.getBases()),
                    100
            );

            playerModelInGames.add(newPlayer);
            gameModel1.setPlayers(playerModelInGames);

            startPlayerScheduler(newPlayer, lobbyId);
        }

        System.out.println(games);
        broadcastGameState(message.getType(), message.getLobbyId(), message.getName());
    }

    private void handleAttack(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        if (gameModel.isPresent() && !message.getTarget().equals("none")) {
            if(message.getTarget().equals("neutral")){
                handleNeutral(session, message);
                return;
            }
            GameModel gameModel1 = gameModel.get();
            PlayerModelInGame player = gameModel1.getPlayers().stream()
                    .filter(p -> p.getName().equals(message.getName()))
                    .findFirst().get();
            PlayerModelInGame target = gameModel1.getPlayers().get(Integer.parseInt(message.getTarget()) - 1);

            if (player.getWarriors() - target.getWarriors() > 0) {
                target.setBases(target.getBases() - 1);
                target.setWarriors(0);
                player.setWarriors(player.getWarriors() - target.getWarriors());
                player.setBases(player.getBases() + 1);
                broadcastGameState("CAPTURED", lobbyId, player.getName());
            } else {
                target.setWarriors(target.getWarriors() - player.getWarriors() == 0 ? 1 : target.getWarriors() - player.getWarriors());
                player.setWarriors(1);
                broadcastGameState("FAILED", lobbyId, player.getName());
            }
        }
    }

    private void handleFinishGame(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        if (gameModel.isPresent()) {
            String playerName = gameModel.get().getPlayers().stream()
                    .max(Comparator.comparingInt(PlayerModelInGame::getBases))
                    .get().getName();
            broadcastGameState("WINNER", lobbyId, playerName);
        }
    }

    private void broadcastGameState(String type, String id, String playerName) throws IOException {
        for (GameModel gameModel : games) {
            if (!gameModel.getId().equals(id)) {
                continue;
            }
            String gameState = objectMapper.writeValueAsString(new GameState(
                    type,
                    gameModel.getId(),
                    gameModel.getPlayers().stream()
                            .filter(p -> p.getName().equals(playerName))
                            .findFirst().get(),
                    gameModel.getPlayers())
            );
            if(!type.equals("WARRIORS_INCREASED"))  System.out.println(gameState);

            for (PlayerModelInGame player : gameModel.getPlayers()) {
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