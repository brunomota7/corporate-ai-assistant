package br.com.api_core.modules.auth;

import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.Role;
import br.com.api_core.domain.repository.UserRepository;
import br.com.api_core.infra.exception.UserAlreadyExistsException;
import br.com.api_core.infra.security.JwtService;
import br.com.api_core.modules.auth.dto.AuthLoginDTO;
import br.com.api_core.modules.auth.dto.AuthRegisterDTO;
import br.com.api_core.modules.auth.dto.AuthResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        AuthRegisterDTO dto = new AuthRegisterDTO("Jhon", "jhon@teste.com", "123teste");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("encoded");

        authService.register(dto);

        verify(userRepository).save(argThat(user ->
                user.getEmail().equals(dto.email()) &&
                        user.getPassword().equals("encoded")
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyExists() {
        AuthRegisterDTO dto = new AuthRegisterDTO("Jhon", "jhon@teste.com", "123teste");

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(dto));
    }

    @Test
    void shouldLoginSuccessfully() {
        AuthLoginDTO dto = new AuthLoginDTO("jhon@teste.com", "123teste");

        User user = new User();
        user.setName("Jhon");
        user.setEmail("jhon@teste.com");
        user.setPassword("encoded");
        user.setRole(Role.ROLE_USER);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token123");

        AuthResponseDTO response = authService.login(dto);

        assertEquals("token123", response.token());
        assertEquals("Jhon", response.name());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        AuthLoginDTO dto = new AuthLoginDTO("jhon@teste.com", "123teste");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(dto));
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {
        AuthLoginDTO dto = new AuthLoginDTO("jhon@teste.com", "123teste");

        User user = new User();
        user.setPassword("encoded");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "encoded")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(dto));
    }
}