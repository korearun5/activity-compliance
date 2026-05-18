import * as ImagePicker from "expo-image-picker";
import { useEffect, useMemo, useState } from "react";
import {
  Image,
  Modal,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View
} from "react-native";

import { appConfig } from "../core/config/appConfig";
import { getErrorMessage } from "../core/errors/AppError";
import {
  activateNextPendingTask,
  calculateTaskProgress,
  findEvidenceForTask,
  isActivityComplete,
  isEvidenceEditable
} from "../core/workflow/workflowEngine";
import { getSavedCropCycles, saveCropCycle } from "../data/cropCycleStore";
import {
  CropCycle,
  CropStep,
  CropTemplate,
  profileTemplate,
  ProfileField,
  ProofSubmission
} from "../data/agricultureConfig";
import { getUserProfile } from "../data/profileStore";
import { getSavedProofs, saveProof } from "../data/proofStore";
import {
  getBackendActivities,
  getBackendActivity,
  getBackendProofs,
  getBackendWorkflowTemplates,
  hasBackendSession,
  startBackendActivity,
  uploadBackendProof,
  upsertProofSubmission
} from "../data/workflowActivityStore";
import { getCachedEnabledModules, PlatformModuleCode } from "../data/moduleStore";
import { getVisibleFarmerTabs, type FarmerTabId } from "../auth/roleAccess";
import { getEnabledClientModuleIds } from "../modules";
import { StatusBadge } from "../ui/StatusBadge";
import { UserCarbonScreen } from "../modules/carbon";

type UserHomeScreenProps = {
  username: string | null;
  onLogout: () => void;
};

type ParticipantTab = FarmerTabId;

type SelectedProof = {
  cycleId: string;
  stepId: string;
  crop: string;
  region: string;
  plot: string;
  action: string;
};

export function UserHomeScreen({ username, onLogout }: UserHomeScreenProps) {
  const [activeTab, setActiveTab] = useState<ParticipantTab>(
    getEnabledClientModuleIds().includes("carbon") ? "carbon" : "cycles"
  );
  const [savedProofs, setSavedProofs] = useState<ProofSubmission[]>([]);
  const [savedCycles, setSavedCycles] = useState<CropCycle[]>([]);
  const [workflowTemplates, setWorkflowTemplates] = useState<CropTemplate[]>([]);
  const [profileFields, setProfileFields] = useState<ProfileField[]>(profileTemplate);
  const [selectedProof, setSelectedProof] = useState<SelectedProof | null>(null);
  const [proofNote, setProofNote] = useState("");
  const [proofPhotoUri, setProofPhotoUri] = useState<string | null>(null);
  const [proofError, setProofError] = useState("");
  const [profileError, setProfileError] = useState("");
  const [isStartCropOpen, setIsStartCropOpen] = useState(false);
  const [selectedCrop, setSelectedCrop] = useState("");
  const [plotName, setPlotName] = useState("");
  const [startDate, setStartDate] = useState(toInputDate(new Date()));
  const [startCropError, setStartCropError] = useState("");
  const [activityLoadError, setActivityLoadError] = useState("");
  const [completedCropName, setCompletedCropName] = useState<string | null>(null);
  const [enabledModules, setEnabledModules] = useState<PlatformModuleCode[] | null>(
    null
  );

  useEffect(() => {
    async function loadParticipantData() {
      const hasToken = await hasBackendSession();

      try {
        const profile = await getUserProfile(username);
        setProfileFields(profile);
        setProfileError("");
      } catch (error) {
        setProfileFields(profileTemplate);
        setProfileError(getErrorMessage(error, "Unable to load farmer profile."));
      }

      if (hasToken) {
        try {
          const [templates, cycles] = await Promise.all([
            getBackendWorkflowTemplates(),
            getBackendActivities()
          ]);
          const proofs = await getBackendProofs(cycles);

          setWorkflowTemplates(templates);
          setSavedCycles(cycles);
          setSavedProofs(proofs);
          setActivityLoadError("");
          return;
        } catch (error) {
          setActivityLoadError(getErrorMessage(error, "Unable to load activities."));
          setWorkflowTemplates([]);
          setSavedCycles([]);
          setSavedProofs([]);
          return;
        }
      }

      const [proofs, cycles] = await Promise.all([
        getSavedProofs(),
        getSavedCropCycles()
      ]);

      setWorkflowTemplates([]);
      setSavedProofs(proofs);
      setSavedCycles(cycles);
      setActivityLoadError("");
    }

    loadParticipantData();
  }, [username]);

  useEffect(() => {
    async function loadModules() {
      setEnabledModules(await getCachedEnabledModules());
    }

    loadModules();
  }, []);

  const participantName = getProfileValue(profileFields, "Name", "Participant");
  const participantRegion = getProfileValue(
    profileFields,
    "Region",
    "Unassigned region"
  );

  const allCycles = useMemo(() => savedCycles, [savedCycles]);

  const displayCycles = useMemo(
    () =>
      allCycles.map((cycle) => {
        const proofAdjustedSteps = cycle.steps.map((step) => {
          const proof = findStepProof(savedProofs, cycle.id, step.id);
          return proof ? { ...step, status: "done" as const } : step;
        });
        const steps = activateNextPendingStep(proofAdjustedSteps);

        return {
          ...cycle,
          progress: calculateTaskProgress(steps),
          status: isActivityComplete({ id: cycle.id, steps }, savedProofs)
            ? ("completed" as const)
            : cycle.status,
          steps
        };
      }),
    [allCycles, savedProofs]
  );

  const runningCycles = displayCycles.filter((cycle) => cycle.status === "running");
  const completedCycles = displayCycles.filter((cycle) => cycle.status === "completed");
  const nextActionCount = runningCycles.filter((cycle) =>
    cycle.steps.some((step) => step.status === "next")
  ).length;

  const summary = [
    { label: "Running activities", value: String(runningCycles.length) },
    { label: "Next actions", value: String(nextActionCount) },
    { label: "Finished activities", value: String(completedCycles.length) },
    { label: "Proof saved", value: String(savedProofs.length) }
  ];
  const uiFeatures = useMemo(
    () => ({ enabledClientModules: getEnabledClientModuleIds() }),
    []
  );
  const visibleTabs = useMemo(
    () => getVisibleFarmerTabs(enabledModules, uiFeatures),
    [enabledModules, uiFeatures]
  );

  useEffect(() => {
    if (!visibleTabs.some((item) => item.tab === activeTab)) {
      setActiveTab(visibleTabs[0]?.tab ?? "cycles");
    }
  }, [activeTab, visibleTabs]);

  async function handlePickPhoto() {
    setProofError("");

    const result = await ImagePicker.launchImageLibraryAsync({
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.75
    });

    if (!result.canceled) {
      setProofPhotoUri(result.assets[0].uri);
    }
  }

  function openProofForm(cycle: CropCycle, step: CropStep) {
    const existingProof = findStepProof(savedProofs, cycle.id, step.id);

    setSelectedProof({
      cycleId: cycle.id,
      stepId: step.id,
      crop: cycle.crop,
      region: cycle.region,
      plot: cycle.plot,
      action: step.title
    });
    setProofNote(existingProof?.note ?? "");
    setProofPhotoUri(existingProof?.photoUri ?? null);
    setProofError("");
  }

  async function handleSubmitProof() {
    if (!selectedProof) {
      return;
    }

    if (!proofPhotoUri) {
      setProofError("Attach a proof photo before submitting.");
      return;
    }

    if (await hasBackendSession()) {
      try {
        const submission = await uploadBackendProof({
          activityId: selectedProof.cycleId,
          activityTaskId: selectedProof.stepId,
          note: proofNote,
          photoUri: proofPhotoUri
        });
        const nextProofs = upsertProofSubmission(savedProofs, {
          ...submission,
          action: selectedProof.action,
          crop: selectedProof.crop,
          locationName: selectedProof.region,
          region: selectedProof.region,
          taskTitle: selectedProof.action,
          workflowName: selectedProof.crop
        });
        setSavedProofs(nextProofs);

        try {
          const refreshedActivity = await getBackendActivity(selectedProof.cycleId);
          setSavedCycles((currentCycles) => [
            refreshedActivity,
            ...currentCycles.filter((cycle) => cycle.id !== refreshedActivity.id)
          ]);

          if (refreshedActivity.status === "completed") {
            setCompletedCropName(refreshedActivity.crop);
          }
        } catch {
          const submittedCycle = allCycles.find(
            (cycle) => cycle.id === selectedProof.cycleId
          );

          if (submittedCycle && isCycleComplete(submittedCycle, nextProofs)) {
            setCompletedCropName(submittedCycle.crop);
          }
        }

        setSelectedProof(null);
        setProofNote("");
        setProofPhotoUri(null);
        setProofError("");
      } catch (error) {
        setProofError(getErrorMessage(error, "Unable to upload proof photo."));
      }
      return;
    }

    const submittedAt = Date.now();
    const submission: ProofSubmission = {
      id: `${selectedProof.cycleId}-${selectedProof.stepId}`,
      cycleId: selectedProof.cycleId,
      stepId: selectedProof.stepId,
      farmerUsername: username ?? undefined,
      farmer: participantName,
      participantName,
      participantUsername: username ?? undefined,
      crop: selectedProof.crop,
      workflowName: selectedProof.crop,
      region: selectedProof.region,
      locationName: selectedProof.region,
      action: selectedProof.action,
      taskTitle: selectedProof.action,
      submittedOn: formatDisplayDate(new Date(submittedAt)),
      submittedAt,
      status: "done",
      activityId: selectedProof.cycleId,
      taskId: selectedProof.stepId,
      note: proofNote.trim() || undefined,
      photoUri: proofPhotoUri
    };

    const nextProofs = await saveProof(submission);
    setSavedProofs(nextProofs);
    const submittedCycle = allCycles.find(
      (cycle) => cycle.id === selectedProof.cycleId
    );

    if (submittedCycle && isCycleComplete(submittedCycle, nextProofs)) {
      setCompletedCropName(submittedCycle.crop);
    }

    setSelectedProof(null);
    setProofNote("");
    setProofPhotoUri(null);
    setProofError("");
  }

  async function handleStartCrop() {
    const template = workflowTemplates.find((item) => item.crop === selectedCrop);
    const parsedStartDate = new Date(startDate);

    if (!workflowTemplates.length) {
      setStartCropError("No crop workflow is configured yet.");
      return;
    }

    if (!template || Number.isNaN(parsedStartDate.getTime())) {
      setStartCropError("Select a crop and enter a valid start date.");
      return;
    }

    if (!plotName.trim()) {
      setStartCropError("Enter a plot name before starting the crop.");
      return;
    }

    if (await hasBackendSession()) {
      if (!template.id) {
        setStartCropError("Selected workflow is missing a backend id.");
        return;
      }

      try {
        const cycle = await startBackendActivity({
          locationName: participantRegion,
          startedOn: startDate,
          unitName: plotName.trim(),
          workflowDefinitionId: template.id
        });

        setSavedCycles((currentCycles) => [
          cycle,
          ...currentCycles.filter((item) => item.id !== cycle.id)
        ]);
        setPlotName("");
        setStartDate(toInputDate(new Date()));
        setStartCropError("");
        setIsStartCropOpen(false);
      } catch (error) {
        setStartCropError(getErrorMessage(error, "Unable to start activity."));
      }
      return;
    }

    const expectedHarvest = addDays(parsedStartDate, template.durationDays);
    const cycle: CropCycle = {
      id: `${template.crop.toLowerCase()}-${Date.now()}`,
      farmerUsername: username ?? undefined,
      participantUsername: username ?? undefined,
      crop: template.crop,
      workflowName: template.crop,
      region: participantRegion,
      locationName: participantRegion,
      plot: plotName.trim(),
      unitName: plotName.trim(),
      startedOn: formatDisplayDate(parsedStartDate),
      expectedHarvest: formatDisplayDate(expectedHarvest),
      expectedCompletion: formatDisplayDate(expectedHarvest),
      progress: 0,
      status: "running",
      steps: template.steps.map((step, index) => ({
        id: step.id,
        title: step.title,
        due: `Week ${step.week}`,
        status: index === 0 ? "next" : "pending"
      }))
    };

    setSavedCycles(await saveCropCycle(cycle));
    setPlotName("");
    setStartDate(toInputDate(new Date()));
    setStartCropError("");
    setIsStartCropOpen(false);
  }

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.topBar}>
        <View>
          <Text style={styles.appTitle}>Activity Dashboard</Text>
          <Text style={styles.appSubtitle}>{participantName}</Text>
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

      <View style={styles.navRow}>
        {visibleTabs.map(({ label, tab }) => (
          <Pressable
            key={tab}
            style={[styles.navButton, activeTab === tab && styles.navButtonActive]}
            onPress={() => setActiveTab(tab)}
          >
            <Text
              style={[
                styles.navButtonText,
                activeTab === tab && styles.navButtonTextActive
              ]}
            >
              {label}
            </Text>
          </Pressable>
        ))}
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.shell}>
          {activeTab === "cycles" ? (
            <CyclesView
              activityLoadError={activityLoadError}
              isStartCropOpen={isStartCropOpen}
              plotName={plotName}
              proofError={proofError}
              proofNote={proofNote}
              proofPhotoUri={proofPhotoUri}
              runningCycles={runningCycles}
              savedProofs={savedProofs}
              selectedCrop={selectedCrop}
              selectedProof={selectedProof}
              startCropError={startCropError}
              startDate={startDate}
              workflowTemplates={workflowTemplates}
              setIsStartCropOpen={setIsStartCropOpen}
              setPlotName={setPlotName}
              setProofNote={setProofNote}
              setSelectedCrop={setSelectedCrop}
              setStartDate={setStartDate}
              onPickPhoto={handlePickPhoto}
              onStartCrop={handleStartCrop}
              onSubmitProof={handleSubmitProof}
              onCancelProof={() => setSelectedProof(null)}
              onOpenProof={openProofForm}
            />
          ) : null}

          {activeTab === "dashboard" ? (
            <DashboardView summary={summary} runningCycles={runningCycles} />
          ) : null}

          {activeTab === "carbon" ? <UserCarbonScreen username={username} /> : null}

          {activeTab === "profile" ? (
            <ProfileView profileError={profileError} profileFields={profileFields} />
          ) : null}

          {activeTab === "history" ? (
            <HistoryView completedCycles={completedCycles} />
          ) : null}
        </View>
      </ScrollView>

      <Modal
        animationType="fade"
        transparent
        visible={Boolean(completedCropName)}
        onRequestClose={() => setCompletedCropName(null)}
      >
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <Text style={styles.modalEyebrow}>Congratulations</Text>
            <Text style={styles.modalTitle}>{completedCropName} activity completed</Text>
            <Text style={styles.modalCopy}>
              All required process steps are done. This activity has moved to History
              with its proof records.
            </Text>
            <Pressable
              style={styles.primaryButton}
              onPress={() => {
                setCompletedCropName(null);
                setActiveTab("history");
              }}
            >
              <Text style={styles.primaryButtonText}>View history</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function CyclesView({
  activityLoadError,
  isStartCropOpen,
  plotName,
  proofError,
  proofNote,
  proofPhotoUri,
  runningCycles,
  savedProofs,
  selectedCrop,
  selectedProof,
  startCropError,
  startDate,
  workflowTemplates,
  setIsStartCropOpen,
  setPlotName,
  setProofNote,
  setSelectedCrop,
  setStartDate,
  onPickPhoto,
  onStartCrop,
  onSubmitProof,
  onCancelProof,
  onOpenProof
}: {
  activityLoadError: string;
  isStartCropOpen: boolean;
  plotName: string;
  proofError: string;
  proofNote: string;
  proofPhotoUri: string | null;
  runningCycles: CropCycle[];
  savedProofs: ProofSubmission[];
  selectedCrop: string;
  selectedProof: SelectedProof | null;
  startCropError: string;
  startDate: string;
  workflowTemplates: CropTemplate[];
  setIsStartCropOpen: (isOpen: boolean) => void;
  setPlotName: (value: string) => void;
  setProofNote: (value: string) => void;
  setSelectedCrop: (value: string) => void;
  setStartDate: (value: string) => void;
  onPickPhoto: () => void;
  onStartCrop: () => void;
  onSubmitProof: () => void;
  onCancelProof: () => void;
  onOpenProof: (cycle: CropCycle, step: CropStep) => void;
}) {
  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={styles.sectionHeaderText}>
          <Text style={styles.pageTitle}>Assigned activities</Text>
          <Text style={styles.pageCopy}>
            Follow each assigned workflow step and mark proof done with a photo and
            note.
          </Text>
        </View>
        <Pressable
          style={styles.primaryButton}
          onPress={() => setIsStartCropOpen(!isStartCropOpen)}
        >
          <Text style={styles.primaryButtonText}>
            {isStartCropOpen ? "Close" : "New activity"}
          </Text>
        </Pressable>
      </View>

      {activityLoadError ? (
        <View style={styles.errorCard}>
          <Text style={styles.errorText}>{activityLoadError}</Text>
        </View>
      ) : null}

      {isStartCropOpen ? (
        <StartCropForm
          plotName={plotName}
          selectedCrop={selectedCrop}
          startCropError={startCropError}
          startDate={startDate}
          workflowTemplates={workflowTemplates}
          setPlotName={setPlotName}
          setSelectedCrop={setSelectedCrop}
          setStartDate={setStartDate}
          onStartCrop={onStartCrop}
        />
      ) : null}

      {runningCycles.length ? (
        runningCycles.map((cycle) => (
          <View key={cycle.id} style={styles.cycleCard}>
            <View style={styles.cardHeader}>
              <View style={styles.cardHeaderText}>
                <Text style={styles.cardTitle}>{cycle.crop}</Text>
                <Text style={styles.cardDescription}>
                  {cycle.plot} - {cycle.startedOn} to {cycle.expectedHarvest}
                </Text>
              </View>
              <StatusBadge label={`${cycle.progress}%`} tone="neutral" />
            </View>

            <View style={styles.progressTrack}>
              <View style={[styles.progressFill, { width: `${cycle.progress}%` }]} />
            </View>

            <View style={styles.stepList}>
              {cycle.steps.map((step) => {
                const proof = findStepProof(savedProofs, cycle.id, step.id);
                const canEditProof = proof ? isProofEditable(proof) : false;
                const canSubmit = step.status === "next" || Boolean(proof);

                return (
                  <View key={step.id} style={styles.stepBlock}>
                    <View style={styles.stepRow}>
                      <View style={styles.stepText}>
                        <Text style={styles.stepTitle}>{step.title}</Text>
                        <Text style={styles.stepMeta}>{step.due}</Text>
                      </View>
                      <StatusBadge
                        label={
                          proof || step.status === "done"
                            ? "Done"
                            : step.status === "next"
                              ? "Needs proof"
                              : "Upcoming"
                        }
                        tone={
                          proof || step.status === "done"
                            ? "good"
                            : step.status === "next"
                              ? "warning"
                              : "neutral"
                        }
                      />
                    </View>

                    {proof ? <SelfReview proof={proof} /> : null}

                    {canSubmit && (!proof || canEditProof) ? (
                      <View style={styles.stepActions}>
                        <Pressable
                          style={proof ? styles.secondaryButton : styles.primaryButton}
                          onPress={() => onOpenProof(cycle, step)}
                        >
                          <Text
                            style={
                              proof
                                ? styles.secondaryButtonText
                                : styles.primaryButtonText
                            }
                          >
                            {proof ? "Edit proof" : "Submit proof"}
                          </Text>
                        </Pressable>
                      </View>
                    ) : null}

                    {selectedProof?.cycleId === cycle.id &&
                    selectedProof.stepId === step.id ? (
                      <ProofForm
                        proofError={proofError}
                        proofNote={proofNote}
                        proofPhotoUri={proofPhotoUri}
                        selectedProof={selectedProof}
                        setProofNote={setProofNote}
                        onCancel={onCancelProof}
                        onPickPhoto={onPickPhoto}
                        onSubmit={onSubmitProof}
                      />
                    ) : null}
                  </View>
                );
              })}
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No assigned activities yet. Assigned workflows will appear here when work
            begins.
          </Text>
        </View>
      )}
    </View>
  );
}

function StartCropForm({
  plotName,
  selectedCrop,
  startCropError,
  startDate,
  workflowTemplates,
  setPlotName,
  setSelectedCrop,
  setStartDate,
  onStartCrop
}: {
  plotName: string;
  selectedCrop: string;
  startCropError: string;
  startDate: string;
  workflowTemplates: CropTemplate[];
  setPlotName: (value: string) => void;
  setSelectedCrop: (value: string) => void;
  setStartDate: (value: string) => void;
  onStartCrop: () => void;
}) {
  return (
    <View style={styles.formCard}>
      <Text style={styles.cardTitle}>Start activity</Text>
      <Text style={styles.inputLabel}>Workflow</Text>
      {workflowTemplates.length ? (
        <View style={styles.choiceRow}>
          {workflowTemplates.map((template) => (
            <Pressable
              key={template.id ?? template.crop}
              style={[
                styles.choiceButton,
                selectedCrop === template.crop && styles.choiceButtonActive
              ]}
              onPress={() => setSelectedCrop(template.crop)}
            >
              <Text
                style={[
                  styles.choiceButtonText,
                  selectedCrop === template.crop && styles.choiceButtonTextActive
                ]}
              >
                {template.crop}
              </Text>
              <Text style={styles.choiceButtonMeta}>{template.durationDays} days</Text>
            </Pressable>
          ))}
        </View>
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No workflow is configured yet. An admin must add the workflow and task list
            before farmers can start work.
          </Text>
        </View>
      )}

      <Text style={styles.inputLabel}>Unit or plot name</Text>
      <TextInput
        onChangeText={setPlotName}
        placeholder="Example: Plot D"
        style={styles.textInput}
        value={plotName}
      />

      <Text style={styles.inputLabel}>Start date</Text>
      <TextInput
        onChangeText={setStartDate}
        placeholder="YYYY-MM-DD"
        style={styles.textInput}
        value={startDate}
      />

      {startCropError ? <Text style={styles.errorText}>{startCropError}</Text> : null}

      <View style={styles.formActions}>
        <Pressable style={styles.primaryButton} onPress={onStartCrop}>
          <Text style={styles.primaryButtonText}>Start activity</Text>
        </Pressable>
      </View>
    </View>
  );
}

function DashboardView({
  summary,
  runningCycles
}: {
  summary: Array<{ label: string; value: string }>;
  runningCycles: CropCycle[];
}) {
  return (
    <View style={styles.section}>
      <View>
        <Text style={styles.pageTitle}>Dashboard summary</Text>
        <Text style={styles.pageCopy}>
          A simple overview of current activity work and proof completion.
        </Text>
      </View>

      <View style={styles.statsGrid}>
        {summary.map((item) => (
          <View key={item.label} style={styles.statCard}>
            <Text style={styles.statValue}>{item.value}</Text>
            <Text style={styles.statLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      <Text style={styles.sectionTitle}>Running activities</Text>
      {runningCycles.length ? (
        runningCycles.map((cycle) => (
          <View key={cycle.id} style={styles.cycleCard}>
            <View style={styles.cardHeader}>
              <View style={styles.cardHeaderText}>
                <Text style={styles.cardTitle}>{cycle.crop}</Text>
                <Text style={styles.cardDescription}>
                  {cycle.plot} - harvest by {cycle.expectedHarvest}
                </Text>
              </View>
              <StatusBadge label={`${cycle.progress}% done`} />
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No running activities are available yet.
          </Text>
        </View>
      )}
    </View>
  );
}

function ProfileView({
  profileError,
  profileFields
}: {
  profileError: string;
  profileFields: ProfileField[];
}) {
  return (
    <View style={styles.section}>
      <View>
        <Text style={styles.pageTitle}>Farmer profile</Text>
        <Text style={styles.pageCopy}>
          Farmer information used for activity records and reports.
        </Text>
      </View>
      {profileError ? <Text style={styles.errorText}>{profileError}</Text> : null}
      <View style={styles.profileGrid}>
        {profileFields.map((field) => (
          <View key={field.label} style={styles.profileField}>
            <Text style={styles.profileLabel}>{field.label}</Text>
            <Text style={styles.profileValue}>{field.value}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

function HistoryView({ completedCycles }: { completedCycles: CropCycle[] }) {
  return (
    <View style={styles.section}>
      <View>
        <Text style={styles.pageTitle}>History</Text>
        <Text style={styles.pageCopy}>
          Completed activities live here. Active proof remains inside the running
          activity until it is finished.
        </Text>
      </View>
      {completedCycles.length ? (
        completedCycles.map((cycle) => (
          <View key={cycle.id} style={styles.cycleCard}>
            <View style={styles.cardHeader}>
              <View style={styles.cardHeaderText}>
                <Text style={styles.cardTitle}>{cycle.crop}</Text>
                <Text style={styles.cardDescription}>
                  Completed on {cycle.expectedHarvest}
                </Text>
              </View>
              <StatusBadge label="Completed" tone="good" />
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            Completed activities will appear here.
          </Text>
        </View>
      )}
    </View>
  );
}

function SelfReview({ proof }: { proof: ProofSubmission }) {
  const canEditProof = isProofEditable(proof);

  return (
    <View style={styles.reviewBox}>
      <View style={styles.reviewText}>
        <Text style={styles.reviewTitle}>Self review</Text>
        <Text style={styles.cardDescription}>Submitted {proof.submittedOn}</Text>
        <Text style={styles.cardDescription}>
          {canEditProof
            ? "You can edit this proof for 24 hours."
            : "Proof editing window has closed."}
        </Text>
        {proof.note ? <Text style={styles.cardDescription}>{proof.note}</Text> : null}
      </View>
      {proof.photoUri ? (
        <Image source={{ uri: proof.photoUri }} style={styles.reviewImage} />
      ) : null}
    </View>
  );
}

function ProofForm({
  proofError,
  proofNote,
  proofPhotoUri,
  selectedProof,
  setProofNote,
  onCancel,
  onPickPhoto,
  onSubmit
}: {
  proofError: string;
  proofNote: string;
  proofPhotoUri: string | null;
  selectedProof: SelectedProof;
  setProofNote: (note: string) => void;
  onCancel: () => void;
  onPickPhoto: () => void;
  onSubmit: () => void;
}) {
  return (
    <View style={styles.formCard}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderText}>
          <Text style={styles.cardTitle}>Submit proof</Text>
          <Text style={styles.cardDescription}>
            {selectedProof.crop} - {selectedProof.plot} - {selectedProof.action}
          </Text>
        </View>
        <Pressable style={styles.closeButton} onPress={onCancel}>
          <Text style={styles.closeButtonText}>Cancel</Text>
        </Pressable>
      </View>

      <Text style={styles.inputLabel}>Action note</Text>
      <TextInput
        multiline
        numberOfLines={4}
        onChangeText={setProofNote}
        placeholder="Example: Irrigation completed for 40 minutes using drip line."
        style={styles.noteInput}
        textAlignVertical="top"
        value={proofNote}
      />

      <Text style={styles.inputLabel}>Proof photo</Text>
      {proofPhotoUri ? (
        <Image source={{ uri: proofPhotoUri }} style={styles.proofImage} />
      ) : (
        <View style={styles.photoPlaceholder}>
          <Text style={styles.photoPlaceholderText}>No photo selected</Text>
        </View>
      )}

      {proofError ? <Text style={styles.errorText}>{proofError}</Text> : null}

      <View style={styles.formActions}>
        <Pressable style={styles.secondaryButton} onPress={onPickPhoto}>
          <Text style={styles.secondaryButtonText}>Choose photo</Text>
        </Pressable>
        <Pressable style={styles.primaryButton} onPress={onSubmit}>
          <Text style={styles.primaryButtonText}>Mark done</Text>
        </Pressable>
      </View>
    </View>
  );
}

function activateNextPendingStep(steps: CropStep[]) {
  return activateNextPendingTask(steps);
}

function findStepProof(proofs: ProofSubmission[], cycleId: string, stepId: string) {
  return findEvidenceForTask(proofs, cycleId, stepId);
}

function isCycleComplete(cycle: CropCycle, proofs: ProofSubmission[]) {
  return isActivityComplete({ id: cycle.id, steps: cycle.steps }, proofs);
}

function isProofEditable(proof: ProofSubmission) {
  return isEvidenceEditable(proof);
}

function getProfileValue(
  profileFields: ProfileField[],
  label: string,
  fallback: string
) {
  const value = profileFields.find((field) => field.label === label)?.value;
  return value && value !== "Not set" ? value : fallback;
}

function addDays(date: Date, days: number) {
  const nextDate = new Date(date);
  nextDate.setDate(nextDate.getDate() + days);
  return nextDate;
}

function formatDisplayDate(date: Date) {
  return date.toLocaleDateString(appConfig.defaultLocale, {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function toInputDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#f4f7fb"
  },
  topBar: {
    alignItems: "center",
    backgroundColor: "#ffffff",
    borderBottomColor: "#d9e4ea",
    borderBottomWidth: 1,
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 18,
    paddingVertical: 14
  },
  appTitle: {
    color: "#172126",
    fontSize: 20,
    fontWeight: "800"
  },
  appSubtitle: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 3
  },
  logoutButton: {
    zIndex: 2,
    minHeight: 40,
    minWidth: 82,
    alignItems: "center",
    justifyContent: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12
  },
  logoutButtonText: {
    color: "#1f6f73",
    fontSize: 14,
    fontWeight: "800"
  },
  navRow: {
    backgroundColor: "#e8eef2",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 4,
    padding: 6
  },
  navButton: {
    alignItems: "center",
    borderRadius: 6,
    flexGrow: 1,
    flexBasis: 110,
    minHeight: 40,
    justifyContent: "center"
  },
  navButtonActive: {
    backgroundColor: "#ffffff"
  },
  navButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  navButtonTextActive: {
    color: "#172126"
  },
  scrollContent: {
    flexGrow: 1
  },
  shell: {
    width: "100%",
    maxWidth: 920,
    alignSelf: "center",
    paddingHorizontal: 18,
    paddingVertical: 20
  },
  section: {
    gap: 14
  },
  sectionHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  sectionHeaderText: {
    flex: 1
  },
  pageTitle: {
    color: "#172126",
    fontSize: 26,
    fontWeight: "800",
    marginBottom: 6
  },
  pageCopy: {
    color: "#53666f",
    fontSize: 15,
    lineHeight: 22
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  statsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
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
  cycleCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  },
  errorCard: {
    backgroundColor: "#fff5f5",
    borderColor: "#f0b8b8",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  cardHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  cardHeaderText: {
    flex: 1
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
  progressTrack: {
    backgroundColor: "#edf2f4",
    borderRadius: 8,
    height: 10,
    overflow: "hidden"
  },
  progressFill: {
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    height: 10
  },
  stepList: {
    gap: 12
  },
  stepBlock: {
    borderTopColor: "#eef3f5",
    borderTopWidth: 1,
    gap: 10,
    paddingTop: 12
  },
  stepRow: {
    alignItems: "center",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  stepText: {
    flex: 1
  },
  stepTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800",
    marginBottom: 3
  },
  stepMeta: {
    color: "#6d7f88",
    fontSize: 13,
    fontWeight: "700"
  },
  stepActions: {
    alignItems: "flex-start"
  },
  reviewBox: {
    backgroundColor: "#f7fafb",
    borderColor: "#dce7ec",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    padding: 12
  },
  reviewText: {
    flex: 1,
    gap: 4
  },
  reviewTitle: {
    color: "#1d7a47",
    fontSize: 14,
    fontWeight: "800"
  },
  reviewImage: {
    backgroundColor: "#edf2f4",
    borderRadius: 8,
    height: 76,
    width: 92
  },
  formCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
  },
  choiceRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  choiceButton: {
    minWidth: 120,
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  choiceButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  choiceButtonText: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  choiceButtonTextActive: {
    color: "#1f6f73"
  },
  choiceButtonMeta: {
    color: "#6d7f88",
    fontSize: 12,
    fontWeight: "700",
    marginTop: 4
  },
  inputLabel: {
    color: "#24343b",
    fontSize: 14,
    fontWeight: "800"
  },
  textInput: {
    minHeight: 48,
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    backgroundColor: "#ffffff",
    color: "#172126",
    fontSize: 15,
    paddingHorizontal: 12
  },
  noteInput: {
    minHeight: 96,
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    backgroundColor: "#ffffff",
    color: "#172126",
    fontSize: 15,
    lineHeight: 21,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  photoPlaceholder: {
    alignItems: "center",
    backgroundColor: "#f4f7fb",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderStyle: "dashed",
    borderWidth: 1,
    height: 170,
    justifyContent: "center"
  },
  photoPlaceholderText: {
    color: "#6d7f88",
    fontSize: 14,
    fontWeight: "800"
  },
  proofImage: {
    backgroundColor: "#edf2f4",
    borderRadius: 8,
    height: 190,
    width: "100%"
  },
  errorText: {
    color: "#b42318",
    fontSize: 14,
    fontWeight: "700"
  },
  formActions: {
    flexDirection: "row",
    gap: 10,
    justifyContent: "flex-end"
  },
  primaryButton: {
    minHeight: 42,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  secondaryButton: {
    minHeight: 42,
    alignItems: "center",
    justifyContent: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 14
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 14,
    fontWeight: "800"
  },
  closeButton: {
    minHeight: 36,
    alignItems: "center",
    justifyContent: "center",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12
  },
  closeButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  profileGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  profileField: {
    minWidth: 180,
    flex: 1,
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  profileLabel: {
    color: "#6d7f88",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 6,
    textTransform: "uppercase"
  },
  profileValue: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  modalBackdrop: {
    alignItems: "center",
    backgroundColor: "rgba(17, 30, 36, 0.42)",
    flex: 1,
    justifyContent: "center",
    padding: 20
  },
  modalCard: {
    width: "100%",
    maxWidth: 420,
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderRadius: 8,
    gap: 12,
    padding: 22
  },
  modalEyebrow: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800",
    textTransform: "uppercase"
  },
  modalTitle: {
    color: "#172126",
    fontSize: 24,
    fontWeight: "800"
  },
  modalCopy: {
    color: "#53666f",
    fontSize: 15,
    lineHeight: 22
  }
});
