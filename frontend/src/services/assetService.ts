import { request } from './api';
import type { Asset, AssetType, AssetStatus } from '../types';

export const assetService = {
  async getAll(type?: AssetType, status?: AssetStatus): Promise<Asset[]> {
    const params = new URLSearchParams();
    if (type) params.set('type', type);
    if (status) params.set('status', status);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request<Asset[]>(`/api/v1/assets${query}`);
  },

  async getById(id: string): Promise<Asset> {
    return request<Asset>(`/api/v1/assets/${id}`);
  },

  async create(data: Partial<Asset>): Promise<Asset> {
    return request<Asset>('/api/v1/assets', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};
