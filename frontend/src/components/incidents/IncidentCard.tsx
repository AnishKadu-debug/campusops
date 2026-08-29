import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { Incident } from '../../types';
import { StatusBadge } from '../common/StatusBadge';
import { PriorityBadge } from '../common/PriorityBadge';
import { SlaIndicator } from '../common/SlaIndicator';
import { User, Wrench, Box, Clock, ChevronRight } from 'lucide-react';

interface IncidentCardProps {
  incident: Incident;
}

export const IncidentCard: React.FC<IncidentCardProps> = ({ incident }) => {
  const navigate = useNavigate();

  const timeAgo = (dateStr: string) => {
    const secs = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
    if (secs < 60) return `${secs}s ago`;
    const mins = Math.floor(secs / 60);
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  return (
    <div
      onClick={() => navigate(`/incidents/${incident.id}`)}
      className="bg-[#14171d] hover:bg-[#1a1e26] border border-[#262c36] hover:border-[#3b4452] transition-colors rounded p-4 cursor-pointer flex flex-col justify-between group shadow-sm"
    >
      <div>
        {/* Header: ID, Priority, Status */}
        <div className="flex items-center justify-between gap-2 mb-2 pb-2 border-b border-[#262c36]">
          <div className="flex items-center gap-2">
            <span className="font-mono text-xs font-bold text-neutral-400 bg-neutral-900 border border-neutral-700 px-1.5 py-0.5 rounded">
              #{incident.id}
            </span>
            <PriorityBadge priority={incident.priority} />
          </div>
          <StatusBadge status={incident.status} size="sm" />
        </div>

        {/* Title & Description */}
        <h4 className="font-semibold text-neutral-100 text-sm group-hover:text-amber-300 transition-colors line-clamp-1 mb-1">
          {incident.title}
        </h4>
        <p className="text-neutral-400 text-xs line-clamp-2 mb-3">
          {incident.description}
        </p>

        {/* Technical Metadata */}
        <div className="grid grid-cols-2 gap-2 text-[11px] font-mono text-neutral-400 bg-[#0c0e12] p-2.5 rounded border border-[#262c36] mb-3">
          <div className="flex items-center gap-1.5 truncate">
            <User className="w-3 h-3 text-cyan-400 shrink-0" />
            <span className="text-neutral-500">Rep:</span>
            <span className="text-neutral-200 font-bold truncate">{incident.reporterId}</span>
          </div>

          <div className="flex items-center gap-1.5 truncate">
            <Wrench className="w-3 h-3 text-indigo-400 shrink-0" />
            <span className="text-neutral-500">Assigned:</span>
            <span className="text-neutral-300 truncate">
              {incident.assigneeId || <span className="text-neutral-600 italic">Unassigned</span>}
            </span>
          </div>

          {incident.assetId ? (
            <div className="flex items-center gap-1.5 col-span-2 truncate">
              <Box className="w-3 h-3 text-amber-400 shrink-0" />
              <span className="text-neutral-500">Asset:</span>
              <span className="text-cyan-300 truncate">{incident.assetId}</span>
            </div>
          ) : (
            <div className="flex items-center gap-1.5 col-span-2 text-neutral-600">
              <Box className="w-3 h-3 text-neutral-700 shrink-0" />
              <span>No asset attached</span>
            </div>
          )}
        </div>
      </div>

      {/* Footer: SLA & Age */}
      <div className="pt-2 border-t border-[#262c36]">
        <div className="mb-2">
          <SlaIndicator
            deadline={incident.slaDeadline}
            breachedAt={incident.slaBreachedAt}
            status={incident.status}
            createdAt={incident.createdAt}
          />
        </div>
        <div className="flex items-center justify-between text-[10px] font-mono text-neutral-500">
          <span className="flex items-center gap-1">
            <Clock className="w-3 h-3" />
            Created {timeAgo(incident.createdAt)}
          </span>
          <span className="text-amber-400/80 group-hover:text-amber-300 flex items-center font-bold">
            Inspect <ChevronRight className="w-3 h-3 ml-0.5" />
          </span>
        </div>
      </div>
    </div>
  );
};
