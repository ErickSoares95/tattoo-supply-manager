package com.ericksoares.tattoo.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .username(email.substring(0, email.indexOf('@')))
                .email(email)
                .password(passwordEncoder.encode("Senha1234"))
                .fullName("Usuario Login")
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.CLIENT)
                .build());
    }

    private String login(String login, String password) {
        return """
                {"login":"%s","password":"%s"}
                """.formatted(login, password);
    }

    @Test
    void shouldLoginWithTheEmailInAnyCase() throws Exception {

        saveUser("case@teste.com");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("  CASE@Teste.com ", "Senha1234")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAnswer429AfterRepeatedWrongPasswords() throws Exception {

        saveUser("brute@teste.com");

        for (int i = 0; i < LoginAttemptGuard.MAX_FAILURES; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(login("brute@teste.com", "Errada123")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("brute@teste.com", "Senha1234")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many failed login attempts")));
    }

    @Test
    void shouldRefuseATokenIssuedBeforeThePasswordChange() throws Exception {

        User user = saveUser("rotate@teste.com");

        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("rotate@teste.com", "Senha1234")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // A change a couple of seconds after the token was issued (iat has second precision).
        user.setPasswordChangedAt(java.time.Instant.now().plusSeconds(2));
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
