package websocket;

import chess.ChessGame;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

public class WebSocketHandler {

    private final DataAccess dataAccess;
    private final Gson gson = new Gson();
    private final ConnectionManager connectionManager =
            new ConnectionManager();

    public WebSocketHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void onMessage(WsContext ctx, String message) {
        try {
            UserGameCommand command =
                    gson.fromJson(message, UserGameCommand.class);

            if (command == null || command.getCommandType() == null) {
                sendError(ctx, "Error: invalid command");
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                case MAKE_MOVE -> handleMakeMove(ctx, command);
                case LEAVE -> handleLeave(ctx, command);
                case RESIGN -> handleResign(ctx, command);
            }

        } catch (Exception ex) {
            sendError(ctx, "Error: " + ex.getMessage());
        }
    }

    private void handleConnect(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        connectionManager.add(command.getGameID(), ctx);

        ctx.send(
                gson.toJson(
                        ServerMessage.loadGame(gameData)
                )
        );

        ServerMessage notification =
                ServerMessage.notification(
                        buildConnectNotification(auth, gameData)
                );

        broadcast(
                command.getGameID(),
                notification,
                ctx
        );
    }

    private void handleMakeMove(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        if (command.getMove() == null) {
            sendError(ctx, "Error: move is required");
            return;
        }

        ChessGame game = gameData.game();

        if (game.isGameOver()) {
            sendError(ctx, "Error: game is over");
            return;
        }

        String username = auth.username();

        ChessGame.TeamColor playerColor =
                getPlayerColor(username, gameData);

        if (playerColor == null) {
            sendError(ctx, "Error: observers cannot make moves");
            return;
        }

        if (game.getTeamTurn() != playerColor) {
            sendError(ctx, "Error: it is not your turn");
            return;
        }

        try {
            game.makeMove(command.getMove());
        } catch (InvalidMoveException ex) {
            sendError(ctx, "Error: invalid move");
            return;
        }

        GameData updatedGame = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );

        dataAccess.updateGame(updatedGame);

        broadcast(
                command.getGameID(),
                ServerMessage.loadGame(updatedGame),
                null
        );

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        username
                                + " moved "
                                + command.getMove().getStartPosition()
                                + " to "
                                + command.getMove().getEndPosition()
                ),
                ctx
        );

        sendGameStatusNotifications(
                command.getGameID(),
                updatedGame
        );
    }

    private void handleLeave(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        String username = auth.username();

        String whiteUsername = gameData.whiteUsername();
        String blackUsername = gameData.blackUsername();

        if (username.equals(whiteUsername)) {
            whiteUsername = null;
        }

        if (username.equals(blackUsername)) {
            blackUsername = null;
        }

        GameData updatedGame = new GameData(
                gameData.gameID(),
                whiteUsername,
                blackUsername,
                gameData.gameName(),
                gameData.game()
        );

        dataAccess.updateGame(updatedGame);

        connectionManager.remove(command.getGameID(), ctx);

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        username + " left the game."
                ),
                null
        );
    }

    private void handleResign(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        String username = auth.username();

        ChessGame.TeamColor playerColor =
                getPlayerColor(username, gameData);

        if (playerColor == null) {
            sendError(ctx, "Error: observers cannot resign");
            return;
        }

        ChessGame game = gameData.game();

        if (game.isGameOver()) {
            sendError(ctx, "Error: game is already over");
            return;
        }

        game.setGameOver(true);

        GameData updatedGame = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );

        dataAccess.updateGame(updatedGame);

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        username + " resigned the game."
                ),
                null
        );
    }

    private AuthData getValidAuth(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        if (command.getAuthToken() == null) {
            sendError(ctx, "Error: unauthorized");
            return null;
        }

        AuthData auth =
                dataAccess.getAuth(command.getAuthToken());

        if (auth == null) {
            sendError(ctx, "Error: unauthorized");
            return null;
        }

        return auth;
    }

    private GameData getValidGame(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        if (command.getGameID() == null) {
            sendError(ctx, "Error: game ID is required");
            return null;
        }

        GameData gameData =
                dataAccess.getGame(command.getGameID());

        if (gameData == null) {
            sendError(ctx, "Error: game not found");
            return null;
        }

        return gameData;
    }

    private String buildConnectNotification(
            AuthData auth,
            GameData gameData
    ) {
        String username = auth.username();

        if (username.equals(gameData.whiteUsername())) {
            return username + " joined the game as WHITE.";
        }

        if (username.equals(gameData.blackUsername())) {
            return username + " joined the game as BLACK.";
        }

        return username + " joined the game as an observer.";
    }

    private ChessGame.TeamColor getPlayerColor(
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

    private void sendGameStatusNotifications(
            int gameID,
            GameData gameData
    ) {
        ChessGame game = gameData.game();
        ChessGame.TeamColor teamToMove = game.getTeamTurn();

        String username =
                getUsernameForColor(teamToMove, gameData);

        if (game.isInCheckmate(teamToMove)) {
            game.setGameOver(true);

            try {
                dataAccess.updateGame(gameData);
            } catch (Exception ex) {
                return;
            }

            broadcast(
                    gameID,
                    ServerMessage.notification(
                            username + " is in checkmate."
                    ),
                    null
            );

            return;
        }

        if (game.isInStalemate(teamToMove)) {
            game.setGameOver(true);

            try {
                dataAccess.updateGame(gameData);
            } catch (Exception ex) {
                return;
            }

            broadcast(
                    gameID,
                    ServerMessage.notification(
                            "The game is in stalemate."
                    ),
                    null
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

    private String getUsernameForColor(
            ChessGame.TeamColor color,
            GameData gameData
    ) {
        if (color == ChessGame.TeamColor.WHITE) {
            if (gameData.whiteUsername() == null) {
                return "White player";
            }

            return gameData.whiteUsername();
        }

        if (gameData.blackUsername() == null) {
            return "Black player";
        }

        return gameData.blackUsername();
    }

    private void broadcast(
            int gameID,
            ServerMessage message,
            WsContext excludedContext
    ) {
        String json = gson.toJson(message);

        for (WsContext connection
                : connectionManager.getConnections(gameID)) {

            if (connection != excludedContext) {
                connection.send(json);
            }
        }
    }

    private void sendError(
            WsContext ctx,
            String message
    ) {
        ctx.send(
                gson.toJson(
                        ServerMessage.error(message)
                )
        );
    }
}