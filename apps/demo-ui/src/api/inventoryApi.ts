import { appConfig } from "../config/appConfig";
import { apiFetch } from "./httpClient";
import type {
  AddRoomTypeRequest,
  HotelResponse,
  HotelSummaryResponse,
  RegisterHotelRequest,
  RoomAvailabilityResponse,
  SetRoomAvailabilityRequest,
  UUID,
} from "./types";

/**
 * Inventory service API client.
 *
 * Public catalog endpoints are used by the booking flow.
 * Admin endpoints are used by the future demo admin page.
 */
export const inventoryApi = {
  /**
   * GET /api/v1/hotels?limit=20
   */
  findHotels(limit = 20): Promise<HotelSummaryResponse[]> {
    return apiFetch<HotelSummaryResponse[]>(
      appConfig.inventoryServiceBaseUrl,
      "/api/v1/hotels",
      {
        query: { limit },
      },
    );
  },

  /**
   * GET /api/v1/hotels/{hotelId}
   */
  getHotelById(hotelId: UUID): Promise<HotelResponse> {
    return apiFetch<HotelResponse>(
      appConfig.inventoryServiceBaseUrl,
      `/api/v1/hotels/${hotelId}`,
    );
  },

  /**
   * GET /api/v1/hotels/{hotelId}/room-types/{roomTypeId}/availability?from=...&to=...
   */
  getRoomAvailability(
    hotelId: UUID,
    roomTypeId: UUID,
    from: string,
    to: string,
  ): Promise<RoomAvailabilityResponse[]> {
    return apiFetch<RoomAvailabilityResponse[]>(
      appConfig.inventoryServiceBaseUrl,
      `/api/v1/hotels/${hotelId}/room-types/${roomTypeId}/availability`,
      {
        query: { from, to },
      },
    );
  },

  /**
   * POST /api/v1/admin/hotels
   *
   * Inventory admin security is intentionally simplified in this training project.
   * We still keep authToken here because Google/admin mode may be added later.
   */
  registerHotel(
    request: RegisterHotelRequest,
    authToken?: string | null,
  ): Promise<HotelResponse> {
    return apiFetch<HotelResponse>(
      appConfig.inventoryServiceBaseUrl,
      "/api/v1/admin/hotels",
      {
        method: "POST",
        body: request,
        authToken,
      },
    );
  },

  /**
   * POST /api/v1/admin/hotels/{hotelId}/room-types
   */
  addRoomType(
    hotelId: UUID,
    request: AddRoomTypeRequest,
    authToken?: string | null,
  ): Promise<HotelResponse> {
    return apiFetch<HotelResponse>(
      appConfig.inventoryServiceBaseUrl,
      `/api/v1/admin/hotels/${hotelId}/room-types`,
      {
        method: "POST",
        body: request,
        authToken,
      },
    );
  },

  /**
   * POST /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/initialization
   */
  initializeRoomAvailability(
    hotelId: UUID,
    roomTypeId: UUID,
    request: SetRoomAvailabilityRequest,
    authToken?: string | null,
  ): Promise<void> {
    return apiFetch<void>(
      appConfig.inventoryServiceBaseUrl,
      `/api/v1/admin/hotels/${hotelId}/room-types/${roomTypeId}/availability/initialization`,
      {
        method: "POST",
        body: request,
        authToken,
      },
    );
  },

  /**
   * PUT /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/capacity
   */
  adjustRoomCapacity(
    hotelId: UUID,
    roomTypeId: UUID,
    request: SetRoomAvailabilityRequest,
    authToken?: string | null,
  ): Promise<void> {
    return apiFetch<void>(
      appConfig.inventoryServiceBaseUrl,
      `/api/v1/admin/hotels/${hotelId}/room-types/${roomTypeId}/availability/capacity`,
      {
        method: "PUT",
        body: request,
        authToken,
      },
    );
  },
};