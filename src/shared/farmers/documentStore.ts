import { apiClient, ApiClientError } from "../../core/api/client";
import { endpoints } from "../../core/api/endpoints";
import { appendUploadFile, UploadFileInput } from "../../core/api/uploadFile";
import { AppError } from "../../core/errors/AppError";

export const FARMER_DOCUMENT_TYPES = [
  "AADHAAR",
  "LAND_RECORD",
  "SOIL_REPORT",
  "BANK_PROOF",
  "OTHER"
] as const;

export type FarmerDocumentType = (typeof FARMER_DOCUMENT_TYPES)[number];

export type FarmerDocumentStatus =
  | "PENDING_VERIFICATION"
  | "VERIFIED"
  | "REJECTED";

export type FarmerDocumentRecord = {
  createdAt: string;
  documentType: FarmerDocumentType;
  farmerMobileNumber: string;
  farmerName: string;
  farmerProfileId: string;
  fileName: string;
  fileUrl: string;
  id: string;
  mimeType: string;
  status: FarmerDocumentStatus;
  updatedAt: string;
  uploadedAt: string;
  verificationNotes?: string;
  verifiedAt?: string;
  verifiedByUserId?: string;
};

type FarmerDocumentResponse = {
  createdAt: string;
  documentType: FarmerDocumentType;
  farmerMobileNumber: string;
  farmerName: string;
  farmerProfileId: string;
  fileName: string;
  fileUrl: string;
  id: string;
  mimeType: string;
  status: FarmerDocumentStatus;
  updatedAt: string;
  uploadedAt: string;
  verificationNotes: string | null;
  verifiedAt: string | null;
  verifiedByUserId: string | null;
};

export type FarmerDocumentUploadInput = {
  documentType: FarmerDocumentType;
  file: UploadFileInput;
};

type FarmerDocumentVerificationInput = {
  notes?: string;
  status: Exclude<FarmerDocumentStatus, "PENDING_VERIFICATION">;
};

export async function listFarmerDocuments() {
  try {
    const response = await apiClient.get<FarmerDocumentResponse[]>(
      endpoints.farmer.documents.list
    );

    return response.map(mapDocument);
  } catch (error) {
    throw toDocumentError(error, "Unable to load documents.");
  }
}

export async function uploadFarmerDocument(input: FarmerDocumentUploadInput) {
  try {
    const formData = new FormData();
    formData.append("document_type", input.documentType);
    await appendUploadFile(
      formData,
      "file",
      input.file,
      `${input.documentType.toLowerCase()}.pdf`
    );

    const response = await apiClient.postFormData<FarmerDocumentResponse>(
      endpoints.farmer.documents.upload,
      formData
    );

    return mapDocument(response);
  } catch (error) {
    throw toDocumentError(error, "Unable to upload document.");
  }
}

export async function deleteFarmerDocument(documentId: string) {
  try {
    await apiClient.delete<null>(endpoints.farmer.documents.byId(documentId));
  } catch (error) {
    throw toDocumentError(error, "Unable to delete document.");
  }
}

export async function listPendingFarmerDocuments() {
  try {
    const response = await apiClient.get<FarmerDocumentResponse[]>(
      endpoints.admin.documents.pending
    );

    return response.map(mapDocument);
  } catch (error) {
    throw toDocumentError(error, "Unable to load pending documents.");
  }
}

export async function verifyFarmerDocument(
  documentId: string,
  input: FarmerDocumentVerificationInput
) {
  try {
    const response = await apiClient.put<
      FarmerDocumentVerificationInput,
      FarmerDocumentResponse
    >(endpoints.admin.documents.verify(documentId), {
      notes: input.notes?.trim() || undefined,
      status: input.status
    });

    return mapDocument(response);
  } catch (error) {
    throw toDocumentError(error, "Unable to update document verification status.");
  }
}

export function documentTypeLabel(documentType: FarmerDocumentType) {
  switch (documentType) {
    case "AADHAAR":
      return "Aadhaar";
    case "LAND_RECORD":
      return "Land record";
    case "SOIL_REPORT":
      return "Soil report";
    case "BANK_PROOF":
      return "Bank proof";
    case "OTHER":
      return "Other";
  }
}

function mapDocument(response: FarmerDocumentResponse): FarmerDocumentRecord {
  return {
    createdAt: response.createdAt,
    documentType: response.documentType,
    farmerMobileNumber: response.farmerMobileNumber,
    farmerName: response.farmerName,
    farmerProfileId: response.farmerProfileId,
    fileName: response.fileName,
    fileUrl: response.fileUrl,
    id: response.id,
    mimeType: response.mimeType,
    status: response.status,
    updatedAt: response.updatedAt,
    uploadedAt: response.uploadedAt,
    verificationNotes: response.verificationNotes ?? undefined,
    verifiedAt: response.verifiedAt ?? undefined,
    verifiedByUserId: response.verifiedByUserId ?? undefined
  };
}

function toDocumentError(error: unknown, fallbackMessage: string) {
  if (error instanceof AppError) {
    return error;
  }

  if (error instanceof ApiClientError) {
    if (error.status === 403) {
      return new AppError(
        "ACCESS_DENIED",
        "Your role or client module settings do not allow document access."
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
