import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { getErrorMessage } from "../../../core/errors/AppError";
import { StatusBadge } from "../../../ui/StatusBadge";
import {
  CARBON_AADHAAR_STATUSES,
  CARBON_BANK_STATUSES,
  CARBON_DOCUMENT_STATUSES,
  CARBON_LANGUAGES,
  CARBON_PARTICIPANT_TYPES,
  CARBON_RECORD_STATUSES,
  CARBON_TILLAGE_STATUSES,
  CarbonFarmPlotInput,
  CarbonFarmPlotRecord,
  CarbonProfileInput,
  CarbonProfileRecord,
  CarbonSoilProfileInput,
  CarbonSoilProfileRecord,
  createCarbonFarmPlot,
  createCarbonProfile,
  createCarbonSoilProfile,
  listCarbonFarmPlots,
  listCarbonProfiles,
  listCarbonSoilProfiles,
  updateCarbonFarmPlot,
  updateCarbonProfile,
  updateCarbonSoilProfile
} from "../data/carbonProfileStore";

type CarbonProfileAdminPanelProps = {
  onProfilesLoaded?: (profiles: CarbonProfileRecord[]) => void;
};

export function CarbonProfileAdminPanel({
  onProfilesLoaded
}: CarbonProfileAdminPanelProps) {
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
      setPlots([]);
      setSoilProfiles([]);
    }
  }, [selectedProfileId]);

  async function loadProfiles() {
    setIsLoading(true);
    setError("");

    try {
      const nextProfiles = await listCarbonProfiles();

      setProfiles(nextProfiles);
      onProfilesLoaded?.(nextProfiles);
      setSelectedProfileId((current) => current ?? nextProfiles[0]?.id ?? null);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load Carbon profiles."));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadProfileDetails(profileId: string) {
    setIsLoading(true);
    setError("");

    try {
      const [nextPlots, nextSoilProfiles] = await Promise.all([
        listCarbonFarmPlots(profileId),
        listCarbonSoilProfiles(profileId)
      ]);

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
      return true;
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Unable to save Carbon profile."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handlePlotSubmit(input: CarbonFarmPlotInput) {
    if (!selectedProfile) {
      setError("Select a Carbon profile before saving a farm plot.");
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
      setError("Select a Carbon profile before saving a soil profile.");
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
      return true;
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Unable to save Carbon soil profile."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View>
          <Text style={styles.subsectionTitle}>Carbon profiles and farm records</Text>
          <Text style={styles.panelMeta}>
            {profiles.length} profile{profiles.length === 1 ? "" : "s"} -{" "}
            {formatNumber(activeArea)} active acres
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

      <ProfileList
        editingProfileId={editingProfile?.id}
        onEdit={setEditingProfile}
        onSelect={(profile) => setSelectedProfileId(profile.id)}
        profiles={profiles}
        selectedProfileId={selectedProfileId}
      />

      <CarbonProfileForm
        editingProfile={editingProfile}
        isSubmitting={savingKey === "profile"}
        onCancel={() => setEditingProfile(null)}
        onSubmit={handleProfileSubmit}
      />

      <View style={styles.divider} />

      {selectedProfile ? (
        <>
          <View style={styles.selectedHeader}>
            <View>
              <Text style={styles.selectedTitle}>{selectedProfile.displayName}</Text>
              <Text style={styles.panelMeta}>
                {selectedProfile.carbonIdentityId} -{" "}
                {[selectedProfile.village, selectedProfile.taluka]
                  .filter(Boolean)
                  .join(", ") || "Location not set"}
              </Text>
            </View>
            <StatusBadge label={selectedProfile.status} tone="neutral" />
          </View>

          <CarbonPlotForm
            editingPlot={editingPlot}
            isSubmitting={savingKey === "plot"}
            onCancel={() => setEditingPlot(null)}
            onSubmit={handlePlotSubmit}
          />

          <PlotList
            editingPlotId={editingPlot?.id}
            onEdit={setEditingPlot}
            plots={plots}
          />

          <CarbonSoilProfileForm
            editingSoilProfile={editingSoilProfile}
            isSubmitting={savingKey === "soil"}
            onCancel={() => setEditingSoilProfile(null)}
            onSubmit={handleSoilSubmit}
            plots={plots}
          />

          <SoilProfileList
            editingSoilProfileId={editingSoilProfile?.id}
            onEdit={setEditingSoilProfile}
            plots={plots}
            soilProfiles={soilProfiles}
          />
        </>
      ) : (
        <Text style={styles.emptyText}>Create a Carbon profile to add farm records.</Text>
      )}
    </View>
  );
}

function CarbonProfileForm({
  editingProfile,
  isSubmitting,
  onCancel,
  onSubmit
}: {
  editingProfile: CarbonProfileRecord | null;
  isSubmitting: boolean;
  onCancel: () => void;
  onSubmit: (input: CarbonProfileInput) => Promise<boolean>;
}) {
  const [aadhaarStatus, setAadhaarStatus] = useState<string>(
    CARBON_AADHAAR_STATUSES[1]
  );
  const [bankStatus, setBankStatus] = useState<string>(CARBON_BANK_STATUSES[0]);
  const [carbonIdentityId, setCarbonIdentityId] = useState("");
  const [coordinatorUserId, setCoordinatorUserId] = useState("");
  const [croppingPattern, setCroppingPattern] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [districtName, setDistrictName] = useState("");
  const [documentStatus, setDocumentStatus] = useState<string>(
    CARBON_DOCUMENT_STATUSES[1]
  );
  const [fpoMemberProfileId, setFpoMemberProfileId] = useState("");
  const [gpsLatitude, setGpsLatitude] = useState("");
  const [gpsLongitude, setGpsLongitude] = useState("");
  const [languagePreference, setLanguagePreference] = useState<string>(
    CARBON_LANGUAGES[0]
  );
  const [livestockCount, setLivestockCount] = useState("");
  const [localError, setLocalError] = useState("");
  const [mobileNumber, setMobileNumber] = useState("");
  const [participantType, setParticipantType] = useState(CARBON_PARTICIPANT_TYPES[0]);
  const [stateName, setStateName] = useState("Maharashtra");
  const [status, setStatus] = useState(CARBON_RECORD_STATUSES[0]);
  const [taluka, setTaluka] = useState("");
  const [tillageStatus, setTillageStatus] = useState<string>(
    CARBON_TILLAGE_STATUSES[1]
  );
  const [totalLandHoldingAcres, setTotalLandHoldingAcres] = useState("");
  const [userId, setUserId] = useState("");
  const [village, setVillage] = useState("");

  useEffect(() => {
    setAadhaarStatus(editingProfile?.aadhaarStatus ?? CARBON_AADHAAR_STATUSES[1]);
    setBankStatus(editingProfile?.bankStatus ?? CARBON_BANK_STATUSES[0]);
    setCarbonIdentityId(editingProfile?.carbonIdentityId ?? "");
    setCoordinatorUserId(editingProfile?.coordinatorUserId ?? "");
    setCroppingPattern(editingProfile?.croppingPattern ?? "");
    setDisplayName(editingProfile?.displayName ?? "");
    setDistrictName(editingProfile?.districtName ?? "");
    setDocumentStatus(editingProfile?.documentStatus ?? CARBON_DOCUMENT_STATUSES[1]);
    setFpoMemberProfileId(editingProfile?.fpoMemberProfileId ?? "");
    setGpsLatitude(toInputNumber(editingProfile?.gpsLatitude));
    setGpsLongitude(toInputNumber(editingProfile?.gpsLongitude));
    setLanguagePreference(editingProfile?.languagePreference ?? CARBON_LANGUAGES[0]);
    setLivestockCount(toInputNumber(editingProfile?.livestockCount));
    setMobileNumber(editingProfile?.mobileNumber ?? "");
    setParticipantType(editingProfile?.participantType ?? CARBON_PARTICIPANT_TYPES[0]);
    setStateName(editingProfile?.stateName ?? "Maharashtra");
    setStatus(editingProfile?.status ?? CARBON_RECORD_STATUSES[0]);
    setTaluka(editingProfile?.taluka ?? "");
    setTillageStatus(editingProfile?.tillageStatus ?? CARBON_TILLAGE_STATUSES[1]);
    setTotalLandHoldingAcres(toInputNumber(editingProfile?.totalLandHoldingAcres));
    setUserId(editingProfile?.userId ?? "");
    setVillage(editingProfile?.village ?? "");
    setLocalError("");
  }, [editingProfile]);

  async function handleSubmit() {
    const input: CarbonProfileInput = {
      aadhaarStatus,
      bankStatus,
      carbonIdentityId,
      coordinatorUserId,
      croppingPattern,
      displayName,
      districtName,
      documentStatus,
      fpoMemberProfileId,
      gpsLatitude,
      gpsLongitude,
      languagePreference,
      livestockCount,
      mobileNumber,
      participantType,
      stateName,
      status,
      taluka,
      tillageStatus,
      totalLandHoldingAcres,
      userId,
      village
    };

    if (!displayName.trim()) {
      setLocalError("Display name is required.");
      return;
    }

    const success = await onSubmit(input);
    if (success && !editingProfile) {
      setCarbonIdentityId("");
      setCoordinatorUserId("");
      setCroppingPattern("");
      setDisplayName("");
      setDistrictName("");
      setFpoMemberProfileId("");
      setGpsLatitude("");
      setGpsLongitude("");
      setLivestockCount("");
      setMobileNumber("");
      setTaluka("");
      setTotalLandHoldingAcres("");
      setUserId("");
      setVillage("");
    }
  }

  return (
    <View style={styles.formBlock}>
      <View style={styles.formTitleRow}>
        <Text style={styles.sectionLabel}>
          {editingProfile ? "Edit Carbon profile" : "Add Carbon profile"}
        </Text>
        {editingProfile ? (
          <Pressable accessibilityRole="button" onPress={onCancel}>
            <Text style={styles.linkText}>Cancel edit</Text>
          </Pressable>
        ) : null}
      </View>

      {localError ? <Text style={styles.formError}>{localError}</Text> : null}

      <View style={styles.formGrid}>
        <FormField label="Display name" value={displayName} onChange={setDisplayName} />
        <FormField
          label="Carbon ID"
          value={carbonIdentityId}
          onChange={setCarbonIdentityId}
        />
        <FormField label="Mobile" value={mobileNumber} onChange={setMobileNumber} />
        <FormField label="Village" value={village} onChange={setVillage} />
        <FormField label="Taluka" value={taluka} onChange={setTaluka} />
        <FormField label="District" value={districtName} onChange={setDistrictName} />
        <FormField label="State" value={stateName} onChange={setStateName} />
        <FormField
          label="Total acres"
          keyboardType="decimal-pad"
          value={totalLandHoldingAcres}
          onChange={setTotalLandHoldingAcres}
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
        <FormField
          label="Cropping pattern"
          value={croppingPattern}
          onChange={setCroppingPattern}
        />
        <FormField label="User ID" value={userId} onChange={setUserId} />
        <FormField
          label="FPO member ID"
          value={fpoMemberProfileId}
          onChange={setFpoMemberProfileId}
        />
        <FormField
          label="Coordinator ID"
          value={coordinatorUserId}
          onChange={setCoordinatorUserId}
        />
      </View>

      <OptionGroup
        label="Participant"
        options={CARBON_PARTICIPANT_TYPES}
        value={participantType}
        onChange={setParticipantType}
      />
      <OptionGroup
        label="Language"
        options={CARBON_LANGUAGES}
        value={languagePreference}
        onChange={setLanguagePreference}
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

      <PrimaryButton
        disabled={isSubmitting}
        label={isSubmitting ? "Saving..." : editingProfile ? "Update profile" : "Add profile"}
        onPress={handleSubmit}
      />
    </View>
  );
}

function CarbonPlotForm({
  editingPlot,
  isSubmitting,
  onCancel,
  onSubmit
}: {
  editingPlot: CarbonFarmPlotRecord | null;
  isSubmitting: boolean;
  onCancel: () => void;
  onSubmit: (input: CarbonFarmPlotInput) => Promise<boolean>;
}) {
  const [areaAcres, setAreaAcres] = useState("");
  const [farmName, setFarmName] = useState("");
  const [irrigationSource, setIrrigationSource] = useState("Drip");
  const [latitude, setLatitude] = useState("");
  const [localError, setLocalError] = useState("");
  const [longitude, setLongitude] = useState("");
  const [primaryCrop, setPrimaryCrop] = useState("");
  const [status, setStatus] = useState(CARBON_RECORD_STATUSES[0]);
  const [surveyNumber, setSurveyNumber] = useState("");
  const [tillageStatus, setTillageStatus] = useState<string>(
    CARBON_TILLAGE_STATUSES[1]
  );

  useEffect(() => {
    setAreaAcres(toInputNumber(editingPlot?.areaAcres));
    setFarmName(editingPlot?.farmName ?? "");
    setIrrigationSource(editingPlot?.irrigationSource ?? "Drip");
    setLatitude(toInputNumber(editingPlot?.latitude));
    setLongitude(toInputNumber(editingPlot?.longitude));
    setPrimaryCrop(editingPlot?.primaryCrop ?? "");
    setStatus(editingPlot?.status ?? CARBON_RECORD_STATUSES[0]);
    setSurveyNumber(editingPlot?.surveyNumber ?? "");
    setTillageStatus(editingPlot?.tillageStatus ?? CARBON_TILLAGE_STATUSES[1]);
    setLocalError("");
  }, [editingPlot]);

  async function handleSubmit() {
    if (!farmName.trim() || !areaAcres.trim() || !latitude.trim() || !longitude.trim()) {
      setLocalError("Farm name, area, latitude, and longitude are required.");
      return;
    }

    const success = await onSubmit({
      areaAcres,
      farmName,
      irrigationSource,
      latitude,
      longitude,
      primaryCrop,
      status,
      surveyNumber,
      tillageStatus
    });

    if (success && !editingPlot) {
      setAreaAcres("");
      setFarmName("");
      setLatitude("");
      setLongitude("");
      setPrimaryCrop("");
      setSurveyNumber("");
    }
  }

  return (
    <View style={styles.formBlock}>
      <View style={styles.formTitleRow}>
        <Text style={styles.sectionLabel}>
          {editingPlot ? "Edit farm plot" : "Add farm plot"}
        </Text>
        {editingPlot ? (
          <Pressable accessibilityRole="button" onPress={onCancel}>
            <Text style={styles.linkText}>Cancel edit</Text>
          </Pressable>
        ) : null}
      </View>
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
        <FormField
          label="Irrigation"
          value={irrigationSource}
          onChange={setIrrigationSource}
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
  onCancel,
  onSubmit,
  plots
}: {
  editingSoilProfile: CarbonSoilProfileRecord | null;
  isSubmitting: boolean;
  onCancel: () => void;
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

  useEffect(() => {
    setBulkDensityGmCm3(toInputNumber(editingSoilProfile?.bulkDensityGmCm3));
    setCarbonFarmPlotId(editingSoilProfile?.carbonFarmPlotId ?? "");
    setElectricalConductivity(toInputNumber(editingSoilProfile?.electricalConductivity));
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
    }
  }

  return (
    <View style={styles.formBlock}>
      <View style={styles.formTitleRow}>
        <Text style={styles.sectionLabel}>
          {editingSoilProfile ? "Edit soil profile" : "Add soil profile"}
        </Text>
        {editingSoilProfile ? (
          <Pressable accessibilityRole="button" onPress={onCancel}>
            <Text style={styles.linkText}>Cancel edit</Text>
          </Pressable>
        ) : null}
      </View>

      {plots.length ? (
        <OptionGroup
          label="Plot"
          options={["", ...plots.map((plot) => plot.id)]}
          value={carbonFarmPlotId}
          renderLabel={(value) =>
            value
              ? plots.find((plot) => plot.id === value)?.farmName ?? value
              : "No plot link"
          }
          onChange={setCarbonFarmPlotId}
        />
      ) : null}

      <View style={styles.formGrid}>
        <FormField label="Test date" value={testDate} onChange={setTestDate} />
        <FormField label="Lab name" value={labName} onChange={setLabName} />
        <FormField
          label="SOC %"
          keyboardType="decimal-pad"
          value={soilOrganicCarbonPercent}
          onChange={setSoilOrganicCarbonPercent}
        />
        <FormField label="pH" keyboardType="decimal-pad" value={ph} onChange={setPh} />
        <FormField
          label="EC"
          keyboardType="decimal-pad"
          value={electricalConductivity}
          onChange={setElectricalConductivity}
        />
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
        <FormField
          label="Bulk density"
          keyboardType="decimal-pad"
          value={bulkDensityGmCm3}
          onChange={setBulkDensityGmCm3}
        />
        <FormField label="Texture" value={texture} onChange={setTexture} />
        <FormField
          label="Report file"
          value={reportFileName}
          onChange={setReportFileName}
        />
        <FormField
          label="Content type"
          value={reportContentType}
          onChange={setReportContentType}
        />
        <FormField
          label="Storage key"
          value={reportStorageKey}
          onChange={setReportStorageKey}
        />
        <FormField label="Report URL" value={reportUrl} onChange={setReportUrl} />
      </View>

      <OptionGroup
        label="Status"
        options={CARBON_RECORD_STATUSES}
        value={status}
        onChange={setStatus}
      />
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
    return <Text style={styles.emptyText}>No Carbon profiles saved yet.</Text>;
  }

  return (
    <View style={styles.listBlock}>
      {profiles.map((profile) => (
        <View
          key={profile.id}
          style={[
            styles.recordRow,
            selectedProfileId === profile.id && styles.selectedRow
          ]}
        >
          <View style={styles.rowText}>
            <Text style={styles.rowTitle}>{profile.displayName}</Text>
            <Text style={styles.rowMeta}>
              {profile.carbonIdentityId} - {profile.participantType}
            </Text>
            <Text style={styles.rowMeta}>
              {[profile.village, profile.taluka, profile.districtName]
                .filter(Boolean)
                .join(", ") || "Location not set"}
            </Text>
          </View>
          <View style={styles.actionColumn}>
            <StatusBadge label={profile.status} tone="neutral" />
            <ActionButton label="Select" onPress={() => onSelect(profile)} />
            <ActionButton
              label={editingProfileId === profile.id ? "Editing" : "Edit"}
              onPress={() => onEdit(profile)}
            />
          </View>
        </View>
      ))}
    </View>
  );
}

function PlotList({
  editingPlotId,
  onEdit,
  plots
}: {
  editingPlotId?: string;
  onEdit: (plot: CarbonFarmPlotRecord) => void;
  plots: CarbonFarmPlotRecord[];
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.sectionLabel}>Saved farm plots</Text>
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
        <Text style={styles.emptyText}>No farm plots saved for this profile.</Text>
      )}
    </View>
  );
}

function SoilProfileList({
  editingSoilProfileId,
  onEdit,
  plots,
  soilProfiles
}: {
  editingSoilProfileId?: string;
  onEdit: (profile: CarbonSoilProfileRecord) => void;
  plots: CarbonFarmPlotRecord[];
  soilProfiles: CarbonSoilProfileRecord[];
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.sectionLabel}>Saved soil profiles</Text>
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
                  {[profile.labName, profile.reportFileName].filter(Boolean).join(" - ") ||
                    "Report metadata not set"}
                </Text>
              </View>
              <View style={styles.actionColumn}>
                <StatusBadge label={profile.status} tone="neutral" />
                <ActionButton
                  label={
                    editingSoilProfileId === profile.id ? "Editing" : "Edit"
                  }
                  onPress={() => onEdit(profile)}
                />
              </View>
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No soil profiles saved for this profile.</Text>
      )}
    </View>
  );
}

function FormField({
  keyboardType,
  label,
  onChange,
  value
}: {
  keyboardType?: "decimal-pad" | "number-pad";
  label: string;
  onChange: (value: string) => void;
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

function ActionButton({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" style={styles.actionButton} onPress={onPress}>
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

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
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
  disabledButton: {
    opacity: 0.55
  },
  divider: {
    backgroundColor: "#d9e4ea",
    height: 1
  },
  emptyText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
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
  formTitleRow: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between"
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
  linkText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  listBlock: {
    gap: 10
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
  recordRow: {
    alignItems: "flex-start",
    backgroundColor: "#f7fafb",
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
    borderColor: "#1f6f73",
    borderWidth: 1
  },
  selectedTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  }
});
