package com.ericksoares.tattoo.user.presentation;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void createUser() {
        User user = User.builder()
                .username("loginuser")
                .email("login@teste.com")
                .password(passwordEncoder.encode("Senha1234"))
                .fullName("Usuario Login")
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.CLIENT)
                .build();

        userRepository.save(user);
    }

    @Test
    void shouldLoginSuccessfullyWithCorrectCredentials() throws Exception {

        createUser();

        String body = """
                {"login":"login@teste.com","password":"Senha1234"}
                """;

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login@teste.com"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {

        createUser();

        String body = """
                {"login":"login@teste.com","password":"SenhaErrada1"}
                """;

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid login or password"));
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginDoesNotExist() throws Exception {

        String body = """
                {"login":"naoexiste@teste.com","password":"Senha1234"}
                """;

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid login or password"));
    }

    @Test
    void shouldLoginSuccessfullyWithCpf() throws Exception {

        User user = User.builder()
                .username("cpfloginuser")
                .email("cpflogin@teste.com")
                .password(passwordEncoder.encode("Senha1234"))
                .fullName("Usuario Login CPF")
                .cpf("98765432100")
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.CLIENT)
                .build();

        userRepository.save(user);

        String body = """
                {"login":"98765432100","password":"Senha1234"}
                """;

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("cpflogin@teste.com"));
    }
}
