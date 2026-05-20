import * as DocumentPicker from "expo-document-picker";
import { ReactNode, useEffect, useMemo, useState } from "react";
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View
} from "react-native";

import { getErrorMessage } from "../../../core/errors/AppError";
import { isClientModuleEnabled } from "../../../modules";
import { StatusBadge } from "../../../ui/StatusBadge";
import { FarmerIdentityFields } from "../../../shared/farmers/FarmerIdentityFields";
import {
  emptyFarmerIdentityInput,
  FarmerIdentityInput,
  normalizeFarmerIdentityInput,
  validateFarmerIdentityInput
} from "../../../shared/farmers/farmerIdentity";
import {
  CARBON_AADHAAR_STATUSES,
  CARBON_BANK_STATUSES,
  CARBON_DOCUMENT_STATUSES,
  CARBON_PARTICIPANT_TYPES,
  CARBON_RECORD_STATUSES,
  CARBON_TILLAGE_STATUSES,
  CarbonActivityCategoryRecord,
  CarbonActivityInput,
  CarbonActivityRecord,
  CarbonFarmPlotInput,
  CarbonFarmPlotRecord,
  CarbonProfileInput,
  CarbonProfileRecord,
  CarbonSoilProfileInput,
  CarbonSoilProfileRecord,
  createCarbonActivity,
  createCarbonFarmPlot,
  createCarbonProfile,
  createCarbonSoilProfile,
  listCarbonActivities,
  listCarbonActivityCategories,
  listCarbonFarmPlots,
  listCarbonProfiles,
  listCarbonSoilProfiles,
  updateCarbonActivity,
  updateCarbonFarmPlot,
  updateCarbonProfile,
  updateCarbonSoilProfile,
  uploadCarbonSoilReport
} from "../data/carbonProfileStore";

type CarbonProfileAdminPanelProps = {
  onProfilesLoaded?: (profiles: CarbonProfileRecord[]) => void;
};

type ActiveEnrollmentForm = "activity" | "profile" | "plot" | "soil" | null;
type CarbonParticipantOption = (typeof CARBON_PARTICIPANT_TYPES)[number];

export function CarbonProfileAdminPanel({
  onProfilesLoaded
}: CarbonProfileAdminPanelProps) {
  const [activeForm, setActiveForm] = useState<ActiveEnrollmentForm>(null);
  const [activities, setActivities] = useState<CarbonActivityRecord[]>([]);
  const [activityCategories, setActivityCategories] = useState<
    CarbonActivityCategoryRecord[]
  >([]);
  const [editingActivity, setEditingActivity] = useState<CarbonActivityRecord | null>(
    null
  );
  const [editingPlot, setEditingPlot] = useState<CarbonFarmPlotRecord | null>(null);
  const [editingProfile, setEditingProfile] = useState<CarbonProfileRecord | null>(
    null
  );
  const [editingSoilProfile, setEditingSoilProfile] =
    useState<CarbonSoilProfileRecord | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [plots, setPlots] = useState<CarbonFarmPlotRecord[]>([]);
  const [profiles, setProfiles] = useState<CarbonProfileRecord[]>([]);
  const [savingKey, setSavingKey] = useState<string | null>(null);
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);
  const [soilProfiles, setSoilProfiles] = useState<CarbonSoilProfileRecord[]>([]);
  const [uploadingReportId, setUploadingReportId] = useState<string | null>(null);

  const selectedProfile = useMemo(
    () => profiles.find((profile) => profile.id === selectedProfileId) ?? null,
    [profiles, selectedProfileId]
  );

  const activeArea = useMemo(
    () =>
      plots
        .filter((plot) => plot.status === "ACTIVE")
        .reduce((total, plot) => total + plot.areaAcres, 0),
    [plots]
  );

  useEffect(() => {
    loadProfiles();
  }, []);

  useEffect(() => {
    if (selectedProfileId) {
      loadProfileDetails(selectedProfileId);
    } else {
      setActivities([]);
      setPlots([]);
      setSoilProfiles([]);
    }
  }, [selectedProfileId]);

  async function loadProfiles() {
    setIsLoading(true);
    setError("");

    try {
      const [nextProfiles, nextCategories] = await Promise.all([
        listCarbonProfiles(),
        listCarbonActivityCategories()
      ]);

      setProfiles(nextProfiles);
      setActivityCategories(nextCategories);
      onProfilesLoaded?.(nextProfiles);
      setSelectedProfileId((current) => current ?? nextProfiles[0]?.id ?? null);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load farmers."));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadProfileDetails(profileId: string) {
    setIsLoading(true);
    setError("");

    try {
      const [nextActivities, nextPlots, nextSoilProfiles] = await Promise.all([
        listCarbonActivities(profileId),
        listCarbonFarmPlots(profileId),
        listCarbonSoilProfiles(profileId)
      ]);

      setActivities(nextActivities);
      setPlots(nextPlots);
      setSoilProfiles(nextSoilProfiles);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load Carbon farm records."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleProfileSubmit(input: CarbonProfileInput) {
    setSavingKey("profile");
    setError("");

    try {
      const saved = editingProfile
        ? await updateCarbonProfile(editingProfile.id, input)
        : await createCarbonProfile(input);
      const nextProfiles = upsertById(profiles, saved);

      setProfiles(nextProfiles);
      onProfilesLoaded?.(nextProfiles);
      setSelectedProfileId(saved.id);
      setEditingProfile(null);
      setActiveForm(null);
      return true;
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Unable to save farmer enrollment."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handlePlotSubmit(input: CarbonFarmPlotInput) {
    if (!selectedProfile) {
      setError("Select a farmer before saving a farm plot.");
      return false;
    }

    setSavingKey("plot");
    setError("");

    try {
      const saved = editingPlot
        ? await updateCarbonFarmPlot(editingPlot.id, input)
        : await createCarbonFarmPlot(selectedProfile.id, input);

      setPlots((current) => upsertById(current, saved));
      setEditingPlot(null);
      setActiveForm(null);
      return true;
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Unable to save Carbon farm plot."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleSoilSubmit(input: CarbonSoilProfileInput) {
    if (!selectedProfile) {
      setError("Select a farmer before saving a soil profile.");
      return false;
    }

    setSavingKey("soil");
    setError("");

    try {
      const saved = editingSoilProfile
        ? await updateCarbonSoilProfile(editingSoilProfile.id, input)
        : await createCarbonSoilProfile(selectedProfile.id, input);

      setSoilProfiles((current) => upsertById(current, saved));
      setEditingSoilProfile(null);
      setActiveForm(null);
      return true;
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Unable to save soil profile."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleActivitySubmit(input: CarbonActivityInput) {
    if (!selectedProfile) {
      setError("Select a farmer before saving an activity.");
      return false;
    }

    setSavingKey("activity");
    setError("");

    try {
      const saved = editingActivity
        ? await updateCarbonActivity(editingActivity.id, input)
        : await createCarbonActivity(selectedProfile.id, input);

      setActivities((current) => upsertById(current, saved));
      setEditingActivity(null);
      setActiveForm(null);
      return true;
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Unable to save Carbon activity."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleSoilReportUpload(soilProfile: CarbonSoilProfileRecord) {
    setUploadingReportId(soilProfile.id);
    setError("");

    try {
      const result = await DocumentPicker.getDocumentAsync({
        copyToCacheDirectory: true,
        multiple: false,
        type: ["application/pdf", "image/*"]
      });

      if (result.canceled) {
        return;
      }

      const asset = result.assets[0];
      const saved = await uploadCarbonSoilReport(soilProfile.id, {
        name: asset.name,
        type: asset.mimeType,
        uri: asset.uri
      });

      setSoilProfiles((current) => upsertById(current, saved));
    } catch (uploadError) {
      setError(getErrorMessage(uploadError, "Unable to upload Carbon soil report."));
    } finally {
      setUploadingReportId(null);
    }
  }

  function handleSelectProfile(profile: CarbonProfileRecord) {
    setSelectedProfileId(profile.id);
    setEditingActivity(null);
    setEditingPlot(null);
    setEditingSoilProfile(null);
    setActiveForm((current) => (current === "profile" ? current : null));
  }

  function openProfileForm(profile: CarbonProfileRecord | null = null) {
    setEditingProfile(profile);
    setEditingActivity(null);
    setEditingPlot(null);
    setEditingSoilProfile(null);
    setActiveForm("profile");
  }

  function openPlotForm(plot: CarbonFarmPlotRecord | null = null) {
    setEditingPlot(plot);
    setEditingActivity(null);
    setEditingProfile(null);
    setEditingSoilProfile(null);
    setActiveForm("plot");
  }

  function openSoilForm(profile: CarbonSoilProfileRecord | null = null) {
    setEditingSoilProfile(profile);
    setEditingActivity(null);
    setEditingProfile(null);
    setEditingPlot(null);
    setActiveForm("soil");
  }

  function openActivityForm(activity: CarbonActivityRecord | null = null) {
    setEditingActivity(activity);
    setEditingProfile(null);
    setEditingPlot(null);
    setEditingSoilProfile(null);
    setActiveForm("activity");
  }

  function closeActiveForm() {
    setActiveForm(null);
    setEditingActivity(null);
    setEditingProfile(null);
    setEditingPlot(null);
    setEditingSoilProfile(null);
  }

  const modalTitle = getEnrollmentModalTitle(
    activeForm,
    Boolean(editingActivity || editingProfile || editingPlot || editingSoilProfile)
  );
  const modalSubtitle =
    activeForm === "profile"
      ? "Capture the participant details needed for carbon enrollment."
      : activeForm === "activity"
        ? "Record a regenerative practice against the selected farmer."
        : selectedProfile
          ? `${selectedProfile.displayName} - ${selectedProfile.carbonIdentityId}`
          : "Select a farmer before adding records.";

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View>
          <Text style={styles.subsectionTitle}>Enrollment workspace</Text>
          <Text style={styles.panelMeta}>
            Select a farmer first, then manage farm plots, soil records, and activities.
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadProfiles}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      <View style={styles.workspaceGrid}>
        <View style={styles.profileColumn}>
          <View style={styles.columnHeader}>
            <View>
              <Text style={styles.columnTitle}>1. Farmers</Text>
              <Text style={styles.panelMeta}>
                {profiles.length} farmer{profiles.length === 1 ? "" : "s"} enrolled
              </Text>
            </View>
            <View style={styles.headerActions}>
              <StatusBadge
                label={`${formatNumber(activeArea)} active ac`}
                tone="neutral"
              />
              <ActionButton
                label={
                  activeForm === "profile" && !editingProfile
                    ? "Adding farmer"
                    : "Add farmer"
                }
                onPress={() => openProfileForm()}
              />
            </View>
          </View>

          <ProfileList
            editingProfileId={editingProfile?.id}
            onEdit={(profile) => openProfileForm(profile)}
            onSelect={handleSelectProfile}
            profiles={profiles}
            selectedProfileId={selectedProfileId}
          />
        </View>

        <View style={styles.detailColumn}>
          <View style={styles.columnHeader}>
            <View>
              <Text style={styles.columnTitle}>2. Selected farmer workspace</Text>
              <Text style={styles.panelMeta}>
                Farm plots, soil profiles, and activities are attached to the selected
                farmer.
              </Text>
            </View>
          </View>

          {selectedProfile ? (
            <>
              <SelectedProfileSummary
                activities={activities}
                plots={plots}
                profile={selectedProfile}
                soilProfiles={soilProfiles}
              />

              <PlotList
                editingPlotId={editingPlot?.id}
                isFormOpen={activeForm === "plot"}
                onAdd={() => openPlotForm()}
                onEdit={(plot) => openPlotForm(plot)}
                plots={plots}
              />

              <SoilProfileList
                editingSoilProfileId={editingSoilProfile?.id}
                isFormOpen={activeForm === "soil"}
                onAdd={() => openSoilForm()}
                onEdit={(soilProfile) => openSoilForm(soilProfile)}
                onUploadReport={handleSoilReportUpload}
                plots={plots}
                soilProfiles={soilProfiles}
                uploadingReportId={uploadingReportId}
              />

              <ActivityList
                activities={activities}
                editingActivityId={editingActivity?.id}
                isFormOpen={activeForm === "activity"}
                onAdd={() => openActivityForm()}
                onEdit={(activity) => openActivityForm(activity)}
              />
            </>
          ) : (
            <View style={styles.emptyStatePanel}>
              <Text style={styles.emptyTitle}>No farmer selected</Text>
              <Text style={styles.emptyText}>
                Select a farmer from the left side to view and manage farm plots, soil
                records, and activities.
              </Text>
            </View>
          )}
        </View>
      </View>

      <EnrollmentFormModal
        subtitle={modalSubtitle}
        title={modalTitle}
        visible={activeForm !== null}
        onClose={closeActiveForm}
      >
        {activeForm === "profile" ? (
          <CarbonProfileForm
            editingProfile={editingProfile}
            isSubmitting={savingKey === "profile"}
            onSubmit={handleProfileSubmit}
          />
        ) : null}
        {activeForm === "plot" ? (
          <CarbonPlotForm
            editingPlot={editingPlot}
            isSubmitting={savingKey === "plot"}
            onSubmit={handlePlotSubmit}
          />
        ) : null}
        {activeForm === "soil" ? (
          <CarbonSoilProfileForm
            editingSoilProfile={editingSoilProfile}
            isSubmitting={savingKey === "soil"}
            onSubmit={handleSoilSubmit}
            plots={plots}
          />
        ) : null}
        {activeForm === "activity" ? (
          <CarbonActivityForm
            activityCategories={activityCategories}
            editingActivity={editingActivity}
            isSubmitting={savingKey === "activity"}
            onSubmit={handleActivitySubmit}
            plots={plots}
          />
        ) : null}
      </EnrollmentFormModal>
    </View>
  );
}

function SelectedProfileSummary({
  activities,
  plots,
  profile,
  soilProfiles
}: {
  activities: CarbonActivityRecord[];
  plots: CarbonFarmPlotRecord[];
  profile: CarbonProfileRecord;
  soilProfiles: CarbonSoilProfileRecord[];
}) {
  const profileArea =
    profile.totalLandHoldingAcres ??
    plots.reduce((total, plot) => total + plot.areaAcres, 0);

  return (
    <View style={styles.selectedHeader}>
      <View style={styles.rowText}>
        <Text style={styles.selectedEyebrow}>Selected farmer</Text>
        <Text style={styles.selectedTitle}>{profile.displayName}</Text>
        <Text style={styles.panelMeta}>
          {profile.carbonIdentityId} -{" "}
          {[profile.village, profile.taluka, profile.districtName]
            .filter(Boolean)
            .join(", ") || "Location not set"}
        </Text>
        <Text style={styles.panelMeta}>
          {[profile.memberNumber, profile.username].filter(Boolean).join(" - ") ||
            "Farmer login not linked"}
        </Text>
        <View style={styles.summaryPillRow}>
          <View style={styles.summaryPill}>
            <Text style={styles.summaryPillValue}>{formatNumber(profileArea)} ac</Text>
            <Text style={styles.summaryPillLabel}>Land</Text>
          </View>
          <View style={styles.summaryPill}>
            <Text style={styles.summaryPillValue}>{plots.length}</Text>
            <Text style={styles.summaryPillLabel}>Plots</Text>
          </View>
          <View style={styles.summaryPill}>
            <Text style={styles.summaryPillValue}>{soilProfiles.length}</Text>
            <Text style={styles.summaryPillLabel}>Soil tests</Text>
          </View>
          <View style={styles.summaryPill}>
            <Text style={styles.summaryPillValue}>{activities.length}</Text>
            <Text style={styles.summaryPillLabel}>Activities</Text>
          </View>
        </View>
      </View>
      <StatusBadge label={profile.status} tone="neutral" />
    </View>
  );
}

function getEnrollmentModalTitle(activeForm: ActiveEnrollmentForm, isEditing: boolean) {
  if (activeForm === "profile") {
    return isEditing ? "Edit farmer enrollment" : "Add farmer";
  }

  if (activeForm === "plot") {
    return isEditing ? "Edit farm plot" : "Add farm plot";
  }

  if (activeForm === "soil") {
    return isEditing ? "Edit soil profile" : "Add soil profile";
  }

  if (activeForm === "activity") {
    return isEditing ? "Edit Carbon activity" : "Add Carbon activity";
  }

  return "Enrollment";
}

function EnrollmentFormModal({
  children,
  subtitle,
  title,
  visible,
  onClose
}: {
  children: ReactNode;
  subtitle: string;
  title: string;
  visible: boolean;
  onClose: () => void;
}) {
  return (
    <Modal animationType="fade" transparent visible={visible} onRequestClose={onClose}>
      <View style={styles.modalOverlay}>
        <Pressable
          accessibilityLabel="Close enrollment form"
          accessibilityRole="button"
          style={styles.modalBackdrop}
          onPress={onClose}
        />
        <View style={styles.modalCard}>
          <View style={styles.modalHeader}>
            <View style={styles.modalTitleBlock}>
              <Text style={styles.modalTitle}>{title}</Text>
              <Text style={styles.modalSubtitle}>{subtitle}</Text>
            </View>
            <Pressable
              accessibilityRole="button"
              style={styles.modalCloseButton}
              onPress={onClose}
            >
              <Text style={styles.modalCloseText}>Cancel</Text>
            </Pressable>
          </View>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            style={styles.modalBody}
            contentContainerStyle={styles.modalBodyContent}
          >
            {children}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function CarbonProfileForm({
  editingProfile,
  isSubmitting,
  onSubmit
}: {
  editingProfile: CarbonProfileRecord | null;
  isSubmitting: boolean;
  onSubmit: (input: CarbonProfileInput) => Promise<boolean>;
}) {
  const [aadhaarStatus, setAadhaarStatus] = useState<string>(
    CARBON_AADHAAR_STATUSES[1]
  );
  const [bankStatus, setBankStatus] = useState<string>(CARBON_BANK_STATUSES[0]);
  const [carbonIdentityId, setCarbonIdentityId] = useState("");
  const [coordinatorUserId, setCoordinatorUserId] = useState("");
  const [croppingPattern, setCroppingPattern] = useState("");
  const [documentStatus, setDocumentStatus] = useState<string>(
    CARBON_DOCUMENT_STATUSES[1]
  );
  const [fpoMemberProfileId, setFpoMemberProfileId] = useState("");
  const [gpsLatitude, setGpsLatitude] = useState("");
  const [gpsLongitude, setGpsLongitude] = useState("");
  const [identity, setIdentity] = useState<FarmerIdentityInput>(
    emptyFarmerIdentityInput()
  );
  const [livestockCount, setLivestockCount] = useState("");
  const [localError, setLocalError] = useState("");
  const [participantType, setParticipantType] = useState<CarbonParticipantOption>(
    CARBON_PARTICIPANT_TYPES[0]
  );
  const [status, setStatus] = useState(CARBON_RECORD_STATUSES[0]);
  const [tillageStatus, setTillageStatus] = useState<string>(
    CARBON_TILLAGE_STATUSES[1]
  );
  const [totalLandHoldingAcres, setTotalLandHoldingAcres] = useState("");
  const [userId, setUserId] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(Boolean(editingProfile));
  const showFpoLinking = isClientModuleEnabled("fpo");
  const participantOptions = useMemo<CarbonParticipantOption[]>(
    () =>
      showFpoLinking
        ? CARBON_PARTICIPANT_TYPES
        : CARBON_PARTICIPANT_TYPES.filter((option) => option !== "FPO_FPC"),
    [showFpoLinking]
  );

  useEffect(() => {
    setAadhaarStatus(editingProfile?.aadhaarStatus ?? CARBON_AADHAAR_STATUSES[1]);
    setBankStatus(editingProfile?.bankStatus ?? CARBON_BANK_STATUSES[0]);
    setCarbonIdentityId(editingProfile?.carbonIdentityId ?? "");
    setCoordinatorUserId(editingProfile?.coordinatorUserId ?? "");
    setCroppingPattern(editingProfile?.croppingPattern ?? "");
    setDocumentStatus(editingProfile?.documentStatus ?? CARBON_DOCUMENT_STATUSES[1]);
    setFpoMemberProfileId(editingProfile?.fpoMemberProfileId ?? "");
    setGpsLatitude(toInputNumber(editingProfile?.gpsLatitude));
    setGpsLongitude(toInputNumber(editingProfile?.gpsLongitude));
    setIdentity(
      emptyFarmerIdentityInput({
        aadhaarNumber: editingProfile?.aadhaarNumber ?? "",
        age: toInputNumber(editingProfile?.age),
        alternateMobileNumber: editingProfile?.alternateMobileNumber ?? "",
        displayName: editingProfile?.displayName ?? "",
        districtName: editingProfile?.districtName ?? "",
        farmerCategory: editingProfile?.farmerCategory ?? undefined,
        gender: editingProfile?.gender ?? undefined,
        memberNumber: editingProfile?.memberNumber ?? "",
        mobileNumber: editingProfile?.mobileNumber ?? "",
        stateName: editingProfile?.stateName ?? "Maharashtra",
        taluka: editingProfile?.taluka ?? "",
        username: editingProfile?.username ?? "",
        village: editingProfile?.village ?? ""
      })
    );
    setLivestockCount(toInputNumber(editingProfile?.livestockCount));
    setParticipantType(editingProfile?.participantType ?? CARBON_PARTICIPANT_TYPES[0]);
    setStatus(editingProfile?.status ?? CARBON_RECORD_STATUSES[0]);
    setTillageStatus(editingProfile?.tillageStatus ?? CARBON_TILLAGE_STATUSES[1]);
    setTotalLandHoldingAcres(toInputNumber(editingProfile?.totalLandHoldingAcres));
    setUserId(editingProfile?.userId ?? "");
    setShowAdvanced(Boolean(editingProfile));
    setLocalError("");
  }, [editingProfile]);

  useEffect(() => {
    if (!participantOptions.includes(participantType)) {
      setParticipantType(participantOptions[0]);
    }
  }, [participantOptions, participantType]);

  async function handleSubmit() {
    const normalizedIdentity = normalizeFarmerIdentityInput(identity);
    const hasFpoMemberLink = showFpoLinking && Boolean(fpoMemberProfileId.trim());
    const requireLogin = !editingProfile && !userId.trim() && !hasFpoMemberLink;
    const validationError = validateFarmerIdentityInput(normalizedIdentity, {
      requireLogin
    });
    const input: CarbonProfileInput = {
      aadhaarNumber: normalizedIdentity.aadhaarNumber,
      aadhaarStatus,
      age: normalizedIdentity.age,
      alternateMobileNumber: normalizedIdentity.alternateMobileNumber,
      bankStatus,
      carbonIdentityId,
      coordinatorUserId,
      croppingPattern,
      displayName: normalizedIdentity.displayName,
      districtName: normalizedIdentity.districtName,
      documentStatus,
      farmerCategory: normalizedIdentity.farmerCategory,
      fpoMemberProfileId: showFpoLinking ? fpoMemberProfileId : "",
      gender: normalizedIdentity.gender,
      gpsLatitude,
      gpsLongitude,
      livestockCount,
      memberNumber: normalizedIdentity.memberNumber,
      mobileNumber: normalizedIdentity.mobileNumber,
      participantType,
      password: normalizedIdentity.password,
      stateName: normalizedIdentity.stateName,
      status,
      taluka: normalizedIdentity.taluka,
      tillageStatus,
      totalLandHoldingAcres,
      userId,
      username: normalizedIdentity.username,
      village: normalizedIdentity.village
    };

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    const success = await onSubmit(input);
    if (success && !editingProfile) {
      setCarbonIdentityId("");
      setCoordinatorUserId("");
      setCroppingPattern("");
      setFpoMemberProfileId("");
      setGpsLatitude("");
      setGpsLongitude("");
      setIdentity(emptyFarmerIdentityInput());
      setLivestockCount("");
      setTotalLandHoldingAcres("");
      setUserId("");
      setShowAdvanced(false);
    }
  }

  return (
    <View style={styles.formBlock}>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}

      <FarmerIdentityFields
        includeLogin={!editingProfile}
        value={identity}
        onChange={setIdentity}
      />

      <View style={styles.formGrid}>
        <FormField
          label="Total acres"
          keyboardType="decimal-pad"
          value={totalLandHoldingAcres}
          onChange={setTotalLandHoldingAcres}
        />
        <FormField
          label="Cropping pattern"
          value={croppingPattern}
          onChange={setCroppingPattern}
        />
      </View>

      <AdvancedSection
        isOpen={showAdvanced}
        label="More carbon and link details"
        onToggle={() => setShowAdvanced((current) => !current)}
      >
        <View style={styles.formGrid}>
          <FormField
            label="Carbon ID"
            value={carbonIdentityId}
            onChange={setCarbonIdentityId}
          />
          <FormField
            label="Latitude"
            keyboardType="decimal-pad"
            value={gpsLatitude}
            onChange={setGpsLatitude}
          />
          <FormField
            label="Longitude"
            keyboardType="decimal-pad"
            value={gpsLongitude}
            onChange={setGpsLongitude}
          />
          <FormField
            label="Livestock count"
            keyboardType="number-pad"
            value={livestockCount}
            onChange={setLivestockCount}
          />
          <FormField label="User ID" value={userId} onChange={setUserId} />
          {showFpoLinking ? (
            <FormField
              label="FPO member ID"
              value={fpoMemberProfileId}
              onChange={setFpoMemberProfileId}
            />
          ) : null}
          <FormField
            label="Coordinator ID"
            value={coordinatorUserId}
            onChange={setCoordinatorUserId}
          />
        </View>
        <OptionGroup
          label="Participant"
          options={participantOptions}
          value={participantType}
          onChange={setParticipantType}
        />
        <OptionGroup
          label="Tillage"
          options={CARBON_TILLAGE_STATUSES}
          value={tillageStatus}
          onChange={setTillageStatus}
        />
        <OptionGroup
          label="Bank"
          options={CARBON_BANK_STATUSES}
          value={bankStatus}
          onChange={setBankStatus}
        />
        <OptionGroup
          label="Aadhaar"
          options={CARBON_AADHAAR_STATUSES}
          value={aadhaarStatus}
          onChange={setAadhaarStatus}
        />
        <OptionGroup
          label="Documents"
          options={CARBON_DOCUMENT_STATUSES}
          value={documentStatus}
          onChange={setDocumentStatus}
        />
        <OptionGroup
          label="Status"
          options={CARBON_RECORD_STATUSES}
          value={status}
          onChange={setStatus}
        />
      </AdvancedSection>

      <PrimaryButton
        disabled={isSubmitting}
        label={
          isSubmitting ? "Saving..." : editingProfile ? "Update farmer" : "Add farmer"
        }
        onPress={handleSubmit}
      />
    </View>
  );
}

function CarbonPlotForm({
  editingPlot,
  isSubmitting,
  onSubmit
}: {
  editingPlot: CarbonFarmPlotRecord | null;
  isSubmitting: boolean;
  onSubmit: (input: CarbonFarmPlotInput) => Promise<boolean>;
}) {
  const [areaAcres, setAreaAcres] = useState("");
  const [blockCode, setBlockCode] = useState("");
  const [farmName, setFarmName] = useState("");
  const [irrigationSource, setIrrigationSource] = useState("Drip");
  const [latitude, setLatitude] = useState("");
  const [localError, setLocalError] = useState("");
  const [longitude, setLongitude] = useState("");
  const [plantingDate, setPlantingDate] = useState("");
  const [primaryCrop, setPrimaryCrop] = useState("");
  const [rootstock, setRootstock] = useState("");
  const [rowCount, setRowCount] = useState("");
  const [spacing, setSpacing] = useState("");
  const [status, setStatus] = useState(CARBON_RECORD_STATUSES[0]);
  const [surveyNumber, setSurveyNumber] = useState("");
  const [tillageStatus, setTillageStatus] = useState<string>(
    CARBON_TILLAGE_STATUSES[1]
  );
  const [variety, setVariety] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(Boolean(editingPlot));

  useEffect(() => {
    setAreaAcres(toInputNumber(editingPlot?.areaAcres));
    setBlockCode(editingPlot?.blockCode ?? "");
    setFarmName(editingPlot?.farmName ?? "");
    setIrrigationSource(editingPlot?.irrigationSource ?? "Drip");
    setLatitude(toInputNumber(editingPlot?.latitude));
    setLongitude(toInputNumber(editingPlot?.longitude));
    setPlantingDate(editingPlot?.plantingDate ?? "");
    setPrimaryCrop(editingPlot?.primaryCrop ?? "");
    setRootstock(editingPlot?.rootstock ?? "");
    setRowCount(toInputNumber(editingPlot?.rowCount));
    setSpacing(editingPlot?.spacing ?? "");
    setStatus(editingPlot?.status ?? CARBON_RECORD_STATUSES[0]);
    setSurveyNumber(editingPlot?.surveyNumber ?? "");
    setTillageStatus(editingPlot?.tillageStatus ?? CARBON_TILLAGE_STATUSES[1]);
    setVariety(editingPlot?.variety ?? "");
    setShowAdvanced(Boolean(editingPlot));
    setLocalError("");
  }, [editingPlot]);

  async function handleSubmit() {
    if (
      !farmName.trim() ||
      !areaAcres.trim() ||
      !latitude.trim() ||
      !longitude.trim()
    ) {
      setLocalError("Farm name, area, latitude, and longitude are required.");
      return;
    }

    const success = await onSubmit({
      areaAcres,
      blockCode,
      farmName,
      irrigationSource,
      latitude,
      longitude,
      plantingDate,
      primaryCrop,
      rootstock,
      rowCount,
      spacing,
      status,
      surveyNumber,
      tillageStatus,
      variety
    });

    if (success && !editingPlot) {
      setAreaAcres("");
      setBlockCode("");
      setFarmName("");
      setLatitude("");
      setLongitude("");
      setPlantingDate("");
      setPrimaryCrop("");
      setRootstock("");
      setRowCount("");
      setSpacing("");
      setSurveyNumber("");
      setVariety("");
      setShowAdvanced(false);
    }
  }

  return (
    <View style={styles.formBlock}>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <View style={styles.formGrid}>
        <FormField label="Farm name" value={farmName} onChange={setFarmName} />
        <FormField label="Survey no." value={surveyNumber} onChange={setSurveyNumber} />
        <FormField
          label="Area acres"
          keyboardType="decimal-pad"
          value={areaAcres}
          onChange={setAreaAcres}
        />
        <FormField
          label="Latitude"
          keyboardType="decimal-pad"
          value={latitude}
          onChange={setLatitude}
        />
        <FormField
          label="Longitude"
          keyboardType="decimal-pad"
          value={longitude}
          onChange={setLongitude}
        />
        <FormField label="Primary crop" value={primaryCrop} onChange={setPrimaryCrop} />
        <FormField label="Variety" value={variety} onChange={setVariety} />
        <FormField label="Block code" value={blockCode} onChange={setBlockCode} />
      </View>

      <AdvancedSection
        isOpen={showAdvanced}
        label="More vineyard details"
        onToggle={() => setShowAdvanced((current) => !current)}
      >
        <View style={styles.formGrid}>
          <FormField
            label="Irrigation"
            value={irrigationSource}
            onChange={setIrrigationSource}
          />
          <FormField label="Rootstock" value={rootstock} onChange={setRootstock} />
          <FormField
            label="Planting date"
            value={plantingDate}
            onChange={setPlantingDate}
          />
          <FormField label="Spacing" value={spacing} onChange={setSpacing} />
          <FormField
            label="Row count"
            keyboardType="number-pad"
            value={rowCount}
            onChange={setRowCount}
          />
        </View>
        <OptionGroup
          label="Tillage"
          options={CARBON_TILLAGE_STATUSES}
          value={tillageStatus}
          onChange={setTillageStatus}
        />
        <OptionGroup
          label="Status"
          options={CARBON_RECORD_STATUSES}
          value={status}
          onChange={setStatus}
        />
      </AdvancedSection>
      <PrimaryButton
        disabled={isSubmitting}
        label={isSubmitting ? "Saving..." : editingPlot ? "Update plot" : "Add plot"}
        onPress={handleSubmit}
      />
    </View>
  );
}

function CarbonSoilProfileForm({
  editingSoilProfile,
  isSubmitting,
  onSubmit,
  plots
}: {
  editingSoilProfile: CarbonSoilProfileRecord | null;
  isSubmitting: boolean;
  onSubmit: (input: CarbonSoilProfileInput) => Promise<boolean>;
  plots: CarbonFarmPlotRecord[];
}) {
  const [bulkDensityGmCm3, setBulkDensityGmCm3] = useState("");
  const [carbonFarmPlotId, setCarbonFarmPlotId] = useState("");
  const [electricalConductivity, setElectricalConductivity] = useState("");
  const [labName, setLabName] = useState("");
  const [nitrogenKgHa, setNitrogenKgHa] = useState("");
  const [ph, setPh] = useState("");
  const [phosphorusKgHa, setPhosphorusKgHa] = useState("");
  const [potassiumKgHa, setPotassiumKgHa] = useState("");
  const [reportContentType, setReportContentType] = useState("application/pdf");
  const [reportFileName, setReportFileName] = useState("");
  const [reportStorageKey, setReportStorageKey] = useState("");
  const [reportUrl, setReportUrl] = useState("");
  const [soilOrganicCarbonPercent, setSoilOrganicCarbonPercent] = useState("");
  const [status, setStatus] = useState(CARBON_RECORD_STATUSES[0]);
  const [testDate, setTestDate] = useState("");
  const [texture, setTexture] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(Boolean(editingSoilProfile));

  useEffect(() => {
    setBulkDensityGmCm3(toInputNumber(editingSoilProfile?.bulkDensityGmCm3));
    setCarbonFarmPlotId(editingSoilProfile?.carbonFarmPlotId ?? "");
    setElectricalConductivity(
      toInputNumber(editingSoilProfile?.electricalConductivity)
    );
    setLabName(editingSoilProfile?.labName ?? "");
    setNitrogenKgHa(toInputNumber(editingSoilProfile?.nitrogenKgHa));
    setPh(toInputNumber(editingSoilProfile?.ph));
    setPhosphorusKgHa(toInputNumber(editingSoilProfile?.phosphorusKgHa));
    setPotassiumKgHa(toInputNumber(editingSoilProfile?.potassiumKgHa));
    setReportContentType(editingSoilProfile?.reportContentType ?? "application/pdf");
    setReportFileName(editingSoilProfile?.reportFileName ?? "");
    setReportStorageKey(editingSoilProfile?.reportStorageKey ?? "");
    setReportUrl(editingSoilProfile?.reportUrl ?? "");
    setSoilOrganicCarbonPercent(
      toInputNumber(editingSoilProfile?.soilOrganicCarbonPercent)
    );
    setStatus(editingSoilProfile?.status ?? CARBON_RECORD_STATUSES[0]);
    setTestDate(editingSoilProfile?.testDate ?? "");
    setTexture(editingSoilProfile?.texture ?? "");
    setShowAdvanced(Boolean(editingSoilProfile));
  }, [editingSoilProfile]);

  async function handleSubmit() {
    const success = await onSubmit({
      bulkDensityGmCm3,
      carbonFarmPlotId,
      electricalConductivity,
      labName,
      nitrogenKgHa,
      ph,
      phosphorusKgHa,
      potassiumKgHa,
      reportContentType,
      reportFileName,
      reportStorageKey,
      reportUrl,
      soilOrganicCarbonPercent,
      status,
      testDate,
      texture
    });

    if (success && !editingSoilProfile) {
      setBulkDensityGmCm3("");
      setCarbonFarmPlotId("");
      setElectricalConductivity("");
      setLabName("");
      setNitrogenKgHa("");
      setPh("");
      setPhosphorusKgHa("");
      setPotassiumKgHa("");
      setReportFileName("");
      setReportStorageKey("");
      setReportUrl("");
      setSoilOrganicCarbonPercent("");
      setTestDate("");
      setTexture("");
      setShowAdvanced(false);
    }
  }

  return (
    <View style={styles.formBlock}>
      {plots.length ? (
        <OptionGroup
          label="Plot"
          options={["", ...plots.map((plot) => plot.id)]}
          value={carbonFarmPlotId}
          renderLabel={(value) =>
            value
              ? (plots.find((plot) => plot.id === value)?.farmName ?? value)
              : "No plot link"
          }
          onChange={setCarbonFarmPlotId}
        />
      ) : null}

      <View style={styles.formGrid}>
        <FormField label="Test date" value={testDate} onChange={setTestDate} />
        <FormField
          label="SOC %"
          keyboardType="decimal-pad"
          value={soilOrganicCarbonPercent}
          onChange={setSoilOrganicCarbonPercent}
        />
        <FormField label="pH" keyboardType="decimal-pad" value={ph} onChange={setPh} />
        <FormField
          label="N kg/ha"
          keyboardType="decimal-pad"
          value={nitrogenKgHa}
          onChange={setNitrogenKgHa}
        />
        <FormField
          label="P kg/ha"
          keyboardType="decimal-pad"
          value={phosphorusKgHa}
          onChange={setPhosphorusKgHa}
        />
        <FormField
          label="K kg/ha"
          keyboardType="decimal-pad"
          value={potassiumKgHa}
          onChange={setPotassiumKgHa}
        />
      </View>

      <AdvancedSection
        isOpen={showAdvanced}
        label="More soil report details"
        onToggle={() => setShowAdvanced((current) => !current)}
      >
        <View style={styles.formGrid}>
          <FormField label="Lab name" value={labName} onChange={setLabName} />
          <FormField
            label="EC"
            keyboardType="decimal-pad"
            value={electricalConductivity}
            onChange={setElectricalConductivity}
          />
          <FormField
            label="Bulk density"
            keyboardType="decimal-pad"
            value={bulkDensityGmCm3}
            onChange={setBulkDensityGmCm3}
          />
          <FormField label="Texture" value={texture} onChange={setTexture} />
          <FormField
            label="Report filename"
            value={reportFileName}
            onChange={setReportFileName}
          />
          <FormField
            label="External report URL"
            value={reportUrl}
            onChange={setReportUrl}
          />
        </View>
        <OptionGroup
          label="Status"
          options={CARBON_RECORD_STATUSES}
          value={status}
          onChange={setStatus}
        />
      </AdvancedSection>
      <PrimaryButton
        disabled={isSubmitting}
        label={
          isSubmitting
            ? "Saving..."
            : editingSoilProfile
              ? "Update soil profile"
              : "Add soil profile"
        }
        onPress={handleSubmit}
      />
    </View>
  );
}

function CarbonActivityForm({
  activityCategories,
  editingActivity,
  isSubmitting,
  onSubmit,
  plots
}: {
  activityCategories: CarbonActivityCategoryRecord[];
  editingActivity: CarbonActivityRecord | null;
  isSubmitting: boolean;
  onSubmit: (input: CarbonActivityInput) => Promise<boolean>;
  plots: CarbonFarmPlotRecord[];
}) {
  const [activityDate, setActivityDate] = useState(toInputDate(new Date()));
  const [carbonFarmPlotId, setCarbonFarmPlotId] = useState("");
  const [categoryId, setCategoryId] = useState(activityCategories[0]?.id ?? "");
  const [cropName, setCropName] = useState("");
  const [inputUsed, setInputUsed] = useState("");
  const [localError, setLocalError] = useState("");
  const [quantityUnit, setQuantityUnit] = useState("");
  const [quantityValue, setQuantityValue] = useState("");
  const [remarks, setRemarks] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(Boolean(editingActivity));
  const [status, setStatus] = useState(CARBON_RECORD_STATUSES[0]);

  useEffect(() => {
    setActivityDate(editingActivity?.activityDate ?? toInputDate(new Date()));
    setCarbonFarmPlotId(editingActivity?.carbonFarmPlotId ?? "");
    setCategoryId(editingActivity?.categoryId ?? activityCategories[0]?.id ?? "");
    setCropName(editingActivity?.cropName ?? "");
    setInputUsed(editingActivity?.inputUsed ?? "");
    setQuantityUnit(editingActivity?.quantityUnit ?? "");
    setQuantityValue(toInputNumber(editingActivity?.quantityValue));
    setRemarks(editingActivity?.remarks ?? "");
    setShowAdvanced(Boolean(editingActivity));
    setStatus(editingActivity?.status ?? CARBON_RECORD_STATUSES[0]);
    setLocalError("");
  }, [activityCategories, editingActivity]);

  async function handleSubmit() {
    if (!categoryId) {
      setLocalError("Activity category is required.");
      return;
    }

    if (!activityDate.trim()) {
      setLocalError("Activity date is required.");
      return;
    }

    if (!cropName.trim()) {
      setLocalError("Crop name is required.");
      return;
    }

    const success = await onSubmit({
      activityDate,
      carbonFarmPlotId,
      categoryId,
      cropName,
      inputUsed,
      quantityUnit,
      quantityValue,
      remarks,
      status
    });

    if (success && !editingActivity) {
      setActivityDate(toInputDate(new Date()));
      setCarbonFarmPlotId("");
      setCropName("");
      setInputUsed("");
      setQuantityUnit("");
      setQuantityValue("");
      setRemarks("");
      setShowAdvanced(false);
    }
  }

  return (
    <View style={styles.formBlock}>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}

      <OptionGroup
        label="Activity category"
        options={activityCategories.map((category) => category.id)}
        value={categoryId}
        renderLabel={(value) =>
          activityCategories.find((category) => category.id === value)?.name ?? value
        }
        onChange={setCategoryId}
      />

      {plots.length ? (
        <OptionGroup
          label="Farm plot"
          options={["", ...plots.map((plot) => plot.id)]}
          value={carbonFarmPlotId}
          renderLabel={(value) =>
            value
              ? (plots.find((plot) => plot.id === value)?.farmName ?? value)
              : "Profile level"
          }
          onChange={setCarbonFarmPlotId}
        />
      ) : null}

      <View style={styles.formGrid}>
        <FormField
          label="Activity date"
          value={activityDate}
          onChange={setActivityDate}
        />
        <FormField label="Crop" value={cropName} onChange={setCropName} />
        <FormField label="Input used" value={inputUsed} onChange={setInputUsed} />
        <FormField
          label="Quantity"
          keyboardType="decimal-pad"
          value={quantityValue}
          onChange={setQuantityValue}
        />
        <FormField label="Unit" value={quantityUnit} onChange={setQuantityUnit} />
      </View>

      <AdvancedSection
        isOpen={showAdvanced}
        label="More activity details"
        onToggle={() => setShowAdvanced((current) => !current)}
      >
        <View style={styles.formGrid}>
          <FormField label="Remarks" value={remarks} onChange={setRemarks} />
        </View>
        <OptionGroup
          label="Status"
          options={CARBON_RECORD_STATUSES}
          value={status}
          onChange={setStatus}
        />
      </AdvancedSection>

      <PrimaryButton
        disabled={isSubmitting}
        label={
          isSubmitting
            ? "Saving..."
            : editingActivity
              ? "Update activity"
              : "Add activity"
        }
        onPress={handleSubmit}
      />
    </View>
  );
}

function ProfileList({
  editingProfileId,
  onEdit,
  onSelect,
  profiles,
  selectedProfileId
}: {
  editingProfileId?: string;
  onEdit: (profile: CarbonProfileRecord) => void;
  onSelect: (profile: CarbonProfileRecord) => void;
  profiles: CarbonProfileRecord[];
  selectedProfileId: string | null;
}) {
  if (!profiles.length) {
    return <Text style={styles.emptyText}>No farmers enrolled yet.</Text>;
  }

  return (
    <View style={styles.listBlock}>
      {profiles.map((profile) => {
        const isSelected = selectedProfileId === profile.id;

        return (
          <Pressable
            key={profile.id}
            accessibilityRole="button"
            style={[styles.recordRow, isSelected && styles.selectedRow]}
            onPress={() => onSelect(profile)}
          >
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{profile.displayName}</Text>
              <Text style={styles.rowMeta}>
                {profile.carbonIdentityId} - {profile.participantType}
              </Text>
              <Text style={styles.rowMeta}>
                {[profile.memberNumber, profile.username].filter(Boolean).join(" - ") ||
                  "Login not linked"}
              </Text>
              <Text style={styles.rowMeta}>
                {[profile.village, profile.taluka, profile.districtName]
                  .filter(Boolean)
                  .join(", ") || "Location not set"}
              </Text>
              {isSelected ? (
                <Text style={styles.selectedHint}>
                  Farm, soil, and activity records shown on right
                </Text>
              ) : null}
            </View>
            <View style={styles.actionColumn}>
              <StatusBadge
                label={isSelected ? "Selected" : profile.status}
                tone="neutral"
              />
              <ActionButton
                label={editingProfileId === profile.id ? "Editing" : "Edit"}
                onPress={() => onEdit(profile)}
              />
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

function PlotList({
  editingPlotId,
  isFormOpen,
  onAdd,
  onEdit,
  plots
}: {
  editingPlotId?: string;
  isFormOpen: boolean;
  onAdd: () => void;
  onEdit: (plot: CarbonFarmPlotRecord) => void;
  plots: CarbonFarmPlotRecord[];
}) {
  return (
    <View style={styles.listBlock}>
      <View style={styles.sectionHeaderRow}>
        <View>
          <Text style={styles.sectionLabel}>Saved farm plots</Text>
          <Text style={styles.panelMeta}>
            {plots.length} plot{plots.length === 1 ? "" : "s"} for this farmer
          </Text>
        </View>
        <ActionButton
          label={isFormOpen && !editingPlotId ? "Adding" : "Add plot"}
          onPress={onAdd}
        />
      </View>
      {plots.length ? (
        plots.map((plot) => (
          <View key={plot.id} style={styles.recordRow}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{plot.farmName}</Text>
              <Text style={styles.rowMeta}>
                {formatNumber(plot.areaAcres)} ac - {plot.latitude}, {plot.longitude}
              </Text>
              <Text style={styles.rowMeta}>
                {[plot.primaryCrop, plot.irrigationSource, plot.tillageStatus]
                  .filter(Boolean)
                  .join(" - ") || "Plot context not set"}
              </Text>
              <Text style={styles.rowMeta}>
                {[
                  plot.variety,
                  plot.rootstock ? `Rootstock ${plot.rootstock}` : null,
                  plot.blockCode ? `Block ${plot.blockCode}` : null,
                  plot.spacing,
                  plot.rowCount !== undefined ? `${plot.rowCount} rows` : null,
                  plot.plantingDate ? `Planted ${formatDate(plot.plantingDate)}` : null
                ]
                  .filter(Boolean)
                  .join(" - ") || "Vineyard details not set"}
              </Text>
            </View>
            <View style={styles.actionColumn}>
              <StatusBadge label={plot.status} tone="neutral" />
              <ActionButton
                label={editingPlotId === plot.id ? "Editing" : "Edit"}
                onPress={() => onEdit(plot)}
              />
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>No farm plots saved for this farmer.</Text>
      )}
    </View>
  );
}

function SoilProfileList({
  editingSoilProfileId,
  isFormOpen,
  onAdd,
  onEdit,
  onUploadReport,
  plots,
  soilProfiles,
  uploadingReportId
}: {
  editingSoilProfileId?: string;
  isFormOpen: boolean;
  onAdd: () => void;
  onEdit: (profile: CarbonSoilProfileRecord) => void;
  onUploadReport: (profile: CarbonSoilProfileRecord) => void;
  plots: CarbonFarmPlotRecord[];
  soilProfiles: CarbonSoilProfileRecord[];
  uploadingReportId: string | null;
}) {
  return (
    <View style={styles.listBlock}>
      <View style={styles.sectionHeaderRow}>
        <View>
          <Text style={styles.sectionLabel}>Saved soil profiles</Text>
          <Text style={styles.panelMeta}>
            {soilProfiles.length} soil record{soilProfiles.length === 1 ? "" : "s"}
          </Text>
        </View>
        <ActionButton
          label={isFormOpen && !editingSoilProfileId ? "Adding" : "Add soil"}
          onPress={onAdd}
        />
      </View>
      {soilProfiles.length ? (
        soilProfiles.map((profile) => {
          const plotName =
            plots.find((plot) => plot.id === profile.carbonFarmPlotId)?.farmName ??
            "Profile level";

          return (
            <View key={profile.id} style={styles.recordRow}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>{plotName}</Text>
                <Text style={styles.rowMeta}>
                  {[
                    profile.soilOrganicCarbonPercent !== undefined
                      ? `SOC ${profile.soilOrganicCarbonPercent}%`
                      : null,
                    profile.ph !== undefined ? `pH ${profile.ph}` : null,
                    profile.nitrogenKgHa !== undefined
                      ? `N ${profile.nitrogenKgHa}`
                      : null
                  ]
                    .filter(Boolean)
                    .join(" - ") || "Lab values not entered"}
                </Text>
                <Text style={styles.rowMeta}>
                  {[
                    profile.labName,
                    profile.reportFileName
                      ? profile.reportStorageKey
                        ? `Uploaded ${profile.reportFileName}`
                        : profile.reportFileName
                      : null
                  ]
                    .filter(Boolean)
                    .join(" - ") || "Report metadata not set"}
                </Text>
              </View>
              <View style={styles.actionColumn}>
                <StatusBadge label={profile.status} tone="neutral" />
                <ActionButton
                  disabled={uploadingReportId === profile.id}
                  label={
                    uploadingReportId === profile.id
                      ? "Uploading"
                      : profile.reportStorageKey
                        ? "Replace report"
                        : "Upload report"
                  }
                  onPress={() => onUploadReport(profile)}
                />
                <ActionButton
                  label={editingSoilProfileId === profile.id ? "Editing" : "Edit"}
                  onPress={() => onEdit(profile)}
                />
              </View>
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No soil profiles saved for this farmer.</Text>
      )}
    </View>
  );
}

function ActivityList({
  activities,
  editingActivityId,
  isFormOpen,
  onAdd,
  onEdit
}: {
  activities: CarbonActivityRecord[];
  editingActivityId?: string;
  isFormOpen: boolean;
  onAdd: () => void;
  onEdit: (activity: CarbonActivityRecord) => void;
}) {
  return (
    <View style={styles.listBlock}>
      <View style={styles.sectionHeaderRow}>
        <View>
          <Text style={styles.sectionLabel}>Saved Carbon activities</Text>
          <Text style={styles.panelMeta}>
            {activities.length} {activities.length === 1 ? "activity" : "activities"}
          </Text>
        </View>
        <ActionButton
          label={isFormOpen && !editingActivityId ? "Adding" : "Add activity"}
          onPress={onAdd}
        />
      </View>
      {activities.length ? (
        activities.map((activity) => (
          <View key={activity.id} style={styles.recordRow}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{activity.categoryName}</Text>
              <Text style={styles.rowMeta}>
                {[
                  activity.cropName,
                  activity.farmName,
                  formatDate(activity.activityDate)
                ]
                  .filter(Boolean)
                  .join(" - ")}
              </Text>
              <Text style={styles.rowMeta}>
                {[activity.inputUsed, formatActivityQuantity(activity)]
                  .filter(Boolean)
                  .join(" - ") || "Input details not set"}
              </Text>
              <Text style={styles.rowMeta}>
                Evidence {activity.evidenceCount} -{" "}
                {formatVerificationStatus(activity.verificationStatus)}
              </Text>
            </View>
            <View style={styles.actionColumn}>
              <StatusBadge label={activity.status} tone="neutral" />
              <ActionButton
                label={editingActivityId === activity.id ? "Editing" : "Edit"}
                onPress={() => onEdit(activity)}
              />
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>
          No Carbon activities saved for this farmer.
        </Text>
      )}
    </View>
  );
}

function FormField({
  keyboardType,
  label,
  onChange,
  secureTextEntry,
  value
}: {
  keyboardType?: "decimal-pad" | "number-pad" | "numeric" | "phone-pad";
  label: string;
  onChange: (value: string) => void;
  secureTextEntry?: boolean;
  value: string;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        keyboardType={keyboardType}
        placeholder={label}
        placeholderTextColor="#8a99a1"
        secureTextEntry={secureTextEntry}
        style={styles.input}
        value={value}
        onChangeText={onChange}
      />
    </View>
  );
}

function OptionGroup<T extends string>({
  label,
  onChange,
  options,
  renderLabel,
  value
}: {
  label: string;
  onChange: (value: T) => void;
  options: readonly T[];
  renderLabel?: (value: T) => string;
  value: T;
}) {
  return (
    <View style={styles.optionBlock}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={styles.optionRow}>
        {options.map((option) => {
          const isSelected = option === value;

          return (
            <Pressable
              key={option || "blank-option"}
              accessibilityRole="button"
              style={[styles.optionButton, isSelected && styles.optionButtonActive]}
              onPress={() => onChange(option)}
            >
              <Text
                style={[
                  styles.optionButtonText,
                  isSelected && styles.optionButtonTextActive
                ]}
              >
                {renderLabel ? renderLabel(option) : option}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function AdvancedSection({
  children,
  isOpen,
  label,
  onToggle
}: {
  children: ReactNode;
  isOpen: boolean;
  label: string;
  onToggle: () => void;
}) {
  return (
    <View style={styles.advancedBlock}>
      <Pressable
        accessibilityRole="button"
        style={styles.advancedHeader}
        onPress={onToggle}
      >
        <Text style={styles.advancedTitle}>{label}</Text>
        <Text style={styles.advancedToggleText}>{isOpen ? "Hide" : "Show"}</Text>
      </Pressable>
      {isOpen ? <View style={styles.advancedContent}>{children}</View> : null}
    </View>
  );
}

function PrimaryButton({
  disabled,
  label,
  onPress
}: {
  disabled?: boolean;
  label: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      style={[styles.primaryButton, disabled && styles.disabledButton]}
      onPress={onPress}
    >
      <Text style={styles.primaryButtonText}>{label}</Text>
    </Pressable>
  );
}

function ActionButton({
  disabled,
  label,
  onPress
}: {
  disabled?: boolean;
  label: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      style={[styles.actionButton, disabled && styles.disabledButton]}
      onPress={onPress}
    >
      <Text style={styles.actionButtonText}>{label}</Text>
    </Pressable>
  );
}

function upsertById<T extends { id: string }>(items: T[], nextItem: T) {
  return [nextItem, ...items.filter((item) => item.id !== nextItem.id)];
}

function toInputNumber(value: number | undefined) {
  return value === undefined ? "" : String(value);
}

function toInputDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function formatActivityQuantity(activity: CarbonActivityRecord) {
  if (activity.quantityValue === undefined) {
    return "";
  }

  return [formatNumber(activity.quantityValue), activity.quantityUnit]
    .filter(Boolean)
    .join(" ");
}

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function formatVerificationStatus(status: CarbonActivityRecord["verificationStatus"]) {
  return status
    .split("_")
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(" ");
}

const styles = StyleSheet.create({
  actionButton: {
    alignItems: "center",
    backgroundColor: "#ffffff",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 7
  },
  actionButtonText: {
    color: "#1f6f73",
    fontSize: 12,
    fontWeight: "800"
  },
  actionColumn: {
    alignItems: "flex-end",
    gap: 8
  },
  advancedBlock: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1
  },
  advancedContent: {
    borderTopColor: "#d9e4ea",
    borderTopWidth: 1,
    gap: 12,
    padding: 12
  },
  advancedHeader: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    minHeight: 44,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  advancedTitle: {
    color: "#172126",
    fontSize: 13,
    fontWeight: "800"
  },
  advancedToggleText: {
    color: "#1f6f73",
    fontSize: 12,
    fontWeight: "800"
  },
  columnHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  columnTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  detailColumn: {
    backgroundColor: "#fbfcfd",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1.4,
    gap: 12,
    minWidth: 360,
    padding: 14
  },
  disabledButton: {
    opacity: 0.55
  },
  emptyText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  emptyStatePanel: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 6,
    padding: 14
  },
  emptyTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  field: {
    flex: 1,
    minWidth: 150
  },
  fieldLabel: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 6
  },
  formBlock: {
    gap: 12
  },
  formError: {
    backgroundColor: "#fff1f0",
    borderColor: "#ffc9c4",
    borderRadius: 8,
    borderWidth: 1,
    color: "#a13a31",
    fontSize: 13,
    fontWeight: "700",
    padding: 10
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  headerActions: {
    alignItems: "flex-end",
    gap: 8
  },
  input: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 14,
    fontWeight: "700",
    minHeight: 42,
    paddingHorizontal: 12,
    paddingVertical: 9
  },
  listBlock: {
    gap: 10
  },
  modalBackdrop: {
    bottom: 0,
    left: 0,
    position: "absolute",
    right: 0,
    top: 0
  },
  modalBody: {
    maxHeight: 620
  },
  modalBodyContent: {
    padding: 18
  },
  modalCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    maxWidth: 760,
    overflow: "hidden",
    width: "92%"
  },
  modalCloseButton: {
    alignItems: "center",
    backgroundColor: "#f7fafb",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    minHeight: 38,
    paddingHorizontal: 13,
    justifyContent: "center"
  },
  modalCloseText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  modalHeader: {
    alignItems: "flex-start",
    backgroundColor: "#eef7f7",
    borderBottomColor: "#d9e4ea",
    borderBottomWidth: 1,
    flexDirection: "row",
    gap: 14,
    justifyContent: "space-between",
    padding: 18
  },
  modalOverlay: {
    alignItems: "center",
    backgroundColor: "rgba(23, 33, 38, 0.54)",
    flex: 1,
    justifyContent: "center",
    padding: 18
  },
  modalSubtitle: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18,
    marginTop: 4
  },
  modalTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  modalTitleBlock: {
    flex: 1
  },
  optionBlock: {
    gap: 6
  },
  optionButton: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 8
  },
  optionButtonActive: {
    backgroundColor: "#1f6f73",
    borderColor: "#1f6f73"
  },
  optionButtonText: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800"
  },
  optionButtonTextActive: {
    color: "#ffffff"
  },
  optionRow: {
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
  panelHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  panelMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  primaryButton: {
    alignItems: "center",
    alignSelf: "flex-start",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 10
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  profileColumn: {
    flex: 1,
    gap: 12,
    minWidth: 320
  },
  recordRow: {
    alignItems: "flex-start",
    backgroundColor: "#f7fafb",
    borderColor: "#edf3f6",
    borderWidth: 1,
    borderRadius: 8,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 12
  },
  rowMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  rowText: {
    flex: 1
  },
  rowTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800",
    marginBottom: 4
  },
  secondaryButton: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 9
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  sectionHeaderRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  sectionLabel: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  selectedHeader: {
    alignItems: "flex-start",
    backgroundColor: "#eef7f7",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 12
  },
  selectedRow: {
    backgroundColor: "#eef7f7",
    borderColor: "#1f6f73",
    borderWidth: 2
  },
  selectedEyebrow: {
    color: "#1f6f73",
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 0,
    marginBottom: 3,
    textTransform: "uppercase"
  },
  selectedHint: {
    color: "#1f6f73",
    fontSize: 12,
    fontWeight: "800",
    marginTop: 7
  },
  selectedTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  summaryPill: {
    backgroundColor: "#ffffff",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    minWidth: 78,
    paddingHorizontal: 10,
    paddingVertical: 8
  },
  summaryPillLabel: {
    color: "#53666f",
    fontSize: 11,
    fontWeight: "800",
    marginTop: 2
  },
  summaryPillRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 10
  },
  summaryPillValue: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  workspaceGrid: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 14
  }
});
