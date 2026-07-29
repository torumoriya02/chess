package chess;

public class ChessMove {

    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;

    public ChessMove(
            ChessPosition startPosition,
            ChessPosition endPosition,
            ChessPiece.PieceType promotionPiece
    ) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
    }
    public ChessPosition getStartPosition() {
        return startPosition;
    }

    public ChessPosition getEndPosition() {
        return endPosition;
    }

    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public String toString() {
        return String.format("%s%s", startPosition, endPosition);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime * result
                + ((startPosition == null) ? 0 : startPosition.hashCode());

        result = prime * result
                + ((endPosition == null) ? 0 : endPosition.hashCode());

        result = prime * result
                + ((promotionPiece == null) ? 0 : promotionPiece.hashCode());

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

        ChessMove other = (ChessMove) obj;

        if (startPosition == null) {
            if (other.startPosition != null) {
                return false;
            }
        } else if (!startPosition.equals(other.startPosition)) {
            return false;
        }

        if (endPosition == null) {
            if (other.endPosition != null) {
                return false;
            }
        } else if (!endPosition.equals(other.endPosition)) {
            return false;
        }

        return promotionPiece == other.promotionPiece;
    }
}