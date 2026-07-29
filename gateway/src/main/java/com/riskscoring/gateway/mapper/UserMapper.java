package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.entity.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserView toView(AppUser user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getAvatarPath(),
                user.getLanguage()
        );
    }
}