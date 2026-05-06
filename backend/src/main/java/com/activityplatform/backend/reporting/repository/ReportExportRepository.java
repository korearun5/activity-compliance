package com.activityplatform.backend.reporting.repository;

import com.activityplatform.backend.reporting.domain.ReportExportEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportExportRepository extends JpaRepository<ReportExportEntity, UUID> {
}
