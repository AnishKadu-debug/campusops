import type { ApiError } from '../types';

const TOKEN_STORAGE_KEY = 'campusops_jwt_token';

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token.trim());
}

export function removeStoredToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

export async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const config: RequestInit = {
    ...options,
    headers,
  };

  try {
    const response = await fetch(endpoint, config);

    if (response.status === 204) {
      return {} as T;
    }

    const isJson = response.headers.get('content-type')?.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      const errorPayload: ApiError = typeof data === 'object' && data !== null
        ? (data as ApiError)
        : {
            status: response.status,
            error: response.statusText,
            message: typeof data === 'string' ? data : 'An unexpected error occurred',
          };
      throw errorPayload;
    }

    return data as T;
  } catch (err: unknown) {
    if ((err as ApiError).status !== undefined) {
      throw err;
    }
    // Network / offline error
    const networkError: ApiError = {
      status: 0,
      error: 'NetworkError',
      message: (err as Error).message || 'Failed to connect to backend microservices.',
    };
    throw networkError;
  }
}
