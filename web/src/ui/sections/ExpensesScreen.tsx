import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import type { Expense } from '../../types/models';
import {
  CreditCard,
  Plus,
  Coffee,
  Car,
  Sparkles,
  Trash2,
  X,
  Calendar,
  Layers
} from 'lucide-react';

export const ExpensesScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();

  const expenses = useLiveQuery(() => db.expenses.toArray()) || [];

  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [showAddModal, setShowAddModal] = useState(false);

  // New expense form
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('Tea & Refreshments');
  const [amount, setAmount] = useState('85');
  const [quantity, setQuantity] = useState('1');

  const categories = [
    'ALL',
    'Tea & Refreshments',
    'Travel & Fuel',
    'Shop Maintenance',
    'Packaging',
    'Miscellaneous'
  ];

  const filtered = expenses.filter(
    e => selectedCategory === 'ALL' || e.category === selectedCategory
  );

  const totalExpenses = expenses.reduce((acc, e) => acc + (e.amount || 0), 0);

  const handleAddExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !amount) return;

    await db.expenses.put({
      id: 'EXP_' + Date.now().toString(36),
      category,
      title: title.trim(),
      amount: Number(amount) || 0,
      quantity: Number(quantity) || 1,
      date: new Date().toISOString().split('T')[0],
      voiceSummary: null
    });

    setShowAddModal(false);
    setTitle('');
  };

  const handleDeleteExpense = async (id: string) => {
    if (confirm('Delete this expense entry?')) {
      await db.expenses.delete(id);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header & Total */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Shop Operational Expenses</h2>
          <p className="text-xs text-slate-500">
            Total Logged Outflow: <strong className="text-amber-400">₹{totalExpenses.toLocaleString()}</strong> across {expenses.length} records
          </p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all self-start sm:self-auto"
          style={{ backgroundColor: accentColor }}
        >
          <Plus className="w-4 h-4" />
          <span>Log Daily Expense</span>
        </button>
      </div>

      {/* Category Pills */}
      <div className="flex flex-wrap gap-2 items-center text-xs font-bold">
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            className={`px-3 py-1.5 rounded-xl border transition-all ${selectedCategory === cat ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white border-blue-500 shadow-sm' : 'border-slate-800/80 text-slate-500 hover:text-slate-300'}`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Expenses Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.length === 0 ? (
          <div className="col-span-full py-16 text-center text-xs text-slate-500">
            No expense records logged in this category.
          </div>
        ) : (
          filtered.map(exp => (
            <div
              key={exp.id}
              className={`p-5 rounded-3xl border shadow-lg transition-all flex flex-col justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
            >
              <div>
                <div className="flex items-start justify-between">
                  <div>
                    <span className="inline-block px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-slate-800 text-slate-400 mb-1.5">
                      {exp.category}
                    </span>
                    <h3 className="font-extrabold text-sm text-slate-100 leading-snug">{exp.title}</h3>
                  </div>

                  <button
                    onClick={() => handleDeleteExpense(exp.id)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-red-400"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                {exp.voiceSummary && (
                  <p className="text-[11px] text-slate-400 italic mt-2 p-2 rounded-xl bg-slate-950/60 border border-slate-800/80">
                    "{exp.voiceSummary}"
                  </p>
                )}

                <div className="mt-4 flex items-center justify-between text-xs text-slate-500">
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>{exp.date}</span>
                  </div>
                  <span>Qty: {exp.quantity || 1}</span>
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-800 flex justify-between items-center">
                <span className="text-xs text-slate-400 font-bold">Amount Spent:</span>
                <span className="text-base font-black text-amber-400">
                  ₹{exp.amount.toFixed(2)}
                </span>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add Expense Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-md rounded-3xl border shadow-2xl p-6 ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-base">Log Shop Expense</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddExpense} className="mt-4 space-y-3.5">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Expense Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                >
                  {categories.filter(c => c !== 'ALL').map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Expense Description / Title
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Evening tea & samosa for staff"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Amount (₹)
                  </label>
                  <input
                    type="number"
                    required
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="85"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Quantity
                  </label>
                  <input
                    type="number"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    placeholder="1"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
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
                  Record Expense
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
