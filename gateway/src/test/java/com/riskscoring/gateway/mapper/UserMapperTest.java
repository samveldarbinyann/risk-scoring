package com.riskscoring.gateway.mapper;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toViewMapsAllFieldsFromUser() {
        Instant now = Instant.now();
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID())
                .username("jane")
                .email("jane@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .language(Language.RU)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserView view = mapper.toView(user);

        assertThat(view).isEqualTo(new UserView(user.getId(), "jane", "Jane", "Doe", "jane@example.com",
                UserRole.ADMIN, UserStatus.ACTIVE, Language.RU));
    }
}
