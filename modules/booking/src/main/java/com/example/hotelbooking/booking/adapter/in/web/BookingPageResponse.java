package com.example.hotelbooking.booking.adapter.in.web;

import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.domain.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Paged booking response")
public record BookingPageResponse(
    @Schema(description = "Bookings on the current page") List<BookingResponse> content,
    @Schema(description = "Zero-based page number", example = "0") int page,
    @Schema(description = "Requested page size", example = "20") int size,
    @Schema(description = "Total number of matching bookings", example = "42") long totalElements,
    @Schema(description = "Total number of pages", example = "3") int totalPages,
    @Schema(description = "Whether this is the first page", example = "true") boolean first,
    @Schema(description = "Whether this is the last page", example = "false") boolean last) {

  public BookingPageResponse {
    Objects.requireNonNull(content, "content must not be null");
    content = List.copyOf(content);
  }

  public static BookingPageResponse from(PagedResult<Booking> result) {
    return new BookingPageResponse(
        result.content().stream().map(BookingResponse::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.first(),
        result.last());
  }
}
