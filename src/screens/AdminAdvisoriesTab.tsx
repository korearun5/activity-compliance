import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import {
  AdvisoryInput,
  AdvisoryRecord,
  createAdvisory,
  getAdvisories,
  updateAdvisoryStatus
} from "../data/advisoryStore";
import {
  AdvisoryStatus,
  AdvisoryTargetType,
  NotificationChannel
} from "../core/api/fpoContracts";
import { getErrorMessage } from "../core/errors/AppError";
import {
  CropCatalog,
  CropSeason,
  getCropCatalog,
  getCropSeasons
} from "../data/cropPlanningStore";
import { FpoMember } from "../data/fpoMemberStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminAdvisoriesTabProps = {
  participants: FpoMember[];
};

const channelOptions: NotificationChannel[] = ["IN_APP", "PUSH", "SMS"];
const statusFilters: Array<AdvisoryStatus | "ALL"> = [
  "ALL",
  "DRAFT",
  "PUBLISHED",
  "ARCHIVED"
];
const targetOptions: AdvisoryTargetType[] = ["ALL_MEMBERS", "VILLAGE", "MEMBER"];

export function AdminAdvisoriesTab({ participants }: AdminAdvisoriesTabProps) {
  const [advisories, setAdvisories] = useState<AdvisoryRecord[]>([]);
  const [channel, setChannel] = useState<NotificationChannel>("IN_APP");
  const [crops, setCrops] = useState<CropCatalog[]>([]);
  const [cropId, setCropId] = useState("");
  const [error, setError] = useState("");
  const [filter, setFilter] = useState<AdvisoryStatus | "ALL">("ALL");
  const [isCreating, setIsCreating] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [seasons, setSeasons] = useState<CropSeason[]>([]);
  const [seasonId, setSeasonId] = useState("");
  const [status, setStatus] = useState<AdvisoryStatus>("DRAFT");
  const [targetMemberId, setTargetMemberId] = useState("");
  const [targetType, setTargetType] = useState<AdvisoryTargetType>("ALL_MEMBERS");
  const [targetVillage, setTargetVillage] = useState("");
  const [title, setTitle] = useState("");
  const [updatingAdvisoryId, setUpdatingAdvisoryId] = useState<string | null>(null);

  useEffect(() => {
    loadReferenceData();
  }, []);

  useEffect(() => {
    loadAdvisories(filter);
  }, [filter]);

  const villages = useMemo(
    () =>
      [
        ...new Set(
          participants.map((participant) => participant.village).filter(Boolean)
        )
      ].sort((left, right) => left.localeCompare(right)),
    [participants]
  );

  async function loadReferenceData() {
    try {
      const [cropRecords, seasonRecords] = await Promise.all([
        getCropCatalog(),
        getCropSeasons()
      ]);
      setCrops(cropRecords.filter((crop) => crop.status === "ACTIVE"));
      setSeasons(seasonRecords.filter((season) => season.status === "ACTIVE"));
    } catch {
      setCrops([]);
      setSeasons([]);
    }
  }

  async function loadAdvisories(nextFilter = filter) {
    setIsLoading(true);
    setError("");

    try {
      setAdvisories(
        await getAdvisories({
          status: nextFilter === "ALL" ? undefined : nextFilter
        })
      );
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load advisories."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreateAdvisory() {
    const input: AdvisoryInput = {
      channel,
      cropId,
      message,
      seasonId,
      status,
      targetMemberId,
      targetType,
      targetVillage,
      title
    };

    setIsCreating(true);
    setError("");

    try {
      const advisory = await createAdvisory(input);
      setAdvisories((current) => [
        advisory,
        ...current.filter((item) => item.id !== advisory.id)
      ]);
      setChannel("IN_APP");
      setCropId("");
      setMessage("");
      setSeasonId("");
      setStatus("DRAFT");
      setTargetMemberId("");
      setTargetType("ALL_MEMBERS");
      setTargetVillage("");
      setTitle("");
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to create advisory."));
    } finally {
      setIsCreating(false);
    }
  }

  async function handleStatusChange(
    advisory: AdvisoryRecord,
    nextStatus: AdvisoryStatus
  ) {
    setUpdatingAdvisoryId(advisory.id);
    setError("");

    try {
      const updated = await updateAdvisoryStatus(advisory, nextStatus);
      setAdvisories((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update advisory status."));
    } finally {
      setUpdatingAdvisoryId(null);
    }
  }

  return (
    <View style={styles.section}>
      <View style={styles.managementCard}>
        <Text style={styles.cardTitle}>Create advisory</Text>
        <View style={styles.formGrid}>
          <AdvisoryField label="Title" value={title} onChange={setTitle} />
          <AdvisoryField label="Message" value={message} onChange={setMessage} />
        </View>

        <Text style={styles.formLabel}>Target</Text>
        <View style={styles.choiceRow}>
          {targetOptions.map((option) => (
            <Pressable
              accessibilityRole="button"
              key={option}
              style={[
                styles.choiceButton,
                targetType === option && styles.choiceButtonActive
              ]}
              onPress={() => setTargetType(option)}
            >
              <Text
                style={[
                  styles.choiceButtonText,
                  targetType === option && styles.choiceButtonTextActive
                ]}
              >
                {targetTypeLabel(option)}
              </Text>
            </Pressable>
          ))}
        </View>

        {targetType === "VILLAGE" ? (
          <>
            <Text style={styles.formLabel}>Village</Text>
            <View style={styles.choiceRow}>
              {villages.map((village) => (
                <Pressable
                  accessibilityRole="button"
                  key={village}
                  style={[
                    styles.choiceButton,
                    targetVillage === village && styles.choiceButtonActive
                  ]}
                  onPress={() => setTargetVillage(village)}
                >
                  <Text
                    style={[
                      styles.choiceButtonText,
                      targetVillage === village && styles.choiceButtonTextActive
                    ]}
                  >
                    {village}
                  </Text>
                </Pressable>
              ))}
            </View>
            <AdvisoryField
              label="Custom village"
              value={targetVillage}
              onChange={setTargetVillage}
            />
          </>
        ) : null}

        {targetType === "MEMBER" ? (
          <>
            <Text style={styles.formLabel}>Member</Text>
            <View style={styles.choiceRow}>
              {participants.map((participant) => (
                <Pressable
                  accessibilityRole="button"
                  key={participant.memberId}
                  style={[
                    styles.choiceButton,
                    targetMemberId === participant.memberId && styles.choiceButtonActive
                  ]}
                  onPress={() => setTargetMemberId(participant.memberId)}
                >
                  <Text
                    style={[
                      styles.choiceButtonText,
                      targetMemberId === participant.memberId &&
                        styles.choiceButtonTextActive
                    ]}
                  >
                    {participant.name}
                  </Text>
                  <Text style={styles.choiceButtonMeta}>{participant.village}</Text>
                </Pressable>
              ))}
            </View>
          </>
        ) : null}

        <Text style={styles.formLabel}>Crop context</Text>
        <View style={styles.choiceRow}>
          <Pressable
            accessibilityRole="button"
            style={[styles.choiceButton, !cropId && styles.choiceButtonActive]}
            onPress={() => setCropId("")}
          >
            <Text
              style={[
                styles.choiceButtonText,
                !cropId && styles.choiceButtonTextActive
              ]}
            >
              Any crop
            </Text>
          </Pressable>
          {crops.map((crop) => (
            <Pressable
              accessibilityRole="button"
              key={crop.id}
              style={[
                styles.choiceButton,
                cropId === crop.id && styles.choiceButtonActive
              ]}
              onPress={() => setCropId(crop.id)}
            >
              <Text
                style={[
                  styles.choiceButtonText,
                  cropId === crop.id && styles.choiceButtonTextActive
                ]}
              >
                {crop.name}
              </Text>
            </Pressable>
          ))}
        </View>

        <Text style={styles.formLabel}>Season context</Text>
        <View style={styles.choiceRow}>
          <Pressable
            accessibilityRole="button"
            style={[styles.choiceButton, !seasonId && styles.choiceButtonActive]}
            onPress={() => setSeasonId("")}
          >
            <Text
              style={[
                styles.choiceButtonText,
                !seasonId && styles.choiceButtonTextActive
              ]}
            >
              Any season
            </Text>
          </Pressable>
          {seasons.map((season) => (
            <Pressable
              accessibilityRole="button"
              key={season.id}
              style={[
                styles.choiceButton,
                seasonId === season.id && styles.choiceButtonActive
              ]}
              onPress={() => setSeasonId(season.id)}
            >
              <Text
                style={[
                  styles.choiceButtonText,
                  seasonId === season.id && styles.choiceButtonTextActive
                ]}
              >
                {season.name}
              </Text>
              <Text style={styles.choiceButtonMeta}>{season.seasonYear}</Text>
            </Pressable>
          ))}
        </View>

        <View style={styles.formGrid}>
          <View style={styles.formBlock}>
            <Text style={styles.formLabel}>Channel</Text>
            <View style={styles.choiceRow}>
              {channelOptions.map((option) => (
                <Pressable
                  accessibilityRole="button"
                  key={option}
                  style={[
                    styles.choiceButton,
                    channel === option && styles.choiceButtonActive
                  ]}
                  onPress={() => setChannel(option)}
                >
                  <Text
                    style={[
                      styles.choiceButtonText,
                      channel === option && styles.choiceButtonTextActive
                    ]}
                  >
                    {channelLabel(option)}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>
          <View style={styles.formBlock}>
            <Text style={styles.formLabel}>Status</Text>
            <View style={styles.choiceRow}>
              {(["DRAFT", "PUBLISHED"] as AdvisoryStatus[]).map((option) => (
                <Pressable
                  accessibilityRole="button"
                  key={option}
                  style={[
                    styles.choiceButton,
                    status === option && styles.choiceButtonActive
                  ]}
                  onPress={() => setStatus(option)}
                >
                  <Text
                    style={[
                      styles.choiceButtonText,
                      status === option && styles.choiceButtonTextActive
                    ]}
                  >
                    {statusLabel(option)}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>
        </View>

        <View style={styles.formActions}>
          <Pressable
            accessibilityRole="button"
            disabled={isCreating}
            style={[styles.primaryButton, isCreating && styles.disabledButton]}
            onPress={handleCreateAdvisory}
          >
            <Text style={styles.primaryButtonText}>
              {isCreating ? "Creating..." : "Create advisory"}
            </Text>
          </Pressable>
        </View>
      </View>

      <View style={styles.headerRow}>
        <Text style={styles.sectionTitle}>Advisory records</Text>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={() => loadAdvisories()}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      <View style={styles.filterRow}>
        {statusFilters.map((filterOption) => (
          <Pressable
            accessibilityRole="button"
            key={filterOption}
            style={[
              styles.filterButton,
              filter === filterOption && styles.filterActive
            ]}
            onPress={() => setFilter(filterOption)}
          >
            <Text
              style={[
                styles.filterButtonText,
                filter === filterOption && styles.filterButtonTextActive
              ]}
            >
              {filterOption === "ALL" ? "All" : statusLabel(filterOption)}
            </Text>
          </Pressable>
        ))}
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      {advisories.length ? (
        advisories.map((advisory) => (
          <View key={advisory.id} style={styles.advisoryCard}>
            <View style={styles.advisoryText}>
              <Text style={styles.cardTitle}>{advisory.title}</Text>
              <Text style={styles.cardDescription}>{advisory.message}</Text>
              <Text style={styles.cardMeta}>
                {targetSummary(advisory)} - {channelLabel(advisory.channel)}
              </Text>
              {advisory.cropName || advisory.seasonName ? (
                <Text style={styles.cardMeta}>
                  {[advisory.cropName, advisory.seasonName].filter(Boolean).join(" / ")}
                </Text>
              ) : null}
            </View>
            <View style={styles.statusActions}>
              <StatusBadge
                label={statusLabel(advisory.status)}
                tone={statusTone(advisory.status)}
              />
              <View style={styles.actionRow}>
                {(["PUBLISHED", "ARCHIVED"] as AdvisoryStatus[]).map((nextStatus) => (
                  <Pressable
                    accessibilityRole="button"
                    disabled={
                      advisory.status === nextStatus ||
                      updatingAdvisoryId === advisory.id
                    }
                    key={nextStatus}
                    style={[
                      styles.statusButton,
                      (advisory.status === nextStatus ||
                        updatingAdvisoryId === advisory.id) &&
                        styles.disabledButton
                    ]}
                    onPress={() => handleStatusChange(advisory, nextStatus)}
                  >
                    <Text style={styles.statusButtonText}>
                      {statusLabel(nextStatus)}
                    </Text>
                  </Pressable>
                ))}
              </View>
            </View>
          </View>
        ))
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.cardDescription}>
            {isLoading ? "Loading advisories..." : "No advisory records found."}
          </Text>
        </View>
      )}
    </View>
  );
}

function AdvisoryField({
  label,
  onChange,
  value
}: {
  label: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <View style={styles.formField}>
      <Text style={styles.formLabel}>{label}</Text>
      <TextInput
        autoCapitalize="sentences"
        autoCorrect
        multiline={label === "Message"}
        numberOfLines={label === "Message" ? 4 : 1}
        onChangeText={onChange}
        style={[styles.formInput, label === "Message" && styles.messageInput]}
        textAlignVertical={label === "Message" ? "top" : "center"}
        value={value}
      />
    </View>
  );
}

function targetTypeLabel(targetType: AdvisoryTargetType) {
  switch (targetType) {
    case "MEMBER":
      return "Member";
    case "VILLAGE":
      return "Village";
    default:
      return "All members";
  }
}

function targetSummary(advisory: AdvisoryRecord) {
  if (advisory.targetType === "MEMBER") {
    return advisory.targetMemberName ?? "Member target";
  }

  if (advisory.targetType === "VILLAGE") {
    return advisory.targetVillage ?? "Village target";
  }

  return "All members";
}

function channelLabel(channel: NotificationChannel) {
  switch (channel) {
    case "PUSH":
      return "Push";
    case "SMS":
      return "SMS";
    case "EMAIL":
      return "Email";
    default:
      return "In app";
  }
}

function statusLabel(status: AdvisoryStatus) {
  switch (status) {
    case "ARCHIVED":
      return "Archived";
    case "PUBLISHED":
      return "Published";
    default:
      return "Draft";
  }
}

function statusTone(status: AdvisoryStatus) {
  switch (status) {
    case "PUBLISHED":
      return "good";
    case "ARCHIVED":
      return "neutral";
    default:
      return "warning";
  }
}

const styles = StyleSheet.create({
  section: {
    gap: 14
  },
  headerRow: {
    alignItems: "center",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  managementCard: {
    backgroundColor: "#ffffff",
    borderColor: "#cfe0df",
    borderRadius: 8,
    borderWidth: 1,
    gap: 14,
    padding: 16
  },
  advisoryCard: {
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
  advisoryText: {
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
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  formBlock: {
    flex: 1,
    gap: 8,
    minWidth: 220
  },
  formField: {
    flex: 1,
    gap: 7,
    minWidth: 230
  },
  formLabel: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  formInput: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 48,
    paddingHorizontal: 12
  },
  messageInput: {
    lineHeight: 21,
    minHeight: 96,
    paddingVertical: 10
  },
  choiceRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  choiceButton: {
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    minWidth: 122,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  choiceButtonActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  choiceButtonText: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  choiceButtonTextActive: {
    color: "#1f6f73"
  },
  choiceButtonMeta: {
    color: "#6d7f88",
    fontSize: 12,
    fontWeight: "700",
    marginTop: 4
  },
  formActions: {
    alignItems: "flex-start"
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 44,
    minWidth: 148,
    paddingHorizontal: 16
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 40,
    minWidth: 108,
    paddingHorizontal: 12
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  disabledButton: {
    opacity: 0.58
  },
  filterRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  filterButton: {
    alignItems: "center",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 92,
    paddingHorizontal: 10
  },
  filterActive: {
    backgroundColor: "#e8f3f2",
    borderColor: "#1f6f73"
  },
  filterButtonText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  filterButtonTextActive: {
    color: "#1f6f73"
  },
  statusActions: {
    alignItems: "flex-end",
    gap: 10
  },
  actionRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end",
    maxWidth: 220
  },
  statusButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 36,
    minWidth: 82,
    paddingHorizontal: 10
  },
  statusButtonText: {
    color: "#1f6f73",
    fontSize: 12,
    fontWeight: "800"
  },
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  emptyCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    padding: 16
  }
});
