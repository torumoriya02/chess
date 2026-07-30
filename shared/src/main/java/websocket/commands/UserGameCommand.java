package websocket.commands;

import chess.ChessMove;

import java.util.Objects;

/**
 * Represents a command a user can send the server over a websocket.
 * <p>
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class UserGameCommand {

    private final CommandType commandType;
    private final String authToken;
    private final Integer gameID;
    private final ChessMove move;

    public UserGameCommand(
            CommandType commandType,
            String authToken,
            Integer gameID
    ) {
        this(commandType, authToken, gameID, null);
    }

    public UserGameCommand(
            CommandType commandType,
            String authToken,
            Integer gameID,
            ChessMove move
    ) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.move = move;
    }

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Integer getGameID() {
        return gameID;
    }

    public ChessMove getMove() {
        return move;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof UserGameCommand other)) {
            return false;
        }

        return commandType == other.commandType
                && Objects.equals(authToken, other.authToken)
                && Objects.equals(gameID, other.gameID)
                && Objects.equals(move, other.move);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                commandType,
                authToken,
                gameID,
                move
        );
    }
}