import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { getErrorMessage } from "../../../core/errors/AppError";
import { ReportExport } from "../../../data/reportStore";
import { StateCard } from "../../../ui/StateCard";
import {
  CarbonActivityReportBreakdown,
  CarbonReportBreakdown,
  CarbonReportFilters,
  CarbonReportSummary,
  exportCarbonOperationsWorkbook,
  getCarbonReportSummary
} from "../data/carbonReportStore";

export function CarbonReportsPanel() {
  const [error, setError] = useState("");
  const [exportRecord, setExportRecord] = useState<ReportExport | null>(null);
  const [filters, setFilters] = useState<CarbonReportFilters>({});
  const [isExporting, setIsExporting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [summary, setSummary] = useState<CarbonReportSummary | null>(null);

  const summaryCards = useMemo(
    () => [
      { label: "Farmers", value: String(summary?.totalProfiles ?? 0) },
      {
        label: "Plot area",
        value: `${formatNumber(summary?.totalPlotAreaAcres ?? 0)} ac`
      },
      { label: "Soil records", value: String(summary?.soilProfileCount ?? 0) },
      { label: "Activities", value: String(summary?.activityCount ?? 0) }
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
      setSummary(await getCarbonReportSummary());
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load Carbon report summary."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleExport() {
    setIsExporting(true);
    setError("");
    setExportRecord(null);

    try {
      setExportRecord(await exportCarbonOperationsWorkbook(filters));
    } catch (exportError) {
      setError(getErrorMessage(exportError, "Unable to export Carbon Excel report."));
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.headerRow}>
        <View style={styles.headerText}>
          <Text style={styles.title}>Carbon reports</Text>
          <Text style={styles.copy}>
            Profile, soil, activity, and verification state for Carbon operations.
          </Text>
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

      {error ? <StateCard message={error} tone="error" /> : null}

      <View style={styles.summaryGrid}>
        {summaryCards.map((item) => (
          <View key={item.label} style={styles.summaryCard}>
            <Text style={styles.summaryValue}>{item.value}</Text>
            <Text style={styles.summaryLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      <View style={styles.metricsGrid}>
        <Metric label="Active farmers" value={summary?.activeProfiles ?? 0} />
        <Metric label="Linked logins" value={summary?.linkedFarmerLogins ?? 0} />
        <Metric label="Active plots" value={summary?.activePlots ?? 0} />
        <Metric
          label="Verified soil"
          value={summary?.verifiedSoilProfiles ?? 0}
        />
        <Metric label="Pending soil" value={summary?.pendingSoilProfiles ?? 0} />
        <Metric
          label="Verified activities"
          value={summary?.verifiedActivities ?? 0}
        />
        <Metric label="Pending activities" value={summary?.pendingActivities ?? 0} />
        <Metric label="Evidence count" value={summary?.evidenceCount ?? 0} />
        <Metric
          label="Average SOC"
          value={`${formatNumber(summary?.averageSoilOrganicCarbonPercent ?? 0)}%`}
        />
      </View>

      <View style={styles.actions}>
        <View style={styles.filterGrid}>
          <ReportFilterField
            label="Village"
            onChange={(value) =>
              setFilters((current) => ({ ...current, village: value }))
            }
            value={filters.village ?? ""}
          />
          <ReportFilterField
            label="Crop"
            onChange={(value) =>
              setFilters((current) => ({ ...current, crop: value }))
            }
            value={filters.crop ?? ""}
          />
          <ReportFilterField
            label="Verification"
            onChange={(value) =>
              setFilters((current) => ({ ...current, verificationStatus: value }))
            }
            placeholder="VERIFIED"
            value={filters.verificationStatus ?? ""}
          />
          <ReportFilterField
            label="Activity status"
            onChange={(value) =>
              setFilters((current) => ({ ...current, activityStatus: value }))
            }
            placeholder="PENDING_REVIEW"
            value={filters.activityStatus ?? ""}
          />
          <ReportFilterField
            label="Date from"
            onChange={(value) =>
              setFilters((current) => ({ ...current, dateFrom: value }))
            }
            placeholder="YYYY-MM-DD"
            value={filters.dateFrom ?? ""}
          />
          <ReportFilterField
            label="Date to"
            onChange={(value) =>
              setFilters((current) => ({ ...current, dateTo: value }))
            }
            placeholder="YYYY-MM-DD"
            value={filters.dateTo ?? ""}
          />
        </View>

        <Pressable
          accessibilityRole="button"
          disabled={isExporting}
          style={[styles.primaryButton, isExporting && styles.disabledButton]}
          onPress={handleExport}
        >
          <Text style={styles.primaryButtonText}>
            {isExporting ? "Exporting..." : "Export Carbon Excel"}
          </Text>
        </Pressable>

        {exportRecord ? (
          <Text style={styles.exportText}>
            Export {exportRecord.status.toLowerCase()}: {exportRecord.storageKey}
          </Text>
        ) : null}
      </View>

      <VillageBreakdownList items={summary?.villageBreakdowns ?? []} />
      <ActivityBreakdownList items={summary?.activityBreakdowns ?? []} />
    </View>
  );
}

function ReportFilterField({
  label,
  onChange,
  placeholder,
  value
}: {
  label: string;
  onChange: (value: string) => void;
  placeholder?: string;
  value: string;
}) {
  return (
    <View style={styles.filterField}>
      <Text style={styles.filterLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor="#8a99a1"
        style={styles.filterInput}
        value={value}
      />
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

function VillageBreakdownList({ items }: { items: CarbonReportBreakdown[] }) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Village readiness</Text>
      {items.length ? (
        items.slice(0, 8).map((item) => (
          <View key={item.label} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{item.label}</Text>
              <Text style={styles.rowMeta}>
                {item.profileCount} farmer{item.profileCount === 1 ? "" : "s"} -{" "}
                {item.plotCount} plot{item.plotCount === 1 ? "" : "s"} -{" "}
                {formatNumber(item.areaAcres)} ac
              </Text>
              <Text style={styles.rowMeta}>
                {item.soilProfileCount} soil record
                {item.soilProfileCount === 1 ? "" : "s"} - {item.activityCount}{" "}
                {item.activityCount === 1 ? "activity" : "activities"} -{" "}
                {item.pendingVerificationCount} pending
              </Text>
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>No Carbon village data available.</Text>
      )}
    </View>
  );
}

function ActivityBreakdownList({
  items
}: {
  items: CarbonActivityReportBreakdown[];
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Activity verification</Text>
      {items.length ? (
        items.slice(0, 8).map((item) => (
          <View key={item.categoryCode} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{item.categoryName}</Text>
              <Text style={styles.rowMeta}>
                {item.activityCount}{" "}
                {item.activityCount === 1 ? "activity" : "activities"} -{" "}
                {item.verifiedActivities} verified - {item.pendingActivities} pending
              </Text>
              <Text style={styles.rowMeta}>
                {item.evidenceCount} evidence file
                {item.evidenceCount === 1 ? "" : "s"}
              </Text>
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>No Carbon activity records available.</Text>
      )}
    </View>
  );
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

const styles = StyleSheet.create({
  actions: {
    alignItems: "flex-start",
    gap: 8
  },
  copy: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  disabledButton: {
    opacity: 0.6
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
  },
  filterField: {
    flex: 1,
    minWidth: 150
  },
  filterGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  filterInput: {
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    minHeight: 38,
    paddingHorizontal: 10
  },
  filterLabel: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 4
  },
  headerRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    justifyContent: "space-between"
  },
  headerText: {
    flex: 1
  },
  listBlock: {
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    gap: 8,
    paddingTop: 12
  },
  metricLabel: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 4
  },
  metricRow: {
    borderColor: "#e1e9ed",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 150,
    padding: 12
  },
  metricValue: {
    color: "#172126",
    fontSize: 16,
    fontWeight: "800"
  },
  metricsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  panel: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 160,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  row: {
    backgroundColor: "#f7fafb",
    borderRadius: 8,
    padding: 12
  },
  rowMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  rowText: {
    flex: 1
  },
  rowTitle: {
    color: "#172126",
    fontSize: 14,
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
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  summaryCard: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    flex: 1,
    minWidth: 132,
    padding: 14
  },
  summaryGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  summaryLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  summaryValue: {
    color: "#1f6f73",
    fontSize: 23,
    fontWeight: "800",
    marginBottom: 4
  },
  title: {
    color: "#172126",
    fontSize: 17,
    fontWeight: "800"
  }
});
