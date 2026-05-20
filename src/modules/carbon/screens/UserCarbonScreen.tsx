import * as DocumentPicker from "expo-document-picker";
import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { ConfirmationModal } from "../../../ui/ConfirmationModal";
import { StateCard } from "../../../ui/StateCard";
import { StatusBadge } from "../../../ui/StatusBadge";
import { ActivityWizard } from "../../../shared/components/ActivityWizard";
import {
  SoilDashboardMetric,
  SoilDashboardRecord,
  SoilProfileDashboard
} from "../../../shared/components/SoilProfileDashboard";
import { SoilManualEntryForm } from "../../../shared/components/SoilManualEntryForm";
import {
  SoilReportField,
  SoilReportValues
} from "../../../shared/components/SoilReportUploader";
import {
  FarmerBankDetailsInput,
  FarmerBankDetailsRecord,
  FarmerBankDetailsStatus,
  getFarmerBankDetails,
  saveFarmerBankDetails
} from "../../../shared/farmers/bankDetailsStore";
import {
  deleteFarmerDocument,
  documentTypeLabel,
  FarmerDocumentRecord,
  FarmerDocumentStatus,
  FarmerDocumentType,
  FARMER_DOCUMENT_TYPES,
  listFarmerDocuments,
  uploadFarmerDocument
} from "../../../shared/farmers/documentStore";
import {
  FarmerProfileCompletionRecord,
  FarmerProfileCompletionStep,
  getFarmerProfileCompletion
} from "../../../shared/farmers/profileCompletionStore";
import { getFarmerCarbonSnapshot } from "../data/carbonStore";
import {
  CarbonFarmPlotRecord,
  CarbonProfileRecord,
  CarbonSoilProfileInput,
  CarbonSoilProfileRecord,
  CarbonSoilReportFile,
  createCarbonSoilProfile,
  getMyCarbonProfile,
  listCarbonFarmPlots,
  listCarbonSoilProfiles,
  updateCarbonSoilProfile,
  uploadCarbonSoilReport
} from "../data/carbonProfileStore";

type UserCarbonScreenProps = {
  username: string | null;
};

type FarmerCarbonSnapshot = Awaited<ReturnType<typeof getFarmerCarbonSnapshot>>;
type CarbonUserSectionId =
  | "activities"
  | "dashboard"
  | "plots"
  | "profile"
  | "soil";

const carbonUserSections: { id: CarbonUserSectionId; label: string }[] = [
  { id: "dashboard", label: "Dashboard" },
  { id: "profile", label: "Farmer profile" },
  { id: "plots", label: "Plots" },
  { id: "soil", label: "Soil" },
  { id: "activities", label: "Activities" }
];

export function UserCarbonScreen({ username }: UserCarbonScreenProps) {
  const [activeSection, setActiveSection] =
    useState<CarbonUserSectionId>("dashboard");
  const [snapshot, setSnapshot] = useState<FarmerCarbonSnapshot | null>(null);
  const [liveError, setLiveError] = useState("");
  const [liveBankDetails, setLiveBankDetails] =
    useState<FarmerBankDetailsRecord | null>(null);
  const [liveDocuments, setLiveDocuments] = useState<FarmerDocumentRecord[]>([]);
  const [livePlots, setLivePlots] = useState<CarbonFarmPlotRecord[]>([]);
  const [liveProfile, setLiveProfile] = useState<CarbonProfileRecord | null>(null);
  const [profileCompletion, setProfileCompletion] =
    useState<FarmerProfileCompletionRecord | null>(null);
  const [liveSoilProfiles, setLiveSoilProfiles] = useState<CarbonSoilProfileRecord[]>(
    []
  );

  useEffect(() => {
    async function loadCarbonData() {
      const nextSnapshot = await getFarmerCarbonSnapshot(username);
      setSnapshot(nextSnapshot);
      setLiveError("");

      try {
        const nextProfile = await getMyCarbonProfile();
        const [
          nextDocuments,
          nextPlots,
          nextSoilProfiles,
          nextProfileCompletion,
          nextBankDetails
        ] = await Promise.all([
          listFarmerDocuments(),
          listCarbonFarmPlots(nextProfile.id),
          listCarbonSoilProfiles(nextProfile.id),
          getFarmerProfileCompletion(),
          getFarmerBankDetails()
        ]);

        setLiveProfile(nextProfile);
        setLiveBankDetails(nextBankDetails);
        setLiveDocuments(nextDocuments);
        setLivePlots(nextPlots);
        setProfileCompletion(nextProfileCompletion);
        setLiveSoilProfiles(nextSoilProfiles);
      } catch (error) {
        setLiveBankDetails(null);
        setLiveDocuments([]);
        setLiveProfile(null);
        setLivePlots([]);
        setProfileCompletion(null);
        setLiveSoilProfiles([]);
        setLiveError(
          error instanceof Error ? error.message : "Carbon profile unavailable."
        );
      }
    }

    loadCarbonData();
  }, [username]);

  const summary = useMemo(() => {
    if (!snapshot) {
      return [];
    }

    const liveArea =
      liveProfile?.totalLandHoldingAcres ?? sum(livePlots.map((plot) => plot.areaAcres));
    const primarySoilProfile = liveSoilProfiles[0];

    return [
      {
        label: "Total plot area",
        value: `${formatNumber(liveArea > 0 ? liveArea : snapshot.profile.totalLandHoldingAcres)} ac`
      },
      {
        label: "Soil carbon",
        value: primarySoilProfile?.soilOrganicCarbonPercent
          ? `${primarySoilProfile.soilOrganicCarbonPercent}% SOC`
          : `${snapshot.soilProfile.soilOrganicCarbonPercent}% SOC`
      },
      {
        label: "Open activities",
        value: String(snapshot.pendingActivities)
      },
      {
        label: "Plots",
        value: String(livePlots.length || 1)
      }
    ];
  }, [livePlots, liveProfile, liveSoilProfiles, snapshot]);

  if (!snapshot) {
    return (
      <View style={styles.section}>
        <StateCard message="Loading carbon dashboard..." tone="empty" />
      </View>
    );
  }

  return (
    <View style={styles.section}>
      <View>
        <Text style={styles.pageTitle}>Carbon farmer workspace</Text>
        <Text style={styles.pageCopy}>
          Profile, vineyard plots, soil records, and carbon activity evidence in one
          Carbon-first flow.
        </Text>
      </View>

      {liveError ? <StateCard message={liveError} tone="warning" /> : null}

      <SectionTabs
        activeSection={activeSection}
        onChange={setActiveSection}
        sections={carbonUserSections}
      />

      {activeSection === "dashboard" ? (
        <DashboardSection
          summary={summary}
          snapshot={snapshot}
          onOpenSection={setActiveSection}
        />
      ) : null}

      {activeSection === "profile" ? (
        <ProfileSection
          bankDetails={liveBankDetails}
          documents={liveDocuments}
          liveProfile={liveProfile}
          onBankDetailsSaved={(details) => {
            setLiveBankDetails(details);
            getFarmerProfileCompletion()
              .then(setProfileCompletion)
              .catch((error) => {
                setLiveError(
                  error instanceof Error
                    ? error.message
                    : "Profile completion unavailable."
                );
              });
          }}
          onDocumentsChanged={(documents) => {
            setLiveDocuments(documents);
            getFarmerProfileCompletion()
              .then(setProfileCompletion)
              .catch((error) => {
                setLiveError(
                  error instanceof Error
                    ? error.message
                    : "Profile completion unavailable."
                );
              });
          }}
          profileCompletion={profileCompletion}
          snapshot={snapshot}
        />
      ) : null}

      {activeSection === "plots" ? (
        <PlotsSection livePlots={livePlots} snapshot={snapshot} />
      ) : null}

      {activeSection === "soil" ? (
        <SoilSection
          livePlots={livePlots}
          liveProfile={liveProfile}
          liveSoilProfiles={liveSoilProfiles}
          snapshot={snapshot}
          onSoilProfilesChanged={setLiveSoilProfiles}
        />
      ) : null}

      {activeSection === "activities" ? (
        <ActivitiesSection snapshot={snapshot} />
      ) : null}
    </View>
  );
}

function DashboardSection({
  onOpenSection,
  snapshot,
  summary
}: {
  onOpenSection: (section: CarbonUserSectionId) => void;
  snapshot: FarmerCarbonSnapshot;
  summary: Array<{ label: string; value: string }>;
}) {
  return (
    <>
      <View style={styles.statsGrid}>
        {summary.map((item) => (
          <View key={item.label} style={styles.statCard}>
            <Text style={styles.statValue}>{item.value}</Text>
            <Text style={styles.statLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>P0 journey</Text>
        <View style={styles.actionGrid}>
          <QuickAction
            label="Farmer profile"
            meta="Identity, bank, documents"
            onPress={() => onOpenSection("profile")}
          />
          <QuickAction
            label="Vineyard plots"
            meta="List, add, detail"
            onPress={() => onOpenSection("plots")}
          />
          <QuickAction
            label="Soil records"
            meta="Dashboard, upload, manual"
            onPress={() => onOpenSection("soil")}
          />
          <QuickAction
            label="Activity wizard"
            meta="Workflow and evidence"
            onPress={() => onOpenSection("activities")}
          />
        </View>
      </View>

      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={styles.cardHeaderText}>
            <Text style={styles.sectionTitle}>Weather snapshot</Text>
            <Text style={styles.cardDescription}>
              {snapshot.weatherSnapshot.condition} -{" "}
              {snapshot.weatherSnapshot.humidityPercent}% humidity - rain risk{" "}
              {snapshot.weatherSnapshot.rainfallRisk}
            </Text>
            <Text style={styles.cardMeta}>{snapshot.weatherSnapshot.advisory}</Text>
          </View>
          <StatusBadge label={snapshot.weatherSnapshot.updatedAt} tone="neutral" />
        </View>
      </View>
    </>
  );
}

function ProfileSection({
  bankDetails,
  documents,
  liveProfile,
  onBankDetailsSaved,
  onDocumentsChanged,
  profileCompletion,
  snapshot
}: {
  bankDetails: FarmerBankDetailsRecord | null;
  documents: FarmerDocumentRecord[];
  liveProfile: CarbonProfileRecord | null;
  onBankDetailsSaved: (details: FarmerBankDetailsRecord) => void;
  onDocumentsChanged: (documents: FarmerDocumentRecord[]) => void;
  profileCompletion: FarmerProfileCompletionRecord | null;
  snapshot: FarmerCarbonSnapshot;
}) {
  const profile = liveProfile;

  return (
    <>
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={styles.cardHeaderText}>
            <Text style={styles.sectionTitle}>Farmer profile</Text>
            <Text style={styles.cardDescription}>
              {profile?.displayName ?? snapshot.profile.farmerName}
            </Text>
            <Text style={styles.cardMeta}>
              {[
                profile?.mobileNumber ?? snapshot.profile.mobileNumber,
                profile?.village ?? snapshot.profile.village,
                profile?.taluka ?? snapshot.profile.taluka,
                profile?.districtName ?? snapshot.profile.district
              ]
                .filter(Boolean)
                .join(" - ")}
            </Text>
          </View>
          <StatusBadge label={profile?.status ?? "ACTIVE"} tone="neutral" />
        </View>
      </View>

      <ProfileCompletionWidget completion={profileCompletion} />

      <BankDetailsForm
        bankDetails={bankDetails}
        onSaved={onBankDetailsSaved}
      />

      <DocumentUploadSection
        documents={documents}
        onDocumentsChanged={onDocumentsChanged}
      />
    </>
  );
}

function BankDetailsForm({
  bankDetails,
  onSaved
}: {
  bankDetails: FarmerBankDetailsRecord | null;
  onSaved: (details: FarmerBankDetailsRecord) => void;
}) {
  const [accountHolderName, setAccountHolderName] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [bankName, setBankName] = useState("");
  const [ifscCode, setIfscCode] = useState("");
  const [upiId, setUpiId] = useState("");
  const [formError, setFormError] = useState("");
  const [formMessage, setFormMessage] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [showConfirmSave, setShowConfirmSave] = useState(false);

  useEffect(() => {
    setAccountHolderName(bankDetails?.accountHolderName ?? "");
    setAccountNumber(bankDetails?.accountNumber ?? "");
    setBankName(bankDetails?.bankName ?? "");
    setIfscCode(bankDetails?.ifscCode ?? "");
    setUpiId(bankDetails?.upiId ?? "");
    setFormError("");
    setFormMessage("");
  }, [bankDetails]);

  const input: FarmerBankDetailsInput = {
    accountHolderName,
    accountNumber,
    bankName,
    ifscCode,
    upiId
  };

  function requestSave() {
    const validationError = validateBankDetails(input);
    if (validationError) {
      setFormError(validationError);
      setFormMessage("");
      return;
    }

    setFormError("");
    setShowConfirmSave(true);
  }

  async function confirmSave() {
    setShowConfirmSave(false);
    setIsSaving(true);
    setFormError("");
    setFormMessage("");

    try {
      const saved = await saveFarmerBankDetails(input, bankDetails?.id);
      onSaved(saved);
      setFormMessage("Bank details saved. Verification is pending.");
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "Unable to save bank details."
      );
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderText}>
          <Text style={styles.sectionTitle}>Bank details</Text>
          <Text style={styles.cardDescription}>
            {bankDetails
              ? `${bankDetails.bankName} - ${bankDetails.ifscCode}`
              : "No bank details saved yet."}
          </Text>
        </View>
        <StatusBadge
          label={bankStatusLabel(bankDetails?.status)}
          tone={bankStatusTone(bankDetails?.status)}
        />
      </View>

      {formError ? (
        <StateCard message={formError} title="Bank details" tone="error" />
      ) : null}
      {formMessage ? (
        <StateCard message={formMessage} title="Bank details" tone="success" />
      ) : null}

      <View style={styles.formGrid}>
        <FormField
          label="Account holder name"
          onChangeText={setAccountHolderName}
          value={accountHolderName}
        />
        <FormField
          label="Account number"
          onChangeText={setAccountNumber}
          value={accountNumber}
        />
        <FormField
          autoCapitalize="characters"
          label="IFSC code"
          onChangeText={setIfscCode}
          value={ifscCode}
        />
        <FormField
          label="UPI ID"
          onChangeText={setUpiId}
          value={upiId}
        />
        <FormField
          label="Bank name"
          onChangeText={setBankName}
          value={bankName}
        />
      </View>

      <View style={styles.formActions}>
        <Pressable
          accessibilityRole="button"
          disabled={isSaving}
          style={[styles.primaryButton, isSaving && styles.buttonDisabled]}
          onPress={requestSave}
        >
          <Text style={styles.primaryButtonText}>
            {isSaving ? "Saving..." : bankDetails ? "Update bank details" : "Save bank details"}
          </Text>
        </Pressable>
      </View>

      <ConfirmationModal
        cancelLabel="Review"
        confirmLabel="Save"
        message="Save these bank details with pending verification status?"
        onCancel={() => setShowConfirmSave(false)}
        onConfirm={confirmSave}
        title="Save bank details"
        visible={showConfirmSave}
      />
    </View>
  );
}

function DocumentUploadSection({
  documents,
  onDocumentsChanged
}: {
  documents: FarmerDocumentRecord[];
  onDocumentsChanged: (documents: FarmerDocumentRecord[]) => void;
}) {
  const [deleteCandidate, setDeleteCandidate] =
    useState<FarmerDocumentRecord | null>(null);
  const [documentType, setDocumentType] =
    useState<FarmerDocumentType>("AADHAAR");
  const [documentError, setDocumentError] = useState("");
  const [documentMessage, setDocumentMessage] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  async function handleUpload() {
    setIsUploading(true);
    setDocumentError("");
    setDocumentMessage("");

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
      const validationError = validateDocumentAsset(asset);
      if (validationError) {
        setDocumentError(validationError);
        return;
      }

      const saved = await uploadFarmerDocument({
        documentType,
        file: {
          name: asset.name,
          type: asset.mimeType,
          uri: asset.uri
        }
      });

      onDocumentsChanged([saved, ...documents]);
      setDocumentMessage("Document uploaded. Verification is pending.");
    } catch (error) {
      setDocumentError(
        error instanceof Error ? error.message : "Unable to upload document."
      );
    } finally {
      setIsUploading(false);
    }
  }

  async function confirmDelete() {
    if (!deleteCandidate) {
      return;
    }

    setDeletingId(deleteCandidate.id);
    setDocumentError("");
    setDocumentMessage("");

    try {
      await deleteFarmerDocument(deleteCandidate.id);
      onDocumentsChanged(
        documents.filter((document) => document.id !== deleteCandidate.id)
      );
      setDocumentMessage("Document deleted.");
    } catch (error) {
      setDocumentError(
        error instanceof Error ? error.message : "Unable to delete document."
      );
    } finally {
      setDeletingId(null);
      setDeleteCandidate(null);
    }
  }

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderText}>
          <Text style={styles.sectionTitle}>KYC & Carbon Documents</Text>
          <Text style={styles.cardDescription}>
            {documents.length
              ? `${documents.length} document${documents.length === 1 ? "" : "s"} uploaded`
              : "No documents uploaded yet."}
          </Text>
        </View>
        <StatusBadge
          label={documents.length ? "Documents uploaded" : "Pending"}
          tone={documents.length ? "good" : "warning"}
        />
      </View>

      {documentError ? (
        <StateCard message={documentError} title="Documents" tone="error" />
      ) : null}
      {documentMessage ? (
        <StateCard message={documentMessage} title="Documents" tone="success" />
      ) : null}

      <View style={styles.documentTypeGrid}>
        {FARMER_DOCUMENT_TYPES.map((type) => {
          const selected = documentType === type;

          return (
            <Pressable
              key={type}
              accessibilityRole="button"
              style={[
                styles.documentTypeButton,
                selected && styles.documentTypeButtonSelected
              ]}
              onPress={() => setDocumentType(type)}
            >
              <Text
                style={[
                  styles.documentTypeButtonText,
                  selected && styles.documentTypeButtonTextSelected
                ]}
              >
                {documentTypeLabel(type)}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <View style={styles.formActions}>
        <Pressable
          accessibilityRole="button"
          disabled={isUploading}
          style={[styles.primaryButton, isUploading && styles.buttonDisabled]}
          onPress={handleUpload}
        >
          <Text style={styles.primaryButtonText}>
            {isUploading ? "Uploading..." : "Upload document"}
          </Text>
        </Pressable>
      </View>

      <View style={styles.documentList}>
        {documents.length ? (
          documents.map((document) => (
            <View key={document.id} style={styles.listRow}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>{document.fileName}</Text>
                <Text style={styles.rowMeta}>
                  {documentTypeLabel(document.documentType)} -{" "}
                  {formatDate(document.uploadedAt)}
                </Text>
                {document.verificationNotes ? (
                  <Text style={styles.rowMeta}>{document.verificationNotes}</Text>
                ) : null}
              </View>
              <View style={styles.documentRowActions}>
                <StatusBadge
                  label={documentStatusLabel(document.status)}
                  tone={documentStatusTone(document.status)}
                />
                {document.status === "PENDING_VERIFICATION" ? (
                  <Pressable
                    accessibilityRole="button"
                    disabled={deletingId === document.id}
                    style={[
                      styles.secondaryDangerButton,
                      deletingId === document.id && styles.buttonDisabled
                    ]}
                    onPress={() => setDeleteCandidate(document)}
                  >
                    <Text style={styles.secondaryDangerButtonText}>
                      {deletingId === document.id ? "Deleting..." : "Delete"}
                    </Text>
                  </Pressable>
                ) : null}
              </View>
            </View>
          ))
        ) : (
          <StateCard
            message="Upload Aadhaar, land record, soil report, bank proof, or another supporting document."
            tone="empty"
          />
        )}
      </View>

      <ConfirmationModal
        cancelLabel="Keep"
        confirmLabel="Delete"
        message="Delete this pending document?"
        onCancel={() => setDeleteCandidate(null)}
        onConfirm={confirmDelete}
        title="Delete document"
        visible={Boolean(deleteCandidate)}
      />
    </View>
  );
}

function FormField({
  autoCapitalize = "none",
  label,
  onChangeText,
  value
}: {
  autoCapitalize?: "none" | "sentences" | "words" | "characters";
  label: string;
  onChangeText: (value: string) => void;
  value: string;
}) {
  return (
    <View style={styles.formField}>
      <Text style={styles.formLabel}>{label}</Text>
      <TextInput
        autoCapitalize={autoCapitalize}
        onChangeText={onChangeText}
        style={styles.textInput}
        value={value}
      />
    </View>
  );
}

function ProfileCompletionWidget({
  completion
}: {
  completion: FarmerProfileCompletionRecord | null;
}) {
  if (!completion) {
    return (
      <StateCard
        message="Profile completion is unavailable until the Carbon profile is linked."
        title="Profile completion"
        tone="warning"
      />
    );
  }

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderText}>
          <Text style={styles.sectionTitle}>Profile completion</Text>
          <Text style={styles.cardDescription}>
            {completion.completedRequiredSteps}/{completion.totalRequiredSteps} required
            steps complete
          </Text>
        </View>
        <View style={styles.completionBadge}>
          <Text style={styles.completionValue}>
            {completion.completionPercentage}%
          </Text>
        </View>
      </View>

      <View style={styles.completionSteps}>
        {completion.steps.map((step) => (
          <CompletionStep key={step.code} step={step} />
        ))}
      </View>
    </View>
  );
}

function CompletionStep({ step }: { step: FarmerProfileCompletionStep }) {
  const statusLabel = profileCompletionStatusLabel(step);
  const tone = profileCompletionTone(step);

  return (
    <View style={styles.completionStep}>
      <View style={styles.rowText}>
        <Text style={styles.rowTitle}>{step.label}</Text>
        <Text style={styles.rowMeta}>{step.description}</Text>
      </View>
      <StatusBadge label={statusLabel} tone={tone} />
    </View>
  );
}

function profileCompletionStatusLabel(step: FarmerProfileCompletionStep) {
  if (step.comingSoon) {
    return "Coming soon";
  }

  return step.complete ? "Complete" : "Pending";
}

function profileCompletionTone(
  step: FarmerProfileCompletionStep
): "good" | "neutral" | "warning" {
  if (step.complete) {
    return "good";
  }

  return step.comingSoon ? "neutral" : "warning";
}

function validateBankDetails(input: FarmerBankDetailsInput) {
  if (!input.accountHolderName.trim()) {
    return "Account holder name is required.";
  }

  if (!input.accountNumber.trim()) {
    return "Account number is required.";
  }

  if (!/^[A-Z]{4}0[A-Z0-9]{6}$/.test(input.ifscCode.trim().toUpperCase())) {
    return "Enter a valid IFSC code.";
  }

  if (!input.bankName.trim()) {
    return "Bank name is required.";
  }

  return "";
}

function bankStatusLabel(status: FarmerBankDetailsStatus | undefined) {
  if (status === "VERIFIED") {
    return "Verified";
  }

  if (status === "REJECTED") {
    return "Rejected";
  }

  return "Pending";
}

function bankStatusTone(
  status: FarmerBankDetailsStatus | undefined
): "good" | "neutral" | "warning" | "danger" {
  if (status === "VERIFIED") {
    return "good";
  }

  if (status === "REJECTED") {
    return "danger";
  }

  return "warning";
}

function documentStatusLabel(status: FarmerDocumentStatus) {
  if (status === "VERIFIED") {
    return "Verified";
  }

  if (status === "REJECTED") {
    return "Rejected";
  }

  return "Pending";
}

function documentStatusTone(
  status: FarmerDocumentStatus
): "good" | "neutral" | "warning" | "danger" {
  if (status === "VERIFIED") {
    return "good";
  }

  if (status === "REJECTED") {
    return "danger";
  }

  return "warning";
}

function validateDocumentAsset(asset: {
  mimeType?: string | null;
  name?: string | null;
  size?: number | null;
}) {
  const mimeType = normalizePickedMimeType(asset.mimeType, asset.name);

  if (mimeType !== "application/pdf" && !mimeType.startsWith("image/")) {
    return "Upload an image or PDF document.";
  }

  if (!asset.size) {
    return "";
  }

  if (mimeType === "application/pdf" && asset.size > 10 * 1024 * 1024) {
    return "PDF documents must be 10 MB or smaller.";
  }

  if (mimeType.startsWith("image/") && asset.size > 5 * 1024 * 1024) {
    return "Image documents must be 5 MB or smaller.";
  }

  return "";
}

function normalizePickedMimeType(mimeType?: string | null, filename?: string | null) {
  const normalized = mimeType?.trim().toLowerCase();
  if (normalized) {
    return normalized;
  }

  const extension = filename?.split(".").pop()?.toLowerCase();
  switch (extension) {
    case "heic":
      return "image/heic";
    case "heif":
      return "image/heif";
    case "jpeg":
    case "jpg":
      return "image/jpeg";
    case "pdf":
      return "application/pdf";
    case "png":
      return "image/png";
    case "webp":
      return "image/webp";
    default:
      return "application/octet-stream";
  }
}

function PlotsSection({
  livePlots,
  snapshot
}: {
  livePlots: CarbonFarmPlotRecord[];
  snapshot: FarmerCarbonSnapshot;
}) {
  const [selectedPlotId, setSelectedPlotId] = useState<string | null>(null);
  const selectedPlot =
    livePlots.find((plot) => plot.id === selectedPlotId) ?? livePlots[0] ?? null;

  useEffect(() => {
    if (!livePlots.length) {
      setSelectedPlotId(null);
      return;
    }

    if (!selectedPlotId || !livePlots.some((plot) => plot.id === selectedPlotId)) {
      setSelectedPlotId(livePlots[0].id);
    }
  }, [livePlots, selectedPlotId]);

  return (
    <>
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={styles.cardHeaderText}>
            <Text style={styles.sectionTitle}>Vineyard plots</Text>
            <Text style={styles.cardDescription}>
              Plot list, add block, and block detail shell for screens 16-19.
            </Text>
          </View>
          <StatusBadge label={`${livePlots.length || 1} visible`} tone="neutral" />
        </View>
      </View>

      {livePlots.length ? (
        <>
          {livePlots.map((plot) => {
            const isSelected = selectedPlot?.id === plot.id;

            return (
              <Pressable
                key={plot.id}
                accessibilityRole="button"
                style={[styles.listRow, isSelected && styles.selectedPlotRow]}
                onPress={() => setSelectedPlotId(plot.id)}
              >
                <PlotMapPlaceholder compact plot={plot} />
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
                      plot.plantingDate
                        ? `Planted ${formatDate(plot.plantingDate)}`
                        : null
                    ]
                      .filter(Boolean)
                      .join(" - ") || "Vineyard details not set"}
                  </Text>
                </View>
                <StatusBadge
                  label={isSelected ? "Selected" : plot.status}
                  tone="neutral"
                />
              </Pressable>
            );
          })}
          {selectedPlot ? <PlotDetailCard plot={selectedPlot} /> : null}
        </>
      ) : (
        <View style={styles.listRow}>
          <View style={styles.rowText}>
            <Text style={styles.rowTitle}>Primary vineyard block</Text>
            <Text style={styles.rowMeta}>
              {snapshot.profile.totalLandHoldingAcres} ac -{" "}
              {snapshot.profile.gpsLocation}
            </Text>
            <Text style={styles.rowMeta}>
              Add/edit and map drawing are planned in CARBON-CLIENT-006 and 007.
            </Text>
          </View>
          <StatusBadge label="Placeholder" tone="warning" />
        </View>
      )}

      <StateCard
        message="Add block and block detail actions are wired as shell placeholders for this sprint."
        title="Plot actions"
        tone="info"
      />
    </>
  );
}

function PlotDetailCard({ plot }: { plot: CarbonFarmPlotRecord }) {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderText}>
          <Text style={styles.sectionTitle}>Plot detail</Text>
          <Text style={styles.cardDescription}>{plot.farmName}</Text>
        </View>
        <StatusBadge label={plot.status} tone="neutral" />
      </View>
      <PlotMapPlaceholder plot={plot} />
      <View style={styles.metricGrid}>
        <Metric label="Area" value={`${formatNumber(plot.areaAcres)} ac`} />
        <Metric label="GPS" value={`${plot.latitude}, ${plot.longitude}`} />
        <Metric label="Variety" value={plot.variety ?? "Not set"} />
        <Metric label="Rootstock" value={plot.rootstock ?? "Not set"} />
        <Metric label="Block" value={plot.blockCode ?? "Not set"} />
        <Metric label="Spacing" value={plot.spacing ?? "Not set"} />
        <Metric
          label="Rows"
          value={plot.rowCount !== undefined ? String(plot.rowCount) : "Not set"}
        />
        <Metric
          label="Planted"
          value={plot.plantingDate ? formatDate(plot.plantingDate) : "Not set"}
        />
      </View>
    </View>
  );
}

function PlotMapPlaceholder({
  compact = false,
  plot
}: {
  compact?: boolean;
  plot: CarbonFarmPlotRecord;
}) {
  return (
    <View style={[styles.mapPlaceholder, compact && styles.mapPlaceholderCompact]}>
      <View style={styles.mapGridLineHorizontal} />
      <View style={styles.mapGridLineVertical} />
      <View style={styles.mapPin} />
      <Text style={styles.mapLabel}>{plot.blockCode ?? plot.farmName}</Text>
      {!compact ? (
        <Text style={styles.mapMeta}>
          {plot.latitude}, {plot.longitude}
        </Text>
      ) : null}
    </View>
  );
}

type SoilScreenId = "dashboard" | "manual" | "upload";

const soilScreens: { id: SoilScreenId; label: string }[] = [
  { id: "dashboard", label: "Dashboard" },
  { id: "upload", label: "Report upload" },
  { id: "manual", label: "Manual entry" }
];

function SoilSection({
  livePlots,
  liveProfile,
  liveSoilProfiles,
  onSoilProfilesChanged,
  snapshot
}: {
  livePlots: CarbonFarmPlotRecord[];
  liveProfile: CarbonProfileRecord | null;
  liveSoilProfiles: CarbonSoilProfileRecord[];
  onSoilProfilesChanged: (profiles: CarbonSoilProfileRecord[]) => void;
  snapshot: FarmerCarbonSnapshot;
}) {
  const [activeSoilScreen, setActiveSoilScreen] =
    useState<SoilScreenId>("dashboard");
  const [draftSoilProfileId, setDraftSoilProfileId] = useState<string | null>(null);
  const [selectedSoilProfileId, setSelectedSoilProfileId] = useState<string | null>(
    null
  );
  const [soilError, setSoilError] = useState("");
  const [soilMessage, setSoilMessage] = useState("");
  const [savingMode, setSavingMode] = useState<"metadata" | "upload" | null>(null);

  const selectedSoilProfile =
    liveSoilProfiles.find((profile) => profile.id === selectedSoilProfileId) ??
    liveSoilProfiles[0] ??
    null;
  const draftSoilProfile =
    draftSoilProfileId === null
      ? null
      : liveSoilProfiles.find((profile) => profile.id === draftSoilProfileId) ??
        null;

  useEffect(() => {
    if (!liveSoilProfiles.length) {
      setSelectedSoilProfileId(null);
      setDraftSoilProfileId(null);
      return;
    }

    if (
      selectedSoilProfileId &&
      !liveSoilProfiles.some((profile) => profile.id === selectedSoilProfileId)
    ) {
      setSelectedSoilProfileId(liveSoilProfiles[0].id);
    }

    if (
      draftSoilProfileId &&
      !liveSoilProfiles.some((profile) => profile.id === draftSoilProfileId)
    ) {
      setDraftSoilProfileId(null);
    }
  }, [draftSoilProfileId, liveSoilProfiles, selectedSoilProfileId]);

  async function handleSaveSoilProfile(
    input: CarbonSoilProfileInput,
    profile: CarbonSoilProfileRecord | null
  ) {
    if (!liveProfile) {
      setSoilError("Carbon profile is not linked yet.");
      setSoilMessage("");
      return null;
    }

    setSavingMode("metadata");
    setSoilError("");
    setSoilMessage("");

    try {
      const saved = profile
        ? await updateCarbonSoilProfile(profile.id, input)
        : await createCarbonSoilProfile(liveProfile.id, input);

      onSoilProfilesChanged(upsertById(liveSoilProfiles, saved));
      setSelectedSoilProfileId(saved.id);
      setDraftSoilProfileId(saved.id);
      setActiveSoilScreen("dashboard");
      setSoilMessage("Soil profile saved.");
      return saved;
    } catch (error) {
      setSoilError(error instanceof Error ? error.message : "Unable to save soil profile.");
      return null;
    } finally {
      setSavingMode(null);
    }
  }

  async function handleUploadSoilReport(
    file: CarbonSoilReportFile,
    input: CarbonSoilProfileInput,
    profile: CarbonSoilProfileRecord | null
  ) {
    if (!liveProfile) {
      setSoilError("Carbon profile is not linked yet.");
      setSoilMessage("");
      return;
    }

    setSavingMode("upload");
    setSoilError("");
    setSoilMessage("");

    try {
      const targetProfile = profile
        ? await updateCarbonSoilProfile(profile.id, input)
        : await createCarbonSoilProfile(liveProfile.id, input);
      const saved = await uploadCarbonSoilReport(targetProfile.id, file);

      onSoilProfilesChanged(upsertById(liveSoilProfiles, saved));
      setSelectedSoilProfileId(saved.id);
      setDraftSoilProfileId(saved.id);
      setActiveSoilScreen("dashboard");
      setSoilMessage("Soil report uploaded.");
    } catch (error) {
      setSoilError(error instanceof Error ? error.message : "Unable to upload soil report.");
    } finally {
      setSavingMode(null);
    }
  }

  const dashboardRecords = liveSoilProfiles.map((profile) =>
    toSoilDashboardRecord(profile, livePlots)
  );

  return (
    <>
      <SectionTabs
        activeSection={activeSoilScreen}
        onChange={(section) => {
          setActiveSoilScreen(section);
          setSoilError("");
          setSoilMessage("");
          if (section === "upload") {
            setDraftSoilProfileId(selectedSoilProfile?.id ?? null);
          }
          if (section === "manual") {
            setDraftSoilProfileId(selectedSoilProfile?.id ?? null);
          }
        }}
        sections={soilScreens}
      />

      {soilError ? <StateCard message={soilError} title="Soil" tone="error" /> : null}
      {soilMessage ? (
        <StateCard message={soilMessage} title="Soil" tone="success" />
      ) : null}

      {activeSoilScreen === "dashboard" ? (
        <SoilProfileDashboard
          description="Latest soil values, report intake status, and SOC trend for vineyard blocks."
          emptyMessage="No soil profile is saved yet. Add a manual entry or upload a soil report to begin."
          fallbackMetrics={soilFallbackMetrics(snapshot)}
          records={dashboardRecords}
          selectedRecordId={selectedSoilProfile?.id}
          title="Soil dashboard"
          onSelectRecord={(record) => setSelectedSoilProfileId(record.id)}
          renderHeaderAction={() => (
            <View style={styles.inlineActions}>
              <Pressable
                accessibilityRole="button"
                style={styles.primaryButton}
                onPress={() => {
                  setDraftSoilProfileId(null);
                  setActiveSoilScreen("manual");
                }}
              >
                <Text style={styles.primaryButtonText}>Add manual entry</Text>
              </Pressable>
            </View>
          )}
          renderRecordAction={(record) => (
            <View style={styles.documentRowActions}>
              <Pressable
                accessibilityRole="button"
                style={styles.secondaryButton}
                onPress={() => {
                  setDraftSoilProfileId(record.id);
                  setActiveSoilScreen("upload");
                }}
              >
                <Text style={styles.secondaryButtonText}>Upload</Text>
              </Pressable>
              <Pressable
                accessibilityRole="button"
                style={styles.secondaryButton}
                onPress={() => {
                  setDraftSoilProfileId(record.id);
                  setActiveSoilScreen("manual");
                }}
              >
                <Text style={styles.secondaryButtonText}>Edit</Text>
              </Pressable>
            </View>
          )}
        />
      ) : null}

      {activeSoilScreen === "upload" ? (
        <CarbonSoilProfileEditor
          isSubmitting={savingMode === "metadata"}
          isUploadingFile={savingMode === "upload"}
          mode="upload"
          plots={livePlots}
          soilProfile={draftSoilProfile}
          onSave={handleSaveSoilProfile}
          onUpload={handleUploadSoilReport}
        />
      ) : null}

      {activeSoilScreen === "manual" ? (
        <CarbonSoilProfileEditor
          isSubmitting={savingMode === "metadata"}
          isUploadingFile={savingMode === "upload"}
          mode="manual"
          plots={livePlots}
          soilProfile={draftSoilProfile}
          onSave={handleSaveSoilProfile}
          onUpload={handleUploadSoilReport}
        />
      ) : null}
    </>
  );
}

function CarbonSoilProfileEditor({
  isSubmitting,
  isUploadingFile,
  mode,
  onSave,
  onUpload,
  plots,
  soilProfile
}: {
  isSubmitting: boolean;
  isUploadingFile: boolean;
  mode: "manual" | "upload";
  onSave: (
    input: CarbonSoilProfileInput,
    profile: CarbonSoilProfileRecord | null
  ) => Promise<CarbonSoilProfileRecord | null>;
  onUpload: (
    file: CarbonSoilReportFile,
    input: CarbonSoilProfileInput,
    profile: CarbonSoilProfileRecord | null
  ) => Promise<void>;
  plots: CarbonFarmPlotRecord[];
  soilProfile: CarbonSoilProfileRecord | null;
}) {
  const [carbonFarmPlotId, setCarbonFarmPlotId] = useState("");
  const initialValues = useMemo(
    () => toSoilReportValues(soilProfile),
    [soilProfile]
  );

  useEffect(() => {
    setCarbonFarmPlotId(soilProfile?.carbonFarmPlotId ?? "");
  }, [soilProfile]);

  const fields =
    mode === "upload"
      ? ([
          "testDate",
          "labName",
          "reportFileName",
          "reportUrl"
        ] as SoilReportField[])
      : ([
          "testDate",
          "soilOrganicCarbon",
          "ph",
          "nitrogen",
          "phosphorus",
          "potassium",
          "electricalConductivity",
          "bulkDensity",
          "texture",
          "labName",
          "reportFileName",
          "reportUrl"
        ] as SoilReportField[]);

  return (
    <SoilManualEntryForm
      badgeLabel={soilProfile ? "Editing saved record" : "New record"}
      badgeTone={soilProfile ? "neutral" : "warning"}
      buildInput={(values) => toCarbonSoilInput(values, carbonFarmPlotId)}
      childrenBeforeFields={
        plots.length ? (
          <View style={styles.optionBlock}>
            <Text style={styles.formLabel}>Vineyard block</Text>
            <View style={styles.choiceRow}>
              <Pressable
                accessibilityRole="button"
                style={[
                  styles.choiceButton,
                  !carbonFarmPlotId && styles.choiceButtonActive
                ]}
                onPress={() => setCarbonFarmPlotId("")}
              >
                <Text
                  style={[
                    styles.choiceButtonText,
                    !carbonFarmPlotId && styles.choiceButtonTextActive
                  ]}
                >
                  Profile level
                </Text>
              </Pressable>
              {plots.map((plot) => {
                const isSelected = carbonFarmPlotId === plot.id;

                return (
                  <Pressable
                    key={plot.id}
                    accessibilityRole="button"
                    style={[
                      styles.choiceButton,
                      isSelected && styles.choiceButtonActive
                    ]}
                    onPress={() => setCarbonFarmPlotId(plot.id)}
                  >
                    <Text
                      style={[
                        styles.choiceButtonText,
                        isSelected && styles.choiceButtonTextActive
                      ]}
                    >
                      {plot.blockCode ?? plot.farmName}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
        ) : null
      }
      description={
        mode === "upload"
          ? "Attach a lab PDF or image to a saved Carbon soil profile."
          : "Record SOC, pH, EC, NPK, texture, and lab metadata."
      }
      endpointPrefix="/api/v1/carbon/soil-profiles"
      fields={fields}
      formTitle={mode === "upload" ? "Report metadata" : "Manual soil values"}
      hasUploadedFile={Boolean(soilProfile?.reportStorageKey)}
      initialValues={initialValues}
      isSubmitting={isSubmitting}
      isUploadingFile={isUploadingFile}
      module="carbon"
      submitLabel={
        soilProfile
          ? mode === "upload"
            ? "Save report metadata"
            : "Update manual entry"
          : mode === "upload"
            ? "Create report record"
            : "Add manual entry"
      }
      title={mode === "upload" ? "Soil report upload" : "Soil manual entry"}
      uploadLabel={soilProfile ? "Upload report" : "Create and upload report"}
      onSubmit={(input) => onSave(input, soilProfile)}
      onUploadFile={(file, input) => onUpload(file, input, soilProfile)}
    />
  );
}

function toSoilReportValues(
  profile: CarbonSoilProfileRecord | null
): SoilReportValues {
  if (!profile) {
    return {};
  }

  return {
    bulkDensity: toOptionalInput(profile.bulkDensityGmCm3),
    electricalConductivity: toOptionalInput(profile.electricalConductivity),
    labName: profile.labName ?? "",
    nitrogen: toOptionalInput(profile.nitrogenKgHa),
    ph: toOptionalInput(profile.ph),
    phosphorus: toOptionalInput(profile.phosphorusKgHa),
    potassium: toOptionalInput(profile.potassiumKgHa),
    reportFileName: profile.reportFileName ?? "",
    reportUrl: profile.reportUrl ?? "",
    soilOrganicCarbon: toOptionalInput(profile.soilOrganicCarbonPercent),
    testDate: profile.testDate ?? "",
    texture: profile.texture ?? ""
  };
}

function toCarbonSoilInput(
  values: SoilReportValues,
  carbonFarmPlotId: string
): CarbonSoilProfileInput {
  const reportFileName = cleanOptional(values.reportFileName);

  return {
    bulkDensityGmCm3: cleanOptional(values.bulkDensity),
    carbonFarmPlotId: cleanOptional(carbonFarmPlotId),
    electricalConductivity: cleanOptional(values.electricalConductivity),
    labName: cleanOptional(values.labName),
    nitrogenKgHa: cleanOptional(values.nitrogen),
    ph: cleanOptional(values.ph),
    phosphorusKgHa: cleanOptional(values.phosphorus),
    potassiumKgHa: cleanOptional(values.potassium),
    reportContentType: reportFileName
      ? guessReportContentType(reportFileName)
      : undefined,
    reportFileName,
    reportUrl: cleanOptional(values.reportUrl),
    soilOrganicCarbonPercent: cleanOptional(values.soilOrganicCarbon),
    status: "ACTIVE",
    testDate: cleanOptional(values.testDate),
    texture: cleanOptional(values.texture)
  };
}

function toSoilDashboardRecord(
  profile: CarbonSoilProfileRecord,
  plots: CarbonFarmPlotRecord[]
): SoilDashboardRecord {
  const plot = plots.find((item) => item.id === profile.carbonFarmPlotId);
  const reportLabel = profile.reportFileName
    ? profile.reportStorageKey
      ? `Uploaded ${profile.reportFileName}`
      : profile.reportFileName
    : profile.reportUrl
      ? "Linked report URL"
      : "No report attached";

  return {
    id: profile.id,
    metaLines: [
      [
        metricLine("SOC", profile.soilOrganicCarbonPercent, "%"),
        metricLine("pH", profile.ph),
        metricLine("N", profile.nitrogenKgHa)
      ]
        .filter(Boolean)
        .join(" - ") || "Lab values not entered",
      profile.labName ?? "Lab not set"
    ],
    metrics: soilProfileMetrics(profile),
    reportLabel,
    socPercent: profile.soilOrganicCarbonPercent,
    statusLabel: profile.status,
    testDate: profile.testDate ? formatDate(profile.testDate) : undefined,
    title: plot?.farmName ?? profile.labName ?? "Profile level soil record"
  };
}

function soilFallbackMetrics(
  snapshot: FarmerCarbonSnapshot
): SoilDashboardMetric[] {
  return [
    { label: "SOC", value: `${snapshot.soilProfile.soilOrganicCarbonPercent}%` },
    { label: "pH", value: snapshot.soilProfile.ph },
    { label: "EC", value: snapshot.soilProfile.ec },
    { label: "Nitrogen", value: snapshot.soilProfile.nitrogenKgHa },
    { label: "Phosphorus", value: snapshot.soilProfile.phosphorusKgHa },
    { label: "Potassium", value: snapshot.soilProfile.potassiumKgHa }
  ];
}

function soilProfileMetrics(
  profile: CarbonSoilProfileRecord
): SoilDashboardMetric[] {
  return [
    { label: "SOC", value: valueOrUnset(profile.soilOrganicCarbonPercent, "%") },
    { label: "pH", value: valueOrUnset(profile.ph) },
    { label: "EC", value: valueOrUnset(profile.electricalConductivity) },
    { label: "Nitrogen", value: valueOrUnset(profile.nitrogenKgHa) },
    { label: "Phosphorus", value: valueOrUnset(profile.phosphorusKgHa) },
    { label: "Potassium", value: valueOrUnset(profile.potassiumKgHa) }
  ];
}

function cleanOptional(value: string | undefined) {
  const trimmed = value?.trim();
  return trimmed || undefined;
}

function toOptionalInput(value: number | undefined) {
  return value === undefined ? "" : String(value);
}

function valueOrUnset(value: number | undefined, suffix = "") {
  return value === undefined ? "Not set" : `${value}${suffix}`;
}

function metricLine(label: string, value: number | undefined, suffix = "") {
  return value === undefined ? "" : `${label} ${value}${suffix}`;
}

function guessReportContentType(fileName: string) {
  const extension = fileName.split(".").pop()?.toLowerCase();

  if (extension === "pdf") {
    return "application/pdf";
  }

  if (extension === "png") {
    return "image/png";
  }

  if (extension === "jpg" || extension === "jpeg") {
    return "image/jpeg";
  }

  if (extension === "webp") {
    return "image/webp";
  }

  return "application/octet-stream";
}

function ActivitiesSection({ snapshot }: { snapshot: FarmerCarbonSnapshot }) {
  return (
    <>
      <ActivityWizard
        domain="CARBON"
        participantName={snapshot.profile.farmerName}
        participantRegion={snapshot.profile.village}
      />

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Legacy carbon activity records</Text>
        <Text style={styles.cardDescription}>
          Existing sample records remain visible while new farmer submissions are saved
          through the generic activity and evidence tables.
        </Text>
        {snapshot.activities.map((activity) => (
          <View key={activity.id} style={styles.listRow}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{activity.category}</Text>
              <Text style={styles.rowMeta}>
                {activity.crop} - {activity.inputUsed} - {activity.quantity}
              </Text>
              <Text style={styles.rowMeta}>
                Score {activity.activityScore} - {activity.emissionReductionTco2e}{" "}
                tCO2e
              </Text>
            </View>
            <StatusBadge
              label={activity.verificationStatus}
              tone={activity.verificationStatus === "Verified" ? "good" : "warning"}
            />
          </View>
        ))}
      </View>
    </>
  );
}

function SectionTabs<T extends string>({
  activeSection,
  onChange,
  sections
}: {
  activeSection: T;
  onChange: (section: T) => void;
  sections: { id: T; label: string }[];
}) {
  return (
    <View style={styles.sectionTabs}>
      {sections.map((section) => {
        const isActive = activeSection === section.id;

        return (
          <Pressable
            key={section.id}
            accessibilityRole="button"
            style={[styles.sectionTab, isActive && styles.sectionTabActive]}
            onPress={() => onChange(section.id)}
          >
            <Text
              style={[styles.sectionTabText, isActive && styles.sectionTabTextActive]}
            >
              {section.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function QuickAction({
  label,
  meta,
  onPress
}: {
  label: string;
  meta: string;
  onPress: () => void;
}) {
  return (
    <Pressable accessibilityRole="button" style={styles.quickAction} onPress={onPress}>
      <Text style={styles.rowTitle}>{label}</Text>
      <Text style={styles.rowMeta}>{meta}</Text>
    </Pressable>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <View style={styles.metric}>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={styles.metricValue}>{value}</Text>
    </View>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function sum(values: number[]) {
  return Math.round(values.reduce((total, value) => total + value, 0) * 100) / 100;
}

function upsertById<T extends { id: string }>(items: T[], nextItem: T) {
  return [nextItem, ...items.filter((item) => item.id !== nextItem.id)];
}

const styles = StyleSheet.create({
  actionGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  card: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
  },
  cardDescription: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  cardGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
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
  cardMeta: {
    color: "#6d7f88",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 7
  },
  choiceButton: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    minHeight: 38,
    paddingHorizontal: 11,
    paddingVertical: 8
  },
  choiceButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  choiceButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  choiceButtonTextActive: {
    color: "#1f6f73"
  },
  choiceRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  completionBadge: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderColor: "#b8d7d5",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 52,
    minWidth: 72,
    paddingHorizontal: 12
  },
  completionStep: {
    alignItems: "flex-start",
    backgroundColor: "#f7fafb",
    borderRadius: 8,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 12
  },
  completionSteps: {
    gap: 8
  },
  completionValue: {
    color: "#1f6f73",
    fontSize: 20,
    fontWeight: "800"
  },
  documentList: {
    gap: 10
  },
  documentRowActions: {
    alignItems: "flex-end",
    gap: 8
  },
  documentTypeButton: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    minHeight: 40,
    paddingHorizontal: 12,
    paddingVertical: 9
  },
  documentTypeButtonSelected: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  documentTypeButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  documentTypeButtonTextSelected: {
    color: "#1f6f73"
  },
  documentTypeGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  buttonDisabled: {
    opacity: 0.65
  },
  formActions: {
    alignItems: "flex-start"
  },
  formField: {
    flex: 1,
    gap: 6,
    minWidth: 220
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  inlineActions: {
    alignItems: "flex-end",
    gap: 8
  },
  listRow: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 14
  },
  mapGridLineHorizontal: {
    backgroundColor: "#d9e4ea",
    height: 1,
    left: 0,
    position: "absolute",
    right: 0,
    top: "50%"
  },
  mapGridLineVertical: {
    backgroundColor: "#d9e4ea",
    bottom: 0,
    left: "50%",
    position: "absolute",
    top: 0,
    width: 1
  },
  mapLabel: {
    color: "#172126",
    fontSize: 12,
    fontWeight: "800",
    maxWidth: "88%",
    textAlign: "center"
  },
  mapMeta: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "700",
    marginTop: 4,
    textAlign: "center"
  },
  mapPin: {
    backgroundColor: "#1f6f73",
    borderColor: "#ffffff",
    borderRadius: 8,
    borderWidth: 2,
    height: 16,
    marginBottom: 8,
    width: 16
  },
  mapPlaceholder: {
    alignItems: "center",
    aspectRatio: 2.8,
    backgroundColor: "#eef7f7",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 112,
    overflow: "hidden",
    padding: 12,
    position: "relative",
    width: "100%"
  },
  mapPlaceholderCompact: {
    aspectRatio: 1,
    minHeight: 72,
    width: 76
  },
  metric: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    flex: 1,
    minWidth: 120,
    padding: 12
  },
  metricGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  metricLabel: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800",
    marginBottom: 4
  },
  metricValue: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  pageCopy: {
    color: "#53666f",
    fontSize: 15,
    lineHeight: 22
  },
  pageTitle: {
    color: "#172126",
    fontSize: 26,
    fontWeight: "800",
    marginBottom: 6
  },
  optionBlock: {
    gap: 8
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    paddingHorizontal: 16
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  quickAction: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 160,
    padding: 14
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
  section: {
    gap: 14
  },
  sectionTab: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 8
  },
  sectionTabActive: {
    backgroundColor: "#1f6f73",
    borderColor: "#1f6f73"
  },
  sectionTabs: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  sectionTabText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  sectionTabTextActive: {
    color: "#ffffff"
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  selectedPlotRow: {
    backgroundColor: "#eef7f7",
    borderColor: "#1f6f73",
    borderWidth: 2
  },
  secondaryDangerButton: {
    alignItems: "center",
    backgroundColor: "#fff4f2",
    borderColor: "#f0b8ad",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 34,
    paddingHorizontal: 12
  },
  secondaryDangerButtonText: {
    color: "#b53b2f",
    fontSize: 13,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 34,
    paddingHorizontal: 12
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  statCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 150,
    padding: 16
  },
  statLabel: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "700"
  },
  textInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 14,
    minHeight: 42,
    paddingHorizontal: 12,
    paddingVertical: 9
  },
  statsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  statValue: {
    color: "#172126",
    fontSize: 26,
    fontWeight: "800",
    marginBottom: 5
  }
});
