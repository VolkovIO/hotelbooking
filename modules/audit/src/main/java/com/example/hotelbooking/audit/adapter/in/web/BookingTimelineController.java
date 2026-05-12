package com.example.hotelbooking.audit.adapter.in.web;

import com.example.hotelbooking.audit.application.service.BookingTimelineQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Booking timeline",
    description =
        """
        Read-only event timeline for a booking.

        This endpoint is intended for demo and troubleshooting purposes.
        It shows what happened to a booking across the distributed flow:
        booking saga, booking events, payment events, notifications and other future integrations.
        """)
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/timeline")
@RequiredArgsConstructor
class BookingTimelineController {

  private final BookingTimelineQueryService queryService;

  @Operation(
      summary = "Get booking event timeline",
      description =
          """
          Returns a chronological timeline of events related to the booking.

          The timeline is a read model built from integration events consumed by audit-service.
          For the first v0.13.0 step it contains booking-service events from Kafka topic `booking.events`.

          A successful empty response means that the booking exists outside the timeline projection,
          or that audit-service has not consumed related events yet.
          """,
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Booking timeline returned successfully",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = TimelineEventResponse.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid bookingId format",
            content = @Content)
      })
  @GetMapping
  List<TimelineEventResponse> findByBookingId(
      @Parameter(
              description =
                  "Booking identifier whose distributed event timeline should be returned",
              example = "11111111-1111-1111-1111-111111111111",
              required = true)
          @PathVariable
          UUID bookingId) {
    return queryService.findByBookingId(bookingId).stream()
        .map(TimelineEventResponse::fromDomain)
        .toList();
  }
}
