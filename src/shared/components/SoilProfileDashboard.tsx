import { ReactNode } from "react";
import { StyleSheet, Text, View } from "react-native";

import {
  SoilProfileList,
  SoilProfileListRecord
} from "./SoilProfileList";

export type SoilDashboardMetric = {
  label: string;
  value: number | string;
};

export type SoilDashboardRecord = SoilProfileListRecord & {
  metrics: SoilDashboardMetric[];
  socPercent?: number;
};

type SoilProfileDashboardProps<T extends SoilDashboardRecord> = {
  description?: string;
  emptyMessage: string;
  fallbackMetrics?: SoilDashboardMetric[];
  onSelectRecord?: (record: T) => void;
  records: T[];
  renderHeaderAction?: () => ReactNode;
  renderRecordAction?: (record: T) => ReactNode;
  selectedRecordId?: string | null;
  title: string;
};

export function SoilProfileDashboard<T extends SoilDashboardRecord>({
  description,
  emptyMessage,
  fallbackMetrics = [],
  onSelectRecord,
  records,
  renderHeaderAction,
  renderRecordAction,
  selectedRecordId,
  title
}: SoilProfileDashboardProps<T>) {
  const selectedRecord =
    records.find((record) => record.id === selectedRecordId) ?? records[0] ?? null;
  const metrics = selectedRecord?.metrics.length
    ? selectedRecord.metrics
    : fallbackMetrics;

  return (
    <View style={styles.dashboard}>
      <View style={styles.header}>
        <View style={styles.headerText}>
          <Text style={styles.title}>{title}</Text>
          {description ? <Text style={styles.description}>{description}</Text> : null}
        </View>
        {renderHeaderAction?.()}
      </View>

      {metrics.length ? (
        <View style={styles.metricGrid}>
          {metrics.map((metric) => (
            <View key={metric.label} style={styles.metric}>
              <Text style={styles.metricLabel}>{metric.label}</Text>
              <Text style={styles.metricValue}>{metric.value}</Text>
            </View>
          ))}
        </View>
      ) : null}

      {records.length ? (
        <View style={styles.contentGrid}>
          <SoilTrend records={records} />
          <SoilProfileList
            emptyMessage={emptyMessage}
            records={records}
            selectedRecordId={selectedRecord?.id}
            onSelectRecord={onSelectRecord}
            renderRecordAction={renderRecordAction}
          />
        </View>
      ) : (
        <SoilProfileList emptyMessage={emptyMessage} records={records} />
      )}
    </View>
  );
}

function SoilTrend<T extends SoilDashboardRecord>({ records }: { records: T[] }) {
  const trendRecords = records
    .filter((record) => record.socPercent !== undefined)
    .slice()
    .reverse();

  if (!trendRecords.length) {
    return (
      <View style={styles.trendPanel}>
        <Text style={styles.trendTitle}>SOC trend</Text>
        <Text style={styles.trendEmpty}>Add SOC values to see the trend.</Text>
      </View>
    );
  }

  const maxSoc = Math.max(...trendRecords.map((record) => record.socPercent ?? 0), 1);

  return (
    <View style={styles.trendPanel}>
      <Text style={styles.trendTitle}>SOC trend</Text>
      <View style={styles.trendBars}>
        {trendRecords.map((record) => {
          const value = record.socPercent ?? 0;
          const height = Math.max(12, Math.round((value / maxSoc) * 88));

          return (
            <View key={record.id} style={styles.trendBarItem}>
              <View style={styles.trendBarTrack}>
                <View style={[styles.trendBarFill, { height }]} />
              </View>
              <Text style={styles.trendValue}>{value}%</Text>
            </View>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  contentGrid: {
    gap: 12
  },
  dashboard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
  },
  description: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  header: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  headerText: {
    flex: 1
  },
  metric: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    flex: 1,
    minWidth: 120,
    padding: 12
  },
  metricGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  metricLabel: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 4
  },
  metricValue: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  title: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  trendBarFill: {
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    width: "100%"
  },
  trendBarItem: {
    alignItems: "center",
    flex: 1,
    gap: 6,
    minWidth: 44
  },
  trendBars: {
    alignItems: "flex-end",
    flexDirection: "row",
    gap: 8,
    minHeight: 112
  },
  trendBarTrack: {
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    height: 96,
    justifyContent: "flex-end",
    overflow: "hidden",
    width: "100%"
  },
  trendEmpty: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  trendPanel: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 10,
    padding: 12
  },
  trendTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  trendValue: {
    color: "#53666f",
    fontSize: 11,
    fontWeight: "800"
  }
});
