import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Channel,
  KpiResponse,
  NotificationEvent,
  NotificationItem,
  NotificationStatus,
  PageResponse,
  SmsTestRecipient,
  TemplateItem,
} from './models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  kpi(windowHours = 24): Observable<KpiResponse> {
    return this.http.get<KpiResponse>(`${this.base}/v1/admin/kpi`, {
      params: { windowHours },
    });
  }

  listNotifications(opts: {
    status?: NotificationStatus | '';
    channel?: Channel | '';
    page?: number;
    size?: number;
  }): Observable<PageResponse<NotificationItem>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.status) params = params.set('status', opts.status);
    if (opts.channel) params = params.set('channel', opts.channel);
    return this.http.get<PageResponse<NotificationItem>>(`${this.base}/v1/notifications`, {
      params,
    });
  }

  getNotification(id: string): Observable<NotificationItem> {
    return this.http.get<NotificationItem>(`${this.base}/v1/notifications/${id}`);
  }

  getEvents(id: string): Observable<NotificationEvent[]> {
    return this.http.get<NotificationEvent[]>(`${this.base}/v1/notifications/${id}/events`);
  }

  listTemplates(): Observable<TemplateItem[]> {
    return this.http.get<TemplateItem[]>(`${this.base}/v1/templates`);
  }

  createTemplate(body: {
    name: string;
    channel: Channel;
    subjectTemplate?: string;
    bodyTemplate: string;
  }): Observable<TemplateItem> {
    return this.http.post<TemplateItem>(`${this.base}/v1/templates`, body);
  }

  previewTemplate(name: string, data: Record<string, unknown>): Observable<{ subject: string; body: string }> {
    return this.http.post<{ subject: string; body: string }>(
      `${this.base}/v1/templates/${encodeURIComponent(name)}/preview`,
      { data },
    );
  }

  listDlq(opts: {
    channel?: Channel | '';
    page?: number;
    size?: number;
  }): Observable<PageResponse<NotificationItem>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.channel) params = params.set('channel', opts.channel);
    return this.http.get<PageResponse<NotificationItem>>(`${this.base}/v1/admin/dlq`, { params });
  }

  requeueDlq(id: string): Observable<NotificationItem> {
    return this.http.post<NotificationItem>(`${this.base}/v1/admin/dlq/${id}/requeue`, {});
  }

  discardDlq(id: string): Observable<NotificationItem> {
    return this.http.post<NotificationItem>(`${this.base}/v1/admin/dlq/${id}/discard`, {});
  }

  listSmsTestRecipients(): Observable<SmsTestRecipient[]> {
    return this.http.get<SmsTestRecipient[]>(`${this.base}/v1/admin/sms-test-recipients`);
  }

  addSmsTestRecipient(email: string): Observable<SmsTestRecipient> {
    return this.http.post<SmsTestRecipient>(`${this.base}/v1/admin/sms-test-recipients`, {
      email,
    });
  }

  removeSmsTestRecipient(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/v1/admin/sms-test-recipients/${id}`);
  }
}
