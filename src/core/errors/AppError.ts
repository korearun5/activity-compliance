export type AppErrorCode =
  | "ACCESS_DENIED"
  | "API_REQUEST_FAILED"
  | "AUTH_INVALID_CREDENTIALS"
  | "AUTH_USERNAME_TAKEN"
  | "DATA_PARSE_FAILED"
  | "DUPLICATE_RESOURCE"
  | "VALIDATION_FAILED";

export class AppError extends Error {
  readonly code: AppErrorCode;

  constructor(code: AppErrorCode, message: string) {
    super(message);
    this.code = code;
    this.name = "AppError";
  }
}

export function getErrorMessage(error: unknown, fallback = "Something went wrong.") {
  return error instanceof Error ? error.message : fallback;
}
