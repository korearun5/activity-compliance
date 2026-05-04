export type ApiSuccess<T> = {
  data: T;
  meta?: ApiMeta;
  success: true;
};

export type ApiFailure = {
  error: ApiErrorBody;
  success: false;
};

export type ApiResponse<T> = ApiSuccess<T> | ApiFailure;

export type ApiErrorBody = {
  code: string;
  details?: Record<string, string[]>;
  message: string;
  traceId?: string;
};

export type ApiMeta = {
  page?: PageMeta;
  requestId?: string;
};

export type PageMeta = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type SortDirection = "asc" | "desc";

export type PageRequest = {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: SortDirection;
};

export type Id = string;

