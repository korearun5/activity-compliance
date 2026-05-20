import { ReactNode } from "react";
import { StyleSheet, Text, View } from "react-native";

import { StatusBadge } from "../../ui/StatusBadge";

export type ActivityTimelineItem = {
  id: string;
  metaLines?: Array<string | null | undefined>;
  statusLabel?: string;
  statusTone?: "good" | "neutral" | "warning";
  title: string;
};

type ActivityTimelineProps<T extends ActivityTimelineItem> = {
  emptyMessage: string;
  items: T[];
  renderAction?: (item: T) => ReactNode;
};

export function ActivityTimeline<T extends ActivityTimelineItem>({
  emptyMessage,
  items,
  renderAction
}: ActivityTimelineProps<T>) {
  if (!items.length) {
    return <Text style={styles.emptyText}>{emptyMessage}</Text>;
  }

  return (
    <View style={styles.timeline}>
      {items.map((item) => (
        <View key={item.id} style={styles.timelineRow}>
          <View style={styles.timelineMarker} />
          <View style={styles.timelineCard}>
            <View style={styles.timelineText}>
              <Text style={styles.timelineTitle}>{item.title}</Text>
              {item.metaLines
                ?.filter((line): line is string => Boolean(line))
                .map((line) => (
                  <Text key={line} style={styles.timelineMeta}>
                    {line}
                  </Text>
                ))}
            </View>
            <View style={styles.actionColumn}>
              {item.statusLabel ? (
                <StatusBadge
                  label={item.statusLabel}
                  tone={item.statusTone ?? "neutral"}
                />
              ) : null}
              {renderAction?.(item)}
            </View>
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  actionColumn: {
    alignItems: "flex-end",
    gap: 8
  },
  emptyText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  },
  timeline: {
    gap: 10
  },
  timelineCard: {
    alignItems: "flex-start",
    backgroundColor: "#f7fafb",
    borderColor: "#edf3f6",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 12
  },
  timelineMarker: {
    backgroundColor: "#1f6f73",
    borderRadius: 6,
    height: 12,
    marginTop: 16,
    width: 12
  },
  timelineMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18,
    marginTop: 4
  },
  timelineRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 10
  },
  timelineText: {
    flex: 1
  },
  timelineTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  }
});
