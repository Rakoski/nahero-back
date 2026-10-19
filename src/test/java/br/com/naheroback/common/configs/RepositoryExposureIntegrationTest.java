package br.com.naheroback.common.configs;

import br.com.naheroback.modules.auth.services.JwtService;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class RepositoryExposureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RepositoryResourceMappings resourceMappings;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer studentId;
    private String token;

    @BeforeEach
    void setUp() {
        Role student = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow();
        User user = new User();
        user.setName("Exposure Student");
        user.setEmail("exposure-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setEmailConfirmedAt(LocalDateTime.now().minusDays(1));
        user.setRoles(new HashSet<>(Set.of(student)));
        studentId = userRepository.save(user).getId();
        token = jwtService.generateToken(studentId, List.of(RolesEnum.IS_STUDENT.name()), 2, "access");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    @Test
    @DisplayName("Should not export any Spring Data repository as a REST resource")
    void shouldExportNoRepository() {
        List<String> exported = StreamSupport.stream(new Repositories(applicationContext).spliterator(), false)
                .filter(domainType -> resourceMappings.getMetadataFor(domainType).isExported())
                .map(Class::getSimpleName)
                .toList();

        assertEquals(List.of(), exported);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users", "/questions", "/studentAnswers", "/subscriptions", "/enrollments", "/domains"})
    @DisplayName("Should refuse to list repository collections to a logged-in student")
    void shouldNotServeRepositoryCollections(String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }
}
