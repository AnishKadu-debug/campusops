import React, { useState, useEffect } from 'react';
import type { IncidentStatus } from '../../types';

interface SlaIndicatorProps {
  deadline: string;
  breachedAt: string | null;
  status: IncidentStatus;
  createdAt?: string;
}

export const SlaIndicator: React.FC<SlaIndicatorProps> = ({
  deadline,
  breachedAt,
  status,
  createdAt,
}) => {
  const [now, setNow] = useState<number>(Date.now());

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const deadlineMs = new Date(deadline).getTime();
  const isTerminal = status === 'RESOLVED' || status === 'CLOSED';
  const isBreached = Boolean(breachedAt) || (!isTerminal && now > deadlineMs);

  const formatRemaining = (diffMs: number) => {
    if (diffMs <= 0) return '00:00:00';
    const totalSecs = Math.floor(diffMs / 1000);
    const hours = Math.floor(totalSecs / 3600);
    const mins = Math.floor((totalSecs % 3600) / 60);
    const secs = totalSecs % 60;

    const pad = (n: number) => n.toString().padStart(2, '0');
    if (hours > 24) {
      const days = Math.floor(hours / 24);
      return `${days}d ${hours % 24}h ${pad(mins)}m`;
    }
    return `${pad(hours)}:${pad(mins)}:${pad(secs)}`;
  };

  const diffMs = deadlineMs - now;

  // Calculate percentage of time elapsed if createdAt is available
  let progressPct = 0;
  if (createdAt) {
    const startMs = new Date(createdAt).getTime();
    const totalDuration = deadlineMs - startMs;
    if (totalDuration > 0) {
      const elapsed = now - startMs;
      progressPct = Math.min(100, Math.max(0, Math.round((elapsed / totalDuration) * 100)));
    }
  }

  if (isTerminal) {
    return (
      <div className="flex items-center gap-2 font-mono text-xs text-neutral-400">
        <span className="inline-block w-2 h-2 rounded-full bg-neutral-600" />
        <span>SLA COMPLETED</span>
      </div>
    );
  }

  if (isBreached) {
    return (
      <div className="flex flex-col gap-1">
        <div className="inline-flex items-center gap-1.5 font-mono text-xs text-rose-400 font-bold bg-rose-950/40 border border-rose-600/50 px-2 py-0.5 rounded-sm">
          <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping" />
          <span>[ SLA BREACHED ]</span>
        </div>
        {breachedAt && (
          <span className="text-[10px] text-neutral-500 font-mono">
            Breach scanned: {new Date(breachedAt).toLocaleTimeString()}
          </span>
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between font-mono text-xs">
        <span className="text-neutral-400 flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          SLA ACTIVE:
        </span>
        <span className="text-emerald-400 font-bold tracking-wider">
          {formatRemaining(diffMs)}
        </span>
      </div>
      {createdAt && (
        <div className="w-full bg-neutral-800 h-1.5 rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-500 ${
              progressPct > 80 ? 'bg-amber-500' : 'bg-emerald-500'
            }`}
            style={{ width: `${progressPct}%` }}
          />
        </div>
      )}
    </div>
  );
};
