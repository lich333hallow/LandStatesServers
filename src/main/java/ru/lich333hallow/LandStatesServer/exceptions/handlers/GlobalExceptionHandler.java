package ru.lich333hallow.LandStatesServer.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.lich333hallow.LandStatesServer.exceptions.LobbyExceptionAlreadyExists;
import ru.lich333hallow.LandStatesServer.exceptions.LobbyExceptionNotFound;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(LobbyExceptionNotFound.class)
    public ResponseEntity<String> handlerLobbyExceptionNotFound(LobbyExceptionNotFound e){
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(LobbyExceptionAlreadyExists.class)
    public ResponseEntity<String> handlerLobbyExceptionAlreadyExists(LobbyExceptionAlreadyExists e){
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
