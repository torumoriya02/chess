package client;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import java.net.URI;

public class WebSocketCommunicator {

    private final Gson gson = new Gson();
    private Session session;

    public WebSocketCommunicator(
            String serverUrl,
            NotificationHandler notificationHandler
    ) throws Exception {

        String websocketUrl = serverUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                + "/ws";

        WebSocketContainer container =
                ContainerProvider.getWebSocketContainer();

        session = container.connectToServer(
                new WebSocketClientEndpoint(notificationHandler),
                URI.create(websocketUrl)
        );
    }

    public void sendCommand(
            UserGameCommand command
    ) throws Exception {

        if (session == null || !session.isOpen()) {
            throw new IllegalStateException(
                    "WebSocket connection is not open"
            );
        }

        String json = gson.toJson(command);
        session.getBasicRemote().sendText(json);
    }

    public void close() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}