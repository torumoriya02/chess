package client;

import com.google.gson.Gson;

import chess.ChessGame;
import model.AuthData;
import model.GameData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ServerFacade {

    private final String serverUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public AuthData register(String username, String password, String email)
            throws IOException, InterruptedException {

        String json = gson.toJson(new RegisterRequest(username, password, email));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/user"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException("Unable to register");
        }

        return gson.fromJson(response.body(), AuthData.class);
    }

    private record RegisterRequest(
            String username,
            String password,
            String email
    ) {
    }
    private record LoginRequest(
            String username,
            String password
    ) {
    }

    private record CreateGameRequest(String gameName) {
    }

    private record CreateGameResult(int gameID) {
    }

    private record ListGamesResult(GameData[] games) {
    }

    private record JoinGameRequest(ChessGame.TeamColor playerColor,int gameID
    ) {
    }
    public void clear() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/db"))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Unable to clear database: "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }
    }

    public AuthData login(String username, String password)
            throws IOException, InterruptedException {

        String json = gson.toJson(new LoginRequest(username, password));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/session"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Unable to login: "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }

        return gson.fromJson(response.body(), AuthData.class);
    }

    public void logout(String authToken)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/session"))
                .header("authorization", authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Unable to logout: "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }
    }

    public int createGame(String authToken, String gameName)
            throws IOException, InterruptedException {

        String json = gson.toJson(new CreateGameRequest(gameName));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/game"))
                .header("Content-Type", "application/json")
                .header("authorization", authToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Unable to create game: "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }

        CreateGameResult result =
                gson.fromJson(response.body(), CreateGameResult.class);

        return result.gameID();
    }

    public GameData[] listGames(String authToken)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/game"))
                .header("authorization", authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Unable to list games: "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }

        ListGamesResult result =
                gson.fromJson(response.body(), ListGamesResult.class);

        return result.games();
    }

    public void joinGame(String authToken,int gameID,ChessGame.TeamColor playerColor
    ) throws IOException, InterruptedException {

        String json = gson.toJson(
                new JoinGameRequest(playerColor, gameID)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/game"))
                .header("Content-Type", "application/json")
                .header("authorization", authToken)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Unable to join game: "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }
    }
}