package websocket;

import com.google.gson.Gson;

import chess.ChessGame;
import chess.InvalidMoveException;
import dataaccess.DataAccess;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import chess.ChessGame;
import chess.InvalidMoveException;

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

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                case MAKE_MOVE -> handleMakeMove(ctx, command);
                case LEAVE -> sendError(ctx, "Error: leave not implemented");
                case RESIGN -> sendError(ctx, "Error: resign not implemented");
            }

        } catch (Exception ex) {
            sendError(ctx, "Error: " + ex.getMessage());
        }
    }

    private void handleConnect(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth =
                dataAccess.getAuth(command.getAuthToken());

        if (auth == null) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        GameData game =
                dataAccess.getGame(command.getGameID());

        if (game == null) {
            sendError(ctx, "Error: game not found");
            return;
        }

        connectionManager.add(command.getGameID(), ctx);

        ctx.send(
                gson.toJson(
                        ServerMessage.loadGame(game)
                )
        );

        String notificationText =
            buildConnectNotification(auth, game);

        ServerMessage notification =
                ServerMessage.notification(notificationText);

        for (WsContext connection
                : connectionManager.getConnections(
                        command.getGameID()
                )) {

            if (connection != ctx) {
                connection.send(
                        gson.toJson(notification)
                );
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

    private String buildConnectNotification(
            AuthData auth,
            GameData game
    ) {
        String username = auth.username();

        if (username.equals(game.whiteUsername())) {
            return username + " joined the game as WHITE.";
        }

        if (username.equals(game.blackUsername())) {
            return username + " joined the game as BLACK.";
        }

        return username + " joined the game as an observer.";
    }

    private void handleMakeMove(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        AuthData auth = dataAccess.getAuth(command.getAuthToken());

        if (auth == null) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        GameData gameData = dataAccess.getGame(command.getGameID());

        if (gameData == null) {
            sendError(ctx, "Error: game not found");
            return;
        }

        if (command.getMove() == null) {
            sendError(ctx, "Error: move is required");
            return;
        }

        String username = auth.username();
        ChessGame game = gameData.game();
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
                        username + " moved "
                                + command.getMove().getStartPosition()
                                + " to "
                                + command.getMove().getEndPosition()
                ),
                ctx
        );
    }

    private ChessGame.TeamColor getPlayerColor(
            String username,
            GameData game
    ) {
        if (username.equals(game.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        }

        if (username.equals(game.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }

        return null;
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
}