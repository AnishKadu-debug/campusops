import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { incidentService } from '../../services/incidentService';
import { assetService } from '../../services/assetService';
import type { Asset, IncidentPriority, ApiError } from '../../types';
import { PriorityBadge } from '../common/PriorityBadge';
import {
  PlusCircle,
  X,
  Shield,
  Layers,
  AlertCircle,
} from 'lucide-react';

interface CreateIncidentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (createdId: number) => void;
}

export const CreateIncidentModal: React.FC<CreateIncidentModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const { role, subject } = useAuth();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<IncidentPriority>('MEDIUM');
  const [assetId, setAssetId] = useState('');
  const [assets, setAssets] = useState<Asset[]>([]);
  const [loadingAssets, setLoadingAssets] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    if (isOpen) {
      setError(null);
      setLoadingAssets(true);
      assetService
        .getAll()
        .then((data) => setAssets(data))
        .catch((err) => console.warn('Failed to load assets list:', err))
        .finally(() => setLoadingAssets(false));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !description.trim()) {
      setError({
        status: 400,
        error: 'Validation Error',
        message: 'Title and description are required.',
      });
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      // Backend forces reporterId from JWT subject, but DTO includes reporterId field
      const created = await incidentService.create({
        title: title.trim(),
        description: description.trim(),
        priority,
        assetId: assetId.trim() || undefined,
        reporterId: subject || 'student-A',
      });
      onSuccess(created.id);
      onClose();
    } catch (err: unknown) {
      console.error('Failed to create incident:', err);
      setError(err as ApiError);
    } finally {
      setSubmitting(false);
    }
  };

  const isStudent = role === 'STUDENT';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-xs font-mono text-xs">
      <div className="bg-[#14171d] border border-[#262c36] w-full max-w-2xl rounded-md shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between p-4 border-b border-[#262c36] bg-[#0c0e12]">
          <div className="flex items-center gap-2">
            <div className="p-1 bg-amber-950/60 border border-amber-600/60 rounded text-amber-400">
              <PlusCircle className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-neutral-100 uppercase tracking-wider text-sm">
                Log New Operational Incident
              </h3>
              <span className="text-[10px] text-neutral-500 font-normal">
                POST /api/v1/incidents &bull; Gated to ROLE_STUDENT
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-neutral-500 hover:text-neutral-200 p-1 rounded hover:bg-neutral-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Form */}
        <form onSubmit={handleSubmit} className="p-5 overflow-y-auto space-y-4">
          {/* Role Check Warning if not STUDENT */}
          {!isStudent && (
            <div className="bg-amber-950/30 border border-amber-600/50 p-3 rounded text-amber-300 flex items-start gap-2">
              <Shield className="w-4 h-4 shrink-0 mt-0.5 text-amber-400" />
              <div>
                <strong className="block text-amber-200">Role Authorization Notice:</strong>
                Current active role is <code className="font-bold">{role || 'ANONYMOUS'}</code>. The
                backend Spring Security rule allows <strong>only STUDENT</strong> users to report
                incidents. Submitting with this token will trigger a backend <code className="text-amber-400">403 Forbidden</code> exception.
              </div>
            </div>
          )}

          {error && (
            <div className="bg-rose-950/40 border border-rose-800 p-3 rounded text-rose-300 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
              <div>
                <strong className="block text-rose-200">Backend Error ({error.status}):</strong>
                {error.message}
              </div>
            </div>
          )}

          {/* Reporter Identity (Immutable, forced from JWT) */}
          <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded">
            <label className="text-[10px] uppercase font-bold text-neutral-500 flex items-center justify-between mb-1">
              <span>Reporter Identity (Immutable)</span>
              <span className="text-cyan-400">JWT SUBJECT</span>
            </label>
            <div className="flex items-center justify-between text-neutral-200">
              <span className="font-bold text-cyan-300 text-sm">{subject || 'student-A'}</span>
              <span className="text-[10px] text-neutral-500">
                Extracted from <code className="text-neutral-400">SecurityContextHolder</code>
              </span>
            </div>
          </div>

          {/* Title */}
          <div>
            <label className="block text-neutral-400 mb-1 uppercase text-[11px] font-bold">
              Incident Title *
            </label>
            <input
              type="text"
              required
              maxLength={150}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Broken Projector in Lab 7, AC Failure in Room 302..."
              className="w-full bg-[#0c0e12] border border-[#262c36] focus:border-amber-500 rounded p-2.5 text-neutral-200 focus:outline-none"
            />
          </div>

          {/* Priority Selection */}
          <div>
            <label className="block text-neutral-400 mb-1.5 uppercase text-[11px] font-bold">
              Priority &amp; SLA Level *
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as IncidentPriority[]).map((p) => {
                const isSelected = priority === p;
                return (
                  <button
                    key={p}
                    type="button"
                    onClick={() => setPriority(p)}
                    className={`p-2 rounded border text-left flex flex-col justify-between transition-colors ${
                      isSelected
                        ? 'bg-neutral-800 border-amber-500 text-neutral-100 shadow-[0_0_8px_rgba(245,158,11,0.15)]'
                        : 'bg-[#0c0e12] border-[#262c36] text-neutral-400 hover:border-neutral-600'
                    }`}
                  >
                    <PriorityBadge priority={p} />
                    <span className="text-[10px] text-neutral-500 mt-1">
                      {p === 'CRITICAL' && '30 min window'}
                      {p === 'HIGH' && '4 hour window'}
                      {p === 'MEDIUM' && '12 hour window'}
                      {p === 'LOW' && '48 hour window'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Asset Selection (from real Asset Service) */}
          <div>
            <label className="block text-neutral-400 mb-1 uppercase text-[11px] font-bold flex items-center justify-between">
              <span>Attach Campus Asset (Optional)</span>
              <span className="text-[10px] text-neutral-500">Feign &rarr; Asset Service (:8083)</span>
            </label>
            <select
              value={assetId}
              onChange={(e) => setAssetId(e.target.value)}
              className="w-full bg-[#0c0e12] border border-[#262c36] focus:border-amber-500 rounded p-2.5 text-neutral-200 focus:outline-none"
            >
              <option value="">-- No Asset Attached --</option>
              {assets.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  {asset.name} ({asset.type} - {asset.location}) [ID: {asset.id}]
                </option>
              ))}
            </select>
            {loadingAssets && (
              <span className="text-[10px] text-neutral-500 mt-1 block">
                Loading campus asset registry via Asset Service...
              </span>
            )}
          </div>

          {/* Description */}
          <div>
            <label className="block text-neutral-400 mb-1 uppercase text-[11px] font-bold">
              Detailed Description *
            </label>
            <textarea
              required
              rows={3}
              maxLength={1000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe symptoms, equipment numbers, observed behavior..."
              className="w-full bg-[#0c0e12] border border-[#262c36] focus:border-amber-500 rounded p-2.5 text-neutral-200 focus:outline-none"
            />
          </div>

          {/* Real Backend Trace Preview */}
          <div className="bg-[#0c0e12] border border-[#262c36] p-3 rounded text-[11px] text-neutral-400">
            <div className="font-bold text-neutral-300 uppercase mb-1.5 flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-amber-400" />
              <span>Architectural Execution on Submit:</span>
            </div>
            <div className="space-y-1 text-[10px] text-neutral-500 font-mono">
              <div>&bull; Browser &rarr; POST /api/v1/incidents (Authorization: Bearer &lt;token&gt;)</div>
              <div>&bull; Spring Security validates JWT &rarr; forces reporterId = &quot;{subject}&quot;</div>
              {assetId && <div>&bull; OpenFeign calls Asset Service (:8083) with caller JWT to validate asset</div>}
              <div>&bull; PostgreSQL commits new row to campusops_incident</div>
              <div>&bull; AFTER_COMMIT publishing bridge emits to Kafka topic &quot;incident.created.v1&quot;</div>
              <div>&bull; Notification Service (:8084) consumes event &amp; writes notification entry</div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#262c36]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-neutral-800 hover:bg-neutral-700 text-neutral-300 rounded font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-amber-500 hover:bg-amber-400 text-black rounded font-bold flex items-center gap-1.5 shadow-sm"
            >
              {submitting ? 'Submitting to Backend...' : 'Dispatch Incident'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
