import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Terminal, Shield, Network, RefreshCw } from 'lucide-react';

interface HeaderProps {
  onRefresh?: () => void;
  isRefreshing?: boolean;
}

export const Header: React.FC<HeaderProps> = ({ onRefresh, isRefreshing }) => {
  const location = useLocation();
  const { role, subject } = useAuth();

  const navLinks = [
    { path: '/', label: 'Operations Board', icon: Terminal },
    { path: '/architecture', label: 'System Topology', icon: Network },
  ];

  return (
    <header className="bg-[#0c0e12] border-b border-[#262c36] sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-14">
          {/* Logo & Title */}
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2.5 text-neutral-100 font-mono group">
              <div className="w-7 h-7 bg-amber-500 text-black flex items-center justify-center font-black rounded-sm group-hover:bg-amber-400 transition-colors">
                CO
              </div>
              <div className="flex flex-col">
                <span className="font-bold text-sm tracking-wider flex items-center gap-1.5">
                  CAMPUS<span className="text-amber-400">OPS</span>
                  <span className="text-[10px] text-neutral-500 font-normal">v1.0 (CONTROL ROOM)</span>
                </span>
              </div>
            </Link>

            {/* Navigation Tabs */}
            <nav className="hidden md:flex items-center space-x-1">
              {navLinks.map((link) => {
                const Icon = link.icon;
                const isActive = location.pathname === link.path;
                return (
                  <Link
                    key={link.path}
                    to={link.path}
                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono transition-colors ${
                      isActive
                        ? 'bg-[#1a1e26] text-amber-300 border border-[#3b4452] font-semibold'
                        : 'text-neutral-400 hover:text-neutral-200 hover:bg-[#14171d]'
                    }`}
                  >
                    <Icon className="w-3.5 h-3.5" />
                    {link.label}
                  </Link>
                );
              })}
            </nav>
          </div>

          {/* Right Status / Auth Pill & Refresh */}
          <div className="flex items-center gap-3">
            {onRefresh && (
              <button
                onClick={onRefresh}
                disabled={isRefreshing}
                className="p-1.5 bg-[#14171d] hover:bg-[#1a1e26] text-neutral-400 hover:text-neutral-200 border border-[#262c36] rounded font-mono text-xs flex items-center gap-1 transition-colors"
                title="Refresh backend data"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-amber-400' : ''}`} />
                <span className="hidden sm:inline">Sync</span>
              </button>
            )}

            <div className="flex items-center gap-2 bg-[#14171d] border border-[#262c36] px-2.5 py-1 rounded text-xs font-mono">
              <Shield className="w-3.5 h-3.5 text-amber-400" />
              <span className="text-neutral-400 hidden sm:inline">ROLE:</span>
              <span className="font-bold text-neutral-200">{role || 'GUEST'}</span>
              <span className="text-neutral-600">|</span>
              <span className="text-cyan-300 truncate max-w-[90px]">{subject || 'anon'}</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
