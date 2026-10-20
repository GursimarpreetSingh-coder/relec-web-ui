import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import type { Worker, WorkerStatus } from '../../types/models';
import {
  Users,
  Plus,
  Phone,
  Clock,
  Briefcase,
  DollarSign,
  ShieldCheck,
  CheckCircle,
  AlertCircle,
  X,
  Trash2,
  Calendar
} from 'lucide-react';

export const WorkersScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();

  const workers = useLiveQuery(() => db.workers.toArray()) || [];
  const attendance = useLiveQuery(() => db.attendance.toArray()) || [];

  const [showAddModal, setShowAddModal] = useState(false);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [salary, setSalary] = useState('18000');
  const [designation, setDesignation] = useState('Electrician');

  const handleAddWorker = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !phone.trim()) return;

    await db.workers.put({
      id: 'W_' + Date.now().toString(36),
      name: name.trim(),
      phone: phone.trim(),
      salary: Number(salary) || 15000,
      designation,
      status: 'IDLE',
      joiningDate: new Date().toISOString().split('T')[0],
      isVerified: true,
      isApproved: true,
      isActive: true
    });

    setShowAddModal(false);
    setName('');
    setPhone('');
  };

  const handleToggleStatus = async (worker: Worker, newStatus: WorkerStatus) => {
    await db.workers.update(worker.id, { status: newStatus });
  };

  const handleDeleteWorker = async (id: string, wName: string) => {
    if (confirm(`Remove technician profile for ${wName}?`)) {
      await db.workers.delete(id);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Field Technicians & Staff</h2>
          <p className="text-xs text-slate-500">
            {workers.length} active electricians, senior technicians, and helpers
          </p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all self-start sm:self-auto"
          style={{ backgroundColor: accentColor }}
        >
          <Plus className="w-4 h-4" />
          <span>Add Electrician</span>
        </button>
      </div>

      {/* Workers Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {workers.map(w => {
          const workerAttendance = attendance.filter(a => a.workerId === w.id);
          const latestPunch = workerAttendance[workerAttendance.length - 1];

          return (
            <div
              key={w.id}
              className={`p-5 rounded-3xl border shadow-lg transition-all flex flex-col justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
            >
              <div>
                {/* Header Row */}
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-11 h-11 rounded-2xl flex items-center justify-center font-black text-sm text-white shadow-md"
                      style={{ backgroundColor: accentColor }}
                    >
                      {w.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="font-extrabold text-sm text-slate-100 leading-snug">{w.name}</h3>
                      <p className="text-xs text-slate-400 font-semibold">{w.designation}</p>
                    </div>
                  </div>

                  <button
                    onClick={() => handleDeleteWorker(w.id, w.name)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-red-400"
                    title="Delete Worker"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Status Indicator Pill */}
                <div className="mt-4 flex items-center justify-between">
                  <span className="text-[11px] font-bold text-slate-500">Current Status:</span>
                  <div className="flex gap-1 text-[10px] font-black uppercase">
                    {(['WORKING', 'ASSIGNED_NOT_STARTED', 'IDLE'] as WorkerStatus[]).map(st => {
                      const isSelected = w.status === st;
                      const color =
                        st === 'WORKING' ? 'bg-emerald-500 text-white' :
                        st === 'ASSIGNED_NOT_STARTED' ? 'bg-amber-500 text-white' :
                        'bg-red-500 text-white';
                      return (
                        <button
                          key={st}
                          onClick={() => handleToggleStatus(w, st)}
                          className={`px-2 py-1 rounded-lg border transition-all ${isSelected ? `${color} border-transparent shadow-sm` : 'border-slate-800 text-slate-500 hover:border-slate-700'}`}
                        >
                          {st === 'WORKING' ? 'Working' : st === 'ASSIGNED_NOT_STARTED' ? 'Assigned' : 'Idle'}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Details */}
                <div className="mt-4 space-y-2 text-xs text-slate-400">
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 text-slate-500">
                      <Phone className="w-3.5 h-3.5" /> Mobile
                    </span>
                    <span className="font-mono text-slate-200 font-semibold">{w.phone}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 text-slate-500">
                      <DollarSign className="w-3.5 h-3.5" /> Salary
                    </span>
                    <span className="font-bold text-emerald-400">₹{w.salary.toLocaleString()}/mo</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 text-slate-500">
                      <Calendar className="w-3.5 h-3.5" /> Joined
                    </span>
                    <span>{w.joiningDate}</span>
                  </div>
                </div>

                {/* Punch Clock Status */}
                <div className="mt-4 p-2.5 rounded-2xl bg-slate-950/60 border border-slate-800/80 text-[11px] flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-slate-400">
                    <Clock className="w-3.5 h-3.5 text-slate-500" />
                    <span>
                      {latestPunch ? (latestPunch.isClockedIn ? `In at ${latestPunch.clockInTime}` : `Out at ${latestPunch.clockOutTime}`) : 'No punch records today'}
                    </span>
                  </div>
                  <span className={`px-2 py-0.5 rounded-md font-bold uppercase text-[9px] ${latestPunch?.isClockedIn ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-800 text-slate-400'}`}>
                    {latestPunch?.isClockedIn ? 'On Clock' : 'Off Clock'}
                  </span>
                </div>
              </div>

              {/* Action: Call worker */}
              <div className="mt-4 pt-3 border-t border-slate-800">
                <a
                  href={`tel:${w.phone}`}
                  className="w-full py-2 rounded-xl text-xs font-bold bg-slate-800 hover:bg-slate-700 text-slate-200 flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Phone className="w-3.5 h-3.5 text-blue-400" />
                  <span>Call Technician (+91 {w.phone})</span>
                </a>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add Worker Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-md rounded-3xl border shadow-2xl p-6 ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-base">Add New Field Electrician</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddWorker} className="mt-4 space-y-3.5">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Full Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Jaswinder Singh"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Mobile Number
                </label>
                <input
                  type="tel"
                  required
                  maxLength={10}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="9821034455"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Designation
                </label>
                <select
                  value={designation}
                  onChange={(e) => setDesignation(e.target.value)}
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                >
                  <option value="Senior Electrician">Senior Electrician</option>
                  <option value="Electrician">Electrician</option>
                  <option value="Helper Technician">Helper Technician</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Monthly Salary (₹)
                </label>
                <input
                  type="number"
                  value={salary}
                  onChange={(e) => setSalary(e.target.value)}
                  placeholder="18000"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="w-1/2 py-2.5 rounded-xl border border-slate-700 text-xs font-bold text-slate-400"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="w-1/2 py-2.5 rounded-xl text-xs font-bold text-white shadow-md"
                  style={{ backgroundColor: accentColor }}
                >
                  Add Technician
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
