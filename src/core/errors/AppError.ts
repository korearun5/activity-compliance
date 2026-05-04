export type AppErrorCode =
  | "AUTH_INVALID_CREDENTIALS"
  | "AUTH_USERNAME_TAKEN"
  | "DATA_PARSE_FAILED"
  | "VALIDATION_FAILED";

export class AppError extends Error {
  readonly code: AppErrorCode;

  constructor(code: AppErrorCode, message: string) {
    super(message);
    this.code = code;
    this.name = "AppError";
  }
}

export function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

