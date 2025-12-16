package io.reign.config;

import io.reign.model.User;
import io.reign.model.World;
import io.reign.repository.TeamMemberRepository;
import io.reign.repository.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class WorldPermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return false;
        }

        if (!(targetDomainObject instanceof String)) {
            return false;
        }

        String worldSlug = (String) targetDomainObject;
        User user = (User) authentication.getPrincipal();

        // Find the world by slug
        World world = worldRepository.findBySlug(worldSlug).orElse(null);
        if (world == null) {
            return false;
        }

        // Check if user is a member of any team in this world
        return teamMemberRepository
                .findByUserIdAndWorldId(user.getId(), world.getId())
                .isPresent();
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                String targetType, Object permission) {
        // Not used in this implementation
        return false;
    }
}
