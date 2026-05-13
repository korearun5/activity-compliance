package com.activityplatform.backend.fpo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmerCropHistoryEntity;
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
    FarmerCropHistoryEntity cropHistory = cropHistory(member, crop, season);
    SeasonalCropPlanEntity plan = plan(member, plot, crop, season);
    InputCatalogEntity input = input(UUID.randomUUID(), "NPK", "NPK 19");
    InputDemandEstimateEntity estimate = estimate(plan, input, new BigDecimal("15.0000"));

    byte[] workbook = service.buildWorkbook(new FpoReportWorkbookService.FpoReportDataset(
        List.of(member),
        List.of(landholding),
        List.of(plot),
        List.of(cropHistory),
        List.of(plan),
        List.of(estimate)
    ));
    Path workbookPath = Files.write(Files.createTempFile("fpo-report-", ".xlsx"), workbook);

    try (ZipFile zipFile = new ZipFile(workbookPath.toFile())) {
      String workbookXml = text(zipFile, "xl/workbook.xml");
      String farmerMaster = text(zipFile, "xl/worksheets/sheet1.xml");
      String cropHistorySheet = text(zipFile, "xl/worksheets/sheet4.xml");
      String seasonalCropPlans = text(zipFile, "xl/worksheets/sheet5.xml");
      String inputDemandSummary = text(zipFile, "xl/worksheets/sheet6.xml");
      String farmerWiseDemand = text(zipFile, "xl/worksheets/sheet7.xml");

      assertThat(workbookXml)
          .contains("Farmer Master")
          .contains("Landholdings")
          .contains("Farm Plots")
          .contains("Crop History")
          .contains("Seasonal Crop Plans")
          .contains("Input Demand Summary")
          .contains("Farmer-wise Input Demand");
      assertThat(farmerMaster)
          .contains("MEM-001")
          .contains("Farmer One")
          .contains("Rampur");
      assertThat(cropHistorySheet)
          .contains("Crop year")
          .contains("2.0000")
          .contains("8.5000");
      assertThat(seasonalCropPlans)
          .contains("Planned area acres")
          .contains("CONFIRMED");
      assertThat(inputDemandSummary)
          .contains("Input code")
          .contains("NPK")
          .contains("15.0000");
      assertThat(farmerWiseDemand)
          .contains("Farmer name")
          .contains("Onion")
          .contains("Kharif")
          .contains("ESTIMATED");
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
    return new FpoMemberProfileEntity(
        memberId,
        tenant,
        user,
        "MEM-001",
        user.getDisplayName(),
        user.getPhone(),
        "+919111111111",
        null,
        "Rampur",
        "North Block",
        "District",
        "Maharashtra",
        "MALE",
        null,
        42,
        "SMALL",
        null,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }

  private FarmLandholdingEntity landholding(FpoMemberProfileEntity member) {
    return new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        member,
        "SUR-1",
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

  private FarmerCropHistoryEntity cropHistory(
      FpoMemberProfileEntity member,
      CropCatalogEntity crop,
      CropSeasonEntity season
  ) {
    return new FarmerCropHistoryEntity(
        UUID.randomUUID(),
        tenant,
        member,
        crop,
        season,
        2025,
        new BigDecimal("2.0000"),
        new BigDecimal("8.5000"),
        "MT",
        "Previous season baseline",
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
        new BigDecimal("1.5000"),
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 9, 30),
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
        input.getUnit(),
        InputDemandEstimateStatus.ESTIMATED,
        Instant.now()
    );
  }
}
