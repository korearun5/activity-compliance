import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { StateCard } from "../../../ui/StateCard";
import { StatusBadge } from "../../../ui/StatusBadge";
import { ActivityTimeline } from "../../../shared/components/ActivityTimeline";
import { CarbonProgramSnapshot, getCarbonProgramSnapshot } from "../data/carbonStore";
import { CarbonProfileRecord } from "../data/carbonProfileStore";
import { AdminBankVerification } from "./AdminBankVerification";
import { CarbonProfileAdminPanel } from "./CarbonProfileAdminPanel";

type AdminCarbonSectionId =
  | "activityVerification"
  | "bankVerification"
  | "dashboard"
  | "farmers"
  | "soilVerification";

const adminCarbonSections: { id: AdminCarbonSectionId; label: string }[] = [
  { id: "dashboard", label: "Dashboard" },
  { id: "farmers", label: "Farmer management" },
  { id: "bankVerification", label: "Bank verification" },
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

      {activeSection === "activityVerification" ? (
        <ActivityVerificationSection snapshot={snapshot} />
      ) : null}

      {activeSection === "bankVerification" ? <AdminBankVerification /> : null}

      {activeSection === "soilVerification" ? (
        <SoilVerificationSection snapshot={snapshot} />
      ) : null}
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

function ActivityVerificationSection({
  snapshot
}: {
  snapshot: CarbonProgramSnapshot;
}) {
  const timelineItems = snapshot.activities.map((activity) => ({
    id: activity.id,
    metaLines: [
      `${activity.crop} - ${activity.inputUsed} - ${activity.quantity}`,
      `Score ${activity.activityScore} - ${activity.emissionReductionTco2e} tCO2e reduction`,
      `${activity.evidenceCount} evidence item${
        activity.evidenceCount === 1 ? "" : "s"
      }`
    ],
    statusLabel: activity.verificationStatus,
    statusTone: activity.verificationStatus === "Verified" ? "good" as const : "warning" as const,
    title: activity.category
  }));

  return (
    <View style={styles.panel}>
      <Text style={styles.subsectionTitle}>Activity verification queue</Text>
      <ActivityTimeline
        emptyMessage="No Carbon activities are waiting for verification."
        items={timelineItems}
      />
      <StateCard
        message="Approve and reject actions will use the shared evidence review path in CARBON-CLIENT-011."
        tone="info"
      />
    </View>
  );
}

function SoilVerificationSection({ snapshot }: { snapshot: CarbonProgramSnapshot }) {
  return (
    <View style={styles.panel}>
      <Text style={styles.subsectionTitle}>Soil verification queue</Text>
      {snapshot.soilProfiles.map((profile) => (
        <View key={profile.id} style={styles.row}>
          <View style={styles.rowText}>
            <Text style={styles.rowTitle}>{profile.plotName}</Text>
            <Text style={styles.rowMeta}>
              SOC {profile.soilOrganicCarbonPercent}% - pH {profile.ph} - NDVI{" "}
              {profile.ndvi}
            </Text>
            <Text style={styles.rowMeta}>
              {profile.carbonPotentialTco2e} tCO2e potential -{" "}
              {profile.recommendedInputs.join(", ")}
            </Text>
          </View>
          <StatusBadge
            label={`${profile.soilHealthScore}`}
            tone={profile.soilHealthScore >= 75 ? "good" : "warning"}
          />
        </View>
      ))}
      <StateCard
        message="OCR and manual verification status will be added with the soil sprint."
        tone="info"
      />
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

const styles = StyleSheet.create({
  copy: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  headerText: {
    flex: 1
  },
  panel: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
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
  workspaceHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    justifyContent: "space-between"
  }
});
