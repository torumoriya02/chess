package service;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTests {

    private MemoryDataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
    }

    @Test
    public void registerSuccess() throws Exception {
        RegisterRequest request =
                new RegisterRequest("toru", "123", "toru@test.com");

        RegisterResult result = userService.register(request);

        assertEquals("toru", result.username());
        assertNotNull(result.authToken());
        assertNotNull(dataAccess.getUser("toru"));
        assertNotNull(dataAccess.getAuth(result.authToken()));
    }

    @Test
    public void registerAlreadyTaken() throws Exception {
        RegisterRequest request =
                new RegisterRequest("toru", "123", "toru@test.com");

        userService.register(request);

        assertThrows(
                IllegalStateException.class,
                () -> userService.register(request)
        );
    }

    @Test
    public void loginSuccess() throws Exception {
        userService.register(
                new RegisterRequest("toru", "123", "toru@test.com")
        );

        LoginResult result = userService.login(
                new LoginRequest("toru", "123")
        );

        assertEquals("toru", result.username());
        assertNotNull(result.authToken());
        assertNotNull(dataAccess.getAuth(result.authToken()));
    }

    @Test
    public void loginUnauthorized() throws Exception {
        userService.register(
                new RegisterRequest("toru", "123", "toru@test.com")
        );

        assertThrows(
                SecurityException.class,
                () -> userService.login(
                        new LoginRequest("toru", "wrong-password")
                )
        );
    }

    @Test
    public void logoutSuccess() throws Exception {
        RegisterResult registerResult = userService.register(
                new RegisterRequest("toru", "123", "toru@test.com")
        );

        userService.logout(registerResult.authToken());

        assertNull(dataAccess.getAuth(registerResult.authToken()));
    }

    @Test
    public void logoutUnauthorized() {
        assertThrows(
                SecurityException.class,
                () -> userService.logout("invalid-token")
        );
    }
}