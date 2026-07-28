package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;

import chess.ChessGame;

public class MySqlDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySqlDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();
        configureDatabase();
    }
    private void configureDatabase() throws DataAccessException {
        String[] statements = {
                """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(256) PRIMARY KEY,
                    password VARCHAR(256) NOT NULL,
                    email VARCHAR(256) NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS auth (
                    authToken VARCHAR(256) PRIMARY KEY,
                    username VARCHAR(256) NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(256),
                    blackUsername VARCHAR(256),
                    gameName VARCHAR(256) NOT NULL,
                    game LONGTEXT NOT NULL
                )
                """
        };

        try (var conn = DatabaseManager.getConnection()) {
            for (String statement : statements) {
                try (var ps = conn.prepareStatement(statement)) {
                    ps.executeUpdate();
                }
            }
        } catch (Exception ex) {
            throw new DataAccessException("Unable to configure database", ex);
        }
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            try (var ps = conn.prepareStatement("DELETE FROM auth")) {
                ps.executeUpdate();
            }

            try (var ps = conn.prepareStatement("DELETE FROM games")) {
                ps.executeUpdate();
            }

            try (var ps = conn.prepareStatement("DELETE FROM users")) {
                ps.executeUpdate();
            }

        } catch (Exception ex) {
            throw new DataAccessException("Unable to clear database", ex);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        String sql = """
                INSERT INTO users (username, password, email)
                VALUES (?, ?, ?)
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.username());
            ps.setString(2, user.password());
            ps.setString(3, user.email());

            ps.executeUpdate();

        } catch (Exception ex) {
            throw new DataAccessException("Unable to create user", ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = """
                SELECT username, password, email
                FROM users
                WHERE username = ?
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
            }

            return null;

        } catch (Exception ex) {
            throw new DataAccessException("Unable to get user", ex);
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String sql = """
                INSERT INTO auth (authToken, username)
                VALUES (?, ?)
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setString(1, auth.authToken());
            ps.setString(2, auth.username());
            ps.executeUpdate();

        } catch (Exception ex) {
            throw new DataAccessException("Unable to create auth", ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = """
                SELECT authToken, username
                FROM auth
                WHERE authToken = ?
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setString(1, authToken);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
            }

            return null;

        } catch (Exception ex) {
            throw new DataAccessException("Unable to get auth", ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = """
                DELETE FROM auth
                WHERE authToken = ?
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setString(1, authToken);
            ps.executeUpdate();

        } catch (Exception ex) {
            throw new DataAccessException("Unable to delete auth", ex);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        String sql = """
                INSERT INTO games (whiteUsername, blackUsername, gameName, game)
                VALUES (?, ?, ?, ?)
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, game.whiteUsername());
            ps.setString(2, game.blackUsername());
            ps.setString(3, game.gameName());
            ps.setString(4, gson.toJson(game.game()));

            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new DataAccessException("Failed to retrieve game ID");

        } catch (Exception ex) {
            throw new DataAccessException("Unable to create game", ex);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = """
                SELECT gameID, whiteUsername, blackUsername, gameName, game
                FROM games
                WHERE gameID = ?
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setInt(1, gameID);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    ChessGame chessGame = gson.fromJson(
                            rs.getString("game"),
                            ChessGame.class
                    );

                    return new GameData(
                            rs.getInt("gameID"),
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            chessGame
                    );
                }
            }

            return null;

        } catch (Exception ex) {
            throw new DataAccessException("Unable to get game", ex);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        String sql = """
                SELECT gameID, whiteUsername, blackUsername, gameName, game
                FROM games
                """;

        List<GameData> games = new ArrayList<>();

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql);
            var rs = ps.executeQuery()) {

            while (rs.next()) {
                ChessGame chessGame = gson.fromJson(
                        rs.getString("game"),
                        ChessGame.class
                );

                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        chessGame
                ));
            }

            return games;

        } catch (Exception ex) {
            throw new DataAccessException("Unable to list games", ex);
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        String sql = """
                UPDATE games
                SET whiteUsername = ?,
                    blackUsername = ?,
                    gameName = ?,
                    game = ?
                WHERE gameID = ?
                """;

        try (var conn = DatabaseManager.getConnection();
            var ps = conn.prepareStatement(sql)) {

            ps.setString(1, game.whiteUsername());
            ps.setString(2, game.blackUsername());
            ps.setString(3, game.gameName());
            ps.setString(4, gson.toJson(game.game()));
            ps.setInt(5, game.gameID());

            ps.executeUpdate();

        } catch (Exception ex) {
            throw new DataAccessException("Unable to update game", ex);
        }
    }
}