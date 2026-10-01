import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { TenantStore } from '../core/tenant.store';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  template: `
    <div class="shell">
      <aside class="nav">
        <div class="brand">
          <span class="mark">NH</span>
          <div>
            <strong>Notification Hub</strong>
            <small>Console ops</small>
          </div>
        </div>
        <nav>
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/notifications" routerLinkActive="active">Notifications</a>
          <a routerLink="/dlq" routerLinkActive="active">DLQ</a>
          <a routerLink="/templates" routerLinkActive="active">Templates</a>
          <a routerLink="/sms-test" routerLinkActive="active">SMS test</a>
        </nav>
        <label class="tenant">
          Tenant
          <input
            [ngModel]="tenant.tenantId()"
            (ngModelChange)="onTenant($event)"
            [disabled]="auth.enabled && !!auth.tenantFromToken()"
          />
        </label>
        @if (auth.enabled) {
          <div class="auth">
            <span>{{ auth.username() || '…' }}</span>
            <button type="button" (click)="auth.logout()">Déconnexion</button>
          </div>
        }
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .shell {
      display: grid;
      grid-template-columns: 240px 1fr;
      min-height: 100vh;
      background: var(--bg);
      color: var(--text);
    }
    .nav {
      border-right: 1px solid var(--border);
      padding: 24px 18px;
      display: flex;
      flex-direction: column;
      gap: 28px;
      background: var(--panel);
    }
    .brand {
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .brand strong {
      display: block;
      font-size: 14px;
    }
    .brand small {
      color: var(--muted);
      font-size: 12px;
    }
    .mark {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: grid;
      place-items: center;
      background: var(--accent);
      color: #081018;
      font-weight: 700;
      font-size: 12px;
    }
    nav {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    nav a {
      color: var(--muted);
      text-decoration: none;
      padding: 10px 12px;
      border-radius: 8px;
      font-size: 14px;
    }
    nav a.active,
    nav a:hover {
      background: var(--panel-2);
      color: var(--text);
    }
    .tenant {
      margin-top: auto;
      display: flex;
      flex-direction: column;
      gap: 6px;
      font-size: 12px;
      color: var(--muted);
    }
    .tenant input {
      background: var(--bg);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 10px;
    }
    .tenant input:disabled {
      opacity: 0.7;
    }
    .auth {
      display: flex;
      flex-direction: column;
      gap: 8px;
      font-size: 12px;
      color: var(--muted);
    }
    .auth button {
      background: var(--panel-2);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px;
      cursor: pointer;
    }
    .content {
      padding: 28px 32px;
    }
    @media (max-width: 860px) {
      .shell {
        grid-template-columns: 1fr;
      }
      .nav {
        border-right: 0;
        border-bottom: 1px solid var(--border);
      }
    }
  `,
})
export class ShellComponent implements OnInit {
  readonly tenant = inject(TenantStore);
  readonly auth = inject(AuthService);

  ngOnInit(): void {
    const fromToken = this.auth.tenantFromToken();
    if (fromToken) {
      this.tenant.setTenant(fromToken);
    }
  }

  onTenant(value: string): void {
    this.tenant.setTenant(value);
  }
}
