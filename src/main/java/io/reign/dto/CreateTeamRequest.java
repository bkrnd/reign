package io.reign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 50, message = "Team name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Team color is required")
    @Pattern(regexp = "^(red|blue|green|yellow|purple|teal)$", message = "Color must be one of: red, blue, green, yellow, purple, teal")
    private String color;
}
