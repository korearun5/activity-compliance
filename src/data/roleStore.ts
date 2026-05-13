import { apiClient } from "../core/api/client";
import { PageResponse } from "../core/api/contracts";
import { endpoints } from "../core/api/endpoints";
import { BackendUserResponse } from "../core/api/userContracts";

export type BackendRole = {
  code: "ADMIN" | "FIELD_COORDINATOR" | "FPO_MANAGER";
  createdAt: string;
  id: string;
  name: string;
  tenantId: string;
};

export type BackendRoleCode = BackendRole["code"];

export type RoleManagedUser = Pick<
  BackendUserResponse,
  | "displayName"
  | "id"
  | "locationName"
  | "roles"
  | "siteName"
  | "status"
  | "tenantId"
  | "username"
>;

export type UserRoles = {
  roles: BackendRole["code"][];
  tenantId: string;
  updatedAt: string;
  userId: string;
  username: string;
};

export async function getBackendRoles() {
  return apiClient.get<BackendRole[]>(endpoints.roles.list);
}

export async function getBackendRoleManagedUsers() {
  const response = await apiClient.getPaginated<PageResponse<BackendUserResponse>>(
    endpoints.users.list,
    { size: 100, sort: "displayName,asc" }
  );

  return response.content.map((user) => ({
    displayName: user.displayName,
    id: user.id,
    locationName: user.locationName,
    roles: user.roles,
    siteName: user.siteName,
    status: user.status,
    tenantId: user.tenantId,
    username: user.username
  }));
}

export async function getBackendUserRoles(userId: string) {
  return apiClient.get<UserRoles>(endpoints.roles.userRoles(userId));
}

export async function updateBackendUserRoles(
  userId: string,
  roles: BackendRoleCode[]
) {
  return apiClient.put<{ roles: BackendRoleCode[] }, UserRoles>(
    endpoints.roles.userRoles(userId),
    { roles }
  );
}
