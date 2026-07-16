package service;

import dataaccess.MemoryDataAccess;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTests {

    private MemoryDataAccess dataAccess;
    private GameService gameService;
    private String authToken;

    @BeforeEach
    public void setUp() throws Exception {
        dataAccess = new MemoryDataAccess();
        gameService = new GameService(dataAccess);

        authToken = "test-token";
        dataAccess.createAuth(new AuthData(authToken, "toru"));
    }

    @Test
    public void listGamesSuccess() throws Exception {
        ListGamesResult result = gameService.listGames(authToken);

        assertNotNull(result);
        assertTrue(result.games().isEmpty());
    }

    @Test
    public void listGamesUnauthorized() {
        assertThrows(
                SecurityException.class,
                () -> gameService.listGames("bad-token")
        );
    }

    @Test
    public void createGameSuccess() throws Exception {
        CreateGameResult result = gameService.createGame(
                authToken,
                new CreateGameRequest("Game One")
        );

        assertEquals(1, result.gameID());
        assertNotNull(dataAccess.getGame(result.gameID()));
    }

    @Test
    public void createGameBadRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> gameService.createGame(
                        authToken,
                        new CreateGameRequest("")
                )
        );
    }

    @Test
    public void joinGameSuccess() throws Exception {
        CreateGameResult created = gameService.createGame(
                authToken,
                new CreateGameRequest("Game One")
        );

        gameService.joinGame(
                authToken,
                new JoinGameRequest("WHITE", created.gameID())
        );

        var game = dataAccess.getGame(created.gameID());

        assertEquals("toru", game.whiteUsername());
        assertNull(game.blackUsername());
    }

    @Test
    public void joinGameAlreadyTaken() throws Exception {
        CreateGameResult created = gameService.createGame(
                authToken,
                new CreateGameRequest("Game One")
        );

        gameService.joinGame(
                authToken,
                new JoinGameRequest("WHITE", created.gameID())
        );

        dataAccess.createAuth(
                new model.AuthData("second-token", "alice")
        );

        assertThrows(
                IllegalStateException.class,
                () -> gameService.joinGame(
                        "second-token",
                        new JoinGameRequest("WHITE", created.gameID())
                )
        );
    }
}