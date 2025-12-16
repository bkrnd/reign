package io.reign.dto;

import io.reign.model.TeamMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {
    private String id;
    private UserResponse user;
    private String joinedAt;
    private String createdAt;

    public static TeamMemberResponse fromTeamMember(TeamMember member) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setId(member.getId());
        response.setUser(UserResponse.fromUser(member.getUser()));
        response.setJoinedAt(member.getJoinedAt().toString());
        response.setCreatedAt(member.getCreatedAt().toString());
        return response;
    }
}
