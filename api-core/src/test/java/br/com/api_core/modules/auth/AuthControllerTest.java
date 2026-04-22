package br.com.api_core.modules.auth;

import br.com.api_core.infra.security.JwtFilter;
import br.com.api_core.infra.security.JwtService;
import br.com.api_core.modules.auth.dto.AuthLoginDTO;
import br.com.api_core.modules.auth.dto.AuthRegisterDTO;
import br.com.api_core.modules.auth.dto.AuthResponseDTO;
import br.com.api_core.support.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUser() throws Exception {
        AuthRegisterDTO dto = new AuthRegisterDTO("Jhon", "jhon@teste.com", "12345678");

        doNothing().when(authService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andDo(print());

        verify(authService).register(any());
    }

    @Test
    void shouldLoginUser() throws Exception {
        AuthLoginDTO dto = new AuthLoginDTO("jhon@teste.com", "12345678");

        AuthResponseDTO response = new AuthResponseDTO(
                "token123",
                "Jhon",
                "jhon@teste.com",
                "ROLE_USER"
        );

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.name").value("Jhon"));
    }

    @Test
    void shouldReturnBadRequestWhenInvalidBody() throws Exception {
        AuthRegisterDTO dto = new AuthRegisterDTO("", "not-an-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}