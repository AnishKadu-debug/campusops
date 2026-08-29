export type UserRole = 'STUDENT' | 'TECHNICIAN' | 'MANAGER';

export interface DecodedToken {
  sub: string;
  roles: UserRole[];
  exp?: number;
  iat?: number;
  jti?: string;
  isExpired?: boolean;
}

export type IncidentStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export type IncidentPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Incident {
  id: number;
  title: string;
  description: string;
  status: IncidentStatus;
  priority: IncidentPriority;
  assetId: string | null;
  reporterId: string;
  assigneeId: string | null;
  slaDeadline: string;
  slaBreachedAt: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIncidentRequest {
  title: string;
  description: string;
  priority: IncidentPriority;
  assetId?: string;
  reporterId: string;
}

export interface AssignIncidentRequest {
  assigneeId: string;
}

export interface UpdateIncidentStatusRequest {
  status: IncidentStatus;
}

export interface UpdateIncidentRequest {
  title: string;
  description: string;
  priority: IncidentPriority;
  assetId?: string;
}

export type AssetType = 'PROJECTOR' | 'ROUTER' | 'AC' | 'LAB_EQUIPMENT';
export type AssetStatus = 'ACTIVE' | 'MAINTENANCE' | 'DECOMMISSIONED';

export interface Asset {
  id: string;
  name: string;
  type: AssetType;
  location: string;
  status: AssetStatus;
  attributes?: Record<string, unknown>;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type NotificationEventType = 'INCIDENT_CREATED' | 'INCIDENT_ASSIGNED' | 'SLA_BREACHED';

export interface NotificationItem {
  id: number;
  incidentId: number;
  eventType: NotificationEventType;
  recipientId: string;
  message: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiError {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
}
