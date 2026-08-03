package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BoardDrawer {

    private static final String RESET = "\u001B[0m";

    private static final String LIGHT_SQUARE = "\u001B[47m";
    private static final String DARK_SQUARE = "\u001B[100m";

    private static final String LIGHT_HIGHLIGHT = "\u001B[102m";
    private static final String DARK_HIGHLIGHT = "\u001B[42m";

    private static final String WHITE_PIECE = "\u001B[31m";
    private static final String BLACK_PIECE = "\u001B[34m";

    public static void draw(
            ChessBoard board,
            ChessGame.TeamColor perspective
    ) {
        draw(board, perspective, Set.of());
    }

    public static void draw(
            ChessBoard board,
            ChessGame.TeamColor perspective,
            Collection<ChessPosition> highlightedPositions
    ) {
        Set<ChessPosition> highlights =
                highlightedPositions == null
                        ? Set.of()
                        : new HashSet<>(highlightedPositions);

        boolean whitePerspective =
                perspective == ChessGame.TeamColor.WHITE;

        int rankStart = whitePerspective ? 8 : 1;
        int rankEnd = whitePerspective ? 0 : 9;
        int rankStep = whitePerspective ? -1 : 1;

        int fileStart = whitePerspective ? 1 : 8;
        int fileEnd = whitePerspective ? 9 : 0;
        int fileStep = whitePerspective ? 1 : -1;

        printFiles(fileStart, fileEnd, fileStep);

        for (int rank = rankStart;
             rank != rankEnd;
             rank += rankStep) {

            System.out.print(rank + " ");

            for (int file = fileStart;
                 file != fileEnd;
                 file += fileStep) {

                ChessPosition position =
                        new ChessPosition(rank, file);

                ChessPiece piece =
                        board.getPiece(position);

                boolean lightSquare =
                        (rank + file) % 2 == 1;

                boolean highlighted =
                        highlights.contains(position);

                String background =
                        getBackground(
                                lightSquare,
                                highlighted
                        );

                String foreground =
                        getForeground(piece);

                System.out.print(
                        background
                                + foreground
                                + " "
                                + pieceSymbol(piece)
                                + " "
                                + RESET
                );
            }

            System.out.println(" " + rank);
        }

        printFiles(fileStart, fileEnd, fileStep);
    }

    private static String getBackground(
            boolean lightSquare,
            boolean highlighted
    ) {
        if (highlighted) {
            return lightSquare
                    ? LIGHT_HIGHLIGHT
                    : DARK_HIGHLIGHT;
        }

        return lightSquare
                ? LIGHT_SQUARE
                : DARK_SQUARE;
    }

    private static String getForeground(
            ChessPiece piece
    ) {
        if (piece == null) {
            return "";
        }

        return piece.getTeamColor()
                == ChessGame.TeamColor.WHITE
                ? WHITE_PIECE
                : BLACK_PIECE;
    }

    private static void printFiles(
            int start,
            int end,
            int step
    ) {
        System.out.print("   ");

        for (int file = start;
             file != end;
             file += step) {

            char letter =
                    (char) ('a' + file - 1);

            System.out.print(letter + "  ");
        }

        System.out.println();
    }

    private static String pieceSymbol(
            ChessPiece piece
    ) {
        if (piece == null) {
            return " ";
        }

        boolean white =
                piece.getTeamColor()
                        == ChessGame.TeamColor.WHITE;

        return switch (piece.getPieceType()) {
            case KING -> white ? "♔" : "♚";
            case QUEEN -> white ? "♕" : "♛";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case ROOK -> white ? "♖" : "♜";
            case PAWN -> white ? "♙" : "♟";
        };
    }
}