package client;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class GameplayClient implements NotificationHandler {

    private final String serverUrl;
    private final Scanner scanner;
    private final String authToken;
    private final Integer gameID;
    private final ChessGame.TeamColor perspective;

    private WebSocketCommunicator webSocket;
    private GameData currentGame;
    private boolean gameplayActive = true;
    private boolean quitRequested;

    public GameplayClient(
            String serverUrl,
            Scanner scanner,
            String authToken,
            Integer gameID,
            ChessGame.TeamColor perspective
    ) {
        this.serverUrl = serverUrl;
        this.scanner = scanner;
        this.authToken = authToken;
        this.gameID = gameID;
        this.perspective = perspective;
    }

    /**
     * Runs the gameplay command loop.
     *
     * @return true when the user requested to quit the entire client
     */
    public boolean run() throws Exception {
        connectToGame();

        while (gameplayActive) {
            System.out.print(">>> ");
            String input = scanner.nextLine().trim();
            handleCommand(input);
        }

        closeWebSocket();
        return quitRequested;
    }

    private void handleCommand(String input) {
        if (input.equalsIgnoreCase("help")) {
            printHelp();
        } else if (input.equalsIgnoreCase("redraw")) {
            redrawBoard();
        } else if (input.equalsIgnoreCase("highlight")) {
            highlightMoves();
        } else if (input.equalsIgnoreCase("move")) {
            makeMove();
        } else if (input.equalsIgnoreCase("resign")) {
            resignGame();
        } else if (input.equalsIgnoreCase("leave")) {
            leaveGame();
        } else if (input.equalsIgnoreCase("quit")) {
            quitGame();
        } else {
            System.out.println("Unknown command. Type help.");
        }
    }

    private void printHelp() {
        System.out.println("help      - show gameplay commands");
        System.out.println("redraw    - redraw the chess board");
        System.out.println("highlight - show legal moves for a piece");
        System.out.println("move      - make a chess move");
        System.out.println("resign    - resign the game");
        System.out.println("leave     - leave the game");
        System.out.println("quit      - exit the program");
    }

    private void redrawBoard() {
        if (!hasLoadedGame()) {
            System.out.println("No game is currently loaded.");
            return;
        }

        BoardDrawer.draw(
                currentGame.game().getBoard(),
                perspective
        );
    }

    private void highlightMoves() {
        if (!hasLoadedGame()) {
            System.out.println("No game is currently loaded.");
            return;
        }

        try {
            ChessPosition startPosition =
                    readPosition(
                            "Piece position, for example e2: "
                    );

            Collection<ChessMove> validMoves =
                    currentGame.game()
                            .validMoves(startPosition);

            if (validMoves == null) {
                System.out.println(
                        "There is no piece at that position."
                );
                return;
            }

            if (validMoves.isEmpty()) {
                System.out.println(
                        "That piece has no legal moves."
                );
                return;
            }

            drawHighlightedMoves(
                    startPosition,
                    validMoves
            );
        } catch (IllegalArgumentException ex) {
            System.out.println(
                    "Invalid position. Use a square such as e2."
            );
        }
    }

    private void drawHighlightedMoves(
            ChessPosition startPosition,
            Collection<ChessMove> validMoves
    ) {
        Set<ChessPosition> highlights =
                new HashSet<>();

        highlights.add(startPosition);

        for (ChessMove move : validMoves) {
            highlights.add(move.getEndPosition());
        }

        BoardDrawer.draw(
                currentGame.game().getBoard(),
                perspective,
                highlights
        );
    }

    private void makeMove() {
        if (webSocket == null) {
            System.out.println(
                    "You are not currently connected to a game."
            );
            return;
        }

        try {
            ChessPosition start =
                    readPosition(
                            "Start position, for example e2: "
                    );

            ChessPosition end =
                    readPosition(
                            "End position, for example e4: "
                    );

            ChessPiece.PieceType promotion =
                    readPromotion(start, end);

            ChessMove move =
                    new ChessMove(
                            start,
                            end,
                            promotion
                    );

            sendCommand(
                    UserGameCommand.CommandType.MAKE_MOVE,
                    move
            );
        } catch (IllegalArgumentException ex) {
            System.out.println(
                    "Invalid position or promotion piece."
            );
        } catch (Exception ex) {
            System.out.println(
                    "Unable to make move: "
                            + ex.getMessage()
            );
        }
    }

    private ChessPiece.PieceType readPromotion(
            ChessPosition start,
            ChessPosition end
    ) {
        if (!isPromotionMove(start, end)) {
            return null;
        }

        System.out.print(
                "Promotion piece "
                        + "(QUEEN, ROOK, BISHOP, KNIGHT): "
        );

        ChessPiece.PieceType promotion =
                ChessPiece.PieceType.valueOf(
                        scanner.nextLine()
                                .trim()
                                .toUpperCase()
                );

        if (promotion == ChessPiece.PieceType.KING
                || promotion == ChessPiece.PieceType.PAWN) {

            throw new IllegalArgumentException(
                    "Invalid promotion piece"
            );
        }

        return promotion;
    }

    private ChessPosition readPosition(String prompt) {
        System.out.print(prompt);
        return parsePosition(scanner.nextLine().trim());
    }

    private ChessPosition parsePosition(String input) {
        if (input == null || input.length() != 2) {
            throw new IllegalArgumentException(
                    "Position must contain two characters"
            );
        }

        char file =
                Character.toLowerCase(input.charAt(0));

        char rank = input.charAt(1);

        if (file < 'a' || file > 'h'
                || rank < '1' || rank > '8') {

            throw new IllegalArgumentException(
                    "Position is outside the board"
            );
        }

        int column = file - 'a' + 1;
        int row = rank - '0';

        return new ChessPosition(row, column);
    }

    private boolean isPromotionMove(
            ChessPosition start,
            ChessPosition end
    ) {
        if (!hasLoadedGame()) {
            return false;
        }

        ChessPiece piece =
                currentGame.game()
                        .getBoard()
                        .getPiece(start);

        return piece != null
                && piece.getPieceType()
                == ChessPiece.PieceType.PAWN
                && (end.getRow() == 1
                || end.getRow() == 8);
    }

    private void resignGame() {
        if (webSocket == null) {
            System.out.println(
                    "You are not currently connected to a game."
            );
            return;
        }

        System.out.print(
                "Are you sure you want to resign? (yes/no): "
        );

        String confirmation =
                scanner.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Resignation canceled.");
            return;
        }

        try {
            sendCommand(
                    UserGameCommand.CommandType.RESIGN,
                    null
            );

            System.out.println("You resigned the game.");
        } catch (Exception ex) {
            System.out.println(
                    "Unable to resign: "
                            + ex.getMessage()
            );
        }
    }

    private void leaveGame() {
        try {
            sendLeaveCommand();
            gameplayActive = false;
            System.out.println("You left the game.");
        } catch (Exception ex) {
            System.out.println(
                    "Unable to leave game: "
                            + ex.getMessage()
            );
        }
    }

    private void quitGame() {
        try {
            sendLeaveCommand();
        } catch (Exception ignored) {
            // The socket may already be closed.
        }

        quitRequested = true;
        gameplayActive = false;
    }

    private void sendLeaveCommand() throws Exception {
        sendCommand(
                UserGameCommand.CommandType.LEAVE,
                null
        );
    }

    private void sendCommand(
            UserGameCommand.CommandType commandType,
            ChessMove move
    ) throws Exception {

        if (webSocket == null) {
            throw new IllegalStateException(
                    "WebSocket connection is not open"
            );
        }

        UserGameCommand command =
                new UserGameCommand(
                        commandType,
                        authToken,
                        gameID,
                        move
                );

        webSocket.sendCommand(command);
    }

    private void connectToGame() throws Exception {
        webSocket =
                new WebSocketCommunicator(
                        serverUrl,
                        this
                );

        sendCommand(
                UserGameCommand.CommandType.CONNECT,
                null
        );
    }

    private void closeWebSocket() {
        if (webSocket == null) {
            return;
        }

        try {
            webSocket.close();
        } catch (Exception ignored) {
            // The socket may already be closed.
        }

        webSocket = null;
    }

    private boolean hasLoadedGame() {
        return currentGame != null
                && currentGame.game() != null;
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
            case NOTIFICATION ->
                    System.out.println(
                            "\n" + message.getMessage()
                    );
            case ERROR ->
                    System.out.println(
                            "\n" + message.getErrorMessage()
                    );
        }
    }

    private void handleLoadGame(ServerMessage message) {
        GameData gameData = message.getGame();

        if (gameData == null || gameData.game() == null) {
            System.out.println("\nUnable to load game.");
            return;
        }

        currentGame = gameData;

        System.out.println();

        BoardDrawer.draw(
                currentGame.game().getBoard(),
                perspective
        );
    }
}