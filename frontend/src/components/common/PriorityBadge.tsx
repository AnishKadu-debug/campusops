import React from 'react';
import type { IncidentPriority } from '../../types';

interface PriorityBadgeProps {
  priority: IncidentPriority;
  showSlaDuration?: boolean;
}

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({
  priority,
  showSlaDuration = false,
}) => {
  const getStyle = (p: IncidentPriority) => {
    switch (p) {
      case 'CRITICAL':
        return {
          container: 'bg-rose-950/40 text-rose-300 border-rose-600/60 shadow-[0_0_12px_rgba(244,63,94,0.15)]',
          dot: 'bg-rose-500 animate-pulse',
          sla: '30M SLA',
        };
      case 'HIGH':
        return {
          container: 'bg-orange-950/40 text-orange-300 border-orange-600/60 shadow-[0_0_10px_rgba(249,115,22,0.12)]',
          dot: 'bg-orange-500',
          sla: '4H SLA',
        };
      case 'MEDIUM':
        return {
          container: 'bg-amber-950/30 text-amber-300 border-amber-600/40',
          dot: 'bg-amber-500',
          sla: '12H SLA',
        };
      case 'LOW':
        return {
          container: 'bg-slate-900 text-slate-300 border-slate-700',
          dot: 'bg-slate-500',
          sla: '48H SLA',
        };
    }
  };

  const style = getStyle(priority);

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-mono text-xs px-2.5 py-0.5 border rounded-sm ${style.container}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${style.dot}`} />
      <span className="font-bold tracking-wider">{priority}</span>
      {showSlaDuration && (
        <span className="text-[10px] opacity-75 font-normal ml-1 pl-1 border-l border-current/30">
          {style.sla}
        </span>
      )}
    </span>
  );
};
