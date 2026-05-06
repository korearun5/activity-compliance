import { useEffect, useMemo, useState } from "react";
import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View
} from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import { EvidenceStatus, UserProfileInput } from "../core/model/types";
import {
  countParticipantActivities,
  countParticipantProofs,
  createRegisteredParticipant,
  CreateRegisteredParticipantInput,
  getRegisteredParticipants,
  RegisteredParticipant,
  updateRegisteredParticipantStatus
} from "../data/adminRegistryStore";
import { getSavedCropCycles } from "../data/cropCycleStore";
import { CropCycle, ProofSubmission } from "../data/agricultureConfig";
import { getSavedProofs } from "../data/proofStore";
import {
  exportBackendReport,
  getBackendReportSummary,
  ReportExport,
  ReportSummary
} from "../data/reportStore";
import {
  BackendWorkflow,
  BackendWorkflowStatus,
  CreateBackendWorkflowInput,
  getBackendActivities,
  getBackendProofs,
  getBackendWorkflowDefinitions,
  hasBackendSession,
  reviewBackendProof,
  startBackendActivity,
  StartBackendActivityInput,
  updateBackendWorkflowStatus,
  createBackendWorkflowDefinition
} from "../data/workflowActivityStore";
import { AdminNotificationsTab } from "./AdminNotificationsTab";
import { AdminRolesTab } from "./AdminRolesTab";
import { AdminWorkflowsTab } from "./AdminWorkflowsTab";
import { StatusBadge } from "../ui/StatusBadge";

type AdminHomeScreenProps = {
  onLogout: () => void;
};

type AdminTab =
  | "notifications"
  | "overview"
  | "participants"
  | "reports"
  | "roles"
  | "workflows";

export function AdminHomeScreen({ onLogout }: AdminHomeScreenProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>("overview");
  const [registeredParticipants, setRegisteredParticipants] = useState<
    RegisteredParticipant[]
  >([]);
  const [savedCycles, setSavedCycles] = useState<CropCycle[]>([]);
  const [savedProofs, setSavedProofs] = useState<ProofSubmission[]>([]);
  const [workflowDefinitions, setWorkflowDefinitions] = useState<BackendWorkflow[]>(
    []
  );
  const [canReviewEvidence, setCanReviewEvidence] = useState(false);
  const [reportSummary, setReportSummary] = useState<ReportSummary | null>(null);
  const [adminDataError, setAdminDataError] = useState("");
  const [participantFormError, setParticipantFormError] = useState("");
  const [reportExport, setReportExport] = useState<ReportExport | null>(null);
  const [reportExportError, setReportExportError] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [workflowFormError, setWorkflowFormError] = useState("");
  const [activityStartError, setActivityStartError] = useState("");
  const [isCreatingParticipant, setIsCreatingParticipant] = useState(false);
  const [isCreatingWorkflow, setIsCreatingWorkflow] = useState(false);
  const [isStartingActivity, setIsStartingActivity] = useState(false);
  const [exportingReportFormat, setExportingReportFormat] = useState<
    ReportExport["format"] | null
  >(null);
  const [reviewingProofId, setReviewingProofId] = useState<string | null>(null);
  const [updatingWorkflowId, setUpdatingWorkflowId] = useState<string | null>(null);
  const [updatingParticipantUsername, setUpdatingParticipantUsername] = useState<
    string | null
  >(null);

  useEffect(() => {
    async function loadAdminData() {
      let participantLoadError = "";

      try {
        const participants = await getRegisteredParticipants();
        setRegisteredParticipants(participants);
      } catch (error) {
        participantLoadError = getErrorMessage(
          error,
          "Unable to load participant profiles."
        );
        setAdminDataError(participantLoadError);
      }

      try {
        const hasToken = await hasBackendSession();
        const [cycles, proofs, summary, workflows] = hasToken
          ? await loadBackendAdminData()
          : await loadLocalAdminData();

        setSavedCycles(cycles);
        setSavedProofs(proofs);
        setWorkflowDefinitions(workflows);
        setCanReviewEvidence(hasToken);
        setReportSummary(summary);
        setAdminDataError(participantLoadError);
        setReviewError("");
        setWorkflowFormError("");
        setActivityStartError("");
      } catch (error) {
        const [cycles, proofs] = await Promise.all([
          getSavedCropCycles(),
          getSavedProofs()
        ]);
        setSavedCycles(cycles);
        setSavedProofs(proofs);
        setWorkflowDefinitions([]);
        setCanReviewEvidence(false);
        setReportSummary(null);
        const activityLoadError = getErrorMessage(
          error,
          "Unable to load activity and report data."
        );
        setAdminDataError(
          participantLoadError
            ? `${participantLoadError} ${activityLoadError}`
            : activityLoadError
        );
      }
    }

    loadAdminData();
  }, []);

  async function handleCreateParticipant({
    password,
    profile,
    username
  }: CreateRegisteredParticipantInput) {
    setIsCreatingParticipant(true);
    setParticipantFormError("");

    try {
      const participant = await createRegisteredParticipant({
        password,
        profile,
        username
      });
      upsertParticipant(participant);
      return true;
    } catch (error) {
      setParticipantFormError(
        getErrorMessage(error, "Unable to create participant profile.")
      );
      return false;
    } finally {
      setIsCreatingParticipant(false);
    }
  }

  async function handleToggleParticipantStatus(participant: RegisteredParticipant) {
    const nextStatus = participant.status === "Active" ? "Inactive" : "Active";
    setUpdatingParticipantUsername(participant.username);
    setParticipantFormError("");

    try {
      const updatedParticipant = await updateRegisteredParticipantStatus(
        participant,
        nextStatus
      );
      upsertParticipant(updatedParticipant);
    } catch (error) {
      setParticipantFormError(
        getErrorMessage(error, "Unable to update participant status.")
      );
    } finally {
      setUpdatingParticipantUsername(null);
    }
  }

  async function handleReviewProof(
    proof: ProofSubmission,
    status: "APPROVED" | "REJECTED"
  ) {
    setReviewingProofId(proof.id);
    setReviewError("");

    try {
      const reviewedProof = await reviewBackendProof({
        evidenceId: proof.id,
        status
      });

      setSavedProofs((currentProofs) =>
        currentProofs.map((item) =>
          item.id === proof.id
            ? {
                ...item,
                ...reviewedProof,
                crop: item.crop,
                locationName: item.locationName,
                region: item.region,
                workflowName: item.workflowName
              }
            : item
        )
      );
    } catch (error) {
      setReviewError(getErrorMessage(error, "Unable to review proof record."));
    } finally {
      setReviewingProofId(null);
    }
  }

  async function handleExportReport(format: ReportExport["format"]) {
    setExportingReportFormat(format);
    setReportExportError("");

    try {
      setReportExport(await exportBackendReport(format));
    } catch (error) {
      const label = format === "PDF" ? "PDF" : "Excel";
      setReportExportError(getErrorMessage(error, `Unable to export ${label} report.`));
    } finally {
      setExportingReportFormat(null);
    }
  }

  async function handleCreateWorkflow(input: CreateBackendWorkflowInput) {
    setIsCreatingWorkflow(true);
    setWorkflowFormError("");

    try {
      const workflow = await createBackendWorkflowDefinition(input);
      setWorkflowDefinitions((currentWorkflows) => [
        workflow,
        ...currentWorkflows.filter((item) => item.id !== workflow.id)
      ]);
      return true;
    } catch (error) {
      setWorkflowFormError(
        getErrorMessage(error, "Unable to create workflow definition.")
      );
      return false;
    } finally {
      setIsCreatingWorkflow(false);
    }
  }

  async function handleUpdateWorkflowStatus(
    workflowId: string,
    status: BackendWorkflowStatus
  ) {
    setUpdatingWorkflowId(workflowId);
    setWorkflowFormError("");

    try {
      const workflow = await updateBackendWorkflowStatus(workflowId, status);
      setWorkflowDefinitions((currentWorkflows) =>
        currentWorkflows.map((item) => (item.id === workflow.id ? workflow : item))
      );
    } catch (error) {
      setWorkflowFormError(
        getErrorMessage(error, "Unable to update workflow status.")
      );
    } finally {
      setUpdatingWorkflowId(null);
    }
  }

  async function handleStartActivity(input: StartBackendActivityInput) {
    setIsStartingActivity(true);
    setActivityStartError("");

    try {
      const cycle = await startBackendActivity(input);
      setSavedCycles((currentCycles) => [
        cycle,
        ...currentCycles.filter((item) => item.id !== cycle.id)
      ]);

      try {
        setReportSummary(await getBackendReportSummary());
      } catch {
        // The activity is created even if the summary refresh fails.
      }

      return true;
    } catch (error) {
      setActivityStartError(getErrorMessage(error, "Unable to start activity."));
      return false;
    } finally {
      setIsStartingActivity(false);
    }
  }

  function upsertParticipant(participant: RegisteredParticipant) {
    setRegisteredParticipants((currentParticipants) => [
      participant,
      ...currentParticipants.filter(
        (item) => item.username.toLowerCase() !== participant.username.toLowerCase()
      )
    ]);
  }

  const participants = registeredParticipants;
  const allCycles = savedCycles;
  const runningCycles = allCycles.filter((cycle) => cycle.status === "running");
  const completedCycles = allCycles.filter((cycle) => cycle.status === "completed");
  const proofRecords = savedProofs;
  const regions = [...new Set(participants.map((participant) => participant.region))];
  const crops = [...new Set(allCycles.map((cycle) => cycle.crop))];
  const complianceScore = calculateComplianceScore(allCycles, proofRecords);

  const summary = useMemo(
    () => [
      { label: "Participants", value: String(participants.length) },
      { label: "Running cycles", value: String(runningCycles.length) },
      { label: "Completed", value: String(completedCycles.length) },
      { label: "Proof records", value: String(proofRecords.length) }
    ],
    [
      completedCycles.length,
      participants.length,
      proofRecords.length,
      runningCycles.length
    ]
  );

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.shell}>
          <View style={styles.headerRow}>
            <View style={styles.headerText}>
              <Text style={styles.eyebrow}>Admin dashboard</Text>
              <Text style={styles.title}>Process Verification</Text>
              <Text style={styles.copy}>
                Track participants, activity progress, proof records, and report-ready
                compliance evidence.
              </Text>
            </View>
            <Pressable
              accessibilityRole="button"
              hitSlop={8}
              style={styles.logoutButton}
              onPress={onLogout}
            >
              <Text style={styles.logoutButtonText}>Log out</Text>
            </Pressable>
          </View>

          <View style={styles.statsGrid}>
            {summary.map((item) => (
              <View key={item.label} style={styles.statCard}>
                <Text style={styles.statValue}>{item.value}</Text>
                <Text style={styles.statLabel}>{item.label}</Text>
              </View>
            ))}
          </View>

          <View style={styles.tabRow}>
            {[
              ["overview", "Overview"],
              ["workflows", "Workflows"],
              ["participants", "Participants"],
              ["roles", "Roles"],
              ["notifications", "Notifications"],
              ["reports", "Reports"]
            ].map(([tab, label]) => (
              <Pressable
                key={tab}
                style={[styles.tabButton, activeTab === tab && styles.tabButtonActive]}
                onPress={() => setActiveTab(tab as AdminTab)}
              >
                <Text
                  style={[
                    styles.tabButtonText,
                    activeTab === tab && styles.tabButtonTextActive
                  ]}
                >
                  {label}
                </Text>
              </Pressable>
            ))}
          </View>

          {activeTab === "overview" ? (
            <OverviewTab
              adminDataError={adminDataError}
              canReviewEvidence={canReviewEvidence}
              onReviewProof={handleReviewProof}
              participants={participants}
              proofRecords={proofRecords}
              reviewError={reviewError}
              reviewingProofId={reviewingProofId}
              runningCycles={runningCycles}
            />
          ) : null}

          {activeTab === "participants" ? (
            <ParticipantsTab
              allCycles={allCycles}
              createError={participantFormError}
              isCreatingParticipant={isCreatingParticipant}
              onCreateParticipant={handleCreateParticipant}
              onToggleParticipantStatus={handleToggleParticipantStatus}
              participants={participants}
              proofRecords={proofRecords}
              updatingParticipantUsername={updatingParticipantUsername}
            />
          ) : null}

          {activeTab === "workflows" ? (
            <AdminWorkflowsTab
              activityStartError={activityStartError}
              canUseBackend={canReviewEvidence}
              createWorkflowError={workflowFormError}
              isCreatingWorkflow={isCreatingWorkflow}
              onCreateWorkflow={handleCreateWorkflow}
              onStartActivity={handleStartActivity}
              onUpdateWorkflowStatus={handleUpdateWorkflowStatus}
              participants={participants}
              startingActivity={isStartingActivity}
              updatingWorkflowId={updatingWorkflowId}
              workflows={workflowDefinitions}
            />
          ) : null}

          {activeTab === "roles" ? (
            <AdminRolesTab canUseBackend={canReviewEvidence} />
          ) : null}

          {activeTab === "notifications" ? (
            <AdminNotificationsTab
              canUseBackend={canReviewEvidence}
              participants={participants}
            />
          ) : null}

          {activeTab === "reports" ? (
            <ReportsTab
              complianceScore={complianceScore}
              crops={crops}
              exportingReportFormat={exportingReportFormat}
              onExportReport={handleExportReport}
              proofRecords={proofRecords}
              reportExport={reportExport}
              reportExportError={reportExportError}
              reportSummary={reportSummary}
              regions={regions}
            />
          ) : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

async function loadBackendAdminData(): Promise<
  [CropCycle[], ProofSubmission[], ReportSummary, BackendWorkflow[]]
> {
  const [cycles, summary, workflows] = await Promise.all([
    getBackendActivities(),
    getBackendReportSummary(),
    getBackendWorkflowDefinitions()
  ]);
  const proofs = await getBackendProofs(cycles);
  return [cycles, proofs, summary, workflows];
}

async function loadLocalAdminData(): Promise<
  [CropCycle[], ProofSubmission[], ReportSummary | null, BackendWorkflow[]]
> {
  const [cycles, proofs] = await Promise.all([
    getSavedCropCycles(),
    getSavedProofs()
  ]);
  return [cycles, proofs, null, []];
}

function OverviewTab({
  adminDataError,
  canReviewEvidence,
  onReviewProof,
  participants,
  proofRecords,
  reviewError,
  reviewingProofId,
  runningCycles
}: {
  adminDataError: string;
  canReviewEvidence: boolean;
  onReviewProof: (
    proof: ProofSubmission,
    status: "APPROVED" | "REJECTED"
  ) => Promise<void>;
  participants: RegisteredParticipant[];
  proofRecords: ProofSubmission[];
  reviewError: string;
  reviewingProofId: string | null;
  runningCycles: CropCycle[];
}) {
  return (
    <View style={styles.section}>
      {adminDataError ? (
        <View style={styles.warningCard}>
          <Text style={styles.warningText}>{adminDataError}</Text>
        </View>
      ) : null}

      <Text style={styles.sectionTitle}>Current field activity</Text>
      {runningCycles.length ? (
        runningCycles.map((cycle) => (
          <View key={cycle.id} style={styles.reviewCard}>
            <View style={styles.reviewText}>
              <Text style={styles.cardTitle}>{cycle.crop}</Text>
              <Text style={styles.cardDescription}>
                {cycle.region} - {cycle.plot} - harvest by {cycle.expectedHarvest}
              </Text>
              <Text style={styles.cardMeta}>{cycle.progress}% process progress</Text>
            </View>
            <StatusBadge label="Running" />
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No active activities have been started yet.
          </Text>
        </View>
      )}

      <Text style={styles.sectionTitle}>Recent proof records</Text>
      {reviewError ? <Text style={styles.formError}>{reviewError}</Text> : null}
      {proofRecords.length ? (
        proofRecords.map((proof) => (
          <View key={proof.id} style={styles.reviewCard}>
            <View style={styles.reviewText}>
              <Text style={styles.cardTitle}>{proof.action}</Text>
              <Text style={styles.cardDescription}>
                {proof.participantName ?? proof.farmer} - {proof.crop} - {proof.region}
              </Text>
              <Text style={styles.cardMeta}>Submitted {proof.submittedOn}</Text>
              {proof.note ? (
                <Text style={styles.cardDescription}>{proof.note}</Text>
              ) : null}
            </View>
            <View style={styles.reviewActions}>
              <StatusBadge
                label={evidenceStatusLabel(proof.status)}
                tone={evidenceStatusTone(proof.status)}
              />
              {canReviewEvidence &&
              (proof.status === "pending" || proof.status === "done") ? (
                <View style={styles.reviewButtonRow}>
                  <Pressable
                    accessibilityRole="button"
                    disabled={reviewingProofId === proof.id}
                    style={({ pressed }) => [
                      styles.approveButton,
                      (pressed || reviewingProofId === proof.id) &&
                        styles.actionButtonPressed
                    ]}
                    onPress={() => onReviewProof(proof, "APPROVED")}
                  >
                    <Text style={styles.approveButtonText}>
                      {reviewingProofId === proof.id ? "Saving..." : "Approve"}
                    </Text>
                  </Pressable>
                  <Pressable
                    accessibilityRole="button"
                    disabled={reviewingProofId === proof.id}
                    style={({ pressed }) => [
                      styles.rejectButton,
                      (pressed || reviewingProofId === proof.id) &&
                        styles.actionButtonPressed
                    ]}
                    onPress={() => onReviewProof(proof, "REJECTED")}
                  >
                    <Text style={styles.rejectButtonText}>Reject</Text>
                  </Pressable>
                </View>
              ) : null}
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No proof records have been submitted yet.
          </Text>
        </View>
      )}

      <Text style={styles.sectionTitle}>Participants by region</Text>
      {participants.length ? (
        participants.map((participant) => (
          <View key={participant.username} style={styles.compactRow}>
            <Text style={styles.compactTitle}>{participant.name}</Text>
            <Text style={styles.compactMeta}>
              {participant.region} - {participant.village}
            </Text>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No participants are registered yet.
          </Text>
        </View>
      )}
    </View>
  );
}

function ParticipantsTab({
  allCycles,
  createError,
  isCreatingParticipant,
  onCreateParticipant,
  onToggleParticipantStatus,
  participants,
  proofRecords,
  updatingParticipantUsername
}: {
  allCycles: CropCycle[];
  createError: string;
  isCreatingParticipant: boolean;
  onCreateParticipant: (data: CreateRegisteredParticipantInput) => Promise<boolean>;
  onToggleParticipantStatus: (participant: RegisteredParticipant) => Promise<void>;
  participants: RegisteredParticipant[];
  proofRecords: ProofSubmission[];
  updatingParticipantUsername: string | null;
}) {
  return (
    <View style={styles.section}>
      <CreateParticipantForm
        error={createError}
        isSubmitting={isCreatingParticipant}
        onSubmit={onCreateParticipant}
      />

      {participants.length ? (
        participants.map((participant) => (
          <View key={participant.username} style={styles.participantCard}>
            <View style={styles.reviewText}>
              <Text style={styles.cardTitle}>{participant.name}</Text>
              <Text style={styles.cardDescription}>
                {participant.region} - {participant.village} - {participant.phone}
              </Text>
              <Text style={styles.cardMeta}>
                {countParticipantActivities(participant, allCycles, "running")} running,{" "}
                {countParticipantActivities(participant, allCycles, "completed")}{" "}
                completed, {countParticipantProofs(participant, proofRecords)} proof
                record(s)
              </Text>
            </View>
            <View style={styles.participantActions}>
              <StatusBadge
                label={participant.status}
                tone={participant.status === "Active" ? "good" : "warning"}
              />
              {participant.status !== "Profile pending" ? (
                <Pressable
                  accessibilityRole="button"
                  disabled={updatingParticipantUsername === participant.username}
                  style={({ pressed }) => [
                    styles.actionButton,
                    (pressed || updatingParticipantUsername === participant.username) &&
                      styles.actionButtonPressed
                  ]}
                  onPress={() => onToggleParticipantStatus(participant)}
                >
                  <Text style={styles.actionButtonText}>
                    {updatingParticipantUsername === participant.username
                      ? "Saving..."
                      : participant.status === "Active"
                        ? "Deactivate"
                        : "Activate"}
                  </Text>
                </Pressable>
              ) : null}
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No participant profiles are available yet.
          </Text>
        </View>
      )}
    </View>
  );
}

function CreateParticipantForm({
  error,
  isSubmitting,
  onSubmit
}: {
  error: string;
  isSubmitting: boolean;
  onSubmit: (data: CreateRegisteredParticipantInput) => Promise<boolean>;
}) {
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [region, setRegion] = useState("");
  const [username, setUsername] = useState("");
  const [village, setVillage] = useState("");
  const [localError, setLocalError] = useState("");

  async function handleSubmit() {
    const profile: UserProfileInput = {
      displayName: name.trim(),
      locationName: region.trim(),
      phone: phone.trim(),
      siteName: village.trim()
    };

    if (
      !username.trim() ||
      !password ||
      !profile.displayName ||
      !profile.phone ||
      !profile.locationName ||
      !profile.siteName
    ) {
      setLocalError("Enter all participant profile fields.");
      return;
    }

    if (password.length < 8) {
      setLocalError("Password must be at least 8 characters.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      password,
      profile,
      username: username.trim()
    });

    if (created) {
      setName("");
      setPassword("");
      setPhone("");
      setRegion("");
      setUsername("");
      setVillage("");
    }
  }

  return (
    <View style={styles.managementCard}>
      <Text style={styles.cardTitle}>Create participant</Text>
      <View style={styles.formGrid}>
        <AdminField label="Full name" value={name} onChange={setName} />
        <AdminField
          label="Phone"
          value={phone}
          onChange={setPhone}
          keyboardType="phone-pad"
        />
        <AdminField label="Region" value={region} onChange={setRegion} />
        <AdminField label="Village" value={village} onChange={setVillage} />
        <AdminField label="Username" value={username} onChange={setUsername} />
        <AdminField
          label="Password"
          value={password}
          onChange={setPassword}
          secureTextEntry
        />
      </View>

      {localError || error ? (
        <Text style={styles.formError}>{localError || error}</Text>
      ) : null}

      <View style={styles.formActions}>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={({ pressed }) => [
            styles.createButton,
            (pressed || isSubmitting) && styles.createButtonPressed
          ]}
          onPress={handleSubmit}
        >
          <Text style={styles.createButtonText}>
            {isSubmitting ? "Creating..." : "Create profile"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

function AdminField({
  keyboardType,
  label,
  onChange,
  secureTextEntry,
  value
}: {
  keyboardType?: "default" | "phone-pad";
  label: string;
  onChange: (value: string) => void;
  secureTextEntry?: boolean;
  value: string;
}) {
  return (
    <View style={styles.formField}>
      <Text style={styles.formLabel}>{label}</Text>
      <TextInput
        autoCapitalize={label === "Full name" ? "words" : "none"}
        autoCorrect={false}
        keyboardType={keyboardType}
        onChangeText={onChange}
        secureTextEntry={secureTextEntry}
        style={styles.formInput}
        value={value}
      />
    </View>
  );
}

function ReportsTab({
  complianceScore,
  crops,
  exportingReportFormat,
  onExportReport,
  proofRecords,
  reportExport,
  reportExportError,
  reportSummary,
  regions
}: {
  complianceScore: number;
  crops: string[];
  exportingReportFormat: ReportExport["format"] | null;
  onExportReport: (format: ReportExport["format"]) => Promise<void>;
  proofRecords: ProofSubmission[];
  reportExport: ReportExport | null;
  reportExportError: string;
  reportSummary: ReportSummary | null;
  regions: string[];
}) {
  const displayedComplianceScore =
    reportSummary?.approvedEvidencePercent ?? complianceScore;
  const displayedProofCount = reportSummary?.evidenceRecords ?? proofRecords.length;
  const displayedRegionCount = reportSummary?.byLocation.length ?? regions.length;
  const isExportingReport = exportingReportFormat !== null;

  return (
    <View style={styles.section}>
      <View style={styles.reportCard}>
        <Text style={styles.cardTitle}>Government evidence report</Text>
        <Text style={styles.cardDescription}>
          This report will show participant coverage, activity progress, proof photo
          records, and process completion by region and crop.
        </Text>
        <View style={styles.reportGrid}>
          <View style={styles.reportMetric}>
            <Text style={styles.reportValue}>{displayedComplianceScore}%</Text>
            <Text style={styles.reportLabel}>Approved evidence</Text>
          </View>
          <View style={styles.reportMetric}>
            <Text style={styles.reportValue}>{displayedRegionCount}</Text>
            <Text style={styles.reportLabel}>Regions covered</Text>
          </View>
          <View style={styles.reportMetric}>
            <Text style={styles.reportValue}>{displayedProofCount}</Text>
            <Text style={styles.reportLabel}>Proof records</Text>
          </View>
          {reportSummary ? (
            <View style={styles.reportMetric}>
              <Text style={styles.reportValue}>
                {reportSummary.taskCompletionPercent}%
              </Text>
              <Text style={styles.reportLabel}>Task completion</Text>
            </View>
          ) : null}
        </View>
        <View style={styles.reportActions}>
          <View style={styles.reportButtonRow}>
            <Pressable
              accessibilityRole="button"
              disabled={!reportSummary || isExportingReport}
              style={({ pressed }) => [
                styles.createButton,
                (!reportSummary || pressed || isExportingReport) &&
                  styles.createButtonPressed
              ]}
              onPress={() => onExportReport("PDF")}
            >
              <Text style={styles.createButtonText}>
                {exportingReportFormat === "PDF" ? "Exporting PDF..." : "Export PDF"}
              </Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              disabled={!reportSummary || isExportingReport}
              style={({ pressed }) => [
                styles.actionButton,
                (!reportSummary || pressed || isExportingReport) &&
                  styles.actionButtonPressed
              ]}
              onPress={() => onExportReport("XLSX")}
            >
              <Text style={styles.actionButtonText}>
                {exportingReportFormat === "XLSX"
                  ? "Exporting Excel..."
                  : "Export Excel"}
              </Text>
            </Pressable>
          </View>
          {reportExport ? (
            <Text style={styles.cardMeta}>
              Export {reportExport.status.toLowerCase()}: {reportExport.storageKey}
            </Text>
          ) : null}
          {reportExportError ? (
            <Text style={styles.formError}>{reportExportError}</Text>
          ) : null}
        </View>
      </View>

      <Text style={styles.sectionTitle}>Report filters</Text>
      {[
        `Region: ${regions.length ? regions.join(", ") : "No region yet"}`,
        `Crop type: ${crops.length ? crops.join(", ") : "No crop yet"}`,
        "Cycle status: running, completed, delayed",
        "Proof status: done, editable window closed",
        "Date range: sowing to harvest or custom period"
      ].map((item) => (
        <View key={item} style={styles.filterRow}>
          <Text style={styles.filterText}>{item}</Text>
        </View>
      ))}

      {reportSummary ? (
        <>
          <Text style={styles.sectionTitle}>Workflow summary</Text>
          {reportSummary.byWorkflow.length ? (
            reportSummary.byWorkflow.map((item) => (
              <View key={item.label} style={styles.filterRow}>
                <Text style={styles.filterText}>{item.label}</Text>
                <Text style={styles.cardMeta}>
                  {item.activities} activities, {item.completedActivities} completed,{" "}
                  {item.approvedEvidence}/{item.evidenceRecords} approved proof records
                </Text>
              </View>
            ))
          ) : (
            <View style={styles.emptyCard}>
              <Text style={styles.cardDescription}>
                No workflow activity is available for reporting yet.
              </Text>
            </View>
          )}
        </>
      ) : null}
    </View>
  );
}

function calculateComplianceScore(cycles: CropCycle[], proofs: ProofSubmission[]) {
  const totalSteps = cycles.reduce((sum, cycle) => sum + cycle.steps.length, 0);

  if (!totalSteps) {
    return 0;
  }

  const acceptedProofCount = proofs.filter(
    (proof) => proof.status === "approved" || proof.status === "done"
  ).length;

  return Math.round((acceptedProofCount / totalSteps) * 100);
}

function evidenceStatusLabel(status: EvidenceStatus) {
  switch (status) {
    case "approved":
      return "Approved";
    case "rejected":
      return "Rejected";
    case "pending":
      return "Pending review";
    default:
      return "Submitted";
  }
}

function evidenceStatusTone(status: EvidenceStatus) {
  switch (status) {
    case "approved":
      return "good";
    case "rejected":
      return "danger";
    case "pending":
      return "warning";
    default:
      return "neutral";
  }
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#f4f7fb"
  },
  scrollContent: {
    flexGrow: 1
  },
  shell: {
    width: "100%",
    maxWidth: 980,
    alignSelf: "center",
    paddingHorizontal: 20,
    paddingVertical: 28
  },
  headerRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 14,
    justifyContent: "space-between",
    marginBottom: 22
  },
  headerText: {
    flex: 1
  },
  eyebrow: {
    color: "#356b6f",
    fontSize: 13,
    fontWeight: "800",
    marginBottom: 8,
    textTransform: "uppercase"
  },
  title: {
    color: "#172126",
    fontSize: 34,
    fontWeight: "800",
    marginBottom: 10
  },
  copy: {
    color: "#53666f",
    fontSize: 16,
    lineHeight: 23,
    maxWidth: 680
  },
  logoutButton: {
    zIndex: 2,
    minHeight: 42,
    minWidth: 82,
    alignItems: "center",
    justifyContent: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 14
  },
  logoutButtonText: {
    color: "#1f6f73",
    fontSize: 14,
    fontWeight: "800"
  },
  statsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    marginBottom: 20
  },
  statCard: {
    minWidth: 150,
    flex: 1,
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  },
  statValue: {
    color: "#172126",
    fontSize: 28,
    fontWeight: "800",
    marginBottom: 5
  },
  statLabel: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "700"
  },
  tabRow: {
    backgroundColor: "#e8eef2",
    borderRadius: 8,
    flexWrap: "wrap",
    flexDirection: "row",
    gap: 4,
    marginBottom: 20,
    padding: 4
  },
  tabButton: {
    alignItems: "center",
    borderRadius: 6,
    flexBasis: 130,
    flexGrow: 1,
    minHeight: 42,
    justifyContent: "center"
  },
  tabButtonActive: {
    backgroundColor: "#ffffff"
  },
  tabButtonText: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "800"
  },
  tabButtonTextActive: {
    color: "#172126"
  },
  section: {
    gap: 14
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800",
    marginTop: 4
  },
  reviewCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  },
  warningCard: {
    backgroundColor: "#fff8e8",
    borderColor: "#f0d38a",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  warningText: {
    color: "#8a5a00",
    fontSize: 13,
    fontWeight: "700"
  },
  reviewText: {
    flex: 1
  },
  reviewActions: {
    alignItems: "flex-end",
    gap: 10
  },
  reviewButtonRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end"
  },
  cardTitle: {
    color: "#172126",
    fontSize: 17,
    fontWeight: "800",
    marginBottom: 5
  },
  cardDescription: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  cardMeta: {
    color: "#6d7f88",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 7
  },
  compactRow: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  compactTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800",
    marginBottom: 3
  },
  compactMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  },
  startCropCard: {
    alignItems: "center",
    backgroundColor: "#ffffff",
    borderColor: "#cfe0df",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 14,
    padding: 16
  },
  managementCard: {
    backgroundColor: "#ffffff",
    borderColor: "#cfe0df",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formField: {
    flex: 1,
    gap: 7,
    minWidth: 210
  },
  formLabel: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  formInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 48,
    paddingHorizontal: 12
  },
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  formActions: {
    alignItems: "flex-start"
  },
  createButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 46,
    minWidth: 140,
    paddingHorizontal: 16
  },
  createButtonPressed: {
    opacity: 0.72
  },
  createButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  participantCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  participantActions: {
    alignItems: "flex-end",
    gap: 10
  },
  actionButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 100,
    paddingHorizontal: 12
  },
  actionButtonPressed: {
    opacity: 0.64
  },
  actionButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  approveButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 92,
    paddingHorizontal: 12
  },
  approveButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  rejectButton: {
    alignItems: "center",
    borderColor: "#b42318",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 82,
    paddingHorizontal: 12
  },
  rejectButtonText: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "800"
  },
  reportCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  reportGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  reportActions: {
    alignItems: "flex-start",
    gap: 8
  },
  reportButtonRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  reportMetric: {
    minWidth: 150,
    flex: 1,
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    padding: 14
  },
  reportValue: {
    color: "#1f6f73",
    fontSize: 26,
    fontWeight: "800",
    marginBottom: 4
  },
  reportLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  filterRow: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  filterText: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "700"
  }
});
