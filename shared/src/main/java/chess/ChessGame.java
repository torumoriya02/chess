package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A class that can manage a chess game, making moves on a board.
 * <p>
 * Note: You can add to this class, but you may not alter
 * the signature of the existing methods.
 */
public class ChessGame {

    private TeamColor teamTurn;
    private ChessBoard board;

    public ChessGame() {
        teamTurn = TeamColor.WHITE;
        board = new ChessBoard();
        board.resetBoard();
    }

    /**
     * @return which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Sets which team's turn it is.
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the two possible teams in a chess game.
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets all valid moves for a piece at the given location.
     *
     * @param startPosition the piece to get valid moves for
     * @return valid moves, or null if there is no piece at the position
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            return null;
        }

        Collection<ChessMove> legalMoves = new ArrayList<>();

        for (ChessMove move : piece.pieceMoves(board, startPosition)) {
            ChessBoard testBoard = copyBoard(board);
            ChessPiece testPiece =
                    testBoard.getPiece(move.getStartPosition());

            testBoard.addPiece(move.getStartPosition(), null);
            applyMoveToBoard(testBoard, move, testPiece);

            ChessGame testGame = new ChessGame();
            testGame.setBoard(testBoard);

            if (!testGame.isInCheck(piece.getTeamColor())) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    private void applyMoveToBoard(
            ChessBoard targetBoard,
            ChessMove move,
            ChessPiece piece
    ) {
        if (move.getPromotionPiece() != null) {
            targetBoard.addPiece(
                    move.getEndPosition(),
                    new ChessPiece(
                            piece.getTeamColor(),
                            move.getPromotionPiece()
                    )
            );
        } else {
            targetBoard.addPiece(move.getEndPosition(), piece);
        }
    }

    /**
     * Makes a move in the chess game.
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if the move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = board.getPiece(move.getStartPosition());

        if (piece == null) {
            throw new InvalidMoveException("No piece");
        }

        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Wrong turn");
        }

        Collection<ChessMove> moves =
                validMoves(move.getStartPosition());

        if (moves == null || !moves.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }

        board.addPiece(move.getStartPosition(), null);
        applyMoveToBoard(board, move, piece);
        teamTurn = oppositeTeam(teamTurn);
    }

    /**
     * Determines if the given team is in check.
     *
     * @param teamColor team to check
     * @return true if the team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findKing(teamColor);

        if (kingPosition == null) {
            return false;
        }

        TeamColor enemyColor = oppositeTeam(teamColor);

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position =
                        new ChessPosition(row, col);

                if (pieceAttacksKing(
                        position,
                        enemyColor,
                        kingPosition
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean pieceAttacksKing(
            ChessPosition position,
            TeamColor enemyColor,
            ChessPosition kingPosition
    ) {
        ChessPiece piece = board.getPiece(position);

        if (piece == null || piece.getTeamColor() != enemyColor) {
            return false;
        }

        for (ChessMove move : piece.pieceMoves(board, position)) {
            if (move.getEndPosition().equals(kingPosition)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if the given team is in checkmate.
     *
     * @param teamColor team to check
     * @return true if the team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor)
                && !hasAnyLegalMove(teamColor);
    }

    /**
     * Determines if the given team is in stalemate.
     *
     * @param teamColor team to check
     * @return true if the team is in stalemate
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor)
                && !hasAnyLegalMove(teamColor);
    }

    /**
     * Sets this game's chessboard.
     *
     * @param board the new board
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * @return the current chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    private ChessPosition findKing(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position =
                        new ChessPosition(row, col);

                ChessPiece piece = board.getPiece(position);

                if (isKingForTeam(piece, teamColor)) {
                    return position;
                }
            }
        }

        return null;
    }

    private boolean isKingForTeam(
            ChessPiece piece,
            TeamColor teamColor
    ) {
        return piece != null
                && piece.getTeamColor() == teamColor
                && piece.getPieceType()
                == ChessPiece.PieceType.KING;
    }

    private boolean hasAnyLegalMove(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position =
                        new ChessPosition(row, col);

                if (hasLegalMoveAtPosition(position, teamColor)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasLegalMoveAtPosition(
            ChessPosition position,
            TeamColor teamColor
    ) {
        ChessPiece piece = board.getPiece(position);

        if (piece == null || piece.getTeamColor() != teamColor) {
            return false;
        }

        Collection<ChessMove> moves = validMoves(position);

        return moves != null && !moves.isEmpty();
    }

    private ChessBoard copyBoard(ChessBoard originalBoard) {
        ChessBoard newBoard = new ChessBoard();

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position =
                        new ChessPosition(row, col);

                copyPieceAtPosition(
                        originalBoard,
                        newBoard,
                        position
                );
            }
        }

        return newBoard;
    }

    private void copyPieceAtPosition(
            ChessBoard originalBoard,
            ChessBoard newBoard,
            ChessPosition position
    ) {
        ChessPiece piece = originalBoard.getPiece(position);

        if (piece != null) {
            newBoard.addPiece(
                    position,
                    new ChessPiece(
                            piece.getTeamColor(),
                            piece.getPieceType()
                    )
            );
        }
    }

    private TeamColor oppositeTeam(TeamColor teamColor) {
        if (teamColor == TeamColor.WHITE) {
            return TeamColor.BLACK;
        }

        return TeamColor.WHITE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ChessGame other)) {
            return false;
        }

        if (teamTurn != other.teamTurn) {
            return false;
        }

        return boardsAreEqual(other.board);
    }

    private boolean boardsAreEqual(ChessBoard otherBoard) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position =
                        new ChessPosition(row, col);

                ChessPiece thisPiece =
                        board.getPiece(position);

                ChessPiece otherPiece =
                        otherBoard.getPiece(position);

                if (!Objects.equals(thisPiece, otherPiece)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(teamTurn);

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position =
                        new ChessPosition(row, col);

                ChessPiece piece = board.getPiece(position);
                result = 31 * result + Objects.hashCode(piece);
            }
        }

        return result;
    }
}