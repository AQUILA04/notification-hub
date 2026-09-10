import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { NotificationEvent, NotificationItem, statusLabel } from '../../core/models';

@Component({
  selector: 'app-notification-detail-page',
  imports: [RouterLink, DatePipe, JsonPipe],
  template: `
    <a routerLink="/notifications" class="back">← Notifications</a>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    @if (item(); as n) {
      <header class="page-head">
        <div>
          <h1>{{ n.subject || n.templateName || shortId(n.id) }}</h1>
          <p>{{ n.channel }} · {{ label(n.status) }} · {{ n.id }}</p>
        </div>
      </header>

      <section class="grid">
        <div class="card">
          <h2>Détail</h2>
          <dl>
            <div><dt>From</dt><dd>{{ n.from }}</dd></div>
            <div><dt>To</dt><dd>{{ n.to.join(', ') }}</dd></div>
            <div><dt>Environnement</dt><dd>{{ n.environment || 'test' }}</dd></div>
            <div><dt>Priorité</dt><dd>{{ n.priority }}</dd></div>
            <div><dt>Tentatives</dt><dd>{{ n.attemptCount }} / {{ n.maxAttempts }}</dd></div>
            <div><dt>Créée</dt><dd>{{ n.createdAt | date: 'medium' }}</dd></div>
            <div><dt>Envoyée</dt><dd>{{ n.sentAt ? (n.sentAt | date: 'medium') : '—' }}</dd></div>
            @if (n.lastError) {
              <div><dt>Erreur</dt><dd class="err">{{ n.lastError }}</dd></div>
            }
          </dl>
        </div>

        <div class="card">
          <h2>Event Store</h2>
          <ol>
            @for (e of events(); track e.id) {
              <li>
                <div class="ev-head">
                  <strong>#{{ e.sequenceNo }} {{ e.eventType }}</strong>
                  <span>{{ e.occurredAt | date: 'short' }}</span>
                </div>
                @if (e.payload) {
                  <pre>{{ e.payload | json }}</pre>
                }
              </li>
            } @empty {
              <li class="empty">Aucun événement</li>
            }
          </ol>
        </div>
      </section>
    }
  `,
  styles: `
    .back {
      color: var(--muted);
      text-decoration: none;
      font-size: 13px;
    }
    .page-head {
      margin: 12px 0 20px;
    }
    h1 {
      margin: 0 0 4px;
      font-size: 26px;
      letter-spacing: -0.03em;
    }
    p {
      margin: 0;
      color: var(--muted);
      word-break: break-all;
    }
    .grid {
      display: grid;
      grid-template-columns: 1fr 1.2fr;
      gap: 12px;
    }
    .card {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
    }
    h2 {
      margin: 0 0 12px;
      font-size: 14px;
    }
    dl {
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    dl > div {
      display: grid;
      grid-template-columns: 110px 1fr;
      gap: 8px;
      font-size: 14px;
    }
    dt {
      color: var(--muted);
    }
    dd {
      margin: 0;
    }
    .err {
      color: #f2a0a0;
    }
    ol {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .ev-head {
      display: flex;
      justify-content: space-between;
      gap: 8px;
      font-size: 13px;
    }
    .ev-head span {
      color: var(--muted);
    }
    pre {
      margin: 6px 0 0;
      background: var(--bg);
      border-radius: 8px;
      padding: 8px;
      overflow: auto;
      font-size: 12px;
    }
    .empty,
    .error {
      color: var(--muted);
    }
    .error {
      color: #f2a0a0;
    }
    @media (max-width: 900px) {
      .grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class NotificationDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  readonly item = signal<NotificationItem | null>(null);
  readonly events = signal<NotificationEvent[]>([]);
  readonly error = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Identifiant manquant');
      return;
    }
    this.api.getNotification(id).subscribe({
      next: (n) => this.item.set(n),
      error: (err) => this.error.set(err?.message || 'Introuvable'),
    });
    this.api.getEvents(id).subscribe({
      next: (ev) => this.events.set(ev),
      error: () => undefined,
    });
  }

  label(status: string): string {
    return statusLabel(status);
  }

  shortId(id: string): string {
    return id.slice(0, 8);
  }
}
