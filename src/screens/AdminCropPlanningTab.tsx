import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { FarmRecordStatus, CropPlanStatus } from "../core/api/fpoContracts";
import { getErrorMessage } from "../core/errors/AppError";
import type { StaffRole } from "../auth/roleAccess";
import {
  createCropCatalog,
  createCropHistory,
  createCropPlan,
  createCropSeason,
  CropCatalog,
  CropCatalogInput,
  CropHistory,
  CropHistoryInput,
  CropPlan,
  CropPlanInput,
  CropSeason,
  CropSeasonInput,
  getCropCatalog,
  getCropHistory,
  getCropPlans,
  getCropSeasons,
  updateCropSeasonStatus,
  updateCropStatus,
  updateCropPlanStatus
} from "../data/cropPlanningStore";
import { FpoMember } from "../data/fpoMemberStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminCropPlanningTabProps = {
  currentRole: StaffRole;
  members: FpoMember[];
};

const planStatuses: CropPlanStatus[] = ["DRAFT", "CONFIRMED", "COMPLETED", "CANCELLED"];

export function AdminCropPlanningTab({
  currentRole,
  members
}: AdminCropPlanningTabProps) {
  const [cropHistory, setCropHistory] = useState<CropHistory[]>([]);
  const [crops, setCrops] = useState<CropCatalog[]>([]);
  const [error, setError] = useState("");
  const [filterCropId, setFilterCropId] = useState("");
  const [filterSeasonId, setFilterSeasonId] = useState("");
  const [filterStatus, setFilterStatus] = useState<CropPlanStatus | "">("");
  const [isLoading, setIsLoading] = useState(false);
  const [plans, setPlans] = useState<CropPlan[]>([]);
  const [savingKey, setSavingKey] = useState<string | null>(null);
  const [seasons, setSeasons] = useState<CropSeason[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState("");
  const canManageMasterData = currentRole === "admin" || currentRole === "fpoManager";

  const activeCrops = useMemo(
    () => crops.filter((crop) => crop.status === "ACTIVE"),
    [crops]
  );
  const activeSeasons = useMemo(
    () => seasons.filter((season) => season.status === "ACTIVE"),
    [seasons]
  );
  const selectedMember = members.find((member) => member.memberId === selectedMemberId);
  const membersById = useMemo(
    () =>
      new Map(
        members.map((member) => [
          member.memberId,
          {
            memberNumber: member.memberNumber,
            name: member.name,
            village: member.village
          }
        ])
      ),
    [members]
  );

  const visiblePlans = useMemo(
    () =>
      plans.filter(
        (plan) =>
          (!filterCropId || plan.cropId === filterCropId) &&
          (!filterSeasonId || plan.seasonId === filterSeasonId) &&
          (!filterStatus || plan.status === filterStatus)
      ),
    [filterCropId, filterSeasonId, filterStatus, plans]
  );

  const summary = useMemo(() => {
    const plannedArea = visiblePlans.reduce(
      (total, plan) => total + plan.plannedAreaAcres,
      0
    );
    const memberCount = new Set(visiblePlans.map((plan) => plan.memberId)).size;
    const villageCount = new Set(visiblePlans.map((plan) => plan.memberVillage)).size;

    return [
      { label: "Planned acres", value: formatNumber(plannedArea) },
      { label: "Farmers", value: String(memberCount) },
      { label: "Villages", value: String(villageCount) },
      { label: "Plans", value: String(visiblePlans.length) }
    ];
  }, [visiblePlans]);

  useEffect(() => {
    if (!members.length) {
      setSelectedMemberId("");
      return;
    }

    if (!members.some((member) => member.memberId === selectedMemberId)) {
      setSelectedMemberId(members[0].memberId);
    }
  }, [members, selectedMemberId]);

  useEffect(() => {
    loadCropPlanning();
  }, []);

  useEffect(() => {
    if (selectedMemberId) {
      loadMemberCropHistory(selectedMemberId);
    }
  }, [selectedMemberId]);

  async function loadCropPlanning() {
    setIsLoading(true);
    setError("");

    try {
      const [nextCrops, nextSeasons, nextPlans] = await Promise.all([
        getCropCatalog(),
        getCropSeasons(),
        getCropPlans()
      ]);
      setCrops(nextCrops);
      setSeasons(nextSeasons);
      setPlans(nextPlans);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load crop planning records."));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadMemberCropHistory(memberId: string) {
    setError("");

    try {
      setCropHistory(await getCropHistory(memberId));
    } catch (historyError) {
      setError(getErrorMessage(historyError, "Unable to load crop history."));
    }
  }

  async function handleCreateCrop(input: CropCatalogInput) {
    setSavingKey("crop:create");
    setError("");

    try {
      const crop = await createCropCatalog(input);
      setCrops((current) => [crop, ...current.filter((item) => item.id !== crop.id)]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save crop."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCropStatus(crop: CropCatalog, status: FarmRecordStatus) {
    setSavingKey(`crop:${crop.id}`);
    setError("");

    try {
      const updated = await updateCropStatus(crop, status);
      setCrops((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update crop status."));
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCreateSeason(input: CropSeasonInput) {
    setSavingKey("season:create");
    setError("");

    try {
      const season = await createCropSeason(input);
      setSeasons((current) => [
        season,
        ...current.filter((item) => item.id !== season.id)
      ]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save crop season."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleSeasonStatus(season: CropSeason, status: FarmRecordStatus) {
    setSavingKey(`season:${season.id}`);
    setError("");

    try {
      const updated = await updateCropSeasonStatus(season, status);
      setSeasons((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update season status."));
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCreateHistory(input: CropHistoryInput) {
    if (!selectedMember) {
      setError("Select a member before saving crop history.");
      return false;
    }

    setSavingKey("history:create");
    setError("");

    try {
      const history = await createCropHistory(
        selectedMember.memberId,
        selectedMember.memberNumber,
        selectedMember.name,
        input,
        crops,
        seasons
      );
      setCropHistory((current) => [
        history,
        ...current.filter((item) => item.id !== history.id)
      ]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save crop history."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCreatePlan(input: CropPlanInput) {
    setSavingKey("plan:create");
    setError("");

    try {
      const plan = await createCropPlan(input, membersById, crops, seasons);
      setPlans((current) => [plan, ...current.filter((item) => item.id !== plan.id)]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save seasonal crop plan."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handlePlanStatus(plan: CropPlan, status: CropPlanStatus) {
    setSavingKey(`plan:${plan.id}:${status}`);
    setError("");

    try {
      const updated = await updateCropPlanStatus(plan, status);
      setPlans((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update plan status."));
    } finally {
      setSavingKey(null);
    }
  }

  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={styles.headerText}>
          <Text style={styles.sectionTitle}>Crop planning</Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadCropPlanning}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      <View style={styles.summaryGrid}>
        {summary.map((item) => (
          <View key={item.label} style={styles.summaryCard}>
            <Text style={styles.summaryValue}>{item.value}</Text>
            <Text style={styles.summaryLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      {canManageMasterData ? (
        <CropCatalogForm
          isSubmitting={savingKey === "crop:create"}
          onSubmit={handleCreateCrop}
        />
      ) : null}
      <CropCatalogList
        canManage={canManageMasterData}
        crops={crops}
        onChangeStatus={handleCropStatus}
        savingKey={savingKey}
      />

      {canManageMasterData ? (
        <CropSeasonForm
          isSubmitting={savingKey === "season:create"}
          onSubmit={handleCreateSeason}
        />
      ) : null}
      <CropSeasonList
        canManage={canManageMasterData}
        onChangeStatus={handleSeasonStatus}
        savingKey={savingKey}
        seasons={seasons}
      />

      <MemberChoice
        members={members}
        selectedMemberId={selectedMemberId}
        onSelect={setSelectedMemberId}
      />

      <CropHistoryForm
        crops={activeCrops}
        isSubmitting={savingKey === "history:create"}
        onSubmit={handleCreateHistory}
        seasons={activeSeasons}
      />
      <CropHistoryList history={cropHistory} />

      <CropPlanFilters
        crops={crops}
        filterCropId={filterCropId}
        filterSeasonId={filterSeasonId}
        filterStatus={filterStatus}
        onFilterCropId={setFilterCropId}
        onFilterSeasonId={setFilterSeasonId}
        onFilterStatus={setFilterStatus}
        seasons={seasons}
      />

      <CropPlanForm
        crops={activeCrops}
        isSubmitting={savingKey === "plan:create"}
        members={members}
        onSubmit={handleCreatePlan}
        selectedMemberId={selectedMemberId}
        seasons={activeSeasons}
      />
      <CropPlanList
        onChangeStatus={handlePlanStatus}
        plans={visiblePlans}
        savingKey={savingKey}
      />
    </View>
  );
}

function CropCatalogForm({
  isSubmitting,
  onSubmit
}: {
  isSubmitting: boolean;
  onSubmit: (input: CropCatalogInput) => Promise<boolean>;
}) {
  const [category, setCategory] = useState("");
  const [code, setCode] = useState("");
  const [localError, setLocalError] = useState("");
  const [name, setName] = useState("");

  async function handleSubmit() {
    if (!code.trim() || !name.trim()) {
      setLocalError("Enter crop code and name.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      category: category.trim(),
      code: code.trim(),
      name: name.trim()
    });

    if (created) {
      setCategory("");
      setCode("");
      setName("");
    }
  }

  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Crop catalog</Text>
      <View style={styles.formGrid}>
        <PlanningField label="Crop code" value={code} onChange={setCode} />
        <PlanningField label="Crop name" value={name} onChange={setName} />
        <PlanningField label="Category" value={category} onChange={setCategory} />
      </View>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
        onPress={handleSubmit}
      >
        <Text style={styles.primaryButtonText}>
          {isSubmitting ? "Saving..." : "Add crop"}
        </Text>
      </Pressable>
    </View>
  );
}

function CropCatalogList({
  canManage,
  crops,
  onChangeStatus,
  savingKey
}: {
  canManage: boolean;
  crops: CropCatalog[];
  onChangeStatus: (crop: CropCatalog, status: FarmRecordStatus) => Promise<void>;
  savingKey: string | null;
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Saved crops</Text>
      {crops.length ? (
        crops.map((crop) => {
          const nextStatus = crop.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";
          const isSaving = savingKey === `crop:${crop.id}`;

          return (
            <View key={crop.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>
                  {crop.name} ({crop.code})
                </Text>
                <Text style={styles.rowMeta}>{crop.category || "No category"}</Text>
              </View>
              {canManage ? (
                <RowStatusActions
                  isSaving={isSaving}
                  onPress={() => onChangeStatus(crop, nextStatus)}
                  status={crop.status}
                />
              ) : (
                <StatusBadge
                  label={recordStatusLabel(crop.status)}
                  tone={recordStatusTone(crop.status)}
                />
              )}
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No crops saved yet.</Text>
      )}
    </View>
  );
}

function CropSeasonForm({
  isSubmitting,
  onSubmit
}: {
  isSubmitting: boolean;
  onSubmit: (input: CropSeasonInput) => Promise<boolean>;
}) {
  const [code, setCode] = useState("");
  const [endMonth, setEndMonth] = useState("");
  const [localError, setLocalError] = useState("");
  const [name, setName] = useState("");
  const [seasonYear, setSeasonYear] = useState(String(new Date().getFullYear()));
  const [startMonth, setStartMonth] = useState("");

  async function handleSubmit() {
    if (!code.trim() || !name.trim() || !seasonYear.trim()) {
      setLocalError("Enter season code, name, and year.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      code: code.trim(),
      endMonth: endMonth.trim(),
      name: name.trim(),
      seasonYear: seasonYear.trim(),
      startMonth: startMonth.trim()
    });

    if (created) {
      setCode("");
      setEndMonth("");
      setName("");
      setStartMonth("");
    }
  }

  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Season setup</Text>
      <View style={styles.formGrid}>
        <PlanningField label="Season code" value={code} onChange={setCode} />
        <PlanningField label="Season name" value={name} onChange={setName} />
        <PlanningField
          keyboardType="numeric"
          label="Year"
          value={seasonYear}
          onChange={setSeasonYear}
        />
        <PlanningField
          keyboardType="numeric"
          label="Start month"
          value={startMonth}
          onChange={setStartMonth}
        />
        <PlanningField
          keyboardType="numeric"
          label="End month"
          value={endMonth}
          onChange={setEndMonth}
        />
      </View>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
        onPress={handleSubmit}
      >
        <Text style={styles.primaryButtonText}>
          {isSubmitting ? "Saving..." : "Add season"}
        </Text>
      </Pressable>
    </View>
  );
}

function CropSeasonList({
  canManage,
  onChangeStatus,
  savingKey,
  seasons
}: {
  canManage: boolean;
  onChangeStatus: (season: CropSeason, status: FarmRecordStatus) => Promise<void>;
  savingKey: string | null;
  seasons: CropSeason[];
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Saved seasons</Text>
      {seasons.length ? (
        seasons.map((season) => {
          const nextStatus = season.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";
          const isSaving = savingKey === `season:${season.id}`;

          return (
            <View key={season.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>
                  {season.name} ({season.code})
                </Text>
                <Text style={styles.rowMeta}>
                  {season.seasonYear}
                  {season.startMonth && season.endMonth
                    ? ` - ${monthName(season.startMonth)} to ${monthName(
                        season.endMonth
                      )}`
                    : ""}
                </Text>
              </View>
              {canManage ? (
                <RowStatusActions
                  isSaving={isSaving}
                  onPress={() => onChangeStatus(season, nextStatus)}
                  status={season.status}
                />
              ) : (
                <StatusBadge
                  label={recordStatusLabel(season.status)}
                  tone={recordStatusTone(season.status)}
                />
              )}
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No seasons saved yet.</Text>
      )}
    </View>
  );
}

function MemberChoice({
  members,
  onSelect,
  selectedMemberId
}: {
  members: FpoMember[];
  onSelect: (memberId: string) => void;
  selectedMemberId: string;
}) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Farmer crop records</Text>
      <Text style={styles.fieldLabel}>Selected farmer</Text>
      <View style={styles.choiceRow}>
        {members.map((member) => (
          <Pressable
            accessibilityRole="button"
            key={member.memberId}
            style={[
              styles.choiceButton,
              selectedMemberId === member.memberId && styles.choiceButtonActive
            ]}
            onPress={() => onSelect(member.memberId)}
          >
            <Text
              style={[
                styles.choiceButtonText,
                selectedMemberId === member.memberId && styles.choiceButtonTextActive
              ]}
            >
              {member.memberNumber} - {member.name}
            </Text>
          </Pressable>
        ))}
      </View>
      {!members.length ? (
        <Text style={styles.emptyText}>
          Create a farmer profile before adding crop records.
        </Text>
      ) : null}
    </View>
  );
}

function CropHistoryForm({
  crops,
  isSubmitting,
  onSubmit,
  seasons
}: {
  crops: CropCatalog[];
  isSubmitting: boolean;
  onSubmit: (input: CropHistoryInput) => Promise<boolean>;
  seasons: CropSeason[];
}) {
  const [areaAcres, setAreaAcres] = useState("");
  const [cropId, setCropId] = useState("");
  const [cropYear, setCropYear] = useState(String(new Date().getFullYear() - 1));
  const [localError, setLocalError] = useState("");
  const [notes, setNotes] = useState("");
  const [seasonId, setSeasonId] = useState("");
  const [yieldQuantity, setYieldQuantity] = useState("");
  const [yieldUnit, setYieldUnit] = useState("");

  useEffect(() => {
    if (!crops.length) {
      setCropId("");
      return;
    }

    if (!crops.some((crop) => crop.id === cropId)) {
      setCropId(crops[0].id);
    }
  }, [cropId, crops]);

  async function handleSubmit() {
    if (!cropId || !cropYear.trim()) {
      setLocalError("Select crop and enter crop year.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      areaAcres: areaAcres.trim(),
      cropId,
      cropYear: cropYear.trim(),
      notes: notes.trim(),
      seasonId,
      yieldQuantity: yieldQuantity.trim(),
      yieldUnit: yieldUnit.trim()
    });

    if (created) {
      setAreaAcres("");
      setNotes("");
      setYieldQuantity("");
      setYieldUnit("");
    }
  }

  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Add crop history</Text>
      <Text style={styles.fieldLabel}>Crop</Text>
      <ChoiceButtons
        choices={crops.map((crop) => ({ id: crop.id, label: crop.name }))}
        emptyLabel="Add an active crop first."
        onSelect={setCropId}
        selectedId={cropId}
      />
      <Text style={styles.fieldLabel}>Season</Text>
      <ChoiceButtons
        choices={[
          { id: "", label: "None" },
          ...seasons.map((season) => ({
            id: season.id,
            label: `${season.name} ${season.seasonYear}`
          }))
        ]}
        onSelect={setSeasonId}
        selectedId={seasonId}
      />
      <View style={styles.formGrid}>
        <PlanningField
          keyboardType="numeric"
          label="Crop year"
          value={cropYear}
          onChange={setCropYear}
        />
        <PlanningField
          keyboardType="decimal-pad"
          label="Area acres"
          value={areaAcres}
          onChange={setAreaAcres}
        />
        <PlanningField
          keyboardType="decimal-pad"
          label="Yield quantity"
          value={yieldQuantity}
          onChange={setYieldQuantity}
        />
        <PlanningField label="Yield unit" value={yieldUnit} onChange={setYieldUnit} />
        <PlanningField label="Notes" value={notes} onChange={setNotes} />
      </View>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
        onPress={handleSubmit}
      >
        <Text style={styles.primaryButtonText}>
          {isSubmitting ? "Saving..." : "Add history"}
        </Text>
      </Pressable>
    </View>
  );
}

function CropHistoryList({ history }: { history: CropHistory[] }) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Saved crop history</Text>
      {history.length ? (
        history.map((item) => (
          <View key={item.id} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>
                {item.cropName} - {item.cropYear}
              </Text>
              <Text style={styles.rowMeta}>
                {[item.seasonName, item.areaAcres ? formatArea(item.areaAcres) : ""]
                  .filter(Boolean)
                  .join(" - ") || "Season and area not set"}
              </Text>
              {item.yieldQuantity !== undefined ? (
                <Text style={styles.rowMeta}>
                  Yield {formatNumber(item.yieldQuantity)} {item.yieldUnit}
                </Text>
              ) : null}
              {item.notes ? <Text style={styles.rowMeta}>{item.notes}</Text> : null}
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>No crop history saved for this member.</Text>
      )}
    </View>
  );
}

function CropPlanFilters({
  crops,
  filterCropId,
  filterSeasonId,
  filterStatus,
  onFilterCropId,
  onFilterSeasonId,
  onFilterStatus,
  seasons
}: {
  crops: CropCatalog[];
  filterCropId: string;
  filterSeasonId: string;
  filterStatus: CropPlanStatus | "";
  onFilterCropId: (cropId: string) => void;
  onFilterSeasonId: (seasonId: string) => void;
  onFilterStatus: (status: CropPlanStatus | "") => void;
  seasons: CropSeason[];
}) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Plan filters</Text>
      <Text style={styles.fieldLabel}>Crop</Text>
      <ChoiceButtons
        choices={[
          { id: "", label: "All crops" },
          ...crops.map((crop) => ({ id: crop.id, label: crop.name }))
        ]}
        onSelect={onFilterCropId}
        selectedId={filterCropId}
      />
      <Text style={styles.fieldLabel}>Season</Text>
      <ChoiceButtons
        choices={[
          { id: "", label: "All seasons" },
          ...seasons.map((season) => ({
            id: season.id,
            label: `${season.name} ${season.seasonYear}`
          }))
        ]}
        onSelect={onFilterSeasonId}
        selectedId={filterSeasonId}
      />
      <Text style={styles.fieldLabel}>Status</Text>
      <ChoiceButtons
        choices={[
          { id: "", label: "All statuses" },
          ...planStatuses.map((status) => ({
            id: status,
            label: planStatusLabel(status)
          }))
        ]}
        onSelect={(status) => onFilterStatus(status as CropPlanStatus | "")}
        selectedId={filterStatus}
      />
    </View>
  );
}

function CropPlanForm({
  crops,
  isSubmitting,
  members,
  onSubmit,
  selectedMemberId,
  seasons
}: {
  crops: CropCatalog[];
  isSubmitting: boolean;
  members: FpoMember[];
  onSubmit: (input: CropPlanInput) => Promise<boolean>;
  selectedMemberId: string;
  seasons: CropSeason[];
}) {
  const [cropId, setCropId] = useState("");
  const [cropYear, setCropYear] = useState("");
  const [expectedHarvestDate, setExpectedHarvestDate] = useState("");
  const [expectedYieldQuintals, setExpectedYieldQuintals] = useState("");
  const [localError, setLocalError] = useState("");
  const [memberId, setMemberId] = useState(selectedMemberId);
  const [plannedAreaAcres, setPlannedAreaAcres] = useState("");
  const [plannedSowingDate, setPlannedSowingDate] = useState("");
  const [seasonId, setSeasonId] = useState("");

  useEffect(() => {
    if (selectedMemberId && selectedMemberId !== memberId) {
      setMemberId(selectedMemberId);
      return;
    }

    if (!members.length) {
      setMemberId("");
      return;
    }

    if (!members.some((member) => member.memberId === memberId)) {
      setMemberId(members[0].memberId);
    }
  }, [memberId, members, selectedMemberId]);

  useEffect(() => {
    if (!crops.length) {
      setCropId("");
      return;
    }

    if (!crops.some((crop) => crop.id === cropId)) {
      setCropId(crops[0].id);
    }
  }, [cropId, crops]);

  useEffect(() => {
    if (!seasons.length) {
      setSeasonId("");
      return;
    }

    if (!seasons.some((season) => season.id === seasonId)) {
      setSeasonId(seasons[0].id);
    }
  }, [seasonId, seasons]);

  useEffect(() => {
    const season = seasons.find((item) => item.id === seasonId);
    if (season && !cropYear.trim()) {
      setCropYear(formatCropYear(season.seasonYear));
    }
  }, [cropYear, seasonId, seasons]);

  async function handleSubmit() {
    const missingFields = [
      !memberId ? "farmer" : "",
      !cropId ? "crop" : "",
      !seasonId ? "season" : "",
      !cropYear.trim() ? "crop year" : "",
      !plannedAreaAcres.trim() ? "planned acreage" : ""
    ].filter(Boolean);

    if (missingFields.length) {
      setLocalError(`Select or enter ${formatList(missingFields)}.`);
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      cropId,
      cropYear: cropYear.trim(),
      expectedHarvestDate: expectedHarvestDate.trim(),
      expectedYieldQuintals: expectedYieldQuintals.trim(),
      memberId,
      plannedAreaAcres: plannedAreaAcres.trim(),
      plannedSowingDate: plannedSowingDate.trim(),
      seasonId,
      status: "DRAFT"
    });

    if (created) {
      setExpectedHarvestDate("");
      setExpectedYieldQuintals("");
      setPlannedAreaAcres("");
      setPlannedSowingDate("");
    }
  }

  function handleSeasonSelect(nextSeasonId: string) {
    setSeasonId(nextSeasonId);
    const season = seasons.find((item) => item.id === nextSeasonId);
    if (season) {
      setCropYear(formatCropYear(season.seasonYear));
    }
  }

  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Add seasonal crop plan</Text>
      <Text style={styles.fieldLabel}>Farmer</Text>
      <ChoiceButtons
        choices={members.map((member) => ({
          id: member.memberId,
          label: `${member.memberNumber} - ${member.name}`
        }))}
        emptyLabel="Create a farmer profile first."
        onSelect={setMemberId}
        selectedId={memberId}
      />
      <Text style={styles.fieldLabel}>Crop</Text>
      <ChoiceButtons
        choices={crops.map((crop) => ({ id: crop.id, label: crop.name }))}
        emptyLabel="Add an active crop first."
        onSelect={setCropId}
        selectedId={cropId}
      />
      <Text style={styles.fieldLabel}>Season</Text>
      <ChoiceButtons
        choices={seasons.map((season) => ({
          id: season.id,
          label: `${season.name} ${season.seasonYear}`
        }))}
        emptyLabel="Add an active season first."
        onSelect={handleSeasonSelect}
        selectedId={seasonId}
      />
      <View style={styles.formGrid}>
        <PlanningField label="Crop year" value={cropYear} onChange={setCropYear} />
        <PlanningField
          keyboardType="decimal-pad"
          label="Planned acres"
          value={plannedAreaAcres}
          onChange={setPlannedAreaAcres}
        />
        <PlanningField
          label="Sowing date"
          placeholder="YYYY-MM-DD"
          value={plannedSowingDate}
          onChange={setPlannedSowingDate}
        />
        <PlanningField
          label="Harvest date"
          placeholder="YYYY-MM-DD"
          value={expectedHarvestDate}
          onChange={setExpectedHarvestDate}
        />
        <PlanningField
          keyboardType="decimal-pad"
          label="Expected yield (quintals)"
          value={expectedYieldQuintals}
          onChange={setExpectedYieldQuintals}
        />
      </View>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
        onPress={handleSubmit}
      >
        <Text style={styles.primaryButtonText}>
          {isSubmitting ? "Saving..." : "Add plan"}
        </Text>
      </Pressable>
    </View>
  );
}

function CropPlanList({
  onChangeStatus,
  plans,
  savingKey
}: {
  onChangeStatus: (plan: CropPlan, status: CropPlanStatus) => Promise<void>;
  plans: CropPlan[];
  savingKey: string | null;
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Seasonal crop plans</Text>
      {plans.length ? (
        plans.map((plan) => (
          <View key={plan.id} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>
                {plan.cropName} - {plan.memberName}
              </Text>
              <Text style={styles.rowMeta}>
                {plan.memberVillage} - {plan.seasonName} {plan.cropYear} -{" "}
                {formatArea(plan.plannedAreaAcres)}
              </Text>
              {plan.expectedYieldQuintals !== undefined ? (
                <Text style={styles.rowMeta}>
                  Expected yield {formatNumber(plan.expectedYieldQuintals)} quintals
                </Text>
              ) : null}
              {plan.confirmedAt ? (
                <Text style={styles.rowMeta}>
                  Confirmed {formatDisplayDate(plan.confirmedAt)}
                </Text>
              ) : null}
              <Text style={styles.rowMeta}>
                {[plan.plannedSowingDate, plan.expectedHarvestDate]
                  .filter(Boolean)
                  .join(" to ") || "Dates not set"}
              </Text>
            </View>
            <View style={styles.rowActions}>
              <StatusBadge
                label={planStatusLabel(plan.status)}
                tone={planStatusTone(plan.status)}
              />
              <View style={styles.buttonRow}>
                {planStatusActions(plan.status).map((status) => {
                  const isSaving = savingKey === `plan:${plan.id}:${status}`;
                  return (
                    <Pressable
                      accessibilityRole="button"
                      disabled={isSaving}
                      key={status}
                      style={[
                        styles.secondaryButton,
                        isSaving && styles.disabledButton
                      ]}
                      onPress={() => onChangeStatus(plan, status)}
                    >
                      <Text style={styles.secondaryButtonText}>
                        {isSaving ? "Saving..." : planStatusActionLabel(status)}
                      </Text>
                    </Pressable>
                  );
                })}
              </View>
            </View>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>No crop plans match the selected filters.</Text>
      )}
    </View>
  );
}

function ChoiceButtons({
  choices,
  emptyLabel,
  onSelect,
  selectedId
}: {
  choices: { id: string; label: string }[];
  emptyLabel?: string;
  onSelect: (id: string) => void;
  selectedId: string;
}) {
  if (!choices.length) {
    return (
      <Text style={styles.emptyText}>{emptyLabel ?? "No options available."}</Text>
    );
  }

  return (
    <View style={styles.choiceRow}>
      {choices.map((choice) => (
        <Pressable
          accessibilityRole="button"
          key={`${choice.id}:${choice.label}`}
          style={[
            styles.choiceButton,
            selectedId === choice.id && styles.choiceButtonActive
          ]}
          onPress={() => onSelect(choice.id)}
        >
          <Text
            style={[
              styles.choiceButtonText,
              selectedId === choice.id && styles.choiceButtonTextActive
            ]}
          >
            {choice.label}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

function RowStatusActions({
  isSaving,
  onPress,
  status
}: {
  isSaving: boolean;
  onPress: () => void;
  status: FarmRecordStatus;
}) {
  const nextStatus = status === "ACTIVE" ? "Archive" : "Activate";

  return (
    <View style={styles.rowActions}>
      <StatusBadge label={recordStatusLabel(status)} tone={recordStatusTone(status)} />
      <Pressable
        accessibilityRole="button"
        disabled={isSaving}
        style={[styles.secondaryButton, isSaving && styles.disabledButton]}
        onPress={onPress}
      >
        <Text style={styles.secondaryButtonText}>
          {isSaving ? "Saving..." : nextStatus}
        </Text>
      </Pressable>
    </View>
  );
}

function PlanningField({
  keyboardType,
  label,
  onChange,
  placeholder,
  value
}: {
  keyboardType?: "decimal-pad" | "default" | "numeric";
  label: string;
  onChange: (value: string) => void;
  placeholder?: string;
  value: string;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        autoCorrect={false}
        keyboardType={keyboardType}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor="#8ca0aa"
        style={styles.input}
        value={value}
      />
    </View>
  );
}

function recordStatusLabel(status: FarmRecordStatus) {
  switch (status) {
    case "ACTIVE":
      return "Active";
    case "ARCHIVED":
      return "Archived";
    default:
      return "Inactive";
  }
}

function recordStatusTone(status: FarmRecordStatus) {
  switch (status) {
    case "ACTIVE":
      return "good";
    case "ARCHIVED":
      return "neutral";
    default:
      return "warning";
  }
}

function planStatusLabel(status: CropPlanStatus) {
  switch (status) {
    case "CONFIRMED":
      return "Confirmed";
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
      return "Cancelled";
    default:
      return "Draft";
  }
}

function planStatusTone(status: CropPlanStatus) {
  switch (status) {
    case "CONFIRMED":
      return "good";
    case "COMPLETED":
      return "neutral";
    case "CANCELLED":
      return "danger";
    default:
      return "warning";
  }
}

function planStatusActions(status: CropPlanStatus): CropPlanStatus[] {
  switch (status) {
    case "DRAFT":
      return ["CONFIRMED", "CANCELLED"];
    case "CONFIRMED":
      return ["COMPLETED", "CANCELLED"];
    case "COMPLETED":
      return ["DRAFT"];
    default:
      return ["DRAFT"];
  }
}

function planStatusActionLabel(status: CropPlanStatus) {
  switch (status) {
    case "CONFIRMED":
      return "Confirm";
    case "COMPLETED":
      return "Complete";
    case "CANCELLED":
      return "Cancel";
    default:
      return "Reopen";
  }
}

function monthName(value: number) {
  const names = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec"
  ];
  return names[value - 1] ?? String(value);
}

function formatArea(value: number) {
  return `${formatNumber(value)} acres`;
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function formatCropYear(seasonYear: number) {
  return `${seasonYear}-${String((seasonYear + 1) % 100).padStart(2, "0")}`;
}

function formatDisplayDate(value: string) {
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

function formatList(values: string[]) {
  if (values.length <= 1) {
    return values[0] ?? "";
  }

  return `${values.slice(0, -1).join(", ")} and ${values[values.length - 1]}`;
}

const styles = StyleSheet.create({
  section: {
    gap: 14
  },
  sectionHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  headerText: {
    flex: 1
  },
  sectionTitle: {
    color: "#172126",
    fontSize: 18,
    fontWeight: "800"
  },
  summaryGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  summaryCard: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    flex: 1,
    minWidth: 132,
    padding: 14
  },
  summaryValue: {
    color: "#1f6f73",
    fontSize: 23,
    fontWeight: "800",
    marginBottom: 4
  },
  summaryLabel: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "800"
  },
  card: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 16
  },
  cardTitle: {
    color: "#172126",
    fontSize: 16,
    fontWeight: "800"
  },
  subsectionTitle: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  },
  listBlock: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea",
    borderRadius: 8,
    borderWidth: 1,
    gap: 8,
    padding: 16
  },
  formGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12
  },
  field: {
    flex: 1,
    gap: 7,
    minWidth: 170
  },
  fieldLabel: {
    color: "#24343b",
    fontSize: 13,
    fontWeight: "800"
  },
  input: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 15,
    minHeight: 46,
    paddingHorizontal: 12
  },
  choiceRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  choiceButton: {
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    minHeight: 38,
    minWidth: 90,
    paddingHorizontal: 10,
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
  row: {
    alignItems: "flex-start",
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    paddingTop: 10
  },
  rowText: {
    flex: 1
  },
  rowTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  rowMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  rowActions: {
    alignItems: "flex-end",
    gap: 8
  },
  buttonRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    justifyContent: "flex-end"
  },
  primaryButton: {
    alignItems: "center",
    backgroundColor: "#1f6f73",
    borderRadius: 8,
    justifyContent: "center",
    minHeight: 42,
    minWidth: 120,
    paddingHorizontal: 14
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 13,
    fontWeight: "800"
  },
  secondaryButton: {
    alignItems: "center",
    borderColor: "#1f6f73",
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: "center",
    minHeight: 38,
    minWidth: 92,
    paddingHorizontal: 12
  },
  secondaryButtonText: {
    color: "#1f6f73",
    fontSize: 13,
    fontWeight: "800"
  },
  disabledButton: {
    opacity: 0.6
  },
  formError: {
    color: "#b42318",
    fontSize: 13,
    fontWeight: "700"
  },
  emptyText: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700"
  }
});
