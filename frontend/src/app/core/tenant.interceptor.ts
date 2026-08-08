import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';
import { TenantStore } from './tenant.store';

export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  const tenant = inject(TenantStore);
  const auth = inject(AuthService);

  const withTenant = (token: string | null) => {
    const headers: Record<string, string> = {
      'X-Tenant-Id': tenant.tenantId(),
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return next(req.clone({ setHeaders: headers }));
  };

  if (!auth.enabled) {
    return withTenant(null);
  }
  return from(auth.ensureToken()).pipe(switchMap((token) => withTenant(token)));
};
