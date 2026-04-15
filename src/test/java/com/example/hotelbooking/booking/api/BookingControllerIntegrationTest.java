package com.example.hotelbooking.booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("in-memory")
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class BookingControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldCreateBookingAndReturnCreatedResponse() throws Exception {
    String requestBody =
        """
        {
          "hotelId": "550e8400-e29b-41d4-a716-446655440000",
          "roomTypeId": "660e8400-e29b-41d4-a716-446655440000",
          "checkIn": "2030-05-10",
          "checkOut": "2030-05-15",
          "guestCount": 2
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bookingId").isNotEmpty())
        .andExpect(jsonPath("$.hotelId").value("550e8400-e29b-41d4-a716-446655440000"))
        .andExpect(jsonPath("$.roomTypeId").value("660e8400-e29b-41d4-a716-446655440000"))
        .andExpect(jsonPath("$.status").value("NEW"));
  }

  @Test
  void shouldCreateBookingAndGetItById() throws Exception {
    String requestBody =
        """
        {
          "hotelId": "550e8400-e29b-41d4-a716-446655440000",
          "roomTypeId": "660e8400-e29b-41d4-a716-446655440000",
          "checkIn": "2030-06-10",
          "checkOut": "2030-06-12",
          "guestCount": 2
        }
        """;

    String response =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String bookingId = extractBookingId(response);

    mockMvc
        .perform(get("/api/v1/bookings/{bookingId}", bookingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookingId").value(bookingId))
        .andExpect(jsonPath("$.status").value("NEW"));
  }

  @Test
  void shouldReturnBadRequestForMalformedUuid() throws Exception {
    String requestBody =
        """
        {
          "hotelId": "invalid-uuid",
          "roomTypeId": "660e8400-e29b-41d4-a716-446655440000",
          "checkIn": "2030-05-10",
          "checkOut": "2030-05-15",
          "guestCount": 2
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
  }

  private String extractBookingId(String responseBody) throws Exception {
    return objectMapper.readTree(responseBody).get("bookingId").asText();
  }
}
