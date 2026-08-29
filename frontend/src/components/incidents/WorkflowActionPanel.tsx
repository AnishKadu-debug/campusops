import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { incidentService } from '../../services/incidentService';
import type { Incident, IncidentStatus, ApiError } from '../../types';
import { StatusBadge } from '../common/StatusBadge';
import {
  Wrench,
  ArrowRight,
  ShieldAlert,
  CheckCircle2,
  AlertCircle,
  Send,
} from 'lucide-react';

interface WorkflowActionPanelProps {
  incident: Incident;
  onUpdate: () => void;
}

export const WorkflowActionPanel: React.FC<WorkflowActionPanelProps> = ({
  incident,
  onUpdate,
}) => {
  const { role, subject } = useAuth();
  const [assigneeInput, setAssigneeInput] = useState('tech-A');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionError, setActionError] = useState<ApiError | null>(null);
  const [lastActionSuccess, setLastActionSuccess] = useState<string | null>(null);

  // Compute next allowed transitions based on backend IncidentStatus enum
  const getNextStatus = (current: IncidentStatus): IncidentStatus | null => {
    switch (current) {
      case 'OPEN':
        return 'ASSIGNED';
      case 'ASSIGNED':
        return 'IN_PROGRESS';
      case 'IN_PROGRESS':
        return 'RESOLVED';
      case 'RESOLVED':
        return 'CLOSED';
      case 'CLOSED':
        return null;
    }
  };

  const nextStatus = getNextStatus(incident.status);

  // Determine authorization permissions according to IncidentServiceImpl rules
  const isManager = role === 'MANAGER';
  const isAssignedTech = role === 'TECHNICIAN' && incident.assigneeId === subject;
  const isReporterStudent = role === 'STUDENT' && incident.reporterId === subject;

  // Status progression allowed if MANAGER, or assigned TECHNICIAN, or reporter STUDENT
  const canProgressStatus = isManager || isAssignedTech || isReporterStudent;

  // Assignment allowed ONLY for MANAGER
  const canAssign = isManager;

  const handleAssign = async (targetAssignee: string) => {
    if (!targetAssignee.trim()) return;
    setIsSubmitting(true);
    setActionError(null);
    setLastActionSuccess(null);

    try {
      await incidentService.assign(incident.id, { assigneeId: targetAssignee.trim() });
      setLastActionSuccess(
        `Dispatched to ${targetAssignee}. State updated to ASSIGNED & event published to incident.assigned.v1.`
      );
      onUpdate();
    } catch (err: unknown) {
      console.error('Assignment error:', err);
      setActionError(err as ApiError);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleProgressStatus = async (targetStatus: IncidentStatus) => {
    setIsSubmitting(true);
    setActionError(null);
    setLastActionSuccess(null);

    try {
      await incidentService.updateStatus(incident.id, { status: targetStatus });
      setLastActionSuccess(
        `Lifecycle state progressed to ${targetStatus} successfully.`
      );
      onUpdate();
    } catch (err: unknown) {
      console.error('Status transition error:', err);
      setActionError(err as ApiError);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-[#14171d] border border-[#262c36] rounded-md p-5 font-mono text-xs space-y-4">
      {/* Title */}
      <div className="flex items-center justify-between pb-3 border-b border-[#262c36]">
        <div className="flex items-center gap-2">
          <div className="p-1 bg-indigo-950/50 border border-indigo-600/50 rounded text-indigo-400">
            <Wrench className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-neutral-100 uppercase tracking-wider text-sm">
              Operational Lifecycle &amp; State Controls
            </h3>
            <span className="text-[10px] text-neutral-500 font-normal">
              Direct execution of real backend state transitions and manager assignment endpoints
            </span>
          </div>
        </div>

        <div className="text-[10px] text-neutral-400">
          CURRENT STATE: <StatusBadge status={incident.status} size="sm" />
        </div>
      </div>

      {/* Success / Error Alerts */}
      {lastActionSuccess && (
        <div className="bg-emerald-950/40 border border-emerald-800 p-3 rounded text-emerald-300 flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
          <span>{lastActionSuccess}</span>
        </div>
      )}

      {actionError && (
        <div className="bg-rose-950/40 border border-rose-800 p-3 rounded text-rose-300 flex items-start gap-2">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
          <div>
            <strong className="block text-rose-200">
              Backend Rejected Operation ({actionError.status}):
            </strong>
            {actionError.message}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* LEFT COLUMN: MANAGER ASSIGNMENT */}
        <div className="bg-[#0c0e12] border border-[#262c36] p-4 rounded flex flex-col justify-between space-y-3">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="font-bold text-neutral-200 uppercase text-[11px] flex items-center gap-1.5">
                <Send className="w-3.5 h-3.5 text-amber-400" />
                [MANAGER] Dispatch Technician
              </span>
              <span
                className={`text-[9px] px-1.5 py-0.2 rounded border font-mono ${
                  canAssign
                    ? 'bg-amber-950/50 text-amber-300 border-amber-700'
                    : 'bg-neutral-900 text-neutral-600 border-neutral-800'
                }`}
              >
                PATCH /{incident.id}/assign
              </span>
            </div>

            <p className="text-[11px] text-neutral-400 mb-3">
              Only users with <code className="text-amber-300 font-bold">ROLE_MANAGER</code> can assign
              technicians. Sets state to <code className="text-sky-300 font-bold">ASSIGNED</code> and
              emits event to Kafka topic <code className="text-amber-300">incident.assigned.v1</code>.
            </p>

            <div className="space-y-2">
              <label className="text-[10px] text-neutral-500 uppercase font-bold">
                Assignee Identity:
              </label>
              <div className="flex gap-1.5 flex-wrap">
                {['tech-A', 'tech-B', 'tech-C'].map((tech) => (
                  <button
                    key={tech}
                    type="button"
                    onClick={() => setAssigneeInput(tech)}
                    className={`px-2 py-1 rounded text-xs border ${
                      assigneeInput === tech
                        ? 'bg-indigo-950 text-indigo-300 border-indigo-600 font-bold'
                        : 'bg-neutral-900 text-neutral-400 border-neutral-700 hover:text-neutral-200'
                    }`}
                  >
                    {tech}
                  </button>
                ))}
              </div>

              <div className="flex gap-2 mt-2">
                <input
                  type="text"
                  value={assigneeInput}
                  onChange={(e) => setAssigneeInput(e.target.value)}
                  placeholder="Custom technician ID..."
                  className="flex-1 bg-neutral-900 border border-neutral-700 focus:border-amber-500 rounded p-1.5 text-neutral-200 text-xs focus:outline-none"
                />
                <button
                  type="button"
                  disabled={isSubmitting || !canAssign || !assigneeInput.trim()}
                  onClick={() => handleAssign(assigneeInput)}
                  className={`px-3 py-1.5 rounded font-bold transition-all ${
                    canAssign
                      ? 'bg-amber-500 hover:bg-amber-400 text-black shadow-sm'
                      : 'bg-neutral-800 text-neutral-500 cursor-not-allowed'
                  }`}
                >
                  {isSubmitting ? 'Dispatching...' : 'Assign & Notify'}
                </button>
              </div>
            </div>
          </div>

          {!canAssign && (
            <div className="text-[10px] text-neutral-500 bg-neutral-900/60 p-2 rounded border border-neutral-800 flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-amber-500 shrink-0" />
              <span>
                Switch active JWT to <strong>MANAGER (manager-1)</strong> above to test assignment.
              </span>
            </div>
          )}
        </div>

        {/* RIGHT COLUMN: STATE MACHINE PROGRESSION */}
        <div className="bg-[#0c0e12] border border-[#262c36] p-4 rounded flex flex-col justify-between space-y-3">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="font-bold text-neutral-200 uppercase text-[11px] flex items-center gap-1.5">
                <ArrowRight className="w-3.5 h-3.5 text-cyan-400" />
                State Machine Transition
              </span>
              <span className="text-[9px] px-1.5 py-0.2 rounded border bg-cyan-950/50 text-cyan-300 border-cyan-800 font-mono">
                PATCH /{incident.id}/status
              </span>
            </div>

            <p className="text-[11px] text-neutral-400 mb-3">
              Strict state machine enforcement:
              <span className="block text-neutral-500 text-[10px] mt-1">
                OPEN &rarr; ASSIGNED &rarr; IN_PROGRESS &rarr; RESOLVED &rarr; CLOSED
              </span>
            </p>

            {/* Allowed Transition Action */}
            {nextStatus ? (
              <div className="space-y-2">
                <div className="text-[10px] text-neutral-500 uppercase font-bold">
                  Next Valid State Transition:
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={incident.status} size="sm" />
                  <ArrowRight className="w-4 h-4 text-amber-400 animate-pulse" />
                  <StatusBadge status={nextStatus} size="sm" />
                </div>

                <div className="pt-2">
                  <button
                    type="button"
                    disabled={isSubmitting || !canProgressStatus}
                    onClick={() => handleProgressStatus(nextStatus)}
                    className={`w-full py-2 px-4 rounded font-bold transition-all flex items-center justify-center gap-2 ${
                      canProgressStatus
                        ? 'bg-cyan-500 hover:bg-cyan-400 text-black shadow-sm'
                        : 'bg-neutral-800 text-neutral-500 cursor-not-allowed'
                    }`}
                  >
                    <span>Progress Status to [{nextStatus}]</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-3 bg-neutral-900/60 rounded border border-neutral-800 text-center text-neutral-400">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 mx-auto mb-1" />
                <strong className="block text-neutral-200">Terminal State Reached</strong>
                <span className="text-[10px] text-neutral-500">
                  Incident is CLOSED. No further state transitions allowed by backend domain logic.
                </span>
              </div>
            )}
          </div>

          {!canProgressStatus && nextStatus && (
            <div className="text-[10px] text-neutral-500 bg-neutral-900/60 p-2 rounded border border-neutral-800 flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5 text-amber-500 shrink-0" />
              <span>
                Authorized only for <strong>{incident.assigneeId || 'assigned technician'}</strong>,{' '}
                <strong>{incident.reporterId}</strong>, or <strong>MANAGER</strong>.
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
