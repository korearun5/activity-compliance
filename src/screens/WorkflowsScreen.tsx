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

type WorkflowTask = {
  id: string;
  title: string;
};

type Workflow = {
  id: string;
  name: string;
  code: string;
  status: string;
  durationDays: number;
  tasks: WorkflowTask[];
};

type WorkflowsScreenProps = {
  onSelectWorkflow: (workflowId: string) => void;
  onCreateActivity: (workflowId: string) => void;
};

export function WorkflowsScreen({
  onSelectWorkflow,
  onCreateActivity
}: WorkflowsScreenProps) {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);

  const loadWorkflows = useCallback(async () => {
    try {
      setError("");
      setLoading(true);
      const response = await apiClient.get<PageResponse<Workflow>>(
        endpoints.workflows.list
      );
      setWorkflows(response.content);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadWorkflows();
    } finally {
      setRefreshing(false);
    }
  }, [loadWorkflows]);

  useFocusEffect(
    useCallback(() => {
      loadWorkflows();
    }, [loadWorkflows])
  );

  function renderWorkflowItem({ item }: { item: Workflow }) {
    return (
      <Pressable
        onPress={() => onSelectWorkflow(item.id)}
        style={({ pressed }) => [
          styles.workflowCard,
          pressed && styles.workflowCardPressed
        ]}
      >
        <View style={styles.workflowHeader}>
          <Text style={styles.workflowName}>{item.name}</Text>
          <Text style={[styles.status, getStatusStyle(item.status)]}>
            {item.status}
          </Text>
        </View>
        <Text style={styles.workflowCode}>Code: {item.code}</Text>
        <Text style={styles.workflowDetails}>
          Duration: {item.durationDays} days | Tasks: {item.tasks.length}
        </Text>
        <Pressable
          onPress={() => onCreateActivity(item.id)}
          style={({ pressed }) => [
            styles.startButton,
            pressed && styles.startButtonPressed
          ]}
        >
          <Text style={styles.startButtonText}>Start Activity</Text>
        </Pressable>
      </Pressable>
    );
  }

  if (loading && workflows.length === 0) {
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
          data={workflows}
          renderItem={renderWorkflowItem}
          keyExtractor={(item) => item.id}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>No workflows available</Text>
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
    case "ACTIVE":
      return styles.statusActive;
    case "DRAFT":
      return styles.statusDraft;
    case "ARCHIVED":
      return styles.statusArchived;
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
  workflowCard: {
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
  workflowCardPressed: {
    opacity: 0.8
  },
  workflowHeader: {
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
  statusDraft: {
    backgroundColor: "#fff3cd",
    color: "#856404"
  },
  statusArchived: {
    backgroundColor: "#e2e3e5",
    color: "#383d41"
  },
  statusDefault: {
    backgroundColor: "#d1ecf1",
    color: "#0c5460"
  },
  workflowCode: {
    fontSize: 13,
    color: "#666",
    marginBottom: 4
  },
  workflowDetails: {
    fontSize: 13,
    color: "#666",
    marginBottom: 12
  },
  startButton: {
    backgroundColor: "#0066cc",
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 6,
    alignItems: "center"
  },
  startButtonPressed: {
    opacity: 0.8
  },
  startButtonText: {
    color: "white",
    fontWeight: "600",
    fontSize: 14
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
    color: "#999"
  }
});
