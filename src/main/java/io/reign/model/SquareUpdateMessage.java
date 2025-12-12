package io.reign.model;

public class SquareUpdateMessage {
    private String type;
    private Square square;
    private String playerId;
    private long timestamp;

    public SquareUpdateMessage() {
    }

    public SquareUpdateMessage(String type, Square square, String playerId, long timestamp) {
        this.type = type;
        this.square = square;
        this.playerId = playerId;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Square getSquare() {
        return square;
    }

    public void setSquare(Square square) {
        this.square = square;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
