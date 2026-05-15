import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import {
  AdvisoryCategory,
  AdvisoryStatus,
  AdvisoryTargetType,
  FpoAdvisoryImageRequest,
  FpoAdvisoryRequest,
  FpoAdvisoryResponse,
  NotificationChannel,
  UpdateFpoAdvisoryStatusRequest
} from "../core/api/fpoContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";

export type AdvisoryRecord = {
  category: AdvisoryCategory;
  channel: NotificationChannel;
  createdAt: string;
  createdByName?: string;
  cropId?: string;
  cropName?: string;
  id: string;
  images: AdvisoryImage[];
  message: string;
  publishedAt?: string;
  seasonId?: string;
  seasonName?: string;
  seasonYear?: number;
  status: AdvisoryStatus;
  targetType: AdvisoryTargetType;
  tenantId?: string;
  title: string;
  updatedAt: string;
};

export type AdvisoryImage = {
  contentType?: string;
  createdAt?: string;
  id?: string;
  imageUrl: string;
  originalFilename?: string;
  sortOrder?: number;
  storageKey?: string;
};

export type AdvisoryInput = {
  category: AdvisoryCategory;
  channel?: NotificationChannel;
  cropId?: string;
  imageUrls?: string[];
  images?: AdvisoryImage[];
  message: string;
  seasonId?: string;
  status?: AdvisoryStatus;
  targetType?: AdvisoryTargetType;
  title: string;
};

export type AdvisoryFilters = {
  cropId?: string;
  seasonId?: string;
  status?: AdvisoryStatus;
  targetType?: AdvisoryTargetType;
};

const dummyAdvisories: AdvisoryRecord[] = [
  {
    channel: "IN_APP",
    category: "AGRONOMY",
    createdAt: "2026-05-09T10:30:00.000Z",
    createdByName: "System demo",
    cropName: "Soybean",
    id: "local-advisory-1",
    images: [],
    message:
      "Retain crop residue after land preparation and add compost before sowing to improve soil organic carbon.",
    publishedAt: "2026-05-09T11:00:00.000Z",
    seasonName: "Kharif",
    seasonYear: 2026,
    status: "PUBLISHED",
    targetType: "ALL_MEMBERS",
    title: "Residue retention for carbon build-up",
    updatedAt: "2026-05-09T11:00:00.000Z"
  },
  {
    channel: "IN_APP",
    category: "PEST_DISEASE_MANAGEMENT",
    createdAt: "2026-05-10T08:15:00.000Z",
    createdByName: "System demo",
    cropName: "Pomegranate",
    id: "local-advisory-2",
    images: [
      {
        imageUrl: "https://example.com/advisories/pest-symptom.jpg",
        originalFilename: "pest-symptom.jpg",
        sortOrder: 0
      }
    ],
    message:
      "Use Trichoderma with compost around the root zone and avoid excess chemical nitrogen this week.",
    seasonName: "Annual",
    seasonYear: 2026,
    status: "DRAFT",
    targetType: "CROP",
    title: "Biological input dosage reminder",
    updatedAt: "2026-05-10T08:15:00.000Z"
  }
];

export async function getAdvisories(
  filters: AdvisoryFilters = {}
): Promise<AdvisoryRecord[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<FpoAdvisoryResponse[]>(
        advisoryListEndpoint(filters),
        { accessToken }
      );
      const advisories = response.map(toAdvisoryRecord);

      await writeJson(storageKeys.fpo.advisories, advisories);
      return advisories;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdvisoryError(error);
      }

      logger.warn("Backend advisories unavailable; using cached advisory records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const advisories = await getLocalAdvisories();
  return advisories.filter((advisory) => matchesFilters(advisory, filters));
}

export async function createAdvisory(input: AdvisoryInput) {
  const request = toAdvisoryRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<FpoAdvisoryRequest, FpoAdvisoryResponse>(
        endpoints.fpo.advisories.create,
        request,
        { accessToken }
      );
      const advisory = toAdvisoryRecord(response);

      await upsertLocalAdvisory(advisory);
      return advisory;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdvisoryError(error);
      }

      logger.warn("Backend advisory creation unavailable; using cached records.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalAdvisory({
    category: request.category,
    channel: request.channel ?? "IN_APP",
    createdAt: new Date().toISOString(),
    cropId: request.cropId,
    id: `local-advisory-${Date.now()}`,
    images: toLocalImages(request.images ?? []),
    message: request.message,
    publishedAt: request.status === "PUBLISHED" ? new Date().toISOString() : undefined,
    seasonId: request.seasonId,
    status: request.status ?? "DRAFT",
    targetType: request.targetType ?? "ALL_MEMBERS",
    title: request.title,
    updatedAt: new Date().toISOString()
  });
}

export async function updateAdvisoryStatus(
  advisory: AdvisoryRecord,
  status: AdvisoryStatus
) {
  const accessToken = await getAccessToken();

  if (accessToken && !advisory.id.startsWith("local-")) {
    try {
      const response = await apiClient.patch<
        UpdateFpoAdvisoryStatusRequest,
        FpoAdvisoryResponse
      >(endpoints.fpo.advisories.status(advisory.id), { status }, { accessToken });
      const updated = toAdvisoryRecord(response);

      await upsertLocalAdvisory(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdvisoryError(error);
      }

      logger.warn("Backend advisory status update unavailable; using cache.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalAdvisory({
    ...advisory,
    publishedAt:
      status === "PUBLISHED"
        ? (advisory.publishedAt ?? new Date().toISOString())
        : undefined,
    status,
    updatedAt: new Date().toISOString()
  });
}

function toAdvisoryRecord(response: FpoAdvisoryResponse): AdvisoryRecord {
  return {
    category: response.category,
    channel: response.channel,
    createdAt: response.createdAt,
    createdByName: response.createdByName ?? undefined,
    cropId: response.cropId ?? undefined,
    cropName: response.cropName ?? undefined,
    id: response.id,
    images: response.images.map((image) => ({
      contentType: image.contentType ?? undefined,
      createdAt: image.createdAt,
      id: image.id,
      imageUrl: image.imageUrl,
      originalFilename: image.originalFilename ?? undefined,
      sortOrder: image.sortOrder,
      storageKey: image.storageKey ?? undefined
    })),
    message: response.message,
    publishedAt: response.publishedAt ?? undefined,
    seasonId: response.seasonId ?? undefined,
    seasonName: response.seasonName ?? undefined,
    seasonYear: response.seasonYear ?? undefined,
    status: response.status,
    targetType: response.targetType,
    tenantId: response.tenantId,
    title: response.title,
    updatedAt: response.updatedAt
  };
}

function toAdvisoryRequest(input: AdvisoryInput): FpoAdvisoryRequest {
  const targetType = input.targetType ?? "ALL_MEMBERS";
  const category = input.category;
  const title = input.title.trim();
  const message = input.message.trim();
  const cropId = cleanOptional(input.cropId);
  const images = normalizeImages(input.images, input.imageUrls);

  if (!title || !message) {
    throw new AppError("VALIDATION_FAILED", "Enter advisory title and message.");
  }

  if (!category) {
    throw new AppError("VALIDATION_FAILED", "Select advisory category.");
  }

  if (targetType === "CROP" && !cropId) {
    throw new AppError("VALIDATION_FAILED", "Select crop for crop targeted advisory.");
  }

  return {
    category,
    channel: input.channel ?? "IN_APP",
    cropId: targetType === "CROP" ? cropId : undefined,
    images,
    message,
    seasonId: cleanOptional(input.seasonId),
    status: input.status ?? "DRAFT",
    targetType,
    title
  };
}

function toStoredAdvisory(advisory: Partial<AdvisoryRecord>): AdvisoryRecord | null {
  if (
    typeof advisory.id !== "string" ||
    typeof advisory.title !== "string" ||
    typeof advisory.message !== "string" ||
    typeof advisory.createdAt !== "string" ||
    typeof advisory.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    category: advisory.category ?? "AGRONOMY",
    channel: advisory.channel ?? "IN_APP",
    createdAt: advisory.createdAt,
    createdByName: advisory.createdByName,
    cropId: advisory.cropId,
    cropName: advisory.cropName,
    id: advisory.id,
    images: (advisory.images ?? []).filter(isAdvisoryImage),
    message: advisory.message,
    publishedAt: advisory.publishedAt,
    seasonId: advisory.seasonId,
    seasonName: advisory.seasonName,
    seasonYear: advisory.seasonYear,
    status: advisory.status ?? "DRAFT",
    targetType: advisory.targetType ?? "ALL_MEMBERS",
    tenantId: advisory.tenantId,
    title: advisory.title,
    updatedAt: advisory.updatedAt
  };
}

async function getLocalAdvisories() {
  const saved = await readJsonArray<Partial<AdvisoryRecord>>([
    storageKeys.fpo.advisories
  ]);
  const advisories = saved
    .map(toStoredAdvisory)
    .filter((advisory): advisory is AdvisoryRecord => Boolean(advisory));

  if (advisories.length) {
    return advisories;
  }

  await writeJson(storageKeys.fpo.advisories, dummyAdvisories);
  return dummyAdvisories;
}

async function upsertLocalAdvisory(advisory: AdvisoryRecord) {
  const current = await getLocalAdvisories();
  await writeJson(storageKeys.fpo.advisories, [
    advisory,
    ...current.filter((item) => item.id !== advisory.id)
  ]);
  return advisory;
}

function advisoryListEndpoint(filters: AdvisoryFilters) {
  const params = new URLSearchParams();
  if (filters.status) params.append("status", filters.status);
  if (filters.cropId) params.append("cropId", filters.cropId);
  if (filters.seasonId) params.append("seasonId", filters.seasonId);
  if (filters.targetType) params.append("targetType", filters.targetType);
  const query = params.toString();
  return query
    ? `${endpoints.fpo.advisories.list}?${query}`
    : endpoints.fpo.advisories.list;
}

function matchesFilters(advisory: AdvisoryRecord, filters: AdvisoryFilters) {
  return (
    (!filters.status || advisory.status === filters.status) &&
    (!filters.cropId || advisory.cropId === filters.cropId) &&
    (!filters.seasonId || advisory.seasonId === filters.seasonId) &&
    (!filters.targetType || advisory.targetType === filters.targetType)
  );
}

function normalizeImages(
  images: AdvisoryImage[] | undefined,
  imageUrls: string[] | undefined
): FpoAdvisoryImageRequest[] {
  const source: AdvisoryImage[] =
    images ?? imageUrls?.map((imageUrl) => ({ imageUrl })) ?? [];
  const normalized = source
    .map((image) => ({
      contentType: cleanOptional(image.contentType),
      imageUrl: image.imageUrl.trim(),
      originalFilename: cleanOptional(image.originalFilename),
      storageKey: cleanOptional(image.storageKey)
    }))
    .filter((image) => image.imageUrl.length > 0);

  if (normalized.length > 10) {
    throw new AppError("VALIDATION_FAILED", "Attach a maximum of 10 advisory images.");
  }

  return normalized;
}

function toLocalImages(images: FpoAdvisoryImageRequest[]): AdvisoryImage[] {
  return images.map((image, index) => ({
    contentType: image.contentType,
    createdAt: new Date().toISOString(),
    id: `local-advisory-image-${Date.now()}-${index}`,
    imageUrl: image.imageUrl,
    originalFilename: image.originalFilename,
    sortOrder: index,
    storageKey: image.storageKey
  }));
}

function isAdvisoryImage(image: Partial<AdvisoryImage>): image is AdvisoryImage {
  return typeof image.imageUrl === "string" && image.imageUrl.trim().length > 0;
}

async function getAccessToken() {
  return AsyncStorage.getItem(storageKeys.auth.accessToken);
}

function cleanOptional(value: string | undefined) {
  const trimmed = value?.trim();
  return trimmed || undefined;
}

function canUseLocalFallback(error: unknown) {
  return !(error instanceof ApiClientError);
}

function toAdvisoryError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", error.message);
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError("ACCESS_DENIED", error.message);
    }

    return new AppError("API_REQUEST_FAILED", error.message);
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage advisories.");
}
