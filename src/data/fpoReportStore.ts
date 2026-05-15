import { apiClient } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import { FpoDashboardSummaryResponse } from "../core/api/fpoContracts";
import { ReportExport } from "./reportStore";

export type FpoDashboardSummary = FpoDashboardSummaryResponse;

export type FpoReportFilters = {
  coordinator?: string;
  crop?: string;
  dateFrom?: string;
  dateTo?: string;
  season?: string;
  village?: string;
};

export async function getFpoDashboardSummary() {
  return apiClient.get<FpoDashboardSummary>(endpoints.fpo.reports.summary);
}

export async function exportFpoOperationsWorkbook(filters: FpoReportFilters = {}) {
  return apiClient.post<{ filters?: Record<string, unknown> }, ReportExport>(
    endpoints.fpo.reports.export,
    { filters: cleanFilters(filters) }
  );
}

function cleanFilters(filters: FpoReportFilters) {
  return Object.fromEntries(
    Object.entries(filters)
      .map(([key, value]) => [key, value?.trim()])
      .filter((entry): entry is [string, string] => Boolean(entry[1]))
  );
}
