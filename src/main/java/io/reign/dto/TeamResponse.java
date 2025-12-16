package io.reign.dto;

import io.reign.model.Team;
import io.reign.model.TeamMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private String id;
    private String name;
    private String color;
    private UserResponse creator;
    private Set<TeamMemberResponse> members;
    private String createdAt;

    public static TeamResponse fromTeam(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setColor(team.getColor());
        response.setCreator(UserResponse.fromUser(team.getCreator()));
        response.setCreatedAt(team.getCreatedAt().toString());

        Set<TeamMemberResponse> memberResponses = new java.util.HashSet<>();
        if (team.getMembers() != null) {
            for (TeamMember member : team.getMembers()) {
                memberResponses.add(TeamMemberResponse.fromTeamMember(member));
            }
        }
        response.setMembers(memberResponses);

        return response;
    }
}
