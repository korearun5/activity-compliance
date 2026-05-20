import { ReactNode } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { StateCard } from "../../ui/StateCard";
import { StatusBadge } from "../../ui/StatusBadge";

export type SoilProfileStatusTone = "danger" | "good" | "neutral" | "warning";

export type SoilProfileListRecord = {
  id: string;
  metaLines?: string[];
  reportLabel?: string;
  statusLabel: string;
  statusTone?: SoilProfileStatusTone;
  testDate?: string;
  title: string;
};

type SoilProfileListProps<T extends SoilProfileListRecord> = {
  deleteLabel?: string;
  editLabel?: string;
  emptyMessage: string;
  onDeleteRecord?: (record: T) => void;
  onEditRecord?: (record: T) => void;
  onSelectRecord?: (record: T) => void;
  records: T[];
  renderRecordAction?: (record: T) => ReactNode;
  selectedRecordId?: string | null;
};

export function SoilProfileList<T extends SoilProfileListRecord>({
  deleteLabel = "Delete",
  editLabel = "Edit",
  emptyMessage,
  onDeleteRecord,
  onEditRecord,
  onSelectRecord,
  records,
  renderRecordAction,
  selectedRecordId
}: SoilProfileListProps<T>) {
  const selectedRecord =
    records.find((record) => record.id === selectedRecordId) ?? records[0] ?? null;

  if (!records.length) {
    return <StateCard message={emptyMessage} tone="empty" />;
  }

  return (
    <View style={styles.recordList}>
      {records.map((record) => {
        const isSelected = selectedRecord?.id === record.id;
        const hasRowAction =
          Boolean(onEditRecord) ||
          Boolean(onDeleteRecord) ||
          Boolean(renderRecordAction);

        return (
          <Pressable
            key={record.id}
            accessibilityRole={onSelectRecord ? "button" : undefined}
            disabled={!onSelectRecord}
            style={[styles.recordRow, isSelected && styles.selectedRecord]}
            onPress={() => onSelectRecord?.(record)}
          >
            <View style={styles.recordText}>
              <Text style={styles.recordTitle}>{record.title}</Text>
              {record.testDate ? (
                <Text style={styles.recordMeta}>{record.testDate}</Text>
              ) : null}
              {record.reportLabel ? (
                <Text style={styles.recordMeta}>{record.reportLabel}</Text>
              ) : null}
              {record.metaLines?.map((line) => (
                <Text key={line} style={styles.recordMeta}>
                  {line}
                </Text>
              ))}
            </View>
            <View style={styles.recordActions}>
              <StatusBadge
                label={record.statusLabel}
                tone={record.statusTone ?? "neutral"}
              />
              {hasRowAction ? (
                <View style={styles.actionStack}>
                  {renderRecordAction?.(record)}
                  {onEditRecord ? (
                    <Pressable
                      accessibilityRole="button"
                      style={styles.actionButton}
                      onPress={() => onEditRecord(record)}
                    >
                      <Text style={styles.actionButtonText}>{editLabel}</Text>
                    </Pressable>
                  ) : null}
                  {onDeleteRecord ? (
                    <Pressable
                      accessibilityRole="button"
                      style={styles.dangerButton}
                      onPress={() => onDeleteRecord(record)}
                    >
                      <Text style={styles.dangerButtonText}>{deleteLabel}</Text>
                    </Pressable>
                  ) : null}
                </View>
              ) : null}
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  actionButton: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 34,
    paddingHorizontal: 12
  },
  actionButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  actionStack: {
    alignItems: "flex-end",
    gap: 8
  },
  dangerButton: {
    alignItems: "center",
    backgroundColor: "#fff4f2",
    borderColor: "#f0b8ad",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 34,
    paddingHorizontal: 12
  },
  dangerButtonText: {
    color: "#b53b2f",
    fontSize: 13,
    fontWeight: "800"
  },
  recordActions: {
    alignItems: "flex-end",
    gap: 8
  },
  recordList: {
    gap: 10
  },
  recordMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  recordRow: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 14
  },
  recordText: {
    flex: 1
  },
  recordTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800",
    marginBottom: 4
  },
  selectedRecord: {
    backgroundColor: "#eef7f7",
    borderColor: "#1f6f73",
    borderWidth: 2
  }
});
