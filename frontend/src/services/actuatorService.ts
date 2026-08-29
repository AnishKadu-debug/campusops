export interface ActuatorHealth {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  components?: {
    db?: { status: string; details?: Record<string, unknown> };
    diskSpace?: { status: string; details?: Record<string, unknown> };
    kafka?: { status: string; details?: Record<string, unknown> };
    mongo?: { status: string; details?: Record<string, unknown> };
    ping?: { status: string };
    discoveryComposite?: { status: string };
  };
}

export interface ServiceHealthReport {
  name: string;
  port: number;
  role: string;
  database: string;
  status: 'UP' | 'DOWN' | 'CHECKING';
  details?: ActuatorHealth;
  error?: string;
  responseTimeMs?: number;
}

export const actuatorService = {
  async checkService(
    name: string,
    endpoint: string,
    port: number,
    role: string,
    database: string
  ): Promise<ServiceHealthReport> {
    const start = performance.now();
    try {
      const res = await fetch(endpoint, { cache: 'no-store' });
      const elapsed = Math.round(performance.now() - start);

      if (!res.ok) {
        return {
          name,
          port,
          role,
          database,
          status: 'DOWN',
          error: `HTTP ${res.status}: ${res.statusText}`,
          responseTimeMs: elapsed,
        };
      }

      const data = (await res.json()) as ActuatorHealth;
      return {
        name,
        port,
        role,
        database,
        status: data.status === 'UP' ? 'UP' : 'DOWN',
        details: data,
        responseTimeMs: elapsed,
      };
    } catch (err: unknown) {
      const elapsed = Math.round(performance.now() - start);
      return {
        name,
        port,
        role,
        database,
        status: 'DOWN',
        error: (err as Error).message || 'Connection failed',
        responseTimeMs: elapsed,
      };
    }
  },

  async checkAll(): Promise<ServiceHealthReport[]> {
    return Promise.all([
      this.checkService(
        'Incident Service',
        '/api/actuator/incident/health',
        8082,
        'Incident Lifecycle, SLA Engine, Kafka Producer',
        'PostgreSQL (campusops_incident)'
      ),
      this.checkService(
        'Asset Service',
        '/api/actuator/asset/health',
        8083,
        'Campus Asset Registry, MongoDB CRUD',
        'MongoDB (campusops_asset)'
      ),
      this.checkService(
        'Notification Service',
        '/api/actuator/notification/health',
        8084,
        'Kafka Consumer, Notification History',
        'PostgreSQL (campusops_notification)'
      ),
    ]);
  },
};
