package io.reign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinTeamRequest {

    @NotBlank(message = "Team ID is required")
    private String teamId;
}
