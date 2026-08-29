import { request } from './api';
import type {
  Incident,
  IncidentStatus,
  CreateIncidentRequest,
  AssignIncidentRequest,
  UpdateIncidentStatusRequest,
  UpdateIncidentRequest,
} from '../types';

export const incidentService = {
  async getAll(status?: IncidentStatus): Promise<Incident[]> {
    const url = status
      ? `/api/v1/incidents?status=${status}`
      : '/api/v1/incidents';
    return request<Incident[]>(url);
  },

  async getById(id: number | string): Promise<Incident> {
    return request<Incident>(`/api/v1/incidents/${id}`);
  },

  async create(data: CreateIncidentRequest): Promise<Incident> {
    return request<Incident>('/api/v1/incidents', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async assign(id: number | string, data: AssignIncidentRequest): Promise<Incident> {
    return request<Incident>(`/api/v1/incidents/${id}/assign`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },

  async updateStatus(id: number | string, data: UpdateIncidentStatusRequest): Promise<Incident> {
    return request<Incident>(`/api/v1/incidents/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },

  async update(id: number | string, data: UpdateIncidentRequest): Promise<Incident> {
    return request<Incident>(`/api/v1/incidents/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
};
