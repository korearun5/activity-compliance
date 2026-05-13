export type BackendRole = "ADMIN" | "FPO_MANAGER" | "FIELD_COORDINATOR";

export type BackendUserStatus = "ACTIVE" | "INACTIVE";

export type BackendUserResponse = {
  createdAt: string;
  displayName: string;
  id: string;
  locationName: string | null;
  phone: string | null;
  roles: BackendRole[];
  siteName: string | null;
  status: BackendUserStatus;
  tenantId: string;
  updatedAt: string;
  username: string;
};

export type CreateBackendUserRequest = {
  displayName: string;
  locationName: string;
  password: string;
  phone: string;
  siteName: string;
  username: string;
};

export type UpdateBackendUserRequest = {
  displayName: string;
  locationName: string;
  phone: string;
  siteName: string;
};

export type UpdateBackendUserStatusRequest = {
  status: BackendUserStatus;
};
