package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

class WebSocketSupport {

    private final DataAccess dataAccess;
    private final ConnectionManager connectionManager;
    private final Gson gson;

    WebSocketSupport(
            DataAccess dataAccess,
            ConnectionManager connectionManager,
            Gson gson
    ) {
        this.dataAccess = dataAccess;
        this.connectionManager = connectionManager;
        this.gson = gson;
    }

    AuthData getValidAuth(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {
        if (command.getAuthToken() == null) {
            sendError(ctx, "Error: unauthorized");
            return null;
        }

        AuthData auth = dataAccess.getAuth(
                command.getAuthToken()
        );

        if (auth == null) {
            sendError(ctx, "Error: unauthorized");
        }

        return auth;
    }

    GameData getValidGame(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {
        if (command.getGameID() == null) {
            sendError(
                    ctx,
                    "Error: game ID is required"
            );
            return null;
        }

        GameData gameData = dataAccess.getGame(
                command.getGameID()
        );

        if (gameData == null) {
            sendError(
                    ctx,
                    "Error: game not found"
            );
        }

        return gameData;
    }

    boolean validateGameForMove(
            WsContext ctx,
            ChessGame game
    ) {
        if (game == null) {
            sendError(
                    ctx,
                    "Error: game data is missing"
            );
            return false;
        }

        if (game.isGameOver()) {
            sendError(
                    ctx,
                    "Error: game is over"
            );
            return false;
        }

        return true;
    }

    boolean validatePlayerTurn(
            WsContext ctx,
            ChessGame.TeamColor playerColor,
            ChessGame game
    ) {
        if (playerColor == null) {
            sendError(
                    ctx,
                    "Error: observers cannot make moves"
            );
            return false;
        }

        if (game.getTeamTurn() != playerColor) {
            sendError(
                    ctx,
                    "Error: it is not your turn"
            );
            return false;
        }

        return true;
    }

    String buildConnectNotification(
            AuthData auth,
            GameData gameData
    ) {
        String username = auth.username();

        ChessGame.TeamColor color = getPlayerColor(
                username,
                gameData
        );

        if (color == ChessGame.TeamColor.WHITE) {
            return username
                    + " joined the game as WHITE.";
        }

        if (color == ChessGame.TeamColor.BLACK) {
            return username
                    + " joined the game as BLACK.";
        }

        return username
                + " joined the game as an observer.";
    }

    boolean isPlayer(
            String username,
            GameData gameData
    ) {
        return getPlayerColor(
                username,
                gameData
        ) != null;
    }

    ChessGame.TeamColor getPlayerColor(
            String username,
            GameData gameData
    ) {
        if (username.equals(gameData.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        }

        if (username.equals(gameData.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }

        return null;
    }

    void sendGameStatusNotifications(
            int gameID,
            GameData gameData
    ) throws Exception {
        ChessGame game = gameData.game();

        if (game == null) {
            return;
        }

        ChessGame.TeamColor teamToMove =
                game.getTeamTurn();

        String username = getUsernameForColor(
                teamToMove,
                gameData
        );

        if (game.isInCheckmate(teamToMove)) {
            finishGame(
                    gameID,
                    gameData,
                    username + " is in checkmate."
            );
            return;
        }

        if (game.isInStalemate(teamToMove)) {
            finishGame(
                    gameID,
                    gameData,
                    "The game is in stalemate."
            );
            return;
        }

        if (game.isInCheck(teamToMove)) {
            broadcast(
                    gameID,
                    ServerMessage.notification(
                            username + " is in check."
                    ),
                    null
            );
        }
    }

    private void finishGame(
            int gameID,
            GameData gameData,
            String notification
    ) throws Exception {
        gameData.game().setGameOver(true);
        dataAccess.updateGame(gameData);

        broadcast(
                gameID,
                ServerMessage.notification(notification),
                null
        );
    }

    private String getUsernameForColor(
            ChessGame.TeamColor color,
            GameData gameData
    ) {
        if (color == ChessGame.TeamColor.WHITE) {
            return gameData.whiteUsername() == null
                    ? "White player"
                    : gameData.whiteUsername();
        }

        return gameData.blackUsername() == null
                ? "Black player"
                : gameData.blackUsername();
    }

    void addConnection(
            int gameID,
            WsContext ctx
    ) {
        connectionManager.add(gameID, ctx);
    }

    void removeConnection(
            int gameID,
            WsContext ctx
    ) {
        connectionManager.remove(gameID, ctx);
    }

    void broadcast(
            int gameID,
            ServerMessage message,
            WsContext excludedContext
    ) {
        String json = gson.toJson(message);

        for (WsContext connection
                : connectionManager.getConnections(gameID)) {

            if (!isExcluded(
                    connection,
                    excludedContext
            )) {
                connection.send(json);
            }
        }
    }

    private boolean isExcluded(
            WsContext connection,
            WsContext excludedContext
    ) {
        return excludedContext != null
                && connection.sessionId().equals(
                excludedContext.sessionId()
        );
    }

    void sendMessage(
            WsContext ctx,
            ServerMessage message
    ) {
        ctx.send(gson.toJson(message));
    }

    void sendError(
            WsContext ctx,
            String message
    ) {
        sendMessage(
                ctx,
                ServerMessage.error(message)
        );
    }
}