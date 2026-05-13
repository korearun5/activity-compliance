import { appConfig } from "../config/appConfig";

const apiRoot = `/api/${appConfig.apiVersion}`;

export const endpoints = {
  activities: {
    byId: (activityId: string) => `${apiRoot}/activities/${activityId}`,
    list: `${apiRoot}/activities`,
    start: `${apiRoot}/activities`
  },
  auth: {
    login: `${apiRoot}/auth/login`,
    me: `${apiRoot}/auth/me`,
    refresh: `${apiRoot}/auth/refresh`
  },
  evidence: {
    byId: (evidenceId: string) => `${apiRoot}/evidence/${evidenceId}`,
    list: `${apiRoot}/evidence`,
    review: (evidenceId: string) => `${apiRoot}/evidence/${evidenceId}/review`,
    upload: `${apiRoot}/evidence`
  },
  fpo: {
    advisories: {
      byId: (advisoryId: string) => `${apiRoot}/fpo/advisories/${advisoryId}`,
      create: `${apiRoot}/fpo/advisories`,
      list: `${apiRoot}/fpo/advisories`,
      status: (advisoryId: string) => `${apiRoot}/fpo/advisories/${advisoryId}/status`
    },
    cropHistory: {
      createForMember: (memberId: string) =>
        `${apiRoot}/fpo/members/${memberId}/crop-history`,
      listByMember: (memberId: string) =>
        `${apiRoot}/fpo/members/${memberId}/crop-history`,
      update: (historyId: string) => `${apiRoot}/fpo/crop-history/${historyId}`
    },
    cropPlans: {
      byId: (planId: string) => `${apiRoot}/fpo/crop-plans/${planId}`,
      create: `${apiRoot}/fpo/crop-plans`,
      list: `${apiRoot}/fpo/crop-plans`,
      status: (planId: string) => `${apiRoot}/fpo/crop-plans/${planId}/status`
    },
    demandEstimates: {
      list: `${apiRoot}/fpo/demand-estimates`,
      run: `${apiRoot}/fpo/demand-estimates/run`,
      summary: `${apiRoot}/fpo/demand-estimates/summary`
    },
    inputRules: {
      byId: (ruleId: string) => `${apiRoot}/fpo/input-rules/${ruleId}`,
      create: `${apiRoot}/fpo/input-rules`,
      list: `${apiRoot}/fpo/input-rules`,
      status: (ruleId: string) => `${apiRoot}/fpo/input-rules/${ruleId}/status`
    },
    inputs: {
      byId: (inputId: string) => `${apiRoot}/fpo/inputs/${inputId}`,
      create: `${apiRoot}/fpo/inputs`,
      list: `${apiRoot}/fpo/inputs`,
      status: (inputId: string) => `${apiRoot}/fpo/inputs/${inputId}/status`
    },
    crops: {
      byId: (cropId: string) => `${apiRoot}/fpo/crops/${cropId}`,
      create: `${apiRoot}/fpo/crops`,
      list: `${apiRoot}/fpo/crops`,
      status: (cropId: string) => `${apiRoot}/fpo/crops/${cropId}/status`
    },
    landholdings: {
      byId: (landholdingId: string) => `${apiRoot}/fpo/landholdings/${landholdingId}`,
      createForMember: (memberId: string) =>
        `${apiRoot}/fpo/members/${memberId}/landholdings`,
      listByMember: (memberId: string) =>
        `${apiRoot}/fpo/members/${memberId}/landholdings`,
      status: (landholdingId: string) =>
        `${apiRoot}/fpo/landholdings/${landholdingId}/status`
    },
    members: {
      byId: (memberId: string) => `${apiRoot}/fpo/members/${memberId}`,
      create: `${apiRoot}/fpo/members`,
      list: `${apiRoot}/fpo/members`,
      me: `${apiRoot}/fpo/members/me`,
      status: (memberId: string) => `${apiRoot}/fpo/members/${memberId}/status`
    },
    plots: {
      byId: (plotId: string) => `${apiRoot}/fpo/plots/${plotId}`,
      createForMember: (memberId: string) => `${apiRoot}/fpo/members/${memberId}/plots`,
      listByMember: (memberId: string) => `${apiRoot}/fpo/members/${memberId}/plots`,
      status: (plotId: string) => `${apiRoot}/fpo/plots/${plotId}/status`
    },
    reports: {
      export: `${apiRoot}/fpo/reports/export`,
      summary: `${apiRoot}/fpo/reports/summary`
    },
    seasons: {
      byId: (seasonId: string) => `${apiRoot}/fpo/seasons/${seasonId}`,
      create: `${apiRoot}/fpo/seasons`,
      list: `${apiRoot}/fpo/seasons`,
      status: (seasonId: string) => `${apiRoot}/fpo/seasons/${seasonId}/status`
    }
  },
  notifications: {
    list: `${apiRoot}/notifications`,
    queue: `${apiRoot}/notifications`,
    status: (notificationId: string) =>
      `${apiRoot}/notifications/${notificationId}/status`
  },
  platform: {
    enabledModules: `${apiRoot}/platform/modules/enabled`,
    moduleSubscriptions: `${apiRoot}/platform/module-subscriptions`,
    modules: `${apiRoot}/platform/modules`
  },
  reports: {
    export: `${apiRoot}/reports/export`,
    summary: `${apiRoot}/reports/summary`
  },
  roles: {
    list: `${apiRoot}/roles`,
    userRoles: (userId: string) => `${apiRoot}/users/${userId}/roles`
  },
  users: {
    byId: (userId: string) => `${apiRoot}/users/${userId}`,
    create: `${apiRoot}/users`,
    list: `${apiRoot}/users`,
    me: `${apiRoot}/users/me`,
    status: (userId: string) => `${apiRoot}/users/${userId}/status`
  },
  workflows: {
    byId: (workflowId: string) => `${apiRoot}/workflows/${workflowId}`,
    create: `${apiRoot}/workflows`,
    status: (workflowId: string) => `${apiRoot}/workflows/${workflowId}/status`,
    list: `${apiRoot}/workflows`
  }
} as const;
