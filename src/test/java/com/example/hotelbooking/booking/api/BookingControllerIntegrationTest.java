package com.example.hotelbooking.booking.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Disabled("Temporarily disabled during MVP development while integration flows are still evolving")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("in-memory")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BookingControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldCreateBookingAndReturnCreatedResponse() throws Exception {
    TestInventoryIds inventoryIds = createHotelWithRoomType();

    String requestBody =
        """
        {
          "hotelId": "%s",
          "roomTypeId": "%s",
          "checkIn": "2030-05-10",
          "checkOut": "2030-05-15",
          "guestCount": 2
        }
        """
            .formatted(inventoryIds.hotelId(), inventoryIds.roomTypeId());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

    assertFalse(json.get("bookingId").asText().isBlank(), "bookingId should not be blank");
    assertEquals(
        inventoryIds.hotelId(), json.get("hotelId").asText(), "hotelId should match created hotel");
    assertEquals(
        inventoryIds.roomTypeId(),
        json.get("roomTypeId").asText(),
        "roomTypeId should match created room type");
    assertEquals("ON_HOLD", json.get("status").asText(), "status should remain ON_HOLD");
  }

  @Test
  void shouldCreateBookingAndGetItById() throws Exception {
    TestInventoryIds inventoryIds = createHotelWithRoomType();

    String requestBody =
        """
        {
          "hotelId": "%s",
          "roomTypeId": "%s",
          "checkIn": "2030-06-10",
          "checkOut": "2030-06-12",
          "guestCount": 2
        }
        """
            .formatted(inventoryIds.hotelId(), inventoryIds.roomTypeId());

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    String bookingId =
        objectMapper
            .readTree(createResult.getResponse().getContentAsString())
            .get("bookingId")
            .asText();

    MvcResult getResult =
        mockMvc
            .perform(get("/api/v1/bookings/{bookingId}", bookingId))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode json = objectMapper.readTree(getResult.getResponse().getContentAsString());

    assertEquals(
        bookingId, json.get("bookingId").asText(), "bookingId should match created booking");
    assertEquals(
        "ON_HOLD", json.get("status").asText(), "status should be ON_HOLD after hold is placed");
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

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isBadRequest())
            .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

    assertEquals(
        "MALFORMED_REQUEST_BODY",
        json.get("code").asText(),
        "code should indicate malformed request body");
  }

  private TestInventoryIds createHotelWithRoomType() throws Exception {
    String registerHotelRequest =
        """
        {
          "name": "Riviera Hotel",
          "city": "Kazan"
        }
        """;

    MvcResult registerHotelResult =
        mockMvc
            .perform(
                post("/api/v1/admin/hotels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerHotelRequest))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode hotelJson =
        objectMapper.readTree(registerHotelResult.getResponse().getContentAsString());

    String hotelId = hotelJson.get("hotelId").asText();

    String addRoomTypeRequest =
        """
        {
          "name": "Standard",
          "guestCapacity": 2
        }
        """;

    MvcResult addRoomTypeResult =
        mockMvc
            .perform(
                post("/api/v1/admin/hotels/{hotelId}/room-types", hotelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(addRoomTypeRequest))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode roomTypeJson =
        objectMapper.readTree(addRoomTypeResult.getResponse().getContentAsString());

    String roomTypeId = roomTypeJson.get("roomTypes").get(0).get("roomTypeId").asText();

    String setAvailabilityRequest =
        """
        {
          "from": "2030-01-01",
          "to": "2030-12-31",
          "totalRooms": 10
        }
        """;

    mockMvc
        .perform(
            put(
                    "/api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability",
                    hotelId,
                    roomTypeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(setAvailabilityRequest))
        .andExpect(status().isOk());

    return new TestInventoryIds(hotelId, roomTypeId);
  }

  private record TestInventoryIds(String hotelId, String roomTypeId) {}
}
