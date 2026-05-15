import { useEffect, useMemo, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import { AdvisoryRecord, getAdvisories } from "../data/advisoryStore";
import { CarbonAdvisory, getFarmerCarbonSnapshot } from "../data/carbonStore";
import { StatusBadge } from "../ui/StatusBadge";

type UserCarbonScreenProps = {
  username: string | null;
};

type FarmerCarbonSnapshot = Awaited<ReturnType<typeof getFarmerCarbonSnapshot>>;

export function UserCarbonScreen({ username }: UserCarbonScreenProps) {
  const [snapshot, setSnapshot] = useState<FarmerCarbonSnapshot | null>(null);
  const [advisories, setAdvisories] = useState<AdvisoryRecord[]>([]);

  useEffect(() => {
    async function loadCarbonData() {
      const nextSnapshot = await getFarmerCarbonSnapshot(username);
      setSnapshot(nextSnapshot);

      try {
        setAdvisories(await getAdvisories({ status: "PUBLISHED" }));
      } catch {
        setAdvisories(nextSnapshot.advisories.map(toLocalAdvisoryRecord));
      }
    }

    loadCarbonData();
  }, [username]);

  const summary = useMemo(() => {
    if (!snapshot) {
      return [];
    }

    return [
      {
        label: "Farm area",
        value: `${snapshot.profile.totalLandHoldingAcres} ac`
      },
      {
        label: "Soil score",
        value: String(snapshot.soilProfile.soilHealthScore)
      },
      {
        label: "Carbon potential",
        value: `${snapshot.soilProfile.carbonPotentialTco2e} tCO2e`
      },
      {
        label: "Advisories",
        value: String(advisories.length)
      }
    ];
  }, [advisories.length, snapshot]);

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
          Soil carbon, activity evidence, advisory, and nearby support are shown with
          demo values until live project data is connected.
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

      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={styles.cardHeaderText}>
            <Text style={styles.cardTitle}>{snapshot.profile.carbonIdentityId}</Text>
            <Text style={styles.cardDescription}>
              {snapshot.profile.farmerName} - {snapshot.profile.village},{" "}
              {snapshot.profile.taluka}
            </Text>
            <Text style={styles.cardMeta}>
              {snapshot.profile.tillageStatus} - {snapshot.profile.irrigationSource} -{" "}
              {snapshot.profile.language}
            </Text>
          </View>
          <StatusBadge
            label={snapshot.profile.bankStatus}
            tone={snapshot.profile.bankStatus === "Linked" ? "good" : "warning"}
          />
        </View>
        <View style={styles.tagRow}>
          {snapshot.profile.documents.map((document) => (
            <View key={document} style={styles.tag}>
              <Text style={styles.tagText}>{document}</Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Soil profile</Text>
        <View style={styles.metricGrid}>
          <Metric
            label="SOC"
            value={`${snapshot.soilProfile.soilOrganicCarbonPercent}%`}
          />
          <Metric label="pH" value={snapshot.soilProfile.ph} />
          <Metric label="EC" value={snapshot.soilProfile.ec} />
          <Metric label="NDVI" value={snapshot.soilProfile.ndvi} />
          <Metric label="Texture" value={snapshot.soilProfile.texture} />
          <Metric label="Microbial count" value={snapshot.soilProfile.microbialCount} />
        </View>
        <Text style={styles.cardMeta}>
          Recommended inputs: {snapshot.soilProfile.recommendedInputs.join(", ")}
        </Text>
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
