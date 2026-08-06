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

class WebSocketGameHandler {

    private final DataAccess dataAccess;
    private final ConnectionManager connectionManager;
    private final Gson gson;

    WebSocketGameHandler(
            DataAccess dataAccess,
            ConnectionManager connectionManager,
            Gson gson
    ) {
        this.dataAccess = dataAccess;
        this.connectionManager = connectionManager;
        this.gson = gson;
    }

    void handleConnect(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);
        GameData gameData = getValidGame(ctx, command);

        if (auth == null || gameData == null) {
            return;
        }

        connectionManager.add(
                command.getGameID(),
                ctx
        );

        sendMessage(
                ctx,
                ServerMessage.loadGame(gameData)
        );

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        buildConnectNotification(
                                auth,
                                gameData
                        )
                ),
                ctx
        );
    }

    void handleMakeMove(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        MoveContext moveContext =
                validateMove(ctx, command);

        if (moveContext == null) {
            return;
        }

        GameData updatedGame =
                applyMove(
                        ctx,
                        command,
                        moveContext
                );

        if (updatedGame == null) {
            return;
        }

        dataAccess.updateGame(updatedGame);

        broadcastGameUpdate(
                command,
                updatedGame
        );

        broadcastMoveNotification(
                ctx,
                command,
                moveContext.auth().username()
        );

        sendGameStatusNotifications(
                command.getGameID(),
                updatedGame
        );
    }

    void handleLeave(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);
        GameData gameData = getValidGame(ctx, command);

        if (auth == null || gameData == null) {
            return;
        }

        GameData updatedGame =
                removePlayer(
                        auth.username(),
                        gameData
                );

        dataAccess.updateGame(updatedGame);

        connectionManager.remove(
                command.getGameID(),
                ctx
        );

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        auth.username()
                                + " left the game."
                ),
                null
        );
    }

    void handleResign(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);
        GameData gameData = getValidGame(ctx, command);

        if (auth == null || gameData == null) {
            return;
        }

        if (!isPlayer(auth.username(), gameData)) {
            sendError(
                    ctx,
                    "Error: observers cannot resign"
            );
            return;
        }

        ChessGame game = gameData.game();

        if (game == null) {
            sendError(
                    ctx,
                    "Error: game data is missing"
            );
            return;
        }

        if (game.isGameOver()) {
            sendError(
                    ctx,
                    "Error: game is already over"
            );
            return;
        }

        game.setGameOver(true);

        GameData updatedGame =
                copyGameData(gameData, game);

        dataAccess.updateGame(updatedGame);

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        auth.username()
                                + " resigned the game."
                ),
                null
        );
    }

    private MoveContext validateMove(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = getValidAuth(ctx, command);
        GameData gameData = getValidGame(ctx, command);

        if (auth == null || gameData == null) {
            return null;
        }

        if (command.getMove() == null) {
            sendError(
                    ctx,
                    "Error: move is required"
            );
            return null;
        }

        ChessGame game = gameData.game();

        if (!isValidGameForMove(ctx, game)) {
            return null;
        }

        ChessGame.TeamColor playerColor =
                getPlayerColor(
                        auth.username(),
                        gameData
                );

        if (!isValidPlayerTurn(
                ctx,
                playerColor,
                game
        )) {
            return null;
        }

        return new MoveContext(
                auth,
                gameData,
                game
        );
    }

    private boolean isValidGameForMove(
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

    private boolean isValidPlayerTurn(
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

    private GameData applyMove(
            WsContext ctx,
            UserGameCommand command,
            MoveContext moveContext
    ) {
        try {
            moveContext.game()
                    .makeMove(command.getMove());

            return copyGameData(
                    moveContext.gameData(),
                    moveContext.game()
            );
        } catch (InvalidMoveException ex) {
            sendError(
                    ctx,
                    "Error: invalid move"
            );
            return null;
        }
    }

    private void broadcastGameUpdate(
            UserGameCommand command,
            GameData updatedGame
    ) {
        broadcast(
                command.getGameID(),
                ServerMessage.loadGame(updatedGame),
                null
        );
    }

    private void broadcastMoveNotification(
            WsContext ctx,
            UserGameCommand command,
            String username
    ) {
        String notification =
                username
                        + " moved "
                        + command.getMove()
                        .getStartPosition()
                        + " to "
                        + command.getMove()
                        .getEndPosition();

        broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        notification
                ),
                ctx
        );
    }

    private GameData removePlayer(
            String username,
            GameData gameData
    ) {
        String whiteUsername =
                gameData.whiteUsername();

        String blackUsername =
                gameData.blackUsername();

        if (username.equals(whiteUsername)) {
            whiteUsername = null;
        }

        if (username.equals(blackUsername)) {
            blackUsername = null;
        }

        return new GameData(
                gameData.gameID(),
                whiteUsername,
                blackUsername,
                gameData.gameName(),
                gameData.game()
        );
    }

    private AuthData getValidAuth(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        if (command.getAuthToken() == null) {
            sendError(
                    ctx,
                    "Error: unauthorized"
            );
            return null;
        }

        AuthData auth =
                dataAccess.getAuth(
                        command.getAuthToken()
                );

        if (auth == null) {
            sendError(
                    ctx,
                    "Error: unauthorized"
            );
        }

        return auth;
    }

    private GameData getValidGame(
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

        GameData gameData =
                dataAccess.getGame(
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

    private String buildConnectNotification(
            AuthData auth,
            GameData gameData
    ) {
        String username = auth.username();

        ChessGame.TeamColor color =
                getPlayerColor(
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

    private boolean isPlayer(
            String username,
            GameData gameData
    ) {
        return getPlayerColor(
                username,
                gameData
        ) != null;
    }

    private ChessGame.TeamColor getPlayerColor(
            String username,
            GameData gameData
    ) {
        if (username.equals(
                gameData.whiteUsername()
        )) {
            return ChessGame.TeamColor.WHITE;
        }

        if (username.equals(
                gameData.blackUsername()
        )) {
            return ChessGame.TeamColor.BLACK;
        }

        return null;
    }

    private GameData copyGameData(
            GameData gameData,
            ChessGame game
    ) {
        return new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );
    }

    private void sendGameStatusNotifications(
            int gameID,
            GameData gameData
    ) throws Exception {

        ChessGame game = gameData.game();

        if (game == null) {
            return;
        }

        ChessGame.TeamColor teamToMove =
                game.getTeamTurn();

        String username =
                getUsernameForColor(
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
                ServerMessage.notification(
                        notification
                ),
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

    private void broadcast(
            int gameID,
            ServerMessage message,
            WsContext excludedContext
    ) {
        String json = gson.toJson(message);

        for (WsContext connection
                : connectionManager
                .getConnections(gameID)) {

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
        if (excludedContext == null) {
            return false;
        }

        return connection.sessionId()
                .equals(
                        excludedContext.sessionId()
                );
    }

    private void sendMessage(
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

    private record MoveContext(
            AuthData auth,
            GameData gameData,
            ChessGame game
    ) {
    }
}