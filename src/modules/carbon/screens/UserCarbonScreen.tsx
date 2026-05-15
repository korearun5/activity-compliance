import { useEffect, useMemo, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import { AdvisoryRecord, getAdvisories } from "../../../data/advisoryStore";
import { StatusBadge } from "../../../ui/StatusBadge";
import { CarbonAdvisory, getFarmerCarbonSnapshot } from "../data/carbonStore";
import {
  CarbonFarmPlotRecord,
  CarbonProfileRecord,
  CarbonSoilProfileRecord,
  getMyCarbonProfile,
  listCarbonFarmPlots,
  listCarbonSoilProfiles
} from "../data/carbonProfileStore";

type UserCarbonScreenProps = {
  username: string | null;
};

type FarmerCarbonSnapshot = Awaited<ReturnType<typeof getFarmerCarbonSnapshot>>;

export function UserCarbonScreen({ username }: UserCarbonScreenProps) {
  const [snapshot, setSnapshot] = useState<FarmerCarbonSnapshot | null>(null);
  const [advisories, setAdvisories] = useState<AdvisoryRecord[]>([]);
  const [liveError, setLiveError] = useState("");
  const [livePlots, setLivePlots] = useState<CarbonFarmPlotRecord[]>([]);
  const [liveProfile, setLiveProfile] = useState<CarbonProfileRecord | null>(null);
  const [liveSoilProfiles, setLiveSoilProfiles] = useState<
    CarbonSoilProfileRecord[]
  >([]);

  useEffect(() => {
    async function loadCarbonData() {
      const nextSnapshot = await getFarmerCarbonSnapshot(username);
      setSnapshot(nextSnapshot);
      setLiveError("");

      try {
        setAdvisories(await getAdvisories({ status: "PUBLISHED" }));
      } catch {
        setAdvisories(nextSnapshot.advisories.map(toLocalAdvisoryRecord));
      }

      try {
        const nextProfile = await getMyCarbonProfile();
        const [nextPlots, nextSoilProfiles] = await Promise.all([
          listCarbonFarmPlots(nextProfile.id),
          listCarbonSoilProfiles(nextProfile.id)
        ]);

        setLiveProfile(nextProfile);
        setLivePlots(nextPlots);
        setLiveSoilProfiles(nextSoilProfiles);
      } catch (error) {
        setLiveProfile(null);
        setLivePlots([]);
        setLiveSoilProfiles([]);
        setLiveError(error instanceof Error ? error.message : "Carbon profile unavailable.");
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
        label: "Total farm area",
        value: `${formatNumber(liveArea > 0 ? liveArea : snapshot.profile.totalLandHoldingAcres)} ac`
      },
      {
        label: "Soil carbon score",
        value: primarySoilProfile?.soilOrganicCarbonPercent
          ? `${primarySoilProfile.soilOrganicCarbonPercent}% SOC`
          : String(snapshot.soilProfile.soilHealthScore)
      },
      {
        label: "Carbon credit potential",
        value: `${snapshot.soilProfile.carbonPotentialTco2e} tCO2e`
      },
      {
        label: "Farm activities pending",
        value: String(snapshot.pendingActivities)
      },
      {
        label: "Advisory alerts",
        value: String(advisories.length)
      },
      {
        label: "Weather snapshot",
        value: `${snapshot.weatherSnapshot.temperatureC} C`
      },
      {
        label: "Nearby dealers",
        value: String(snapshot.nearbyDealerCount)
      },
      {
        label: "Farm plots",
        value: String(livePlots.length)
      }
    ];
  }, [advisories.length, livePlots, liveProfile, liveSoilProfiles, snapshot]);

  if (!snapshot) {
    return (
      <View style={styles.section}>
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>Loading carbon dashboard...</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.section}>
      <View>
        <Text style={styles.pageTitle}>Carbon dashboard</Text>
        <Text style={styles.pageCopy}>
          Soil carbon, activity evidence, advisory, and nearby support for your
          Carbon program.
        </Text>
      </View>

      {liveError ? <Text style={styles.errorText}>{liveError}</Text> : null}

      <View style={styles.statsGrid}>
        {summary.map((item) => (
          <View key={item.label} style={styles.statCard}>
            <Text style={styles.statValue}>{item.value}</Text>
            <Text style={styles.statLabel}>{item.label}</Text>
          </View>
        ))}
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

      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={styles.cardHeaderText}>
            <Text style={styles.cardTitle}>
              {liveProfile?.carbonIdentityId ?? snapshot.profile.carbonIdentityId}
            </Text>
            <Text style={styles.cardDescription}>
              {liveProfile?.displayName ?? snapshot.profile.farmerName} -{" "}
              {[liveProfile?.village, liveProfile?.taluka].filter(Boolean).join(", ") ||
                `${snapshot.profile.village}, ${snapshot.profile.taluka}`}
            </Text>
            <Text style={styles.cardMeta}>
              {liveProfile?.tillageStatus ?? snapshot.profile.tillageStatus} -{" "}
              {liveProfile?.languagePreference ?? snapshot.profile.language}
            </Text>
          </View>
          <StatusBadge
            label={liveProfile?.bankStatus ?? snapshot.profile.bankStatus}
            tone={
              (liveProfile?.bankStatus ?? snapshot.profile.bankStatus) === "Linked"
                ? "good"
                : "warning"
            }
          />
        </View>
        <View style={styles.tagRow}>
          {(liveProfile
            ? [
                liveProfile.documentStatus ?? "Documents pending",
                liveProfile.aadhaarStatus ?? "Aadhaar optional",
                liveProfile.status
              ]
            : snapshot.profile.documents
          ).map((documentLabel) => (
            <View key={documentLabel} style={styles.tag}>
              <Text style={styles.tagText}>{documentLabel}</Text>
            </View>
          ))}
        </View>
      </View>

      {livePlots.length ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Farm plots</Text>
          {livePlots.map((plot) => (
            <View key={plot.id} style={styles.listRow}>
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
              <StatusBadge label={plot.status} tone="neutral" />
            </View>
          ))}
        </View>
      ) : null}

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Soil profile</Text>
        {liveSoilProfiles[0] ? (
          <View style={styles.metricGrid}>
            <Metric
              label="SOC"
              value={
                liveSoilProfiles[0].soilOrganicCarbonPercent !== undefined
                  ? `${liveSoilProfiles[0].soilOrganicCarbonPercent}%`
                  : "Not set"
              }
            />
            <Metric label="pH" value={liveSoilProfiles[0].ph ?? "Not set"} />
            <Metric
              label="EC"
              value={liveSoilProfiles[0].electricalConductivity ?? "Not set"}
            />
            <Metric
              label="Nitrogen"
              value={liveSoilProfiles[0].nitrogenKgHa ?? "Not set"}
            />
            <Metric
              label="Phosphorus"
              value={liveSoilProfiles[0].phosphorusKgHa ?? "Not set"}
            />
            <Metric
              label="Potassium"
              value={liveSoilProfiles[0].potassiumKgHa ?? "Not set"}
            />
            <Metric label="Texture" value={liveSoilProfiles[0].texture ?? "Not set"} />
            <Metric
              label="Report"
              value={liveSoilProfiles[0].reportFileName ?? "Not linked"}
            />
          </View>
        ) : (
          <>
            <View style={styles.metricGrid}>
              <Metric
                label="SOC"
                value={`${snapshot.soilProfile.soilOrganicCarbonPercent}%`}
              />
              <Metric label="pH" value={snapshot.soilProfile.ph} />
              <Metric label="EC" value={snapshot.soilProfile.ec} />
              <Metric label="NDVI" value={snapshot.soilProfile.ndvi} />
              <Metric label="Texture" value={snapshot.soilProfile.texture} />
              <Metric
                label="Microbial count"
                value={snapshot.soilProfile.microbialCount}
              />
            </View>
            <Text style={styles.cardMeta}>
              Recommended inputs: {snapshot.soilProfile.recommendedInputs.join(", ")}
            </Text>
          </>
        )}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Carbon activities</Text>
        {snapshot.activities.map((activity) => (
          <View key={activity.id} style={styles.listRow}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>{activity.category}</Text>
              <Text style={styles.rowMeta}>
                {activity.crop} - {activity.inputUsed} - {activity.quantity}
              </Text>
              <Text style={styles.rowMeta}>
                Score {activity.activityScore} - {activity.emissionReductionTco2e} tCO2e
              </Text>
            </View>
            <StatusBadge
              label={activity.verificationStatus}
              tone={activity.verificationStatus === "Verified" ? "good" : "warning"}
            />
          </View>
        ))}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Advisory alerts</Text>
        {advisories.length ? (
          advisories.map((advisory) => (
            <View key={advisory.id} style={styles.listRow}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>{advisory.title}</Text>
                <Text style={styles.rowMeta}>{advisory.message}</Text>
                <Text style={styles.rowMeta}>
                  {[advisory.cropName, advisory.seasonName]
                    .filter(Boolean)
                    .join(" / ") || "General advisory"}
                </Text>
              </View>
              <StatusBadge label="Published" tone="good" />
            </View>
          ))
        ) : (
          <Text style={styles.cardDescription}>No advisory alerts are available.</Text>
        )}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Nearby dealers and labs</Text>
        {snapshot.dealers.map((dealer) => (
          <View key={dealer.id} style={styles.listRow}>
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
    </View>
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

function toLocalAdvisoryRecord(advisory: CarbonAdvisory): AdvisoryRecord {
  return {
    category: "AGRONOMY",
    channel: "IN_APP",
    createdAt: advisory.createdAt,
    createdByName: "Demo advisory",
    id: advisory.id,
    images: [],
    message: advisory.message,
    publishedAt: advisory.createdAt,
    status: "PUBLISHED",
    targetType: "ALL_MEMBERS",
    title: advisory.title,
    updatedAt: advisory.createdAt
  };
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function sum(values: number[]) {
  return Math.round(values.reduce((total, value) => total + value, 0) * 100) / 100;
}

const styles = StyleSheet.create({
  section: {
    gap: 14
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
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 150,
    padding: 16
  },
  statValue: {
    color: "#172126",
    fontSize: 26,
    fontWeight: "800",
    marginBottom: 5
  },
  statLabel: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "700"
  },
  card: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  },
  errorText: {
    backgroundColor: "#fff1f0",
    borderColor: "#ffc9c4",
    borderRadius: 8,
    borderWidth: 1,
    color: "#a13a31",
    fontSize: 13,
    fontWeight: "700",
    padding: 10
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
  cardMeta: {
    color: "#6d7f88",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 7
  },
  tagRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  tag: {
    backgroundColor: "#e8f3f2",
    borderColor: "#b9d8d6",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 7
  },
  tagText: {
    color: "#1f6f73",
    fontSize: 12,
    fontWeight: "800"
  },
  metricGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  metric: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    flex: 1,
    minWidth: 120,
    padding: 12
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
  listRow: {
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
  }
});
