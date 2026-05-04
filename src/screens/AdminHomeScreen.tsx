import { useEffect, useMemo, useState } from "react";
import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View
} from "react-native";

import {
  countFarmerCycles,
  countFarmerProofs,
  getRegisteredFarmers,
  RegisteredFarmer
} from "../data/adminRegistryStore";
import { getSavedCropCycles } from "../data/cropCycleStore";
import { CropCycle, cropCycles, ProofSubmission } from "../data/farmDemoData";
import { getSavedProofs } from "../data/proofStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminHomeScreenProps = {
  onLogout: () => void;
};

type AdminTab = "overview" | "farmers" | "reports";

const demoFarmers: RegisteredFarmer[] = [
  {
    displayName: "Ravi Kumar",
    locationName: "North Block",
    username: "user",
    name: "Ravi Kumar",
    phone: "+91 98765 43210",
    region: "North Block",
    siteName: "Rampur",
    village: "Rampur",
    status: "Active"
  }
];

export function AdminHomeScreen({ onLogout }: AdminHomeScreenProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>("overview");
  const [registeredFarmers, setRegisteredFarmers] = useState<RegisteredFarmer[]>(
    []
  );
  const [savedCycles, setSavedCycles] = useState<CropCycle[]>([]);
  const [savedProofs, setSavedProofs] = useState<ProofSubmission[]>([]);

  useEffect(() => {
    async function loadAdminData() {
      const [farmers, cycles, proofs] = await Promise.all([
        getRegisteredFarmers(),
        getSavedCropCycles(),
        getSavedProofs()
      ]);

      setRegisteredFarmers(farmers);
      setSavedCycles(cycles);
      setSavedProofs(proofs);
    }

    loadAdminData();
  }, []);

  const farmers = registeredFarmers.length ? registeredFarmers : demoFarmers;
  const allCycles = [...savedCycles, ...cropCycles];
  const runningCycles = allCycles.filter((cycle) => cycle.status === "running");
  const completedCycles = allCycles.filter(
    (cycle) => cycle.status === "completed"
  );
  const proofRecords = savedProofs;
  const regions = [...new Set(farmers.map((farmer) => farmer.region))];
  const crops = [...new Set(allCycles.map((cycle) => cycle.crop))];
  const complianceScore = calculateComplianceScore(allCycles, proofRecords);

  const summary = useMemo(
    () => [
      { label: "Farmers", value: String(farmers.length) },
      { label: "Running cycles", value: String(runningCycles.length) },
      { label: "Completed", value: String(completedCycles.length) },
      { label: "Proof records", value: String(proofRecords.length) }
    ],
    [
      completedCycles.length,
      farmers.length,
      proofRecords.length,
      runningCycles.length
    ]
  );

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.shell}>
          <View style={styles.headerRow}>
            <View style={styles.headerText}>
              <Text style={styles.eyebrow}>Admin dashboard</Text>
              <Text style={styles.title}>Process Verification</Text>
              <Text style={styles.copy}>
                Track farmers, crop progress, proof records, and report-ready
                compliance evidence.
              </Text>
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

          <View style={styles.statsGrid}>
            {summary.map((item) => (
              <View key={item.label} style={styles.statCard}>
                <Text style={styles.statValue}>{item.value}</Text>
                <Text style={styles.statLabel}>{item.label}</Text>
              </View>
            ))}
          </View>

          <View style={styles.tabRow}>
            {[
              ["overview", "Overview"],
              ["farmers", "Farmers"],
              ["reports", "Reports"]
            ].map(([tab, label]) => (
              <Pressable
                key={tab}
                style={[
                  styles.tabButton,
                  activeTab === tab && styles.tabButtonActive
                ]}
                onPress={() => setActiveTab(tab as AdminTab)}
              >
                <Text
                  style={[
                    styles.tabButtonText,
                    activeTab === tab && styles.tabButtonTextActive
                  ]}
                >
                  {label}
                </Text>
              </Pressable>
            ))}
          </View>

          {activeTab === "overview" ? (
            <OverviewTab
              farmers={farmers}
              proofRecords={proofRecords}
              runningCycles={runningCycles}
            />
          ) : null}

          {activeTab === "farmers" ? (
            <FarmersTab
              allCycles={allCycles}
              farmers={farmers}
              proofRecords={proofRecords}
            />
          ) : null}

          {activeTab === "reports" ? (
            <ReportsTab
              complianceScore={complianceScore}
              crops={crops}
              proofRecords={proofRecords}
              regions={regions}
            />
          ) : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function OverviewTab({
  farmers,
  proofRecords,
  runningCycles
}: {
  farmers: RegisteredFarmer[];
  proofRecords: ProofSubmission[];
  runningCycles: CropCycle[];
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>Current field activity</Text>
      {runningCycles.map((cycle) => (
        <View key={cycle.id} style={styles.reviewCard}>
          <View style={styles.reviewText}>
            <Text style={styles.cardTitle}>{cycle.crop}</Text>
            <Text style={styles.cardDescription}>
              {cycle.region} - {cycle.plot} - harvest by{" "}
              {cycle.expectedHarvest}
            </Text>
            <Text style={styles.cardMeta}>{cycle.progress}% process progress</Text>
          </View>
          <StatusBadge label="Running" />
        </View>
      ))}

      <Text style={styles.sectionTitle}>Recent proof records</Text>
      {proofRecords.length ? (
        proofRecords.map((proof) => (
          <View key={proof.id} style={styles.reviewCard}>
            <View style={styles.reviewText}>
              <Text style={styles.cardTitle}>{proof.action}</Text>
              <Text style={styles.cardDescription}>
                {proof.farmer} - {proof.crop} - {proof.region}
              </Text>
              <Text style={styles.cardMeta}>Submitted {proof.submittedOn}</Text>
            </View>
            <StatusBadge label="Done" tone="good" />
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            No farmer proof has been submitted yet.
          </Text>
        </View>
      )}

      <Text style={styles.sectionTitle}>Farmers by region</Text>
      {farmers.map((farmer) => (
        <View key={farmer.username} style={styles.compactRow}>
          <Text style={styles.compactTitle}>{farmer.name}</Text>
          <Text style={styles.compactMeta}>
            {farmer.region} - {farmer.village}
          </Text>
        </View>
      ))}
    </View>
  );
}

function FarmersTab({
  allCycles,
  farmers,
  proofRecords
}: {
  allCycles: CropCycle[];
  farmers: RegisteredFarmer[];
  proofRecords: ProofSubmission[];
}) {
  return (
    <View style={styles.section}>
      <View style={styles.startCropCard}>
        <View style={styles.reviewText}>
          <Text style={styles.cardTitle}>Farmer management</Text>
          <Text style={styles.cardDescription}>
            Admin can view farmer profile basics, active crop work, completed
            cycles, and proof coverage. Add/edit/delete actions will connect to
            Spring Boot later.
          </Text>
        </View>
      </View>

      {farmers.map((farmer) => (
        <View key={farmer.username} style={styles.farmerCard}>
          <View style={styles.reviewText}>
            <Text style={styles.cardTitle}>{farmer.name}</Text>
            <Text style={styles.cardDescription}>
              {farmer.region} - {farmer.village} - {farmer.phone}
            </Text>
            <Text style={styles.cardMeta}>
              {countFarmerCycles(farmer, allCycles, "running")} running,{" "}
              {countFarmerCycles(farmer, allCycles, "completed")} completed,{" "}
              {countFarmerProofs(farmer, proofRecords)} proof record(s)
            </Text>
          </View>
          <StatusBadge
            label={farmer.status}
            tone={farmer.status === "Active" ? "good" : "warning"}
          />
        </View>
      ))}
    </View>
  );
}

function ReportsTab({
  complianceScore,
  crops,
  proofRecords,
  regions
}: {
  complianceScore: number;
  crops: string[];
  proofRecords: ProofSubmission[];
  regions: string[];
}) {
  return (
    <View style={styles.section}>
      <View style={styles.reportCard}>
        <Text style={styles.cardTitle}>Government evidence report</Text>
        <Text style={styles.cardDescription}>
          This report will show farmer coverage, crop-cycle progress, proof
          photo records, and process completion by region and crop.
        </Text>
        <View style={styles.reportGrid}>
          <View style={styles.reportMetric}>
            <Text style={styles.reportValue}>{complianceScore}%</Text>
            <Text style={styles.reportLabel}>Process compliance</Text>
          </View>
          <View style={styles.reportMetric}>
            <Text style={styles.reportValue}>{regions.length}</Text>
            <Text style={styles.reportLabel}>Regions covered</Text>
          </View>
          <View style={styles.reportMetric}>
            <Text style={styles.reportValue}>{proofRecords.length}</Text>
            <Text style={styles.reportLabel}>Proof records</Text>
          </View>
        </View>
      </View>

      <Text style={styles.sectionTitle}>Report filters</Text>
      {[
        `Region: ${regions.length ? regions.join(", ") : "No region yet"}`,
        `Crop type: ${crops.length ? crops.join(", ") : "No crop yet"}`,
        "Cycle status: running, completed, delayed",
        "Proof status: done, editable window closed",
        "Date range: sowing to harvest or custom period"
      ].map((item) => (
        <View key={item} style={styles.filterRow}>
          <Text style={styles.filterText}>{item}</Text>
        </View>
      ))}
    </View>
  );
}

function calculateComplianceScore(
  cycles: CropCycle[],
  proofs: ProofSubmission[]
) {
  const totalSteps = cycles.reduce((sum, cycle) => sum + cycle.steps.length, 0);

  if (!totalSteps) {
    return 0;
  }

  return Math.round((proofs.length / totalSteps) * 100);
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#f4f7fb"
  },
  scrollContent: {
    flexGrow: 1
  },
  shell: {
    width: "100%",
    maxWidth: 980,
    alignSelf: "center",
    paddingHorizontal: 20,
    paddingVertical: 28
  },
  headerRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 14,
    justifyContent: "space-between",
    marginBottom: 22
  },
  headerText: {
    flex: 1
  },
  eyebrow: {
    color: "#356b6f",
    fontSize: 13,
    fontWeight: "800",
    marginBottom: 8,
    textTransform: "uppercase"
  },
  title: {
    color: "#172126",
    fontSize: 34,
    fontWeight: "800",
    marginBottom: 10
  },
  copy: {
    color: "#53666f",
    fontSize: 16,
    lineHeight: 23,
    maxWidth: 680
  },
  logoutButton: {
    zIndex: 2,
    minHeight: 42,
    minWidth: 82,
    alignItems: "center",
    justifyContent: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 14
  },
  logoutButtonText: {
    color: "#1f6f73",
    fontSize: 14,
    fontWeight: "800"
  },
  statsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    marginBottom: 20
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
  tabRow: {
    backgroundColor: "#e8eef2",
    borderRadius: 8,
    flexDirection: "row",
    gap: 4,
    marginBottom: 20,
    padding: 4
  },
  tabButton: {
    alignItems: "center",
    borderRadius: 6,
    flex: 1,
    minHeight: 42,
    justifyContent: "center"
  },
  tabButtonActive: {
    backgroundColor: "#ffffff"
  },
  tabButtonText: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "800"
  },
  tabButtonTextActive: {
    color: "#172126"
  },
  section: {
    gap: 14
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800",
    marginTop: 4
  },
  reviewCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  },
  reviewText: {
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
  compactRow: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  compactTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800",
    marginBottom: 3
  },
  compactMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  },
  startCropCard: {
    alignItems: "center",
    backgroundColor: "#ffffff",
    borderColor: "#cfe0df",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 14,
    padding: 16
  },
  farmerCard: {
    alignItems: "flex-start",
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    padding: 16
  },
  reportCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  reportGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  reportMetric: {
    minWidth: 150,
    flex: 1,
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    padding: 14
  },
  reportValue: {
    color: "#1f6f73",
    fontSize: 26,
    fontWeight: "800",
    marginBottom: 4
  },
  reportLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  filterRow: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 14
  },
  filterText: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "700"
  }
});
