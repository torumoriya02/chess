package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

public class BoardDrawer {

    public static void draw(ChessBoard board, ChessGame.TeamColor perspective) {
        boolean whitePerspective =
                perspective == ChessGame.TeamColor.WHITE;

        int rankStart = whitePerspective ? 8 : 1;
        int rankEnd = whitePerspective ? 0 : 9;
        int rankStep = whitePerspective ? -1 : 1;

        int fileStart = whitePerspective ? 1 : 8;
        int fileEnd = whitePerspective ? 9 : 0;
        int fileStep = whitePerspective ? 1 : -1;

        printFiles(fileStart, fileEnd, fileStep);

        for (int rank = rankStart; rank != rankEnd; rank += rankStep) {
            System.out.print(rank + " ");

            for (int file = fileStart; file != fileEnd; file += fileStep) {
                ChessPiece piece = board.getPiece(
                        new ChessPosition(rank, file)
                );

                System.out.print(pieceSymbol(piece) + " ");
            }

            System.out.println(rank);
        }

        printFiles(fileStart, fileEnd, fileStep);
    }

    private static void printFiles(int start, int end, int step) {
        System.out.print("  ");

        for (int file = start; file != end; file += step) {
            char letter = (char) ('a' + file - 1);
            System.out.print(letter + " ");
        }

        System.out.println();
    }

    private static String pieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return ".";
        }

        return switch (piece.getPieceType()) {
            case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♔" : "♚";
            case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♕" : "♛";
            case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♗" : "♝";
            case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♘" : "♞";
            case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♖" : "♜";
            case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♙" : "♟";
        };
    }
}