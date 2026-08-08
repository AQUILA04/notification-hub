import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private keycloak: Keycloak | null = null;
  readonly authenticated = signal(false);
  readonly username = signal<string | null>(null);
  readonly token = signal<string | null>(null);

  get enabled(): boolean {
    return environment.auth.enabled;
  }

  async init(): Promise<boolean> {
    if (!this.enabled) {
      this.authenticated.set(true);
      return true;
    }
    this.keycloak = new Keycloak({
      url: environment.auth.url,
      realm: environment.auth.realm,
      clientId: environment.auth.clientId,
    });
    const ok = await this.keycloak.init({
      onLoad: 'login-required',
      checkLoginIframe: false,
      pkceMethod: 'S256',
    });
    this.authenticated.set(!!ok);
    this.username.set(this.keycloak.tokenParsed?.['preferred_username'] as string ?? null);
    this.token.set(this.keycloak.token ?? null);
    if (this.keycloak.tokenParsed?.['tenant_id']) {
      // tenant claim available for display; interceptor still sends X-Tenant-Id from store as fallback
    }
    return !!ok;
  }

  async ensureToken(): Promise<string | null> {
    if (!this.enabled || !this.keycloak) {
      return null;
    }
    try {
      await this.keycloak.updateToken(30);
      this.token.set(this.keycloak.token ?? null);
      return this.keycloak.token ?? null;
    } catch {
      await this.keycloak.login();
      return null;
    }
  }

  logout(): void {
    this.keycloak?.logout({ redirectUri: window.location.origin });
  }

  tenantFromToken(): string | null {
    const claim = this.keycloak?.tokenParsed?.['tenant_id'];
    return typeof claim === 'string' ? claim : null;
  }
}
