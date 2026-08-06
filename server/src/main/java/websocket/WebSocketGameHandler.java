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
    private final WebSocketSupport support;

    WebSocketGameHandler(
            DataAccess dataAccess,
            ConnectionManager connectionManager,
            Gson gson
    ) {
        this.dataAccess = dataAccess;
        this.support = new WebSocketSupport(
                dataAccess,
                connectionManager,
                gson
        );
    }

    void handleConnect(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {
        AuthData auth = support.getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = support.getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        support.addConnection(command.getGameID(), ctx);
        support.sendMessage(
                ctx,
                ServerMessage.loadGame(gameData)
        );

        support.broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        support.buildConnectNotification(
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
        MoveContext moveContext = validateMove(ctx, command);

        if (moveContext == null) {
            return;
        }

        GameData updatedGame = applyMove(
                ctx,
                command,
                moveContext
        );

        if (updatedGame == null) {
            return;
        }

        dataAccess.updateGame(updatedGame);
        broadcastMoveResults(
                ctx,
                command,
                moveContext.auth().username(),
                updatedGame
        );
    }

    void handleLeave(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {
        AuthData auth = support.getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = support.getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        GameData updatedGame = removePlayer(
                auth.username(),
                gameData
        );

        dataAccess.updateGame(updatedGame);
        support.removeConnection(command.getGameID(), ctx);

        support.broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        auth.username() + " left the game."
                ),
                null
        );
    }

    void handleResign(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {
        AuthData auth = support.getValidAuth(ctx, command);

        if (auth == null) {
            return;
        }

        GameData gameData = support.getValidGame(ctx, command);

        if (gameData == null) {
            return;
        }

        if (!support.isPlayer(auth.username(), gameData)) {
            support.sendError(
                    ctx,
                    "Error: observers cannot resign"
            );
            return;
        }

        ChessGame game = gameData.game();

        if (!validateResignation(ctx, game)) {
            return;
        }

        game.setGameOver(true);

        GameData updatedGame = copyGameData(
                gameData,
                game
        );

        dataAccess.updateGame(updatedGame);

        support.broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        auth.username() + " resigned the game."
                ),
                null
        );
    }

    private MoveContext validateMove(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {
        AuthData auth = support.getValidAuth(ctx, command);

        if (auth == null) {
            return null;
        }

        GameData gameData = support.getValidGame(ctx, command);

        if (gameData == null) {
            return null;
        }

        if (command.getMove() == null) {
            support.sendError(
                    ctx,
                    "Error: move is required"
            );
            return null;
        }

        ChessGame game = gameData.game();

        if (!support.validateGameForMove(ctx, game)) {
            return null;
        }

        ChessGame.TeamColor playerColor =
                support.getPlayerColor(
                        auth.username(),
                        gameData
                );

        if (!support.validatePlayerTurn(
                ctx,
                playerColor,
                game
        )) {
            return null;
        }

        return new MoveContext(auth, gameData, game);
    }

    private GameData applyMove(
            WsContext ctx,
            UserGameCommand command,
            MoveContext moveContext
    ) {
        try {
            moveContext.game().makeMove(command.getMove());

            return copyGameData(
                    moveContext.gameData(),
                    moveContext.game()
            );
        } catch (InvalidMoveException ex) {
            support.sendError(
                    ctx,
                    "Error: invalid move"
            );
            return null;
        }
    }

    private void broadcastMoveResults(
            WsContext ctx,
            UserGameCommand command,
            String username,
            GameData updatedGame
    ) throws Exception {
        support.broadcast(
                command.getGameID(),
                ServerMessage.loadGame(updatedGame),
                null
        );

        support.broadcast(
                command.getGameID(),
                ServerMessage.notification(
                        buildMoveNotification(
                                username,
                                command
                        )
                ),
                ctx
        );

        support.sendGameStatusNotifications(
                command.getGameID(),
                updatedGame
        );
    }

    private String buildMoveNotification(
            String username,
            UserGameCommand command
    ) {
        return username
                + " moved "
                + command.getMove().getStartPosition()
                + " to "
                + command.getMove().getEndPosition();
    }

    private boolean validateResignation(
            WsContext ctx,
            ChessGame game
    ) {
        if (game == null) {
            support.sendError(
                    ctx,
                    "Error: game data is missing"
            );
            return false;
        }

        if (game.isGameOver()) {
            support.sendError(
                    ctx,
                    "Error: game is already over"
            );
            return false;
        }

        return true;
    }

    private GameData removePlayer(
            String username,
            GameData gameData
    ) {
        String whiteUsername = gameData.whiteUsername();
        String blackUsername = gameData.blackUsername();

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

    void sendError(
            WsContext ctx,
            String message
    ) {
        support.sendError(ctx, message);
    }

    private record MoveContext(
            AuthData auth,
            GameData gameData,
            ChessGame game
    ) {
    }
}