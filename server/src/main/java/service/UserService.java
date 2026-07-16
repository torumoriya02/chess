package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class UserService {

    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public RegisterResult register(RegisterRequest request)
            throws DataAccessException {

        if (request == null
                || request.username() == null
                || request.password() == null
                || request.email() == null
                || request.username().isBlank()
                || request.password().isBlank()
                || request.email().isBlank()) {
            throw new IllegalArgumentException("Error: bad request");
        }

        UserData existingUser = dataAccess.getUser(request.username());

        if (existingUser != null) {
            throw new IllegalStateException("Error: already taken");
        }

        UserData newUser = new UserData(
                request.username(),
                request.password(),
                request.email()
        );

        dataAccess.createUser(newUser);

        String authToken = UUID.randomUUID().toString();

        AuthData authData = new AuthData(
                authToken,
                request.username()
        );

        dataAccess.createAuth(authData);

        return new RegisterResult(
                request.username(),
                authToken
        );
    }

    public LoginResult login(LoginRequest request)
        throws DataAccessException {

    if (request == null
            || request.username() == null
            || request.password() == null
            || request.username().isBlank()
            || request.password().isBlank()) {
        throw new IllegalArgumentException("Error: bad request");
    }

    UserData user = dataAccess.getUser(request.username());

    if (user == null || !user.password().equals(request.password())) {
        throw new SecurityException("Error: unauthorized");
    }

    String authToken = UUID.randomUUID().toString();

    AuthData authData = new AuthData(
            authToken,
            request.username()
    );

    dataAccess.createAuth(authData);

    return new LoginResult(
            request.username(),
            authToken
    );
}
    public void logout(String authToken)
        throws DataAccessException {

    AuthData authData = dataAccess.getAuth(authToken);

    if (authData == null) {
        throw new SecurityException("Error: unauthorized");
    }

    dataAccess.deleteAuth(authToken);
    }
}