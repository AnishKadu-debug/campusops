import React from 'react';
import type { Incident, Asset } from '../../types';
import {
  Database,
  ShieldCheck,
  Radio,
  Bell,
  Cpu,
  Layers,
  AlertTriangle,
  Server,
} from 'lucide-react';

interface SystemTraceProps {
  incident: Incident;
  asset?: Asset | null;
}

export const SystemTrace: React.FC<SystemTraceProps> = ({ incident, asset }) => {
  const isAssigned = Boolean(incident.assigneeId || incident.status !== 'OPEN');
  const hasAsset = Boolean(incident.assetId);
  const isBreached = Boolean(incident.slaBreachedAt);

  return (
    <div className="bg-[#14171d] border border-[#262c36] rounded-md p-5 mt-6 font-mono text-xs">
      {/* Title */}
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-[#262c36]">
        <div className="flex items-center gap-2">
          <div className="p-1 bg-amber-950/40 border border-amber-600/40 rounded text-amber-400">
            <Layers className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-neutral-200 uppercase tracking-wider text-sm">
              Live System Trace &amp; Architectural Flow
            </h3>
            <span className="text-[11px] text-neutral-500 font-normal">
              Direct reverse-engineered view of backend execution paths for Incident #{incident.id}
            </span>
          </div>
        </div>
        <div className="hidden sm:flex items-center gap-2 text-[10px] text-neutral-500">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span>REAL BACKEND STATE</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Primary Lifecycle Pipeline */}
        <div className="space-y-4">
          <div className="text-neutral-400 font-bold tracking-wider text-[11px] uppercase flex items-center gap-1.5 border-b border-[#262c36] pb-1.5">
            <span>[1] INGESTION &amp; DISPATCH PIPELINE</span>
          </div>

          {/* Step 1: HTTP Request */}
          <div className="relative pl-6 pb-4 border-l border-neutral-700">
            <span className="absolute -left-2.5 top-0 w-5 h-5 rounded-full bg-neutral-900 border border-neutral-600 flex items-center justify-center text-[10px] text-neutral-300">
              1
            </span>
            <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
              <div className="text-[10px] text-neutral-500 uppercase font-bold flex items-center justify-between">
                <span>HTTP REST INGRESS</span>
                <span className="text-cyan-400">POST /api/v1/incidents</span>
              </div>
              <div className="mt-1 text-neutral-300">
                Incoming request received by <code className="text-amber-300">IncidentController.createIncident()</code>
              </div>
            </div>
          </div>

          {/* Step 2: JWT Security Gate */}
          <div className="relative pl-6 pb-4 border-l border-neutral-700">
            <span className="absolute -left-2.5 top-0 w-5 h-5 rounded-full bg-neutral-900 border border-neutral-600 flex items-center justify-center text-[10px] text-neutral-300">
              2
            </span>
            <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
              <div className="text-[10px] text-neutral-500 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <ShieldCheck className="w-3 h-3 text-emerald-400" />
                  SPRING SECURITY &amp; JWT
                </span>
                <span className="text-emerald-400">HS256 VALIDATED</span>
              </div>
              <div className="mt-1 text-neutral-300">
                Extracted subject: <code className="text-cyan-300 font-bold">{incident.reporterId}</code>
              </div>
              <div className="text-[10px] text-neutral-500 mt-1">
                * Note: Body claims are ignored; reporter identity is forced from JWT token subject.
              </div>
            </div>
          </div>

          {/* Step 3: Incident Service & SLA calculation */}
          <div className="relative pl-6 pb-4 border-l border-neutral-700">
            <span className="absolute -left-2.5 top-0 w-5 h-5 rounded-full bg-neutral-900 border border-neutral-600 flex items-center justify-center text-[10px] text-neutral-300">
              3
            </span>
            <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
              <div className="text-[10px] text-neutral-500 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <Cpu className="w-3 h-3 text-amber-400" />
                  INCIDENT SERVICE + SLA ENGINE
                </span>
                <span className="text-amber-400">PRIORITY: {incident.priority}</span>
              </div>
              <div className="mt-1 text-neutral-300">
                Deadline set to: <code className="text-neutral-200">{new Date(incident.slaDeadline).toLocaleString()}</code>
              </div>
              <div className="text-[10px] text-neutral-500 mt-0.5">
                Calculated via <code className="text-neutral-400">SlaCalculator</code> (LOW: 48h, MED: 12h, HIGH: 4h, CRIT: 30m)
              </div>
            </div>
          </div>

          {/* Step 4: PostgreSQL Persistence */}
          <div className="relative pl-6 pb-4 border-l border-neutral-700">
            <span className="absolute -left-2.5 top-0 w-5 h-5 rounded-full bg-neutral-900 border border-neutral-600 flex items-center justify-center text-[10px] text-neutral-300">
              4
            </span>
            <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
              <div className="text-[10px] text-neutral-500 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <Database className="w-3 h-3 text-blue-400" />
                  POSTGRESQL TRANSACTION
                </span>
                <span className="text-blue-400">campusops_incident</span>
              </div>
              <div className="mt-1 text-neutral-300">
                Persisted incident ID <strong className="text-neutral-100">#{incident.id}</strong> (Status: <code className="text-amber-300">{incident.status}</code>)
              </div>
            </div>
          </div>

          {/* Step 5: Kafka Event Publishing */}
          <div className="relative pl-6 border-l border-neutral-700">
            <span className="absolute -left-2.5 top-0 w-5 h-5 rounded-full bg-neutral-900 border border-neutral-600 flex items-center justify-center text-[10px] text-neutral-300">
              5
            </span>
            <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
              <div className="text-[10px] text-neutral-500 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <Radio className="w-3 h-3 text-indigo-400" />
                  KAFKA EVENT PRODUCER
                </span>
                <span className="text-indigo-400 font-mono">AFTER_COMMIT</span>
              </div>
              <div className="mt-1 text-neutral-300">
                Published to topic: <code className="text-amber-300 bg-neutral-900 px-1 py-0.5 rounded">incident.created.v1</code>
              </div>
              <div className="text-[10px] text-neutral-500 mt-1">
                Payload contains: incidentId: {incident.id}, reporterId: {incident.reporterId}, title: &quot;{incident.title}&quot;
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Downstream & Asynchronous Integrations */}
        <div className="space-y-4">
          <div className="text-neutral-400 font-bold tracking-wider text-[11px] uppercase flex items-center gap-1.5 border-b border-[#262c36] pb-1.5">
            <span>[2] ASYNC CONSUMERS &amp; DISTRIBUTED INTEGRATIONS</span>
          </div>

          {/* Feign / Asset Service block */}
          {hasAsset ? (
            <div className="bg-[#0c0e12] border border-cyan-900/60 p-3 rounded">
              <div className="text-[10px] text-cyan-400 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <Server className="w-3 h-3 text-cyan-400" />
                  EUREKA + FEIGN INTERACTION
                </span>
                <span className="text-[10px] bg-cyan-950 text-cyan-300 px-1.5 py-0.2 rounded border border-cyan-800">
                  ASSET RESOLVED
                </span>
              </div>
              <div className="mt-2 space-y-1 text-neutral-300">
                <div className="text-xs">
                  Asset ID: <code className="text-cyan-300">{incident.assetId}</code>
                </div>
                {asset && (
                  <div className="text-neutral-400 text-[11px]">
                    Name: <strong className="text-neutral-200">{asset.name}</strong> | Type: {asset.type} | Loc: {asset.location}
                  </div>
                )}
                <div className="text-[10px] text-neutral-500 pt-1 border-t border-[#262c36] mt-1">
                  Path: Incident Service &rarr; Eureka Discovery (&quot;asset-service&quot;) &rarr; OpenFeign (Bearer JWT propagated) &rarr; MongoDB
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-[#0c0e12]/60 border border-[#262c36] p-3 rounded text-neutral-500">
              <div className="text-[10px] uppercase font-bold text-neutral-500">
                EUREKA + FEIGN ASSET LOOKUP
              </div>
              <div className="mt-1 text-[11px]">
                No asset attached to this incident. Feign call skipped.
              </div>
            </div>
          )}

          {/* Assignment Event & Path */}
          {isAssigned ? (
            <div className="bg-[#0c0e12] border border-sky-900/60 p-3 rounded">
              <div className="text-[10px] text-sky-400 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <Radio className="w-3 h-3 text-sky-400" />
                  ASSIGNMENT WORKFLOW &amp; KAFKA
                </span>
                <span className="text-sky-300 font-mono">PATCH /assign</span>
              </div>
              <div className="mt-2 space-y-1 text-neutral-300">
                <div className="text-xs">
                  Assignee: <code className="text-sky-300 font-bold">{incident.assigneeId || 'tech-A'}</code>
                </div>
                <div className="text-[10px] text-neutral-400">
                  Manager dispatched assignment &rarr; Published event to <code className="text-amber-300">incident.assigned.v1</code>
                </div>
                <div className="text-[10px] text-neutral-500">
                  State transition: OPEN &rarr; ASSIGNED
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-[#0c0e12]/60 border border-[#262c36] p-3 rounded text-neutral-500">
              <div className="text-[10px] uppercase font-bold text-neutral-500">
                ASSIGNMENT EVENT (incident.assigned.v1)
              </div>
              <div className="mt-1 text-[11px]">
                Incident is currently OPEN and unassigned.
              </div>
            </div>
          )}

          {/* Notification Service Consumer */}
          <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
            <div className="text-[10px] text-neutral-500 uppercase font-bold flex items-center justify-between">
              <span className="flex items-center gap-1">
                <Bell className="w-3 h-3 text-amber-400" />
                NOTIFICATION SERVICE CONSUMER (:8084)
              </span>
              <span className="text-amber-400 font-mono">PORT 8084</span>
            </div>
            <div className="mt-2 text-neutral-300">
              Listens to Kafka consumer group <code className="text-neutral-400">campusops-notification-group</code>
            </div>
            <div className="text-[10px] text-neutral-500 mt-1">
              Records notification entry in <code className="text-neutral-400">campusops_notification</code> PostgreSQL DB.
            </div>
          </div>

          {/* SLA Breach Scanner Trace */}
          {isBreached ? (
            <div className="bg-[#0c0e12] border border-rose-900/80 p-3 rounded">
              <div className="text-[10px] text-rose-400 uppercase font-bold flex items-center justify-between">
                <span className="flex items-center gap-1">
                  <AlertTriangle className="w-3 h-3 text-rose-400" />
                  SLA BREACH DETECTED &amp; BROADCAST
                </span>
                <span className="bg-rose-950 text-rose-300 px-1 py-0.2 rounded border border-rose-800 text-[10px]">
                  BREACHED
                </span>
              </div>
              <div className="mt-2 space-y-1 text-neutral-300">
                <div className="text-[11px]">
                  Breached at: <code className="text-rose-300">{new Date(incident.slaBreachedAt!).toLocaleString()}</code>
                </div>
                <div className="text-[10px] text-neutral-400">
                  <code className="text-neutral-300">SlaBreachScanner</code> ran scheduled scan (30s) &rarr; Published to <code className="text-rose-400">incident.sla-breached.v1</code>
                </div>
                <div className="text-[10px] text-neutral-500">
                  Deduplicated by non-null <code className="text-neutral-400">slaBreachedAt</code> database column.
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-[#0c0e12]/60 border border-[#262c36] p-3 rounded text-neutral-500">
              <div className="text-[10px] uppercase font-bold text-neutral-500 flex items-center justify-between">
                <span>SLA BREACH SCANNER</span>
                <span className="text-emerald-500 text-[10px]">ACTIVE &bull; WITHIN SLA</span>
              </div>
              <div className="mt-1 text-[11px]">
                Periodic scanner runs every 30s. No breach triggered.
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
