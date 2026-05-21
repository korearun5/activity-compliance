import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { StateCard } from "../../../ui/StateCard";
import { StatusBadge } from "../../../ui/StatusBadge";
import { ProofSubmission } from "../../../data/agricultureConfig";
import { getBackendProofs, reviewBackendProof } from "../../../data/workflowActivityStore";
import {
  EvidenceReviewItem,
  EvidenceReviewQueue
} from "../../../shared/components/EvidenceReviewQueue";
import { ApprovalActions } from "../../../shared/components/ApprovalActions";
import { CarbonProgramSnapshot, getCarbonProgramSnapshot } from "../data/carbonStore";
import {
  CarbonProfileRecord,
  CarbonSoilProfileRecord,
  listPendingCarbonSoilProfiles,
  verifyCarbonSoilProfile
} from "../data/carbonProfileStore";
import { AdminBankVerification } from "./AdminBankVerification";
import { AdminDocumentVerification } from "./AdminDocumentVerification";
import { CarbonProfileAdminPanel } from "./CarbonProfileAdminPanel";

type AdminCarbonSectionId =
  | "activityVerification"
  | "dashboard"
  | "farmerVerification"
  | "farmers"
  | "soilVerification";

const adminCarbonSections: { id: AdminCarbonSectionId; label: string }[] = [
  { id: "dashboard", label: "Dashboard" },
  { id: "farmers", label: "Farmer management" },
  { id: "farmerVerification", label: "Farmer verification" },
  { id: "activityVerification", label: "Activity verification" },
  { id: "soilVerification", label: "Soil verification" }
];

type AdminCarbonOverviewTabProps = {
  onProfilesLoaded?: (profiles: CarbonProfileRecord[]) => void;
};

export function AdminCarbonOverviewTab({
  onProfilesLoaded
}: AdminCarbonOverviewTabProps) {
  const [activeSection, setActiveSection] =
    useState<AdminCarbonSectionId>("dashboard");
  const [liveProfiles, setLiveProfiles] = useState<CarbonProfileRecord[]>([]);
  const [snapshot, setSnapshot] = useState<CarbonProgramSnapshot | null>(null);

  useEffect(() => {
    getCarbonProgramSnapshot().then(setSnapshot);
  }, []);

  const totalFarmAreaAcres = useMemo(
    () =>
      liveProfiles.length
        ? round(
            liveProfiles.reduce(
              (sum, profile) => sum + (profile.totalLandHoldingAcres ?? 0),
              0
            )
          )
        : (snapshot?.totalFarmAreaAcres ?? 0),
    [liveProfiles, snapshot?.totalFarmAreaAcres]
  );

  const summaryCards = useMemo(
    () => [
      {
        label: "Farmers enrolled",
        value: String(liveProfiles.length || snapshot?.farmerParticipation || 0)
      },
      {
        label: "Total plot area",
        value: `${formatNumber(totalFarmAreaAcres)} ac`
      },
      {
        label: "Pending activities",
        value: String(snapshot?.pendingActivities ?? 0)
      },
      {
        label: "Soil carbon score",
        value: String(snapshot?.soilCarbonScore ?? 0)
      }
    ],
    [liveProfiles.length, snapshot, totalFarmAreaAcres]
  );

  if (!snapshot) {
    return (
      <View style={styles.section}>
        <StateCard message="Loading carbon admin workspace..." tone="empty" />
      </View>
    );
  }

  return (
    <View style={styles.section}>
      <View style={styles.panel}>
        <View style={styles.workspaceHeader}>
          <View style={styles.headerText}>
            <Text style={styles.title}>Carbon admin workspace</Text>
            <Text style={styles.copy}>
              P0 operations for farmer management, activity evidence, and soil review.
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            style={styles.primaryAction}
            onPress={() => setActiveSection("farmers")}
          >
            <Text style={styles.primaryActionText}>Add farmer</Text>
          </Pressable>
        </View>

        <SectionTabs
          activeSection={activeSection}
          onChange={setActiveSection}
          sections={adminCarbonSections}
        />
      </View>

      {activeSection === "dashboard" ? (
        <DashboardSection
          profileCount={liveProfiles.length || snapshot.farmerParticipation}
          snapshot={snapshot}
          summaryCards={summaryCards}
          totalFarmAreaAcres={totalFarmAreaAcres}
        />
      ) : null}

      {activeSection === "farmers" ? (
        <CarbonProfileAdminPanel
          onProfilesLoaded={(profiles) => {
            setLiveProfiles(profiles);
            onProfilesLoaded?.(profiles);
          }}
        />
      ) : null}

      {activeSection === "activityVerification" ? <ActivityVerificationSection /> : null}

      {activeSection === "farmerVerification" ? <FarmerVerificationSection /> : null}

      {activeSection === "soilVerification" ? <SoilVerificationSection /> : null}
    </View>
  );
}

function DashboardSection({
  profileCount,
  snapshot,
  summaryCards,
  totalFarmAreaAcres
}: {
  profileCount: number;
  snapshot: CarbonProgramSnapshot;
  summaryCards: Array<{ label: string; value: string }>;
  totalFarmAreaAcres: number;
}) {
  const journeySteps = [
    {
      meta: `${profileCount} farmer${profileCount === 1 ? "" : "s"}`,
      status: profileCount ? "Ready" : "Needs data",
      title: "1. Farmer management"
    },
    {
      meta: `${formatNumber(totalFarmAreaAcres)} ac captured`,
      status: totalFarmAreaAcres > 0 ? "Ready" : "Needs data",
      title: "2. Plots and soil"
    },
    {
      meta: `${snapshot.pendingActivities} pending`,
      status: snapshot.pendingActivities ? "Review" : "Ready",
      title: "3. Activity evidence"
    },
    {
      meta: `${snapshot.soilProfiles.length} soil profile records`,
      status: snapshot.soilProfiles.length ? "Ready" : "Needs data",
      title: "4. Soil verification"
    }
  ];

  return (
    <>
      <View style={styles.summaryGrid}>
        {summaryCards.map((item) => (
          <View key={item.label} style={styles.summaryCard}>
            <Text style={styles.summaryValue}>{item.value}</Text>
            <Text style={styles.summaryLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      <View style={styles.stepGrid}>
        {journeySteps.map((step) => (
          <View key={step.title} style={styles.stepCard}>
            <StatusBadge
              label={step.status}
              tone={step.status === "Ready" ? "good" : "warning"}
            />
            <Text style={styles.stepTitle}>{step.title}</Text>
            <Text style={styles.rowMeta}>{step.meta}</Text>
          </View>
        ))}
      </View>
    </>
  );
}

function FarmerVerificationSection() {
  return (
    <View style={styles.verificationStack}>
      <AdminBankVerification />
      <AdminDocumentVerification />
    </View>
  );
}

type CarbonEvidenceQueueItem = EvidenceReviewItem & {
  proof: ProofSubmission;
};

function ActivityVerificationSection() {
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [proofs, setProofs] = useState<ProofSubmission[]>([]);
  const [reviewingItemId, setReviewingItemId] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadActivityEvidence();
  }, []);

  async function loadActivityEvidence() {
    setIsLoading(true);
    setError("");

    try {
      setProofs(await getBackendProofs([], "CARBON"));
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "Unable to load Carbon activity evidence."
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function handleReview(
    item: CarbonEvidenceQueueItem,
    status: "APPROVED" | "REJECTED"
  ) {
    setReviewingItemId(item.id);
    setError("");
    setSuccessMessage("");

    try {
      await reviewBackendProof({ evidenceId: item.id, status });
      setSuccessMessage(
        `${item.title} ${status === "APPROVED" ? "approved" : "rejected"}.`
      );
      await loadActivityEvidence();
    } catch (reviewError) {
      setError(
        reviewError instanceof Error
          ? reviewError.message
          : "Unable to update activity evidence review."
      );
    } finally {
      setReviewingItemId(null);
    }
  }

  const queueItems: CarbonEvidenceQueueItem[] = proofs.map((proof) => ({
    description: [
      proof.farmer,
      proof.region,
      proof.unitName ? `Block ${proof.unitName}` : null
    ]
      .filter(Boolean)
      .join(" - "),
    id: proof.id,
    note: proof.note,
    proof,
    status: proof.status,
    submittedLabel: `Submitted ${proof.submittedOn}`,
    title: `${proof.crop} - ${proof.action}`
  }));

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View style={styles.headerText}>
          <Text style={styles.subsectionTitle}>Activity evidence queue</Text>
          <Text style={styles.panelMeta}>
            Carbon workflow evidence waiting for admin or FPO manager review.
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadActivityEvidence}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {successMessage ? <StateCard message={successMessage} tone="success" /> : null}

      <EvidenceReviewQueue
        canReview
        emptyMessage={
          isLoading
            ? "Loading Carbon activity evidence..."
            : "No Carbon activity evidence is pending review."
        }
        error={error}
        items={queueItems}
        module="carbon"
        reviewingItemId={reviewingItemId}
        onReview={handleReview}
      />
    </View>
  );
}

function SoilVerificationSection() {
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [notesById, setNotesById] = useState<Record<string, string>>({});
  const [savingId, setSavingId] = useState<string | null>(null);
  const [soilProfiles, setSoilProfiles] = useState<CarbonSoilProfileRecord[]>([]);
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadPendingSoilProfiles();
  }, []);

  async function loadPendingSoilProfiles() {
    setIsLoading(true);
    setError("");

    try {
      setSoilProfiles(await listPendingCarbonSoilProfiles());
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "Unable to load pending Carbon soil profiles."
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function handleVerify(
    profile: CarbonSoilProfileRecord,
    status: "VERIFIED" | "REJECTED"
  ) {
    setSavingId(profile.id);
    setError("");
    setSuccessMessage("");

    try {
      await verifyCarbonSoilProfile(profile.id, {
        notes: notesById[profile.id],
        status
      });
      setSuccessMessage(
        `${profile.profileName ?? "Soil profile"} ${status === "VERIFIED" ? "approved" : "rejected"}.`
      );
      setNotesById((current) => {
        const next = { ...current };
        delete next[profile.id];
        return next;
      });
      await loadPendingSoilProfiles();
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : "Unable to update soil verification."
      );
    } finally {
      setSavingId(null);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View style={styles.headerText}>
          <Text style={styles.subsectionTitle}>Soil verification queue</Text>
          <Text style={styles.panelMeta}>
            Uploaded reports and manual soil values that need lab review.
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadPendingSoilProfiles}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <StateCard message={error} tone="error" /> : null}
      {successMessage ? <StateCard message={successMessage} tone="success" /> : null}

      {soilProfiles.length ? (
        <View style={styles.table}>
          {soilProfiles.map((profile) => (
            <View key={profile.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>
                  {profile.profileName ?? "Carbon farmer"}
                </Text>
                <Text style={styles.rowMeta}>
                  {profile.farmName ?? "Profile level soil record"} -{" "}
                  {profile.profileMobileNumber ?? "Mobile not set"}
                </Text>
                <Text style={styles.rowMeta}>
                  {[
                    metricLine("SOC", profile.soilOrganicCarbonPercent, "%"),
                    metricLine("pH", profile.ph),
                    metricLine("N", profile.nitrogenKgHa)
                  ]
                    .filter(Boolean)
                    .join(" - ") || "Lab values not entered"}
                </Text>
                <Text style={styles.rowMeta}>
                  {profile.reportFileName ?? profile.reportUrl ?? "No report attached"}
                </Text>
              </View>
              <View style={styles.actionBlock}>
                <StatusBadge label="Pending" tone="warning" />
                <TextInput
                  multiline
                  onChangeText={(value) =>
                    setNotesById((current) => ({
                      ...current,
                      [profile.id]: value
                    }))
                  }
                  placeholder="Notes"
                  placeholderTextColor="#8a99a1"
                  style={styles.notesInput}
                  value={notesById[profile.id] ?? ""}
                />
                <ApprovalActions
                  isSubmitting={savingId === profile.id}
                  onApprove={() => handleVerify(profile, "VERIFIED")}
                  onReject={() => handleVerify(profile, "REJECTED")}
                />
              </View>
            </View>
          ))}
        </View>
      ) : (
        <StateCard
          message={
            isLoading
              ? "Loading pending soil profiles..."
              : "No Carbon soil profiles are pending verification."
          }
          tone="empty"
        />
      )}
    </View>
  );
}

function SectionTabs({
  activeSection,
  onChange,
  sections
}: {
  activeSection: AdminCarbonSectionId;
  onChange: (section: AdminCarbonSectionId) => void;
  sections: { id: AdminCarbonSectionId; label: string }[];
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

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function round(value: number) {
  return Math.round(value * 100) / 100;
}

function metricLine(label: string, value: number | undefined, suffix = "") {
  return value === undefined ? "" : `${label} ${value}${suffix}`;
}

const styles = StyleSheet.create({
  actionBlock: {
    alignItems: "flex-end",
    gap: 8,
    minWidth: 220
  },
  copy: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  disabledButton: {
    opacity: 0.55
  },
  headerText: {
    flex: 1
  },
  notesInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 13,
    minHeight: 40,
    paddingHorizontal: 10,
    paddingVertical: 8,
    width: "100%"
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
    flexWrap: "wrap",
    gap: 12,
    justifyContent: "space-between"
  },
  panelMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  primaryAction: {
    alignItems: "center",
    alignSelf: "flex-start",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 40,
    paddingHorizontal: 14,
    paddingVertical: 9
  },
  primaryActionText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  row: {
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
  section: {
    gap: 14
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
  stepCard: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    gap: 8,
    minWidth: 150,
    padding: 12
  },
  stepGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  stepTitle: {
    color: "#172126",
    fontSize: 13,
    fontWeight: "800"
  },
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  summaryCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 150,
    padding: 16
  },
  summaryGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  summaryLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  summaryValue: {
    color: "#1f6f73",
    fontSize: 24,
    fontWeight: "800",
    marginBottom: 4
  },
  title: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800",
    marginBottom: 6
  },
  table: {
    gap: 10
  },
  verificationStack: {
    gap: 14
  },
  workspaceHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    justifyContent: "space-between"
  }
});
