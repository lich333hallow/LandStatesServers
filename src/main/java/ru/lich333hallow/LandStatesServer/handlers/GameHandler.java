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
    private final List<GameModel> games = new CopyOnWriteArrayList<>();
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
            Optional<GameModel> game = games.stream().filter(g -> g.getPlayers().stream().anyMatch(s -> s.getNumber() == gamer.getPlayer().getNumber())).findFirst();
            if(game.isPresent() && game.get().getPlayers().isEmpty()){
                games.removeIf(g -> g.getId().equals(game.get().getId()));
                ScheduledExecutorService service = lobbySchedulers.remove(game.get().getId());
                service.shutdown();
            }
        }

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

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();

        System.err.println("Transport error on session " + sessionId + ": " + exception.getMessage());

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
                    scheduler.shutdownNow();
                }
            }

            gamers.remove(gamer);
        }

        Optional<GameModel> game = games.stream().filter(g -> g.getPlayers().stream().anyMatch(s -> s.getNumber() == gamers.stream().filter(j -> j.getSessionId().equals(sessionId)).findFirst().get().getPlayer().getNumber())).findFirst();
        if(game.isPresent() && game.get().getPlayers().isEmpty()){
            games.removeIf(g -> g.getId().equals(game.get().getId()));
            ScheduledExecutorService service = lobbySchedulers.remove(game.get().getId());
            service.shutdown();
        }

        sessions.remove(session);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleNeutral(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<Gamer> gamer = gamers.stream().filter(g -> g.getSessionId().equals(session.getId())).findFirst();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        if (gamer.isEmpty() || gameModel.isEmpty()) return;

        PlayerModelInGame attacker = gamer.get().getPlayer();

        State state = message.getState();
        Optional<State> attack = attacker.getBases().stream().filter(s -> s.getId() == state.getSourceId()).findFirst();
        if (attack.isEmpty()) return;

        State attackState = attack.get();

        int k = attackState.getWarriors() - state.getWarriors();
        Random random = new Random();
        double chance = random.nextDouble();
        attackState.setWarriors(0);

        if ((k == 0 && chance <= 0.25) || (k > 0)) {
            attacker.getBases().add(generateNeutralState(attacker.getBases().stream().mapToInt(State::getId).max().orElse(0) + 1, k, state.getId()));
            broadcastGameState("CAPTURED_NEUTRAL", lobbyId, attacker.getName(), attacker.getName());
        } else {
            broadcastGameState("FAILED", lobbyId, attacker.getName(), attacker.getName());
        }
    }

    private State generateNeutralState(int id, int warriors, int sourceId){
        State neutral = new State();
        neutral.setId(id);
        neutral.setType(0);
        neutral.setFood(100);
        neutral.setPeasants(0);
        neutral.setMiners(0);
        neutral.setWarriors(warriors);
        neutral.setSourceId(sourceId);
        return neutral;
    }

    private void handleChangeStateType(WebSocketSession session, WebSocketMessageGame messageGame) throws IOException {
            String lobbyId = messageGame.getLobbyId();
            String playerName = messageGame.getPlayer().getName();
            int stateId = messageGame.getState().getId();
            int newType = messageGame.getState().getType();

            Optional<GameModel> gameModel = games.stream()
                    .filter(g -> g.getId().equals(lobbyId))
                    .findFirst();

            if (gameModel.isEmpty()) {
                session.sendMessage(new TextMessage(
                        String.format("{\"error\":\"Lobby %s not found\"}", lobbyId)
                ));
                return;
            }

            Optional<PlayerModelInGame> playerOpt = gameModel.get().getPlayers().stream()
                    .filter(p -> p.getName().equals(playerName))
                    .findFirst();

            if (playerOpt.isEmpty()) {
                session.sendMessage(new TextMessage(
                        String.format("{\"error\":\"Player %s not found\"}", playerName)
                ));
                return;
            }

            PlayerModelInGame player = playerOpt.get();
            Optional<State> stateOpt = player.getBases().stream()
                    .filter(s -> s.getId() == stateId)
                    .findFirst();

            if (stateOpt.isEmpty()) {
                session.sendMessage(new TextMessage(
                        String.format("{\"error\":\"State with id %d not found\"}", stateId)
                ));
                return;
            }

            State state = stateOpt.get();
            state.setType(newType);

            broadcastGameState("UPDATE_STATE", lobbyId, playerName, playerName);
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
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void updatePlayerState(String lobbyId, String playerName) throws IOException {
        if (!lobbySchedulers.containsKey(lobbyId)) {
            return;
        }
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
            int foodProduction = state.getPeasants() * 2;
            int foodConsumption = state.getWarriors() + state.getMiners() + state.getPeasants();
            int newFood = state.getFood() + foodProduction - foodConsumption;

            if (newFood < 0) {
                handleFoodDeficit(state, Math.abs(newFood));
                newFood = 0;
            }

            player.setBalance(player.getBalance() + state.getMiners());

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

        broadcastGameState("UPDATE", lobbyId, playerName, playerName);
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

            broadcastGameState("CREATE", message.getLobbyId(), playerName, playerName);
        } else {
            gameModel.get().getPlayers().add(message.getPlayer());

            schedulePlayerStateUpdates(lobbyId, playerName);
            broadcastGameState("JOIN", message.getLobbyId(), playerName, playerName);
        }
    }

    private void handleAttack(WebSocketSession session, WebSocketMessageGame message) throws IOException {
        String lobbyId = message.getLobbyId();
        Optional<GameModel> gameModel = games.stream().filter(g -> g.getId().equals(lobbyId)).findFirst();
        Optional<Gamer> dGamer = gamers.stream().filter(g -> g.getPlayer().getNumber() == message.getTarget().getNumber()).findFirst();
        Optional<Gamer> aGamer = gamers.stream().filter(g -> g.getPlayer().getNumber() == message.getPlayer().getNumber()).findFirst();
        if(dGamer.isEmpty() || gameModel.isEmpty() || aGamer.isEmpty()) return;

        PlayerModelInGame attacker = aGamer.get().getPlayer();
        PlayerModelInGame defender = dGamer.get().getPlayer();
        State defenderState = message.getState();
        Optional<State> attackState = attacker.getBases().stream().filter(s -> s.getId() == defenderState.getSourceId()).findFirst();
        if (attackState.isEmpty()) return;

        State attackerState = attackState.get();

        int k = defenderState.getWarriors() - attackerState.getWarriors();
        attackerState.setWarriors(0);
        Random random = new Random();
        double chance = random.nextDouble();

        if(k > 0 || (k == 0 && chance <= 0.75)){
            defenderState.setWarriors(k);
            defenderState.setSourceId(0);
            broadcastGameState("FAILED", lobbyId, attacker.getName(), defender.getName());
        } else {
            defender.getBases().removeIf(s -> s.getId() == defenderState.getId());
            defenderState.setWarriors(Math.abs(k));
            defenderState.setSourceId(defenderState.getId());
            defenderState.setId(attacker.getBases().stream().mapToInt(State::getId).max().orElse(0) + 1);
            attacker.getBases().add(defenderState);

            broadcastGameState("CAPTURED", lobbyId, attacker.getName(), defender.getName());
        }
    }

    private void scheduleGameTimer(String lobbyId, int gameTimeInSeconds) {
        if (lobbySchedulers.containsKey(lobbyId)) {
            return;
        }

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
        if (!lobbySchedulers.containsKey(lobbyId)) {
            return;
        }
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
                            schedulers.forEach(ExecutorService::shutdownNow);
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

    private void broadcastGameState(String type, String lobbyId, String playerName, String target) throws IOException {
        Optional<GameModel> gameModel = games.stream()
                .filter(g -> g.getId().equals(lobbyId))
                .findFirst();

        if (gameModel.isPresent()) {
            Optional<PlayerModelInGame> playerOpt = gameModel.get().getPlayers().stream()
                    .filter(p -> p.getName().equals(playerName))
                    .findFirst();
            Optional<PlayerModelInGame> targetOpt = target.equals(playerName)
                    ? playerOpt
                    : gameModel.get().getPlayers().stream()
                    .filter(p -> p.getName().equals(target))
                    .findFirst();

            if (playerOpt.isPresent()) {
                PlayerModelInGame player = playerOpt.get();
                GameState gameState = new GameState(type, lobbyId, player, targetOpt.orElse(player));
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