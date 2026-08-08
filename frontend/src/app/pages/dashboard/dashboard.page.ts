import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, PercentPipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { KpiResponse, statusLabel } from '../../core/models';

@Component({
  selector: 'app-dashboard-page',
  imports: [DecimalPipe, PercentPipe],
  template: `
    <header class="page-head">
      <div>
        <h1>Dashboard</h1>
        <p>État des notifications sur {{ kpi()?.windowHours || 24 }} h</p>
      </div>
      <button type="button" (click)="load()">Rafraîchir</button>
    </header>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    <section class="stats">
      <article>
        <span>Volume</span>
        <strong>{{ kpi()?.total || 0 | number }}</strong>
      </article>
      <article>
        <span>Envoyées</span>
        <strong>{{ kpi()?.sent || 0 | number }}</strong>
      </article>
      <article>
        <span>Taux succès</span>
        <strong>{{ kpi()?.successRate || 0 | percent: '1.0-1' }}</strong>
      </article>
      <article>
        <span>En file / outbox</span>
        <strong>{{ kpi()?.queued || 0 }} / {{ kpi()?.outboxPending || 0 }}</strong>
      </article>
      <article>
        <span>Échecs / Dead</span>
        <strong>{{ kpi()?.failed || 0 }} / {{ kpi()?.dead || 0 }}</strong>
      </article>
      <article>
        <span>Tentatives moy.</span>
        <strong>{{ kpi()?.avgAttempts || 0 | number: '1.0-2' }}</strong>
      </article>
      <article>
        <span>Coût estimé</span>
        <strong>{{ kpi()?.estimatedCostTotal || 0 | number: '1.2-4' }}</strong>
      </article>
    </section>

    <section class="split">
      <div class="card">
        <h2>Par canal</h2>
        <ul>
          @for (row of kpi()?.byChannel || []; track row.channel) {
            <li>
              <span>{{ row.channel }}</span>
              <strong>{{ row.count }}</strong>
            </li>
          } @empty {
            <li class="empty">Aucune donnée</li>
          }
        </ul>
      </div>
      <div class="card">
        <h2>Par statut</h2>
        <ul>
          @for (row of kpi()?.byStatus || []; track row.status) {
            <li>
              <span>{{ label(row.status) }}</span>
              <strong>{{ row.count }}</strong>
            </li>
          } @empty {
            <li class="empty">Aucune donnée</li>
          }
        </ul>
      </div>
    </section>

    <section class="card cost">
      <h2>Coût estimé / canal</h2>
      <ul>
        @for (row of kpi()?.estimatedCostByChannel || []; track row.channel) {
          <li>
            <span>{{ row.channel }} · {{ row.count }} × {{ row.unitCost | number: '1.2-4' }}</span>
            <strong>{{ row.estimatedCost | number: '1.2-4' }}</strong>
          </li>
        } @empty {
          <li class="empty">Aucune donnée</li>
        }
      </ul>
    </section>
  `,
  styles: `
    .page-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
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
    button {
      background: var(--panel-2);
      border: 1px solid var(--border);
      color: var(--text);
      border-radius: 8px;
      padding: 8px 14px;
      cursor: pointer;
    }
    .stats {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 20px;
    }
    .stats article {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .stats span {
      color: var(--muted);
      font-size: 12px;
    }
    .stats strong {
      font-size: 24px;
      letter-spacing: -0.03em;
    }
    .split {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-bottom: 12px;
    }
    .card {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
    }
    .card.cost {
      margin-top: 0;
    }
    h2 {
      margin: 0 0 12px;
      font-size: 14px;
    }
    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    li {
      display: flex;
      justify-content: space-between;
      font-size: 14px;
    }
    .empty,
    .error {
      color: var(--muted);
    }
    .error {
      color: #f2a0a0;
      margin-bottom: 12px;
    }
    @media (max-width: 900px) {
      .stats,
      .split {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly kpi = signal<KpiResponse | null>(null);
  readonly error = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.error.set('');
    this.api.kpi(24).subscribe({
      next: (data) => this.kpi.set(data),
      error: (err) => this.error.set(err?.message || 'Impossible de charger les KPI'),
    });
  }

  label(status: string): string {
    return statusLabel(status);
  }
}
