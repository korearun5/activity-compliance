import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import {
  exportFpoOperationsWorkbook,
  FpoDashboardSummary,
  getFpoDashboardSummary
} from "../data/fpoReportStore";
import { ReportExport } from "../data/reportStore";

export function AdminFpoReportsPanel() {
  const [error, setError] = useState("");
  const [exportRecord, setExportRecord] = useState<ReportExport | null>(null);
  const [isExporting, setIsExporting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [summary, setSummary] = useState<FpoDashboardSummary | null>(null);

  const summaryCards = useMemo(
    () => [
      { label: "Members", value: String(summary?.totalMembers ?? 0) },
      { label: "Active plots", value: String(summary?.activePlots ?? 0) },
      {
        label: "Confirmed acres",
        value: formatNumber(summary?.confirmedPlannedAreaAcres ?? 0)
      },
      { label: "Demand rows", value: String(summary?.demandEstimateCount ?? 0) }
    ],
    [summary]
  );

  useEffect(() => {
    loadSummary();
  }, []);

  async function loadSummary() {
    setIsLoading(true);
    setError("");

    try {
      setSummary(await getFpoDashboardSummary());
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load FPO dashboard summary."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleExport() {
    setIsExporting(true);
    setError("");

    try {
      setExportRecord(await exportFpoOperationsWorkbook());
    } catch (exportError) {
      setError(getErrorMessage(exportError, "Unable to export FPO Excel report."));
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.headerRow}>
        <View style={styles.headerText}>
          <Text style={styles.title}>FPO dashboard</Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadSummary}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      <View style={styles.summaryGrid}>
        {summaryCards.map((item) => (
          <View key={item.label} style={styles.summaryCard}>
            <Text style={styles.summaryValue}>{item.value}</Text>
            <Text style={styles.summaryLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      <View style={styles.metricsGrid}>
        <Metric label="Active members" value={summary?.activeMembers ?? 0} />
        <Metric
          label="Active land acres"
          value={formatNumber(summary?.activeLandAreaAcres ?? 0)}
        />
        <Metric
          label="Cultivable acres"
          value={formatNumber(summary?.totalCultivableAreaAcres ?? 0)}
        />
        <Metric label="Geo-tagged plots" value={summary?.geoTaggedPlots ?? 0} />
      </View>

      <View style={styles.actions}>
        <Pressable
          accessibilityRole="button"
          disabled={isExporting}
          style={[styles.primaryButton, isExporting && styles.disabledButton]}
          onPress={handleExport}
        >
          <Text style={styles.primaryButtonText}>
            {isExporting ? "Exporting..." : "Export FPO Excel"}
          </Text>
        </Pressable>
        {exportRecord ? (
          <Text style={styles.exportText}>
            Export {exportRecord.status.toLowerCase()}: {exportRecord.storageKey}
          </Text>
        ) : null}
      </View>

      <BreakdownList
        emptyLabel="No confirmed crop plans available."
        items={summary?.cropPlanAreaByCrop ?? []}
        title="Crop acreage"
      />
      <BreakdownList
        emptyLabel="No village acreage available."
        items={summary?.cropPlanAreaByVillage ?? []}
        title="Village acreage"
      />
      <InputDemandList items={summary?.inputDemandByInput ?? []} />
    </View>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <View style={styles.metricRow}>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={styles.metricValue}>{value}</Text>
    </View>
  );
}

function BreakdownList({
  emptyLabel,
  items,
  title
}: {
  emptyLabel: string;
  items: FpoDashboardSummary["cropPlanAreaByCrop"];
  title: string;
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>{title}</Text>
      {items.length ? (
        items.slice(0, 8).map((item) => (
          <View key={`${title}:${item.id ?? item.label}`} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{item.label}</Text>
              <Text style={styles.rowMeta}>
                {formatNumber(item.areaAcres)} acres - {item.planCount} plan
                {item.planCount === 1 ? "" : "s"} - {item.memberCount} farmer
                {item.memberCount === 1 ? "" : "s"}
              </Text>
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>{emptyLabel}</Text>
      )}
    </View>
  );
}

function InputDemandList({
  items
}: {
  items: FpoDashboardSummary["inputDemandByInput"];
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Input demand</Text>
      {items.length ? (
        items.slice(0, 8).map((item) => (
          <View key={item.inputId} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>
                {item.inputName} ({item.inputCode})
              </Text>
              <Text style={styles.rowMeta}>
                {formatNumber(item.estimatedQuantity)} {item.unit} - {item.planCount}{" "}
                plan{item.planCount === 1 ? "" : "s"} - {item.memberCount} farmer
                {item.memberCount === 1 ? "" : "s"}
              </Text>
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>No input demand estimate available.</Text>
      )}
    </View>
  );
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

const styles = StyleSheet.create({
  panel: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  headerRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  headerText: {
    flex: 1
  },
  title: {
    color: "#172126",
    fontSize: 17,
    fontWeight: "800"
  },
  summaryGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  summaryCard: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    flex: 1,
    minWidth: 132,
    padding: 14
  },
  summaryValue: {
    color: "#1f6f73",
    fontSize: 23,
    fontWeight: "800",
    marginBottom: 4
  },
  summaryLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  metricsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  metricRow: {
    borderColor: "#e1e9ed",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 160,
    padding: 12
  },
  metricLabel: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 4
  },
  metricValue: {
    color: "#172126",
    fontSize: 16,
    fontWeight: "800"
  },
  listBlock: {
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    gap: 8,
    paddingTop: 12
  },
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  row: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    padding: 12
  },
  rowText: {
    flex: 1
  },
  rowTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  rowMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  actions: {
    alignItems: "flex-start",
    gap: 8
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 150,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 92,
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
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  emptyText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  },
  exportText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  }
});
