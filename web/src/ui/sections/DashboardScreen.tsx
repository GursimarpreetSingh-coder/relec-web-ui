import React from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import {
  TrendingUp,
  Wrench,
  Users,
  AlertTriangle,
  Plus,
  Receipt,
  Scan,
  Mic,
  Clock,
  ArrowRight,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';

interface DashboardScreenProps {
  onNavigateToSection: (section: string) => void;
  onOpenScanner: () => void;
  onOpenVoice: () => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  onNavigateToSection,
  onOpenScanner,
  onOpenVoice
}) => {
  const { accentColor, isDarkTheme, bannerSrc, brandName, isRevolt } = useTheme();

  // Reactive DB queries matching Flow
  const bills = useLiveQuery(() => db.bills.toArray()) || [];
  const services = useLiveQuery(() => db.services.toArray()) || [];
  const workers = useLiveQuery(() => db.workers.toArray()) || [];
  const inventory = useLiveQuery(() => db.inventory.toArray()) || [];
  const attendance = useLiveQuery(() => db.attendance.toArray()) || [];

  // Metrics
  const totalRevenue = bills.reduce((acc, b) => acc + (b.totalAmount || 0), 0);
  const activeJobs = services.filter(s => s.status !== 'CLOSED' && s.status !== 'COMPLETED').length;
  const workersOnDuty = workers.filter(w => w.status === 'WORKING').length;
  const lowStockItems = inventory.filter(i => i.stockLevel <= i.minAlertLevel);

  const recentServices = [...services].reverse().slice(0, 4);

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Visual Brand Banner */}
      <div className="relative h-44 sm:h-52 rounded-3xl overflow-hidden border border-slate-800 shadow-2xl group">
        <img
          src={bannerSrc}
          alt="Banner"
          className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-black/85 via-black/50 to-transparent p-6 flex flex-col justify-end">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-black/60 border border-white/10 backdrop-blur-md w-fit mb-2">
            <span className="w-2 h-2 rounded-full animate-ping" style={{ backgroundColor: accentColor }} />
            <span className="text-[11px] font-bold text-white uppercase tracking-wider">
              {brandName} Enterprise Core Active
            </span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
            Rattan Electricals Management Desk
          </h2>
          <p className="text-xs text-slate-300 font-medium max-w-md mt-1">
            Real-time synchronization for shop stock, GST invoices, service boards, and field technician dispatch.
          </p>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* Total Revenue */}
        <div
          onClick={() => onNavigateToSection('Billing')}
          className={`p-4 sm:p-5 rounded-3xl border cursor-pointer transition-all hover:scale-[1.02] shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Total Billed</span>
            <div className="p-2 rounded-xl bg-emerald-500/15 text-emerald-500">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-black text-emerald-400">
              ₹{totalRevenue.toLocaleString('en-IN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">{bills.length} invoices generated</p>
          </div>
        </div>

        {/* Active Jobs */}
        <div
          onClick={() => onNavigateToSection('Services')}
          className={`p-4 sm:p-5 rounded-3xl border cursor-pointer transition-all hover:scale-[1.02] shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Active Jobs</span>
            <div className="p-2 rounded-xl bg-blue-500/15 text-blue-500">
              <Wrench className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-black text-blue-400">{activeJobs}</span>
            <p className="text-[11px] text-slate-500 mt-0.5">{services.length} total tickets logged</p>
          </div>
        </div>

        {/* Workers on Duty */}
        <div
          onClick={() => onNavigateToSection('Workers')}
          className={`p-4 sm:p-5 rounded-3xl border cursor-pointer transition-all hover:scale-[1.02] shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Electricians</span>
            <div className="p-2 rounded-xl bg-amber-500/15 text-amber-500">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-black text-amber-400">
              {workersOnDuty} / {workers.length}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">Technicians in field</p>
          </div>
        </div>

        {/* Low Stock Alerts */}
        <div
          onClick={() => onNavigateToSection('Inventory')}
          className={`p-4 sm:p-5 rounded-3xl border cursor-pointer transition-all hover:scale-[1.02] shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Low Stock</span>
            <div className={`p-2 rounded-xl ${lowStockItems.length > 0 ? 'bg-red-500/20 text-red-500 animate-pulse' : 'bg-slate-800 text-slate-400'}`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className={`text-xl sm:text-2xl font-black ${lowStockItems.length > 0 ? 'text-red-400' : 'text-slate-400'}`}>
              {lowStockItems.length}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">Items needing restock</p>
          </div>
        </div>
      </div>

      {/* Low Stock Warning Alert if any item is low */}
      {lowStockItems.length > 0 && (
        <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-red-500/20 text-red-400">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-xs font-black text-red-400 uppercase tracking-wider">
                Critical Stock Alert: {lowStockItems.length} Products Below Threshold
              </h4>
              <p className="text-xs text-slate-300 mt-0.5">
                {lowStockItems.map(i => `${i.name} (${i.stockLevel} left)`).join(', ')}
              </p>
            </div>
          </div>
          <button
            onClick={() => onNavigateToSection('Inventory')}
            className="px-3.5 py-1.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs shrink-0 shadow-md"
          >
            Restock Now
          </button>
        </div>
      )}

      {/* Quick Actions Bar */}
      <div>
        <h3 className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3">
          Quick Operational Actions
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          <button
            onClick={() => onNavigateToSection('Services')}
            className={`p-3.5 rounded-2xl border flex items-center gap-2.5 font-bold text-xs transition-all hover:scale-[1.02] ${isDarkTheme ? 'bg-slate-900 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
          >
            <div className="p-2 rounded-xl bg-blue-500/15 text-blue-400">
              <Plus className="w-4 h-4" />
            </div>
            <span>New Job Ticket</span>
          </button>

          <button
            onClick={() => onNavigateToSection('Billing')}
            className={`p-3.5 rounded-2xl border flex items-center gap-2.5 font-bold text-xs transition-all hover:scale-[1.02] ${isDarkTheme ? 'bg-slate-900 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
          >
            <div className="p-2 rounded-xl bg-emerald-500/15 text-emerald-400">
              <Receipt className="w-4 h-4" />
            </div>
            <span>Create Bill</span>
          </button>

          <button
            onClick={() => onNavigateToSection('Customers')}
            className={`p-3.5 rounded-2xl border flex items-center gap-2.5 font-bold text-xs transition-all hover:scale-[1.02] ${isDarkTheme ? 'bg-slate-900 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
          >
            <div className="p-2 rounded-xl bg-purple-500/15 text-purple-400">
              <Users className="w-4 h-4" />
            </div>
            <span>Add Customer</span>
          </button>

          <button
            onClick={onOpenScanner}
            className={`p-3.5 rounded-2xl border flex items-center gap-2.5 font-bold text-xs transition-all hover:scale-[1.02] ${isDarkTheme ? 'bg-slate-900 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
          >
            <div className="p-2 rounded-xl bg-amber-500/15 text-amber-400">
              <Scan className="w-4 h-4" />
            </div>
            <span>Gemini Vision AI</span>
          </button>

          <button
            onClick={onOpenVoice}
            className={`p-3.5 rounded-2xl border flex items-center gap-2.5 font-bold text-xs transition-all hover:scale-[1.02] ${isDarkTheme ? 'bg-slate-900 border-slate-800 hover:border-slate-700' : 'bg-white border-slate-200 hover:border-slate-300'}`}
          >
            <div className="p-2 rounded-xl bg-red-500/15 text-red-400">
              <Mic className="w-4 h-4" />
            </div>
            <span>Speech Entry</span>
          </button>
        </div>
      </div>

      {/* Recent Service Requests Stage Timeline */}
      <div className={`p-5 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="font-extrabold text-base">Live Service Dispatch Board</h3>
            <p className="text-xs text-slate-500">Real-time status tracking for customer electrical complaints</p>
          </div>
          <button
            onClick={() => onNavigateToSection('Services')}
            className="text-xs font-bold flex items-center gap-1 hover:underline"
            style={{ color: accentColor }}
          >
            <span>View All ({services.length})</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="space-y-3">
          {recentServices.length === 0 ? (
            <p className="text-xs text-slate-500 py-4 text-center">No active service tickets.</p>
          ) : (
            recentServices.map(job => (
              <div
                key={job.id}
                onClick={() => onNavigateToSection('Services')}
                className={`p-3.5 rounded-2xl border transition-all hover:scale-[1.01] cursor-pointer flex items-center justify-between ${isDarkTheme ? 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700' : 'bg-slate-50 border-slate-200 hover:border-slate-300'}`}
              >
                <div className="flex items-center gap-3">
                  <div className={`p-2.5 rounded-xl ${
                    job.priority === 'EMERGENCY' ? 'bg-red-500/20 text-red-400' :
                    job.priority === 'HIGH' ? 'bg-amber-500/20 text-amber-400' :
                    'bg-blue-500/20 text-blue-400'
                  }`}>
                    <Wrench className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-xs text-slate-100">{job.customerName}</h4>
                      <span className="text-[10px] text-slate-500 font-mono">({job.customerPhone})</span>
                    </div>
                    <p className="text-xs text-slate-400 line-clamp-1 mt-0.5">{job.problemDescription}</p>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <span className={`inline-block px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider ${
                    job.status === 'COMPLETED' ? 'bg-emerald-500/20 text-emerald-400' :
                    job.status === 'IN_PROGRESS' ? 'bg-blue-500/20 text-blue-400' :
                    job.status === 'ASSIGNED' ? 'bg-amber-500/20 text-amber-400' :
                    'bg-slate-800 text-slate-400'
                  }`}>
                    {job.status.replace('_', ' ')}
                  </span>
                  <p className="text-[10px] text-slate-500 mt-0.5">
                    {job.assignedWorkerName ? `Tech: ${job.assignedWorkerName}` : 'Unassigned'}
                  </p>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
