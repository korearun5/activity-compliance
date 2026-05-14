package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.CropInputRuleRequest;
import com.activityplatform.backend.fpo.api.CropInputRuleResponse;
import com.activityplatform.backend.fpo.api.InputCatalogRequest;
import com.activityplatform.backend.fpo.api.InputCatalogResponse;
import com.activityplatform.backend.fpo.api.InputDemandByCropResponse;
import com.activityplatform.backend.fpo.api.InputDemandByInputResponse;
import com.activityplatform.backend.fpo.api.InputDemandByVillageResponse;
import com.activityplatform.backend.fpo.api.InputDemandEstimateResponse;
import com.activityplatform.backend.fpo.api.InputDemandRunRequest;
import com.activityplatform.backend.fpo.api.InputDemandRunResponse;
import com.activityplatform.backend.fpo.api.InputDemandSummaryResponse;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropInputRuleEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateStatus;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropInputRuleRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.InputCatalogRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InputDemandService {
  private static final int QUANTITY_SCALE = 4;
  private static final BigDecimal PHASE1_BUFFER_PERCENT = new BigDecimal("5.00");
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private final AuditEventService auditEventService;
  private final CropCatalogRepository cropRepository;
  private final CropInputRuleRepository ruleRepository;
  private final CropSeasonRepository seasonRepository;
  private final InputCatalogRepository inputRepository;
  private final InputDemandEstimateRepository estimateRepository;
  private final SeasonalCropPlanRepository cropPlanRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public InputDemandService(
      AuditEventService auditEventService,
      CropCatalogRepository cropRepository,
      CropInputRuleRepository ruleRepository,
      CropSeasonRepository seasonRepository,
      InputCatalogRepository inputRepository,
      InputDemandEstimateRepository estimateRepository,
      SeasonalCropPlanRepository cropPlanRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.cropRepository = cropRepository;
    this.ruleRepository = ruleRepository;
    this.seasonRepository = seasonRepository;
    this.inputRepository = inputRepository;
    this.estimateRepository = estimateRepository;
    this.cropPlanRepository = cropPlanRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<InputCatalogResponse> listInputs(CurrentUser currentUser) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    return inputRepository.findByTenantIdOrderByNameAsc(currentUser.tenantId()).stream()
        .map(InputCatalogResponse::from)
        .toList();
  }

  @Transactional
  public InputCatalogResponse createInput(CurrentUser currentUser, InputCatalogRequest request) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    String code = normalizeCode(request.code());
    ensureInputCodeAvailable(currentUser.tenantId(), code, null);
    Instant now = Instant.now();
    InputCatalogEntity input = new InputCatalogEntity(
        UUID.randomUUID(),
        requireTenant(currentUser.tenantId()),
        code,
        request.name().trim(),
        normalizeOptional(request.category()),
        normalizeCode(request.unit()),
        request.status() == null ? FarmRecordStatus.ACTIVE : request.status(),
        now
    );

    InputCatalogEntity saved = inputRepository.save(input);
    auditInput(currentUser, saved, AuditAction.FPO_INPUT_CREATED);
    return InputCatalogResponse.from(saved);
  }

  @Transactional
  public InputCatalogResponse updateInput(
      CurrentUser currentUser,
      UUID inputId,
      InputCatalogRequest request
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    InputCatalogEntity input = requireInput(currentUser, inputId);
    String code = normalizeCode(request.code());
    ensureInputCodeAvailable(currentUser.tenantId(), code, input.getId());
    input.updateDetails(
        code,
        request.name().trim(),
        normalizeOptional(request.category()),
        normalizeCode(request.unit()),
        request.status() == null ? input.getStatus() : request.status(),
        Instant.now()
    );

    InputCatalogEntity saved = inputRepository.save(input);
    auditInput(currentUser, saved, AuditAction.FPO_INPUT_UPDATED);
    return InputCatalogResponse.from(saved);
  }

  @Transactional
  public InputCatalogResponse updateInputStatus(
      CurrentUser currentUser,
      UUID inputId,
      FarmRecordStatus status
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    InputCatalogEntity input = requireInput(currentUser, inputId);
    input.updateStatus(status, Instant.now());
    InputCatalogEntity saved = inputRepository.save(input);
    auditInput(currentUser, saved, AuditAction.FPO_INPUT_STATUS_CHANGED);
    return InputCatalogResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<CropInputRuleResponse> listRules(
      CurrentUser currentUser,
      UUID cropId,
      UUID inputId,
      FarmRecordStatus status
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    return ruleRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId()).stream()
        .filter(rule -> cropId == null || rule.getCrop().getId().equals(cropId))
        .filter(rule -> inputId == null || rule.getInput().getId().equals(inputId))
        .filter(rule -> status == null || rule.getStatus() == status)
        .map(CropInputRuleResponse::from)
        .toList();
  }

  @Transactional
  public CropInputRuleResponse createRule(
      CurrentUser currentUser,
      CropInputRuleRequest request
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    CropCatalogEntity crop = requireActiveCrop(currentUser, request.cropId());
    InputCatalogEntity input = requireActiveInput(currentUser, request.inputId());
    String stage = normalizeOptional(request.applicationStage());
    ensureRuleAvailable(currentUser.tenantId(), crop.getId(), input.getId(), stage, null);
    Instant now = Instant.now();
    CropInputRuleEntity rule = new CropInputRuleEntity(
        UUID.randomUUID(),
        crop.getTenant(),
        crop,
        input,
        request.quantityPerAcre(),
        stage,
        normalizeOptional(request.notes()),
        request.status() == null ? FarmRecordStatus.ACTIVE : request.status(),
        now
    );

    CropInputRuleEntity saved = ruleRepository.save(rule);
    auditRule(currentUser, saved, AuditAction.FPO_INPUT_RULE_CREATED);
    return CropInputRuleResponse.from(saved);
  }

  @Transactional
  public CropInputRuleResponse updateRule(
      CurrentUser currentUser,
      UUID ruleId,
      CropInputRuleRequest request
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    CropInputRuleEntity rule = requireRule(currentUser, ruleId);
    CropCatalogEntity crop = requireActiveCrop(currentUser, request.cropId());
    InputCatalogEntity input = requireActiveInput(currentUser, request.inputId());
    String stage = normalizeOptional(request.applicationStage());
    ensureRuleAvailable(currentUser.tenantId(), crop.getId(), input.getId(), stage, rule.getId());
    rule.updateDetails(
        crop,
        input,
        request.quantityPerAcre(),
        stage,
        normalizeOptional(request.notes()),
        request.status() == null ? rule.getStatus() : request.status(),
        Instant.now()
    );

    CropInputRuleEntity saved = ruleRepository.save(rule);
    auditRule(currentUser, saved, AuditAction.FPO_INPUT_RULE_UPDATED);
    return CropInputRuleResponse.from(saved);
  }

  @Transactional
  public CropInputRuleResponse updateRuleStatus(
      CurrentUser currentUser,
      UUID ruleId,
      FarmRecordStatus status
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    CropInputRuleEntity rule = requireRule(currentUser, ruleId);
    rule.updateStatus(status, Instant.now());
    CropInputRuleEntity saved = ruleRepository.save(rule);
    auditRule(currentUser, saved, AuditAction.FPO_INPUT_RULE_STATUS_CHANGED);
    return CropInputRuleResponse.from(saved);
  }

  @Transactional
  public InputDemandRunResponse runDemandEstimate(
      CurrentUser currentUser,
      InputDemandRunRequest request
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    CropSeasonEntity season = requireSeason(currentUser, request.seasonId());
    CropCatalogEntity crop = request.cropId() == null
        ? null
        : requireActiveCrop(currentUser, request.cropId());
    String village = normalizeOptional(request.village());
    CropPlanStatus planStatus = requireConfirmedPlanStatus(request.planStatus());
    List<SeasonalCropPlanEntity> plans = matchingPlans(
        currentUser,
        season.getId(),
        crop == null ? null : crop.getId(),
        village,
        planStatus
    );
    int missingRulePlanCount = 0;
    List<InputDemandEstimateEntity> savedEstimates = new java.util.ArrayList<>();
    Instant now = Instant.now();

    for (SeasonalCropPlanEntity plan : plans) {
      List<CropInputRuleEntity> activeRules = ruleRepository
          .findByTenantIdAndCropIdAndStatus(
              currentUser.tenantId(),
              plan.getCrop().getId(),
              FarmRecordStatus.ACTIVE
          )
          .stream()
          .filter(rule -> rule.getInput().getStatus() == FarmRecordStatus.ACTIVE)
          .toList();

      if (activeRules.isEmpty()) {
        missingRulePlanCount++;
        continue;
      }

      Map<UUID, InputQuantity> quantitiesByInput = calculateQuantities(plan, activeRules);
      for (InputQuantity quantity : quantitiesByInput.values()) {
        savedEstimates.add(saveEstimate(currentUser, plan, quantity, now));
      }
    }

    auditEventService.record(
        season.getTenant(),
        actor(currentUser),
        "FPO_INPUT_DEMAND",
        season.getId(),
        AuditAction.FPO_INPUT_DEMAND_CALCULATED,
        Map.of(
            "plansConsidered", plans.size(),
            "estimatesGenerated", savedEstimates.size(),
            "missingRulePlanCount", missingRulePlanCount
        )
    );

    return new InputDemandRunResponse(
        season.getId(),
        crop == null ? null : crop.getId(),
        village,
        planStatus.name(),
        plans.size(),
        missingRulePlanCount,
        savedEstimates.size(),
        savedEstimates.stream().map(InputDemandEstimateResponse::from).toList()
    );
  }

  @Transactional(readOnly = true)
  public List<InputDemandEstimateResponse> listEstimates(
      CurrentUser currentUser,
      UUID seasonId,
      UUID cropId,
      String village
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    return matchingEstimates(currentUser, seasonId, cropId, normalizeOptional(village)).stream()
        .map(InputDemandEstimateResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public InputDemandSummaryResponse summarize(
      CurrentUser currentUser,
      UUID seasonId,
      UUID cropId,
      String village
  ) {
    requireInputDemandModule(currentUser);
    requireManager(currentUser);
    String normalizedVillage = normalizeOptional(village);
    List<InputDemandEstimateEntity> estimates = matchingEstimates(
        currentUser,
        seasonId,
        cropId,
        normalizedVillage
    );
    Map<UUID, SeasonalCropPlanEntity> uniquePlans = estimates.stream()
        .map(InputDemandEstimateEntity::getCropPlan)
        .collect(Collectors.toMap(
            SeasonalCropPlanEntity::getId,
            Function.identity(),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    Set<UUID> memberIds = uniquePlans.values().stream()
        .map(plan -> plan.getMemberProfile().getId())
        .collect(Collectors.toSet());

    return new InputDemandSummaryResponse(
        seasonId,
        cropId,
        normalizedVillage,
        uniquePlans.size(),
        memberIds.size(),
        estimates.size(),
        sumPlans(uniquePlans.values().stream().toList()),
        inputBreakdowns(estimates),
        cropBreakdowns(uniquePlans.values().stream().toList()),
        villageBreakdowns(uniquePlans.values().stream().toList())
    );
  }

  private void requireInputDemandModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.INPUT_DEMAND);
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private CropCatalogEntity requireCrop(CurrentUser currentUser, UUID cropId) {
    return cropRepository.findByIdAndTenantId(cropId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop not found."));
  }

  private CropCatalogEntity requireActiveCrop(CurrentUser currentUser, UUID cropId) {
    CropCatalogEntity crop = requireCrop(currentUser, cropId);
    if (crop.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Crop must be active.");
    }
    return crop;
  }

  private CropSeasonEntity requireSeason(CurrentUser currentUser, UUID seasonId) {
    return seasonRepository.findByIdAndTenantId(seasonId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop season not found."));
  }

  private InputCatalogEntity requireInput(CurrentUser currentUser, UUID inputId) {
    return inputRepository.findByIdAndTenantId(inputId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Input not found."));
  }

  private InputCatalogEntity requireActiveInput(CurrentUser currentUser, UUID inputId) {
    InputCatalogEntity input = requireInput(currentUser, inputId);
    if (input.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Input must be active.");
    }
    return input;
  }

  private CropInputRuleEntity requireRule(CurrentUser currentUser, UUID ruleId) {
    return ruleRepository.findByIdAndTenantId(ruleId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Input rule not found."));
  }

  private List<SeasonalCropPlanEntity> matchingPlans(
      CurrentUser currentUser,
      UUID seasonId,
      UUID cropId,
      String village,
      CropPlanStatus planStatus
  ) {
    return cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId()).stream()
        .filter(plan -> seasonId == null || plan.getSeason().getId().equals(seasonId))
        .filter(plan -> cropId == null || plan.getCrop().getId().equals(cropId))
        .filter(plan -> planStatus == null || plan.getStatus() == planStatus)
        .filter(plan -> village == null || plan.getMemberProfile().getVillage().equalsIgnoreCase(village))
        .toList();
  }

  private List<InputDemandEstimateEntity> matchingEstimates(
      CurrentUser currentUser,
      UUID seasonId,
      UUID cropId,
      String village
  ) {
    return estimateRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId()).stream()
        .filter(estimate -> seasonId == null
            || estimate.getCropPlan().getSeason().getId().equals(seasonId))
        .filter(estimate -> cropId == null
            || estimate.getCropPlan().getCrop().getId().equals(cropId))
        .filter(estimate -> village == null
            || estimate.getCropPlan().getMemberProfile().getVillage().equalsIgnoreCase(village))
        .toList();
  }

  private Map<UUID, InputQuantity> calculateQuantities(
      SeasonalCropPlanEntity plan,
      List<CropInputRuleEntity> rules
  ) {
    Map<UUID, InputQuantity> quantitiesByInput = new LinkedHashMap<>();
    for (CropInputRuleEntity rule : rules) {
      BigDecimal totalDemandQuantity = plan.getPlannedAreaAcres()
          .multiply(rule.getQuantityPerAcre())
          .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
      quantitiesByInput.merge(
          rule.getInput().getId(),
          InputQuantity.fromRule(rule.getInput(), rule.getQuantityPerAcre(), totalDemandQuantity),
          InputQuantity::add
      );
    }
    return quantitiesByInput;
  }

  private InputDemandEstimateEntity saveEstimate(
      CurrentUser currentUser,
      SeasonalCropPlanEntity plan,
      InputQuantity quantity,
      Instant now
  ) {
    InputDemandEstimateEntity estimate = estimateRepository
        .findByTenantIdAndCropPlanIdAndInputId(
            currentUser.tenantId(),
            plan.getId(),
            quantity.input().getId()
        )
        .orElseGet(() -> new InputDemandEstimateEntity(
            UUID.randomUUID(),
            plan.getTenant(),
            plan,
            quantity.input(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            PHASE1_BUFFER_PERCENT,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            quantity.input().getUnit(),
            InputDemandEstimateStatus.ESTIMATED,
            now
        ));
    estimate.updateEstimate(
        quantity.finalDemandQuantity(),
        quantity.recommendedQuantityPerAcre(),
        quantity.totalDemandQuantity(),
        quantity.bufferPercent(),
        quantity.bufferQuantity(),
        quantity.finalDemandQuantity(),
        quantity.input().getUnit(),
        InputDemandEstimateStatus.ESTIMATED,
        now
    );
    return estimateRepository.save(estimate);
  }

  private List<InputDemandByInputResponse> inputBreakdowns(
      List<InputDemandEstimateEntity> estimates
  ) {
    return estimates.stream()
        .collect(Collectors.groupingBy(
            estimate -> estimate.getInput().getId(),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .values()
        .stream()
        .map(group -> {
          InputCatalogEntity input = group.getFirst().getInput();
          int planCount = (int) group.stream()
              .map(estimate -> estimate.getCropPlan().getId())
              .distinct()
              .count();
          BigDecimal quantity = group.stream()
              .map(InputDemandEstimateEntity::getEstimatedQuantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal totalDemandQuantity = group.stream()
              .map(InputDemandEstimateEntity::getTotalDemandQuantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal bufferQuantity = group.stream()
              .map(InputDemandEstimateEntity::getBufferQuantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal finalDemandQuantity = group.stream()
              .map(InputDemandEstimateEntity::getFinalDemandQuantity)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          return new InputDemandByInputResponse(
              input.getId(),
              input.getCode(),
              input.getName(),
              input.getUnit(),
              quantity,
              totalDemandQuantity,
              bufferQuantity,
              finalDemandQuantity,
              planCount
          );
        })
        .sorted(Comparator.comparing(InputDemandByInputResponse::inputName))
        .toList();
  }

  private List<InputDemandByCropResponse> cropBreakdowns(
      List<SeasonalCropPlanEntity> plans
  ) {
    return plans.stream()
        .collect(Collectors.groupingBy(
            plan -> plan.getCrop().getId(),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .values()
        .stream()
        .map(group -> new InputDemandByCropResponse(
            group.getFirst().getCrop().getId(),
            group.getFirst().getCrop().getName(),
            sumPlans(group),
            group.size()
        ))
        .sorted(Comparator.comparing(InputDemandByCropResponse::cropName))
        .toList();
  }

  private List<InputDemandByVillageResponse> villageBreakdowns(
      List<SeasonalCropPlanEntity> plans
  ) {
    return plans.stream()
        .collect(Collectors.groupingBy(
            plan -> plan.getMemberProfile().getVillage(),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .values()
        .stream()
        .map(group -> {
          Set<UUID> memberIds = group.stream()
              .map(plan -> plan.getMemberProfile().getId())
              .collect(Collectors.toSet());
          return new InputDemandByVillageResponse(
              group.getFirst().getMemberProfile().getVillage(),
              sumPlans(group),
              memberIds.size(),
              group.size()
          );
        })
        .sorted(Comparator.comparing(InputDemandByVillageResponse::village))
        .toList();
  }

  private BigDecimal sumPlans(List<SeasonalCropPlanEntity> plans) {
    return plans.stream()
        .map(SeasonalCropPlanEntity::getPlannedAreaAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void ensureInputCodeAvailable(UUID tenantId, String code, UUID currentId) {
    inputRepository.findByTenantIdAndCodeIgnoreCase(tenantId, code)
        .filter(input -> !input.getId().equals(currentId))
        .ifPresent(input -> {
          throw conflict("Input code already exists for this tenant.");
        });
  }

  private void ensureRuleAvailable(
      UUID tenantId,
      UUID cropId,
      UUID inputId,
      String applicationStage,
      UUID currentId
  ) {
    String stageKey = stageKey(applicationStage);
    ruleRepository.findByTenantIdAndCropIdAndInputId(tenantId, cropId, inputId).stream()
        .filter(rule -> !rule.getId().equals(currentId))
        .filter(rule -> stageKey(rule.getApplicationStage()).equals(stageKey))
        .findAny()
        .ifPresent(rule -> {
          throw conflict("Input rule already exists for this crop, input, and stage.");
        });
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only Phase 1 staff can manage input demand records.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private CropPlanStatus requireConfirmedPlanStatus(CropPlanStatus requestedStatus) {
    CropPlanStatus planStatus = requestedStatus == null
        ? CropPlanStatus.CONFIRMED
        : requestedStatus;
    if (planStatus != CropPlanStatus.CONFIRMED) {
      throw validation("Input demand can be calculated only from confirmed crop plans.");
    }
    return planStatus;
  }

  private void auditInput(CurrentUser currentUser, InputCatalogEntity input, AuditAction action) {
    auditEventService.record(
        input.getTenant(),
        actor(currentUser),
        "FPO_INPUT",
        input.getId(),
        action,
        Map.of(
            "code", input.getCode(),
            "unit", input.getUnit(),
            "status", input.getStatus().name()
        )
    );
  }

  private void auditRule(CurrentUser currentUser, CropInputRuleEntity rule, AuditAction action) {
    auditEventService.record(
        rule.getTenant(),
        actor(currentUser),
        "FPO_INPUT_RULE",
        rule.getId(),
        action,
        Map.of(
            "cropId", rule.getCrop().getId().toString(),
            "inputId", rule.getInput().getId().toString(),
            "status", rule.getStatus().name()
        )
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
  }

  private String normalizeOptional(String value) {
    return hasText(value) ? value.trim() : null;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String stageKey(String applicationStage) {
    return normalizeOptional(applicationStage) == null
        ? ""
        : normalizeOptional(applicationStage).toLowerCase();
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException conflict(String message) {
    return new ApplicationException(ErrorCode.DUPLICATE_RESOURCE, message, HttpStatus.CONFLICT);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }

  private record InputQuantity(
      InputCatalogEntity input,
      BigDecimal recommendedQuantityPerAcre,
      BigDecimal totalDemandQuantity,
      BigDecimal bufferPercent,
      BigDecimal bufferQuantity,
      BigDecimal finalDemandQuantity
  ) {
    private static InputQuantity fromRule(
        InputCatalogEntity input,
        BigDecimal recommendedQuantityPerAcre,
        BigDecimal totalDemandQuantity
    ) {
      BigDecimal bufferQuantity = totalDemandQuantity
          .multiply(PHASE1_BUFFER_PERCENT)
          .divide(ONE_HUNDRED, QUANTITY_SCALE, RoundingMode.HALF_UP);
      BigDecimal finalDemandQuantity = totalDemandQuantity
          .add(bufferQuantity)
          .setScale(0, RoundingMode.CEILING)
          .setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
      return new InputQuantity(
          input,
          recommendedQuantityPerAcre.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP),
          totalDemandQuantity,
          PHASE1_BUFFER_PERCENT,
          bufferQuantity,
          finalDemandQuantity
      );
    }

    InputQuantity add(InputQuantity next) {
      BigDecimal totalDemand = totalDemandQuantity
          .add(next.totalDemandQuantity())
          .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
      BigDecimal buffer = totalDemand
          .multiply(PHASE1_BUFFER_PERCENT)
          .divide(ONE_HUNDRED, QUANTITY_SCALE, RoundingMode.HALF_UP);
      BigDecimal finalDemand = totalDemand
          .add(buffer)
          .setScale(0, RoundingMode.CEILING)
          .setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);

      return new InputQuantity(
          input,
          recommendedQuantityPerAcre
              .add(next.recommendedQuantityPerAcre())
              .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP),
          totalDemand,
          PHASE1_BUFFER_PERCENT,
          buffer,
          finalDemand
      );
    }
  }
}
