import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import { FarmRecordStatus } from "../core/api/fpoContracts";
import {
  createFarmLandholding,
  createFarmPlot,
  FarmLandholding,
  FarmLandholdingInput,
  FarmPlot,
  FarmPlotInput,
  getFarmLandholdings,
  getFarmPlots,
  updateFarmLandholdingStatus,
  updateFarmPlotStatus
} from "../data/farmAssetStore";
import { FpoMember } from "../data/fpoMemberStore";
import { StatusBadge } from "../ui/StatusBadge";

type AdminFarmAssetsPanelProps = {
  member: FpoMember;
};

type Coordinates = {
  latitude: number;
  longitude: number;
};

export function AdminFarmAssetsPanel({ member }: AdminFarmAssetsPanelProps) {
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [landholdings, setLandholdings] = useState<FarmLandholding[]>([]);
  const [plots, setPlots] = useState<FarmPlot[]>([]);
  const [savingKey, setSavingKey] = useState<string | null>(null);

  const activePlotArea = useMemo(
    () =>
      plots
        .filter((plot) => plot.status === "ACTIVE")
        .reduce((total, plot) => total + plot.areaAcres, 0),
    [plots]
  );

  useEffect(() => {
    loadFarmAssets();
  }, [member.memberId]);

  async function loadFarmAssets() {
    setIsLoading(true);
    setError("");

    try {
      const [nextLandholdings, nextPlots] = await Promise.all([
        getFarmLandholdings(member.memberId),
        getFarmPlots(member.memberId)
      ]);
      setLandholdings(nextLandholdings);
      setPlots(nextPlots);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Unable to load farm records."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreateLandholding(input: FarmLandholdingInput) {
    setSavingKey("landholding:create");
    setError("");

    try {
      const landholding = await createFarmLandholding(
        member.memberId,
        member.memberNumber,
        input
      );
      setLandholdings((current) => [
        landholding,
        ...current.filter((item) => item.id !== landholding.id)
      ]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save landholding."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCreatePlot(input: FarmPlotInput) {
    setSavingKey("plot:create");
    setError("");

    try {
      const plot = await createFarmPlot(member.memberId, member.memberNumber, input);
      setPlots((current) => [
        plot,
        ...current.filter((item) => item.id !== plot.id)
      ]);
      return true;
    } catch (createError) {
      setError(getErrorMessage(createError, "Unable to save farm plot."));
      return false;
    } finally {
      setSavingKey(null);
    }
  }

  async function handleLandholdingStatus(
    landholding: FarmLandholding,
    status: FarmRecordStatus
  ) {
    setSavingKey(`landholding:${landholding.id}`);
    setError("");

    try {
      const updated = await updateFarmLandholdingStatus(landholding, status);
      setLandholdings((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update landholding status."));
    } finally {
      setSavingKey(null);
    }
  }

  async function handlePlotStatus(plot: FarmPlot, status: FarmRecordStatus) {
    setSavingKey(`plot:${plot.id}`);
    setError("");

    try {
      const updated = await updateFarmPlotStatus(plot, status);
      setPlots((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
    } catch (statusError) {
      setError(getErrorMessage(statusError, "Unable to update plot status."));
    } finally {
      setSavingKey(null);
    }
  }

  return (
    <View style={styles.panel}>
      <View style={styles.panelHeader}>
        <View style={styles.panelTitleGroup}>
          <Text style={styles.panelTitle}>Landholdings and plots</Text>
          <Text style={styles.panelMeta}>
            Active plot area: {formatArea(activePlotArea)}
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          disabled={isLoading}
          style={[styles.secondaryButton, isLoading && styles.disabledButton]}
          onPress={loadFarmAssets}
        >
          <Text style={styles.secondaryButtonText}>
            {isLoading ? "Refreshing..." : "Refresh"}
          </Text>
        </Pressable>
      </View>

      {error ? <Text style={styles.formError}>{error}</Text> : null}

      <LandholdingForm
        isSubmitting={savingKey === "landholding:create"}
        onSubmit={handleCreateLandholding}
      />

      <FarmLandholdingList
        landholdings={landholdings}
        onChangeStatus={handleLandholdingStatus}
        savingKey={savingKey}
      />

      <PlotForm
        isSubmitting={savingKey === "plot:create"}
        landholdings={landholdings}
        onSubmit={handleCreatePlot}
      />

      <FarmPlotList
        landholdings={landholdings}
        onChangeStatus={handlePlotStatus}
        plots={plots}
        savingKey={savingKey}
      />
    </View>
  );
}

function LandholdingForm({
  isSubmitting,
  onSubmit
}: {
  isSubmitting: boolean;
  onSubmit: (input: FarmLandholdingInput) => Promise<boolean>;
}) {
  const [cultivableAreaAcres, setCultivableAreaAcres] = useState("");
  const [irrigationSource, setIrrigationSource] = useState("");
  const [localError, setLocalError] = useState("");
  const [ownershipType, setOwnershipType] = useState("");
  const [surveyNumber, setSurveyNumber] = useState("");
  const [totalAreaAcres, setTotalAreaAcres] = useState("");

  async function handleSubmit() {
    const input: FarmLandholdingInput = {
      cultivableAreaAcres: cultivableAreaAcres.trim(),
      irrigationSource: irrigationSource.trim(),
      ownershipType: ownershipType.trim(),
      surveyNumber: surveyNumber.trim(),
      totalAreaAcres: totalAreaAcres.trim()
    };
    const validationError = validateLandholdingInput(input);

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    setLocalError("");
    const created = await onSubmit(input);

    if (created) {
      setCultivableAreaAcres("");
      setIrrigationSource("");
      setOwnershipType("");
      setSurveyNumber("");
      setTotalAreaAcres("");
    }
  }

  return (
    <View style={styles.formSection}>
      <Text style={styles.sectionLabel}>Add landholding</Text>
      <View style={styles.formGrid}>
        <AssetField
          label="Survey number"
          value={surveyNumber}
          onChange={setSurveyNumber}
        />
        <AssetField
          keyboardType="decimal-pad"
          label="Total area acres"
          value={totalAreaAcres}
          onChange={setTotalAreaAcres}
        />
        <AssetField
          keyboardType="decimal-pad"
          label="Cultivable acres"
          value={cultivableAreaAcres}
          onChange={setCultivableAreaAcres}
        />
        <AssetField
          label="Ownership type"
          value={ownershipType}
          onChange={setOwnershipType}
        />
        <AssetField
          label="Irrigation source"
          value={irrigationSource}
          onChange={setIrrigationSource}
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
          {isSubmitting ? "Saving..." : "Add landholding"}
        </Text>
      </Pressable>
    </View>
  );
}

function FarmLandholdingList({
  landholdings,
  onChangeStatus,
  savingKey
}: {
  landholdings: FarmLandholding[];
  onChangeStatus: (
    landholding: FarmLandholding,
    status: FarmRecordStatus
  ) => Promise<void>;
  savingKey: string | null;
}) {
  return (
    <View style={styles.listSection}>
      <Text style={styles.sectionLabel}>Saved landholdings</Text>
      {landholdings.length ? (
        landholdings.map((landholding) => {
          const isSaving = savingKey === `landholding:${landholding.id}`;
          const nextStatus =
            landholding.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";

          return (
            <View key={landholding.id} style={styles.assetRow}>
              <View style={styles.assetText}>
                <Text style={styles.assetTitle}>
                  {landholding.surveyNumber || "Landholding"}
                </Text>
                <Text style={styles.assetMeta}>
                  Total {formatArea(landholding.totalAreaAcres)}
                  {landholding.cultivableAreaAcres !== undefined
                    ? `, cultivable ${formatArea(landholding.cultivableAreaAcres)}`
                    : ""}
                </Text>
                <Text style={styles.assetMeta}>
                  {[landholding.ownershipType, landholding.irrigationSource]
                    .filter(Boolean)
                    .join(" - ") || "Ownership and irrigation not set"}
                </Text>
              </View>
              <View style={styles.rowActions}>
                <StatusBadge
                  label={statusLabel(landholding.status)}
                  tone={statusTone(landholding.status)}
                />
                <Pressable
                  accessibilityRole="button"
                  disabled={isSaving}
                  style={[styles.secondaryButton, isSaving && styles.disabledButton]}
                  onPress={() => onChangeStatus(landholding, nextStatus)}
                >
                  <Text style={styles.secondaryButtonText}>
                    {isSaving
                      ? "Saving..."
                      : nextStatus === "ACTIVE"
                        ? "Activate"
                        : "Archive"}
                  </Text>
                </Pressable>
              </View>
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No landholdings saved for this member.</Text>
      )}
    </View>
  );
}

function PlotForm({
  isSubmitting,
  landholdings,
  onSubmit
}: {
  isSubmitting: boolean;
  landholdings: FarmLandholding[];
  onSubmit: (input: FarmPlotInput) => Promise<boolean>;
}) {
  const [areaAcres, setAreaAcres] = useState("");
  const [isLocating, setIsLocating] = useState(false);
  const [landholdingId, setLandholdingId] = useState("");
  const [latitude, setLatitude] = useState("");
  const [localError, setLocalError] = useState("");
  const [longitude, setLongitude] = useState("");
  const [plotName, setPlotName] = useState("");
  const [soilType, setSoilType] = useState("");

  async function handleUseCurrentLocation() {
    setIsLocating(true);
    setLocalError("");

    try {
      const coordinates = await requestCurrentCoordinates();
      setLatitude(coordinates.latitude.toFixed(7));
      setLongitude(coordinates.longitude.toFixed(7));
    } catch (locationError) {
      setLocalError(
        getErrorMessage(
          locationError,
          "Current location is unavailable. Enter latitude and longitude manually."
        )
      );
    } finally {
      setIsLocating(false);
    }
  }

  async function handleSubmit() {
    const input: FarmPlotInput = {
      areaAcres: areaAcres.trim(),
      landholdingId,
      latitude: latitude.trim(),
      longitude: longitude.trim(),
      plotName: plotName.trim(),
      soilType: soilType.trim()
    };
    const validationError = validatePlotInput(input);

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    setLocalError("");
    const created = await onSubmit(input);

    if (created) {
      setAreaAcres("");
      setLandholdingId("");
      setLatitude("");
      setLongitude("");
      setPlotName("");
      setSoilType("");
    }
  }

  return (
    <View style={styles.formSection}>
      <Text style={styles.sectionLabel}>Add farm plot</Text>

      <Text style={styles.fieldLabel}>Linked landholding</Text>
      <View style={styles.choiceRow}>
        <Pressable
          accessibilityRole="button"
          style={[styles.choiceButton, !landholdingId && styles.choiceButtonActive]}
          onPress={() => setLandholdingId("")}
        >
          <Text
            style={[
              styles.choiceButtonText,
              !landholdingId && styles.choiceButtonTextActive
            ]}
          >
            None
          </Text>
        </Pressable>
        {landholdings.map((landholding) => (
          <Pressable
            accessibilityRole="button"
            key={landholding.id}
            style={[
              styles.choiceButton,
              landholdingId === landholding.id && styles.choiceButtonActive
            ]}
            onPress={() => setLandholdingId(landholding.id)}
          >
            <Text
              style={[
                styles.choiceButtonText,
                landholdingId === landholding.id && styles.choiceButtonTextActive
              ]}
            >
              {landholding.surveyNumber || formatArea(landholding.totalAreaAcres)}
            </Text>
          </Pressable>
        ))}
      </View>

      <View style={styles.formGrid}>
        <AssetField label="Plot name" value={plotName} onChange={setPlotName} />
        <AssetField
          keyboardType="decimal-pad"
          label="Area acres"
          value={areaAcres}
          onChange={setAreaAcres}
        />
        <AssetField
          keyboardType="decimal-pad"
          label="Latitude"
          value={latitude}
          onChange={setLatitude}
        />
        <AssetField
          keyboardType="decimal-pad"
          label="Longitude"
          value={longitude}
          onChange={setLongitude}
        />
        <AssetField label="Soil type" value={soilType} onChange={setSoilType} />
      </View>

      {localError ? <Text style={styles.formError}>{localError}</Text> : null}

      <View style={styles.actionRow}>
        <Pressable
          accessibilityRole="button"
          disabled={isLocating}
          style={[styles.secondaryButton, isLocating && styles.disabledButton]}
          onPress={handleUseCurrentLocation}
        >
          <Text style={styles.secondaryButtonText}>
            {isLocating ? "Locating..." : "Use current location"}
          </Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          disabled={isSubmitting}
          style={[styles.primaryButton, isSubmitting && styles.disabledButton]}
          onPress={handleSubmit}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? "Saving..." : "Add plot"}
          </Text>
        </Pressable>
      </View>
    </View>
  );
}

function FarmPlotList({
  landholdings,
  onChangeStatus,
  plots,
  savingKey
}: {
  landholdings: FarmLandholding[];
  onChangeStatus: (plot: FarmPlot, status: FarmRecordStatus) => Promise<void>;
  plots: FarmPlot[];
  savingKey: string | null;
}) {
  return (
    <View style={styles.listSection}>
      <Text style={styles.sectionLabel}>Saved plots</Text>
      {plots.length ? (
        plots.map((plot) => {
          const landholding = landholdings.find(
            (item) => item.id === plot.landholdingId
          );
          const isSaving = savingKey === `plot:${plot.id}`;
          const nextStatus = plot.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";

          return (
            <View key={plot.id} style={styles.assetRow}>
              <View style={styles.assetText}>
                <Text style={styles.assetTitle}>{plot.plotName}</Text>
                <Text style={styles.assetMeta}>
                  {formatArea(plot.areaAcres)}
                  {landholding
                    ? ` - ${landholding.surveyNumber || "Linked landholding"}`
                    : ""}
                </Text>
                <Text style={styles.assetMeta}>
                  {formatCoordinates(plot.latitude, plot.longitude)}
                </Text>
                {plot.soilType ? (
                  <Text style={styles.assetMeta}>Soil: {plot.soilType}</Text>
                ) : null}
              </View>
              <View style={styles.rowActions}>
                <StatusBadge
                  label={statusLabel(plot.status)}
                  tone={statusTone(plot.status)}
                />
                <Pressable
                  accessibilityRole="button"
                  disabled={isSaving}
                  style={[styles.secondaryButton, isSaving && styles.disabledButton]}
                  onPress={() => onChangeStatus(plot, nextStatus)}
                >
                  <Text style={styles.secondaryButtonText}>
                    {isSaving
                      ? "Saving..."
                      : nextStatus === "ACTIVE"
                        ? "Activate"
                        : "Archive"}
                  </Text>
                </Pressable>
              </View>
            </View>
          );
        })
      ) : (
        <Text style={styles.emptyText}>No plots saved for this member.</Text>
      )}
    </View>
  );
}

function AssetField({
  keyboardType,
  label,
  onChange,
  value
}: {
  keyboardType?: "default" | "decimal-pad";
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
        style={styles.input}
        value={value}
      />
    </View>
  );
}

function validateLandholdingInput(input: FarmLandholdingInput) {
  const totalArea = Number(input.totalAreaAcres);
  const cultivableArea = input.cultivableAreaAcres
    ? Number(input.cultivableAreaAcres)
    : undefined;

  if (!Number.isFinite(totalArea) || totalArea <= 0) {
    return "Total area must be greater than zero.";
  }

  if (
    cultivableArea !== undefined &&
    (!Number.isFinite(cultivableArea) || cultivableArea < 0)
  ) {
    return "Cultivable area must be zero or greater.";
  }

  if (cultivableArea !== undefined && cultivableArea > totalArea) {
    return "Cultivable area cannot exceed total area.";
  }

  return "";
}

function validatePlotInput(input: FarmPlotInput) {
  const area = Number(input.areaAcres);

  if (!input.plotName) {
    return "Enter a plot name.";
  }

  if (!Number.isFinite(area) || area <= 0) {
    return "Plot area must be greater than zero.";
  }

  if (!input.latitude && !input.longitude) {
    return "";
  }

  const latitude = Number(input.latitude);
  const longitude = Number(input.longitude);

  if (
    !input.latitude ||
    !Number.isFinite(latitude) ||
    latitude < -90 ||
    latitude > 90
  ) {
    return "Latitude must be between -90 and 90.";
  }

  if (
    !input.longitude ||
    !Number.isFinite(longitude) ||
    longitude < -180 ||
    longitude > 180
  ) {
    return "Longitude must be between -180 and 180.";
  }

  return "";
}

async function requestCurrentCoordinates(): Promise<Coordinates> {
  const navigatorWithLocation = globalThis.navigator as
    | {
        geolocation?: {
          getCurrentPosition: (
            success: (position: {
              coords: {
                latitude: number;
                longitude: number;
              };
            }) => void,
            error: (error: { message?: string }) => void,
            options?: {
              enableHighAccuracy?: boolean;
              maximumAge?: number;
              timeout?: number;
            }
          ) => void;
        };
      }
    | undefined;

  const geolocation = navigatorWithLocation?.geolocation;
  if (!geolocation) {
    throw new Error(
      "Current location is unavailable. Enter latitude and longitude manually."
    );
  }

  return new Promise((resolve, reject) => {
    geolocation.getCurrentPosition(
      (position) =>
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        }),
      (error) =>
        reject(
          new Error(
            error.message ||
              "Location permission was denied. Enter latitude and longitude manually."
          )
        ),
      {
        enableHighAccuracy: true,
        maximumAge: 0,
        timeout: 10000
      }
    );
  });
}

function formatArea(value: number) {
  return `${roundDecimal(value)} acres`;
}

function formatCoordinates(latitude: number | undefined, longitude: number | undefined) {
  if (latitude === undefined || longitude === undefined) {
    return "GPS not captured";
  }

  return `${latitude.toFixed(7)}, ${longitude.toFixed(7)}`;
}

function roundDecimal(value: number) {
  return Number.isInteger(value) ? value.toString() : value.toFixed(2);
}

function statusLabel(status: FarmRecordStatus) {
  switch (status) {
    case "ACTIVE":
      return "Active";
    case "ARCHIVED":
      return "Archived";
    default:
      return "Inactive";
  }
}

function statusTone(status: FarmRecordStatus) {
  switch (status) {
    case "ACTIVE":
      return "good";
    case "ARCHIVED":
      return "neutral";
    default:
      return "warning";
  }
}

const styles = StyleSheet.create({
  panel: {
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    gap: 16,
    paddingTop: 14,
    width: "100%"
  },
  panelHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between"
  },
  panelTitleGroup: {
    flex: 1
  },
  panelTitle: {
    color: "#172126",
    fontSize: 16,
    fontWeight: "800"
  },
  panelMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  formSection: {
    gap: 12
  },
  listSection: {
    gap: 8
  },
  sectionLabel: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
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
    minWidth: 92,
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
  actionRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
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
  assetRow: {
    alignItems: "flex-start",
    borderTopColor: "#e6eef2",
    borderTopWidth: 1,
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-between",
    paddingTop: 10
  },
  assetText: {
    flex: 1
  },
  assetTitle: {
    color: "#172126",
    fontSize: 14,
    fontWeight: "800"
  },
  assetMeta: {
    color: "#53666f",
    fontSize: 13,
    fontWeight: "700",
    marginTop: 4
  },
  rowActions: {
    alignItems: "flex-end",
    gap: 8
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
