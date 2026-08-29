import { request } from './api';
import type { NotificationItem } from '../types';

export const notificationService = {
  async getNotifications(incidentId?: number | string): Promise<NotificationItem[]> {
    const query = incidentId ? `?incidentId=${incidentId}` : '';
    return request<NotificationItem[]>(`/api/v1/notifications${query}`);
  },
};
