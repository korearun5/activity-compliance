import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import {
  FarmerBankDetailsRecord,
  listPendingFarmerBankDetails,
  verifyFarmerBankDetails
} from "../../../shared/farmers/bankDetailsStore";
import { StateCard } from "../../../ui/StateCard";
import { StatusBadge } from "../../../ui/StatusBadge";

export function AdminBankVerification() {
  const [bankDetails, setBankDetails] = useState<FarmerBankDetailsRecord[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [notesById, setNotesById] = useState<Record<string, string>>({});
  const [savingId, setSavingId] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadPendingBankDetails();
  }, []);

  async function loadPendingBankDetails() {
    setIsLoading(true);
    setError("");

    try {
      setBankDetails(await listPendingFarmerBankDetails());
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "Unable to load pending bank details."
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function handleVerify(
    details: FarmerBankDetailsRecord,
    status: "VERIFIED" | "REJECTED"
  ) {
    setSavingId(details.id);
    setError("");
    setSuccessMessage("");

    try {
      await verifyFarmerBankDetails(details.id, {
        notes: notesById[details.id],
        status
      });
      setSuccessMessage(
        `${details.farmerName} bank details ${status === "VERIFIED" ? "approved" : "rejected"}.`
      );
      setNotesById((current) => {
        const next = { ...current };
        delete next[details.id];
        return next;
      });
      await loadPendingBankDetails();
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : "Unable to update bank details."
      );
    } finally {
      setSavingId(null);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View style={styles.headerText}>
          <Text style={styles.subsectionTitle}>Bank verification</Text>
          <Text style={styles.panelMeta}>
            Pending farmer bank details can be approved or rejected for UAT.
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadPendingBankDetails}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <StateCard message={error} tone="error" /> : null}
      {successMessage ? <StateCard message={successMessage} tone="success" /> : null}

      {bankDetails.length ? (
        <View style={styles.table}>
          {bankDetails.map((details) => (
            <View key={details.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>{details.farmerName}</Text>
                <Text style={styles.rowMeta}>{details.farmerMobileNumber}</Text>
                <Text style={styles.rowMeta}>
                  {maskAccountNumber(details.accountNumber)} - {details.ifscCode}
                </Text>
                <Text style={styles.rowMeta}>{details.bankName}</Text>
              </View>
              <View style={styles.actionBlock}>
                <StatusBadge label="Pending" tone="warning" />
                <TextInput
                  multiline
                  onChangeText={(value) =>
                    setNotesById((current) => ({
                      ...current,
                      [details.id]: value
                    }))
                  }
                  placeholder="Notes"
                  placeholderTextColor="#8a99a1"
                  style={styles.notesInput}
                  value={notesById[details.id] ?? ""}
                />
                <View style={styles.buttonRow}>
                  <Pressable
                    accessibilityRole="button"
                    disabled={savingId === details.id}
                    style={[styles.primaryButton, savingId === details.id && styles.disabledButton]}
                    onPress={() => handleVerify(details, "VERIFIED")}
                  >
                    <Text style={styles.primaryButtonText}>Approve</Text>
                  </Pressable>
                  <Pressable
                    accessibilityRole="button"
                    disabled={savingId === details.id}
                    style={[styles.dangerButton, savingId === details.id && styles.disabledButton]}
                    onPress={() => handleVerify(details, "REJECTED")}
                  >
                    <Text style={styles.dangerButtonText}>Reject</Text>
                  </Pressable>
                </View>
              </View>
            </View>
          ))}
        </View>
      ) : (
        <StateCard
          message={
            isLoading
              ? "Loading pending bank details..."
              : "No bank details are pending verification."
          }
          tone="empty"
        />
      )}
    </View>
  );
}

function maskAccountNumber(accountNumber: string) {
  const suffix = accountNumber.slice(-4);
  return suffix ? `****${suffix}` : "****";
}

const styles = StyleSheet.create({
  actionBlock: {
    alignItems: "flex-end",
    gap: 8,
    minWidth: 220
  },
  buttonRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end"
  },
  dangerButton: {
    alignItems: "center",
    backgroundColor: "#fde8e7",
    borderColor: "#f7b8b2",
    borderRadius: 8,
    borderWidth: 1,
    minHeight: 38,
    paddingHorizontal: 12,
    justifyContent: "center"
  },
  dangerButtonText: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "800"
  },
  disabledButton: {
    opacity: 0.55
  },
  headerText: {
    flex: 1
  },
  notesInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 13,
    minHeight: 40,
    paddingHorizontal: 10,
    paddingVertical: 8,
    width: "100%"
  },
  panel: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  panelHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  panelMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    minHeight: 38,
    paddingHorizontal: 12,
    justifyContent: "center"
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  row: {
    alignItems: "flex-start",
    backgroundColor: "#f7fafb",
    borderColor: "#edf3f6",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    justifyContent: "space-between",
    padding: 12
  },
  rowMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  rowText: {
    flex: 1,
    minWidth: 220
  },
  rowTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800",
    marginBottom: 4
  },
  secondaryButton: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 9
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  table: {
    gap: 10
  }
});
