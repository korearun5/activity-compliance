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
import { EvidenceStatus } from "../core/model/types";
import {
  countMemberActivities,
  countMemberProofs,
  createFpoMember,
  CreateFpoMemberInput,
  FpoMember,
  getFpoMembers,
  updateFpoMember,
  UpdateFpoMemberInput,
  updateFpoMemberStatus
} from "../data/fpoMemberStore";
import { getSavedCropCycles } from "../data/cropCycleStore";
import { CropCycle, ProofSubmission } from "../data/agricultureConfig";
import { getSavedProofs } from "../data/proofStore";
import { loadEnabledModules, PlatformModuleCode } from "../data/moduleStore";
import {
  canRolePerform,
  getVisibleAdminTabs,
  type AdminTabId,
  type StaffRole
} from "../auth/roleAccess";
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
import { AdminAdvisoriesTab } from "./AdminAdvisoriesTab";
import { AdminCarbonOverviewTab } from "./AdminCarbonOverviewTab";
import { AdminCropPlanningTab } from "./AdminCropPlanningTab";
import { AdminFarmAssetsPanel } from "./AdminFarmAssetsPanel";
import { AdminFpoReportsPanel } from "./AdminFpoReportsPanel";
import { AdminInputDemandTab } from "./AdminInputDemandTab";
import { AdminRolesTab } from "./AdminRolesTab";
import { AdminWorkflowsTab } from "./AdminWorkflowsTab";
import { StatusBadge } from "../ui/StatusBadge";

type AdminHomeScreenProps = {
  currentRole: StaffRole;
  onLogout: () => void;
};

export function AdminHomeScreen({ currentRole, onLogout }: AdminHomeScreenProps) {
  const [activeTab, setActiveTab] = useState<AdminTabId>("overview");
  const [members, setMembers] = useState<FpoMember[]>([]);
  const [savedCycles, setSavedCycles] = useState<CropCycle[]>([]);
  const [savedProofs, setSavedProofs] = useState<ProofSubmission[]>([]);
  const [enabledModules, setEnabledModules] = useState<PlatformModuleCode[] | null>(
    null
  );
  const [workflowDefinitions, setWorkflowDefinitions] = useState<BackendWorkflow[]>([]);
  const [canReviewEvidence, setCanReviewEvidence] = useState(false);
  const [hasBackendAccess, setHasBackendAccess] = useState(false);
  const [reportSummary, setReportSummary] = useState<ReportSummary | null>(null);
  const [adminDataError, setAdminDataError] = useState("");
  const [memberEditError, setMemberEditError] = useState("");
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
  const [editingMemberId, setEditingMemberId] = useState<string | null>(null);
  const [reviewingProofId, setReviewingProofId] = useState<string | null>(null);
  const [updatingMemberId, setUpdatingMemberId] = useState<string | null>(null);
  const [updatingWorkflowId, setUpdatingWorkflowId] = useState<string | null>(null);
  const [updatingParticipantUsername, setUpdatingParticipantUsername] = useState<
    string | null
  >(null);

  useEffect(() => {
    async function loadAdminData() {
      let participantLoadError = "";

      try {
        const fpoMembers = await getFpoMembers();
        setMembers(fpoMembers);
      } catch (error) {
        participantLoadError = getErrorMessage(
          error,
          "Unable to load farmer profiles."
        );
        setAdminDataError(participantLoadError);
      }

      try {
        const hasToken = await hasBackendSession();
        const [cycles, proofs, summary, workflows] = hasToken
          ? await loadBackendAdminData(currentRole)
          : await loadLocalAdminData();

        setSavedCycles(cycles);
        setSavedProofs(proofs);
        setWorkflowDefinitions(workflows);
        setHasBackendAccess(hasToken);
        setCanReviewEvidence(hasToken && canRolePerform(currentRole, "reviewEvidence"));
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
        setHasBackendAccess(false);
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
  }, [currentRole]);

  useEffect(() => {
    async function loadModules() {
      setEnabledModules(await loadEnabledModules());
    }

    loadModules();
  }, []);

  async function handleCreateMember(input: CreateFpoMemberInput) {
    setIsCreatingParticipant(true);
    setParticipantFormError("");

    try {
      const member = await createFpoMember(input);
      upsertMember(member);
      return true;
    } catch (error) {
      setParticipantFormError(
        getErrorMessage(error, "Unable to create farmer profile and login.")
      );
      return false;
    } finally {
      setIsCreatingParticipant(false);
    }
  }

  async function handleUpdateMember(member: FpoMember, input: UpdateFpoMemberInput) {
    setUpdatingMemberId(member.memberId);
    setMemberEditError("");

    try {
      const updatedMember = await updateFpoMember(member, input);
      upsertMember(updatedMember);
      setEditingMemberId(null);
      return true;
    } catch (error) {
      setMemberEditError(getErrorMessage(error, "Unable to update farmer profile."));
      return false;
    } finally {
      setUpdatingMemberId(null);
    }
  }

  async function handleUpdateMemberStatus(
    member: FpoMember,
    nextStatus: "Active" | "Inactive" | "Suspended"
  ) {
    setUpdatingParticipantUsername(member.username);
    setParticipantFormError("");

    try {
      const updatedMember = await updateFpoMemberStatus(member, nextStatus);
      upsertMember(updatedMember);
    } catch (error) {
      setParticipantFormError(
        getErrorMessage(error, "Unable to update farmer status.")
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
      setWorkflowFormError(getErrorMessage(error, "Unable to update workflow status."));
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
        if (canRolePerform(currentRole, "viewReportSummary")) {
          setReportSummary(await getBackendReportSummary());
        }
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

  function upsertMember(member: FpoMember) {
    setMembers((currentMembers) => [
      member,
      ...currentMembers.filter((item) => item.memberId !== member.memberId)
    ]);
  }

  const participants = members;
  const allCycles = savedCycles;
  const runningCycles = allCycles.filter((cycle) => cycle.status === "running");
  const completedCycles = allCycles.filter((cycle) => cycle.status === "completed");
  const proofRecords = savedProofs;
  const regions = [...new Set(participants.map((member) => member.region))];
  const crops = [...new Set(allCycles.map((cycle) => cycle.crop))];
  const complianceScore = calculateComplianceScore(allCycles, proofRecords);
  const visibleTabs = useMemo(
    () => getVisibleAdminTabs(currentRole, enabledModules),
    [currentRole, enabledModules]
  );

  const summary = useMemo(
    () => [
      { label: "Farmers", value: String(participants.length) },
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

  useEffect(() => {
    if (!visibleTabs.some((item) => item.tab === activeTab)) {
      setActiveTab("overview");
    }
  }, [activeTab, visibleTabs]);

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.shell}>
          <View style={styles.headerRow}>
            <View style={styles.headerText}>
              <Text style={styles.eyebrow}>{dashboardEyebrow(currentRole)}</Text>
              <Text style={styles.title}>Carbon Farming Operations</Text>
              <Text style={styles.copy}>
                Track farmer profiles, regenerative activities, proof records,
                advisories, and report-ready carbon evidence.
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
            {visibleTabs.map(({ label, tab }) => (
              <Pressable
                key={tab}
                style={[styles.tabButton, activeTab === tab && styles.tabButtonActive]}
                onPress={() => setActiveTab(tab)}
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
            <MembersTab
              allCycles={allCycles}
              createError={participantFormError}
              editError={memberEditError}
              editingMemberId={editingMemberId}
              isCreatingParticipant={isCreatingParticipant}
              onCreateMember={handleCreateMember}
              onEditMemberIdChange={setEditingMemberId}
              onUpdateMemberStatus={handleUpdateMemberStatus}
              onUpdateMember={handleUpdateMember}
              participants={participants}
              proofRecords={proofRecords}
              updatingMemberId={updatingMemberId}
              updatingParticipantUsername={updatingParticipantUsername}
            />
          ) : null}

          {activeTab === "carbon" ? <AdminCarbonOverviewTab /> : null}

          {activeTab === "workflows" ? (
            <AdminWorkflowsTab
              activityStartError={activityStartError}
              canManageDefinitions={canRolePerform(
                currentRole,
                "manageWorkflowDefinitions"
              )}
              canUseBackend={hasBackendAccess}
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

          {activeTab === "cropPlanning" ? (
            <AdminCropPlanningTab currentRole={currentRole} members={participants} />
          ) : null}

          {activeTab === "inputDemand" ? <AdminInputDemandTab /> : null}

          {activeTab === "roles" ? (
            <AdminRolesTab canUseBackend={hasBackendAccess} currentRole={currentRole} />
          ) : null}

          {activeTab === "advisories" ? (
            <AdminAdvisoriesTab
              canManageAdvisories={canRolePerform(currentRole, "manageAdvisories")}
              participants={participants}
            />
          ) : null}

          {activeTab === "reports" ? (
            <ReportsTab
              canExportComplianceReport={canRolePerform(
                currentRole,
                "exportComplianceReport"
              )}
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

async function loadBackendAdminData(
  currentRole: StaffRole
): Promise<[CropCycle[], ProofSubmission[], ReportSummary | null, BackendWorkflow[]]> {
  const [cycles, workflows, summary] = await Promise.all([
    getBackendActivities(),
    getBackendWorkflowDefinitions(),
    canRolePerform(currentRole, "viewReportSummary")
      ? getBackendReportSummary()
      : Promise.resolve(null)
  ]);
  const proofs = await getBackendProofs(cycles);
  return [cycles, proofs, summary, workflows];
}

async function loadLocalAdminData(): Promise<
  [CropCycle[], ProofSubmission[], ReportSummary | null, BackendWorkflow[]]
> {
  const [cycles, proofs] = await Promise.all([getSavedCropCycles(), getSavedProofs()]);
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
  participants: FpoMember[];
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

      <Text style={styles.sectionTitle}>Farmers by village</Text>
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
            No farmer profiles are registered yet.
          </Text>
        </View>
      )}
    </View>
  );
}

function MembersTab({
  allCycles,
  createError,
  editError,
  editingMemberId,
  isCreatingParticipant,
  onCreateMember,
  onEditMemberIdChange,
  onUpdateMemberStatus,
  onUpdateMember,
  participants,
  proofRecords,
  updatingMemberId,
  updatingParticipantUsername
}: {
  allCycles: CropCycle[];
  createError: string;
  editError: string;
  editingMemberId: string | null;
  isCreatingParticipant: boolean;
  onCreateMember: (data: CreateFpoMemberInput) => Promise<boolean>;
  onEditMemberIdChange: (memberId: string | null) => void;
  onUpdateMemberStatus: (
    member: FpoMember,
    status: "Active" | "Inactive" | "Suspended"
  ) => Promise<void>;
  onUpdateMember: (member: FpoMember, data: UpdateFpoMemberInput) => Promise<boolean>;
  participants: FpoMember[];
  proofRecords: ProofSubmission[];
  updatingMemberId: string | null;
  updatingParticipantUsername: string | null;
}) {
  const [farmAssetMemberId, setFarmAssetMemberId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const normalizedQuery = searchQuery.trim().toLowerCase();
  const filteredMembers = normalizedQuery
    ? participants.filter((member) =>
        [
          member.name,
          member.memberNumber,
          member.mobileNumber,
          member.village,
          member.taluka,
          member.districtName,
          member.stateName,
          member.username
        ]
          .filter((value): value is string => Boolean(value))
          .some((value) => value.toLowerCase().includes(normalizedQuery))
      )
    : participants;

  return (
    <View style={styles.section}>
      <CreateMemberForm
        error={createError}
        isSubmitting={isCreatingParticipant}
        onSubmit={onCreateMember}
      />

      <View style={styles.managementCard}>
        <Text style={styles.cardTitle}>Farmer profile directory</Text>
        <AdminField
          label="Search farmers"
          value={searchQuery}
          onChange={setSearchQuery}
        />
      </View>

      {filteredMembers.length ? (
        filteredMembers.map((participant) => (
          <View key={participant.username} style={styles.participantCard}>
            <View style={styles.memberCardHeader}>
              <View style={styles.reviewText}>
                <Text style={styles.cardTitle}>{participant.name}</Text>
                <Text style={styles.cardDescription}>
                  Member {participant.memberNumber} - {participant.village} -{" "}
                  {participant.phone}
                </Text>
                <Text style={styles.cardDescription}>
                  {participant.siteName} - Login {participant.username}
                </Text>
                {participant.farmerCategory || participant.age !== undefined ? (
                  <Text style={styles.cardMeta}>
                    {[participant.farmerCategory, formatAge(participant.age)]
                      .filter(Boolean)
                      .join(" - ")}
                  </Text>
                ) : null}
                <Text style={styles.cardMeta}>
                  {countMemberActivities(participant, allCycles, "running")} running,{" "}
                  {countMemberActivities(participant, allCycles, "completed")}{" "}
                  completed, {countMemberProofs(participant, proofRecords)} proof
                  record(s)
                </Text>
              </View>
              <View style={styles.participantActions}>
                <StatusBadge
                  label={participant.status}
                  tone={participant.status === "Active" ? "good" : "warning"}
                />
                <Pressable
                  accessibilityRole="button"
                  disabled={updatingMemberId === participant.memberId}
                  style={({ pressed }) => [
                    styles.secondaryActionButton,
                    (pressed || updatingMemberId === participant.memberId) &&
                      styles.actionButtonPressed
                  ]}
                  onPress={() =>
                    onEditMemberIdChange(
                      editingMemberId === participant.memberId
                        ? null
                        : participant.memberId
                    )
                  }
                >
                  <Text style={styles.secondaryActionButtonText}>
                    {editingMemberId === participant.memberId ? "Close" : "Edit"}
                  </Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  style={({ pressed }) => [
                    styles.secondaryActionButton,
                    pressed && styles.actionButtonPressed
                  ]}
                  onPress={() =>
                    setFarmAssetMemberId(
                      farmAssetMemberId === participant.memberId
                        ? null
                        : participant.memberId
                    )
                  }
                >
                  <Text style={styles.secondaryActionButtonText}>
                    {farmAssetMemberId === participant.memberId
                      ? "Hide land"
                      : "Land records"}
                  </Text>
                </Pressable>
                {participant.status !== "Active" ? (
                  <Pressable
                    accessibilityRole="button"
                    disabled={updatingParticipantUsername === participant.username}
                    style={({ pressed }) => [
                      styles.actionButton,
                      (pressed ||
                        updatingParticipantUsername === participant.username) &&
                        styles.actionButtonPressed
                    ]}
                    onPress={() => onUpdateMemberStatus(participant, "Active")}
                  >
                    <Text style={styles.actionButtonText}>
                      {updatingParticipantUsername === participant.username
                        ? "Saving..."
                        : "Activate"}
                    </Text>
                  </Pressable>
                ) : null}
                {participant.status === "Active" ? (
                  <Pressable
                    accessibilityRole="button"
                    disabled={updatingParticipantUsername === participant.username}
                    style={({ pressed }) => [
                      styles.actionButton,
                      (pressed ||
                        updatingParticipantUsername === participant.username) &&
                        styles.actionButtonPressed
                    ]}
                    onPress={() => onUpdateMemberStatus(participant, "Inactive")}
                  >
                    <Text style={styles.actionButtonText}>
                      {updatingParticipantUsername === participant.username
                        ? "Saving..."
                        : "Deactivate"}
                    </Text>
                  </Pressable>
                ) : null}
                {participant.status !== "Suspended" ? (
                  <Pressable
                    accessibilityRole="button"
                    disabled={updatingParticipantUsername === participant.username}
                    style={({ pressed }) => [
                      styles.secondaryActionButton,
                      (pressed ||
                        updatingParticipantUsername === participant.username) &&
                        styles.actionButtonPressed
                    ]}
                    onPress={() => onUpdateMemberStatus(participant, "Suspended")}
                  >
                    <Text style={styles.secondaryActionButtonText}>Suspend</Text>
                  </Pressable>
                ) : null}
              </View>
            </View>
            {editingMemberId === participant.memberId ? (
              <EditMemberForm
                error={editError}
                isSubmitting={updatingMemberId === participant.memberId}
                member={participant}
                onCancel={() => onEditMemberIdChange(null)}
                onSubmit={(input) => onUpdateMember(participant, input)}
              />
            ) : null}
            {farmAssetMemberId === participant.memberId ? (
              <AdminFarmAssetsPanel member={participant} />
            ) : null}
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            {participants.length
              ? "No farmer profiles match this search."
              : "No farmer profiles are available yet."}
          </Text>
        </View>
      )}
    </View>
  );
}

function CreateMemberForm({
  error,
  isSubmitting,
  onSubmit
}: {
  error: string;
  isSubmitting: boolean;
  onSubmit: (data: CreateFpoMemberInput) => Promise<boolean>;
}) {
  const [aadhaarNumber, setAadhaarNumber] = useState("");
  const [age, setAge] = useState("");
  const [alternateMobileNumber, setAlternateMobileNumber] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [districtName, setDistrictName] = useState("");
  const [farmerCategory, setFarmerCategory] = useState("");
  const [gender, setGender] = useState("");
  const [memberNumber, setMemberNumber] = useState("");
  const [mobileNumber, setMobileNumber] = useState("");
  const [password, setPassword] = useState("");
  const [stateName, setStateName] = useState("");
  const [taluka, setTaluka] = useState("");
  const [username, setUsername] = useState("");
  const [village, setVillage] = useState("");
  const [localError, setLocalError] = useState("");

  async function handleSubmit() {
    const input: CreateFpoMemberInput = {
      aadhaarNumber: aadhaarNumber.trim(),
      age: age.trim(),
      alternateMobileNumber: alternateMobileNumber.trim(),
      displayName: displayName.trim(),
      districtName: districtName.trim(),
      farmerCategory: farmerCategory.trim(),
      gender: gender.trim(),
      memberNumber: memberNumber.trim(),
      mobileNumber: mobileNumber.trim(),
      password,
      stateName: stateName.trim(),
      taluka: taluka.trim(),
      username: username.trim(),
      village: village.trim()
    };

    if (
      !input.memberNumber ||
      !input.displayName ||
      !input.mobileNumber ||
      !input.village ||
      !input.taluka ||
      !input.districtName ||
      !input.stateName ||
      !input.gender ||
      !input.farmerCategory ||
      !input.username ||
      !input.password
    ) {
      setLocalError(
        "Enter member number, name, mobile, village, taluka, district, state, gender, category, username, and password."
      );
      return;
    }

    if (!isValidIndianMobile(input.mobileNumber)) {
      setLocalError("Mobile number must be a 10 digit Indian mobile number.");
      return;
    }

    if (!isValidOptionalAadhaar(input.aadhaarNumber)) {
      setLocalError("Aadhaar number must be 12 digits when provided.");
      return;
    }

    if (password.length < 8) {
      setLocalError("Password must be at least 8 characters.");
      return;
    }

    if (!isValidOptionalAge(input.age)) {
      setLocalError("Age must be a whole number from 0 to 120.");
      return;
    }

    setLocalError("");
    const created = await onSubmit(input);

    if (created) {
      setAadhaarNumber("");
      setAge("");
      setAlternateMobileNumber("");
      setDisplayName("");
      setDistrictName("");
      setFarmerCategory("");
      setGender("");
      setMemberNumber("");
      setMobileNumber("");
      setPassword("");
      setStateName("");
      setTaluka("");
      setUsername("");
      setVillage("");
    }
  }

  return (
    <View style={styles.managementCard}>
      <Text style={styles.cardTitle}>Create farmer profile and login</Text>
      <View style={styles.formGrid}>
        <AdminField
          label="Member number"
          value={memberNumber}
          onChange={setMemberNumber}
        />
        <AdminField label="Full name" value={displayName} onChange={setDisplayName} />
        <AdminField
          label="Mobile number"
          value={mobileNumber}
          onChange={setMobileNumber}
          keyboardType="phone-pad"
        />
        <AdminField
          label="Alternate mobile"
          value={alternateMobileNumber}
          onChange={setAlternateMobileNumber}
          keyboardType="phone-pad"
        />
        <AdminField
          label="Aadhaar number"
          value={aadhaarNumber}
          onChange={setAadhaarNumber}
          keyboardType="numeric"
        />
        <AdminField label="Village" value={village} onChange={setVillage} />
        <AdminField label="Taluka" value={taluka} onChange={setTaluka} />
        <AdminField label="District" value={districtName} onChange={setDistrictName} />
        <AdminField label="State" value={stateName} onChange={setStateName} />
        <AdminField label="Gender" value={gender} onChange={setGender} />
        <AdminField label="Age" value={age} onChange={setAge} keyboardType="numeric" />
        <AdminField
          label="Farmer category"
          value={farmerCategory}
          onChange={setFarmerCategory}
        />
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
            {isSubmitting ? "Creating..." : "Create farmer login"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

function EditMemberForm({
  error,
  isSubmitting,
  member,
  onCancel,
  onSubmit
}: {
  error: string;
  isSubmitting: boolean;
  member: FpoMember;
  onCancel: () => void;
  onSubmit: (data: UpdateFpoMemberInput) => Promise<boolean>;
}) {
  const [aadhaarNumber, setAadhaarNumber] = useState(member.aadhaarNumber ?? "");
  const [age, setAge] = useState(member.age?.toString() ?? "");
  const [alternateMobileNumber, setAlternateMobileNumber] = useState(
    member.alternateMobileNumber ?? ""
  );
  const [displayName, setDisplayName] = useState(member.displayName);
  const [districtName, setDistrictName] = useState(member.districtName ?? "");
  const [farmerCategory, setFarmerCategory] = useState(member.farmerCategory ?? "");
  const [gender, setGender] = useState(member.gender ?? "");
  const [memberNumber, setMemberNumber] = useState(member.memberNumber);
  const [mobileNumber, setMobileNumber] = useState(member.mobileNumber);
  const [stateName, setStateName] = useState(member.stateName);
  const [taluka, setTaluka] = useState(member.taluka);
  const [village, setVillage] = useState(member.village);
  const [localError, setLocalError] = useState("");

  async function handleSubmit() {
    const input: UpdateFpoMemberInput = {
      aadhaarNumber: aadhaarNumber.trim(),
      age: age.trim(),
      alternateMobileNumber: alternateMobileNumber.trim(),
      displayName: displayName.trim(),
      districtName: districtName.trim(),
      farmerCategory: farmerCategory.trim(),
      gender: gender.trim(),
      memberNumber: memberNumber.trim(),
      mobileNumber: mobileNumber.trim(),
      stateName: stateName.trim(),
      taluka: taluka.trim(),
      village: village.trim()
    };

    if (
      !input.memberNumber ||
      !input.displayName ||
      !input.mobileNumber ||
      !input.village ||
      !input.taluka ||
      !input.districtName ||
      !input.stateName ||
      !input.gender ||
      !input.farmerCategory
    ) {
      setLocalError(
        "Enter member number, name, mobile, village, taluka, district, state, gender, and category."
      );
      return;
    }

    if (!isValidIndianMobile(input.mobileNumber)) {
      setLocalError("Mobile number must be a 10 digit Indian mobile number.");
      return;
    }

    if (!isValidOptionalAadhaar(input.aadhaarNumber)) {
      setLocalError("Aadhaar number must be 12 digits when provided.");
      return;
    }

    if (!isValidOptionalAge(input.age)) {
      setLocalError("Age must be a whole number from 0 to 120.");
      return;
    }

    setLocalError("");
    await onSubmit(input);
  }

  return (
    <View style={styles.inlineEditCard}>
      <Text style={styles.subsectionTitle}>Edit farmer profile</Text>
      <View style={styles.formGrid}>
        <AdminField
          label="Member number"
          value={memberNumber}
          onChange={setMemberNumber}
        />
        <AdminField label="Full name" value={displayName} onChange={setDisplayName} />
        <AdminField
          label="Mobile number"
          value={mobileNumber}
          onChange={setMobileNumber}
          keyboardType="phone-pad"
        />
        <AdminField
          label="Alternate mobile"
          value={alternateMobileNumber}
          onChange={setAlternateMobileNumber}
          keyboardType="phone-pad"
        />
        <AdminField
          label="Aadhaar number"
          value={aadhaarNumber}
          onChange={setAadhaarNumber}
          keyboardType="numeric"
        />
        <AdminField label="Village" value={village} onChange={setVillage} />
        <AdminField label="Taluka" value={taluka} onChange={setTaluka} />
        <AdminField label="District" value={districtName} onChange={setDistrictName} />
        <AdminField label="State" value={stateName} onChange={setStateName} />
        <AdminField label="Gender" value={gender} onChange={setGender} />
        <AdminField label="Age" value={age} onChange={setAge} keyboardType="numeric" />
        <AdminField
          label="Farmer category"
          value={farmerCategory}
          onChange={setFarmerCategory}
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
            {isSubmitting ? "Saving..." : "Save farmer"}
          </Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={styles.secondaryActionButton}
          onPress={onCancel}
        >
          <Text style={styles.secondaryActionButtonText}>Cancel</Text>
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
  keyboardType?: "default" | "numeric" | "phone-pad";
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
  canExportComplianceReport,
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
  canExportComplianceReport: boolean;
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
  const canExportReport =
    canExportComplianceReport && Boolean(reportSummary) && !isExportingReport;

  return (
    <View style={styles.section}>
      <AdminFpoReportsPanel />

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
              disabled={!canExportReport}
              style={({ pressed }) => [
                styles.createButton,
                (!canExportReport || pressed) && styles.createButtonPressed
              ]}
              onPress={() => onExportReport("PDF")}
            >
              <Text style={styles.createButtonText}>
                {exportingReportFormat === "PDF" ? "Exporting PDF..." : "Export PDF"}
              </Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              disabled={!canExportReport}
              style={({ pressed }) => [
                styles.actionButton,
                (!canExportReport || pressed) && styles.actionButtonPressed
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
          {!canExportComplianceReport ? (
            <Text style={styles.cardMeta}>
              Compliance export is available to Admin and FPO Manager roles.
            </Text>
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

function dashboardEyebrow(role: StaffRole) {
  switch (role) {
    case "admin":
      return "Platform admin dashboard";
    case "fpoManager":
      return "FPO manager dashboard";
    case "fieldCoordinator":
      return "Field coordinator dashboard";
    default:
      return "Operations dashboard";
  }
}

function formatAge(age: number | undefined) {
  return age === undefined ? "" : `${age} years`;
}

function isValidOptionalAge(age: string | undefined) {
  if (!age) {
    return true;
  }

  const parsedAge = Number(age);
  return Number.isInteger(parsedAge) && parsedAge >= 0 && parsedAge <= 120;
}

function isValidIndianMobile(mobileNumber: string | undefined) {
  return Boolean(mobileNumber?.replace(/\D/g, "").match(/^[6-9][0-9]{9}$/));
}

function isValidOptionalAadhaar(aadhaarNumber: string | undefined) {
  if (!aadhaarNumber) {
    return true;
  }

  return /^[0-9]{12}$/.test(aadhaarNumber.replace(/\D/g, ""));
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
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
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
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
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
    gap: 12,
    padding: 16
  },
  memberCardHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    width: "100%"
  },
  participantActions: {
    alignItems: "flex-end",
    gap: 10
  },
  inlineEditCard: {
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    gap: 14,
    paddingTop: 14,
    width: "100%"
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
  secondaryActionButton: {
    alignItems: "center",
    borderColor: "#9fb4bf",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 88,
    paddingHorizontal: 12
  },
  secondaryActionButtonText: {
    color: "#53666f",
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
