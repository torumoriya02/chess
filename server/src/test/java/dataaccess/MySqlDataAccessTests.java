package dataaccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MySqlDataAccessTests {

    private MySqlDataAccess dataAccess;

    @BeforeEach
    void setup() throws DataAccessException {
        dataAccess = new MySqlDataAccess();
        dataAccess.clear();
    }

    @Test
    void clearSuccess() {
        assertDoesNotThrow(() -> dataAccess.clear());
    }

    @Test
    public void createUserSuccess() {
        UserData user = new UserData("bob", "password", "bob@test.com");

        assertDoesNotThrow(() -> dataAccess.createUser(user));
    }

    @Test
    public void createUserDuplicate() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@test.com");

        dataAccess.createUser(user);

        assertThrows(DataAccessException.class, () -> dataAccess.createUser(user));
    }

    @Test
    public void getUserSuccess() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@test.com");

        dataAccess.createUser(user);

        UserData found = dataAccess.getUser("bob");

        assertNotNull(found);
        assertEquals(user, found);
    }

    @Test
    public void getUserNotFound() throws DataAccessException {
        assertNull(dataAccess.getUser("missing"));
    }

    @Test
    public void createAndGetAuthSuccess() throws DataAccessException {
        AuthData auth = new AuthData("token123", "bob");

        dataAccess.createAuth(auth);

        assertEquals(auth, dataAccess.getAuth("token123"));
    }

    @Test
    public void getAuthNotFound() throws DataAccessException {
        assertNull(dataAccess.getAuth("missing-token"));
    }

    @Test
    public void createAuthDuplicate() throws DataAccessException {
        AuthData auth = new AuthData("token123", "bob");

        dataAccess.createAuth(auth);

        assertThrows(
                DataAccessException.class,
                () -> dataAccess.createAuth(auth)
        );
    }

    @Test
    public void deleteAuthSuccess() throws DataAccessException {
        AuthData auth = new AuthData("token123", "bob");

        dataAccess.createAuth(auth);
        dataAccess.deleteAuth("token123");

        assertNull(dataAccess.getAuth("token123"));
    }

    @Test
    public void createGameSuccess() throws DataAccessException {
        GameData game = new GameData(
                0,
                null,
                null,
                "Test Game",
                new ChessGame()
        );

        int id = dataAccess.createGame(game);

        assertTrue(id > 0);
    }

    @Test
    public void getGameSuccess() throws DataAccessException {
        GameData game = new GameData(
                0,
                null,
                null,
                "Test Game",
                new ChessGame()
        );

        int id = dataAccess.createGame(game);
        GameData found = dataAccess.getGame(id);

        assertNotNull(found);
        assertEquals(id, found.gameID());
        assertEquals("Test Game", found.gameName());
        assertNull(found.whiteUsername());
        assertNull(found.blackUsername());
        assertNotNull(found.game());
    }

    @Test
    public void getGameNotFound() throws DataAccessException {
        assertNull(dataAccess.getGame(999999));
    }

    @Test
    public void listGamesSuccess() throws DataAccessException {
        dataAccess.createGame(new GameData(
                0,
                null,
                null,
                "Game One",
                new ChessGame()
        ));

        dataAccess.createGame(new GameData(
                0,
                "bob",
                null,
                "Game Two",
                new ChessGame()
        ));

        Collection<GameData> games = dataAccess.listGames();

        assertEquals(2, games.size());
    }
    @Test
    public void listGamesEmpty() throws DataAccessException {
        Collection<GameData> games = dataAccess.listGames();

        assertNotNull(games);
        assertTrue(games.isEmpty());
    }
    @Test
    public void updateGameSuccess() throws DataAccessException {
        int id = dataAccess.createGame(new GameData(
                0,
                null,
                null,
                "Original Game",
                new ChessGame()
        ));

        GameData updatedGame = new GameData(
                id,
                "bob",
                "alice",
                "Updated Game",
                new ChessGame()
        );

        dataAccess.updateGame(updatedGame);

        GameData found = dataAccess.getGame(id);

        assertNotNull(found);
        assertEquals("bob", found.whiteUsername());
        assertEquals("alice", found.blackUsername());
        assertEquals("Updated Game", found.gameName());
    }
    @Test
    public void updateGameNotFound() {
        GameData missingGame = new GameData(
                999999,
                "bob",
                "alice",
                "Missing Game",
                new ChessGame()
        );

        assertDoesNotThrow(() -> dataAccess.updateGame(missingGame));
    }
}
