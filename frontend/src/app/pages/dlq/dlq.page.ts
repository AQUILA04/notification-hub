import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Channel, NotificationItem } from '../../core/models';

@Component({
  selector: 'app-dlq-page',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <header class="page-head">
      <div>
        <h1>Dead letter (DLQ)</h1>
        <p>{{ total() }} notification(s) en échec définitif</p>
      </div>
      <button type="button" (click)="load()">Rafraîchir</button>
    </header>

    <div class="filters">
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
    @if (flash()) {
      <p class="flash">{{ flash() }}</p>
    }

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Référence</th>
            <th>Canal</th>
            <th>Erreur</th>
            <th>Tentatives</th>
            <th>Mise à jour</th>
            <th>Actions</th>
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
              <td class="err">{{ n.lastError || '—' }}</td>
              <td>{{ n.attemptCount }} / {{ n.maxAttempts }}</td>
              <td>{{ n.updatedAt | date: 'short' }}</td>
              <td class="actions">
                <button type="button" [disabled]="busyId() === n.id" (click)="requeue(n)">
                  Rejouer
                </button>
                <button
                  type="button"
                  class="danger"
                  [disabled]="busyId() === n.id"
                  (click)="discard(n)"
                >
                  Écarter
                </button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6" class="empty">DLQ vide</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .page-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 18px;
      gap: 16px;
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
    select,
    button {
      background: var(--panel);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 10px;
      cursor: pointer;
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
    .err {
      color: #f2a0a0;
      font-size: 12px;
      max-width: 280px;
      word-break: break-word;
    }
    .actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .actions button.danger {
      color: #f2a0a0;
    }
    .actions button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .empty,
    .error {
      color: var(--muted);
    }
    .error {
      color: #f2a0a0;
    }
    .flash {
      color: #9be7b4;
      margin-bottom: 10px;
    }
  `,
})
export class DlqPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<NotificationItem[]>([]);
  readonly total = signal(0);
  readonly error = signal('');
  readonly flash = signal('');
  readonly busyId = signal('');
  channel: Channel | '' = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.error.set('');
    this.api.listDlq({ channel: this.channel, page: 0, size: 50 }).subscribe({
      next: (page) => {
        this.items.set(page.items);
        this.total.set(page.totalElements);
      },
      error: (err) => this.error.set(err?.error?.detail || err?.message || 'Chargement impossible'),
    });
  }

  requeue(n: NotificationItem): void {
    this.busyId.set(n.id);
    this.flash.set('');
    this.error.set('');
    this.api.requeueDlq(n.id).subscribe({
      next: () => {
        this.busyId.set('');
        this.flash.set(`Rejouée : ${this.shortId(n.id)}`);
        this.load();
      },
      error: (err) => {
        this.busyId.set('');
        this.error.set(err?.error?.detail || err?.message || 'Requeue impossible');
      },
    });
  }

  discard(n: NotificationItem): void {
    if (!confirm(`Écarter définitivement ${this.shortId(n.id)} ?`)) {
      return;
    }
    this.busyId.set(n.id);
    this.flash.set('');
    this.error.set('');
    this.api.discardDlq(n.id).subscribe({
      next: () => {
        this.busyId.set('');
        this.flash.set(`Écartée : ${this.shortId(n.id)}`);
        this.load();
      },
      error: (err) => {
        this.busyId.set('');
        this.error.set(err?.error?.detail || err?.message || 'Discard impossible');
      },
    });
  }

  shortId(id: string): string {
    return id.slice(0, 8);
  }
}
