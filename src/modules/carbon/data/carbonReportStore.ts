import { apiClient } from "../../../core/api/client";
import { endpoints } from "../../../core/api/endpoints";
import { ReportExport } from "../../../data/reportStore";

export type CarbonReportBreakdown = {
  activityCount: number;
  areaAcres: number;
  label: string;
  pendingVerificationCount: number;
  plotCount: number;
  profileCount: number;
  soilProfileCount: number;
};

export type CarbonActivityReportBreakdown = {
  activityCount: number;
  categoryCode: string;
  categoryName: string;
  evidenceCount: number;
  pendingActivities: number;
  verifiedActivities: number;
};

export type CarbonReportSummary = {
  activePlots: number;
  activeProfiles: number;
  activityBreakdowns: CarbonActivityReportBreakdown[];
  activityCount: number;
  averageSoilOrganicCarbonPercent: number;
  evidenceCount: number;
  linkedFarmerLogins: number;
  pendingActivities: number;
  pendingSoilProfiles: number;
  soilProfileCount: number;
  tenantId: string;
  totalLandHoldingAcres: number;
  totalPlotAreaAcres: number;
  totalPlots: number;
  totalProfiles: number;
  verifiedActivities: number;
  verifiedSoilProfiles: number;
  villageBreakdowns: CarbonReportBreakdown[];
};

export type CarbonReportFilters = {
  activityStatus?: string;
  crop?: string;
  dateFrom?: string;
  dateTo?: string;
  verificationStatus?: string;
  village?: string;
};

export async function getCarbonReportSummary() {
  return apiClient.get<CarbonReportSummary>(endpoints.carbon.reports.summary);
}

export async function exportCarbonOperationsWorkbook(
  filters: CarbonReportFilters = {}
) {
  return apiClient.post<{ filters?: Record<string, unknown> }, ReportExport>(
    endpoints.carbon.reports.export,
    { filters: cleanFilters(filters) }
  );
}

function cleanFilters(filters: CarbonReportFilters) {
  return Object.fromEntries(
    Object.entries(filters)
      .map(([key, value]) => [key, value?.trim()])
      .filter((entry): entry is [string, string] => Boolean(entry[1]))
  );
}
