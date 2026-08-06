package websocket;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import io.javalin.websocket.WsContext;
import websocket.commands.UserGameCommand;

public class WebSocketHandler {

    private final Gson gson = new Gson();
    private final WebSocketGameHandler gameHandler;

    public WebSocketHandler(DataAccess dataAccess) {
        ConnectionManager connectionManager =
                new ConnectionManager();

        this.gameHandler =
                new WebSocketGameHandler(
                        dataAccess,
                        connectionManager,
                        gson
                );
    }

    public void onMessage(
            WsContext ctx,
            String message
    ) {
        try {
            UserGameCommand command =
                    gson.fromJson(
                            message,
                            UserGameCommand.class
                    );

            if (!isValidCommand(command)) {
                gameHandler.sendError(
                        ctx,
                        "Error: invalid command"
                );
                return;
            }

            dispatchCommand(ctx, command);
        } catch (Exception ex) {
            gameHandler.sendError(
                    ctx,
                    getErrorMessage(ex)
            );
        }
    }

    private boolean isValidCommand(
            UserGameCommand command
    ) {
        return command != null
                && command.getCommandType() != null;
    }

    private void dispatchCommand(
            WsContext ctx,
            UserGameCommand command
    ) throws Exception {

        switch (command.getCommandType()) {
            case CONNECT ->
                    gameHandler.handleConnect(
                            ctx,
                            command
                    );

            case MAKE_MOVE ->
                    gameHandler.handleMakeMove(
                            ctx,
                            command
                    );

            case LEAVE ->
                    gameHandler.handleLeave(
                            ctx,
                            command
                    );

            case RESIGN ->
                    gameHandler.handleResign(
                            ctx,
                            command
                    );
        }
    }

    private String getErrorMessage(Exception ex) {
        if (ex.getMessage() == null) {
            return "Error: internal server error";
        }

        return "Error: " + ex.getMessage();
    }
}