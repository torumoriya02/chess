package websocket;

import io.javalin.websocket.WsContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final ConcurrentHashMap<Integer, Set<WsContext>> connections =
            new ConcurrentHashMap<>();

    public void add(int gameID, WsContext context) {
        connections
                .computeIfAbsent(
                        gameID,
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(context);
    }

    public void remove(int gameID, WsContext context) {
        Set<WsContext> gameConnections = connections.get(gameID);

        if (gameConnections == null) {
            return;
        }

        gameConnections.remove(context);

        if (gameConnections.isEmpty()) {
            connections.remove(gameID);
        }
    }

    public Set<WsContext> getConnections(int gameID) {
        return connections.getOrDefault(gameID, Set.of());
    }
}