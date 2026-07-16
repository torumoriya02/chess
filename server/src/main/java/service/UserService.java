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
}