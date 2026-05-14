import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import {
  CropInputRuleRequest,
  CropInputRuleResponse,
  CropPlanStatus,
  FarmRecordStatus,
  InputCatalogRequest,
  InputCatalogResponse,
  InputDemandByCropResponse,
  InputDemandByInputResponse,
  InputDemandByVillageResponse,
  InputDemandEstimateResponse,
  InputDemandEstimateStatus,
  InputDemandRunRequest,
  InputDemandRunResponse,
  InputDemandSummaryResponse,
  UpdateFarmRecordStatusRequest
} from "../core/api/fpoContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { CropCatalog, CropPlan, getCropPlans } from "./cropPlanningStore";

export type InputCatalog = {
  category?: string;
  code: string;
  createdAt: string;
  id: string;
  name: string;
  status: FarmRecordStatus;
  tenantId?: string;
  unit: string;
  updatedAt: string;
};

export type CropInputRule = {
  applicationStage?: string;
  createdAt: string;
  cropCode: string;
  cropId: string;
  cropName: string;
  id: string;
  inputCode: string;
  inputId: string;
  inputName: string;
  inputUnit: string;
  notes?: string;
  quantityPerAcre: number;
  status: FarmRecordStatus;
  tenantId?: string;
  updatedAt: string;
};

export type InputDemandEstimate = {
  createdAt: string;
  cropCode: string;
  cropId: string;
  cropName: string;
  cropPlanId: string;
  estimatedQuantity: number;
  id: string;
  inputCategory?: string;
  inputCode: string;
  inputId: string;
  inputName: string;
  memberId: string;
  memberName: string;
  memberNumber: string;
  memberVillage: string;
  bufferPercent: number;
  bufferQuantity: number;
  finalDemandQuantity: number;
  plannedAreaAcres?: number;
  recommendedQuantityPerAcre: number;
  seasonCode: string;
  seasonId: string;
  seasonName: string;
  seasonYear: number;
  status: InputDemandEstimateStatus;
  tenantId?: string;
  totalDemandQuantity: number;
  unit: string;
  updatedAt: string;
};

export type InputDemandByInput = InputDemandByInputResponse;
export type InputDemandByCrop = InputDemandByCropResponse;
export type InputDemandByVillage = InputDemandByVillageResponse;

export type InputDemandSummary = {
  byCrop: InputDemandByCrop[];
  byInput: InputDemandByInput[];
  byVillage: InputDemandByVillage[];
  cropId?: string;
  estimateCount: number;
  memberCount: number;
  planCount: number;
  seasonId?: string;
  totalPlannedAreaAcres: number;
  village?: string;
};

export type InputCatalogInput = {
  category?: string;
  code: string;
  name: string;
  unit: string;
};

export type CropInputRuleInput = {
  applicationStage?: string;
  cropId: string;
  inputId: string;
  notes?: string;
  quantityPerAcre: string;
};

export type InputDemandRunInput = {
  cropId?: string;
  planStatus?: CropPlanStatus;
  seasonId: string;
  village?: string;
};

export type InputDemandFilters = {
  cropId?: string;
  seasonId?: string;
  village?: string;
};

export async function getInputCatalog(): Promise<InputCatalog[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<InputCatalogResponse[]>(
        endpoints.fpo.inputs.list,
        { accessToken }
      );
      const inputs = response.map(toInputCatalog);

      await writeJson(storageKeys.fpo.inputs, inputs);
      return inputs;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input catalog unavailable; using cached inputs.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalInputs();
}

export async function createInputCatalog(
  input: InputCatalogInput
): Promise<InputCatalog> {
  const request = toInputCatalogRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<InputCatalogRequest, InputCatalogResponse>(
        endpoints.fpo.inputs.create,
        request,
        { accessToken }
      );
      const created = toInputCatalog(response);

      await upsertLocalInput(created);
      return created;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input creation unavailable; using cached inputs.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  await ensureLocalInputCodeAvailable(request.code);
  return upsertLocalInput({
    category: request.category,
    code: request.code,
    createdAt: new Date().toISOString(),
    id: `local-input-${Date.now()}`,
    name: request.name,
    status: request.status ?? "ACTIVE",
    unit: request.unit,
    updatedAt: new Date().toISOString()
  });
}

export async function updateInputCatalog(
  input: InputCatalog,
  values: InputCatalogInput
): Promise<InputCatalog> {
  const request = { ...toInputCatalogRequest(values), status: input.status };
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.put<InputCatalogRequest, InputCatalogResponse>(
        endpoints.fpo.inputs.byId(input.id),
        request,
        { accessToken }
      );
      const updated = toInputCatalog(response);

      await upsertLocalInput(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input update unavailable; using cached inputs.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  await ensureLocalInputCodeAvailable(request.code, input.id);
  return upsertLocalInput({
    ...input,
    category: request.category,
    code: request.code,
    name: request.name,
    unit: request.unit,
    updatedAt: new Date().toISOString()
  });
}

export async function updateInputStatus(
  input: InputCatalog,
  status: FarmRecordStatus
): Promise<InputCatalog> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateFarmRecordStatusRequest,
        InputCatalogResponse
      >(endpoints.fpo.inputs.status(input.id), { status }, { accessToken });
      const updated = toInputCatalog(response);

      await upsertLocalInput(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input status unavailable; using cached inputs.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalInput({ ...input, status, updatedAt: new Date().toISOString() });
}

export async function getCropInputRules(
  filters: InputRuleFilters = {}
): Promise<CropInputRule[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<CropInputRuleResponse[]>(
        inputRuleListEndpoint(filters),
        { accessToken }
      );
      const rules = response.map(toCropInputRule);

      await writeJson(storageKeys.fpo.inputRules, rules);
      return rules;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input rules unavailable; using cached rules.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const rules = await getLocalRules();
  return rules.filter((rule) => matchesRuleFilters(rule, filters));
}

export async function createCropInputRule(
  input: CropInputRuleInput,
  crops: CropCatalog[],
  inputs: InputCatalog[]
): Promise<CropInputRule> {
  const request = toCropInputRuleRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<
        CropInputRuleRequest,
        CropInputRuleResponse
      >(endpoints.fpo.inputRules.create, request, { accessToken });
      const rule = toCropInputRule(response);

      await upsertLocalRule(rule);
      return rule;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input rule creation unavailable; using cached rules.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const crop = crops.find((item) => item.id === request.cropId);
  const inputRecord = inputs.find((item) => item.id === request.inputId);
  if (
    !crop ||
    crop.status !== "ACTIVE" ||
    !inputRecord ||
    inputRecord.status !== "ACTIVE"
  ) {
    throw new AppError("VALIDATION_FAILED", "Select active crop and input records.");
  }

  await ensureLocalRuleAvailable(
    request.cropId,
    request.inputId,
    request.applicationStage
  );
  return upsertLocalRule({
    applicationStage: request.applicationStage,
    createdAt: new Date().toISOString(),
    cropCode: crop.code,
    cropId: crop.id,
    cropName: crop.name,
    id: `local-input-rule-${Date.now()}`,
    inputCode: inputRecord.code,
    inputId: inputRecord.id,
    inputName: inputRecord.name,
    inputUnit: inputRecord.unit,
    notes: request.notes,
    quantityPerAcre: request.quantityPerAcre,
    status: request.status ?? "ACTIVE",
    updatedAt: new Date().toISOString()
  });
}

export async function updateCropInputRule(
  rule: CropInputRule,
  input: CropInputRuleInput,
  crops: CropCatalog[],
  inputs: InputCatalog[]
): Promise<CropInputRule> {
  const request = { ...toCropInputRuleRequest(input), status: rule.status };
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.put<CropInputRuleRequest, CropInputRuleResponse>(
        endpoints.fpo.inputRules.byId(rule.id),
        request,
        { accessToken }
      );
      const updated = toCropInputRule(response);

      await upsertLocalRule(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input rule update unavailable; using cached rules.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const crop = crops.find((item) => item.id === request.cropId);
  const inputRecord = inputs.find((item) => item.id === request.inputId);
  if (
    !crop ||
    crop.status !== "ACTIVE" ||
    !inputRecord ||
    inputRecord.status !== "ACTIVE"
  ) {
    throw new AppError("VALIDATION_FAILED", "Select active crop and input records.");
  }

  await ensureLocalRuleAvailable(
    request.cropId,
    request.inputId,
    request.applicationStage,
    rule.id
  );
  return upsertLocalRule({
    ...rule,
    applicationStage: request.applicationStage,
    cropCode: crop.code,
    cropId: crop.id,
    cropName: crop.name,
    inputCode: inputRecord.code,
    inputId: inputRecord.id,
    inputName: inputRecord.name,
    inputUnit: inputRecord.unit,
    notes: request.notes,
    quantityPerAcre: request.quantityPerAcre,
    updatedAt: new Date().toISOString()
  });
}

export async function updateCropInputRuleStatus(
  rule: CropInputRule,
  status: FarmRecordStatus
): Promise<CropInputRule> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        UpdateFarmRecordStatusRequest,
        CropInputRuleResponse
      >(endpoints.fpo.inputRules.status(rule.id), { status }, { accessToken });
      const updated = toCropInputRule(response);

      await upsertLocalRule(updated);
      return updated;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input rule status unavailable; using cached rules.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalRule({ ...rule, status, updatedAt: new Date().toISOString() });
}

export async function runInputDemand(
  input: InputDemandRunInput
): Promise<InputDemandRunResponse> {
  const request = toInputDemandRunRequest(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.post<
        InputDemandRunRequest,
        InputDemandRunResponse
      >(endpoints.fpo.demandEstimates.run, request, { accessToken });
      const estimates = response.estimates.map(toInputDemandEstimate);

      await replaceLocalEstimates(estimates);
      return response;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend input demand run unavailable; using local calculation.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return runLocalInputDemand(request);
}

export async function getInputDemandEstimates(
  filters: InputDemandFilters = {}
): Promise<InputDemandEstimate[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<InputDemandEstimateResponse[]>(
        demandEstimateEndpoint(endpoints.fpo.demandEstimates.list, filters),
        { accessToken }
      );
      const estimates = response.map(toInputDemandEstimate);

      await writeJson(storageKeys.fpo.demandEstimates, estimates);
      return estimates;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend demand estimates unavailable; using cached estimates.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  const estimates = await getLocalEstimates();
  return estimates.filter((estimate) => matchesEstimateFilters(estimate, filters));
}

export async function getInputDemandSummary(
  filters: InputDemandFilters = {}
): Promise<InputDemandSummary> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.get<InputDemandSummaryResponse>(
        demandEstimateEndpoint(endpoints.fpo.demandEstimates.summary, filters),
        { accessToken }
      );
      const summary = toInputDemandSummary(response);

      await writeJson(storageKeys.fpo.demandSummary, summary);
      return summary;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toInputDemandError(error);
      }

      logger.warn("Backend demand summary unavailable; using cached estimates.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return summarizeLocalDemand(filters);
}

type InputRuleFilters = {
  cropId?: string;
  inputId?: string;
  status?: FarmRecordStatus;
};

function toInputCatalog(response: InputCatalogResponse): InputCatalog {
  return {
    category: response.category ?? undefined,
    code: response.code,
    createdAt: response.createdAt,
    id: response.id,
    name: response.name,
    status: response.status,
    tenantId: response.tenantId,
    unit: response.unit,
    updatedAt: response.updatedAt
  };
}

function toCropInputRule(response: CropInputRuleResponse): CropInputRule {
  return {
    applicationStage: response.applicationStage ?? undefined,
    createdAt: response.createdAt,
    cropCode: response.cropCode,
    cropId: response.cropId,
    cropName: response.cropName,
    id: response.id,
    inputCode: response.inputCode,
    inputId: response.inputId,
    inputName: response.inputName,
    inputUnit: response.inputUnit,
    notes: response.notes ?? undefined,
    quantityPerAcre: response.quantityPerAcre,
    status: response.status,
    tenantId: response.tenantId,
    updatedAt: response.updatedAt
  };
}

function toInputDemandEstimate(
  response: InputDemandEstimateResponse
): InputDemandEstimate {
  return {
    createdAt: response.createdAt,
    cropCode: response.cropCode,
    cropId: response.cropId,
    cropName: response.cropName,
    cropPlanId: response.cropPlanId,
    estimatedQuantity: response.estimatedQuantity,
    id: response.id,
    inputCategory: response.inputCategory ?? undefined,
    inputCode: response.inputCode,
    inputId: response.inputId,
    inputName: response.inputName,
    memberId: response.memberId,
    memberName: response.memberName,
    memberNumber: response.memberNumber,
    memberVillage: response.memberVillage,
    bufferPercent: response.bufferPercent,
    bufferQuantity: response.bufferQuantity,
    finalDemandQuantity: response.finalDemandQuantity,
    recommendedQuantityPerAcre: response.recommendedQuantityPerAcre,
    seasonCode: response.seasonCode,
    seasonId: response.seasonId,
    seasonName: response.seasonName,
    seasonYear: response.seasonYear,
    status: response.status,
    tenantId: response.tenantId,
    totalDemandQuantity: response.totalDemandQuantity,
    unit: response.unit,
    updatedAt: response.updatedAt
  };
}

function toInputDemandSummary(
  response: InputDemandSummaryResponse
): InputDemandSummary {
  return {
    byCrop: response.byCrop,
    byInput: response.byInput,
    byVillage: response.byVillage,
    cropId: response.cropId ?? undefined,
    estimateCount: response.estimateCount,
    memberCount: response.memberCount,
    planCount: response.planCount,
    seasonId: response.seasonId ?? undefined,
    totalPlannedAreaAcres: response.totalPlannedAreaAcres,
    village: response.village ?? undefined
  };
}

function toInputCatalogRequest(input: InputCatalogInput): InputCatalogRequest {
  if (!input.code.trim() || !input.name.trim() || !input.unit.trim()) {
    throw new AppError("VALIDATION_FAILED", "Enter input code, name, and unit.");
  }

  return {
    category: cleanOptional(input.category),
    code: input.code.trim().toUpperCase(),
    name: input.name.trim(),
    status: "ACTIVE",
    unit: input.unit.trim().toUpperCase()
  };
}

function toCropInputRuleRequest(input: CropInputRuleInput): CropInputRuleRequest {
  if (!input.cropId || !input.inputId) {
    throw new AppError("VALIDATION_FAILED", "Select crop and input.");
  }

  return {
    applicationStage: cleanOptional(input.applicationStage),
    cropId: input.cropId,
    inputId: input.inputId,
    notes: cleanOptional(input.notes),
    quantityPerAcre: parsePositiveNumber(input.quantityPerAcre, "Quantity per acre"),
    status: "ACTIVE"
  };
}

function toInputDemandRunRequest(input: InputDemandRunInput): InputDemandRunRequest {
  if (!input.seasonId) {
    throw new AppError(
      "VALIDATION_FAILED",
      "Select a season before calculating demand."
    );
  }

  return {
    cropId: cleanOptional(input.cropId),
    planStatus: input.planStatus ?? "CONFIRMED",
    seasonId: input.seasonId,
    village: cleanOptional(input.village)
  };
}

async function runLocalInputDemand(
  request: InputDemandRunRequest
): Promise<InputDemandRunResponse> {
  const [plans, rules, inputs] = await Promise.all([
    getCropPlans({
      cropId: request.cropId,
      seasonId: request.seasonId,
      status: request.planStatus ?? "CONFIRMED"
    }),
    getLocalRules(),
    getLocalInputs()
  ]);
  const activeInputsById = new Map(
    inputs
      .filter((input) => input.status === "ACTIVE")
      .map((input) => [input.id, input])
  );
  const matchingPlans = plans.filter(
    (plan) =>
      !request.village ||
      plan.memberVillage.toLowerCase() === request.village.toLowerCase()
  );
  const now = new Date().toISOString();
  const estimates: InputDemandEstimate[] = [];
  let missingRulePlanCount = 0;

  for (const plan of matchingPlans) {
    const activeRules = rules.filter(
      (rule) =>
        rule.status === "ACTIVE" &&
        rule.cropId === plan.cropId &&
        activeInputsById.has(rule.inputId)
    );

    if (!activeRules.length) {
      missingRulePlanCount += 1;
      continue;
    }

    const quantities = activeRules.reduce((accumulator, rule) => {
      const input = activeInputsById.get(rule.inputId);
      if (!input) {
        return accumulator;
      }

      const current = accumulator.get(rule.inputId);
      const totalDemandQuantity = roundQuantity(
        plan.plannedAreaAcres * rule.quantityPerAcre
      );
      const recommendedQuantityPerAcre = roundQuantity(
        (current?.recommendedQuantityPerAcre ?? 0) + rule.quantityPerAcre
      );
      const nextTotalDemandQuantity = roundQuantity(
        (current?.totalDemandQuantity ?? 0) + totalDemandQuantity
      );
      const calculated = calculateDemandQuantities(nextTotalDemandQuantity);
      accumulator.set(rule.inputId, {
        ...calculated,
        input,
        recommendedQuantityPerAcre,
        totalDemandQuantity: nextTotalDemandQuantity
      });
      return accumulator;
    }, new Map<string, LocalInputQuantity>());

    quantities.forEach((quantity) => {
      estimates.push({
        bufferPercent: quantity.bufferPercent,
        bufferQuantity: quantity.bufferQuantity,
        createdAt: now,
        cropCode: plan.cropCode,
        cropId: plan.cropId,
        cropName: plan.cropName,
        cropPlanId: plan.id,
        estimatedQuantity: quantity.finalDemandQuantity,
        finalDemandQuantity: quantity.finalDemandQuantity,
        id: `local-demand-${plan.id}-${quantity.input.id}`,
        inputCategory: quantity.input.category,
        inputCode: quantity.input.code,
        inputId: quantity.input.id,
        inputName: quantity.input.name,
        memberId: plan.memberId,
        memberName: plan.memberName,
        memberNumber: plan.memberNumber,
        memberVillage: plan.memberVillage,
        plannedAreaAcres: plan.plannedAreaAcres,
        recommendedQuantityPerAcre: quantity.recommendedQuantityPerAcre,
        seasonCode: plan.seasonCode,
        seasonId: plan.seasonId,
        seasonName: plan.seasonName,
        seasonYear: plan.seasonYear,
        status: "ESTIMATED",
        totalDemandQuantity: quantity.totalDemandQuantity,
        unit: quantity.input.unit,
        updatedAt: now
      });
    });
  }

  await replaceLocalEstimates(estimates);
  return {
    cropId: request.cropId ?? null,
    estimates: estimates.map(toEstimateResponse),
    estimatesGenerated: estimates.length,
    missingRulePlanCount,
    planStatus: request.planStatus ?? "CONFIRMED",
    plansConsidered: matchingPlans.length,
    seasonId: request.seasonId,
    village: request.village ?? null
  };
}

async function summarizeLocalDemand(
  filters: InputDemandFilters
): Promise<InputDemandSummary> {
  const [estimates, plans] = await Promise.all([
    getInputDemandEstimates(filters),
    getCropPlans({
      cropId: filters.cropId,
      seasonId: filters.seasonId,
      status: "CONFIRMED"
    })
  ]);
  const plansById = new Map(plans.map((plan) => [plan.id, plan]));
  const uniquePlanIds = [...new Set(estimates.map((estimate) => estimate.cropPlanId))];
  const uniquePlans = uniquePlanIds
    .map((planId) => plansById.get(planId) ?? planFromEstimate(estimates, planId))
    .filter((plan): plan is CropPlan => Boolean(plan));
  const memberCount = new Set(uniquePlans.map((plan) => plan.memberId)).size;

  const summary: InputDemandSummary = {
    byCrop: cropBreakdowns(uniquePlans),
    byInput: inputBreakdowns(estimates),
    byVillage: villageBreakdowns(uniquePlans),
    cropId: filters.cropId,
    estimateCount: estimates.length,
    memberCount,
    planCount: uniquePlans.length,
    seasonId: filters.seasonId,
    totalPlannedAreaAcres: sumPlannedArea(uniquePlans),
    village: cleanOptional(filters.village)
  };

  await writeJson(storageKeys.fpo.demandSummary, summary);
  return summary;
}

function toEstimateResponse(
  estimate: InputDemandEstimate
): InputDemandEstimateResponse {
  return {
    createdAt: estimate.createdAt,
    cropCode: estimate.cropCode,
    cropId: estimate.cropId,
    cropName: estimate.cropName,
    cropPlanId: estimate.cropPlanId,
    estimatedQuantity: estimate.estimatedQuantity,
    id: estimate.id,
    inputCategory: estimate.inputCategory ?? null,
    inputCode: estimate.inputCode,
    inputId: estimate.inputId,
    inputName: estimate.inputName,
    memberId: estimate.memberId,
    memberName: estimate.memberName,
    memberNumber: estimate.memberNumber,
    memberVillage: estimate.memberVillage,
    bufferPercent: estimate.bufferPercent,
    bufferQuantity: estimate.bufferQuantity,
    finalDemandQuantity: estimate.finalDemandQuantity,
    recommendedQuantityPerAcre: estimate.recommendedQuantityPerAcre,
    seasonCode: estimate.seasonCode,
    seasonId: estimate.seasonId,
    seasonName: estimate.seasonName,
    seasonYear: estimate.seasonYear,
    status: estimate.status,
    tenantId: estimate.tenantId ?? "",
    totalDemandQuantity: estimate.totalDemandQuantity,
    unit: estimate.unit,
    updatedAt: estimate.updatedAt
  };
}

function inputBreakdowns(estimates: InputDemandEstimate[]): InputDemandByInput[] {
  const groups = new Map<string, InputDemandEstimate[]>();
  estimates.forEach((estimate) => {
    groups.set(estimate.inputId, [...(groups.get(estimate.inputId) ?? []), estimate]);
  });

  return [...groups.values()]
    .map((group) => {
      const first = group[0];
      return {
        estimatedQuantity: roundQuantity(
          group.reduce((total, estimate) => total + estimate.estimatedQuantity, 0)
        ),
        bufferQuantity: roundQuantity(
          group.reduce((total, estimate) => total + estimate.bufferQuantity, 0)
        ),
        finalDemandQuantity: roundQuantity(
          group.reduce((total, estimate) => total + estimate.finalDemandQuantity, 0)
        ),
        inputCode: first.inputCode,
        inputId: first.inputId,
        inputName: first.inputName,
        planCount: new Set(group.map((estimate) => estimate.cropPlanId)).size,
        totalDemandQuantity: roundQuantity(
          group.reduce((total, estimate) => total + estimate.totalDemandQuantity, 0)
        ),
        unit: first.unit
      };
    })
    .sort((left, right) => left.inputName.localeCompare(right.inputName));
}

function cropBreakdowns(plans: CropPlan[]): InputDemandByCrop[] {
  const groups = new Map<string, CropPlan[]>();
  plans.forEach((plan) => {
    groups.set(plan.cropId, [...(groups.get(plan.cropId) ?? []), plan]);
  });

  return [...groups.values()]
    .map((group) => ({
      cropId: group[0].cropId,
      cropName: group[0].cropName,
      plannedAreaAcres: sumPlannedArea(group),
      planCount: group.length
    }))
    .sort((left, right) => left.cropName.localeCompare(right.cropName));
}

function villageBreakdowns(plans: CropPlan[]): InputDemandByVillage[] {
  const groups = new Map<string, CropPlan[]>();
  plans.forEach((plan) => {
    groups.set(plan.memberVillage, [...(groups.get(plan.memberVillage) ?? []), plan]);
  });

  return [...groups.values()]
    .map((group) => ({
      memberCount: new Set(group.map((plan) => plan.memberId)).size,
      planCount: group.length,
      plannedAreaAcres: sumPlannedArea(group),
      village: group[0].memberVillage
    }))
    .sort((left, right) => left.village.localeCompare(right.village));
}

function planFromEstimate(
  estimates: InputDemandEstimate[],
  planId: string
): CropPlan | null {
  const estimate = estimates.find((item) => item.cropPlanId === planId);
  if (!estimate || estimate.plannedAreaAcres === undefined) {
    return null;
  }

  return {
    createdAt: estimate.createdAt,
    cropCode: estimate.cropCode,
    cropId: estimate.cropId,
    cropName: estimate.cropName,
    id: estimate.cropPlanId,
    memberId: estimate.memberId,
    memberName: estimate.memberName,
    memberNumber: estimate.memberNumber,
    memberVillage: estimate.memberVillage,
    plannedAreaAcres: estimate.plannedAreaAcres,
    seasonCode: estimate.seasonCode,
    seasonId: estimate.seasonId,
    seasonName: estimate.seasonName,
    seasonYear: estimate.seasonYear,
    cropYear: formatCropYear(estimate.seasonYear),
    status: "CONFIRMED",
    updatedAt: estimate.updatedAt
  };
}

function formatCropYear(seasonYear: number) {
  return `${seasonYear}-${String((seasonYear + 1) % 100).padStart(2, "0")}`;
}

function toStoredInput(input: Partial<InputCatalog>): InputCatalog | null {
  if (
    typeof input.id !== "string" ||
    typeof input.code !== "string" ||
    typeof input.name !== "string" ||
    typeof input.unit !== "string" ||
    typeof input.createdAt !== "string" ||
    typeof input.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    category: input.category,
    code: input.code,
    createdAt: input.createdAt,
    id: input.id,
    name: input.name,
    status: input.status ?? "ACTIVE",
    tenantId: input.tenantId,
    unit: input.unit,
    updatedAt: input.updatedAt
  };
}

function toStoredRule(rule: Partial<CropInputRule>): CropInputRule | null {
  if (
    typeof rule.id !== "string" ||
    typeof rule.cropId !== "string" ||
    typeof rule.cropCode !== "string" ||
    typeof rule.cropName !== "string" ||
    typeof rule.inputId !== "string" ||
    typeof rule.inputCode !== "string" ||
    typeof rule.inputName !== "string" ||
    typeof rule.inputUnit !== "string" ||
    typeof rule.quantityPerAcre !== "number" ||
    typeof rule.createdAt !== "string" ||
    typeof rule.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    applicationStage: rule.applicationStage,
    createdAt: rule.createdAt,
    cropCode: rule.cropCode,
    cropId: rule.cropId,
    cropName: rule.cropName,
    id: rule.id,
    inputCode: rule.inputCode,
    inputId: rule.inputId,
    inputName: rule.inputName,
    inputUnit: rule.inputUnit,
    notes: rule.notes,
    quantityPerAcre: rule.quantityPerAcre,
    status: rule.status ?? "ACTIVE",
    tenantId: rule.tenantId,
    updatedAt: rule.updatedAt
  };
}

function toStoredEstimate(
  estimate: Partial<InputDemandEstimate>
): InputDemandEstimate | null {
  if (
    typeof estimate.id !== "string" ||
    typeof estimate.cropPlanId !== "string" ||
    typeof estimate.cropId !== "string" ||
    typeof estimate.cropCode !== "string" ||
    typeof estimate.cropName !== "string" ||
    typeof estimate.inputId !== "string" ||
    typeof estimate.inputCode !== "string" ||
    typeof estimate.inputName !== "string" ||
    typeof estimate.estimatedQuantity !== "number" ||
    typeof estimate.bufferPercent !== "number" ||
    typeof estimate.bufferQuantity !== "number" ||
    typeof estimate.finalDemandQuantity !== "number" ||
    typeof estimate.recommendedQuantityPerAcre !== "number" ||
    typeof estimate.totalDemandQuantity !== "number" ||
    typeof estimate.memberId !== "string" ||
    typeof estimate.memberName !== "string" ||
    typeof estimate.memberNumber !== "string" ||
    typeof estimate.memberVillage !== "string" ||
    typeof estimate.seasonId !== "string" ||
    typeof estimate.seasonCode !== "string" ||
    typeof estimate.seasonName !== "string" ||
    typeof estimate.seasonYear !== "number" ||
    typeof estimate.unit !== "string" ||
    typeof estimate.createdAt !== "string" ||
    typeof estimate.updatedAt !== "string"
  ) {
    return null;
  }

  return {
    createdAt: estimate.createdAt,
    cropCode: estimate.cropCode,
    cropId: estimate.cropId,
    cropName: estimate.cropName,
    cropPlanId: estimate.cropPlanId,
    estimatedQuantity: estimate.estimatedQuantity,
    id: estimate.id,
    inputCategory: estimate.inputCategory,
    inputCode: estimate.inputCode,
    inputId: estimate.inputId,
    inputName: estimate.inputName,
    memberId: estimate.memberId,
    memberName: estimate.memberName,
    memberNumber: estimate.memberNumber,
    memberVillage: estimate.memberVillage,
    bufferPercent: estimate.bufferPercent,
    bufferQuantity: estimate.bufferQuantity,
    finalDemandQuantity: estimate.finalDemandQuantity,
    plannedAreaAcres: estimate.plannedAreaAcres,
    recommendedQuantityPerAcre: estimate.recommendedQuantityPerAcre,
    seasonCode: estimate.seasonCode,
    seasonId: estimate.seasonId,
    seasonName: estimate.seasonName,
    seasonYear: estimate.seasonYear,
    status: estimate.status ?? "ESTIMATED",
    tenantId: estimate.tenantId,
    totalDemandQuantity: estimate.totalDemandQuantity,
    unit: estimate.unit,
    updatedAt: estimate.updatedAt
  };
}

async function getLocalInputs() {
  const saved = await readJsonArray<Partial<InputCatalog>>([storageKeys.fpo.inputs]);
  return saved
    .map(toStoredInput)
    .filter((input): input is InputCatalog => Boolean(input));
}

async function upsertLocalInput(input: InputCatalog) {
  const current = await getLocalInputs();
  await writeJson(storageKeys.fpo.inputs, [
    input,
    ...current.filter((item) => item.id !== input.id)
  ]);
  return input;
}

async function ensureLocalInputCodeAvailable(code: string, currentId?: string) {
  const inputs = await getLocalInputs();
  if (
    inputs.some(
      (input) =>
        input.id !== currentId && input.code.toLowerCase() === code.toLowerCase()
    )
  ) {
    throw new AppError("DUPLICATE_RESOURCE", "Input code already exists.");
  }
}

async function getLocalRules() {
  const saved = await readJsonArray<Partial<CropInputRule>>([
    storageKeys.fpo.inputRules
  ]);
  return saved.map(toStoredRule).filter((rule): rule is CropInputRule => Boolean(rule));
}

async function upsertLocalRule(rule: CropInputRule) {
  const current = await getLocalRules();
  await writeJson(storageKeys.fpo.inputRules, [
    rule,
    ...current.filter((item) => item.id !== rule.id)
  ]);
  return rule;
}

async function ensureLocalRuleAvailable(
  cropId: string,
  inputId: string,
  applicationStage?: string,
  currentId?: string
) {
  const stageKey = normalizeStage(applicationStage);
  const rules = await getLocalRules();
  if (
    rules.some(
      (rule) =>
        rule.id !== currentId &&
        rule.cropId === cropId &&
        rule.inputId === inputId &&
        normalizeStage(rule.applicationStage) === stageKey
    )
  ) {
    throw new AppError(
      "DUPLICATE_RESOURCE",
      "Input rule already exists for this crop, input, and stage."
    );
  }
}

async function getLocalEstimates() {
  const saved = await readJsonArray<Partial<InputDemandEstimate>>([
    storageKeys.fpo.demandEstimates
  ]);
  return saved
    .map(toStoredEstimate)
    .filter((estimate): estimate is InputDemandEstimate => Boolean(estimate));
}

async function replaceLocalEstimates(estimates: InputDemandEstimate[]) {
  const current = await getLocalEstimates();
  const replacedKeys = new Set(
    estimates.map((estimate) => `${estimate.cropPlanId}:${estimate.inputId}`)
  );
  await writeJson(storageKeys.fpo.demandEstimates, [
    ...estimates,
    ...current.filter(
      (estimate) => !replacedKeys.has(`${estimate.cropPlanId}:${estimate.inputId}`)
    )
  ]);
}

function inputRuleListEndpoint(filters: InputRuleFilters) {
  const params = new URLSearchParams();
  if (filters.cropId) params.append("cropId", filters.cropId);
  if (filters.inputId) params.append("inputId", filters.inputId);
  if (filters.status) params.append("status", filters.status);
  const query = params.toString();
  return query
    ? `${endpoints.fpo.inputRules.list}?${query}`
    : endpoints.fpo.inputRules.list;
}

function demandEstimateEndpoint(basePath: string, filters: InputDemandFilters) {
  const params = new URLSearchParams();
  if (filters.cropId) params.append("cropId", filters.cropId);
  if (filters.seasonId) params.append("seasonId", filters.seasonId);
  if (filters.village) params.append("village", filters.village);
  const query = params.toString();
  return query ? `${basePath}?${query}` : basePath;
}

function matchesRuleFilters(rule: CropInputRule, filters: InputRuleFilters) {
  return (
    (!filters.cropId || rule.cropId === filters.cropId) &&
    (!filters.inputId || rule.inputId === filters.inputId) &&
    (!filters.status || rule.status === filters.status)
  );
}

function matchesEstimateFilters(
  estimate: InputDemandEstimate,
  filters: InputDemandFilters
) {
  return (
    (!filters.cropId || estimate.cropId === filters.cropId) &&
    (!filters.seasonId || estimate.seasonId === filters.seasonId) &&
    (!filters.village ||
      estimate.memberVillage.toLowerCase() === filters.village.toLowerCase())
  );
}

function sumPlannedArea(plans: CropPlan[]) {
  return roundQuantity(plans.reduce((total, plan) => total + plan.plannedAreaAcres, 0));
}

type LocalInputQuantity = {
  bufferPercent: number;
  bufferQuantity: number;
  finalDemandQuantity: number;
  input: InputCatalog;
  recommendedQuantityPerAcre: number;
  totalDemandQuantity: number;
};

function calculateDemandQuantities(totalDemandQuantity: number) {
  const bufferPercent = 5;
  const bufferQuantity = roundQuantity((totalDemandQuantity * bufferPercent) / 100);
  return {
    bufferPercent,
    bufferQuantity,
    finalDemandQuantity: Math.ceil(totalDemandQuantity + bufferQuantity)
  };
}

function roundQuantity(value: number) {
  return Math.round((value + Number.EPSILON) * 10000) / 10000;
}

function parsePositiveNumber(value: string, label: string) {
  const parsed = Number(value.trim());
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new AppError("VALIDATION_FAILED", `${label} must be greater than zero.`);
  }

  return parsed;
}

function cleanOptional(value: string | undefined) {
  const trimmed = value?.trim();
  return trimmed || undefined;
}

function normalizeStage(applicationStage: string | undefined) {
  return cleanOptional(applicationStage)?.toLowerCase() ?? "";
}

async function getAccessToken() {
  return AsyncStorage.getItem(storageKeys.auth.accessToken);
}

function canUseLocalFallback(error: unknown) {
  return !(error instanceof ApiClientError);
}

function toInputDemandError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 409) {
      return new AppError("DUPLICATE_RESOURCE", error.message);
    }

    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", error.message);
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError("ACCESS_DENIED", error.message);
    }

    return new AppError("API_REQUEST_FAILED", error.message);
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage input demand.");
}
