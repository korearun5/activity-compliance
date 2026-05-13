import { useEffect, useMemo, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import { CarbonProgramSnapshot, getCarbonProgramSnapshot } from "../data/carbonStore";
import { StatusBadge } from "../ui/StatusBadge";

export function AdminCarbonOverviewTab() {
  const [snapshot, setSnapshot] = useState<CarbonProgramSnapshot | null>(null);

  useEffect(() => {
    getCarbonProgramSnapshot().then(setSnapshot);
  }, []);

  const summaryCards = useMemo(
    () => [
      {
        label: "Farm area",
        value: `${formatNumber(snapshot?.totalFarmAreaAcres ?? 0)} ac`
      },
      {
        label: "Carbon potential",
        value: `${formatNumber(snapshot?.carbonCreditPotentialTco2e ?? 0)} tCO2e`
      },
      {
        label: "Pending verification",
        value: String(snapshot?.pendingActivities ?? 0)
      },
      {
        label: "Participating farmers",
        value: String(snapshot?.farmerParticipation ?? 0)
      }
    ],
    [snapshot]
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
        <View>
          <Text style={styles.title}>Carbon program base</Text>
          <Text style={styles.copy}>
            Regenerative agriculture, soil carbon, advisory, dealer, and report
            readiness using dummy records until client data is finalized.
          </Text>
        </View>

        <View style={styles.summaryGrid}>
          {summaryCards.map((item) => (
            <View key={item.label} style={styles.summaryCard}>
              <Text style={styles.summaryValue}>{item.value}</Text>
              <Text style={styles.summaryLabel}>{item.label}</Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.panel}>
        <Text style={styles.subsectionTitle}>Farmer carbon identity</Text>
        {snapshot.profiles.map((profile) => (
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
              label={profile.documents.length >= 3 ? "Docs ready" : "Docs partial"}
              tone={profile.documents.length >= 3 ? "good" : "warning"}
            />
          </View>
        ))}
      </View>

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
                Score {activity.activityScore} - {activity.emissionReductionTco2e} tCO2e
                reduction - {activity.evidenceCount} evidence item
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
    </View>
  );
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
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
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
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
