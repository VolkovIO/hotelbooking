import { ApiError } from "../api";

type ErrorMessageProps = {
  error: unknown;
};

/**
 * Converts unknown JavaScript errors into user-readable text.
 *
 * We keep the prop as unknown because fetch(), JSON parsing, network failures
 * and our ApiError can all produce different error shapes.
 */
export function ErrorMessage({ error }: ErrorMessageProps) {
  return (
    <div className="state-card state-card-error">
      <strong>Request failed</strong>
      <p>{toErrorMessage(error)}</p>
    </div>
  );
}

function toErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return `Backend returned ${error.status}: ${error.message}`;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Unknown error";
}