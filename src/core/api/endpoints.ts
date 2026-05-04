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
    uploadForTask: (activityId: string, taskId: string) =>
      `${apiRoot}/activities/${activityId}/tasks/${taskId}/evidence`
  },
  reports: {
    export: `${apiRoot}/reports/export`,
    summary: `${apiRoot}/reports/summary`
  },
  users: {
    byId: (userId: string) => `${apiRoot}/users/${userId}`,
    list: `${apiRoot}/users`
  },
  workflows: {
    byId: (workflowId: string) => `${apiRoot}/workflows/${workflowId}`,
    list: `${apiRoot}/workflows`
  }
} as const;

