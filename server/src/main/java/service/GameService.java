package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.GameData;

import java.util.Collection;

public class GameService {

    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public ListGamesResult listGames(String authToken)
            throws DataAccessException {

        if (dataAccess.getAuth(authToken) == null) {
            throw new SecurityException("Error: unauthorized");
        }

        Collection<GameData> games = dataAccess.listGames();

        return new ListGamesResult(games);
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest request)
            throws DataAccessException {

        if (dataAccess.getAuth(authToken) == null) {
            throw new SecurityException("Error: unauthorized");
        }

        if (request == null
                || request.gameName() == null
                || request.gameName().isBlank()) {
            throw new IllegalArgumentException("Error: bad request");
        }

        GameData game = new GameData(
                0,
                null,
                null,
                request.gameName(),
                new ChessGame()
        );

        int gameID = dataAccess.createGame(game);

        return new CreateGameResult(gameID);
    }

    public void joinGame(String authToken, JoinGameRequest request)
        throws DataAccessException {

    var authData = dataAccess.getAuth(authToken);

    if (authData == null) {
        throw new SecurityException("Error: unauthorized");
    }

    if (request == null
            || request.playerColor() == null
            || (!request.playerColor().equals("WHITE")
            && !request.playerColor().equals("BLACK"))) {
        throw new IllegalArgumentException("Error: bad request");
    }

    GameData game = dataAccess.getGame(request.gameID());

    if (game == null) {
        throw new IllegalArgumentException("Error: bad request");
    }

    String whiteUsername = game.whiteUsername();
    String blackUsername = game.blackUsername();

    if (request.playerColor().equals("WHITE")) {
        if (whiteUsername != null) {
            throw new IllegalStateException("Error: already taken");
        }

        whiteUsername = authData.username();
    } else {
        if (blackUsername != null) {
            throw new IllegalStateException("Error: already taken");
        }

        blackUsername = authData.username();
    }

    GameData updatedGame = new GameData(
            game.gameID(),
            whiteUsername,
            blackUsername,
            game.gameName(),
            game.game()
    );

    dataAccess.updateGame(updatedGame);
    }
}