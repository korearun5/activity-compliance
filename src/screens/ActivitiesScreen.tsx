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
import { PageResponse } from "../core/api/contracts";
import { endpoints } from "../core/api/endpoints";
import { getErrorMessage } from "../core/errors/AppError";

type Activity = {
  id: string;
  workflowName: string;
  unitName: string;
  status: string;
  progressPercent: number;
  startedOn: string;
  expectedCompletion: string;
  participantName?: string;
};

type ActivitiesScreenProps = {
  onSelectActivity: (activityId: string) => void;
  onStartActivity: () => void;
};

export function ActivitiesScreen({
  onSelectActivity,
  onStartActivity
}: ActivitiesScreenProps) {
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);

  const loadActivities = useCallback(async () => {
    try {
      setError("");
      setLoading(true);
      const response = await apiClient.get<PageResponse<Activity>>(
        endpoints.activities.list
      );
      setActivities(response.content);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadActivities();
    } finally {
      setRefreshing(false);
    }
  }, [loadActivities]);

  useFocusEffect(
    useCallback(() => {
      loadActivities();
    }, [loadActivities])
  );

  function renderActivityItem({ item }: { item: Activity }) {
    return (
      <Pressable
        onPress={() => onSelectActivity(item.id)}
        style={({ pressed }) => [
          styles.activityCard,
          pressed && styles.activityCardPressed
        ]}
      >
        <View style={styles.activityHeader}>
          <Text style={styles.workflowName}>{item.workflowName}</Text>
          <Text style={[styles.status, getStatusStyle(item.status)]}>
            {item.status}
          </Text>
        </View>
        <Text style={styles.unitName}>{item.unitName}</Text>
        {item.participantName && (
          <Text style={styles.participantName}>
            Participant: {item.participantName}
          </Text>
        )}
        <View style={styles.progressContainer}>
          <View style={styles.progressBar}>
            <View
              style={[
                styles.progressFill,
                { width: `${Math.min(item.progressPercent, 100)}%` }
              ]}
            />
          </View>
          <Text style={styles.progressText}>{item.progressPercent}%</Text>
        </View>
        <View style={styles.dateContainer}>
          <Text style={styles.dateLabel}>Started: {item.startedOn}</Text>
          <Text style={styles.dateLabel}>Due: {item.expectedCompletion}</Text>
        </View>
      </Pressable>
    );
  }

  if (loading && activities.length === 0) {
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
          data={activities}
          renderItem={renderActivityItem}
          keyExtractor={(item) => item.id}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>No activities yet</Text>
              <Pressable onPress={onStartActivity} style={styles.startButton}>
                <Text style={styles.startButtonText}>Start Activity</Text>
              </Pressable>
            </View>
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
    case "RUNNING":
      return styles.statusActive;
    case "COMPLETED":
      return styles.statusCompleted;
    case "CANCELLED":
      return styles.statusCancelled;
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
  activityCard: {
    backgroundColor: "white",
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3
  },
  activityCardPressed: {
    opacity: 0.8
  },
  activityHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8
  },
  workflowName: {
    fontSize: 16,
    fontWeight: "600",
    color: "#222",
    flex: 1
  },
  status: {
    fontSize: 12,
    fontWeight: "600",
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4
  },
  statusActive: {
    backgroundColor: "#d4edda",
    color: "#155724"
  },
  statusCompleted: {
    backgroundColor: "#cfe2ff",
    color: "#084298"
  },
  statusCancelled: {
    backgroundColor: "#f8d7da",
    color: "#842029"
  },
  statusDefault: {
    backgroundColor: "#e2e3e5",
    color: "#383d41"
  },
  unitName: {
    fontSize: 14,
    fontWeight: "500",
    color: "#222",
    marginBottom: 4
  },
  participantName: {
    fontSize: 13,
    color: "#666",
    marginBottom: 8
  },
  progressContainer: {
    flexDirection: "row",
    alignItems: "center",
    marginVertical: 8
  },
  progressBar: {
    flex: 1,
    height: 8,
    backgroundColor: "#e0e0e0",
    borderRadius: 4,
    marginRight: 8,
    overflow: "hidden"
  },
  progressFill: {
    height: "100%",
    backgroundColor: "#0066cc"
  },
  progressText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#0066cc",
    minWidth: 35
  },
  dateContainer: {
    marginTop: 8
  },
  dateLabel: {
    fontSize: 12,
    color: "#666",
    marginVertical: 2
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
  emptyText: {
    fontSize: 16,
    color: "#999",
    marginBottom: 16
  },
  startButton: {
    backgroundColor: "#0066cc",
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 6
  },
  startButtonText: {
    color: "white",
    fontWeight: "600",
    fontSize: 14
  }
});
