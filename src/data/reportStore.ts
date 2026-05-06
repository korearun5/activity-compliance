import { apiClient } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";

export type ReportBreakdown = {
  activities: number;
  approvedEvidence: number;
  completedActivities: number;
  evidenceRecords: number;
  label: string;
  taskCompletionPercent: number;
};

export type ReportSummary = {
  approvedEvidence: number;
  approvedEvidencePercent: number;
  byLocation: ReportBreakdown[];
  byWorkflow: ReportBreakdown[];
  cancelledActivities: number;
  completedActivities: number;
  completedTasks: number;
  evidenceRecords: number;
  participantCount: number;
  pendingReviewEvidence: number;
  rejectedEvidence: number;
  runningActivities: number;
  submittedEvidence: number;
  taskCompletionPercent: number;
  tenantId: string;
  totalActivities: number;
  totalTasks: number;
};

export type ReportExport = {
  completedAt: string | null;
  format: "PDF" | "XLSX";
  id: string;
  reportType: string;
  requestedAt: string;
  status: "COMPLETED" | "FAILED" | "QUEUED" | "RUNNING";
  storageKey: string | null;
  tenantId: string;
};

export async function getBackendReportSummary() {
  return apiClient.get<ReportSummary>(endpoints.reports.summary);
}

export async function exportBackendReport(format: ReportExport["format"]) {
  return apiClient.post<
    { format: ReportExport["format"]; reportType: string },
    ReportExport
  >(endpoints.reports.export, {
    format,
    reportType: "GOVERNMENT_EVIDENCE"
  });
}

export async function exportBackendReportPdf() {
  return exportBackendReport("PDF");
}

export async function exportBackendReportExcel() {
  return exportBackendReport("XLSX");
}
