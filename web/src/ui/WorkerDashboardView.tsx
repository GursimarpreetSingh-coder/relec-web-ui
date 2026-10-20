import React, { useState } from 'react';
import { useTheme } from './theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../data/database';
import { authService } from '../data/authService';
import type { ServiceRequest } from '../types/models';
import {
  Wrench,
  Clock,
  CheckCircle,
  MapPin,
  Phone,
  AlertTriangle,
  User,
  LogOut,
  Sun,
  Moon,
  Calendar,
  X
} from 'lucide-react';

interface WorkerDashboardViewProps {
  onLogout: () => void;
}

export const WorkerDashboardView: React.FC<WorkerDashboardViewProps> = ({ onLogout }) => {
  const { accentColor, isDarkTheme, toggleDarkTheme, brandName, logoSrc } = useTheme();

  const currentSession = authService.getCurrentSession();
  const services = useLiveQuery(() => db.services.toArray()) || [];
  const attendance = useLiveQuery(() => db.attendance.toArray()) || [];

  const [selectedTab, setSelectedTab] = useState<number>(0); // 0: My Jobs, 1: Available Unassigned
  const [jobToComplete, setJobToComplete] = useState<ServiceRequest | null>(null);
  const [completionNotes, setCompletionNotes] = useState('');

  // Target technician W1 Sukhvinder Singh or current session
  const techId = 'W1';
  const techName = currentSession?.displayName || 'Sukhvinder Singh';

  const today = new Date().toISOString().split('T')[0];
  const todayPunch = attendance.find(a => a.workerId === techId && a.date === today);
  const isClockedIn = Boolean(todayPunch && !todayPunch.clockOutTime);

  // Filter jobs
  const myAssignedJobs = services.filter(s => s.assignedWorkerId === techId || s.assignedWorkerName === techName);
  const unassignedJobs = services.filter(s => (!s.assignedWorkerId || !s.assignedWorkerName) && s.status === 'CREATED');

  // Punch clock action
  const handleTogglePunch = async () => {
    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    if (isClockedIn && todayPunch) {
      // Clock Out
      await db.attendance.update(todayPunch.id, {
        clockOutTime: timeStr,
        isClockedIn: false
      });
    } else {
      // Clock In
      await db.attendance.put({
        id: 'ATT_' + Date.now().toString(36),
        workerId: techId,
        workerName: techName,
        clockInTime: timeStr,
        clockOutTime: null,
        date: today,
        isClockedIn: true
      });
    }
  };

  // Stage updates
  const handleStartJob = async (job: ServiceRequest) => {
    const history = [...job.statusHistory];
    history.push({
      status: 'IN_PROGRESS',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      notes: `${techName} started working on site`
    });

    await db.services.update(job.id, {
      status: 'IN_PROGRESS',
      statusHistory: history
    });
  };

  const handleCompleteJob = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!jobToComplete) return;

    const history = [...jobToComplete.statusHistory];
    history.push({
      status: 'COMPLETED',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      notes: completionNotes.trim() || 'Work completed successfully by field technician'
    });

    await db.services.update(jobToComplete.id, {
      status: 'COMPLETED',
      statusHistory: history
    });

    setJobToComplete(null);
    setCompletionNotes('');
  };

  const handleClaimJob = async (job: ServiceRequest) => {
    const history = [...job.statusHistory];
    history.push({
      status: 'ASSIGNED',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      notes: `Claimed by ${techName}`
    });

    await db.services.update(job.id, {
      assignedWorkerId: techId,
      assignedWorkerName: techName,
      status: 'ASSIGNED',
      statusHistory: history
    });
  };

  return (
    <div className={`min-h-screen flex flex-col transition-colors duration-300 ${isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'}`}>
      {/* Worker App Bar */}
      <header className={`sticky top-0 z-40 px-4 sm:px-6 py-3 border-b backdrop-blur-xl flex items-center justify-between transition-colors ${isDarkTheme ? 'bg-slate-950/85 border-slate-800/80 shadow-md' : 'bg-white/90 border-slate-200 shadow-sm'}`}>
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl border border-slate-700 overflow-hidden shrink-0">
            <img src={logoSrc} alt="Logo" className="w-full h-full object-cover" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-black text-sm tracking-tight">{brandName} FIELD WORKER</span>
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: accentColor }} />
            </div>
            <p className="text-[10px] text-slate-500 font-semibold">{techName} (Senior Electrician)</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={toggleDarkTheme}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-blue-400' : 'bg-slate-100 border-slate-200 text-blue-600'}`}
          >
            {isDarkTheme ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
          <button
            onClick={onLogout}
            className="p-2 rounded-xl border border-red-500/20 bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-all"
            title="Sign Out"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 sm:p-6 space-y-6 pb-20">
        {/* Attendance Punch Clock Card */}
        <div className={`p-6 rounded-3xl border shadow-xl flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div className="flex items-center gap-4">
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center text-white shadow-md"
              style={{ backgroundColor: accentColor }}
            >
              <Wrench className="w-7 h-7" />
            </div>
            <div>
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
                Field Technician Identity
              </span>
              <h2 className="text-xl font-black text-slate-100">{techName}</h2>
              <div className="flex items-center gap-2 mt-1 text-xs text-slate-400">
                <Clock className="w-3.5 h-3.5 text-slate-500" />
                <span>
                  {todayPunch
                    ? (isClockedIn ? `Clocked in at ${todayPunch.clockInTime}` : `Clocked out at ${todayPunch.clockOutTime}`)
                    : 'Not clocked in yet today'}
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={handleTogglePunch}
            className={`px-6 py-3 rounded-2xl font-black text-xs text-white tracking-wider shadow-lg transition-all transform hover:scale-105 active:scale-95 flex items-center justify-center gap-2 ${isClockedIn ? 'bg-red-600 hover:bg-red-500 shadow-red-600/30' : 'bg-emerald-600 hover:bg-emerald-500 shadow-emerald-600/30'}`}
          >
            <Clock className="w-4 h-4" />
            <span>{isClockedIn ? 'PUNCH CLOCK OUT' : 'PUNCH CLOCK IN'}</span>
          </button>
        </div>

        {/* Tab Toggle: My Jobs vs Available */}
        <div className={`grid grid-cols-2 p-1 rounded-2xl text-xs font-bold border ${isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-slate-100 border-slate-200'}`}>
          <button
            onClick={() => setSelectedTab(0)}
            className={`py-2.5 rounded-xl transition-all flex items-center justify-center gap-2 ${selectedTab === 0 ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-200'}`}
          >
            <Wrench className="w-4 h-4" />
            <span>My Assigned Jobs ({myAssignedJobs.length})</span>
          </button>
          <button
            onClick={() => setSelectedTab(1)}
            className={`py-2.5 rounded-xl transition-all flex items-center justify-center gap-2 ${selectedTab === 1 ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-200'}`}
          >
            <AlertTriangle className="w-4 h-4" />
            <span>Available Unassigned ({unassignedJobs.length})</span>
          </button>
        </div>

        {/* TAB 0: MY ASSIGNED JOBS */}
        {selectedTab === 0 && (
          <div className="space-y-4">
            {myAssignedJobs.length === 0 ? (
              <div className="py-16 text-center text-xs text-slate-500">
                You currently have no jobs assigned. Switch to "Available Unassigned" to claim incoming repair requests.
              </div>
            ) : (
              myAssignedJobs.map(job => (
                <div
                  key={job.id}
                  className={`p-5 rounded-3xl border shadow-lg space-y-4 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`px-2.5 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${
                          job.priority === 'EMERGENCY' ? 'bg-red-500/20 text-red-400 animate-pulse' :
                          job.priority === 'HIGH' ? 'bg-amber-500/20 text-amber-400' :
                          'bg-blue-500/20 text-blue-400'
                        }`}>
                          {job.priority} PRIORITY
                        </span>
                        <span className="text-xs text-slate-500 font-bold">· {job.category}</span>
                      </div>
                      <h3 className="text-base font-extrabold text-slate-100 mt-1">{job.problemDescription}</h3>
                    </div>

                    <span className={`px-3 py-1 rounded-xl text-xs font-black uppercase tracking-wider ${
                      job.status === 'COMPLETED' ? 'bg-emerald-500/20 text-emerald-400' :
                      job.status === 'IN_PROGRESS' ? 'bg-blue-500/20 text-blue-400' :
                      'bg-amber-500/20 text-amber-400'
                    }`}>
                      {job.status.replace('_', ' ')}
                    </span>
                  </div>

                  {/* Customer Information */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-400 bg-slate-950/60 p-3 rounded-2xl border border-slate-800/80">
                    <div className="flex items-center gap-2">
                      <User className="w-3.5 h-3.5 text-slate-500" />
                      <span className="font-bold text-slate-200">{job.customerName}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Phone className="w-3.5 h-3.5 text-slate-500" />
                      <a href={`tel:${job.customerPhone}`} className="text-blue-400 hover:underline font-mono">
                        +91 {job.customerPhone}
                      </a>
                    </div>
                    <div className="flex items-center gap-2 sm:col-span-2">
                      <MapPin className="w-3.5 h-3.5 text-slate-500" />
                      <span className="line-clamp-1">{job.customerAddress}</span>
                    </div>
                  </div>

                  {/* Stage Transition Buttons */}
                  <div className="pt-2 flex items-center justify-between">
                    <div className="text-xs text-slate-500">
                      Scheduled: {job.preferredDateTime}
                    </div>

                    <div className="flex gap-2">
                      {job.status === 'ASSIGNED' && (
                        <button
                          onClick={() => handleStartJob(job)}
                          className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-extrabold text-xs shadow-md"
                        >
                          Start Work (In-Progress)
                        </button>
                      )}
                      {job.status === 'IN_PROGRESS' && (
                        <button
                          onClick={() => setJobToComplete(job)}
                          className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold text-xs shadow-md"
                        >
                          Complete & Close Job
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* TAB 1: AVAILABLE UNASSIGNED JOBS */}
        {selectedTab === 1 && (
          <div className="space-y-4">
            {unassignedJobs.length === 0 ? (
              <div className="py-16 text-center text-xs text-slate-500">
                No unclaimed service tickets waiting at the counter desk.
              </div>
            ) : (
              unassignedJobs.map(job => (
                <div
                  key={job.id}
                  className={`p-5 rounded-3xl border shadow-lg space-y-3 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-red-500/20 text-red-400">
                        {job.priority} Priority
                      </span>
                      <h3 className="text-sm font-extrabold text-slate-100 mt-1">{job.problemDescription}</h3>
                      <p className="text-xs text-slate-400 mt-0.5">{job.customerName} · {job.customerAddress}</p>
                    </div>

                    <button
                      onClick={() => handleClaimJob(job)}
                      className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs shadow-md shrink-0"
                    >
                      Claim This Job
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </main>

      {/* Complete Job Modal */}
      {jobToComplete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-md rounded-3xl border shadow-2xl p-6 ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h3 className="font-extrabold text-base">Complete Job Confirmation</h3>
              <button
                onClick={() => setJobToComplete(null)}
                className="p-1 rounded-full text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCompleteJob} className="mt-4 space-y-4">
              <p className="text-xs text-slate-400 leading-relaxed">
                Confirming completion for <strong>{jobToComplete.customerName}</strong> ({jobToComplete.problemDescription}).
              </p>

              <div>
                <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Technician Work Notes & Parts Fitted
                </label>
                <textarea
                  rows={3}
                  value={completionNotes}
                  onChange={(e) => setCompletionNotes(e.target.value)}
                  placeholder="e.g. Replaced 16A MCB breaker, verified load stability"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setJobToComplete(null)}
                  className="w-1/2 py-2.5 rounded-xl border border-slate-700 text-xs font-bold text-slate-400"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="w-1/2 py-2.5 rounded-xl text-xs font-bold bg-emerald-600 text-white shadow-md"
                >
                  Confirm Completion
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
