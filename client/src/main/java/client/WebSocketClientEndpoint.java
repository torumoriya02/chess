package client;

import com.google.gson.Gson;
import websocket.messages.ServerMessage;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;

@ClientEndpoint
public class WebSocketClientEndpoint {

    private final Gson gson = new Gson();
    private final NotificationHandler notificationHandler;

    public WebSocketClientEndpoint(
            NotificationHandler notificationHandler
    ) {
        this.notificationHandler = notificationHandler;
    }

    @OnMessage
    public void onMessage(String json) {
        ServerMessage message =
                gson.fromJson(json, ServerMessage.class);

        notificationHandler.notify(message);
    }
}