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
  notifications: {
    list: `${apiRoot}/notifications`,
    queue: `${apiRoot}/notifications`,
    status: (notificationId: string) => `${apiRoot}/notifications/${notificationId}/status`
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
