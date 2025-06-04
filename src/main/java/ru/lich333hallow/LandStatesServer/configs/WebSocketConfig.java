package ru.lich333hallow.LandStatesServer.configs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import ru.lich333hallow.LandStatesServer.handlers.GameHandler;
import ru.lich333hallow.LandStatesServer.handlers.LobbyHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        registry.addHandler(new LobbyHandler(objectMapper), "/ws/lobby")
                .addHandler(new GameHandler(objectMapper), "/ws/game")
                .setAllowedOrigins("*");
    }
}
