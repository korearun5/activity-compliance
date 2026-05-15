package com.activityplatform.backend.fpo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateStatus;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class FpoReportWorkbookServiceTest {
  private final TenantEntity tenant = new TenantEntity(
      UUID.randomUUID(),
      "basecraft-fpo",
      "BaseCraft FPO",
      "ACTIVE",
      Instant.now()
  );
  private final FpoReportWorkbookService service = new FpoReportWorkbookService(
      null,
      null,
      null,
      null
  );

  @Test
  void buildWorkbookContainsRequiredFpoSheetsAndDemandRows() throws Exception {
    UserEntity user = user(UUID.randomUUID(), "Farmer One");
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user);
    FarmLandholdingEntity landholding = landholding(member);
    FarmPlotEntity plot = plot(member, landholding);
    CropCatalogEntity crop = crop(UUID.randomUUID(), "ONI", "Onion");
    CropSeasonEntity season = season(UUID.randomUUID(), "KHA", "Kharif");
    SeasonalCropPlanEntity plan = plan(member, plot, crop, season);
    InputCatalogEntity input = input(UUID.randomUUID(), "NPK", "NPK 19");
    InputDemandEstimateEntity estimate = estimate(plan, input, new BigDecimal("15.0000"));

    byte[] workbook = service.buildWorkbook(new FpoReportWorkbookService.FpoReportDataset(
        List.of(member),
        List.of(landholding),
        List.of(plan),
        List.of(estimate)
    ));
    Path workbookPath = Files.write(Files.createTempFile("fpo-report-", ".xlsx"), workbook);

    try (ZipFile zipFile = new ZipFile(workbookPath.toFile())) {
      String workbookXml = text(zipFile, "xl/workbook.xml");
      String farmerRegister = text(zipFile, "xl/worksheets/sheet1.xml");
      String cropPlanSummary = text(zipFile, "xl/worksheets/sheet2.xml");
      String inputDemand = text(zipFile, "xl/worksheets/sheet3.xml");

      assertThat(workbookXml)
          .contains("Farmer Register")
          .contains("Crop Plan Summary")
          .contains("Input Demand")
          .doesNotContain("Landholdings")
          .doesNotContain("Farm Plots")
          .doesNotContain("Farmer-wise Input Demand");
      assertThat(farmerRegister)
          .contains("Name")
          .contains("Survey No")
          .contains("Carbon Farming Platform - FPO Digitization")
          .contains("Confidential - For internal FPO use")
          .contains("Farmer One")
          .contains("Rampur")
          .contains("SUR-1")
          .contains("3.0000");
      assertThat(cropPlanSummary)
          .contains("No. of Farmers")
          .contains("Total Area (acres)")
          .contains("Expected Yield (quintals)")
          .contains("Kharif")
          .contains("2026-27")
          .contains("Onion")
          .contains("1.5000")
          .contains("24.0000");
      assertThat(inputDemand)
          .contains("Input Type (Seed/Fertilizer)")
          .contains("Recommended Qty/acre")
          .contains("Buffer 5%")
          .contains("Fertilizer")
          .contains("15.0000");
    }
  }

  @Test
  void buildWorkbookAppliesPhase1FiltersToAllSheets() throws Exception {
    UserEntity coordinator = user(UUID.randomUUID(), "Coordinator One");
    FpoMemberProfileEntity matchingMember = member(
        UUID.randomUUID(),
        user(UUID.randomUUID(), "Farmer One"),
        "Rampur",
        coordinator
    );
    FpoMemberProfileEntity otherMember = member(
        UUID.randomUUID(),
        user(UUID.randomUUID(), "Farmer Two"),
        "Other Village",
        null
    );
    FarmLandholdingEntity matchingLand = landholding(matchingMember, "SUR-1");
    FarmLandholdingEntity otherLand = landholding(otherMember, "SUR-2");
    CropSeasonEntity kharif = season(UUID.randomUUID(), "KHA", "Kharif");
    CropSeasonEntity rabi = season(UUID.randomUUID(), "RAB", "Rabi");
    CropCatalogEntity onion = crop(UUID.randomUUID(), "ONI", "Onion");
    CropCatalogEntity wheat = crop(UUID.randomUUID(), "WHE", "Wheat");
    SeasonalCropPlanEntity matchingPlan = plan(
        matchingMember,
        plot(matchingMember, matchingLand),
        onion,
        kharif
    );
    SeasonalCropPlanEntity otherPlan = plan(
        otherMember,
        plot(otherMember, otherLand),
        wheat,
        rabi
    );
    InputCatalogEntity input = input(UUID.randomUUID(), "NPK", "NPK 19");

    byte[] workbook = service.buildWorkbook(
        new FpoReportWorkbookService.FpoReportDataset(
            List.of(matchingMember, otherMember),
            List.of(matchingLand, otherLand),
            List.of(matchingPlan, otherPlan),
            List.of(
                estimate(matchingPlan, input, new BigDecimal("15.0000")),
                estimate(otherPlan, input, new BigDecimal("7.0000"))
            )
        ),
        FpoReportWorkbookService.FpoReportFilters.from(Map.of(
            "village", "Rampur",
            "crop", "Onion",
            "season", "Kharif 2026",
            "coordinator", "Coordinator One",
            "dateFrom", LocalDate.now().minusDays(1).toString(),
            "dateTo", LocalDate.now().plusDays(1).toString()
        ))
    );
    Path workbookPath = Files.write(Files.createTempFile("fpo-report-filtered-", ".xlsx"), workbook);

    try (ZipFile zipFile = new ZipFile(workbookPath.toFile())) {
      String farmerRegister = text(zipFile, "xl/worksheets/sheet1.xml");
      String cropPlanSummary = text(zipFile, "xl/worksheets/sheet2.xml");
      String inputDemand = text(zipFile, "xl/worksheets/sheet3.xml");

      assertThat(farmerRegister)
          .contains("Farmer One")
          .contains("SUR-1")
          .doesNotContain("Farmer Two")
          .doesNotContain("SUR-2");
      assertThat(cropPlanSummary)
          .contains("Onion")
          .contains("Kharif")
          .doesNotContain("Wheat")
          .doesNotContain("Rabi");
      assertThat(inputDemand)
          .contains("15.0000")
          .doesNotContain("7.0000");
    }
  }

  private String text(ZipFile zipFile, String entryName) throws Exception {
    return new String(
        zipFile.getInputStream(zipFile.getEntry(entryName)).readAllBytes(),
        StandardCharsets.UTF_8
    );
  }

  private UserEntity user(UUID userId, String displayName) {
    return new UserEntity(
        userId,
        tenant,
        "farmer-one",
        "hash",
        displayName,
        "+919000000000",
        "Rampur",
        "North Block",
        "ACTIVE",
        Instant.now()
    );
  }

  private FpoMemberProfileEntity member(UUID memberId, UserEntity user) {
    return member(memberId, user, "Rampur", null);
  }

  private FpoMemberProfileEntity member(
      UUID memberId,
      UserEntity user,
      String village,
      UserEntity coordinator
  ) {
    return new FpoMemberProfileEntity(
        memberId,
        tenant,
        user,
        "MEM-001",
        user.getDisplayName(),
        user.getPhone(),
        "+919111111111",
        null,
        village,
        "North Block",
        "District",
        "Maharashtra",
        "MALE",
        null,
        42,
        "SMALL",
        coordinator,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }

  private FarmLandholdingEntity landholding(FpoMemberProfileEntity member) {
    return landholding(member, "SUR-1");
  }

  private FarmLandholdingEntity landholding(FpoMemberProfileEntity member, String surveyNumber) {
    return new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        member,
        surveyNumber,
        new BigDecimal("3.0000"),
        new BigDecimal("2.5000"),
        "Self-owned",
        "Canal",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private FarmPlotEntity plot(
      FpoMemberProfileEntity member,
      FarmLandholdingEntity landholding
  ) {
    return new FarmPlotEntity(
        UUID.randomUUID(),
        tenant,
        member,
        landholding,
        "North plot",
        new BigDecimal("1.5000"),
        new BigDecimal("19.8765432"),
        new BigDecimal("73.1234567"),
        "LOAM",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropCatalogEntity crop(UUID cropId, String code, String name) {
    return new CropCatalogEntity(
        cropId,
        tenant,
        code,
        name,
        "Vegetable",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropSeasonEntity season(UUID seasonId, String code, String name) {
    return new CropSeasonEntity(
        seasonId,
        tenant,
        code,
        name,
        6,
        9,
        2026,
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private SeasonalCropPlanEntity plan(
      FpoMemberProfileEntity member,
      FarmPlotEntity plot,
      CropCatalogEntity crop,
      CropSeasonEntity season
  ) {
    return new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        tenant,
        member,
        plot,
        crop,
        season,
        "2026-27",
        new BigDecimal("1.5000"),
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 9, 30),
        new BigDecimal("24.0000"),
        CropPlanStatus.CONFIRMED,
        Instant.now()
    );
  }

  private InputCatalogEntity input(UUID inputId, String code, String name) {
    return new InputCatalogEntity(
        inputId,
        tenant,
        code,
        name,
        "Fertilizer",
        "KG",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private InputDemandEstimateEntity estimate(
      SeasonalCropPlanEntity plan,
      InputCatalogEntity input,
      BigDecimal quantity
  ) {
    return new InputDemandEstimateEntity(
        UUID.randomUUID(),
        tenant,
        plan,
        input,
        quantity,
        quantity,
        quantity,
        new BigDecimal("5.00"),
        BigDecimal.ZERO,
        quantity,
        input.getUnit(),
        InputDemandEstimateStatus.ESTIMATED,
        Instant.now()
    );
  }
}
