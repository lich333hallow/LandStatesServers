package ru.lich333hallow.LandStatesServer.exceptions;

public class LobbyExceptionAlreadyExists extends RuntimeException {
    public LobbyExceptionAlreadyExists(String message) {
        super(message);
    }
}
