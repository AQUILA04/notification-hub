import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./pages/notifications/notifications-list.page').then(
            (m) => m.NotificationsListPage,
          ),
      },
      {
        path: 'notifications/:id',
        loadComponent: () =>
          import('./pages/notifications/notification-detail.page').then(
            (m) => m.NotificationDetailPage,
          ),
      },
      {
        path: 'templates',
        loadComponent: () =>
          import('./pages/templates/templates.page').then((m) => m.TemplatesPage),
      },
      {
        path: 'dlq',
        loadComponent: () => import('./pages/dlq/dlq.page').then((m) => m.DlqPage),
      },
      {
        path: 'sms-test',
        loadComponent: () =>
          import('./pages/sms-test/sms-test-recipients.page').then(
            (m) => m.SmsTestRecipientsPage,
          ),
      },
    ],
  },
];
