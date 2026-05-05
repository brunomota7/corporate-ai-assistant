package br.com.api_core.modules.auth;

import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.Role;
import br.com.api_core.domain.repository.UserRepository;
import br.com.api_core.infra.exception.UserAlreadyExistsException;
import br.com.api_core.infra.messaging.EventPublisher;
import br.com.api_core.infra.messaging.dto.UserRegisteredEventDTO;
import br.com.api_core.infra.security.JwtService;
import br.com.api_core.modules.auth.dto.AuthLoginDTO;
import br.com.api_core.modules.auth.dto.AuthRegisterDTO;
import br.com.api_core.modules.auth.dto.AuthResponseDTO;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       BCryptPasswordEncoder passwordEncoder,
                       EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(AuthRegisterDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException(dto.email());
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.ROLE_USER);
        user.setActive(true);

        userRepository.save(user);

        eventPublisher.publishUserRegistered(new UserRegisteredEventDTO(
                user.getId().toString(),
                user.getName(),
                user.getEmail()
        ));
    }

    public AuthResponseDTO login(AuthLoginDTO dto) {

        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() ->
                    new BadCredentialsException("Invalid email or password")
                );

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponseDTO(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole().toString()
        );
    }
}
