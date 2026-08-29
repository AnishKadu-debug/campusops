import React, { useState, useEffect, useCallback } from 'react';
import { Layout } from '../components/layout/Layout';
import { actuatorService, type ServiceHealthReport } from '../services/actuatorService';
import {
  Network,
  Radio,
  Shield,
  Activity,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Clock,
} from 'lucide-react';

export const ArchitecturePage: React.FC = () => {
  const [healthReports, setHealthReports] = useState<ServiceHealthReport[]>([]);
  const [loadingHealth, setLoadingHealth] = useState(true);
  const [lastCheck, setLastCheck] = useState<Date | null>(null);

  const fetchHealth = useCallback(async () => {
    setLoadingHealth(true);
    try {
      const reports = await actuatorService.checkAll();
      setHealthReports(reports);
      setLastCheck(new Date());
    } finally {
      setLoadingHealth(false);
    }
  }, []);

  useEffect(() => {
    fetchHealth();
  }, [fetchHealth]);

  return (
    <Layout onRefresh={fetchHealth} isRefreshing={loadingHealth}>
      <div className="space-y-6 font-mono text-xs">
        {/* Page Header */}
        <div className="bg-[#14171d] border border-[#262c36] p-5 rounded">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <div className="p-1.5 bg-amber-950/50 border border-amber-600/50 rounded text-amber-400">
                <Network className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-neutral-100 uppercase tracking-wider">
                  CampusOps System Topology &amp; Observability
                </h2>
                <p className="text-neutral-400 text-[11px]">
                  Microservices architecture, Spring Boot Actuator health, JWT security flow, and Kafka event matrix
                </p>
              </div>
            </div>

            <button
              onClick={fetchHealth}
              disabled={loadingHealth}
              className="px-3 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 border border-neutral-600 rounded flex items-center gap-1.5 text-[11px]"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingHealth ? 'animate-spin text-amber-400' : ''}`} />
              <span>Poll Actuators</span>
            </button>
          </div>
        </div>

        {/* Live Microservices Actuator Health Monitor */}
        <div className="bg-[#14171d] border border-[#262c36] p-5 rounded space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-[#262c36]">
            <div className="flex items-center gap-2">
              <Activity className="w-4 h-4 text-emerald-400" />
              <span className="font-bold text-neutral-200 uppercase text-xs">
                Live Spring Boot Actuator Health Status
              </span>
            </div>
            {lastCheck && (
              <span className="text-[10px] text-neutral-500 flex items-center gap-1">
                <Clock className="w-3 h-3" /> Checked: {lastCheck.toLocaleTimeString()}
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {healthReports.map((srv) => (
              <div
                key={srv.name}
                className="bg-[#0c0e12] border border-[#262c36] p-3 rounded flex flex-col justify-between space-y-2"
              >
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-bold text-neutral-100 text-xs">{srv.name}</span>
                    <span
                      className={`inline-flex items-center gap-1 text-[10px] font-bold px-1.5 py-0.2 rounded border ${
                        srv.status === 'UP'
                          ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800'
                          : 'bg-rose-950/60 text-rose-400 border-rose-800'
                      }`}
                    >
                      {srv.status === 'UP' ? (
                        <>
                          <CheckCircle2 className="w-3 h-3" /> UP
                        </>
                      ) : (
                        <>
                          <AlertTriangle className="w-3 h-3" /> DOWN
                        </>
                      )}
                    </span>
                  </div>

                  <div className="text-[10px] text-neutral-500 space-y-0.5">
                    <div>Port: <code className="text-neutral-300">:{srv.port}</code></div>
                    <div>Role: <span className="text-neutral-400">{srv.role}</span></div>
                    <div>Database: <span className="text-cyan-300">{srv.database}</span></div>
                  </div>
                </div>

                <div className="pt-2 border-t border-[#262c36] flex items-center justify-between text-[10px] text-neutral-500">
                  <span>Endpoint: /actuator/health</span>
                  <span className="text-neutral-400">{srv.responseTimeMs}ms</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ASCII / Monospace Architecture Diagram Box */}
        <div className="bg-[#0c0e12] border border-[#262c36] p-6 rounded text-neutral-300 overflow-x-auto shadow-inner">
          <div className="text-[11px] text-amber-400 font-bold mb-3 uppercase tracking-wider flex items-center justify-between">
            <span>[ SYSTEM DATA &amp; EVENT FLOW TOPOLOGY ]</span>
            <span className="text-[10px] text-neutral-500">10-Second System Blueprint</span>
          </div>
          <pre className="text-neutral-300 leading-relaxed font-mono text-[11px]">
{`                     +-------------------+
                     |   Eureka Server   | (:8761)
                     | Service Discovery |
                     +---------+---------+
                               |
                               | (registration & lookup)
                               |
Browser / Client               v
       |             +-------------------+
       | (HTTP/JWT)  | Incident Service  | (:8082)
       +------------>|  (Spring Boot 4)  |
       |             +---+-----+-----+---+
       |                 |     |     |
       |                 |     |     +---> OpenFeign (JWT propagated)
       |                 |     |           |
       |                 |     |           v
       |                 |     |     +-------------------+
       |                 |     |     |   Asset Service   | (:8083)
       |                 |     |     |   (Spring Boot 4) |
       |                 |     |     +---------+---------+
       |                 |     |               |
       |                 |     |               v
       |                 |     |     +-------------------+
       |                 |     |     |      MongoDB      | (:27017 / 27018)
       |                 |     |     |  campusops_asset  |
       |                 |     |     +-------------------+
       |                 |     |
       |                 |     +---------> PostgreSQL (:5432 / 5433)
       |                 |                 campusops_incident
       |                 |
       |                 +---------------> Apache Kafka 4.0 (KRaft :9092)
       |                                   Topics:
       |                                     - incident.created.v1
       |                                     - incident.assigned.v1
       |                                     - incident.sla-breached.v1
       |                                           |
       |                                           v
       |                                   +----------------------+
       |                                   | Notification Service | (:8084)
       |                                   |  (Kafka Consumer)    |
       |                                   +-----------+----------+
       |                                               |
       +-----------------------------------------------+
         (GET /api/v1/notifications)                   v
                                           +----------------------+
                                           |      PostgreSQL      |
                                           | campusops_notification
                                           +----------------------+`}
          </pre>
        </div>

        {/* Security & Event Details Matrix */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* JWT & Authorization Rules Deep Dive */}
          <div className="bg-[#14171d] border border-[#262c36] p-4 rounded space-y-2.5">
            <div className="flex items-center gap-2 text-cyan-400 font-bold border-b border-[#262c36] pb-2">
              <Shield className="w-4 h-4" />
              <span className="uppercase text-xs">Security &amp; Authorization Model</span>
            </div>
            <p className="text-neutral-400 text-[11px] leading-relaxed">
              Stateless HS256 JWT tokens. The <strong className="text-neutral-200">Incident Service</strong> forces
              reporter identity from JWT subject on creation (`reporterId = currentUser.subject()`).
            </p>
            <div className="bg-[#0c0e12] p-2.5 rounded border border-[#262c36] space-y-1 text-[10px]">
              <div>&bull; <strong className="text-cyan-300">STUDENT:</strong> Own incidents only (`reporterId == sub`). Allowed to report tickets.</div>
              <div>&bull; <strong className="text-indigo-300">TECHNICIAN:</strong> Assigned incidents only (`assigneeId == sub`). Progress status only.</div>
              <div>&bull; <strong className="text-amber-300">MANAGER:</strong> Universal read/update access &amp; exclusive technician assignment (`@PreAuthorize("hasRole('MANAGER')")`).</div>
              <div>&bull; <strong className="text-emerald-300">OpenFeign:</strong> FeignAuthPropagationInterceptor copies Bearer JWT downstream to Asset Service.</div>
            </div>
          </div>

          {/* Kafka Event Matrix */}
          <div className="bg-[#14171d] border border-[#262c36] p-4 rounded space-y-2.5">
            <div className="flex items-center gap-2 text-indigo-400 font-bold border-b border-[#262c36] pb-2">
              <Radio className="w-4 h-4" />
              <span className="uppercase text-xs">Kafka Event Flow &amp; Topics</span>
            </div>
            <p className="text-neutral-400 text-[11px] leading-relaxed">
              Domain events are published transactionally (AFTER_COMMIT bridge) and consumed by the Notification Service.
            </p>
            <div className="bg-[#0c0e12] p-2.5 rounded border border-[#262c36] space-y-1.5 text-[10px]">
              <div>
                <code className="text-amber-300">incident.created.v1</code>
                <span className="text-neutral-500 block">Published on incident creation &rarr; Notification Service writes INCIDENT_CREATED</span>
              </div>
              <div>
                <code className="text-sky-300">incident.assigned.v1</code>
                <span className="text-neutral-500 block">Published on manager dispatch &rarr; Notification Service writes INCIDENT_ASSIGNED to tech</span>
              </div>
              <div>
                <code className="text-rose-300">incident.sla-breached.v1</code>
                <span className="text-neutral-500 block">Published by SlaBreachScanner (30s) &rarr; Notification Service writes SLA_BREACHED</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};
