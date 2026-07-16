package service;
import dataaccess.DataAccessException;
import model.GameData;
import java.util.Collection;
import dataaccess.DataAccess;

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
}
