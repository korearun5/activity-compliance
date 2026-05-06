import { useFocusEffect } from "@react-navigation/native";
import { useCallback, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View
} from "react-native";

import { apiClient } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import { getErrorMessage } from "../core/errors/AppError";

type Evidence = {
  id: string;
  originalFilename: string;
  sizeBytes: number;
  contentType: string;
  status: string;
  submittedAt: string;
  taskTitle?: string;
  participantName?: string;
  note?: string;
};

type EvidenceScreenProps = {
  onSelectEvidence: (evidenceId: string) => void;
  onUploadEvidence: () => void;
  activityId?: string;
};

export function EvidenceScreen({
  onSelectEvidence,
  onUploadEvidence,
  activityId
}: EvidenceScreenProps) {
  const [evidenceList, setEvidenceList] = useState<Evidence[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);

  const loadEvidence = useCallback(async () => {
    try {
      setError("");
      setLoading(true);
      const params = activityId ? `?activityId=${encodeURIComponent(activityId)}` : "";
      const response = await apiClient.get<Evidence[]>(
        endpoints.evidence.list + params
      );
      setEvidenceList(response);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [activityId]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadEvidence();
    } finally {
      setRefreshing(false);
    }
  }, [loadEvidence]);

  useFocusEffect(
    useCallback(() => {
      loadEvidence();
    }, [loadEvidence])
  );

  function formatFileSize(bytes: number): string {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${Math.round((bytes / Math.pow(k, i)) * 100) / 100} ${sizes[i]}`;
  }

  function renderEvidenceItem({ item }: { item: Evidence }) {
    const isImage = item.contentType.startsWith("image/");

    return (
      <Pressable
        onPress={() => onSelectEvidence(item.id)}
        style={({ pressed }) => [
          styles.evidenceCard,
          pressed && styles.evidenceCardPressed
        ]}
      >
        <View style={styles.evidenceContent}>
          <View style={isImage ? styles.imagePreview : styles.fileIcon}>
            <Text style={isImage ? styles.previewText : styles.fileIconText}>
              {isImage ? "Photo" : "File"}
            </Text>
          </View>
          <View style={styles.evidenceInfo}>
            <Text style={styles.fileName} numberOfLines={2}>
              {item.originalFilename}
            </Text>
            <Text style={styles.fileSize}>{formatFileSize(item.sizeBytes)}</Text>
            {item.taskTitle ? (
              <Text style={styles.note} numberOfLines={1}>
                {item.taskTitle}
              </Text>
            ) : null}
            {item.note ? (
              <Text style={styles.note} numberOfLines={1}>
                {item.note}
              </Text>
            ) : null}
            <Text style={styles.uploadedAt}>Uploaded: {item.submittedAt}</Text>
          </View>
        </View>
        <Text style={[styles.status, getStatusStyle(item.status)]}>{item.status}</Text>
      </Pressable>
    );
  }

  if (loading && evidenceList.length === 0) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.centerContent}>
          <ActivityIndicator size="large" color="#0066cc" />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {error ? (
        <ScrollView
          style={styles.errorContainer}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
        >
          <Text style={styles.errorText}>{error}</Text>
          <Pressable onPress={onRefresh} style={styles.retryButton}>
            <Text style={styles.retryButtonText}>Retry</Text>
          </Pressable>
        </ScrollView>
      ) : (
        <FlatList
          data={evidenceList}
          renderItem={renderEvidenceItem}
          keyExtractor={(item) => item.id}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyIcon}>No files</Text>
              <Text style={styles.emptyText}>No evidence uploaded</Text>
              <Pressable onPress={onUploadEvidence} style={styles.uploadButton}>
                <Text style={styles.uploadButtonText}>Upload Evidence</Text>
              </Pressable>
            </View>
          }
          ListFooterComponent={
            evidenceList.length > 0 ? (
              <Pressable onPress={onUploadEvidence} style={styles.uploadFab}>
                <Text style={styles.uploadFabText}>Upload Evidence</Text>
              </Pressable>
            ) : null
          }
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
          contentContainerStyle={styles.listContent}
        />
      )}
    </SafeAreaView>
  );
}

function getStatusStyle(status: string) {
  switch (status) {
    case "PENDING_REVIEW":
      return styles.statusPending;
    case "APPROVED":
      return styles.statusApproved;
    case "REJECTED":
      return styles.statusRejected;
    default:
      return styles.statusDefault;
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f5f5f5"
  },
  centerContent: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center"
  },
  listContent: {
    padding: 12,
    paddingBottom: 20
  },
  evidenceCard: {
    backgroundColor: "white",
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3
  },
  evidenceCardPressed: {
    opacity: 0.8
  },
  evidenceContent: {
    flexDirection: "row",
    marginBottom: 8
  },
  imagePreview: {
    width: 60,
    height: 60,
    backgroundColor: "#e3f2fd",
    borderRadius: 6,
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12
  },
  previewText: {
    fontSize: 12,
    fontWeight: "700",
    color: "#0c5460"
  },
  fileIcon: {
    width: 60,
    height: 60,
    backgroundColor: "#f5f5f5",
    borderRadius: 6,
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
    borderWidth: 1,
    borderColor: "#ddd"
  },
  fileIconText: {
    fontSize: 12,
    fontWeight: "700",
    color: "#383d41"
  },
  evidenceInfo: {
    flex: 1
  },
  fileName: {
    fontSize: 14,
    fontWeight: "600",
    color: "#222",
    marginBottom: 4
  },
  fileSize: {
    fontSize: 12,
    color: "#666",
    marginBottom: 2
  },
  note: {
    fontSize: 12,
    color: "#666",
    fontStyle: "italic",
    marginBottom: 4
  },
  uploadedAt: {
    fontSize: 11,
    color: "#999"
  },
  status: {
    fontSize: 12,
    fontWeight: "600",
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    alignSelf: "flex-start"
  },
  statusPending: {
    backgroundColor: "#fff3cd",
    color: "#856404"
  },
  statusApproved: {
    backgroundColor: "#d4edda",
    color: "#155724"
  },
  statusRejected: {
    backgroundColor: "#f8d7da",
    color: "#842029"
  },
  statusDefault: {
    backgroundColor: "#e2e3e5",
    color: "#383d41"
  },
  errorContainer: {
    flex: 1
  },
  errorText: {
    color: "#d32f2f",
    fontSize: 14,
    padding: 16,
    textAlign: "center"
  },
  retryButton: {
    backgroundColor: "#0066cc",
    paddingVertical: 10,
    marginHorizontal: 16,
    borderRadius: 6,
    alignItems: "center"
  },
  retryButtonText: {
    color: "white",
    fontWeight: "600",
    fontSize: 14
  },
  emptyContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 40
  },
  emptyIcon: {
    color: "#666",
    fontSize: 13,
    fontWeight: "700",
    marginBottom: 12
  },
  emptyText: {
    fontSize: 16,
    color: "#999",
    marginBottom: 16
  },
  uploadButton: {
    backgroundColor: "#0066cc",
    paddingVertical: 10,
    paddingHorizontal: 24,
    borderRadius: 6
  },
  uploadButtonText: {
    color: "white",
    fontWeight: "600",
    fontSize: 14
  },
  uploadFab: {
    backgroundColor: "#0066cc",
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 6,
    alignItems: "center",
    marginTop: 8
  },
  uploadFabText: {
    color: "white",
    fontWeight: "600",
    fontSize: 14
  }
});
