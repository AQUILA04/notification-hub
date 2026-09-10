import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import {
  Channel,
  NotificationItem,
  NotificationStatus,
  statusLabel,
} from '../../core/models';

@Component({
  selector: 'app-notifications-list-page',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <header class="page-head">
      <div>
        <h1>Notifications</h1>
        <p>{{ total() }} résultat(s)</p>
      </div>
    </header>

    <div class="filters">
      <select [(ngModel)]="status" (ngModelChange)="load()">
        <option value="">Tous statuts</option>
        @for (s of statuses; track s) {
          <option [value]="s">{{ label(s) }}</option>
        }
      </select>
      <select [(ngModel)]="channel" (ngModelChange)="load()">
        <option value="">Tous canaux</option>
        <option value="EMAIL">EMAIL</option>
        <option value="SMS">SMS</option>
        <option value="WHATSAPP">WHATSAPP</option>
      </select>
    </div>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Référence</th>
            <th>Canal</th>
            <th>Env</th>
            <th>Statut</th>
            <th>Destinataires</th>
            <th>Créée</th>
          </tr>
        </thead>
        <tbody>
          @for (n of items(); track n.id) {
            <tr>
              <td>
                <a [routerLink]="['/notifications', n.id]">{{ shortId(n.id) }}</a>
                @if (n.subject) {
                  <div class="sub">{{ n.subject }}</div>
                }
              </td>
              <td>{{ n.channel }}</td>
              <td>{{ n.environment || '—' }}</td>
              <td><span class="pill" [attr.data-s]="n.status">{{ label(n.status) }}</span></td>
              <td>{{ n.to.join(', ') }}</td>
              <td>{{ n.createdAt | date: 'short' }}</td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6" class="empty">Aucune notification</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .page-head {
      margin-bottom: 18px;
    }
    h1 {
      margin: 0 0 4px;
      font-size: 28px;
      letter-spacing: -0.03em;
    }
    p {
      margin: 0;
      color: var(--muted);
    }
    .filters {
      display: flex;
      gap: 10px;
      margin-bottom: 14px;
    }
    select {
      background: var(--panel);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 10px;
    }
    .table-wrap {
      overflow: auto;
      border: 1px solid var(--border);
      border-radius: 12px;
      background: var(--panel);
    }
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    th,
    td {
      padding: 12px 14px;
      text-align: left;
      border-bottom: 1px solid var(--border);
      vertical-align: top;
    }
    th {
      color: var(--muted);
      font-weight: 500;
      font-size: 12px;
    }
    a {
      color: var(--accent);
      text-decoration: none;
      font-family: ui-monospace, monospace;
    }
    .sub {
      color: var(--muted);
      font-size: 12px;
      margin-top: 4px;
    }
    .pill {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 999px;
      background: var(--panel-2);
      font-size: 12px;
    }
    .pill[data-s='SENT'],
    .pill[data-s='DELIVERED'] {
      color: #9be7b4;
    }
    .pill[data-s='FAILED'],
    .pill[data-s='DEAD'] {
      color: #f2a0a0;
    }
    .empty,
    .error {
      color: var(--muted);
    }
    .error {
      color: #f2a0a0;
    }
  `,
})
export class NotificationsListPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<NotificationItem[]>([]);
  readonly total = signal(0);
  readonly error = signal('');
  status: NotificationStatus | '' = '';
  channel: Channel | '' = '';
  readonly statuses: NotificationStatus[] = [
    'QUEUED',
    'SENT',
    'FAILED',
    'DEAD',
    'DELIVERED',
    'CANCELLED',
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.error.set('');
    this.api
      .listNotifications({
        status: this.status,
        channel: this.channel,
        page: 0,
        size: 50,
      })
      .subscribe({
        next: (page) => {
          this.items.set(page.items);
          this.total.set(page.totalElements);
        },
        error: (err) => this.error.set(err?.message || 'Chargement impossible'),
      });
  }

  label(status: string): string {
    return statusLabel(status);
  }

  shortId(id: string): string {
    return id.slice(0, 8);
  }
}
