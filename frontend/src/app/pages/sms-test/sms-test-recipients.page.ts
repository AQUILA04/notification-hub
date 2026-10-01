import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { SmsTestRecipient } from '../../core/models';

@Component({
  selector: 'app-sms-test-recipients-page',
  imports: [FormsModule, DatePipe],
  template: `
    <header class="page-head">
      <div>
        <h1>SMS test</h1>
        <p>
          Les SMS envoyés avec environment ≠ prod sont copiés par email à cette liste de diffusion
        </p>
      </div>
      <button type="button" (click)="load()">Rafraîchir</button>
    </header>

    <section class="card form">
      <h2>Ajouter un destinataire</h2>
      <div class="row">
        <input
          type="email"
          [(ngModel)]="email"
          placeholder="destinataire@exemple.com"
          (keyup.enter)="add()"
        />
        <button type="button" [disabled]="busy()" (click)="add()">Ajouter</button>
      </div>
    </section>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    @if (flash()) {
      <p class="flash">{{ flash() }}</p>
    }
    @if (items().length === 0 && !error()) {
      <p class="warn">
        Liste vide — les SMS de test seront envoyés à l'adresse de repli configurée côté serveur
        (SMS_TEST_MAIL_TO).
      </p>
    }

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Email</th>
            <th>Ajouté par</th>
            <th>Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (r of items(); track r.id) {
            <tr>
              <td class="email">{{ r.email }}</td>
              <td>{{ r.createdBy || '—' }}</td>
              <td>{{ r.createdAt | date: 'short' }}</td>
              <td>
                <button
                  type="button"
                  class="danger"
                  [disabled]="busyId() === r.id"
                  (click)="remove(r)"
                >
                  Retirer
                </button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="empty">Aucun destinataire</td>
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
    .card {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 14px;
    }
    h2 {
      margin: 0 0 12px;
      font-size: 14px;
    }
    .form .row {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
    input {
      flex: 1;
      min-width: 220px;
      background: var(--bg);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 10px;
    }
    button {
      background: var(--panel);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 10px;
      cursor: pointer;
    }
    button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    button.danger {
      color: #f2a0a0;
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
    .email {
      font-family: ui-monospace, monospace;
    }
    .empty,
    .error,
    .warn {
      color: var(--muted);
    }
    .error {
      color: #f2a0a0;
      margin-bottom: 10px;
    }
    .flash {
      color: #9be7b4;
      margin-bottom: 10px;
    }
    .warn {
      color: #e7c89b;
      margin-bottom: 10px;
    }
  `,
})
export class SmsTestRecipientsPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<SmsTestRecipient[]>([]);
  readonly error = signal('');
  readonly flash = signal('');
  readonly busy = signal(false);
  readonly busyId = signal('');
  email = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.error.set('');
    this.api.listSmsTestRecipients().subscribe({
      next: (items) => this.items.set(items),
      error: (err) =>
        this.error.set(err?.error?.detail || err?.message || 'Chargement impossible'),
    });
  }

  add(): void {
    const value = this.email.trim();
    if (!value) {
      this.error.set('Email requis');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.flash.set('');
    this.api.addSmsTestRecipient(value).subscribe({
      next: (created) => {
        this.busy.set(false);
        this.email = '';
        this.flash.set(`Ajouté : ${created.email}`);
        this.load();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.detail || err?.message || 'Ajout impossible');
      },
    });
  }

  remove(r: SmsTestRecipient): void {
    if (!confirm(`Retirer ${r.email} de la liste de diffusion ?`)) {
      return;
    }
    this.busyId.set(r.id);
    this.error.set('');
    this.flash.set('');
    this.api.removeSmsTestRecipient(r.id).subscribe({
      next: () => {
        this.busyId.set('');
        this.flash.set(`Retiré : ${r.email}`);
        this.load();
      },
      error: (err) => {
        this.busyId.set('');
        this.error.set(err?.error?.detail || err?.message || 'Retrait impossible');
      },
    });
  }
}
