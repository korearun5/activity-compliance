import AsyncStorage from "@react-native-async-storage/async-storage";

import { appConfig } from "../config/appConfig";
import { storageKeys } from "../storage/storageKeys";
import { ApiErrorBody, ApiResponse } from "./contracts";

type RequestOptions = {
  accessToken?: string | null;
};

export type PaginationParams = {
  page?: number;
  size?: number;
  sort?: string;
};

export class ApiClientError extends Error {
  readonly error?: ApiErrorBody;
  readonly status: number;

  constructor(message: string, status: number, error?: ApiErrorBody) {
    super(message);
    this.name = "ApiClientError";
    this.error = error;
    this.status = status;
  }
}

export async function postJson<TRequest, TResponse>(
  path: string,
  body: TRequest,
  options: RequestOptions = {}
) {
  return requestJson<TResponse>(path, {
    accessToken: options.accessToken,
    body: JSON.stringify(body),
    method: "POST"
  });
}

export async function putJson<TRequest, TResponse>(
  path: string,
  body: TRequest,
  options: RequestOptions = {}
) {
  return requestJson<TResponse>(path, {
    accessToken: options.accessToken,
    body: JSON.stringify(body),
    method: "PUT"
  });
}

export async function patchJson<TRequest, TResponse>(
  path: string,
  body: TRequest,
  options: RequestOptions = {}
) {
  return requestJson<TResponse>(path, {
    accessToken: options.accessToken,
    body: JSON.stringify(body),
    method: "PATCH"
  });
}

export async function postFormData<TResponse>(
  path: string,
  body: FormData,
  options: RequestOptions = {}
) {
  return requestJson<TResponse>(path, {
    accessToken: options.accessToken,
    body,
    method: "POST",
    skipJsonContentType: true
  });
}

export async function getJson<TResponse>(path: string, options: RequestOptions = {}) {
  return requestJson<TResponse>(path, {
    accessToken: options.accessToken,
    method: "GET"
  });
}

export async function getJsonPaginated<TResponse>(
  path: string,
  pagination: PaginationParams = {},
  options: RequestOptions = {}
) {
  const params = new URLSearchParams();
  if (pagination.page !== undefined) {
    params.append("page", pagination.page.toString());
  }
  if (pagination.size !== undefined) {
    params.append("size", pagination.size.toString());
  }
  if (pagination.sort !== undefined) params.append("sort", pagination.sort);

  const separator = path.includes("?") ? "&" : "?";
  const fullPath = params.toString() ? `${path}${separator}${params.toString()}` : path;

  return requestJson<TResponse>(fullPath, {
    accessToken: options.accessToken,
    method: "GET"
  });
}

async function requestJson<TResponse>(
  path: string,
  options: RequestInit & RequestOptions & { skipJsonContentType?: boolean }
) {
  const headers = new Headers(options.headers);
  const accessToken = await resolveAccessToken(options.accessToken);

  headers.set("Accept", "application/json");
  if (!options.skipJsonContentType) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    ...options,
    headers
  });
  const text = await response.text();
  const payload = parseApiResponse<TResponse>(text);

  if (!response.ok || !payload?.success) {
    const error = payload && !payload.success ? payload.error : undefined;
    throw new ApiClientError(
      error?.message ?? "Unable to complete API request.",
      response.status,
      error
    );
  }

  return payload.data;
}

async function resolveAccessToken(accessToken: string | null | undefined) {
  if (accessToken !== undefined) {
    return accessToken;
  }

  return AsyncStorage.getItem(storageKeys.auth.accessToken);
}

function parseApiResponse<TResponse>(text: string) {
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as ApiResponse<TResponse>;
  } catch {
    return null;
  }
}

export const apiClient = {
  get: getJson,
  getPaginated: getJsonPaginated,
  patch: patchJson,
  post: postJson,
  postFormData,
  put: putJson
};
