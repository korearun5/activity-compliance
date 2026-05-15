package com.activityplatform.backend.auth.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {
  private boolean enabled = false;
  private String tenantCode = "default";
  private String tenantName = "Default Client";
  private List<String> enabledModules = new ArrayList<>();
  private List<SeedUser> users = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getTenantCode() {
    return tenantCode;
  }

  public void setTenantCode(String tenantCode) {
    this.tenantCode = tenantCode;
  }

  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public List<String> getEnabledModules() {
    return enabledModules;
  }

  public void setEnabledModules(List<String> enabledModules) {
    this.enabledModules = enabledModules == null ? new ArrayList<>() : enabledModules;
  }

  public List<SeedUser> getUsers() {
    return users;
  }

  public void setUsers(List<SeedUser> users) {
    this.users = users == null ? new ArrayList<>() : users;
  }

  public static class SeedUser {
    private String username;
    private String password;
    private String displayName;
    private String phone;
    private String locationName;
    private String siteName;
    private List<String> roles = new ArrayList<>();

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getLocationName() {
      return locationName;
    }

    public void setLocationName(String locationName) {
      this.locationName = locationName;
    }

    public String getSiteName() {
      return siteName;
    }

    public void setSiteName(String siteName) {
      this.siteName = siteName;
    }

    public List<String> getRoles() {
      return roles;
    }

    public void setRoles(List<String> roles) {
      this.roles = roles == null ? new ArrayList<>() : roles;
    }
  }
}
