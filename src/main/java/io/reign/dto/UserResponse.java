package io.reign.dto;

import io.reign.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String userType;
    private String createdAt;

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        response.setCreatedAt(user.getCreatedAt().toString());
        return response;
    }
}
