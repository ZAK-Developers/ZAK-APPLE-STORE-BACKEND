package com.zakapplestore.ZAKAppleStore.security;

import com.zakapplestore.ZAKAppleStore.entity.AuthProvider;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.entity.UserRole;
import com.zakapplestore.ZAKAppleStore.entity.UserStatus;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadsGoogleUserWithoutPasswordForJwtValidation() {
        User user = User.builder()
                .username("Reshma")
                .email("reshma@example.com")
                .provider(AuthProvider.GOOGLE)
                .password(null)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmailIgnoreCase("reshma@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("reshma@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("reshma@example.com");
        assertThat(userDetails.getPassword()).isNotBlank();
        assertThat(userDetails.isEnabled()).isTrue();
    }
}
