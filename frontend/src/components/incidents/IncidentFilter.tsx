import React from 'react';
import type { IncidentStatus } from '../../types';
import { Filter, Search } from 'lucide-react';

interface IncidentFilterProps {
  selectedStatus: IncidentStatus | 'ALL';
  onSelectStatus: (status: IncidentStatus | 'ALL') => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  counts: Record<string, number>;
}

export const IncidentFilter: React.FC<IncidentFilterProps> = ({
  selectedStatus,
  onSelectStatus,
  searchQuery,
  onSearchChange,
  counts,
}) => {
  const statuses: Array<{ key: IncidentStatus | 'ALL'; label: string }> = [
    { key: 'ALL', label: 'All Incidents' },
    { key: 'OPEN', label: 'Open' },
    { key: 'ASSIGNED', label: 'Assigned' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'RESOLVED', label: 'Resolved' },
    { key: 'CLOSED', label: 'Closed' },
  ];

  return (
    <div className="flex flex-wrap items-center justify-between gap-4 mb-6 bg-[#14171d] p-3 rounded border border-[#262c36]">
      {/* Status Filter Buttons */}
      <div className="flex flex-wrap items-center gap-1.5">
        <span className="text-neutral-500 text-xs font-mono mr-1 hidden sm:inline flex items-center gap-1">
          <Filter className="w-3.5 h-3.5" /> Filter:
        </span>
        {statuses.map((item) => {
          const isActive = selectedStatus === item.key;
          const count = counts[item.key] ?? 0;

          return (
            <button
              key={item.key}
              onClick={() => onSelectStatus(item.key)}
              className={`font-mono text-xs px-2.5 py-1 rounded border transition-colors flex items-center gap-1.5 ${
                isActive
                  ? 'bg-amber-500 text-black border-amber-400 font-bold'
                  : 'bg-[#0c0e12] text-neutral-400 border-[#262c36] hover:border-neutral-600 hover:text-neutral-200'
              }`}
            >
              <span>{item.label}</span>
              <span
                className={`text-[10px] px-1 py-0.2 rounded font-mono ${
                  isActive ? 'bg-black/30 text-black' : 'bg-neutral-800 text-neutral-400'
                }`}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Search Input */}
      <div className="relative min-w-[200px] flex-1 max-w-xs">
        <Search className="w-3.5 h-3.5 text-neutral-500 absolute left-2.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Filter by title, asset, id..."
          className="w-full bg-[#0c0e12] border border-[#262c36] focus:border-amber-500/80 rounded pl-8 pr-3 py-1 text-xs font-mono text-neutral-200 placeholder:text-neutral-600 focus:outline-none"
        />
      </div>
    </div>
  );
};
