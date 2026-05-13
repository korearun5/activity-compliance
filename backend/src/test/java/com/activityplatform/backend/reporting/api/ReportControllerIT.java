package com.activityplatform.backend.reporting.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.activityplatform.backend.TestDataFactory;
import com.activityplatform.backend.TestcontainersConfiguration;
import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
import com.activityplatform.backend.activity.repository.ActivityRepository;
import com.activityplatform.backend.audit.repository.AuditEventRepository;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.auth.service.JwtService;
import com.activityplatform.backend.evidence.domain.EvidenceEntity;
import com.activityplatform.backend.evidence.domain.EvidenceStatus;
import com.activityplatform.backend.evidence.repository.EvidenceRepository;
import com.activityplatform.backend.reporting.domain.ReportExportEntity;
import com.activityplatform.backend.reporting.domain.ReportFormat;
import com.activityplatform.backend.reporting.repository.ReportExportRepository;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.domain.TaskStatus;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.repository.WorkflowDefinitionRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReportControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ActivityRepository activityRepository;

  @Autowired
  private AuditEventRepository auditEventRepository;

  @Autowired
  private EvidenceRepository evidenceRepository;

  @Autowired
  private ReportExportRepository reportExportRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WorkflowDefinitionRepository workflowDefinitionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String FIELD_COORDINATORToken;
  private TenantEntity tenant;
  private UserEntity adminUser;
  private UserEntity FIELD_COORDINATORUser;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Admin User",
        adminRole
    ));
    FIELD_COORDINATORUser = userRepository.save(TestDataFactory.user(
        tenant,
        "FIELD_COORDINATOR-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "FIELD_COORDINATOR User",
        FIELD_COORDINATORRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    FIELD_COORDINATORToken = jwtService.issueTokens(FIELD_COORDINATORUser).accessToken();
  }

  @Test
  void testSummaryCountsTenantActivityAndEvidence() throws Exception {
    createCompletedActivityWithApprovedEvidence();
    long auditCount = auditEventRepository.count();

    mockMvc.perform(get("/api/v1/reports/summary")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.tenantId").value(tenant.getId().toString()))
        .andExpect(jsonPath("$.data.fieldCoordinatorCount").value(1))
        .andExpect(jsonPath("$.data.totalActivities").value(1))
        .andExpect(jsonPath("$.data.completedActivities").value(1))
        .andExpect(jsonPath("$.data.totalTasks").value(1))
        .andExpect(jsonPath("$.data.completedTasks").value(1))
        .andExpect(jsonPath("$.data.evidenceRecords").value(1))
        .andExpect(jsonPath("$.data.approvedEvidence").value(1))
        .andExpect(jsonPath("$.data.taskCompletionPercent").value(100))
        .andExpect(jsonPath("$.data.approvedEvidencePercent").value(100))
        .andExpect(jsonPath("$.data.byWorkflow[0].label").value("Test Workflow"))
        .andExpect(jsonPath("$.data.byLocation[0].label").value("Nashik"));

    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testPdfExportCreatesCompletedReportExport() throws Exception {
    createCompletedActivityWithApprovedEvidence();
    long auditCount = auditEventRepository.count();
    long exportCount = reportExportRepository.count();

    mockMvc.perform(post("/api/v1/reports/export")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "format": "PDF",
                  "reportType": "GOVERNMENT_EVIDENCE"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.reportType").value("GOVERNMENT_EVIDENCE"))
        .andExpect(jsonPath("$.data.format").value("PDF"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.storageKey").isString())
        .andExpect(jsonPath("$.data.completedAt").isString());

    assertThat(reportExportRepository.count()).isEqualTo(exportCount + 1);
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 2);
  }

  @Test
  void testExcelExportCreatesCompletedWorkbook() throws Exception {
    createCompletedActivityWithApprovedEvidence();
    long auditCount = auditEventRepository.count();
    long exportCount = reportExportRepository.count();

    mockMvc.perform(post("/api/v1/reports/export")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "format": "XLSX",
                  "reportType": "GOVERNMENT_EVIDENCE"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.reportType").value("GOVERNMENT_EVIDENCE"))
        .andExpect(jsonPath("$.data.format").value("XLSX"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.storageKey").isString())
        .andExpect(jsonPath("$.data.completedAt").isString());

    ReportExportEntity export = reportExportRepository.findAll().stream()
        .filter(item -> item.getFormat() == ReportFormat.XLSX)
        .max(Comparator.comparing(ReportExportEntity::getRequestedAt))
        .orElseThrow();
    Path workbookPath = Path.of("target/test-storage").resolve(export.getStorageKey());

    assertThat(reportExportRepository.count()).isEqualTo(exportCount + 1);
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 2);
    assertThat(export.getStorageKey()).endsWith(".xlsx");
    assertThat(workbookPath).exists();
    assertWorkbookContainsProofSequence(workbookPath);
  }

  @Test
  void testSummaryForbiddenForFIELD_COORDINATOR() throws Exception {
    mockMvc.perform(get("/api/v1/reports/summary")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void testExportForbiddenForFIELD_COORDINATOR() throws Exception {
    mockMvc.perform(post("/api/v1/reports/export")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken)
            .contentType("application/json")
            .content("""
                {
                  "format": "PDF"
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void testSummaryRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/reports/summary"))
        .andExpect(status().isUnauthorized());
  }

  private void createCompletedActivityWithApprovedEvidence() {
    Instant now = Instant.now();
    WorkflowDefinitionEntity workflow = workflowDefinitionRepository.save(
        TestDataFactory.workflow(
            tenant,
            "report-workflow-" + UUID.randomUUID(),
            WorkflowDefinitionStatus.ACTIVE
        )
    );
    ActivityEntity activity = new ActivityEntity(
        UUID.randomUUID(),
        tenant,
        workflow,
        FIELD_COORDINATORUser,
        "Plot 1",
        "Nashik",
        LocalDate.now(),
        LocalDate.now().plusDays(30),
        now
    );
    ActivityTaskEntity task = new ActivityTaskEntity(
        UUID.randomUUID(),
        workflow.getTasks().get(0),
        TaskStatus.DONE,
        LocalDate.now(),
        now
    );
    activity.addTask(task);
    activity.updateProgress(100, true, now);
    activityRepository.save(activity);

    EvidenceEntity evidence = new EvidenceEntity(
        UUID.randomUUID(),
        tenant,
        task,
        FIELD_COORDINATORUser,
        "tenant/evidence/file.jpg",
        "file.jpg",
        "image/jpeg",
        12,
        "Proof note",
        now
    );
    evidence.review(EvidenceStatus.APPROVED, adminUser, now);
    evidenceRepository.save(evidence);
  }

  private void assertWorkbookContainsProofSequence(Path workbookPath) throws Exception {
    try (ZipFile workbook = new ZipFile(workbookPath.toFile())) {
      assertThat(workbook.getEntry("[Content_Types].xml")).isNotNull();
      assertThat(workbook.getEntry("xl/workbook.xml")).isNotNull();
      assertThat(workbook.getEntry("xl/worksheets/sheet3.xml")).isNotNull();

      String taskEvidenceSheet = new String(
          workbook.getInputStream(workbook.getEntry("xl/worksheets/sheet3.xml")).readAllBytes(),
          StandardCharsets.UTF_8
      );
      assertThat(taskEvidenceSheet)
          .contains("Task sequence")
          .contains("APPROVED")
          .contains("Proof note")
          .contains("tenant/evidence/file.jpg");
    }
  }
}
