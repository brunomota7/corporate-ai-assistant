package br.com.api_core.modules.auth;

import br.com.api_core.modules.auth.dto.AuthLoginDTO;
import br.com.api_core.modules.auth.dto.AuthRegisterDTO;
import br.com.api_core.modules.auth.dto.AuthResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody AuthRegisterDTO dto) {

        authService.register(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody AuthLoginDTO dto) {

        AuthResponseDTO response = authService.login(dto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
