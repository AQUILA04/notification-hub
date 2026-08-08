import { Injectable, signal } from '@angular/core';

const KEY = 'nhub.tenantId';

@Injectable({ providedIn: 'root' })
export class TenantStore {
  readonly tenantId = signal(localStorage.getItem(KEY) || 'demo-tenant');

  setTenant(value: string): void {
    const next = value.trim() || 'demo-tenant';
    localStorage.setItem(KEY, next);
    this.tenantId.set(next);
  }
}
