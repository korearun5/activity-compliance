import { apiClient } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import { FpoDashboardSummaryResponse } from "../core/api/fpoContracts";
import { ReportExport } from "./reportStore";

export type FpoDashboardSummary = FpoDashboardSummaryResponse;

export async function getFpoDashboardSummary() {
  return apiClient.get<FpoDashboardSummary>(endpoints.fpo.reports.summary);
}

export async function exportFpoOperationsWorkbook() {
  return apiClient.post<{ filters?: Record<string, unknown> }, ReportExport>(
    endpoints.fpo.reports.export,
    { filters: {} }
  );
}
