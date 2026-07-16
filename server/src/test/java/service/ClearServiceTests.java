package service;

import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTests {

    @Test
    public void clearSuccess() throws Exception {
        MemoryDataAccess dataAccess = new MemoryDataAccess();

        dataAccess.createUser(
                new UserData("toru", "123", "toru@test.com")
        );

        dataAccess.createAuth(
                new AuthData("test-token", "toru")
        );

        ClearService clearService = new ClearService(dataAccess);

        clearService.clear();

        assertNull(dataAccess.getUser("toru"));
        assertNull(dataAccess.getAuth("test-token"));
        assertTrue(dataAccess.listGames().isEmpty());
    }
}