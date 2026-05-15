import { useEffect, useState } from "react";
import {
  auditApi,
  bookingApi,
  type BookingPageResponse,
  type BookingStatus,
  type TimelineEventResponse,
  type UUID,
} from "../api";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingState } from "../components/LoadingState";
import { StatusBadge } from "../components/StatusBadge";

const DEFAULT_PAGE_SIZE = 10;

/**
 * MyBookingsPage shows bookings owned by the current user and allows:
 *
 * - opening audit timeline
 * - cancelling active bookings
 *
 * Backend endpoints:
 *
 *   GET  /api/v1/bookings/my?page=0&size=10
 *   GET  /api/v1/bookings/{bookingId}/timeline
 *   POST /api/v1/bookings/{bookingId}/cancel
 *
 * This page is important for the demo because it closes the basic booking lifecycle:
 *
 *   create saga -> observe status/timeline -> cancel booking -> observe BookingCancelled
 */
export function MyBookingsPage() {
  const [bookingsPage, setBookingsPage] = useState<BookingPageResponse | null>(null);
  const [page, setPage] = useState(0);
  const [bookingsLoading, setBookingsLoading] = useState(true);
  const [bookingsError, setBookingsError] = useState<unknown>(null);

  const [selectedBookingId, setSelectedBookingId] = useState<UUID | null>(null);
  const [timeline, setTimeline] = useState<TimelineEventResponse[]>([]);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [timelineError, setTimelineError] = useState<unknown>(null);

  const [cancellingBookingId, setCancellingBookingId] = useState<UUID | null>(null);
  const [cancellationError, setCancellationError] = useState<unknown>(null);
  const [cancellationMessage, setCancellationMessage] = useState<string | null>(null);

  async function loadBookings(pageToLoad: number) {
    try {
      setBookingsLoading(true);
      setBookingsError(null);

      const result = await bookingApi.getMyBookings({
        page: pageToLoad,
        size: DEFAULT_PAGE_SIZE,
      });

      setBookingsPage(result);
      setPage(result.page);
    } catch (requestError) {
      setBookingsError(requestError);
    } finally {
      setBookingsLoading(false);
    }
  }

  /**
   * Reload bookings when page number changes.
   */
  useEffect(() => {
    loadBookings(page);
  }, [page]);

  async function openTimeline(bookingId: UUID) {
    try {
      setSelectedBookingId(bookingId);
      setTimeline([]);
      setTimelineError(null);
      setTimelineLoading(true);

      const result = await auditApi.getBookingTimeline(bookingId);

      setTimeline(result);
    } catch (requestError) {
      setTimelineError(requestError);
    } finally {
      setTimelineLoading(false);
    }
  }

  async function cancelBooking(bookingId: UUID) {
    /**
     * window.confirm is enough for this demo UI.
     *
     * Later we can replace it with a nicer modal, but for a senior backend demo
     * the important part is the integration with booking-service and timeline refresh.
     */
    const confirmed = window.confirm("Cancel this booking?");

    if (!confirmed) {
      return;
    }

    try {
      setCancellingBookingId(bookingId);
      setCancellationError(null);
      setCancellationMessage(null);

      const cancelledBooking = await bookingApi.cancelBooking(bookingId);

      setCancellationMessage(`Booking ${cancelledBooking.bookingId} cancelled.`);

      await loadBookings(page);

      if (selectedBookingId === bookingId) {
        await openTimeline(bookingId);
      }
    } catch (requestError) {
      setCancellationError(requestError);
    } finally {
      setCancellingBookingId(null);
    }
  }

  function reloadCurrentPage() {
    loadBookings(page);

    if (selectedBookingId !== null) {
      openTimeline(selectedBookingId);
    }
  }

  function goToPreviousPage() {
    setPage((currentPage) => Math.max(currentPage - 1, 0));
  }

  function goToNextPage() {
    if (bookingsPage !== null && !bookingsPage.last) {
      setPage((currentPage) => currentPage + 1);
    }
  }

  return (
    <section className="page-section">
      <div className="page-header">
        <div>
          <p className="eyebrow">Booking service + audit service</p>
          <h1>My bookings</h1>
          <p className="page-description">
            Paginated list of current user bookings. Select a booking to inspect
            the cross-service audit timeline or cancel an active booking.
          </p>
        </div>

        <button className="secondary-button" type="button" onClick={reloadCurrentPage}>
          Refresh
        </button>
      </div>

      {cancellationMessage !== null && (
        <div className="state-card state-card-success">{cancellationMessage}</div>
      )}

      {cancellationError !== null && <ErrorMessage error={cancellationError} />}

      {bookingsLoading && <LoadingState text="Loading current user bookings..." />}

      {!bookingsLoading && bookingsError !== null && <ErrorMessage error={bookingsError} />}

      {!bookingsLoading &&
        bookingsError === null &&
        bookingsPage !== null &&
        bookingsPage.content.length === 0 && (
          <div className="state-card">
            No bookings found. Go to Hotels, choose a room type and start booking saga.
          </div>
        )}

      {!bookingsLoading &&
        bookingsError === null &&
        bookingsPage !== null &&
        bookingsPage.content.length > 0 && (
          <>
            <div className="bookings-table-card">
              <div className="table-header">
                <h3>Bookings</h3>
                <p className="muted-text">
                  Total: {bookingsPage.totalElements}, page {bookingsPage.page + 1} of{" "}
                  {Math.max(bookingsPage.totalPages, 1)}
                </p>
              </div>

              <div className="responsive-table">
                <table>
                  <thead>
                    <tr>
                      <th>Status</th>
                      <th>Check-in</th>
                      <th>Check-out</th>
                      <th>Guests</th>
                      <th>Hotel</th>
                      <th>Room type</th>
                      <th>Booking ID</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {bookingsPage.content.map((booking) => (
                      <tr
                        className={
                          selectedBookingId === booking.bookingId ? "selected-table-row" : undefined
                        }
                        key={booking.bookingId}
                      >
                        <td>
                          <StatusBadge status={booking.status} />
                        </td>
                        <td>{booking.checkIn}</td>
                        <td>{booking.checkOut}</td>
                        <td>{booking.guestCount}</td>
                        <td>
                          <code className="compact-code" title={booking.hotelId}>
                            {booking.hotelId}
                          </code>
                        </td>
                        <td>
                          <code className="compact-code" title={booking.roomTypeId}>
                            {booking.roomTypeId}
                          </code>
                        </td>
                        <td>
                          <code className="compact-code" title={booking.bookingId}>
                            {booking.bookingId}
                          </code>
                        </td>
                        <td>
                          <div className="row-actions">
                            <button
                              className="small-action-button"
                              type="button"
                              onClick={() => openTimeline(booking.bookingId)}
                            >
                              Timeline
                            </button>

                            <button
                              className="small-action-button small-action-button-danger"
                              type="button"
                              disabled={
                                !canCancelBooking(booking.status) ||
                                cancellingBookingId === booking.bookingId
                              }
                              onClick={() => cancelBooking(booking.bookingId)}
                            >
                              {cancellingBookingId === booking.bookingId ? "Cancelling..." : "Cancel"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="pagination-bar">
              <button
                className="secondary-button"
                type="button"
                disabled={bookingsPage.first}
                onClick={goToPreviousPage}
              >
                Previous
              </button>

              <span>
                Page <strong>{bookingsPage.page + 1}</strong> /{" "}
                <strong>{Math.max(bookingsPage.totalPages, 1)}</strong>
              </span>

              <button
                className="secondary-button"
                type="button"
                disabled={bookingsPage.last}
                onClick={goToNextPage}
              >
                Next
              </button>
            </div>
          </>
        )}

      <TimelinePanel
        selectedBookingId={selectedBookingId}
        timeline={timeline}
        loading={timelineLoading}
        error={timelineError}
      />
    </section>
  );
}

function canCancelBooking(status: BookingStatus): boolean {
  return status === "ON_HOLD" || status === "CONFIRMED";
}

type TimelinePanelProps = {
  selectedBookingId: UUID | null;
  timeline: TimelineEventResponse[];
  loading: boolean;
  error: unknown;
};

/**
 * TimelinePanel renders audit events for one selected booking.
 *
 * The audit-service timeline is useful because it shows the distributed story,
 * not just the final booking status.
 */
function TimelinePanel({ selectedBookingId, timeline, loading, error }: TimelinePanelProps) {
  if (selectedBookingId === null) {
    return (
      <div className="timeline-panel timeline-panel-empty">
        <p className="eyebrow">Audit timeline</p>
        <h2>Select a booking</h2>
        <p className="muted-text">
          Click Timeline in the bookings table to see booking and payment events.
        </p>
      </div>
    );
  }

  return (
    <div className="timeline-panel">
      <div className="timeline-header">
        <div>
          <p className="eyebrow">Audit timeline</p>
          <h2>Booking lifecycle</h2>
          <p className="muted-text">
            Events recorded by audit-service for booking{" "}
            <code className="inline-code">{selectedBookingId}</code>
          </p>
        </div>
      </div>

      {loading && <LoadingState text="Loading booking timeline..." />}

      {!loading && error !== null && <ErrorMessage error={error} />}

      {!loading && error === null && timeline.length === 0 && (
        <div className="state-card">
          No timeline events found yet. Wait a few seconds and click Refresh.
        </div>
      )}

      {!loading && error === null && timeline.length > 0 && (
        <ol className="timeline-list">
          {timeline.map((event) => (
            <TimelineItem event={event} key={event.eventId} />
          ))}
        </ol>
      )}
    </div>
  );
}

type TimelineItemProps = {
  event: TimelineEventResponse;
};

function TimelineItem({ event }: TimelineItemProps) {
  return (
    <li className="timeline-item">
      <div className="timeline-marker" />

      <article className="timeline-card">
        <div className="timeline-card-header">
          <div>
            <h3>{event.eventType}</h3>
            <p className="muted-text">
              {event.source} · {event.aggregateType}
            </p>
          </div>

          <time>{formatInstant(event.occurredAt)}</time>
        </div>

        <div className="timeline-meta-grid">
          <MetaItem label="eventId" value={event.eventId} />
          <MetaItem label="aggregateId" value={event.aggregateId} />
          <MetaItem label="correlationId" value={event.correlationId ?? "none"} />
          <MetaItem label="recordedAt" value={formatInstant(event.recordedAt)} />
        </div>

        <details className="payload-details">
          <summary>Payload</summary>
          <pre>{JSON.stringify(event.payload, null, 2)}</pre>
        </details>
      </article>
    </li>
  );
}

type MetaItemProps = {
  label: string;
  value: string;
};

function MetaItem({ label, value }: MetaItemProps) {
  return (
    <div className="meta-item">
      <span>{label}</span>
      <code title={value}>{value}</code>
    </div>
  );
}

function formatInstant(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "medium",
  }).format(date);
}