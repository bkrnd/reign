package io.reign.model;

import java.util.List;
import java.util.Set;

public class SquareUpdateMessage {
    private String type;
    private List<Square> board;
    private Set<Team> teams;
    private String playerId;
    private long timestamp;

    public SquareUpdateMessage() {
    }

    public SquareUpdateMessage(String type, List<Square> board, Set<Team> teams, String playerId, long timestamp) {
        this.type = type;
        this.board = board;
        this.teams = teams;
        this.playerId = playerId;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Square> getBoard() {
        return board;
    }

    public void setBoard(List<Square> board) {
        this.board = board;
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

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }
}
