package com.omkar.resourcebooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omkar.resourcebooking.dto.LoginRequest;
import com.omkar.resourcebooking.dto.ReservationRequest;
import com.omkar.resourcebooking.dto.ResourceRequest;
import com.omkar.resourcebooking.entity.Resource;
import com.omkar.resourcebooking.entity.Role;
import com.omkar.resourcebooking.entity.User;
import com.omkar.resourcebooking.repository.ReservationRepository;
import com.omkar.resourcebooking.repository.ResourceRepository;
import com.omkar.resourcebooking.repository.UserRepository;
import com.omkar.resourcebooking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ResourceBookingApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String user1Token;
    private String user2Token;
    private User testUser1;
    private User testUser2;
    private Resource testResource1;
    private Resource testResource2;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User("admin_test", passwordEncoder.encode("admin123"), Role.ADMIN);
        userRepository.save(admin);
        adminToken = jwtService.generateToken("admin_test", "ADMIN");

        testUser1 = new User("user1_test", passwordEncoder.encode("user123"), Role.USER);
        userRepository.save(testUser1);
        user1Token = jwtService.generateToken("user1_test", "USER");

        testUser2 = new User("user2_test", passwordEncoder.encode("user123"), Role.USER);
        userRepository.save(testUser2);
        user2Token = jwtService.generateToken("user2_test", "USER");

        testResource1 = new Resource("Test Room", "Conference room for testing", "Room", true, new BigDecimal("100.00"));
        resourceRepository.save(testResource1);

        testResource2 = new Resource("Test Car", "EV vehicle for testing", "Vehicle", true, new BigDecimal("250.00"));
        resourceRepository.save(testResource2);
    }

    // 1. Login success
    @Test
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin_test");
        request.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    // 2. Invalid login
    @Test
    void testInvalidLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin_test");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // 3. JWT protected endpoint without token
    @Test
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized());
    }

    // 4. USER can read resources
    @Test
    void testUserCanReadResources() throws Exception {
        mockMvc.perform(get("/resources")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // 5. USER cannot create resource
    @Test
    void testUserCannotCreateResource() throws Exception {
        ResourceRequest request = new ResourceRequest("Unauthorized Room", "Desc", "Room", true, new BigDecimal("50.00"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // 6. USER cannot delete resource
    @Test
    void testUserCannotDeleteResource() throws Exception {
        mockMvc.perform(delete("/resources/" + testResource1.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    // 7. ADMIN can create resource
    @Test
    void testAdminCanCreateResource() throws Exception {
        ResourceRequest request = new ResourceRequest("Admin Room", "Admin Desc", "Room", true, new BigDecimal("120.00"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Admin Room")));
    }

    // 8. ADMIN can update resource
    @Test
    void testAdminCanUpdateResource() throws Exception {
        ResourceRequest request = new ResourceRequest("Updated Room Name", "Updated Desc", "Room", true, new BigDecimal("180.00"));

        mockMvc.perform(put("/resources/" + testResource1.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Room Name")))
                .andExpect(jsonPath("$.price", is(180.00)));
    }

    // 9. ADMIN can delete resource
    @Test
    void testAdminCanDeleteResource() throws Exception {
        mockMvc.perform(delete("/resources/" + testResource2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // 10. USER can create reservation
    @Test
    void testUserCanCreateReservation() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(testResource1.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));
        request.setPrice(new BigDecimal("100.00"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("user1_test")))
                .andExpect(jsonPath("$.resourceId", is(testResource1.getId().intValue())));
    }

    // 11. USER can view own reservations
    @Test
    void testUserCanViewOwnReservations() throws Exception {
        createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("user1_test")));
    }

    // 12. USER cannot view another user's reservation directly
    @Test
    void testUserCannotViewAnotherUserReservation() throws Exception {
        String resId = createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));

        mockMvc.perform(get("/reservations/" + resId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // 13. ADMIN can view all reservations
    @Test
    void testAdminCanViewAllReservations() throws Exception {
        createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));
        createTestReservation(user2Token, testResource2.getId(), new BigDecimal("250.00"));

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // 14. Reservation filtering by price
    @Test
    void testReservationFiltering() throws Exception {
        createTestReservation(adminToken, testResource1.getId(), new BigDecimal("100.00"));
        createTestReservation(adminToken, testResource2.getId(), new BigDecimal("500.00"));

        mockMvc.perform(get("/reservations?minPrice=200")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].price", is(500.00)));
    }

    // 15. Pagination
    @Test
    void testPagination() throws Exception {
        createTestReservation(adminToken, testResource1.getId(), new BigDecimal("100.00"));
        createTestReservation(adminToken, testResource2.getId(), new BigDecimal("200.00"));

        mockMvc.perform(get("/reservations?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    // 16. Sorting
    @Test
    void testSorting() throws Exception {
        createTestReservation(adminToken, testResource1.getId(), new BigDecimal("100.00"));
        createTestReservation(adminToken, testResource2.getId(), new BigDecimal("500.00"));

        mockMvc.perform(get("/reservations?sort=price,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price", is(100.00)))
                .andExpect(jsonPath("$.content[1].price", is(500.00)));
    }

    // 17. Validation errors
    @Test
    void testValidationErrorOnInvalidPrice() throws Exception {
        ResourceRequest request = new ResourceRequest("", "Desc", "", true, new BigDecimal("-50.00"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 18. Missing resource
    @Test
    void testMissingResourceNotFound() throws Exception {
        mockMvc.perform(get("/resources/999999")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    // 19. Invalid reservation times (endTime <= startTime)
    @Test
    void testInvalidReservationTimes() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(testResource1.getId());
        request.setStartTime(LocalDateTime.now().plusHours(5));
        request.setEndTime(LocalDateTime.now().plusHours(2)); // end before start
        request.setPrice(new BigDecimal("100.00"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 20. USER cannot PUT a reservation
    @Test
    void testUserCannotPutReservation() throws Exception {
        String resId = createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));

        ReservationRequest updateRequest = new ReservationRequest();
        updateRequest.setResourceId(testResource1.getId());
        updateRequest.setStartTime(LocalDateTime.now().plusHours(2));
        updateRequest.setEndTime(LocalDateTime.now().plusHours(4));
        updateRequest.setPrice(new BigDecimal("150.00"));

        mockMvc.perform(put("/reservations/" + resId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    // 21. USER cannot DELETE a reservation
    @Test
    void testUserCannotDeleteReservation() throws Exception {
        String resId = createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));

        mockMvc.perform(delete("/reservations/" + resId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    // 22. ADMIN can PUT a reservation
    @Test
    void testAdminCanPutReservation() throws Exception {
        String resId = createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));

        ReservationRequest updateRequest = new ReservationRequest();
        updateRequest.setResourceId(testResource1.getId());
        updateRequest.setStartTime(LocalDateTime.now().plusHours(2));
        updateRequest.setEndTime(LocalDateTime.now().plusHours(4));
        updateRequest.setPrice(new BigDecimal("150.00"));

        mockMvc.perform(put("/reservations/" + resId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(150.00)));
    }

    // 23. ADMIN can DELETE a reservation
    @Test
    void testAdminCanDeleteReservation() throws Exception {
        String resId = createTestReservation(user1Token, testResource1.getId(), new BigDecimal("100.00"));

        mockMvc.perform(delete("/reservations/" + resId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // 24. Invalid sort field returns HTTP 400 Bad Request
    @Test
    void testInvalidSortFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/reservations?sort=invalidColumn,asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));
    }

    // 25. Invalid enum status query parameter returns HTTP 400 Bad Request
    @Test
    void testInvalidEnumQueryParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/reservations?status=INVALID_STATUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value 'INVALID_STATUS'")));
    }

    private String createTestReservation(String token, Long resourceId, BigDecimal price) throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(resourceId);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));
        request.setPrice(price);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
