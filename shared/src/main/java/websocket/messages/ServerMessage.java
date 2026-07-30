package websocket.messages;

import model.GameData;

import java.util.Objects;

/**
 * Represents a message the server can send through a WebSocket.
 * <p>
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {

    private final ServerMessageType serverMessageType;
    private final GameData game;
    private final String errorMessage;
    private final String message;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this(type, null, null, null);
    }

    public ServerMessage(
            ServerMessageType type,
            GameData game,
            String errorMessage,
            String message
    ) {
        this.serverMessageType = type;
        this.game = game;
        this.errorMessage = errorMessage;
        this.message = message;
    }

    public ServerMessageType getServerMessageType() {
        return serverMessageType;
    }

    public GameData getGame() {
        return game;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public static ServerMessage loadGame(GameData game) {
        return new ServerMessage(
                ServerMessageType.LOAD_GAME,
                game,
                null,
                null
        );
    }

    public static ServerMessage error(String errorMessage) {
        return new ServerMessage(
                ServerMessageType.ERROR,
                null,
                errorMessage,
                null
        );
    }

    public static ServerMessage notification(String message) {
        return new ServerMessage(
                ServerMessageType.NOTIFICATION,
                null,
                null,
                message
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ServerMessage other)) {
            return false;
        }

        return serverMessageType == other.serverMessageType
                && Objects.equals(game, other.game)
                && Objects.equals(errorMessage, other.errorMessage)
                && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                serverMessageType,
                game,
                errorMessage,
                message
        );
    }
}