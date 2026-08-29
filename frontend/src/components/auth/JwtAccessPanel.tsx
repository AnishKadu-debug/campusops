import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import type { UserRole } from '../../types';
import { Key, Shield, User, Clock, CheckCircle2, AlertCircle, Terminal } from 'lucide-react';

export const JwtAccessPanel: React.FC = () => {
  const { token, decoded, role, subject, setToken, quickSwitch, isAuthenticated } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [customToken, setCustomToken] = useState(token);
  const [showCliHelper, setShowCliHelper] = useState(false);

  const handleApplyCustom = (e: React.FormEvent) => {
    e.preventDefault();
    setToken(customToken);
    setIsEditing(false);
  };

  const handlePresetSwitch = async (targetRole: UserRole, targetSub: string) => {
    await quickSwitch(targetRole, targetSub);
    setCustomToken('');
    setIsEditing(false);
  };

  const formatExpiry = (expSec?: number) => {
    if (!expSec) return 'No expiration claim';
    const expDate = new Date(expSec * 1000);
    const nowSec = Math.floor(Date.now() / 1000);
    const remainingSecs = expSec - nowSec;

    if (remainingSecs <= 0) {
      return `EXPIRED at ${expDate.toLocaleTimeString()}`;
    }

    const hours = Math.floor(remainingSecs / 3600);
    const mins = Math.floor((remainingSecs % 3600) / 60);
    return `Valid for ${hours}h ${mins}m (until ${expDate.toLocaleTimeString()})`;
  };

  return (
    <div className="bg-[#14171d] border border-[#262c36] rounded-md p-4 mb-6 shadow-sm font-mono text-xs">
      {/* Top bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-[#262c36]">
        <div className="flex items-center gap-2">
          <div className="p-1.5 bg-neutral-800 border border-neutral-700 rounded text-neutral-300">
            <Key className="w-4 h-4 text-amber-400" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs uppercase tracking-widest text-neutral-300 font-bold">
                DevToken / Access Panel
              </span>
              <span
                className={`inline-flex items-center gap-1 font-mono text-[10px] px-1.5 py-0.2 rounded border ${
                  isAuthenticated
                    ? 'bg-emerald-950/40 text-emerald-400 border-emerald-800/60'
                    : 'bg-rose-950/40 text-rose-400 border-rose-800/60'
                }`}
              >
                {isAuthenticated ? (
                  <>
                    <CheckCircle2 className="w-3 h-3" /> TOKEN ACTIVE
                  </>
                ) : (
                  <>
                    <AlertCircle className="w-3 h-3" /> NO VALID JWT
                  </>
                )}
              </span>
            </div>
            <span className="text-[10px] text-neutral-500">
              Authoritative generator: <code className="text-neutral-400">DevTokenGenerator.java</code>
            </span>
          </div>
        </div>

        {/* 1-Click Role Switcher */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="text-[11px] text-neutral-500 mr-1 hidden sm:inline">
            Dev Roles:
          </span>

          <button
            onClick={() => handlePresetSwitch('STUDENT', 'student-A')}
            className={`font-mono text-xs px-2.5 py-1 rounded border transition-all ${
              role === 'STUDENT'
                ? 'bg-cyan-950/60 text-cyan-300 border-cyan-500/80 shadow-[0_0_8px_rgba(6,182,212,0.2)] font-bold'
                : 'bg-neutral-900 text-neutral-400 border-neutral-700 hover:border-neutral-500 hover:text-neutral-200'
            }`}
          >
            STUDENT (student-A)
          </button>

          <button
            onClick={() => handlePresetSwitch('TECHNICIAN', 'tech-A')}
            className={`font-mono text-xs px-2.5 py-1 rounded border transition-all ${
              role === 'TECHNICIAN'
                ? 'bg-indigo-950/60 text-indigo-300 border-indigo-500/80 shadow-[0_0_8px_rgba(99,102,241,0.2)] font-bold'
                : 'bg-neutral-900 text-neutral-400 border-neutral-700 hover:border-neutral-500 hover:text-neutral-200'
            }`}
          >
            TECHNICIAN (tech-A)
          </button>

          <button
            onClick={() => handlePresetSwitch('MANAGER', 'manager-1')}
            className={`font-mono text-xs px-2.5 py-1 rounded border transition-all ${
              role === 'MANAGER'
                ? 'bg-amber-950/60 text-amber-300 border-amber-500/80 shadow-[0_0_8px_rgba(245,158,11,0.2)] font-bold'
                : 'bg-neutral-900 text-neutral-400 border-neutral-700 hover:border-neutral-500 hover:text-neutral-200'
            }`}
          >
            MANAGER (manager-1)
          </button>

          <button
            onClick={() => {
              setCustomToken(token);
              setIsEditing(!isEditing);
            }}
            className="font-mono text-xs px-2 py-1 bg-neutral-800 hover:bg-neutral-700 text-neutral-300 border border-neutral-600 rounded"
            title="Paste custom DevToken"
          >
            {isEditing ? 'Close' : 'Paste Token'}
          </button>

          <button
            onClick={() => setShowCliHelper(!showCliHelper)}
            className="font-mono text-xs p-1 bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-amber-300 border border-neutral-600 rounded"
            title="Show backend CLI command"
          >
            <Terminal className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Backend DevTokenGenerator CLI Guide */}
      {showCliHelper && (
        <div className="mt-3 pt-3 border-t border-[#262c36] bg-[#0c0e12] p-3 rounded text-[11px] text-neutral-300">
          <div className="text-amber-400 font-bold mb-1 uppercase flex items-center gap-1.5">
            <Terminal className="w-3.5 h-3.5" /> Authoritative Backend DevTokenGenerator Command:
          </div>
          <p className="text-neutral-400 text-[10px] mb-2">
            To generate signed tokens directly from the authoritative backend Java utility:
          </p>
          <pre className="bg-black/60 p-2 rounded border border-neutral-800 text-amber-300 text-[10px] overflow-x-auto select-all">
{`cd incident-service
.\\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\\cp.txt"
$cp = "target\\classes;$((Get-Content target\\cp.txt -Raw).Trim())"
java -cp $cp com.campusops.incident.security.dev.DevTokenGenerator <STUDENT|TECHNICIAN|MANAGER> [subject]`}
          </pre>
        </div>
      )}

      {/* Manual token paste drawer */}
      {isEditing && (
        <form onSubmit={handleApplyCustom} className="mt-3 pt-3 border-t border-[#262c36]">
          <div className="text-xs text-neutral-400 mb-1.5 font-mono">
            Paste JWT token string minted by <code className="text-amber-300">DevTokenGenerator</code>:
          </div>
          <div className="flex gap-2">
            <textarea
              rows={2}
              value={customToken}
              onChange={(e) => setCustomToken(e.target.value)}
              placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
              className="w-full bg-[#0c0e12] border border-[#3b4452] rounded p-2 text-xs font-mono text-neutral-200 focus:outline-none focus:border-amber-500"
            />
            <button
              type="submit"
              className="bg-amber-500 hover:bg-amber-400 text-black font-mono font-bold text-xs px-4 py-2 rounded self-end shrink-0"
            >
              Apply Token
            </button>
          </div>
        </form>
      )}

      {/* Decoded Claims */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mt-3 pt-1">
        <div className="bg-[#0c0e12] border border-[#262c36] p-2.5 rounded">
          <div className="flex items-center gap-1.5 text-neutral-500 text-[10px] uppercase">
            <User className="w-3 h-3 text-cyan-400" />
            JWT Subject (sub)
          </div>
          <div className="text-sm font-bold text-neutral-200 truncate mt-0.5">
            {subject || 'None'}
          </div>
        </div>

        <div className="bg-[#0c0e12] border border-[#262c36] p-2.5 rounded">
          <div className="flex items-center gap-1.5 text-neutral-500 text-[10px] uppercase">
            <Shield className="w-3 h-3 text-indigo-400" />
            Role Claim
          </div>
          <div className="text-sm font-bold text-neutral-200 truncate mt-0.5">
            {role || 'None'}
          </div>
        </div>

        <div className="bg-[#0c0e12] border border-[#262c36] p-2.5 rounded md:col-span-2">
          <div className="flex items-center gap-1.5 text-neutral-500 text-[10px] uppercase">
            <Clock className="w-3 h-3 text-amber-400" />
            TTL &amp; Expiration (exp)
          </div>
          <div className="text-xs text-neutral-300 truncate mt-0.5">
            {formatExpiry(decoded?.exp)}
          </div>
        </div>
      </div>

      {/* Security Architecture Notice */}
      <div className="mt-3 flex items-center justify-between text-[11px] text-neutral-500 bg-[#0c0e12]/60 px-3 py-1.5 rounded border border-[#262c36]/60">
        <div className="flex items-center gap-2">
          <span className="text-amber-500/80 font-bold">SECURITY BOUNDARY:</span>
          <span>
            <strong className="text-neutral-400">Frontend role visibility = UX guidance</strong> |{' '}
            <strong className="text-neutral-400">Backend Spring Security = Authoritative authorization</strong>
          </span>
        </div>
        <span className="text-[10px] text-neutral-600 hidden lg:inline">
          Authorization: Bearer &lt;token&gt; attached to all API requests
        </span>
      </div>
    </div>
  );
};
