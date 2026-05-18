package com.activityplatform.backend.farmer;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FarmerProfileBackfillMigrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @Test
  void testV15BackfillsFarmerProfilesAndAuditsFlaggedRows() throws Exception {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .target("14")
        .load()
        .migrate();

    UUID tenantId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    UUID farmerRoleId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000010");
    UUID fpoUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000101");
    UUID carbonUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000102");
    UUID fpoMemberId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000201");
    UUID carbonByUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000301");
    UUID carbonByFpoMemberId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000302");
    UUID carbonByMobileId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000303");
    UUID carbonFlaggedId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000304");

    try (Connection connection = connection()) {
      insertTenant(connection, tenantId);
      insertRole(connection, tenantId, farmerRoleId, "FARMER", "Farmer");
      insertUser(
          connection,
          tenantId,
          fpoUserId,
          farmerRoleId,
          "fpo-farmer",
          "FPO Farmer",
          "9999900000"
      );
      insertUser(
          connection,
          tenantId,
          carbonUserId,
          farmerRoleId,
          "carbon-farmer",
          "Carbon Farmer",
          "9876543210"
      );
      insertFpoMember(connection, tenantId, fpoMemberId, fpoUserId);
      insertCarbonProfile(
          connection,
          tenantId,
          carbonByUserId,
          carbonUserId,
          null,
          "CAR-BY-USER",
          "Carbon Farmer",
          "9876543210"
      );
      insertCarbonProfile(
          connection,
          tenantId,
          carbonByFpoMemberId,
          null,
          fpoMemberId,
          "CAR-BY-FPO",
          "FPO Farmer",
          "9999900000"
      );
      insertCarbonProfile(
          connection,
          tenantId,
          carbonByMobileId,
          null,
          null,
          "CAR-BY-MOBILE",
          "FPO Farmer Mobile",
          "9999900000"
      );
      insertCarbonProfile(
          connection,
          tenantId,
          carbonFlaggedId,
          null,
          null,
          "CAR-FLAGGED",
          "Unmatched Farmer",
          "7777700000"
      );
    }

    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = connection()) {
      UUID fpoFarmerProfileId = requiredUuid(
          connection,
          "select farmer_profile_id from fpo_member_profiles where id = ?",
          fpoMemberId
      );
      UUID carbonFarmerProfileId = requiredUuid(
          connection,
          "select farmer_profile_id from carbon_profiles where id = ?",
          carbonByUserId
      );
      UUID carbonByFpoMemberProfileId = requiredUuid(
          connection,
          "select farmer_profile_id from carbon_profiles where id = ?",
          carbonByFpoMemberId
      );
      UUID carbonByMobileProfileId = requiredUuid(
          connection,
          "select farmer_profile_id from carbon_profiles where id = ?",
          carbonByMobileId
      );

      assertThat(fpoFarmerProfileId).isNotNull();
      assertThat(carbonFarmerProfileId).isNotNull();
      assertThat(carbonByFpoMemberProfileId).isEqualTo(fpoFarmerProfileId);
      assertThat(carbonByMobileProfileId).isEqualTo(fpoFarmerProfileId);
      assertThat(countRows(connection, "farmer_profiles")).isEqualTo(2);
      assertThat(countAuditRows(connection, "FPO_AUTO_LINKED_BY_USER")).isEqualTo(1);
      assertThat(countAuditRows(connection, "CARBON_AUTO_LINKED_BY_USER")).isEqualTo(1);
      assertThat(countAuditRows(connection, "CARBON_AUTO_LINKED_BY_FPO_MEMBER")).isEqualTo(1);
      assertThat(countAuditRows(connection, "CARBON_AUTO_LINKED_BY_UNIQUE_MOBILE")).isEqualTo(1);
      assertThat(countAuditRows(connection, "CARBON_FLAGGED_NO_SAFE_USER_OR_MOBILE_MATCH"))
          .isEqualTo(1);
    }
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword()
    );
  }

  private void insertTenant(Connection connection, UUID tenantId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        insert into tenants (id, code, name, status)
        values (?, 'migration-test', 'Migration Test Tenant', 'ACTIVE')
        """)) {
      statement.setObject(1, tenantId);
      statement.executeUpdate();
    }
  }

  private void insertUser(
      Connection connection,
      UUID tenantId,
      UUID userId,
      UUID roleId,
      String username,
      String displayName,
      String phone
  ) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        insert into users (
          id, tenant_id, username, password_hash, display_name, phone, status
        )
        values (?, ?, ?, 'hash', ?, ?, 'ACTIVE')
        """)) {
      statement.setObject(1, userId);
      statement.setObject(2, tenantId);
      statement.setString(3, username);
      statement.setString(4, displayName);
      statement.setString(5, phone);
      statement.executeUpdate();
    }
    try (PreparedStatement statement = connection.prepareStatement("""
        insert into user_roles (user_id, role_id)
        values (?, ?)
        """)) {
      statement.setObject(1, userId);
      statement.setObject(2, roleId);
      statement.executeUpdate();
    }
  }

  private void insertRole(
      Connection connection,
      UUID tenantId,
      UUID roleId,
      String code,
      String name
  ) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        insert into roles (id, tenant_id, code, name)
        values (?, ?, ?, ?)
        """)) {
      statement.setObject(1, roleId);
      statement.setObject(2, tenantId);
      statement.setString(3, code);
      statement.setString(4, name);
      statement.executeUpdate();
    }
  }

  private void insertFpoMember(
      Connection connection,
      UUID tenantId,
      UUID memberId,
      UUID userId
  ) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        insert into fpo_member_profiles (
          id, tenant_id, user_id, member_number, display_name, mobile_number,
          village, taluka, district_name, state_name, gender, farmer_category, status
        )
        values (
          ?, ?, ?, 'MEM-001', 'FPO Farmer', '9999900000',
          'Wagholi', 'Haveli', 'Pune', 'Maharashtra', 'MALE', 'SMALL', 'ACTIVE'
        )
        """)) {
      statement.setObject(1, memberId);
      statement.setObject(2, tenantId);
      statement.setObject(3, userId);
      statement.executeUpdate();
    }
  }

  private void insertCarbonProfile(
      Connection connection,
      UUID tenantId,
      UUID profileId,
      UUID userId,
      UUID fpoMemberId,
      String carbonIdentityId,
      String displayName,
      String mobileNumber
  ) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        insert into carbon_profiles (
          id, tenant_id, user_id, fpo_member_profile_id, carbon_identity_id,
          participant_type, display_name, mobile_number, village, taluka,
          district_name, state_name, gender, farmer_category, status
        )
        values (
          ?, ?, ?, ?, ?, 'FARMER', ?, ?, 'Wagholi', 'Haveli',
          'Pune', 'Maharashtra', 'MALE', 'SMALL', 'ACTIVE'
        )
        """)) {
      statement.setObject(1, profileId);
      statement.setObject(2, tenantId);
      statement.setObject(3, userId);
      statement.setObject(4, fpoMemberId);
      statement.setString(5, carbonIdentityId);
      statement.setString(6, displayName);
      statement.setString(7, mobileNumber);
      statement.executeUpdate();
    }
  }

  private UUID requiredUuid(Connection connection, String sql, UUID id) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getObject(1, UUID.class);
      }
    }
  }

  private int countRows(Connection connection, String tableName) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "select count(*) from " + tableName
    );
        ResultSet resultSet = statement.executeQuery()) {
      assertThat(resultSet.next()).isTrue();
      return resultSet.getInt(1);
    }
  }

  private int countAuditRows(Connection connection, String action) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        select count(*)
        from farmer_profile_migration_audit
        where migration_version = 'V15'
          and action = ?
        """)) {
      statement.setString(1, action);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }
}
