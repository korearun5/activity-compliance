import { Platform } from "react-native";

export type UploadFileInput = {
  name?: string | null;
  type?: string | null;
  uri: string;
};

export async function appendUploadFile(
  formData: FormData,
  fieldName: string,
  file: UploadFileInput,
  fallbackFilename: string
) {
  const name = resolveFilename(file, fallbackFilename);
  const type = file.type?.trim() || contentTypeFromFilename(name);

  if (Platform.OS === "web") {
    const response = await fetch(file.uri);
    const blob = await response.blob();
    formData.append(fieldName, new Blob([blob], { type }), name);
    return;
  }

  formData.append(fieldName, {
    name,
    type,
    uri: file.uri
  } as unknown as Blob);
}

function resolveFilename(file: UploadFileInput, fallbackFilename: string) {
  const candidate = file.name?.trim() || fileNameFromUri(file.uri);
  const safeCandidate = candidate ? safeBasename(candidate) : null;

  return safeCandidate?.includes(".") ? safeCandidate : fallbackFilename;
}

function fileNameFromUri(uri: string) {
  const cleanUri = uri.split("?")[0] ?? uri;
  return cleanUri.split("/").filter(Boolean).pop() ?? null;
}

function safeBasename(filename: string) {
  const normalized = filename.replace(/\\/g, "/");
  if (normalized.includes("..")) {
    return null;
  }

  const basename = normalized.split("/").filter(Boolean).pop()?.trim();
  return basename || null;
}

function contentTypeFromFilename(filename: string) {
  const extension = filename.split(".").pop()?.toLowerCase();

  switch (extension) {
    case "heic":
      return "image/heic";
    case "heif":
      return "image/heif";
    case "jpeg":
    case "jpg":
      return "image/jpeg";
    case "pdf":
      return "application/pdf";
    case "png":
      return "image/png";
    case "webp":
      return "image/webp";
    default:
      return "application/octet-stream";
  }
}
