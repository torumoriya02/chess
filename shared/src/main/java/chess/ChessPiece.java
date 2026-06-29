package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
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
     * @return Which team this chess piece belongs to
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
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ChessPiece piece = board.getPiece(myPosition);
        if (piece.getPieceType() == PieceType.BISHOP) {
            return bishopMoves(board, myPosition);

        }

        if (piece.getPieceType() == PieceType.KING) {
            return kingMoves(board, myPosition);

        }
        if (piece.getPieceType() == PieceType.KNIGHT) {
            return knightMoves(board, myPosition);

        }

        if (piece.getPieceType() == PieceType.PAWN) {
            return pawnMoves(board, myPosition);

        }

        if (piece.getPieceType() == PieceType.QUEEN) {
            return queenMoves(board, myPosition);

        }

        if (piece.getPieceType() == PieceType.ROOK) {
            return rookMoves(board, myPosition);

        }
        return List.of();
    }
    private Collection<ChessMove> bishopMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        addBishopDirection(board, start, moves, 1, 1);
        addBishopDirection(board, start, moves, 1, -1);
        addBishopDirection(board, start, moves, -1, 1);
        addBishopDirection(board, start, moves, -1, -1);

        return moves;
    }

    private void addBishopDirection(ChessBoard board, ChessPosition start, List<ChessMove> moves, int rowChange, int colChange) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        while (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
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

    private Collection<ChessMove> kingMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        addKingDirection(board, start, moves, 1, 0);
        addKingDirection(board, start, moves, 0, 1);
        addKingDirection(board, start, moves, -1, 0);
        addKingDirection(board, start, moves, 0, -1);

        addKingDirection(board, start, moves, 1, 1);
        addKingDirection(board, start, moves, 1, -1);
        addKingDirection(board, start, moves, -1, 1);
        addKingDirection(board, start, moves, -1, -1);

        return moves;
    }

    private void addKingDirection(ChessBoard board, ChessPosition start, List<ChessMove> moves, int rowChange, int colChange) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        if (row < 1 || row > 8 || col < 1 || col > 8) {
            return;
        }

            ChessPosition end = new ChessPosition(row, col);
            ChessPiece pieceAtEnd = board.getPiece(end);

        if (pieceAtEnd == null || pieceAtEnd.getTeamColor() != pieceColor) {
            moves.add(new ChessMove(start, end, null));
        }

    }

    private Collection<ChessMove> knightMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        addKnightDirection(board, start, moves, 2, 1);
        addKnightDirection(board, start, moves, 1, 2);
        addKnightDirection(board, start, moves, -1, 2);
        addKnightDirection(board, start, moves, -2, 1);

        addKnightDirection(board, start, moves, 1, -2);
        addKnightDirection(board, start, moves, 2, -1);
        addKnightDirection(board, start, moves, -1, -2);
        addKnightDirection(board, start, moves, -2, -1); 

        return moves;
    }

    private void addKnightDirection(ChessBoard board, ChessPosition start, List<ChessMove> moves, int rowChange, int colChange) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        if (row < 1 || row > 8 || col < 1 || col > 8) {
            return;
        }

            ChessPosition end = new ChessPosition(row, col);
            ChessPiece pieceAtEnd = board.getPiece(end);

        if (pieceAtEnd == null || pieceAtEnd.getTeamColor() != pieceColor) {
            moves.add(new ChessMove(start, end, null));
        }

    }

    private Collection<ChessMove> pawnMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        int direction = pieceColor == ChessGame.TeamColor.WHITE ? 1 : -1;
        int startRow = pieceColor == ChessGame.TeamColor.WHITE ? 2 : 7;

        int oneRow = start.getRow() + direction;
        int col = start.getColumn();

        if (oneRow >= 1 && oneRow <= 8) {
            ChessPosition oneForward = new ChessPosition(oneRow, col);

            if (board.getPiece(oneForward) == null) {
                addPawnMove(moves, start, oneForward);

                if (start.getRow() == startRow) {
                    int twoRow = start.getRow() + (2 * direction);
                    ChessPosition twoForward = new ChessPosition(twoRow, col);

                    if (board.getPiece(twoForward) == null) {
                        addPawnMove(moves, start, twoForward);
                    }
                }
            }
        }

        addPawnCapture(board, start, moves, oneRow, col - 1);
        addPawnCapture(board, start, moves, oneRow, col + 1);

        return moves;
    }

    private void addPawnCapture(ChessBoard board, ChessPosition start,
                                List<ChessMove> moves, int row, int col) {
        if (row < 1 || row > 8 || col < 1 || col > 8) {
            return;
        }

        ChessPosition capturePosition = new ChessPosition(row, col);
        ChessPiece pieceAtEnd = board.getPiece(capturePosition);

        if (pieceAtEnd != null && pieceAtEnd.getTeamColor() != pieceColor) {
            addPawnMove(moves, start, capturePosition);
        }
    }

    private void addPawnMove(List<ChessMove> moves, ChessPosition start, ChessPosition end) {
        if (end.getRow() == 8 || end.getRow() == 1) {
            moves.add(new ChessMove(start, end, PieceType.QUEEN));
            moves.add(new ChessMove(start, end, PieceType.ROOK));
            moves.add(new ChessMove(start, end, PieceType.BISHOP));
            moves.add(new ChessMove(start, end, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(start, end, null));
        }
    }


    private Collection<ChessMove> queenMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        addBishopDirection(board, start, moves, 1, 0);
        addBishopDirection(board, start, moves, 0, 1);
        addBishopDirection(board, start, moves, -1, 0);
        addBishopDirection(board, start, moves, 0, -1);

        addBishopDirection(board, start, moves, 1, 1);
        addBishopDirection(board, start, moves, 1, -1);
        addBishopDirection(board, start, moves, -1, 1);
        addBishopDirection(board, start, moves, -1, -1);

        return moves;
    }

    private void addQueenDirection(ChessBoard board, ChessPosition start, List<ChessMove> moves, int rowChange, int colChange) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        while (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
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

    private Collection<ChessMove> rookMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        addBishopDirection(board, start, moves, 1, 0);
        addBishopDirection(board, start, moves, 0, 1);
        addBishopDirection(board, start, moves, -1, 0);
        addBishopDirection(board, start, moves, 0, -1);

        return moves;
    }

    private void addRookDirection(ChessBoard board, ChessPosition start, List<ChessMove> moves, int rowChange, int colChange) {
        int row = start.getRow() + rowChange;
        int col = start.getColumn() + colChange;

        while (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pieceColor == null) ? 0 : pieceColor.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChessPiece other = (ChessPiece) obj;
        if (pieceColor != other.pieceColor)
            return false;
        if (type != other.type)
            return false;
        return true;
    }

}
