type QueryParams = Record<string, string | number | boolean | null | undefined>;

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

type ApiRequestOptions = {
  method?: HttpMethod;
  query?: QueryParams;
  body?: unknown;

  /**
   * Google mode will use this later.
   *
   * In demo mode authToken is undefined, so Authorization header is not sent.
   */
  authToken?: string | null;
};

/**
 * Small typed wrapper around browser fetch().
 *
 * Why not call fetch() directly from React components?
 *
 * Because we want the UI to stay clean:
 * - React components render screens.
 * - api/* modules know backend URLs and endpoints.
 * - httpClient knows low-level HTTP details.
 *
 * This is similar to keeping REST client logic in a separate adapter in Java.
 */
export async function apiFetch<T>(
  baseUrl: string,
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const url = buildUrl(baseUrl, path, options.query);
  const headers = new Headers();

  headers.set("Accept", "application/json");

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.authToken) {
    headers.set("Authorization", `Bearer ${options.authToken}`);
  }

  const response = await fetch(url, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (!response.ok) {
    throw await ApiError.fromResponse(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();

  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

function buildUrl(baseUrl: string, path: string, query?: QueryParams): string {
  const normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const url = new URL(`${normalizedBaseUrl}${normalizedPath}`);

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    });
  }

  return url.toString();
}

/**
 * Error type for failed backend calls.
 *
 * Spring services return structured error responses, but different services may
 * have slightly different shapes. Therefore details is kept as unknown.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly details: unknown;

  private constructor(message: string, status: number, details: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    const details = await readErrorBody(response);
    const message = extractErrorMessage(details) ?? response.statusText;

    return new ApiError(message, response.status, details);
  }
}

async function readErrorBody(response: Response): Promise<unknown> {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

function extractErrorMessage(details: unknown): string | undefined {
  if (!isRecord(details)) {
    return undefined;
  }

  const message = details["message"];

  return typeof message === "string" ? message : undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}