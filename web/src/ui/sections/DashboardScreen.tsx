import React, { useState } from 'react';
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
  CheckCircle2,
  UserPlus,
  QrCode,
  Sparkles
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

  // Reactive DB queries matching Android StateFlow
  const customers = useLiveQuery(() => db.customers.toArray()) || [];
  const bills = useLiveQuery(() => db.bills.toArray()) || [];
  const services = useLiveQuery(() => db.services.toArray()) || [];
  const workers = useLiveQuery(() => db.workers.toArray()) || [];
  const inventory = useLiveQuery(() => db.inventory.toArray()) || [];

  // Metrics matching Android calculation
  const totalCust = customers.length;
  const totalRevenue = bills.reduce((acc, b) => acc + (b.totalAmount || 0), 0);
  const activeJobs = services.filter(s => s.status !== 'CLOSED' && s.status !== 'COMPLETED').length;
  const lowStockItems = inventory.filter(i => i.stockLevel <= i.minAlertLevel);

  const recentServices = [...services].reverse().slice(0, 4);

  return (
    <div className="space-y-5 animate-fadeIn pb-12">
      {/* 1. Hero Artwork Banner matching Jetpack Compose AdminDashboardScreen */}
      <div className="relative h-36 sm:h-44 rounded-3xl overflow-hidden border border-slate-800 shadow-xl group">
        <img
          src={bannerSrc}
          alt="Dashboard Banner"
          className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
        />
        {/* Scrim gradient overlay matching Compose Brush.verticalGradient */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/70 to-slate-950/40 p-4 sm:p-5 flex flex-col justify-between">
          {/* Top Row: Brand Edition & Elegant Unboxed Green Online Indicator */}
          <div className="flex items-center justify-between">
            <span
              className="text-[10px] font-black uppercase tracking-widest"
              style={{ color: isRevolt ? '#FF4D4D' : accentColor }}
            >
              {isRevolt ? 'RE-VOLT ENTERPRISE' : 'RELEC EDITION'}
            </span>

            {/* Unboxed Green Online Indicator matching Android */}
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              <span className="text-xs font-bold text-emerald-400 tracking-wide">
                Online
              </span>
            </div>
          </div>

          {/* Middle Row: Large Business Title */}
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
              Rattan Electricals
            </h2>
            <p className="text-[11px] text-slate-300 font-medium line-clamp-1 mt-0.5">
              Authorized Electrical Service & Operations Management Desk
            </p>
          </div>
        </div>
      </div>

      {/* 2. Quick Action Shortcut Buttons Row matching Android item 2 */}
      <div className="flex items-center gap-2.5 overflow-x-auto pb-1 no-scrollbar">
        {/* Quick Action 1: New Job */}
        <button
          onClick={() => onNavigateToSection('Services')}
          className="px-3.5 py-2 rounded-xl text-white font-bold text-xs flex items-center gap-1.5 shadow-md shrink-0 transition-transform active:scale-95"
          style={{ backgroundColor: accentColor, border: `1px solid ${accentColor}80` }}
        >
          <Plus className="w-3.5 h-3.5" />
          <span>New Job</span>
        </button>

        {/* Quick Action 2: Add Customer */}
        <button
          onClick={() => onNavigateToSection('Customers')}
          className="px-3.5 py-2 rounded-xl border border-purple-500/40 bg-purple-500/15 text-purple-400 hover:bg-purple-500/25 font-bold text-xs flex items-center gap-1.5 shrink-0 transition-transform active:scale-95"
        >
          <UserPlus className="w-3.5 h-3.5" />
          <span>Add Customer</span>
        </button>

        {/* Quick Action 3: Generate Invoice */}
        <button
          onClick={() => onNavigateToSection('Billing')}
          className="px-3.5 py-2 rounded-xl border border-emerald-500/40 bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 font-bold text-xs flex items-center gap-1.5 shrink-0 transition-transform active:scale-95"
        >
          <Receipt className="w-3.5 h-3.5" />
          <span>Generate Invoice</span>
        </button>

        {/* Quick Action 4: Customer QR Standee */}
        <button
          onClick={() => onNavigateToSection('SupportDesk')}
          className="px-3.5 py-2 rounded-xl border border-amber-500/40 bg-amber-500/15 text-amber-400 hover:bg-amber-500/25 font-bold text-xs flex items-center gap-1.5 shrink-0 transition-transform active:scale-95"
        >
          <QrCode className="w-3.5 h-3.5" />
          <span>Customer QR</span>
        </button>
      </div>

      {/* 3. The 4 QuadStat Cards in exact Android Jetpack Compose order */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* Stat 1: Total Customers */}
        <div
          onClick={() => onNavigateToSection('Customers')}
          className={`p-4 sm:p-5 rounded-2xl border cursor-pointer transition-all hover:scale-[1.02] shadow-sm ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-blue-500/40' : 'bg-white border-slate-200 hover:border-blue-500/40'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Total Customers</span>
            <div className="p-2 rounded-xl bg-blue-500/15 text-blue-500">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-black text-blue-400">
              {totalCust}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">
              {totalCust > 0 ? `${totalCust} Registered` : 'Tap to Add First'}
            </p>
          </div>
        </div>

        {/* Stat 2: Monthly Revenue */}
        <div
          onClick={() => onNavigateToSection('Billing')}
          className={`p-4 sm:p-5 rounded-2xl border cursor-pointer transition-all hover:scale-[1.02] shadow-sm ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-emerald-500/40' : 'bg-white border-slate-200 hover:border-emerald-500/40'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Monthly Revenue</span>
            <div className="p-2 rounded-xl bg-emerald-500/15 text-emerald-500">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-black text-emerald-400">
              ₹{totalRevenue.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">
              {totalRevenue > 0 ? `${bills.length} Invoiced` : 'Ready to Bill'}
            </p>
          </div>
        </div>

        {/* Stat 3: Active Jobs (Services) */}
        <div
          onClick={() => onNavigateToSection('Services')}
          className={`p-4 sm:p-5 rounded-2xl border cursor-pointer transition-all hover:scale-[1.02] shadow-sm ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-cyan-500/40' : 'bg-white border-slate-200 hover:border-cyan-500/40'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Active Jobs</span>
            <div className="p-2 rounded-xl bg-cyan-500/15 text-cyan-400">
              <Wrench className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-black text-cyan-400">{activeJobs}</span>
            <p className="text-[11px] text-slate-500 mt-0.5">
              {activeJobs > 0 ? `${activeJobs} in Progress` : 'All Caught Up'}
            </p>
          </div>
        </div>

        {/* Stat 4: Low Stock Alert */}
        <div
          onClick={() => onNavigateToSection('Inventory')}
          className={`p-4 sm:p-5 rounded-2xl border cursor-pointer transition-all hover:scale-[1.02] shadow-sm ${isDarkTheme ? 'bg-slate-900/90 border-slate-800 hover:border-red-500/40' : 'bg-white border-slate-200 hover:border-red-500/40'}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Low Stock</span>
            <div className={`p-2 rounded-xl ${lowStockItems.length > 0 ? 'bg-red-500/20 text-red-500 animate-pulse' : 'bg-slate-800 text-slate-400'}`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className={`text-xl sm:text-2xl font-black ${lowStockItems.length > 0 ? 'text-red-400' : 'text-slate-400'}`}>
              {lowStockItems.length}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">
              {lowStockItems.length > 0 ? 'Restock Required' : 'Healthy Inventory'}
            </p>
          </div>
        </div>
      </div>

      {/* 4. Low Stock Alert Banner if items require restock */}
      {lowStockItems.length > 0 && (
        <div className="p-3.5 rounded-2xl bg-red-500/10 border border-red-500/25 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
            <span className="text-xs text-red-300">
              <strong>{lowStockItems.length} item(s)</strong> are below critical threshold. Restock required.
            </span>
          </div>
          <button
            onClick={() => onNavigateToSection('Inventory')}
            className="text-xs font-bold text-red-400 hover:underline shrink-0"
          >
            Open Stock &rarr;
          </button>
        </div>
      )}

      {/* 5. Recent Service Requests / Jobs Deck */}
      <div className={`p-5 rounded-3xl border ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200 shadow-sm'}`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Wrench className="w-4 h-4" style={{ color: accentColor }} />
            <h3 className="font-extrabold text-sm tracking-tight">Recent Service Jobs</h3>
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

        {recentServices.length === 0 ? (
          <div className="py-8 text-center text-slate-500 text-xs">
            <p>No active service jobs logged yet.</p>
            <button
              onClick={() => onNavigateToSection('Services')}
              className="mt-2 text-xs font-bold underline"
              style={{ color: accentColor }}
            >
              + Create First Service Job
            </button>
          </div>
        ) : (
          <div className="space-y-2.5">
            {recentServices.map((s) => (
              <div
                key={s.id}
                onClick={() => onNavigateToSection('Services')}
                className={`p-3 rounded-2xl border cursor-pointer transition-all hover:border-slate-600 flex items-center justify-between ${isDarkTheme ? 'bg-slate-950/60 border-slate-800/80' : 'bg-slate-50 border-slate-200'}`}
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-xs">{s.customerName}</span>
                    <span className={`px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider ${
                      s.priority === 'HIGH' || s.priority === 'EMERGENCY'
                        ? 'bg-red-500/20 text-red-400'
                        : 'bg-blue-500/20 text-blue-400'
                    }`}>
                      {s.priority}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 line-clamp-1 mt-0.5">
                    {s.problemDescription}
                  </p>
                </div>

                <div className="text-right shrink-0">
                  <span className={`px-2.5 py-1 rounded-xl text-[10px] font-black uppercase ${
                    s.status === 'COMPLETED'
                      ? 'bg-emerald-500/20 text-emerald-400'
                      : 'bg-amber-500/20 text-amber-400'
                  }`}>
                    {s.status.replace('_', ' ')}
                  </span>
                  <p className="text-[10px] text-slate-500 mt-1">{s.dateCreated}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
