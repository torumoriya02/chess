package client;

import chess.ChessGame;
import model.GameData;

import java.util.Scanner;

public class ChessClient {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade facade;
    private final String serverUrl;

    private String authToken;
    private GameData[] listedGames = new GameData[0];
    private State state = State.PRELOGIN;
    private boolean shouldQuit;

    private enum State {
        PRELOGIN,
        POSTLOGIN
    }

    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.facade = new ServerFacade(serverUrl);
    }

    public void run() {
        System.out.println("♕ 240 Chess Client");
        System.out.println("Type help to see available commands.");

        while (!shouldQuit) {
            System.out.print(">>> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                shouldQuit = true;
            } else if (state == State.PRELOGIN) {
                handlePreloginCommand(input);
            } else {
                handlePostloginCommand(input);
            }
        }

        System.out.println("Goodbye!");
    }

    private void handlePreloginCommand(String input) {
        if (input.equalsIgnoreCase("help")) {
            printPreloginHelp();
        } else if (input.equalsIgnoreCase("register")) {
            register();
        } else if (input.equalsIgnoreCase("login")) {
            login();
        } else {
            System.out.println("Unknown command. Type help.");
        }
    }

    private void handlePostloginCommand(String input) {
        if (input.equalsIgnoreCase("help")) {
            printPostloginHelp();
        } else if (input.equalsIgnoreCase("logout")) {
            logout();
        } else if (input.equalsIgnoreCase("create game")) {
            createGame();
        } else if (input.equalsIgnoreCase("list games")) {
            listGames();
        } else if (input.equalsIgnoreCase("play game")) {
            playGame();
        } else if (input.equalsIgnoreCase("observe game")) {
            observeGame();
        } else {
            System.out.println("Unknown command. Type help.");
        }
    }

    private void printPreloginHelp() {
        System.out.println("help     - show available commands");
        System.out.println("login    - log in to your account");
        System.out.println("register - create a new account");
        System.out.println("quit     - exit the program");
    }

    private void printPostloginHelp() {
        System.out.println("help         - show available commands");
        System.out.println("logout       - log out");
        System.out.println("create game  - create a new game");
        System.out.println("list games   - list available games");
        System.out.println("play game    - join a game");
        System.out.println("observe game - observe a game");
        System.out.println("quit         - exit the program");
    }

    private void login() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            var result = facade.login(username, password);

            authToken = result.authToken();
            state = State.POSTLOGIN;

            System.out.println("Logged in as " + result.username());
        } catch (Exception ex) {
            System.out.println(
                    "Unable to log in. Check your username and password."
            );
        }
    }

    private void register() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            var result = facade.register(
                    username,
                    password,
                    email
            );

            authToken = result.authToken();
            state = State.POSTLOGIN;

            System.out.println(
                    "Registered and logged in as "
                            + result.username()
            );
        } catch (Exception ex) {
            System.out.println(
                    "Unable to register. Please check your information."
            );
        }
    }

    private void logout() {
        try {
            facade.logout(authToken);

            authToken = null;
            listedGames = new GameData[0];
            state = State.PRELOGIN;

            System.out.println("Logged out.");
        } catch (Exception ex) {
            System.out.println("Unable to log out.");
        }
    }

    private void createGame() {
        try {
            System.out.print("Game name: ");
            String gameName = scanner.nextLine().trim();

            if (gameName.isBlank()) {
                System.out.println("Game name cannot be empty.");
                return;
            }

            facade.createGame(authToken, gameName);
            System.out.println("Game created.");
        } catch (Exception ex) {
            System.out.println("Unable to create game.");
        }
    }

    private void listGames() {
        try {
            listedGames = facade.listGames(authToken);

            if (listedGames == null || listedGames.length == 0) {
                listedGames = new GameData[0];
                System.out.println("No games available.");
                return;
            }

            printListedGames();
        } catch (Exception ex) {
            System.out.println("Unable to list games.");
        }
    }

    private void printListedGames() {
        for (int index = 0; index < listedGames.length; index++) {
            GameData game = listedGames[index];

            String white = displayUsername(
                    game.whiteUsername()
            );

            String black = displayUsername(
                    game.blackUsername()
            );

            System.out.printf(
                    "%d. %s | White: %s | Black: %s%n",
                    index + 1,
                    game.gameName(),
                    white,
                    black
            );
        }
    }

    private String displayUsername(String username) {
        return username == null
                ? "available"
                : username;
    }

    private void playGame() {
        try {
            GameData selectedGame = selectGame();

            if (selectedGame == null) {
                return;
            }

            ChessGame.TeamColor color = selectColor();

            facade.joinGame(
                    authToken,
                    selectedGame.gameID(),
                    color
            );

            System.out.println(
                    "Joined "
                            + selectedGame.gameName()
                            + " as "
                            + color
            );

            enterGameplay(selectedGame, color);
        } catch (IllegalArgumentException ex) {
            System.out.println("Color must be WHITE or BLACK.");
        } catch (Exception ex) {
            System.out.println(
                    "Unable to join game: "
                            + ex.getMessage()
            );
        }
    }

    private void observeGame() {
        try {
            GameData selectedGame = selectGame();

            if (selectedGame == null) {
                return;
            }

            System.out.println(
                    "Observing "
                            + selectedGame.gameName()
            );

            enterGameplay(
                    selectedGame,
                    ChessGame.TeamColor.WHITE
            );
        } catch (Exception ex) {
            System.out.println(
                    "Unable to observe game: "
                            + ex.getMessage()
            );
        }
    }

    private GameData selectGame() {
        if (listedGames.length == 0) {
            System.out.println("List games first.");
            return null;
        }

        System.out.print("Game number: ");

        try {
            int gameNumber = Integer.parseInt(
                    scanner.nextLine().trim()
            );

            if (gameNumber < 1
                    || gameNumber > listedGames.length) {

                System.out.println("Invalid game number.");
                return null;
            }

            return listedGames[gameNumber - 1];
        } catch (NumberFormatException ex) {
            System.out.println(
                    "Game number must be a number."
            );
            return null;
        }
    }

    private ChessGame.TeamColor selectColor() {
        System.out.print("Color (WHITE or BLACK): ");

        String input = scanner.nextLine()
                .trim()
                .toUpperCase();

        return ChessGame.TeamColor.valueOf(input);
    }

    private void enterGameplay(
            GameData selectedGame,
            ChessGame.TeamColor perspective
    ) throws Exception {

        GameplayClient gameplayClient =
                new GameplayClient(
                        serverUrl,
                        scanner,
                        authToken,
                        selectedGame.gameID(),
                        perspective
                );

        shouldQuit = gameplayClient.run();
    }
}