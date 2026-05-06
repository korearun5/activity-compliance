import { appConfig } from "../config/appConfig";

const namespace = appConfig.storageNamespace;
const legacyNamespace = "role-login-app";

export const storageKeys = {
  activity: {
    records: `${namespace}:activity:records`
  },
  auth: {
    accessToken: `${namespace}:auth:access-token`,
    localAccounts: `${namespace}:auth:local-accounts`,
    refreshToken: `${namespace}:auth:refresh-token`,
    sessionRole: `${namespace}:auth:session-role`,
    sessionUsername: `${namespace}:auth:session-username`
  },
  evidence: {
    records: `${namespace}:evidence:records`
  },
  legacy: {
    activity: {
      records: `${legacyNamespace}:farmer-crop-cycles`
    },
    auth: {
      localAccounts: `${legacyNamespace}:local-accounts`,
      sessionRole: `${legacyNamespace}:signed-in-role`,
      sessionUsername: `${legacyNamespace}:signed-in-username`
    },
    evidence: {
      records: `${legacyNamespace}:farmer-proof-submissions`
    },
    profile: {
      byUsername: (username: string) =>
        `${legacyNamespace}:farmer-profile:${username.toLowerCase()}`
    },
    registry: {
      users: `${legacyNamespace}:admin-farmers`
    }
  },
  profile: {
    byUsername: (username: string) =>
      `${namespace}:profile:${username.toLowerCase()}`
  },
  registry: {
    users: `${namespace}:registry:users`
  }
} as const;
