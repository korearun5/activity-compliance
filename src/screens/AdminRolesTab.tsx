import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import {
  BackendRole,
  BackendRoleCode,
  getBackendRoleManagedUsers,
  getBackendRoles,
  RoleManagedUser,
  updateBackendUserRoles
} from "../data/roleStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminRolesTabProps = {
  canUseBackend: boolean;
};

const roleOrder: BackendRoleCode[] = ["ADMIN", "FPO_MANAGER", "FIELD_COORDINATOR"];

export function AdminRolesTab({ canUseBackend }: AdminRolesTabProps) {
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [roles, setRoles] = useState<BackendRole[]>([]);
  const [updatingUserId, setUpdatingUserId] = useState<string | null>(null);
  const [users, setUsers] = useState<RoleManagedUser[]>([]);

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

  async function handleToggleRole(user: RoleManagedUser, role: BackendRoleCode) {
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
            <View style={styles.roleButtonGrid}>
              {sortedRoles.map((role) => {
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
    default:
      return "Field Coordinator";
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
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
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
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  }
});
