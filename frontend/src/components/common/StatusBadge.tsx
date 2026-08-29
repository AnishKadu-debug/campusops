import React from 'react';
import type { IncidentStatus } from '../../types';

interface StatusBadgeProps {
  status: IncidentStatus;
  size?: 'sm' | 'md' | 'lg';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const getStyle = (s: IncidentStatus) => {
    switch (s) {
      case 'OPEN':
        return 'bg-amber-950/40 text-amber-300 border-amber-600/50 shadow-[0_0_10px_rgba(245,158,11,0.1)]';
      case 'ASSIGNED':
        return 'bg-sky-950/40 text-sky-300 border-sky-600/50 shadow-[0_0_10px_rgba(56,189,248,0.1)]';
      case 'IN_PROGRESS':
        return 'bg-indigo-950/40 text-indigo-300 border-indigo-500/50 shadow-[0_0_10px_rgba(99,102,241,0.1)]';
      case 'RESOLVED':
        return 'bg-emerald-950/40 text-emerald-300 border-emerald-500/50 shadow-[0_0_10px_rgba(16,185,129,0.1)]';
      case 'CLOSED':
        return 'bg-neutral-900 text-neutral-400 border-neutral-700';
      default:
        return 'bg-neutral-800 text-neutral-300 border-neutral-700';
    }
  };

  const sizeClasses = {
    sm: 'text-[10px] px-1.5 py-0.5 tracking-wider',
    md: 'text-xs px-2.5 py-1 tracking-widest',
    lg: 'text-sm px-3.5 py-1.5 tracking-widest',
  };

  return (
    <span
      className={`inline-flex items-center font-mono font-bold uppercase border rounded-sm ${getStyle(
        status
      )} ${sizeClasses[size]}`}
    >
      <span className="opacity-70 mr-1">[</span>
      {status.replace('_', ' ')}
      <span className="opacity-70 ml-1">]</span>
    </span>
  );
};
