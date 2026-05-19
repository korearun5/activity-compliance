import { apiClient, ApiClientError } from "../../core/api/client";
import { endpoints } from "../../core/api/endpoints";
import { AppError } from "../../core/errors/AppError";

export type FarmerBankDetailsStatus =
  | "PENDING_VERIFICATION"
  | "VERIFIED"
  | "REJECTED";

export type FarmerBankDetailsRecord = {
  accountHolderName: string;
  accountNumber: string;
  bankName: string;
  createdAt: string;
  farmerProfileId: string;
  farmerMobileNumber: string;
  farmerName: string;
  id: string;
  ifscCode: string;
  status: FarmerBankDetailsStatus;
  updatedAt: string;
  upiId?: string;
  verificationNotes?: string;
  verifiedAt?: string;
  verifiedByUserId?: string;
};

export type FarmerBankDetailsInput = {
  accountHolderName: string;
  accountNumber: string;
  bankName: string;
  ifscCode: string;
  upiId?: string;
};

type FarmerBankDetailsResponse = {
  accountHolderName: string;
  accountNumber: string;
  bankName: string;
  createdAt: string;
  farmerProfileId: string;
  farmerMobileNumber: string;
  farmerName: string;
  id: string;
  ifscCode: string;
  status: FarmerBankDetailsStatus;
  updatedAt: string;
  upiId: string | null;
  verificationNotes: string | null;
  verifiedAt: string | null;
  verifiedByUserId: string | null;
};

type FarmerBankDetailsVerificationInput = {
  notes?: string;
  status: Exclude<FarmerBankDetailsStatus, "PENDING_VERIFICATION">;
};

export async function getFarmerBankDetails() {
  try {
    const response = await apiClient.get<FarmerBankDetailsResponse | null>(
      endpoints.farmer.bankDetails.current
    );

    return response ? mapBankDetails(response) : null;
  } catch (error) {
    throw toBankDetailsError(error, "Unable to load bank details.");
  }
}

export async function saveFarmerBankDetails(
  input: FarmerBankDetailsInput,
  existingId?: string
) {
  try {
    const payload = normalizeBankDetailsInput(input);
    const response = existingId
      ? await apiClient.put<FarmerBankDetailsInput, FarmerBankDetailsResponse>(
          endpoints.farmer.bankDetails.update(existingId),
          payload
        )
      : await apiClient.post<FarmerBankDetailsInput, FarmerBankDetailsResponse>(
          endpoints.farmer.bankDetails.current,
          payload
        );

    return mapBankDetails(response);
  } catch (error) {
    throw toBankDetailsError(error, "Unable to save bank details.");
  }
}

export async function listPendingFarmerBankDetails() {
  try {
    const response = await apiClient.get<FarmerBankDetailsResponse[]>(
      endpoints.admin.bankDetails.pending
    );

    return response.map(mapBankDetails);
  } catch (error) {
    throw toBankDetailsError(error, "Unable to load pending bank details.");
  }
}

export async function verifyFarmerBankDetails(
  bankDetailsId: string,
  input: FarmerBankDetailsVerificationInput
) {
  try {
    const response = await apiClient.put<
      FarmerBankDetailsVerificationInput,
      FarmerBankDetailsResponse
    >(endpoints.admin.bankDetails.verify(bankDetailsId), {
      notes: input.notes?.trim() || undefined,
      status: input.status
    });

    return mapBankDetails(response);
  } catch (error) {
    throw toBankDetailsError(error, "Unable to update bank verification status.");
  }
}

function mapBankDetails(
  response: FarmerBankDetailsResponse
): FarmerBankDetailsRecord {
  return {
    accountHolderName: response.accountHolderName,
    accountNumber: response.accountNumber,
    bankName: response.bankName,
    createdAt: response.createdAt,
    farmerProfileId: response.farmerProfileId,
    farmerMobileNumber: response.farmerMobileNumber,
    farmerName: response.farmerName,
    id: response.id,
    ifscCode: response.ifscCode,
    status: response.status,
    updatedAt: response.updatedAt,
    upiId: response.upiId ?? undefined,
    verificationNotes: response.verificationNotes ?? undefined,
    verifiedAt: response.verifiedAt ?? undefined,
    verifiedByUserId: response.verifiedByUserId ?? undefined
  };
}

function normalizeBankDetailsInput(
  input: FarmerBankDetailsInput
): FarmerBankDetailsInput {
  return {
    accountHolderName: input.accountHolderName.trim(),
    accountNumber: input.accountNumber.trim(),
    bankName: input.bankName.trim(),
    ifscCode: input.ifscCode.trim().toUpperCase(),
    upiId: input.upiId?.trim() || undefined
  };
}

function toBankDetailsError(error: unknown, fallbackMessage: string) {
  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof ApiClientError) {
    if (error.status === 403) {
      return new AppError(
        "ACCESS_DENIED",
        "Your role or client module settings do not allow bank details access."
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
        ? fallbackMessage
        : error.message
    );
  }

  return new AppError("API_REQUEST_FAILED", fallbackMessage);
}
