import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { ApprovalActions } from "../../../shared/components/ApprovalActions";
import {
  documentTypeLabel,
  FarmerDocumentRecord,
  listPendingFarmerDocuments,
  verifyFarmerDocument
} from "../../../shared/farmers/documentStore";
import { StateCard } from "../../../ui/StateCard";
import { StatusBadge } from "../../../ui/StatusBadge";

export function AdminDocumentVerification() {
  const [documents, setDocuments] = useState<FarmerDocumentRecord[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [notesById, setNotesById] = useState<Record<string, string>>({});
  const [savingId, setSavingId] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadPendingDocuments();
  }, []);

  async function loadPendingDocuments() {
    setIsLoading(true);
    setError("");

    try {
      setDocuments(await listPendingFarmerDocuments());
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "Unable to load pending farmer documents."
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function handleVerify(
    document: FarmerDocumentRecord,
    status: "VERIFIED" | "REJECTED"
  ) {
    setSavingId(document.id);
    setError("");
    setSuccessMessage("");

    try {
      await verifyFarmerDocument(document.id, {
        notes: notesById[document.id],
        status
      });
      setSuccessMessage(
        `${document.farmerName} ${documentTypeLabel(document.documentType).toLowerCase()} ${status === "VERIFIED" ? "approved" : "rejected"}.`
      );
      setNotesById((current) => {
        const next = { ...current };
        delete next[document.id];
        return next;
      });
      await loadPendingDocuments();
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : "Unable to update document verification."
      );
    } finally {
      setSavingId(null);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View style={styles.headerText}>
          <Text style={styles.subsectionTitle}>Document verification</Text>
          <Text style={styles.panelMeta}>
            KYC and Carbon onboarding documents waiting for staff review.
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadPendingDocuments}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <StateCard message={error} tone="error" /> : null}
      {successMessage ? <StateCard message={successMessage} tone="success" /> : null}

      {documents.length ? (
        <View style={styles.table}>
          {documents.map((document) => (
            <View key={document.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>{document.farmerName}</Text>
                <Text style={styles.rowMeta}>{document.farmerMobileNumber}</Text>
                <Text style={styles.rowMeta}>
                  {documentTypeLabel(document.documentType)} - {document.fileName}
                </Text>
                <Text style={styles.rowMeta}>{document.mimeType}</Text>
              </View>
              <View style={styles.actionBlock}>
                <StatusBadge label="Pending" tone="warning" />
                <TextInput
                  multiline
                  onChangeText={(value) =>
                    setNotesById((current) => ({
                      ...current,
                      [document.id]: value
                    }))
                  }
                  placeholder="Notes"
                  placeholderTextColor="#8a99a1"
                  style={styles.notesInput}
                  value={notesById[document.id] ?? ""}
                />
                <ApprovalActions
                  isSubmitting={savingId === document.id}
                  onApprove={() => handleVerify(document, "VERIFIED")}
                  onReject={() => handleVerify(document, "REJECTED")}
                />
              </View>
            </View>
          ))}
        </View>
      ) : (
        <StateCard
          message={
            isLoading
              ? "Loading pending documents..."
              : "No farmer documents are pending verification."
          }
          tone="empty"
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  actionBlock: {
    alignItems: "flex-end",
    gap: 8,
    minWidth: 220
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
