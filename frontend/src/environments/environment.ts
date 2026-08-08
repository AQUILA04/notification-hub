export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8088',
  auth: {
    /** false = mode local (X-Tenant-Id only). true = Keycloak login-required */
    enabled: false,
    url: 'http://localhost:8081',
    realm: 'notification-hub',
    clientId: 'notification-hub-console',
  },
};
