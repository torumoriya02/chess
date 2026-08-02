package websocket;

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

            if (command.getCommandType()
                    == UserGameCommand.CommandType.CONNECT) {
                handleConnect(ctx, command);
            } else {
                sendError(ctx, "Error: command not implemented");
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
}