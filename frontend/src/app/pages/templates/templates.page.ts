import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { Channel, TemplateItem } from '../../core/models';

@Component({
  selector: 'app-templates-page',
  imports: [FormsModule, DatePipe],
  template: `
    <header class="page-head">
      <div>
        <h1>Templates</h1>
        <p>Pebble · versionnés par tenant</p>
      </div>
    </header>

    <section class="card form">
      <h2>Nouveau template</h2>
      <div class="row">
        <input [(ngModel)]="name" placeholder="nom (ex. order-ready-fr)" />
        <select [(ngModel)]="channel">
          <option value="EMAIL">EMAIL</option>
          <option value="SMS">SMS</option>
        </select>
      </div>
      <input [(ngModel)]="subjectTemplate" placeholder="Sujet Pebble (email)" />
      <textarea [(ngModel)]="bodyTemplate" rows="5" placeholder="Corps Pebble"></textarea>
      <div class="actions">
        <button type="button" (click)="create()">Enregistrer</button>
        <button type="button" class="ghost" (click)="preview()">Prévisualiser</button>
      </div>
      @if (previewResult()) {
        <pre>{{ previewResult() }}</pre>
      }
      @if (message()) {
        <p class="msg">{{ message() }}</p>
      }
    </section>

    <section class="card">
      <h2>Liste</h2>
      <table>
        <thead>
          <tr>
            <th>Nom</th>
            <th>v</th>
            <th>Canal</th>
            <th>Actif</th>
            <th>Créé</th>
          </tr>
        </thead>
        <tbody>
          @for (t of items(); track t.id) {
            <tr>
              <td>{{ t.name }}</td>
              <td>{{ t.version }}</td>
              <td>{{ t.channel }}</td>
              <td>{{ t.active ? 'oui' : 'non' }}</td>
              <td>{{ t.createdAt | date: 'short' }}</td>
            </tr>
          } @empty {
            <tr>
              <td colspan="5" class="empty">Aucun template</td>
            </tr>
          }
        </tbody>
      </table>
    </section>
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
    .card {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
    }
    h2 {
      margin: 0 0 12px;
      font-size: 14px;
    }
    .form {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .row {
      display: grid;
      grid-template-columns: 1fr 160px;
      gap: 10px;
    }
    input,
    select,
    textarea {
      background: var(--bg);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 10px;
      font: inherit;
    }
    .actions {
      display: flex;
      gap: 8px;
    }
    button {
      background: var(--accent);
      color: #081018;
      border: 0;
      border-radius: 8px;
      padding: 8px 14px;
      cursor: pointer;
      font-weight: 600;
    }
    button.ghost {
      background: transparent;
      border: 1px solid var(--border);
      color: var(--text);
      font-weight: 500;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }
    th,
    td {
      text-align: left;
      padding: 10px 8px;
      border-bottom: 1px solid var(--border);
    }
    th {
      color: var(--muted);
      font-size: 12px;
      font-weight: 500;
    }
    pre {
      background: var(--bg);
      border-radius: 8px;
      padding: 10px;
      white-space: pre-wrap;
      font-size: 12px;
    }
    .msg {
      color: #9be7b4;
    }
    .empty {
      color: var(--muted);
    }
  `,
})
export class TemplatesPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<TemplateItem[]>([]);
  readonly message = signal('');
  readonly previewResult = signal('');
  name = '';
  channel: Channel = 'EMAIL';
  subjectTemplate = '';
  bodyTemplate = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listTemplates().subscribe({
      next: (list) => this.items.set(list),
      error: () => this.message.set('Impossible de charger les templates'),
    });
  }

  create(): void {
    this.message.set('');
    this.api
      .createTemplate({
        name: this.name,
        channel: this.channel,
        subjectTemplate: this.subjectTemplate || undefined,
        bodyTemplate: this.bodyTemplate,
      })
      .subscribe({
        next: () => {
          this.message.set('Template enregistré');
          this.reload();
        },
        error: (err) => this.message.set(err?.error?.detail || err?.message || 'Erreur'),
      });
  }

  preview(): void {
    this.api.previewTemplate(this.name, { clientName: 'Awa', reference: 'CTP-1042' }).subscribe({
      next: (res) =>
        this.previewResult.set(`Sujet: ${res.subject}\n\n${res.body}`),
      error: (err) => this.message.set(err?.error?.detail || 'Preview impossible'),
    });
  }
}
