import { useEffect, useState } from "react";
import { bookingApi, type BookingPageResponse } from "../api";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingState } from "../components/LoadingState";
import { StatusBadge } from "../components/StatusBadge";

const DEFAULT_PAGE_SIZE = 10;

/**
 * MyBookingsPage shows bookings owned by the current user.
 *
 * Backend endpoint:
 *
 *   GET /api/v1/bookings/my?page=0&size=10
 *
 * This page is important for the demo UI because booking saga is asynchronous:
 * after starting a saga, the initial response may not be the final business state.
 * The user should be able to refresh this page and see the final booking status.
 */
export function MyBookingsPage() {
  const [bookingsPage, setBookingsPage] = useState<BookingPageResponse | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  async function loadBookings(pageToLoad: number) {
    try {
      setLoading(true);
      setError(null);

      const result = await bookingApi.getMyBookings({
        page: pageToLoad,
        size: DEFAULT_PAGE_SIZE,
      });

      setBookingsPage(result);
      setPage(result.page);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setLoading(false);
    }
  }

  /**
   * Reload bookings when page number changes.
   */
  useEffect(() => {
    loadBookings(page);
  }, [page]);

  function reloadCurrentPage() {
    loadBookings(page);
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
          <p className="eyebrow">Booking service</p>
          <h1>My bookings</h1>
          <p className="page-description">
            Paginated list of bookings owned by the current user. In demo mode this is the fixed
            backend user dev@example.com.
          </p>
        </div>

        <button className="secondary-button" type="button" onClick={reloadCurrentPage}>
          Refresh
        </button>
      </div>

      {loading && <LoadingState text="Loading current user bookings..." />}

      {!loading && error !== null && <ErrorMessage error={error} />}

      {!loading && error === null && bookingsPage !== null && bookingsPage.content.length === 0 && (
        <div className="state-card">
          No bookings found. Go to Hotels, choose a room type and start booking saga.
        </div>
      )}

      {!loading && error === null && bookingsPage !== null && bookingsPage.content.length > 0 && (
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
                  </tr>
                </thead>

                <tbody>
                  {bookingsPage.content.map((booking) => (
                    <tr key={booking.bookingId}>
                      <td>
                        <StatusBadge status={booking.status} />
                      </td>
                      <td>{booking.checkIn}</td>
                      <td>{booking.checkOut}</td>
                      <td>{booking.guestCount}</td>
                      <td>
                        <code className="compact-code">{booking.hotelId}</code>
                      </td>
                      <td>
                        <code className="compact-code">{booking.roomTypeId}</code>
                      </td>
                      <td>
                        <code className="compact-code">{booking.bookingId}</code>
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
    </section>
  );
}