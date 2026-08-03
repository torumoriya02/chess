package client;

import chess.ChessGame;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.util.Scanner;

public class ChessClient implements NotificationHandler {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade facade;
    private final String serverUrl;

    private String authToken;
    private GameData[] listedGames = new GameData[0];
    private WebSocketCommunicator webSocket;

    private Integer currentGameID;
    private ChessGame.TeamColor boardPerspective =
            ChessGame.TeamColor.WHITE;

    private State state = State.PRELOGIN;

    private enum State {
        PRELOGIN,
        POSTLOGIN,
        GAMEPLAY
    }

    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.facade = new ServerFacade(serverUrl);
    }

    public void run() {
        System.out.println("♕ 240 Chess Client");
        System.out.println("Type help to see available commands.");

        while (true) {
            System.out.print(">>> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                closeWebSocket();
                System.out.println("Goodbye!");
                return;
            }

            if (state == State.PRELOGIN) {
                handlePreloginCommand(input);
            } else if (state == State.POSTLOGIN) {
                handlePostloginCommand(input);
            } else {
                handleGameplayCommand(input);
            }
        }
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

    private void handleGameplayCommand(String input) {
        if (input.equalsIgnoreCase("help")) {
            printGameplayHelp();
        } else if (input.equalsIgnoreCase("redraw")) {
            System.out.println("Redraw will be implemented next.");
        } else if (input.equalsIgnoreCase("move")) {
            System.out.println("Move will be implemented next.");
        } else if (input.equalsIgnoreCase("resign")) {
            System.out.println("Resign will be implemented next.");
        } else if (input.equalsIgnoreCase("leave")) {
            leaveGame();
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

    private void printGameplayHelp() {
        System.out.println("help   - show gameplay commands");
        System.out.println("redraw - redraw the chess board");
        System.out.println("move   - make a chess move");
        System.out.println("resign - resign the game");
        System.out.println("leave  - leave the game");
        System.out.println("quit   - exit the program");
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

            System.out.println(
                    "Logged in as " + result.username()
            );

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

            var result =
                    facade.register(username, password, email);

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
            closeWebSocket();

            facade.logout(authToken);

            authToken = null;
            listedGames = new GameData[0];
            currentGameID = null;
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
                System.out.println(
                        "Game name cannot be empty."
                );
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

            if (listedGames == null
                    || listedGames.length == 0) {

                listedGames = new GameData[0];
                System.out.println("No games available.");
                return;
            }

            for (int i = 0; i < listedGames.length; i++) {
                GameData game = listedGames[i];

                String white =
                        game.whiteUsername() == null
                                ? "available"
                                : game.whiteUsername();

                String black =
                        game.blackUsername() == null
                                ? "available"
                                : game.blackUsername();

                System.out.printf(
                        "%d. %s | White: %s | Black: %s%n",
                        i + 1,
                        game.gameName(),
                        white,
                        black
                );
            }

        } catch (Exception ex) {
            System.out.println("Unable to list games.");
        }
    }

    private void playGame() {
        try {
            if (listedGames.length == 0) {
                System.out.println("List games first.");
                return;
            }

            System.out.print("Game number: ");
            int gameNumber =
                    Integer.parseInt(
                            scanner.nextLine().trim()
                    );

            if (gameNumber < 1
                    || gameNumber > listedGames.length) {

                System.out.println("Invalid game number.");
                return;
            }

            System.out.print("Color (WHITE or BLACK): ");
            String colorInput =
                    scanner.nextLine()
                            .trim()
                            .toUpperCase();

            ChessGame.TeamColor color =
                    ChessGame.TeamColor.valueOf(colorInput);

            GameData selectedGame =
                    listedGames[gameNumber - 1];

            facade.joinGame(
                    authToken,
                    selectedGame.gameID(),
                    color
            );

            currentGameID = selectedGame.gameID();
            boardPerspective = color;

            connectToGame();
            state = State.GAMEPLAY;

            System.out.println(
                    "Joined "
                            + selectedGame.gameName()
                            + " as "
                            + color
            );

        } catch (NumberFormatException ex) {
            System.out.println(
                    "Game number must be a number."
            );

        } catch (IllegalArgumentException ex) {
            System.out.println(
                    "Color must be WHITE or BLACK."
            );

        } catch (Exception ex) {
            System.out.println(
                    "Unable to join game: "
                            + ex.getMessage()
            );
        }
    }

    private void observeGame() {
        try {
            if (listedGames.length == 0) {
                System.out.println("List games first.");
                return;
            }

            System.out.print("Game number: ");
            int gameNumber =
                    Integer.parseInt(
                            scanner.nextLine().trim()
                    );

            if (gameNumber < 1
                    || gameNumber > listedGames.length) {

                System.out.println("Invalid game number.");
                return;
            }

            GameData selectedGame =
                    listedGames[gameNumber - 1];

            currentGameID = selectedGame.gameID();
            boardPerspective = ChessGame.TeamColor.WHITE;

            connectToGame();
            state = State.GAMEPLAY;

            System.out.println(
                    "Observing "
                            + selectedGame.gameName()
            );

        } catch (NumberFormatException ex) {
            System.out.println(
                    "Game number must be a number."
            );

        } catch (Exception ex) {
            System.out.println(
                    "Unable to observe game: "
                            + ex.getMessage()
            );
        }
    }

    private void leaveGame() {
        if (webSocket == null || currentGameID == null) {
            System.out.println(
                    "You are not currently in a game."
            );
            return;
        }

        try {
            UserGameCommand leaveCommand =
                    new UserGameCommand(
                            UserGameCommand.CommandType.LEAVE,
                            authToken,
                            currentGameID
                    );

            webSocket.sendCommand(leaveCommand);
            closeWebSocket();

            currentGameID = null;
            boardPerspective = ChessGame.TeamColor.WHITE;
            state = State.POSTLOGIN;

            System.out.println("You left the game.");

        } catch (Exception ex) {
            System.out.println(
                    "Unable to leave game: "
                            + ex.getMessage()
            );
        }
    }

    private void connectToGame() throws Exception {
        closeWebSocket();

        webSocket =
                new WebSocketCommunicator(
                        serverUrl,
                        this
                );

        UserGameCommand connectCommand =
                new UserGameCommand(
                        UserGameCommand.CommandType.CONNECT,
                        authToken,
                        currentGameID
                );

        webSocket.sendCommand(connectCommand);
    }

    private void closeWebSocket() {
        if (webSocket == null) {
            return;
        }

        try {
            webSocket.close();
        } catch (Exception ignored) {
            // The connection may already be closed.
        }

        webSocket = null;
    }

    @Override
    public void notify(ServerMessage message) {
        if (message == null
                || message.getServerMessageType() == null) {

            System.out.println(
                    "\nReceived an invalid server message."
            );
            return;
        }

        switch (message.getServerMessageType()) {
            case LOAD_GAME -> handleLoadGame(message);

            case NOTIFICATION -> System.out.println(
                    "\n" + message.getMessage()
            );

            case ERROR -> System.out.println(
                    "\n" + message.getErrorMessage()
            );
        }

        System.out.print(">>> ");
    }

    private void handleLoadGame(ServerMessage message) {
        GameData gameData = message.getGame();

        if (gameData == null || gameData.game() == null) {
            System.out.println(
                    "\nUnable to load game."
            );
            return;
        }

        System.out.println();

        BoardDrawer.draw(
                gameData.game().getBoard(),
                boardPerspective
        );
    }
}