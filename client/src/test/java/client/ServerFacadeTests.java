package client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import chess.ChessGame;
import model.GameData;
import server.Server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        facade = new ServerFacade("http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    void registerSuccess() throws Exception {
        var result = facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        assertNotNull(result);
        assertNotNull(result.authToken());
        assertEquals("player1", result.username());
    }

    @BeforeEach
    void clearDatabase() throws Exception {
        facade.clear();
    }

    @Test
    void registerFailure() throws Exception {
        facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        assertThrows(
                Exception.class,
                () -> facade.register(
                        "player1",
                        "anotherPassword",
                        "another@email.com"
                )
        );
    }

    @Test
    void loginSuccess() throws Exception {
        facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        var result = facade.login("player1", "password");

        assertNotNull(result);
        assertNotNull(result.authToken());
        assertEquals("player1", result.username());
    }

    @Test
    void loginFailure() throws Exception {
        facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        assertThrows(
                Exception.class,
                () -> facade.login("player1", "wrongPassword")
        );
    }

    @Test
    void logoutSuccess() throws Exception {
        var auth = facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }
    @Test
    void logoutFailure() {
        assertThrows(
                Exception.class,
                () -> facade.logout("bad-token")
        );
    }

    @Test
    void createGameSuccess() throws Exception {
        var auth = facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        int gameID = facade.createGame(
                auth.authToken(),
                "My Game"
        );

        assertTrue(gameID > 0);
    }
    @Test
    void createGameFailure() {
        assertThrows(
                Exception.class,
                () -> facade.createGame(
                        "bad-token",
                        "My Game"
                )
        );
    }

    @Test
    void listGamesSuccess() throws Exception {
        var auth = facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        facade.createGame(auth.authToken(), "Game One");
        facade.createGame(auth.authToken(), "Game Two");

        GameData[] games = facade.listGames(auth.authToken());

        assertNotNull(games);
        assertEquals(2, games.length);
    }

    @Test
    void listGamesFailure() {
        assertThrows(
                Exception.class,
                () -> facade.listGames("bad-token")
        );
    }

    @Test
    void joinGameSuccess() throws Exception {
        var auth = facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        int gameID = facade.createGame(
                auth.authToken(),
                "My Game"
        );

        assertDoesNotThrow(() ->
                facade.joinGame(
                        auth.authToken(),
                        gameID,
                        ChessGame.TeamColor.WHITE
                )
        );
    }

    @Test
    void joinGameFailure() throws Exception {
        var auth = facade.register(
                "player1",
                "password",
                "player1@email.com"
        );

        assertThrows(
                Exception.class,
                () -> facade.joinGame(
                        auth.authToken(),
                        999999,
                        ChessGame.TeamColor.WHITE
                )
        );
    }
}