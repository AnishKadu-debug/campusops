import type { DecodedToken, UserRole } from '../types';

export const DEV_JWT_SECRET = 'campusops-dev-jwt-secret-change-me-0123456789';

/**
 * Base64URL decode helper
 */
function base64UrlDecode(str: string): string {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4) {
    base64 += '=';
  }
  return decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join('')
  );
}

/**
 * Base64URL encode helper
 */
function base64UrlEncode(buffer: ArrayBuffer | Uint8Array | string): string {
  let binary = '';
  if (typeof buffer === 'string') {
    binary = btoa(encodeURIComponent(buffer).replace(/%([0-9A-F]{2})/g, (_, p1) =>
      String.fromCharCode(parseInt(p1, 16))
    ));
  } else {
    const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    binary = btoa(binary);
  }
  return binary.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * Decodes a JWT token safely without external dependencies
 */
export function decodeJwt(token: string): DecodedToken | null {
  if (!token || typeof token !== 'string') return null;

  try {
    const parts = token.trim().split('.');
    if (parts.length !== 3) return null;

    const payloadJson = base64UrlDecode(parts[1]);
    const payload = JSON.parse(payloadJson);

    const nowSec = Math.floor(Date.now() / 1000);
    const isExpired = payload.exp ? payload.exp < nowSec : false;

    // Standardize roles array
    let roles: UserRole[] = [];
    if (Array.isArray(payload.roles)) {
      roles = payload.roles.map((r: string) => r.replace(/^ROLE_/, '') as UserRole);
    } else if (typeof payload.role === 'string') {
      roles = [payload.role.replace(/^ROLE_/, '') as UserRole];
    }

    return {
      sub: payload.sub || 'unknown',
      roles,
      exp: payload.exp,
      iat: payload.iat,
      jti: payload.jti,
      isExpired,
    };
  } catch (err) {
    console.error('Failed to decode JWT:', err);
    return null;
  }
}

/**
 * Mints an HS256 JWT using the Web Crypto API, compatible with backend Nimbus JwtDecoder.
 */
export async function mintDevToken(
  role: UserRole,
  subject: string,
  secret: string = DEV_JWT_SECRET,
  ttlHours: number = 8
): Promise<string> {
  const header = {
    alg: 'HS256',
    typ: 'JWT',
  };

  const nowSec = Math.floor(Date.now() / 1000);
  const expSec = nowSec + ttlHours * 3600;

  const payload = {
    jti: crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2),
    sub: subject,
    roles: [role],
    iat: nowSec,
    exp: expSec,
  };

  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const message = `${encodedHeader}.${encodedPayload}`;

  const encoder = new TextEncoder();
  const keyData = encoder.encode(secret);
  const messageData = encoder.encode(message);

  const key = await window.crypto.subtle.importKey(
    'raw',
    keyData,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );

  const signatureBuffer = await window.crypto.subtle.sign('HMAC', key, messageData);
  const encodedSignature = base64UrlEncode(new Uint8Array(signatureBuffer));

  return `${message}.${encodedSignature}`;
}
