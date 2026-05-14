import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { CropPlanStatus, FarmRecordStatus } from "../core/api/fpoContracts";
import { getErrorMessage } from "../core/errors/AppError";
import {
  CropCatalog,
  CropSeason,
  getCropCatalog,
  getCropSeasons
} from "../data/cropPlanningStore";
import {
  createCropInputRule,
  createInputCatalog,
  CropInputRule,
  CropInputRuleInput,
  getCropInputRules,
  getInputCatalog,
  getInputDemandEstimates,
  getInputDemandSummary,
  InputCatalog,
  InputCatalogInput,
  InputDemandEstimate,
  InputDemandSummary,
  runInputDemand,
  updateCropInputRuleStatus,
  updateInputStatus
} from "../data/inputDemandStore";
import { StatusBadge } from "../ui/StatusBadge";

const confirmedPlanStatus: CropPlanStatus = "CONFIRMED";

export function AdminInputDemandTab() {
  const [crops, setCrops] = useState<CropCatalog[]>([]);
  const [error, setError] = useState("");
  const [estimates, setEstimates] = useState<InputDemandEstimate[]>([]);
  const [filterCropId, setFilterCropId] = useState("");
  const [filterVillage, setFilterVillage] = useState("");
  const [inputs, setInputs] = useState<InputCatalog[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [rules, setRules] = useState<CropInputRule[]>([]);
  const [savingKey, setSavingKey] = useState<string | null>(null);
  const [seasons, setSeasons] = useState<CropSeason[]>([]);
  const [selectedSeasonId, setSelectedSeasonId] = useState("");
  const [summary, setSummary] = useState<InputDemandSummary | null>(null);

  const activeCrops = useMemo(
    () => crops.filter((crop) => crop.status === "ACTIVE"),
    [crops]
  );
  const activeInputs = useMemo(
    () => inputs.filter((input) => input.status === "ACTIVE"),
    [inputs]
  );
  const activeSeasons = useMemo(
    () => seasons.filter((season) => season.status === "ACTIVE"),
    [seasons]
  );
  const selectedSeason =
    seasons.find((season) => season.id === selectedSeasonId) ?? null;

  const summaryCards = useMemo(
    () => [
      {
        label: "Planned acres",
        value: formatNumber(summary?.totalPlannedAreaAcres ?? 0)
      },
      { label: "Farmers", value: String(summary?.memberCount ?? 0) },
      { label: "Plans", value: String(summary?.planCount ?? 0) },
      { label: "Estimate rows", value: String(summary?.estimateCount ?? 0) }
    ],
    [summary]
  );

  useEffect(() => {
    loadInputDemand();
  }, []);

  async function loadInputDemand() {
    setIsLoading(true);
    setError("");

    try {
      const [nextCrops, nextSeasons, nextInputs, nextRules] = await Promise.all([
        getCropCatalog(),
        getCropSeasons(),
        getInputCatalog(),
        getCropInputRules()
      ]);
      const nextSeasonId =
        selectedSeasonId ||
        nextSeasons.find((season) => season.status === "ACTIVE")?.id ||
        nextSeasons[0]?.id ||
        "";

      setCrops(nextCrops);
      setSeasons(nextSeasons);
      setInputs(nextInputs);
      setRules(nextRules);
      setSelectedSeasonId(nextSeasonId);
      await refreshDemandOutput(nextSeasonId, filterCropId, filterVillage);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load input demand records."));
    } finally {
      setIsLoading(false);
    }
  }

  async function refreshDemandOutput(
    seasonId = selectedSeasonId,
    cropId = filterCropId,
    village = filterVillage
  ) {
    if (!seasonId) {
      setEstimates([]);
      setSummary(null);
      return;
    }

    const filters = {
      cropId: cropId || undefined,
      seasonId,
      village: village.trim() || undefined
    };
    const [nextSummary, nextEstimates] = await Promise.all([
      getInputDemandSummary(filters),
      getInputDemandEstimates(filters)
    ]);

    setSummary(nextSummary);
    setEstimates(nextEstimates);
  }

  async function handleCreateInput(input: InputCatalogInput) {
    setSavingKey("input:create");
    setError("");

    try {
      const created = await createInputCatalog(input);
      setInputs((current) => [
        created,
        ...current.filter((item) => item.id !== created.id)
      ]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save input."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleInputStatus(input: InputCatalog, status: FarmRecordStatus) {
    setSavingKey(`input:${input.id}`);
    setError("");

    try {
      const updated = await updateInputStatus(input, status);
      setInputs((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update input status."));
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCreateRule(input: CropInputRuleInput) {
    setSavingKey("rule:create");
    setError("");

    try {
      const created = await createCropInputRule(input, activeCrops, activeInputs);
      setRules((current) => [
        created,
        ...current.filter((item) => item.id !== created.id)
      ]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save input rule."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleRuleStatus(rule: CropInputRule, status: FarmRecordStatus) {
    setSavingKey(`rule:${rule.id}`);
    setError("");

    try {
      const updated = await updateCropInputRuleStatus(rule, status);
      setRules((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
      await refreshDemandOutput();
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update input rule status."));
    } finally {
      setSavingKey(null);
    }
  }

  async function handleRunDemand() {
    if (!selectedSeasonId) {
      setError("Select a season before calculating demand.");
      return;
    }

    setSavingKey("demand:run");
    setError("");

    try {
      await runInputDemand({
        cropId: filterCropId,
        planStatus: confirmedPlanStatus,
        seasonId: selectedSeasonId,
        village: filterVillage.trim()
      });
      await refreshDemandOutput();
    } catch (runError) {
      setError(getErrorMessage(runError, "Unable to calculate input demand."));
    } finally {
      setSavingKey(null);
    }
  }

  async function handleApplyFilters() {
    setSavingKey("demand:filter");
    setError("");

    try {
      await refreshDemandOutput();
    } catch (filterError) {
      setError(getErrorMessage(filterError, "Unable to refresh demand summary."));
    } finally {
      setSavingKey(null);
    }
  }

  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={styles.headerText}>
          <Text style={styles.sectionTitle}>Input demand</Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadInputDemand}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      <View style={styles.summaryGrid}>
        {summaryCards.map((item) => (
          <View key={item.label} style={styles.summaryCard}>
            <Text style={styles.summaryValue}>{item.value}</Text>
            <Text style={styles.summaryLabel}>{item.label}</Text>
          </View>
        ))}
      </View>

      <DemandRunPanel
        crops={crops}
        filterCropId={filterCropId}
        filterVillage={filterVillage}
        isFiltering={savingKey === "demand:filter"}
        isRunning={savingKey === "demand:run"}
        onApplyFilters={handleApplyFilters}
        onFilterCropId={setFilterCropId}
        onFilterVillage={setFilterVillage}
        onRun={handleRunDemand}
        onSeasonId={setSelectedSeasonId}
        seasons={seasons}
        selectedSeasonId={selectedSeasonId}
      />

      <DemandBreakdown
        estimates={estimates}
        selectedSeason={selectedSeason}
        summary={summary}
      />

      <InputCatalogForm
        isSubmitting={savingKey === "input:create"}
        onSubmit={handleCreateInput}
      />
      <InputCatalogList
        inputs={inputs}
        onChangeStatus={handleInputStatus}
        savingKey={savingKey}
      />

      <CropInputRuleForm
        crops={activeCrops}
        inputs={activeInputs}
        isSubmitting={savingKey === "rule:create"}
        onSubmit={handleCreateRule}
      />
      <CropInputRuleList
        onChangeStatus={handleRuleStatus}
        rules={rules}
        savingKey={savingKey}
      />

      {!activeSeasons.length || !activeCrops.length || !activeInputs.length ? (
        <Text style={styles.emptyText}>
          Demand needs active seasons, crops, and inputs before calculation.
        </Text>
      ) : null}
    </View>
  );
}

function DemandRunPanel({
  crops,
  filterCropId,
  filterVillage,
  isFiltering,
  isRunning,
  onApplyFilters,
  onFilterCropId,
  onFilterVillage,
  onRun,
  onSeasonId,
  seasons,
  selectedSeasonId
}: {
  crops: CropCatalog[];
  filterCropId: string;
  filterVillage: string;
  isFiltering: boolean;
  isRunning: boolean;
  onApplyFilters: () => Promise<void>;
  onFilterCropId: (cropId: string) => void;
  onFilterVillage: (village: string) => void;
  onRun: () => Promise<void>;
  onSeasonId: (seasonId: string) => void;
  seasons: CropSeason[];
  selectedSeasonId: string;
}) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Demand calculation</Text>
      <Text style={styles.fieldLabel}>Season</Text>
      <ChoiceButtons
        choices={seasons.map((season) => ({
          id: season.id,
          label: `${season.name} ${season.seasonYear}`
        }))}
        emptyLabel="No seasons available."
        onSelect={onSeasonId}
        selectedId={selectedSeasonId}
      />
      <Text style={styles.fieldLabel}>Crop</Text>
      <ChoiceButtons
        choices={[
          { id: "", label: "All crops" },
          ...crops.map((crop) => ({ id: crop.id, label: crop.name }))
        ]}
        onSelect={onFilterCropId}
        selectedId={filterCropId}
      />
      <View style={styles.formGrid}>
        <DemandField label="Village" onChange={onFilterVillage} value={filterVillage} />
      </View>
      <View style={styles.buttonRow}>
        <Pressable
          accessibilityRole="button"
          disabled={isRunning || !selectedSeasonId}
          style={[
            styles.primaryButton,
            (isRunning || !selectedSeasonId) && styles.disabledButton
          ]}
          onPress={onRun}
        >
          <Text style={styles.primaryButtonText}>
            {isRunning ? "Calculating..." : "Calculate demand"}
          </Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          disabled={isFiltering || !selectedSeasonId}
          style={[
            styles.secondaryButton,
            (isFiltering || !selectedSeasonId) && styles.disabledButton
          ]}
          onPress={onApplyFilters}
        >
          <Text style={styles.secondaryButtonText}>
            {isFiltering ? "Refreshing..." : "Apply filters"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

function DemandBreakdown({
  estimates,
  selectedSeason,
  summary
}: {
  estimates: InputDemandEstimate[];
  selectedSeason: CropSeason | null;
  summary: InputDemandSummary | null;
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>
        Demand summary{selectedSeason ? ` - ${selectedSeason.name}` : ""}
      </Text>
      {summary?.byInput.length ? (
        <>
          <Text style={styles.fieldLabel}>By input</Text>
          {summary.byInput.map((item) => (
            <View key={item.inputId} style={styles.compactRow}>
              <Text style={styles.rowTitle}>
                {item.inputName} ({item.inputCode})
              </Text>
              <Text style={styles.rowMeta}>
                Final {formatNumber(item.finalDemandQuantity)} {item.unit} across{" "}
                {item.planCount} plan{item.planCount === 1 ? "" : "s"}
              </Text>
              <Text style={styles.rowMeta}>
                Base {formatNumber(item.totalDemandQuantity)} {item.unit} + buffer{" "}
                {formatNumber(item.bufferQuantity)} {item.unit}
              </Text>
            </View>
          ))}

          <Text style={styles.fieldLabel}>By crop</Text>
          {summary.byCrop.map((item) => (
            <View key={item.cropId} style={styles.compactRow}>
              <Text style={styles.rowTitle}>{item.cropName}</Text>
              <Text style={styles.rowMeta}>
                {formatArea(item.plannedAreaAcres)} across {item.planCount} plan
                {item.planCount === 1 ? "" : "s"}
              </Text>
            </View>
          ))}

          <Text style={styles.fieldLabel}>By village</Text>
          {summary.byVillage.map((item) => (
            <View key={item.village} style={styles.compactRow}>
              <Text style={styles.rowTitle}>{item.village}</Text>
              <Text style={styles.rowMeta}>
                {formatArea(item.plannedAreaAcres)} - {item.memberCount} farmer
                {item.memberCount === 1 ? "" : "s"}
              </Text>
            </View>
          ))}
        </>
      ) : (
        <Text style={styles.emptyText}>No demand estimate is available yet.</Text>
      )}

      <Text style={styles.fieldLabel}>Estimate rows</Text>
      {estimates.length ? (
        estimates.slice(0, 12).map((estimate) => (
          <View key={estimate.id} style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.rowTitle}>
                {estimate.inputName} - {estimate.cropName}
              </Text>
              <Text style={styles.rowMeta}>
                {estimate.memberName} ({estimate.memberNumber}) -{" "}
                {estimate.memberVillage}
              </Text>
              <Text style={styles.rowMeta}>
                Final {formatNumber(estimate.finalDemandQuantity)} {estimate.unit}
              </Text>
              <Text style={styles.rowMeta}>
                {formatNumber(estimate.recommendedQuantityPerAcre)} {estimate.unit}/acre
                on {formatArea(estimate.plannedAreaAcres ?? 0)} - base{" "}
                {formatNumber(estimate.totalDemandQuantity)} + buffer{" "}
                {formatNumber(estimate.bufferQuantity)}
              </Text>
            </View>
            <StatusBadge label="Estimated" tone="good" />
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>
          No estimate rows match the selected filters.
        </Text>
      )}
    </View>
  );
}

function InputCatalogForm({
  isSubmitting,
  onSubmit
}: {
  isSubmitting: boolean;
  onSubmit: (input: InputCatalogInput) => Promise<boolean>;
}) {
  const [category, setCategory] = useState("");
  const [code, setCode] = useState("");
  const [localError, setLocalError] = useState("");
  const [name, setName] = useState("");
  const [unit, setUnit] = useState("");

  async function handleSubmit() {
    if (!code.trim() || !name.trim() || !unit.trim()) {
      setLocalError("Enter input code, name, and unit.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      category: category.trim(),
      code: code.trim(),
      name: name.trim(),
      unit: unit.trim()
    });

    if (created) {
      setCategory("");
      setCode("");
      setName("");
      setUnit("");
    }
  }

  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Input catalog</Text>
      <View style={styles.formGrid}>
        <DemandField label="Input code" onChange={setCode} value={code} />
        <DemandField label="Input name" onChange={setName} value={name} />
        <DemandField label="Category" onChange={setCategory} value={category} />
        <DemandField label="Unit" onChange={setUnit} value={unit} />
      </View>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
        onPress={handleSubmit}
      >
        <Text style={styles.primaryButtonText}>
          {isSubmitting ? "Saving..." : "Add input"}
        </Text>
      </Pressable>
    </View>
  );
}

function InputCatalogList({
  inputs,
  onChangeStatus,
  savingKey
}: {
  inputs: InputCatalog[];
  onChangeStatus: (input: InputCatalog, status: FarmRecordStatus) => Promise<void>;
  savingKey: string | null;
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Saved inputs</Text>
      {inputs.length ? (
        inputs.map((input) => {
          const nextStatus = input.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";
          const isSaving = savingKey === `input:${input.id}`;

          return (
            <View key={input.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>
                  {input.name} ({input.code})
                </Text>
                <Text style={styles.rowMeta}>
                  {[input.category, input.unit].filter(Boolean).join(" - ")}
                </Text>
              </View>
              <RowStatusActions
                isSaving={isSaving}
                onPress={() => onChangeStatus(input, nextStatus)}
                status={input.status}
              />
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No inputs saved yet.</Text>
      )}
    </View>
  );
}

function CropInputRuleForm({
  crops,
  inputs,
  isSubmitting,
  onSubmit
}: {
  crops: CropCatalog[];
  inputs: InputCatalog[];
  isSubmitting: boolean;
  onSubmit: (input: CropInputRuleInput) => Promise<boolean>;
}) {
  const [applicationStage, setApplicationStage] = useState("");
  const [cropId, setCropId] = useState("");
  const [inputId, setInputId] = useState("");
  const [localError, setLocalError] = useState("");
  const [notes, setNotes] = useState("");
  const [quantityPerAcre, setQuantityPerAcre] = useState("");

  useEffect(() => {
    if (!cropId && crops.length) {
      setCropId(crops[0].id);
    }
  }, [cropId, crops]);

  useEffect(() => {
    if (!inputId && inputs.length) {
      setInputId(inputs[0].id);
    }
  }, [inputId, inputs]);

  async function handleSubmit() {
    if (!cropId || !inputId || !quantityPerAcre.trim()) {
      setLocalError("Select crop, input, and quantity per acre.");
      return;
    }

    setLocalError("");
    const created = await onSubmit({
      applicationStage: applicationStage.trim(),
      cropId,
      inputId,
      notes: notes.trim(),
      quantityPerAcre: quantityPerAcre.trim()
    });

    if (created) {
      setApplicationStage("");
      setNotes("");
      setQuantityPerAcre("");
    }
  }

  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>Crop input rules</Text>
      <Text style={styles.fieldLabel}>Crop</Text>
      <ChoiceButtons
        choices={crops.map((crop) => ({ id: crop.id, label: crop.name }))}
        emptyLabel="Add an active crop first."
        onSelect={setCropId}
        selectedId={cropId}
      />
      <Text style={styles.fieldLabel}>Input</Text>
      <ChoiceButtons
        choices={inputs.map((input) => ({
          id: input.id,
          label: `${input.name} (${input.unit})`
        }))}
        emptyLabel="Add an active input first."
        onSelect={setInputId}
        selectedId={inputId}
      />
      <View style={styles.formGrid}>
        <DemandField
          keyboardType="decimal-pad"
          label="Quantity per acre"
          onChange={setQuantityPerAcre}
          value={quantityPerAcre}
        />
        <DemandField
          label="Stage"
          onChange={setApplicationStage}
          value={applicationStage}
        />
        <DemandField label="Notes" onChange={setNotes} value={notes} />
      </View>
      {localError ? <Text style={styles.formError}>{localError}</Text> : null}
      <Pressable
        accessibilityRole="button"
        disabled={isSubmitting}
        style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
        onPress={handleSubmit}
      >
        <Text style={styles.primaryButtonText}>
          {isSubmitting ? "Saving..." : "Add rule"}
        </Text>
      </Pressable>
    </View>
  );
}

function CropInputRuleList({
  onChangeStatus,
  rules,
  savingKey
}: {
  onChangeStatus: (rule: CropInputRule, status: FarmRecordStatus) => Promise<void>;
  rules: CropInputRule[];
  savingKey: string | null;
}) {
  return (
    <View style={styles.listBlock}>
      <Text style={styles.subsectionTitle}>Saved rules</Text>
      {rules.length ? (
        rules.map((rule) => {
          const nextStatus = rule.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";
          const isSaving = savingKey === `rule:${rule.id}`;

          return (
            <View key={rule.id} style={styles.row}>
              <View style={styles.rowText}>
                <Text style={styles.rowTitle}>
                  {rule.cropName} - {rule.inputName}
                </Text>
                <Text style={styles.rowMeta}>
                  {formatNumber(rule.quantityPerAcre)} {rule.inputUnit}/acre
                  {rule.applicationStage ? ` - ${rule.applicationStage}` : ""}
                </Text>
                {rule.notes ? <Text style={styles.rowMeta}>{rule.notes}</Text> : null}
              </View>
              <RowStatusActions
                isSaving={isSaving}
                onPress={() => onChangeStatus(rule, nextStatus)}
                status={rule.status}
              />
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No crop input rules saved yet.</Text>
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

function DemandField({
  keyboardType,
  label,
  onChange,
  value
}: {
  keyboardType?: "decimal-pad" | "default" | "numeric";
  label: string;
  onChange: (value: string) => void;
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

function formatArea(value: number) {
  return `${formatNumber(value)} acres`;
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
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
  compactRow: {
    backgroundColor: "#f4f7fb",
    borderRadius: 8,
    padding: 12
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
    gap: 8
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
