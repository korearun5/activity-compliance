import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import type { Role } from "../auth/authService";
import {
  BackendRole,
  BackendRoleCode,
  createBackendStaffUser,
  CreateStaffUserInput,
  getBackendRoleManagedUsers,
  getBackendRoles,
  RoleManagedUser,
  updateBackendUserRoles
} from "../data/roleStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminRolesTabProps = {
  canUseBackend: boolean;
  currentRole: StaffRole;
};

type StaffRole = Exclude<Role, "farmer">;
type CreateStaffRole = Extract<BackendRoleCode, "FIELD_COORDINATOR" | "FPO_MANAGER">;

const roleOrder: BackendRoleCode[] = [
  "ADMIN",
  "FPO_MANAGER",
  "FIELD_COORDINATOR",
  "FARMER"
];
const adminStaffCreationRoles: CreateStaffRole[] = [
  "FPO_MANAGER",
  "FIELD_COORDINATOR"
];
const fpoManagerStaffCreationRoles: CreateStaffRole[] = ["FIELD_COORDINATOR"];

export function AdminRolesTab({
  canUseBackend,
  currentRole
}: AdminRolesTabProps) {
  const [error, setError] = useState("");
  const [createError, setCreateError] = useState("");
  const [isCreatingStaffUser, setIsCreatingStaffUser] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [roles, setRoles] = useState<BackendRole[]>([]);
  const [updatingUserId, setUpdatingUserId] = useState<string | null>(null);
  const [users, setUsers] = useState<RoleManagedUser[]>([]);
  const canUpdateRoles = currentRole === "admin";
  const allowedStaffRoles =
    currentRole === "admin"
      ? adminStaffCreationRoles
      : fpoManagerStaffCreationRoles;

  useEffect(() => {
    if (!canUseBackend) {
      setRoles([]);
      setUsers([]);
      return;
    }

    loadRoleData();
  }, [canUseBackend]);

  const sortedRoles = useMemo(
    () =>
      roles
        .slice()
        .sort(
          (left, right) =>
            roleOrder.indexOf(left.code) - roleOrder.indexOf(right.code)
        ),
    [roles]
  );

  async function loadRoleData() {
    setIsLoading(true);
    setError("");

    try {
      const [loadedRoles, loadedUsers] = await Promise.all([
        getBackendRoles(),
        getBackendRoleManagedUsers()
      ]);
      setRoles(loadedRoles);
      setUsers(loadedUsers);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load role data."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreateStaffUser(input: CreateStaffUserInput) {
    setIsCreatingStaffUser(true);
    setCreateError("");

    try {
      const user = await createBackendStaffUser(input);
      setUsers((currentUsers) => [
        user,
        ...currentUsers.filter((item) => item.id !== user.id)
      ]);
      return true;
    } catch (createStaffError) {
      setCreateError(
        getErrorMessage(createStaffError, "Unable to create staff login.")
      );
      return false;
    } finally {
      setIsCreatingStaffUser(false);
    }
  }

  async function handleToggleRole(user: RoleManagedUser, role: BackendRoleCode) {
    if (!canUpdateRoles) {
      setError("Only platform admins can change staff roles.");
      return;
    }

    const hasRole = user.roles.includes(role);
    const nextRoles = hasRole
      ? user.roles.filter((item) => item !== role)
      : [...user.roles, role];

    if (!nextRoles.length) {
      setError("Each user must keep at least one role.");
      return;
    }

    setUpdatingUserId(user.id);
    setError("");

    try {
      const updatedRoles = await updateBackendUserRoles(
        user.id,
        sortRoleCodes(nextRoles)
      );
      setUsers((currentUsers) =>
        currentUsers.map((item) =>
          item.id === user.id ? { ...item, roles: updatedRoles.roles } : item
        )
      );
    } catch (updateError) {
      setError(getErrorMessage(updateError, "Unable to update user roles."));
    } finally {
      setUpdatingUserId(null);
    }
  }

  return (
    <View style={styles.section}>
      {!canUseBackend ? (
        <View style={styles.warningCard}>
          <Text style={styles.warningText}>
            Backend session is required for role management.
          </Text>
        </View>
      ) : null}

      <View style={styles.headerRow}>
        <Text style={styles.sectionTitle}>Role management</Text>
        <Pressable
          accessibilityRole="button"
          disabled={!canUseBackend || isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadRoleData}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      <CreateStaffUserForm
        allowedRoles={allowedStaffRoles}
        error={createError}
        isSubmitting={isCreatingStaffUser}
        onSubmit={handleCreateStaffUser}
      />

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      {users.length ? (
        users.map((user) => (
          <View key={user.id} style={styles.userCard}>
            <View style={styles.userText}>
              <Text style={styles.cardTitle}>{user.displayName}</Text>
              <Text style={styles.cardDescription}>
                {user.username} - {user.locationName ?? "No location"} -{" "}
                {user.siteName ?? "No site"}
              </Text>
              <View style={styles.badgeRow}>
                <StatusBadge
                  label={user.status}
                  tone={user.status === "ACTIVE" ? "good" : "warning"}
                />
                {user.roles.map((role) => (
                  <StatusBadge key={role} label={roleLabel(role)} tone="neutral" />
                ))}
              </View>
            </View>
            {user.roles.includes("FARMER") ? (
              <View style={styles.roleLockedBox}>
                <Text style={styles.roleLockedText}>
                  Farmer role is managed from the farmer profile.
                </Text>
              </View>
            ) : !canUpdateRoles ? (
              <View style={styles.roleLockedBox}>
                <Text style={styles.roleLockedText}>
                  Role changes are platform admin-only.
                </Text>
              </View>
            ) : (
              <View style={styles.roleButtonGrid}>
                {sortedRoles
                  .filter((role) => role.code !== "FARMER")
                  .map((role) => {
                    const active = user.roles.includes(role.code);
                    const disabled = updatingUserId === user.id;

                    return (
                      <Pressable
                        accessibilityRole="button"
                        disabled={disabled}
                        key={role.id}
                        style={[
                          styles.roleButton,
                          active && styles.roleButtonActive,
                          disabled && styles.disabledButton
                        ]}
                        onPress={() => handleToggleRole(user, role.code)}
                      >
                        <Text
                          style={[
                            styles.roleButtonText,
                            active && styles.roleButtonTextActive
                          ]}
                        >
                          {roleLabel(role.code)}
                        </Text>
                      </Pressable>
                    );
                  })}
              </View>
            )}
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            {isLoading ? "Loading role data..." : "No managed users are available."}
          </Text>
        </View>
      )}
    </View>
  );
}

function CreateStaffUserForm({
  allowedRoles,
  error,
  isSubmitting,
  onSubmit
}: {
  allowedRoles: CreateStaffRole[];
  error: string;
  isSubmitting: boolean;
  onSubmit: (input: CreateStaffUserInput) => Promise<boolean>;
}) {
  const [displayName, setDisplayName] = useState("");
  const [localError, setLocalError] = useState("");
  const [locationName, setLocationName] = useState("");
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [role, setRole] = useState<CreateStaffUserInput["role"]>(
    allowedRoles[0]
  );
  const [siteName, setSiteName] = useState("");
  const [username, setUsername] = useState("");

  useEffect(() => {
    if (!allowedRoles.includes(role)) {
      setRole(allowedRoles[0]);
    }
  }, [allowedRoles, role]);

  async function handleSubmit() {
    const input: CreateStaffUserInput = {
      displayName: displayName.trim(),
      locationName: locationName.trim(),
      password,
      phone: phone.trim(),
      role,
      siteName: siteName.trim(),
      username: username.trim()
    };

    if (!input.displayName || !input.username || !input.password) {
      setLocalError("Enter name, username, and password.");
      return;
    }

    if (input.password.length < 8) {
      setLocalError("Password must be at least 8 characters.");
      return;
    }

    setLocalError("");
    const created = await onSubmit(input);

    if (created) {
      setDisplayName("");
      setLocationName("");
      setPassword("");
      setPhone("");
      setRole(allowedRoles[0]);
      setSiteName("");
      setUsername("");
    }
  }

  return (
    <View style={styles.managementCard}>
      <View>
        <Text style={styles.cardTitle}>Create staff login</Text>
        <Text style={styles.cardDescription}>
          {allowedRoles.includes("FPO_MANAGER")
            ? "Add FPO manager or field coordinator accounts. Farmer logins are created from farmer profiles."
            : "Add field coordinator accounts for this FPO. Farmer logins are created from farmer profiles."}
        </Text>
      </View>

      <View style={styles.segmentRow}>
        {allowedRoles.map((option) => (
          <Pressable
            accessibilityRole="button"
            key={option}
            style={[
              styles.segmentButton,
              role === option && styles.segmentButtonActive
            ]}
            onPress={() => setRole(option)}
          >
            <Text
              style={[
                styles.segmentButtonText,
                role === option && styles.segmentButtonTextActive
              ]}
            >
              {roleLabel(option)}
            </Text>
          </Pressable>
        ))}
      </View>

      <View style={styles.formGrid}>
        <StaffField label="Full name" value={displayName} onChange={setDisplayName} />
        <StaffField label="Username" value={username} onChange={setUsername} />
        <StaffField
          label="Password"
          secureTextEntry
          value={password}
          onChange={setPassword}
        />
        <StaffField label="Mobile" value={phone} onChange={setPhone} />
        <StaffField
          label={role === "FPO_MANAGER" ? "FPO location" : "Assigned village"}
          value={locationName}
          onChange={setLocationName}
        />
        <StaffField
          label={role === "FPO_MANAGER" ? "FPO name" : "Taluka / FPO"}
          value={siteName}
          onChange={setSiteName}
        />
      </View>

      {localError || error ? (
        <Text style={styles.formError}>{localError || error}</Text>
      ) : null}

      <View style={styles.formActions}>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
          onPress={handleSubmit}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? "Creating..." : "Create staff login"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

function StaffField({
  label,
  onChange,
  secureTextEntry,
  value
}: {
  label: string;
  onChange: (value: string) => void;
  secureTextEntry?: boolean;
  value: string;
}) {
  return (
    <View style={styles.formField}>
      <Text style={styles.formLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        autoCorrect={false}
        onChangeText={onChange}
        secureTextEntry={secureTextEntry}
        style={styles.formInput}
        value={value}
      />
    </View>
  );
}

function sortRoleCodes(roles: BackendRoleCode[]) {
  return roles
    .slice()
    .sort((left, right) => roleOrder.indexOf(left) - roleOrder.indexOf(right));
}

function roleLabel(role: BackendRoleCode) {
  switch (role) {
    case "ADMIN":
      return "Admin";
    case "FPO_MANAGER":
      return "FPO Manager";
    case "FIELD_COORDINATOR":
      return "Field Coordinator";
    case "FARMER":
      return "Farmer";
    default:
      return role;
  }
}

const styles = StyleSheet.create({
  section: {
    gap: 14
  },
  headerRow: {
    alignItems: "center",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  userCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  userText: {
    flex: 1
  },
  managementCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  cardTitle: {
    color: "#172126",
    fontSize: 17,
    fontWeight: "800",
    marginBottom: 5
  },
  cardDescription: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  badgeRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 10
  },
  roleButtonGrid: {
    alignItems: "flex-end",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end",
    maxWidth: 330
  },
  roleButton: {
    alignItems: "center",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 96,
    paddingHorizontal: 12
  },
  roleButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  roleButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  roleButtonTextActive: {
    color: "#1f6f73"
  },
  roleLockedBox: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    maxWidth: 260,
    padding: 12
  },
  roleLockedText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  warningCard: {
    backgroundColor: "#fff8e8",
    borderColor: "#f0d38a",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  warningText: {
    color: "#8a5a00",
    fontSize: 13,
    fontWeight: "700"
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formField: {
    flex: 1,
    gap: 7,
    minWidth: 170
  },
  formLabel: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  formInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 48,
    paddingHorizontal: 12
  },
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  formActions: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 44,
    minWidth: 150,
    paddingHorizontal: 16
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 40,
    minWidth: 108,
    paddingHorizontal: 12
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  disabledButton: {
    opacity: 0.6
  },
  segmentRow: {
    backgroundColor: "#e8eef2",
    borderRadius: 8,
    flexDirection: "row",
    gap: 4,
    padding: 4
  },
  segmentButton: {
    alignItems: "center",
    borderRadius: 6,
    flex: 1,
    justifyContent: "center",
    minHeight: 38
  },
  segmentButtonActive: {
    backgroundColor: "#ffffff"
  },
  segmentButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  segmentButtonTextActive: {
    color: "#172126"
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  }
});
