import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { incidentService } from '../services/incidentService';
import { assetService } from '../services/assetService';
import { notificationService } from '../services/notificationService';
import type { Incident, Asset, NotificationItem, ApiError } from '../types';
import { StatusBadge } from '../components/common/StatusBadge';
import { PriorityBadge } from '../components/common/PriorityBadge';
import { SlaIndicator } from '../components/common/SlaIndicator';
import { SystemTrace } from '../components/trace/SystemTrace';
import { WorkflowActionPanel } from '../components/incidents/WorkflowActionPanel';
import { JwtAccessPanel } from '../components/auth/JwtAccessPanel';
import { Layout } from '../components/layout/Layout';
import {
  ArrowLeft,
  User,
  Wrench,
  Box,
  Clock,
  Bell,
  AlertCircle,
  Terminal,
  RefreshCw,
  Server,
} from 'lucide-react';

export const DetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token, role } = useAuth();

  const [incident, setIncident] = useState<Incident | null>(null);
  const [asset, setAsset] = useState<Asset | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchDetails = useCallback(async () => {
    if (!id || !token) return;
    setIsRefreshing(true);
    setError(null);
    try {
      // 1. Fetch real incident from Incident Service (:8082)
      const incData = await incidentService.getById(id);
      setIncident(incData);

      // 2. If assetId is present, fetch asset details from Asset Service (:8083)
      if (incData.assetId) {
        try {
          const assetData = await assetService.getById(incData.assetId);
          setAsset(assetData);
        } catch (assetErr) {
          console.warn('Could not fetch asset details from asset-service:', assetErr);
          setAsset(null);
        }
      } else {
        setAsset(null);
      }

      // 3. Fetch notifications for this incident from Notification Service (:8084)
      try {
        const notifData = await notificationService.getNotifications(incData.id);
        setNotifications(notifData);
      } catch (notifErr) {
        console.warn('Could not fetch notifications from notification-service:', notifErr);
        setNotifications([]);
      }
    } catch (err: unknown) {
      console.error('Error fetching incident detail:', err);
      setError(err as ApiError);
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  }, [id, token]);

  useEffect(() => {
    fetchDetails();
  }, [fetchDetails, token, role]);

  return (
    <Layout onRefresh={fetchDetails} isRefreshing={isRefreshing}>
      {/* Access panel to test role authorization differences on the fly */}
      <JwtAccessPanel />

      {/* Navigation header */}
      <div className="flex items-center justify-between mb-4">
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 text-xs font-mono text-neutral-400 hover:text-neutral-200 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>BACK TO OPERATIONS BOARD</span>
        </Link>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchDetails}
            disabled={isRefreshing}
            className="p-1.5 bg-[#14171d] hover:bg-[#1a1e26] text-neutral-400 hover:text-neutral-200 border border-[#262c36] rounded font-mono text-xs flex items-center gap-1"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-amber-400' : ''}`} />
            <span>Sync Live State</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="bg-[#14171d] border border-[#262c36] rounded p-12 text-center font-mono">
          <div className="inline-block animate-spin text-amber-400 mb-3">
            <Terminal className="w-8 h-8" />
          </div>
          <div className="text-neutral-300 text-sm font-bold">LOADING INCIDENT #{id}...</div>
          <div className="text-neutral-500 text-xs mt-1">
            Querying Incident Service (:8082) with Spring Security token
          </div>
        </div>
      ) : error ? (
        <div className="bg-rose-950/20 border border-rose-800/60 rounded p-6 font-mono text-xs">
          <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-2">
            <AlertCircle className="w-5 h-5" />
            <span>AUTHORIZATION OR LOOKUP ERROR ({error.status || 'ERROR'})</span>
          </div>
          <div className="text-neutral-300 mb-2">
            <strong>Endpoint:</strong> GET /api/v1/incidents/{id}
          </div>
          <div className="bg-black/60 p-3 rounded border border-rose-900/50 text-rose-300">
            {error.message || 'Access denied or incident not found.'}
          </div>
          {error.status === 403 && (
            <div className="mt-3 text-neutral-400 bg-neutral-900/80 p-3 rounded border border-neutral-700">
              <strong className="text-amber-400">Security Rule Explanation:</strong>
              <div className="mt-1">
                STUDENTS can only view their own incidents (`reporterId == sub`).
                TECHNICIANS can only view assigned incidents (`assigneeId == sub`).
                MANAGERS have universal access. Switch roles in the panel above to test access control.
              </div>
            </div>
          )}
          <div className="mt-4 flex gap-2">
            <button
              onClick={() => navigate('/')}
              className="px-3 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-white rounded font-bold"
            >
              Return to Board
            </button>
            <button
              onClick={fetchDetails}
              className="px-3 py-1.5 bg-rose-900/60 hover:bg-rose-800 text-white rounded border border-rose-700 font-bold"
            >
              Retry
            </button>
          </div>
        </div>
      ) : incident ? (
        <div className="space-y-6">
          {/* Main Work-order Header Card */}
          <div className="bg-[#14171d] border border-[#262c36] rounded-md p-5">
            <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-[#262c36]">
              <div className="flex items-center gap-3">
                <span className="font-mono text-lg font-bold text-neutral-100 bg-[#0c0e12] border border-neutral-700 px-2.5 py-1 rounded">
                  INCIDENT #{incident.id}
                </span>
                <StatusBadge status={incident.status} size="md" />
                <PriorityBadge priority={incident.priority} showSlaDuration />
              </div>
              <div className="text-neutral-500 font-mono text-xs">
                Created: {new Date(incident.createdAt).toLocaleString()}
              </div>
            </div>

            {/* Title & Full Description */}
            <div className="mt-4">
              <h2 className="text-xl font-bold text-neutral-100 mb-2">{incident.title}</h2>
              <div className="bg-[#0c0e12] border border-[#262c36] p-3.5 rounded text-neutral-300 text-xs font-mono whitespace-pre-wrap leading-relaxed">
                {incident.description}
              </div>
            </div>

            {/* Metadata Matrix */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-5">
              {/* Reporter */}
              <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
                <div className="text-neutral-500 text-[10px] font-mono uppercase flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5 text-cyan-400" />
                  REPORTER IDENTITY
                </div>
                <div className="font-mono text-sm font-bold text-cyan-300 mt-1">
                  {incident.reporterId}
                </div>
                <div className="text-[10px] text-neutral-500 mt-0.5">
                  Extracted from JWT subject on creation
                </div>
              </div>

              {/* Assignee */}
              <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
                <div className="text-neutral-500 text-[10px] font-mono uppercase flex items-center gap-1.5">
                  <Wrench className="w-3.5 h-3.5 text-indigo-400" />
                  ASSIGNEE
                </div>
                <div className="font-mono text-sm font-bold text-neutral-200 mt-1">
                  {incident.assigneeId || (
                    <span className="text-neutral-500 font-normal italic">Unassigned</span>
                  )}
                </div>
                <div className="text-[10px] text-neutral-500 mt-0.5">
                  {incident.assigneeId
                    ? 'Dispatched via manager assignment'
                    : 'Awaiting manager dispatch'}
                </div>
              </div>

              {/* SLA Status */}
              <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
                <div className="text-neutral-500 text-[10px] font-mono uppercase flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-amber-400" />
                  SLA DEADLINE
                </div>
                <div className="mt-1">
                  <SlaIndicator
                    deadline={incident.slaDeadline}
                    breachedAt={incident.slaBreachedAt}
                    status={incident.status}
                    createdAt={incident.createdAt}
                  />
                </div>
                <div className="text-[10px] text-neutral-500 mt-1 font-mono">
                  Deadline: {new Date(incident.slaDeadline).toLocaleTimeString()}
                </div>
              </div>
            </div>

            {/* Real Asset Details (OpenFeign -> Eureka -> Asset Service -> MongoDB) */}
            {incident.assetId && (
              <div className="mt-4 bg-[#0c0e12] border border-cyan-950 p-4 rounded">
                <div className="flex items-center justify-between mb-3 pb-2 border-b border-cyan-950">
                  <div className="text-cyan-400 text-xs font-mono font-bold flex items-center gap-1.5">
                    <Box className="w-4 h-4" />
                    ASSOCIATED CAMPUS ASSET &amp; DISTRIBUTED FEIGN FLOW
                  </div>
                  <span className="text-[10px] font-mono text-neutral-500 flex items-center gap-1">
                    <Server className="w-3 h-3 text-cyan-400" />
                    OpenFeign &rarr; Eureka Discovery &rarr; Asset Service (:8083) &rarr; MongoDB
                  </span>
                </div>

                {asset ? (
                  <div className="space-y-3 font-mono text-xs">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                      <div>
                        <span className="text-neutral-500 text-[10px] block">ASSET NAME</span>
                        <strong className="text-neutral-200">{asset.name}</strong>
                      </div>
                      <div>
                        <span className="text-neutral-500 text-[10px] block">TYPE</span>
                        <span className="text-cyan-300 font-bold">{asset.type}</span>
                      </div>
                      <div>
                        <span className="text-neutral-500 text-[10px] block">LOCATION</span>
                        <span className="text-neutral-300">{asset.location}</span>
                      </div>
                      <div>
                        <span className="text-neutral-500 text-[10px] block">STATUS</span>
                        <span className="text-emerald-400 font-bold">{asset.status}</span>
                      </div>
                    </div>

                    {asset.attributes && Object.keys(asset.attributes).length > 0 && (
                      <div className="bg-[#14171d] p-2.5 rounded border border-[#262c36]">
                        <span className="text-[10px] text-neutral-500 block uppercase font-bold mb-1">
                          Dynamic MongoDB Attributes (Polyglot Persistence):
                        </span>
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px]">
                          {Object.entries(asset.attributes).map(([k, v]) => (
                            <div key={k}>
                              <span className="text-neutral-500">{k}:</span>{' '}
                              <span className="text-amber-300">{String(v)}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-xs font-mono text-neutral-400">
                    Asset ID: <code className="text-cyan-300">{incident.assetId}</code> (Asset record
                    fetched or Circuit Breaker fallback applied)
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Real Lifecycle Action Controls (Manager Assignment & Status Progression) */}
          <WorkflowActionPanel incident={incident} onUpdate={fetchDetails} />

          {/* Real Notification History (Consumed from Kafka by Notification Service) */}
          <div className="bg-[#14171d] border border-[#262c36] rounded-md p-5 font-mono text-xs">
            <div className="flex items-center justify-between pb-3 mb-3 border-b border-[#262c36]">
              <div className="flex items-center gap-2">
                <div className="p-1 bg-neutral-800 border border-neutral-700 rounded text-amber-400">
                  <Bell className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-neutral-200 text-sm">
                    NOTIFICATION TIMELINE (NOTIFICATION SERVICE :8084)
                  </h3>
                  <span className="text-[11px] text-neutral-500">
                    Real records consumed asynchronously from Kafka topics &amp; persisted to PostgreSQL
                  </span>
                </div>
              </div>
              <span className="text-[10px] text-neutral-500 bg-neutral-900 px-2 py-0.5 rounded border border-neutral-700">
                {notifications.length} RECORD(S)
              </span>
            </div>

            {notifications.length === 0 ? (
              <div className="p-4 text-center text-neutral-500 bg-[#0c0e12] rounded border border-[#262c36]">
                No notification records currently found in Notification Service for Incident #{incident.id}.
              </div>
            ) : (
              <div className="space-y-2">
                {notifications.map((notif) => (
                  <div
                    key={notif.id}
                    className="bg-[#0c0e12] border border-[#262c36] p-3 rounded flex flex-wrap items-center justify-between gap-2"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`font-bold px-1.5 py-0.2 rounded text-[10px] border ${
                            notif.eventType === 'SLA_BREACHED'
                              ? 'bg-rose-950/60 text-rose-300 border-rose-700'
                              : notif.eventType === 'INCIDENT_ASSIGNED'
                              ? 'bg-sky-950/60 text-sky-300 border-sky-700'
                              : 'bg-amber-950/60 text-amber-300 border-amber-700'
                          }`}
                        >
                          {notif.eventType}
                        </span>
                        <span className="text-neutral-300 text-xs">{notif.message}</span>
                      </div>
                      <div className="text-[10px] text-neutral-500">
                        Recipient: <code className="text-cyan-300">{notif.recipientId}</code> &bull;
                        Topic Source:{' '}
                        <code className="text-neutral-400">
                          {notif.eventType === 'INCIDENT_CREATED' && 'incident.created.v1'}
                          {notif.eventType === 'INCIDENT_ASSIGNED' && 'incident.assigned.v1'}
                          {notif.eventType === 'SLA_BREACHED' && 'incident.sla-breached.v1'}
                        </code>
                      </div>
                    </div>
                    <div className="text-[10px] text-neutral-500 font-mono">
                      {new Date(notif.createdAt).toLocaleTimeString()}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* System Trace Learning View */}
          <SystemTrace incident={incident} asset={asset} />
        </div>
      ) : null}
    </Layout>
  );
};
