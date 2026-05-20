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
import { getEnabledClientModuleIds, isClientModuleEnabled } from "../modules";
import {
  ActivityParticipant,
  getActivityParticipants,
  mergeActivityParticipants,
  toActivityParticipantFromCarbonProfile,
  toActivityParticipantFromFpoMember
} from "../data/activityParticipantStore";
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
import { AdminCarbonOverviewTab } from "../modules/carbon";
import { CarbonProfileRecord } from "../modules/carbon/data/carbonProfileStore";
import { AdminCropPlanningTab } from "./AdminCropPlanningTab";
import { AdminFarmAssetsPanel } from "./AdminFarmAssetsPanel";
import { AdminFpoReportsPanel } from "./AdminFpoReportsPanel";
import { AdminInputDemandTab } from "./AdminInputDemandTab";
import { AdminRolesTab } from "./AdminRolesTab";
import { AdminWorkflowsTab } from "./AdminWorkflowsTab";
import { StatusBadge } from "../ui/StatusBadge";
import { EvidenceReviewQueue } from "../shared/components/EvidenceReviewQueue";
import { FarmerIdentityFields } from "../shared/farmers/FarmerIdentityFields";
import {
  emptyFarmerIdentityInput,
  FarmerIdentityInput,
  normalizeFarmerIdentityInput,
  validateFarmerIdentityInput
} from "../shared/farmers/farmerIdentity";

type AdminHomeScreenProps = {
  currentRole: StaffRole;
  onLogout: () => void;
};

export function AdminHomeScreen({ currentRole, onLogout }: AdminHomeScreenProps) {
  const [activeTab, setActiveTab] = useState<AdminTabId>(
    isClientModuleEnabled("carbon") ? "carbon" : "overview"
  );
  const [activityParticipants, setActivityParticipants] = useState<
    ActivityParticipant[]
  >([]);
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
  const [carbonUpsellMessage, setCarbonUpsellMessage] = useState("");
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
      let nextFpoMembers: FpoMember[] = [];
      const hasToken = await hasBackendSession();

      if (isClientModuleEnabled("fpo")) {
        try {
          nextFpoMembers = await getFpoMembers();
          setMembers(nextFpoMembers);
        } catch (error) {
          participantLoadError = getErrorMessage(
            error,
            "Unable to load farmer profiles."
          );
          setAdminDataError(participantLoadError);
        }
      } else {
        setMembers([]);
      }

      try {
        setActivityParticipants(
          await getActivityParticipants({
            fpoMembers: nextFpoMembers,
            includeCarbon: isClientModuleEnabled("carbon"),
            includeFpo: isClientModuleEnabled("fpo"),
            useBackend: hasToken
          })
        );
      } catch (error) {
        const participantRegistryError = getErrorMessage(
          error,
          "Unable to load activity participants."
        );
        setActivityParticipants(hasToken
          ? []
          : mergeActivityParticipants(
              nextFpoMembers.map(toActivityParticipantFromFpoMember)
            ));
        participantLoadError = participantLoadError
          ? `${participantLoadError} ${participantRegistryError}`
          : participantRegistryError;
        setAdminDataError(participantLoadError);
      }

      try {
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
    if (hasBackendAccess) {
      getActivityParticipants({
        fpoMembers: [member, ...members.filter((item) => item.memberId !== member.memberId)],
        includeCarbon: isClientModuleEnabled("carbon"),
        includeFpo: isClientModuleEnabled("fpo"),
        useBackend: true
      })
        .then(setActivityParticipants)
        .catch((error) =>
          setParticipantFormError(
            getErrorMessage(error, "Unable to refresh activity participants.")
          )
        );
    } else {
      setActivityParticipants((currentParticipants) =>
        mergeActivityParticipants([
          ...currentParticipants,
          toActivityParticipantFromFpoMember(member)
        ])
      );
    }
  }

  function handleCarbonProfilesLoaded(profiles: CarbonProfileRecord[]) {
    if (hasBackendAccess) {
      getActivityParticipants({
        fpoMembers: members,
        includeCarbon: isClientModuleEnabled("carbon"),
        includeFpo: isClientModuleEnabled("fpo"),
        useBackend: true
      })
        .then(setActivityParticipants)
        .catch((error) =>
          setAdminDataError(
            getErrorMessage(error, "Unable to refresh activity participants.")
          )
        );
      return;
    }

    setActivityParticipants(
      mergeActivityParticipants([
        ...members.map(toActivityParticipantFromFpoMember),
        ...profiles
          .map(toActivityParticipantFromCarbonProfile)
          .filter((participant): participant is ActivityParticipant =>
            Boolean(participant)
          )
      ])
    );
  }

  const participants = members;
  const workflowParticipants = activityParticipants;
  const allCycles = savedCycles;
  const runningCycles = allCycles.filter((cycle) => cycle.status === "running");
  const completedCycles = allCycles.filter((cycle) => cycle.status === "completed");
  const proofRecords = savedProofs;
  const regions = [...new Set(participants.map((member) => member.region))];
  const crops = [...new Set(allCycles.map((cycle) => cycle.crop))];
  const complianceScore = calculateComplianceScore(allCycles, proofRecords);
  const carbonUiEnabled = isClientModuleEnabled("carbon");
  const fpoUiEnabled = isClientModuleEnabled("fpo");
  const showCarbonUpsell =
    fpoUiEnabled && !carbonUiEnabled;
  const uiFeatures = useMemo(
    () => ({ enabledClientModules: getEnabledClientModuleIds() }),
    []
  );
  const visibleTabs = useMemo(
    () => getVisibleAdminTabs(currentRole, enabledModules, uiFeatures),
    [currentRole, enabledModules, uiFeatures]
  );

  const summary = useMemo(
    () =>
      fpoUiEnabled
        ? [
            { label: "Farmers", value: String(participants.length) },
            { label: "Running cycles", value: String(runningCycles.length) },
            { label: "Completed", value: String(completedCycles.length) },
            { label: "Proof records", value: String(proofRecords.length) }
          ]
        : [
            { label: "Running activities", value: String(runningCycles.length) },
            { label: "Completed", value: String(completedCycles.length) },
            { label: "Proof records", value: String(proofRecords.length) },
            {
              label: "Client package",
              value: carbonUiEnabled ? "Carbon" : "Core"
            }
          ],
    [
      completedCycles.length,
      carbonUiEnabled,
      fpoUiEnabled,
      participants.length,
      proofRecords.length,
      runningCycles.length
    ]
  );

  useEffect(() => {
    if (!visibleTabs.some((item) => item.tab === activeTab)) {
      setActiveTab(visibleTabs[0]?.tab ?? "overview");
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
              <Text style={styles.eyebrow}>
                {dashboardEyebrow(currentRole, fpoUiEnabled)}
              </Text>
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

          {showCarbonUpsell ? (
            <View style={styles.upsellCard}>
              <View style={styles.upsellText}>
                <Text style={styles.upsellTitle}>Carbon credits are available</Text>
                <Text style={styles.cardDescription}>
                  Add soil carbon scoring, carbon-positive activities, and
                  credit-readiness reports to this FPO package.
                </Text>
                {carbonUpsellMessage ? (
                  <Text style={styles.cardMeta}>{carbonUpsellMessage}</Text>
                ) : null}
              </View>
              <Pressable
                accessibilityRole="button"
                style={styles.upsellButton}
                onPress={() =>
                  setCarbonUpsellMessage(
                    "Ask the platform admin to enable the Carbon Accounting module for this tenant."
                  )
                }
              >
                <Text style={styles.upsellButtonText}>Get Carbon Credits</Text>
              </Pressable>
            </View>
          ) : null}

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
              showFpoSummary={fpoUiEnabled}
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

          {activeTab === "carbon" ? (
            <AdminCarbonOverviewTab onProfilesLoaded={handleCarbonProfilesLoaded} />
          ) : null}

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
              participants={workflowParticipants}
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
              managerRoleLabel={fpoUiEnabled ? "FPO Manager" : "Farm Manager"}
              onExportReport={handleExportReport}
              proofRecords={proofRecords}
              reportExport={reportExport}
              reportExportError={reportExportError}
              reportSummary={reportSummary}
              regions={regions}
              showFpoReports={fpoUiEnabled}
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
  runningCycles,
  showFpoSummary
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
  showFpoSummary: boolean;
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
      <EvidenceReviewQueue
        canReview={canReviewEvidence}
        emptyMessage="No proof records have been submitted yet."
        error={reviewError}
        items={proofRecords.map((proof) => ({
          ...proof,
          description: `${proof.participantName ?? proof.farmer} - ${proof.crop} - ${
            proof.region
          }`,
          submittedLabel: `Submitted ${proof.submittedOn}`,
          title: proof.action
        }))}
        module="fpo"
        reviewingItemId={reviewingProofId}
        onReview={onReviewProof}
      />

      {showFpoSummary ? (
        <>
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
        </>
      ) : null}
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
  const [identity, setIdentity] = useState<FarmerIdentityInput>(
    emptyFarmerIdentityInput()
  );
  const [localError, setLocalError] = useState("");

  async function handleSubmit() {
    const input = normalizeFarmerIdentityInput(identity);
    const validationError = validateFarmerIdentityInput(input, {
      requireLogin: true
    });

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    setLocalError("");
    const created = await onSubmit(toCreateFpoMemberInput(input));

    if (created) {
      setIdentity(emptyFarmerIdentityInput());
    }
  }

  return (
    <View style={styles.managementCard}>
      <Text style={styles.cardTitle}>Create farmer profile and login</Text>
      <FarmerIdentityFields
        includeLogin
        value={identity}
        onChange={setIdentity}
      />

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
  const [identity, setIdentity] = useState<FarmerIdentityInput>(
    emptyFarmerIdentityInput({
      aadhaarNumber: member.aadhaarNumber ?? "",
      age: member.age?.toString() ?? "",
      alternateMobileNumber: member.alternateMobileNumber ?? "",
      displayName: member.displayName,
      districtName: member.districtName ?? "",
      farmerCategory: member.farmerCategory ?? "",
      gender: member.gender ?? "",
      memberNumber: member.memberNumber,
      mobileNumber: member.mobileNumber,
      stateName: member.stateName,
      taluka: member.taluka,
      username: member.username,
      village: member.village
    })
  );
  const [localError, setLocalError] = useState("");

  async function handleSubmit() {
    const input = normalizeFarmerIdentityInput(identity);
    const validationError = validateFarmerIdentityInput(input, {
      requireLogin: false
    });

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    setLocalError("");
    await onSubmit(toUpdateFpoMemberInput(input));
  }

  return (
    <View style={styles.inlineEditCard}>
      <Text style={styles.subsectionTitle}>Edit farmer profile</Text>
      <FarmerIdentityFields
        includeLogin={false}
        value={identity}
        onChange={setIdentity}
      />

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

function toCreateFpoMemberInput(input: FarmerIdentityInput): CreateFpoMemberInput {
  return {
    aadhaarNumber: input.aadhaarNumber,
    age: input.age,
    alternateMobileNumber: input.alternateMobileNumber,
    displayName: input.displayName,
    districtName: input.districtName,
    farmerCategory: input.farmerCategory,
    gender: input.gender,
    memberNumber: input.memberNumber,
    mobileNumber: input.mobileNumber,
    password: input.password,
    stateName: input.stateName,
    taluka: input.taluka,
    username: input.username,
    village: input.village
  };
}

function toUpdateFpoMemberInput(input: FarmerIdentityInput): UpdateFpoMemberInput {
  return {
    aadhaarNumber: input.aadhaarNumber,
    age: input.age,
    alternateMobileNumber: input.alternateMobileNumber,
    displayName: input.displayName,
    districtName: input.districtName,
    farmerCategory: input.farmerCategory,
    gender: input.gender,
    memberNumber: input.memberNumber,
    mobileNumber: input.mobileNumber,
    stateName: input.stateName,
    taluka: input.taluka,
    village: input.village
  };
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
  managerRoleLabel,
  onExportReport,
  proofRecords,
  reportExport,
  reportExportError,
  reportSummary,
  regions,
  showFpoReports
}: {
  canExportComplianceReport: boolean;
  complianceScore: number;
  crops: string[];
  exportingReportFormat: ReportExport["format"] | null;
  managerRoleLabel: string;
  onExportReport: (format: ReportExport["format"]) => Promise<void>;
  proofRecords: ProofSubmission[];
  reportExport: ReportExport | null;
  reportExportError: string;
  reportSummary: ReportSummary | null;
  regions: string[];
  showFpoReports: boolean;
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
      {showFpoReports ? <AdminFpoReportsPanel /> : null}

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
              Compliance export is available to Admin and {managerRoleLabel} roles.
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

function dashboardEyebrow(role: StaffRole, fpoUiEnabled: boolean) {
  switch (role) {
    case "admin":
      return "Platform admin dashboard";
    case "fpoManager":
      return fpoUiEnabled ? "FPO manager dashboard" : "Farm manager dashboard";
    case "fieldCoordinator":
      return "Field coordinator dashboard";
    default:
      return "Operations dashboard";
  }
}

function formatAge(age: number | undefined) {
  return age === undefined ? "" : `${age} years`;
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
  upsellCard: {
    alignItems: "center",
    backgroundColor: "#ffffff",
    borderColor: "#b8d8d0",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 14,
    justifyContent: "space-between",
    marginBottom: 20,
    padding: 16
  },
  upsellText: {
    flex: 1,
    gap: 4
  },
  upsellTitle: {
    color: "#172126",
    fontSize: 16,
    fontWeight: "800"
  },
  upsellButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    paddingHorizontal: 14
  },
  upsellButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
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
