import React from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import {
  TrendingUp,
  DollarSign,
  PieChart,
  BarChart3,
  CheckCircle,
  Package,
  Wrench,
  Receipt
} from 'lucide-react';

export const ReportsScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();

  const bills = useLiveQuery(() => db.bills.toArray()) || [];
  const purchases = useLiveQuery(() => db.purchases.toArray()) || [];
  const expenses = useLiveQuery(() => db.expenses.toArray()) || [];
  const services = useLiveQuery(() => db.services.toArray()) || [];
  const inventory = useLiveQuery(() => db.inventory.toArray()) || [];

  // Metrics
  const totalSales = bills.reduce((acc, b) => acc + (b.totalAmount || 0), 0);
  const totalPurchasesCost = purchases.reduce((acc, p) => acc + (p.cost || 0), 0);
  const totalExpensesCost = expenses.reduce((acc, e) => acc + (e.amount || 0), 0);
  const totalOutflows = totalPurchasesCost + totalExpensesCost;
  const netProfit = totalSales - totalOutflows;

  const totalServices = services.length;
  const completedServices = services.filter(s => s.status === 'COMPLETED' || s.status === 'CLOSED').length;
  const completionRate = totalServices > 0 ? Math.round((completedServices / totalServices) * 100) : 0;

  const inventoryValuation = inventory.reduce((acc, i) => acc + i.stockLevel * i.price, 0);

  // Group expenses by category
  const expensesByCategory: Record<string, number> = {};
  for (const e of expenses) {
    expensesByCategory[e.category] = (expensesByCategory[e.category] || 0) + e.amount;
  }

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      <div>
        <h2 className="text-xl sm:text-2xl font-black tracking-tight">Financial & Operational Analytics</h2>
        <p className="text-xs text-slate-500">
          Executive performance overview, net profit margin, and inventory valuation
        </p>
      </div>

      {/* Top 3 High Level KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Total Inflow */}
        <div className={`p-5 rounded-3xl border shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div className="flex items-center justify-between text-xs text-slate-500 font-bold uppercase">
            <span>Total Sales Billed</span>
            <div className="p-2 rounded-xl bg-emerald-500/15 text-emerald-400">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-emerald-400">
              ₹{totalSales.toLocaleString('en-IN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">{bills.length} invoices recorded</p>
          </div>
        </div>

        {/* Total Outflow */}
        <div className={`p-5 rounded-3xl border shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div className="flex items-center justify-between text-xs text-slate-500 font-bold uppercase">
            <span>Expenses & Purchases</span>
            <div className="p-2 rounded-xl bg-amber-500/15 text-amber-400">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-amber-400">
              ₹{totalOutflows.toLocaleString('en-IN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">Purchases ₹{totalPurchasesCost.toLocaleString()} + Expenses ₹{totalExpensesCost.toLocaleString()}</p>
          </div>
        </div>

        {/* Net Profit */}
        <div className={`p-5 rounded-3xl border shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div className="flex items-center justify-between text-xs text-slate-500 font-bold uppercase">
            <span>Net Business Margin</span>
            <div className="p-2 rounded-xl bg-blue-500/15 text-blue-400">
              <BarChart3 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className={`text-2xl font-black ${netProfit >= 0 ? 'text-blue-400' : 'text-red-400'}`}>
              ₹{netProfit.toLocaleString('en-IN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
            </span>
            <p className="text-[11px] text-slate-500 mt-0.5">Estimated gross operational profit</p>
          </div>
        </div>
      </div>

      {/* Operational Efficiency Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Service Completion Ratio */}
        <div className={`p-5 rounded-3xl border shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-extrabold text-sm text-slate-100">Job Completion Ratio</h3>
              <p className="text-xs text-slate-500">Service desk ticket turnaround rate</p>
            </div>
            <span className="text-xl font-black text-emerald-400">{completionRate}%</span>
          </div>

          <div className="w-full h-3 rounded-full bg-slate-800 overflow-hidden mb-3">
            <div
              className="h-full rounded-full bg-emerald-500 transition-all duration-700"
              style={{ width: `${completionRate}%` }}
            />
          </div>

          <div className="flex justify-between text-xs text-slate-400">
            <span>Completed Tickets: <strong>{completedServices}</strong></span>
            <span>Active / Pending: <strong>{totalServices - completedServices}</strong></span>
          </div>
        </div>

        {/* Stock Inventory Capital */}
        <div className={`p-5 rounded-3xl border shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-extrabold text-sm text-slate-100">Live Inventory Asset Valuation</h3>
              <p className="text-xs text-slate-500">Total electrical merchandise sitting on shelves</p>
            </div>
            <span className="text-xl font-black" style={{ color: accentColor }}>
              ₹{inventoryValuation.toLocaleString()}
            </span>
          </div>

          <p className="text-xs text-slate-400 leading-relaxed">
            Representing <strong>{inventory.length} distinct item lines</strong>. Products include high-voltage switchgear, copper wire coils, 1200mm ceiling fans, submersible motors, and modular switchboard fittings.
          </p>
        </div>
      </div>

      {/* Expense Breakdown by Category */}
      <div className={`p-5 rounded-3xl border shadow-lg ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <h3 className="font-extrabold text-sm text-slate-100 mb-4">Expense Outflow Distribution</h3>

        <div className="space-y-3">
          {Object.entries(expensesByCategory).length === 0 ? (
            <p className="text-xs text-slate-500 text-center py-4">No categorized expenses recorded.</p>
          ) : (
            Object.entries(expensesByCategory).map(([cat, amt]) => {
              const pct = totalExpensesCost > 0 ? Math.round((amt / totalExpensesCost) * 100) : 0;
              return (
                <div key={cat} className="space-y-1">
                  <div className="flex justify-between text-xs font-bold">
                    <span className="text-slate-300">{cat}</span>
                    <span className="font-mono text-amber-400">₹{amt.toFixed(2)} ({pct}%)</span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-amber-500 transition-all duration-500"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
