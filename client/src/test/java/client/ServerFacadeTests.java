package client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    }