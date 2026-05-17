import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { StatusBadge } from "../../../ui/StatusBadge";
import { CarbonProgramSnapshot, getCarbonProgramSnapshot } from "../data/carbonStore";
import { CarbonProfileRecord } from "../data/carbonProfileStore";
import { CarbonProfileAdminPanel } from "./CarbonProfileAdminPanel";

type AdminCarbonSectionId = "overview" | "enrollment" | "evidence" | "marketplace";

const adminCarbonSections: { id: AdminCarbonSectionId; label: string }[] = [
  { id: "overview", label: "Overview" },
  { id: "enrollment", label: "Enrollment" },
  { id: "evidence", label: "Evidence" },
  { id: "marketplace", label: "Marketplace" }
];

export function AdminCarbonOverviewTab() {
  const [activeSection, setActiveSection] = useState<AdminCarbonSectionId>("overview");
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
        label: "Total farm area",
        value: `${formatNumber(totalFarmAreaAcres)} ac`
      },
      {
        label: "Soil carbon score",
        value: String(snapshot?.soilCarbonScore ?? 0)
      },
      {
        label: "Carbon credit potential",
        value: `${formatNumber(snapshot?.carbonCreditPotentialTco2e ?? 0)} tCO2e`
      },
      {
        label: "Farm activities pending",
        value: String(snapshot?.pendingActivities ?? 0)
      },
      {
        label: "Advisory alerts",
        value: String(snapshot?.advisoryAlertCount ?? 0)
      },
      {
        label: "Weather snapshot",
        value: `${snapshot?.weatherSnapshot.temperatureC ?? 0} C`
      },
      {
        label: "Nearby dealers",
        value: String(snapshot?.nearbyDealerCount ?? 0)
      },
      {
        label: "Farmers enrolled",
        value: String(liveProfiles.length || snapshot?.farmerParticipation || 0)
      }
    ],
    [liveProfiles.length, snapshot, totalFarmAreaAcres]
  );
  const profileCount = liveProfiles.length || snapshot?.farmerParticipation || 0;
  const journeySteps = useMemo(
    () => [
      {
        meta: `${profileCount} farmer${profileCount === 1 ? "" : "s"}`,
        status: profileCount ? "Ready" : "Needs data",
        title: "1. Enroll farmers"
      },
      {
        meta: `${formatNumber(totalFarmAreaAcres)} ac captured`,
        status: totalFarmAreaAcres > 0 ? "Ready" : "Needs data",
        title: "2. Add farms and soil"
      },
      {
        meta: `${snapshot?.pendingActivities ?? 0} pending`,
        status: snapshot?.pendingActivities ? "Review" : "Ready",
        title: "3. Track activities"
      },
      {
        meta: `${snapshot?.advisoryAlertCount ?? 0} published`,
        status: snapshot?.advisoryAlertCount ? "Ready" : "Draft",
        title: "4. Advisory and support"
      }
    ],
    [
      profileCount,
      snapshot?.advisoryAlertCount,
      snapshot?.pendingActivities,
      totalFarmAreaAcres
    ]
  );

  if (!snapshot) {
    return (
      <View style={styles.panel}>
        <Text style={styles.emptyText}>Loading carbon program base...</Text>
      </View>
    );
  }

  return (
    <View style={styles.section}>
      <View style={styles.panel}>
        <View style={styles.workspaceHeader}>
          <View style={styles.headerText}>
            <Text style={styles.title}>Carbon workspace</Text>
            <Text style={styles.copy}>
              Enroll farmers, capture farm and soil records, track carbon activities,
              and prepare advisory and marketplace support.
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            style={styles.primaryAction}
            onPress={() => setActiveSection("enrollment")}
          >
            <Text style={styles.primaryActionText}>Add farmer</Text>
          </Pressable>
        </View>

        <SectionTabs
          activeSection={activeSection}
          onChange={setActiveSection}
          sections={adminCarbonSections}
        />

        {activeSection === "overview" ? (
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

            <View style={styles.weatherCard}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>Weather snapshot</Text>
                <Text style={styles.rowMeta}>
                  {snapshot.weatherSnapshot.condition} -{" "}
                  {snapshot.weatherSnapshot.humidityPercent}% humidity - rain risk{" "}
                  {snapshot.weatherSnapshot.rainfallRisk}
                </Text>
                <Text style={styles.rowMeta}>{snapshot.weatherSnapshot.advisory}</Text>
              </View>
              <StatusBadge label={snapshot.weatherSnapshot.updatedAt} tone="neutral" />
            </View>
          </>
        ) : null}

        {activeSection === "enrollment" ? (
          <Text style={styles.copy}>
            Farmer carbon identity, farm plots, GPS points, soil reports, and enrollment
            readiness.
          </Text>
        ) : null}

        {activeSection === "evidence" ? (
          <Text style={styles.copy}>
            Soil profile review and carbon activity evidence are grouped here for
            operations users.
          </Text>
        ) : null}

        {activeSection === "marketplace" ? (
          <Text style={styles.copy}>
            Dealer and lab discovery stays separate from farmer enrollment and evidence
            review.
          </Text>
        ) : null}
      </View>

      {activeSection === "overview" ? (
        <View style={styles.panel}>
          <Text style={styles.subsectionTitle}>Farmer carbon identity</Text>
          {liveProfiles.length
            ? liveProfiles.map((profile) => (
                <View key={profile.id} style={styles.row}>
                  <View style={styles.rowText}>
                    <Text style={styles.rowTitle}>{profile.displayName}</Text>
                    <Text style={styles.rowMeta}>
                      {profile.carbonIdentityId} -{" "}
                      {[profile.village, profile.taluka].filter(Boolean).join(", ") ||
                        "Location not set"}
                    </Text>
                    <Text style={styles.rowMeta}>
                      {formatNumber(profile.totalLandHoldingAcres ?? 0)} ac -{" "}
                      {profile.tillageStatus ?? "Tillage not set"} -{" "}
                      {profile.bankStatus ?? "Bank not set"}
                    </Text>
                  </View>
                  <StatusBadge
                    label={profile.documentStatus ?? profile.status}
                    tone={profile.documentStatus === "Ready" ? "good" : "warning"}
                  />
                </View>
              ))
            : snapshot.profiles.map((profile) => (
                <View key={profile.id} style={styles.row}>
                  <View style={styles.rowText}>
                    <Text style={styles.rowTitle}>{profile.farmerName}</Text>
                    <Text style={styles.rowMeta}>
                      {profile.carbonIdentityId} - {profile.village}, {profile.taluka}
                    </Text>
                    <Text style={styles.rowMeta}>
                      {profile.totalLandHoldingAcres} ac - {profile.tillageStatus} -{" "}
                      {profile.bankStatus}
                    </Text>
                  </View>
                  <StatusBadge
                    label={
                      profile.documents.length >= 3 ? "Docs ready" : "Docs partial"
                    }
                    tone={profile.documents.length >= 3 ? "good" : "warning"}
                  />
                </View>
              ))}
        </View>
      ) : null}

      {activeSection === "enrollment" ? (
        <CarbonProfileAdminPanel onProfilesLoaded={setLiveProfiles} />
      ) : null}

      {activeSection === "evidence" ? (
        <>
          <View style={styles.panel}>
            <Text style={styles.subsectionTitle}>Soil profile pipeline</Text>
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
          </View>

          <View style={styles.panel}>
            <Text style={styles.subsectionTitle}>Activity verification queue</Text>
            {snapshot.activities.map((activity) => (
              <View key={activity.id} style={styles.row}>
                <View style={styles.rowText}>
                  <Text style={styles.rowTitle}>{activity.category}</Text>
                  <Text style={styles.rowMeta}>
                    {activity.crop} - {activity.inputUsed} - {activity.quantity}
                  </Text>
                  <Text style={styles.rowMeta}>
                    Score {activity.activityScore} - {activity.emissionReductionTco2e}{" "}
                    tCO2e reduction - {activity.evidenceCount} evidence item
                    {activity.evidenceCount === 1 ? "" : "s"}
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
      ) : null}

      {activeSection === "marketplace" ? (
        <View style={styles.panel}>
          <Text style={styles.subsectionTitle}>Dealer and lab directory</Text>
          {snapshot.dealers.map((dealer) => (
            <View key={dealer.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>{dealer.name}</Text>
                <Text style={styles.rowMeta}>
                  {dealer.category} - {dealer.distanceKm} km - rating {dealer.rating}
                </Text>
                <Text style={styles.rowMeta}>{dealer.products.join(", ")}</Text>
              </View>
              <StatusBadge
                label={dealer.stockStatus}
                tone={dealer.stockStatus === "Available" ? "good" : "warning"}
              />
            </View>
          ))}
        </View>
      ) : null}
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
  section: {
    gap: 14
  },
  panel: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  title: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800",
    marginBottom: 6
  },
  copy: {
    color: "#53666f",
    fontSize: 14,
    lineHeight: 20
  },
  headerText: {
    flex: 1
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
  summaryGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  summaryCard: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    flex: 1,
    minWidth: 132,
    padding: 14
  },
  summaryValue: {
    color: "#1f6f73",
    fontSize: 22,
    fontWeight: "800",
    marginBottom: 4
  },
  summaryLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
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
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  weatherCard: {
    alignItems: "flex-start",
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 12
  },
  workspaceHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    justifyContent: "space-between"
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
  rowText: {
    flex: 1
  },
  rowTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800",
    marginBottom: 4
  },
  rowMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    lineHeight: 18
  },
  emptyText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  }
});
