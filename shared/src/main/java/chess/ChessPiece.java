package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single chess piece.
 * <p>
 * Note: You can add to this class, but you may not alter
 * the signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(
            ChessGame.TeamColor pieceColor,
            ChessPiece.PieceType type
    ) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options.
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all positions this chess piece can move to.
     * Does not account for moves that leave the king in danger.
     *
     * @return collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(
            ChessBoard board,
            ChessPosition myPosition
    ) {
        return switch (type) {
            case BISHOP -> bishopMoves(board, myPosition);
            case KING -> kingMoves(board, myPosition);
            case KNIGHT -> knightMoves(board, myPosition);
            case PAWN -> pawnMoves(board, myPosition);
            case QUEEN -> queenMoves(board, myPosition);
            case ROOK -> rookMoves(board, myPosition);
        };
    }

    private Collection<ChessMove> bishopMoves(
            ChessBoard board,
            ChessPosition start
    ) {
        int[][] directions = {
                {1, 1},
                {1, -1},
                {-1, 1},
                {-1, -1}
        };

        return slidingMoves(board, start, directions);
    }

    private Collection<ChessMove> rookMoves(
            ChessBoard board,
            ChessPosition start
    ) {
        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        return slidingMoves(board, start, directions);
    }

    private Collection<ChessMove> queenMoves(
            ChessBoard board,
            ChessPosition start
    ) {
        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1},
                {1, 1},
                {1, -1},
                {-1, 1},
                {-1, -1}
        };

        return slidingMoves(board, start, directions);
    }

    private Collection<ChessMove> slidingMoves(
            ChessBoard board,
            ChessPosition start,
            int[][] directions
    ) {
        List<ChessMove> moves = new ArrayList<>();

        for (int[] direction : directions) {
            addSlidingDirection(
                    board,
                    start,
                    moves,
                    direction[0],
                    direction[1]
            );
        }

        return moves;
    }

    private void addSlidingDirection(
            ChessBoard board,
            ChessPosition start,
            List<ChessMove> moves,
            int rowChange,
            int colChange
    ) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        while (isOnBoard(row, col)) {
            ChessPosition end = new ChessPosition(row, col);
            ChessPiece pieceAtEnd = board.getPiece(end);

            if (pieceAtEnd == null) {
                moves.add(new ChessMove(start, end, null));
            } else {
                if (pieceAtEnd.getTeamColor() != pieceColor) {
                    moves.add(new ChessMove(start, end, null));
                }

                break;
            }

            row += rowChange;
            col += colChange;
        }
    }

    private Collection<ChessMove> kingMoves(
            ChessBoard board,
            ChessPosition start
    ) {
        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1},
                {1, 1},
                {1, -1},
                {-1, 1},
                {-1, -1}
        };

        return stepMoves(board, start, directions);
    }

    private Collection<ChessMove> knightMoves(
            ChessBoard board,
            ChessPosition start
    ) {
        int[][] directions = {
                {2, 1},
                {2, -1},
                {-2, 1},
                {-2, -1},
                {1, 2},
                {1, -2},
                {-1, 2},
                {-1, -2}
        };

        return stepMoves(board, start, directions);
    }

    private Collection<ChessMove> stepMoves(
            ChessBoard board,
            ChessPosition start,
            int[][] directions
    ) {
        List<ChessMove> moves = new ArrayList<>();

        for (int[] direction : directions) {
            addStepMove(
                    board,
                    start,
                    moves,
                    direction[0],
                    direction[1]
            );
        }

        return moves;
    }

    private void addStepMove(
            ChessBoard board,
            ChessPosition start,
            List<ChessMove> moves,
            int rowChange,
            int colChange
    ) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        if (!isOnBoard(row, col)) {
            return;
        }

        ChessPosition end = new ChessPosition(row, col);
        ChessPiece pieceAtEnd = board.getPiece(end);

        if (pieceAtEnd == null
                || pieceAtEnd.getTeamColor() != pieceColor) {
            moves.add(new ChessMove(start, end, null));
        }
    }

    private Collection<ChessMove> pawnMoves(
            ChessBoard board,
            ChessPosition start
    ) {
        List<ChessMove> moves = new ArrayList<>();

        int direction =
                pieceColor == ChessGame.TeamColor.WHITE ? 1 : -1;

        int startingRow =
                pieceColor == ChessGame.TeamColor.WHITE ? 2 : 7;

        addPawnForwardMoves(
                board,
                start,
                moves,
                direction,
                startingRow
        );

        int captureRow = start.getRow() + direction;

        addPawnCapture(
                board,
                start,
                moves,
                captureRow,
                start.getColumn() - 1
        );

        addPawnCapture(
                board,
                start,
                moves,
                captureRow,
                start.getColumn() + 1
        );

        return moves;
    }

    private void addPawnForwardMoves(
            ChessBoard board,
            ChessPosition start,
            List<ChessMove> moves,
            int direction,
            int startingRow
    ) {
        int oneRowForward = start.getRow() + direction;
        int column = start.getColumn();

        if (!isOnBoard(oneRowForward, column)) {
            return;
        }

        ChessPosition oneForward =
                new ChessPosition(oneRowForward, column);

        if (board.getPiece(oneForward) != null) {
            return;
        }

        addPawnMove(moves, start, oneForward);

        if (start.getRow() != startingRow) {
            return;
        }

        int twoRowsForward = start.getRow() + (2 * direction);
        ChessPosition twoForward =
                new ChessPosition(twoRowsForward, column);

        if (board.getPiece(twoForward) == null) {
            addPawnMove(moves, start, twoForward);
        }
    }

    private void addPawnCapture(
            ChessBoard board,
            ChessPosition start,
            List<ChessMove> moves,
            int row,
            int col
    ) {
        if (!isOnBoard(row, col)) {
            return;
        }

        ChessPosition capturePosition =
                new ChessPosition(row, col);

        ChessPiece pieceAtEnd =
                board.getPiece(capturePosition);

        if (pieceAtEnd != null
                && pieceAtEnd.getTeamColor() != pieceColor) {
            addPawnMove(moves, start, capturePosition);
        }
    }

    private void addPawnMove(
            List<ChessMove> moves,
            ChessPosition start,
            ChessPosition end
    ) {
        boolean isPromotion =
                end.getRow() == 1 || end.getRow() == 8;

        if (isPromotion) {
            moves.add(new ChessMove(start, end, PieceType.QUEEN));
            moves.add(new ChessMove(start, end, PieceType.ROOK));
            moves.add(new ChessMove(start, end, PieceType.BISHOP));
            moves.add(new ChessMove(start, end, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(start, end, null));
        }
    }

    private boolean isOnBoard(int row, int col) {
        return row >= 1
                && row <= 8
                && col >= 1
                && col <= 8;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result
                + ((pieceColor == null) ? 0 : pieceColor.hashCode());

        result = prime * result
                + ((type == null) ? 0 : type.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ChessPiece other = (ChessPiece) obj;

        if (pieceColor != other.pieceColor) {
            return false;
        }

        return type == other.type;
    }
}