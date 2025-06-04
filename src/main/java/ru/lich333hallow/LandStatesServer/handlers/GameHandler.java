package ru.lich333hallow.LandStatesServer.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.lich333hallow.LandStatesServer.models.*;
import ru.lich333hallow.LandStatesServer.states.GameState;
import ru.lich333hallow.LandStatesServer.states.WinnerState;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class GameHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final List<GameModel> games = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final List<Gamer> gamers = new ArrayList<>();
    private final Map<String, List<ScheduledExecutorService>> playerSchedulers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> lobbySchedulers = new ConcurrentHashMap<>();
    private final Map<String, Queue<TextMessage>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isSending = new ConcurrentHashMap<>();

    public GameHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();

        messageQueues.remove(sessionId);
        isSending.remove(sessionId);

        Optional<Gamer> gamerOpt = gamers.stream()
                .filter(g -> g.getSessionId().equals(sessionId))
                .findFirst();

        if (gamerOpt.isPresent()) {
            Gamer gamer = gamerOpt.get();
            String playerName = gamer.getPlayer().getName();

            List<ScheduledExecutorService> schedulers = playerSchedulers.remove(playerName);
            if (schedulers != null) {
                for (ScheduledExecutorService scheduler : schedulers) {
                    scheduler.shutdown();
                }
            }
        }

        games.removeIf(g -> g.getPlayers().isEmpty());
        gamers.removeIf(g -> g.getSessionId().equals(sessionId));
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebSocketMessageGame wsMessage;

        try {
            wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessageGame.class);
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
                case "ATTACK":
                    handleAttack(session, wsMessage);
                    break;
                case "NEUTRAL":
                    handleNeutral(session, wsMessage);
                    break;
                case "CHANGE_TYPE":
                    handleChangeStateType(session, wsMessage);
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

        }
    }

    private void handleChangeStateType(WebSocketSession session, WebSocketMessageGame messageGame) throws IOException {
        String lobbyId = messageGame.getLobbyId();
        String playerName = messageGame.getPlayer().getName();
        int stateId = messageGame.getState().getId();
        int newType = messageGame.getState().getType();

        Optional<GameModel> gameModel = games.stream()
                .filter(g -> g.getId().equals(lobbyId))
                .findFirst();

        if (gameModel.isPresent()) {
            Optional<PlayerModelInGame> playerOpt = gameModel.get().getPlayers().stream()
                    .filter(p -> p.getName().equals(playerName))
                    .findFirst();

            if (playerOpt.isPresent()) {
                PlayerModelInGame player = playerOpt.get();
                Optional<State> stateOpt = player.getBases().stream()
                        .filter(s -> s.getId() == stateId)
                        .findFirst();

                if (stateOpt.isPresent()) {
                    State state = stateOpt.get();
                    state.setType(newType);
                    broadcastGameState("UPDATE", lobbyId, playerName);
                } else {
                    session.sendMessage(new TextMessage(
                            String.format("{\"error\":\"State with id %d not found\"}", stateId)
                    ));
                }
            } else {
                session.sendMessage(new TextMessage(
                        String.format("{\"error\":\"Player %s not found\"}", playerName)
                ));
            }
        } else {
            session.sendMessage(new TextMessage(
                    String.format("{\"error\":\"Lobby %s not found\"}", lobbyId)
            ));
        }
    }

    private void handleFoodDeficit(State state, int deficit) {
        int minersToReduce = Math.min(state.getMiners(), deficit);
        state.setMiners(state.getMiners() - minersToReduce);
        deficit -= minersToReduce;

        if (deficit > 0) {
            int warriorsToReduce = Math.min(state.getWarriors(), deficit);
            state.setWarriors(state.getWarriors() - warriorsToReduce);
            deficit -= warriorsToReduce;
        }

        if (deficit > 0) {
            int peasantsToReduce = Math.min(state.getPeasants(), deficit);
            state.setPeasants(state.getPeasants() - peasantsToReduce);
        }
    }

    private void schedulePlayerStateUpdates(String lobbyId, String playerName) {
        List<ScheduledExecutorService> schedulers = playerSchedulers.computeIfAbsent(playerName, k -> new ArrayList<>());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulers.add(scheduler);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePlayerState(lobbyId, playerName);
            } catch (Exception e) {
                System.err.println("Error updating player state for player " + playerName + ": " + e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    private void updatePlayerState(String lobbyId, String playerName) throws IOException {
        Optional<Gamer> gamerOpt = gamers.stream()
                .filter(g -> {
                    boolean nameMatches = g.getPlayer().getName().equals(playerName);
                    if (!nameMatches) return false;
                    return games.stream()
                            .filter(game -> game.getId().equals(lobbyId))
                            .anyMatch(game -> game.getPlayers().contains(g.getPlayer()));
                })
                .findFirst();

        if (gamerOpt.isEmpty()) return;
        Gamer gamer = gamerOpt.get();
        PlayerModelInGame player = gamer.getPlayer();
        String sessionId = gamer.getSessionId();

        boolean sessionActive = sessions.stream()
                .anyMatch(s -> s.getId().equals(sessionId) && s.isOpen());

        if (!sessionActive) return;

        for (State state : player.getBases()) {
            int foodProduction = (int) (state.getPeasants() * 1.15);
            int foodConsumption = state.getWarriors() + state.getMiners() + state.getPeasants();
            int newFood = state.getFood() + foodProduction - foodConsumption;

            if (newFood < 0) {
                handleFoodDeficit(state, Math.abs(newFood));
                newFood = 0;
            }

            state.setFood(newFood);

            switch (state.getType()) {
                case 0:
                    state.setPeasants(state.getPeasants() + 1);
                    break;
                case 1:
                    state.setWarriors(state.getWarriors() + 1);
                    break;
                case 2:
                    state.setMiners(state.getMiners() + 1);
                    break;
            }
        }

        broadcastGameState("UPDATE", lobbyId, playerName);
    }

    private void handleJoinMessage(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        String playerName = message.getPlayer().getName();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        Gamer gamer = new Gamer();
        gamer.setSessionId(session.getId());
        gamer.setPlayer(message.getPlayer());
        gamers.add(gamer);

        if (gameModel.isEmpty()) {
            GameModel gameModel1 = new GameModel();
            gameModel1.setId(message.getLobbyId());
            List<PlayerModelInGame> playerModelInGames = new ArrayList<>();
            playerModelInGames.add(message.getPlayer());
            gameModel1.setPlayers(playerModelInGames);
            gameModel1.setGameTime(message.getGameTime());
            games.add(gameModel1);

            scheduleGameTimer(lobbyId, gameModel1.getGameTime());
            schedulePlayerStateUpdates(lobbyId, playerName);

            broadcastGameState("CREATE", message.getLobbyId(), playerName);
        } else {
            List<PlayerModelInGame> playerModelInGames = new ArrayList<>(gameModel.get().getPlayers());
            playerModelInGames.add(message.getPlayer());
            gameModel.get().setPlayers(playerModelInGames);

            schedulePlayerStateUpdates(lobbyId, playerName);
            broadcastGameState("JOIN", message.getLobbyId(), playerName);
        }
    }


    private void handleAttack(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        if (gameModel.isPresent()) {

        }
    }

    private void scheduleGameTimer(String lobbyId, int gameTimeInSeconds) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        lobbySchedulers.put(lobbyId, scheduler);

        scheduler.schedule(() -> {
            handleFinishGame(lobbyId);
            ScheduledExecutorService removedScheduler = lobbySchedulers.remove(lobbyId);
            if (removedScheduler != null) {
                removedScheduler.shutdown();
            }
        }, gameTimeInSeconds, TimeUnit.SECONDS);
    }

    private void handleFinishGame(String lobbyId) {
        games.stream()
                .filter(g -> g.getId().equals(lobbyId))
                .findFirst()
                .ifPresent(game -> {
                    List<PlayerModelInGame> players = game.getPlayers();
                    int maxBalance = players.stream()
                            .mapToInt(PlayerModelInGame::getBalance)
                            .max()
                            .orElse(0);
                    List<PlayerModelInGame> winners = players.stream()
                            .filter(p -> p.getBalance() == maxBalance && !p.getBases().isEmpty())
                            .toList();
                    broadCastWinner(lobbyId, winners);
                    ScheduledExecutorService lobbyScheduler = lobbySchedulers.remove(lobbyId);
                    if (lobbyScheduler != null) {
                        lobbyScheduler.shutdownNow();
                    }

                    players.forEach(player -> {
                        List<ScheduledExecutorService> schedulers = playerSchedulers.remove(player.getName());
                        if (schedulers != null) {
                            schedulers.forEach(scheduler -> scheduler.shutdownNow());
                        }
                        gamers.stream()
                                .filter(g -> g.getPlayer().getName().equals(player.getName()))
                                .findFirst()
                                .ifPresent(gamer -> {
                                    messageQueues.remove(gamer.getSessionId());
                                    isSending.remove(gamer.getSessionId());
                                });
                    });
                    games.remove(game);
                    players.forEach(player -> {
                        gamers.stream()
                                .filter(g -> g.getPlayer().getName().equals(player.getName()))
                                .findFirst()
                                .ifPresent(gamer -> {
                                    try {
                                        if (sessions.stream().anyMatch(s -> s.getId().equals(gamer.getSessionId()) && s.isOpen())) {
                                            sendMessageToSession(gamer.getSessionId(),
                                                    new TextMessage("{\"type\":\"GAME_ENDED\",\"message\":\"Game session has ended\"}"));
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error sending game end notification: " + e.getMessage());
                                    }
                                });
                    });
                });
    }

    private void checkQueue(String sessionId) {
        synchronized (isSending) {
            Queue<TextMessage> queue = messageQueues.get(sessionId);
            if (queue != null && !queue.isEmpty()) {
                TextMessage nextMessage = queue.poll();
                if (nextMessage != null) {
                    sessions.stream()
                            .filter(s -> s.getId().equals(sessionId))
                            .findFirst()
                            .ifPresent(session -> {
                                try {
                                    if (session.isOpen()) {
                                        session.sendMessage(nextMessage);
                                    }
                                } catch (IOException e) {
                                    System.err.println("Error sending queued message to session " + sessionId + ": " + e.getMessage());
                                } finally {
                                    checkQueue(sessionId);
                                }
                            });
                    return;
                }
            }
            isSending.put(sessionId, false);
        }
    }

    private void broadCastWinner(String lobbyId, List<PlayerModelInGame> winners) {
        games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst().ifPresent(g -> {
            try {
                String mess = objectMapper.writeValueAsString(new WinnerState("FINISH_GAME", winners));
                for (PlayerModelInGame player : g.getPlayers()){
                    Gamer gamer = gamers.stream().filter(gamer1 -> gamer1.getPlayer().getName().equals(player.getName())).findFirst().get();
                    sessions.stream()
                            .filter(s -> s.getId().equals(gamer.getSessionId()))
                            .findFirst()
                            .ifPresent(s -> {
                                try {
                                    s.sendMessage(new TextMessage(mess));
                                } catch (IOException e) {
                                    System.err.println("Error sending finish game mess to " + s.getId());
                                }
                            });
                }
            } catch (IOException e){
                System.err.println("Error sending finish game mess: " + e.getMessage());
            }
        });
    }

    private void broadcastGameState(String type, String lobbyId, String playerName) throws IOException {
        Optional<GameModel> gameModel = games.stream()
                .filter(g -> g.getId().equals(lobbyId))
                .findFirst();

        if (gameModel.isPresent()) {
            Optional<PlayerModelInGame> playerOpt = gameModel.get().getPlayers().stream()
                    .filter(p -> p.getName().equals(playerName))
                    .findFirst();

            if (playerOpt.isPresent()) {
                PlayerModelInGame player = playerOpt.get();
                GameState gameState = new GameState(type, lobbyId, player);
                String wsMess = objectMapper.writeValueAsString(gameState);
                TextMessage message = new TextMessage(wsMess);
                sendMessageToLobby(lobbyId, message);
            }
        }
    }

    private void sendMessageToSession(String sessionId, TextMessage message) {
        synchronized (isSending) {
            if (isSending.getOrDefault(sessionId, false)) {
                messageQueues.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>()).add(message);
                return;
            }

            isSending.put(sessionId, true);
        }

        sessions.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .ifPresent(session -> {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(message);
                        }
                    } catch (IOException e) {
                        System.err.println("Error sending message to session " + sessionId + ": " + e.getMessage());
                    } finally {
                        checkQueue(sessionId);
                    }
                });
    }

    private void sendMessageToLobby(String lobbyId, TextMessage message) {
        Optional<GameModel> lobbyOpt = games.stream()
                .filter(g -> g.getId().equals(lobbyId))
                .findFirst();

        if (lobbyOpt.isEmpty()) return;
        for (PlayerModelInGame player : lobbyOpt.get().getPlayers()) {
            Optional<Gamer> gamerOpt = gamers.stream()
                    .filter(g -> g.getPlayer().getName().equals(player.getName()))
                    .findFirst();

            if (gamerOpt.isPresent()) {
                String sessionId = gamerOpt.get().getSessionId();
                sendMessageToSession(sessionId, message);
            }
        }
    }

}