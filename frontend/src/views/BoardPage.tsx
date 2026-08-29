import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { incidentService } from '../services/incidentService';
import type { Incident, IncidentStatus, ApiError } from '../types';
import { JwtAccessPanel } from '../components/auth/JwtAccessPanel';
import { IncidentCard } from '../components/incidents/IncidentCard';
import { IncidentFilter } from '../components/incidents/IncidentFilter';
import { CreateIncidentModal } from '../components/incidents/CreateIncidentModal';
import { Layout } from '../components/layout/Layout';
import {
  AlertTriangle,
  Layers,
  CheckCircle2,
  Inbox,
  AlertCircle,
  Terminal,
  Activity,
  Plus,
} from 'lucide-react';

export const BoardPage: React.FC = () => {
  const { token, role, subject } = useAuth();
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<IncidentStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const fetchIncidents = useCallback(async () => {
    if (!token) return;
    setIsRefreshing(true);
    setError(null);
    try {
      // Backend automatically applies role-based filter based on JWT subject and role
      const data = await incidentService.getAll();
      setIncidents(data);
    } catch (err: unknown) {
      console.error('Error fetching incidents:', err);
      setError(err as ApiError);
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  }, [token]);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents, token, role]);

  // Compute counts for filter tabs
  const counts = useMemo(() => {
    const map: Record<string, number> = {
      ALL: incidents.length,
      OPEN: 0,
      ASSIGNED: 0,
      IN_PROGRESS: 0,
      RESOLVED: 0,
      CLOSED: 0,
    };
    for (const inc of incidents) {
      if (map[inc.status] !== undefined) {
        map[inc.status]++;
      }
    }
    return map;
  }, [incidents]);

  // Filter incidents by status and search text
  const filteredIncidents = useMemo(() => {
    return incidents.filter((inc) => {
      const matchStatus = selectedStatus === 'ALL' || inc.status === selectedStatus;
      const q = searchQuery.toLowerCase().trim();
      const matchSearch =
        !q ||
        inc.id.toString().includes(q) ||
        inc.title.toLowerCase().includes(q) ||
        inc.description.toLowerCase().includes(q) ||
        (inc.assetId && inc.assetId.toLowerCase().includes(q)) ||
        inc.reporterId.toLowerCase().includes(q) ||
        (inc.assigneeId && inc.assigneeId.toLowerCase().includes(q));
      return matchStatus && matchSearch;
    });
  }, [incidents, selectedStatus, searchQuery]);

  // Summary statistics
  const activeCount = useMemo(
    () => incidents.filter((i) => i.status !== 'RESOLVED' && i.status !== 'CLOSED').length,
    [incidents]
  );
  const breachedCount = useMemo(
    () => incidents.filter((i) => Boolean(i.slaBreachedAt)).length,
    [incidents]
  );
  const resolvedCount = useMemo(
    () => incidents.filter((i) => i.status === 'RESOLVED' || i.status === 'CLOSED').length,
    [incidents]
  );

  return (
    <Layout onRefresh={fetchIncidents} isRefreshing={isRefreshing}>
      {/* JWT Identity Engine */}
      <JwtAccessPanel />

      {/* Control Room Metric Summary Bar & Create Action */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div>
          <h2 className="text-lg font-bold font-mono text-neutral-100 uppercase tracking-wider flex items-center gap-2">
            <span>Incident Operations Queue</span>
            <span className="text-xs text-neutral-500 font-normal">
              ({role}: {subject})
            </span>
          </h2>
          <p className="text-xs text-neutral-400 font-mono">
            Live work-orders filtered by Spring Security backend authorization rules
          </p>
        </div>

        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="bg-amber-500 hover:bg-amber-400 text-black font-mono font-bold text-xs px-4 py-2 rounded flex items-center gap-1.5 shadow-sm transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Report Incident</span>
        </button>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        <div className="bg-[#14171d] border border-[#262c36] p-3 rounded">
          <div className="flex items-center justify-between text-neutral-500 text-xs font-mono">
            <span>TOTAL VISIBLE</span>
            <Layers className="w-3.5 h-3.5 text-neutral-400" />
          </div>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-mono text-2xl font-bold text-neutral-100">{incidents.length}</span>
            <span className="text-[10px] font-mono text-neutral-500">work-orders</span>
          </div>
        </div>

        <div className="bg-[#14171d] border border-[#262c36] p-3 rounded">
          <div className="flex items-center justify-between text-amber-400/80 text-xs font-mono">
            <span>ACTIVE / IN-FLIGHT</span>
            <Activity className="w-3.5 h-3.5 text-amber-400" />
          </div>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-mono text-2xl font-bold text-amber-300">{activeCount}</span>
            <span className="text-[10px] font-mono text-neutral-500">open/progress</span>
          </div>
        </div>

        <div className="bg-[#14171d] border border-[#262c36] p-3 rounded">
          <div className="flex items-center justify-between text-rose-400/80 text-xs font-mono">
            <span>SLA BREACHED</span>
            <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
          </div>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-mono text-2xl font-bold text-rose-400">{breachedCount}</span>
            <span className="text-[10px] font-mono text-neutral-500">deadline passed</span>
          </div>
        </div>

        <div className="bg-[#14171d] border border-[#262c36] p-3 rounded">
          <div className="flex items-center justify-between text-emerald-400/80 text-xs font-mono">
            <span>COMPLETED</span>
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
          </div>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="font-mono text-2xl font-bold text-emerald-400">{resolvedCount}</span>
            <span className="text-[10px] font-mono text-neutral-500">resolved/closed</span>
          </div>
        </div>
      </div>

      {/* Filter and search */}
      <IncidentFilter
        selectedStatus={selectedStatus}
        onSelectStatus={setSelectedStatus}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        counts={counts}
      />

      {/* Main Content Area */}
      {loading ? (
        <div className="bg-[#14171d] border border-[#262c36] rounded p-12 text-center font-mono">
          <div className="inline-block animate-spin text-amber-400 mb-3">
            <Terminal className="w-8 h-8" />
          </div>
          <div className="text-neutral-300 text-sm font-bold">QUERYING INCIDENT SERVICE (:8082)...</div>
          <div className="text-neutral-500 text-xs mt-1">GET /api/v1/incidents with Bearer JWT</div>
        </div>
      ) : error ? (
        <div className="bg-rose-950/20 border border-rose-800/60 rounded p-6 font-mono text-xs">
          <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-2">
            <AlertCircle className="w-5 h-5" />
            <span>BACKEND INTEGRATION ERROR ({error.status || 'OFFLINE'})</span>
          </div>
          <div className="text-neutral-300 mb-2">
            <strong>Endpoint:</strong> GET /api/v1/incidents
          </div>
          <div className="bg-black/60 p-3 rounded border border-rose-900/50 text-rose-300">
            {error.message || 'Failed to communicate with Incident Service. Ensure Docker Compose is running.'}
          </div>
          <div className="mt-4 flex gap-2">
            <button
              onClick={fetchIncidents}
              className="px-3 py-1.5 bg-rose-900/60 hover:bg-rose-800 text-white rounded border border-rose-700 font-bold"
            >
              Retry Request
            </button>
          </div>
        </div>
      ) : filteredIncidents.length === 0 ? (
        <div className="bg-[#14171d] border border-[#262c36] rounded p-12 text-center font-mono">
          <Inbox className="w-10 h-10 text-neutral-600 mx-auto mb-3" />
          <div className="text-neutral-300 text-sm font-bold">NO INCIDENTS MATCH CRITERIA</div>
          <div className="text-neutral-500 text-xs mt-2 max-w-md mx-auto">
            {role === 'STUDENT' && (
              <span>
                As a <strong>STUDENT ({subject})</strong>, the backend Spring Security layer only returns
                incidents reported by your JWT identity. Click <strong>&quot;Report Incident&quot;</strong> above to create one.
              </span>
            )}
            {role === 'TECHNICIAN' && (
              <span>
                As a <strong>TECHNICIAN ({subject})</strong>, the backend only returns incidents assigned to
                your subject. Switch to <strong>MANAGER</strong> to assign an incident to {subject}.
              </span>
            )}
            {role === 'MANAGER' && (
              <span>No incidents currently exist in the database with status &quot;{selectedStatus}&quot;.</span>
            )}
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredIncidents.map((incident) => (
            <IncidentCard key={incident.id} incident={incident} />
          ))}
        </div>
      )}

      {/* Create Incident Modal */}
      <CreateIncidentModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => fetchIncidents()}
      />
    </Layout>
  );
};
