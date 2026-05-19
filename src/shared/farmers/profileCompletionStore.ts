import { apiClient, ApiClientError } from "../../core/api/client";
import { endpoints } from "../../core/api/endpoints";
import { AppError } from "../../core/errors/AppError";

export type FarmerProfileCompletionStepStatus =
  | "COMING_SOON"
  | "COMPLETE"
  | "INCOMPLETE";

export type FarmerProfileCompletionStep = {
  code: string;
  comingSoon: boolean;
  complete: boolean;
  description: string;
  label: string;
  required: boolean;
  status: FarmerProfileCompletionStepStatus;
};

export type FarmerProfileCompletionRecord = {
  carbonProfileId?: string;
  completedRequiredSteps: number;
  completionPercentage: number;
  farmerProfileId: string;
  generatedAt: string;
  steps: FarmerProfileCompletionStep[];
  totalRequiredSteps: number;
  userId: string;
};

type FarmerProfileCompletionResponse = {
  carbonProfileId: string | null;
  completedRequiredSteps: number;
  completionPercentage: number;
  farmerProfileId: string;
  generatedAt: string;
  steps: FarmerProfileCompletionStep[];
  totalRequiredSteps: number;
  userId: string;
};

export async function getFarmerProfileCompletion(): Promise<FarmerProfileCompletionRecord> {
  try {
    const response = await apiClient.get<FarmerProfileCompletionResponse>(
      endpoints.farmer.profileCompletion
    );

    return {
      carbonProfileId: response.carbonProfileId ?? undefined,
      completedRequiredSteps: response.completedRequiredSteps,
      completionPercentage: response.completionPercentage,
      farmerProfileId: response.farmerProfileId,
      generatedAt: response.generatedAt,
      steps: response.steps,
      totalRequiredSteps: response.totalRequiredSteps,
      userId: response.userId
    };
  } catch (error) {
    throw toCompletionError(error);
  }
}

function toCompletionError(error: unknown) {
  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof ApiClientError) {
    if (error.status === 403) {
      return new AppError(
        "ACCESS_DENIED",
        "Your role or client module settings do not allow profile completion access."
      );
    }

    if (error.status === 0) {
      return new AppError(
        "API_REQUEST_FAILED",
        "Unable to reach the backend API. Confirm Spring Boot is running on http://localhost:8080."
      );
    }

    return new AppError(
      "API_REQUEST_FAILED",
      error.message === "Unable to complete API request."
        ? "Unable to load profile completion."
        : error.message
    );
  }

  return new AppError("API_REQUEST_FAILED", "Unable to load profile completion.");
}
