import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import type { InventoryItem } from '../../types/models';
import {
  Package,
  Plus,
  Search,
  AlertTriangle,
  TrendingDown,
  TrendingUp,
  Trash2,
  X,
  Layers,
  Sparkles
} from 'lucide-react';

export const InventoryScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();

  const inventory = useLiveQuery(() => db.inventory.toArray()) || [];

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [showAddModal, setShowAddModal] = useState(false);

  // New item form
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Wires & Cables');
  const [stockLevel, setStockLevel] = useState('20');
  const [minAlertLevel, setMinAlertLevel] = useState('10');
  const [supplierName, setSupplierName] = useState('Havells India Ltd');
  const [price, setPrice] = useState('1450');

  const categories = [
    'ALL',
    'Wires & Cables',
    'Switches & Sockets',
    'Switchgears',
    'Fans & Cooling',
    'Motors & Pumps',
    'Capacitors',
    'General'
  ];

  const filtered = inventory.filter(item => {
    const matchCat = selectedCategory === 'ALL' || item.category === selectedCategory;
    const matchSearch =
      item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.supplierName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.category.toLowerCase().includes(searchQuery.toLowerCase());
    return matchCat && matchSearch;
  });

  const totalCatalogValue = inventory.reduce((acc, i) => acc + i.stockLevel * i.price, 0);
  const lowStockCount = inventory.filter(i => i.stockLevel <= i.minAlertLevel).length;

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    await db.inventory.put({
      id: 'I_' + Date.now().toString(36),
      name: name.trim(),
      category,
      stockLevel: Number(stockLevel) || 0,
      minAlertLevel: Number(minAlertLevel) || 5,
      supplierName: supplierName.trim() || 'Local Distributor',
      price: Number(price) || 0
    });

    setShowAddModal(false);
    setName('');
  };

  const handleAdjustStock = async (item: InventoryItem, delta: number) => {
    const newStock = Math.max(0, item.stockLevel + delta);
    await db.inventory.update(item.id, { stockLevel: newStock });
  };

  const handleDeleteItem = async (id: string, itemName: string) => {
    if (confirm(`Remove ${itemName} from catalog?`)) {
      await db.inventory.delete(id);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header & Valuation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Electrical Stock & Inventory</h2>
          <p className="text-xs text-slate-500">
            {inventory.length} catalog items · Total Stock Valuation: <strong className="text-emerald-400">₹{totalCatalogValue.toLocaleString()}</strong>
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-56">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search part or supplier..."
              className={`w-full pl-9 pr-4 py-2 rounded-2xl text-xs border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-white' : 'bg-white border-slate-200 text-slate-900'}`}
            />
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all shrink-0"
            style={{ backgroundColor: accentColor }}
          >
            <Plus className="w-4 h-4" />
            <span>Add Stock Item</span>
          </button>
        </div>
      </div>

      {/* Category Filter Pills */}
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

      {/* Low Stock Warning Banner if any */}
      {lowStockCount > 0 && (
        <div className="p-3.5 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2.5 text-red-400 font-bold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{lowStockCount} inventory items have reached minimum threshold warning level.</span>
          </div>
          <span className="text-[11px] font-mono text-red-300 font-black">Restock Alert</span>
        </div>
      )}

      {/* Inventory Items Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.length === 0 ? (
          <div className="col-span-full py-16 text-center text-xs text-slate-500">
            No stock products found matching filters.
          </div>
        ) : (
          filtered.map(item => {
            const isLow = item.stockLevel <= item.minAlertLevel;
            const percentage = Math.min(100, Math.round((item.stockLevel / (item.minAlertLevel * 2 || 20)) * 100));

            return (
              <div
                key={item.id}
                className={`p-5 rounded-3xl border shadow-lg transition-all flex flex-col justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
              >
                <div>
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="inline-block px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-slate-800 text-slate-400 mb-1.5">
                        {item.category}
                      </span>
                      <h3 className="font-extrabold text-sm text-slate-100 leading-snug">{item.name}</h3>
                      <p className="text-xs text-slate-500 mt-0.5">Supplier: {item.supplierName}</p>
                    </div>

                    <button
                      onClick={() => handleDeleteItem(item.id, item.name)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-red-400"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Stock Level Bar */}
                  <div className="mt-4">
                    <div className="flex justify-between items-center text-xs font-bold mb-1">
                      <span className="text-slate-400">Stock Quantity:</span>
                      <span className={`font-mono ${isLow ? 'text-red-400 animate-pulse font-black' : 'text-slate-200'}`}>
                        {item.stockLevel} units {isLow && '(LOW)'}
                      </span>
                    </div>

                    <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${isLow ? 'bg-red-500' : 'bg-emerald-500'}`}
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                    <div className="flex justify-between text-[10px] text-slate-500 mt-1">
                      <span>Min alert: {item.minAlertLevel}</span>
                      <span>Rate: ₹{item.price.toFixed(2)}</span>
                    </div>
                  </div>
                </div>

                {/* Bottom Actions: Adjust Quantity */}
                <div className="mt-5 pt-3 border-t border-slate-800 flex items-center justify-between">
                  <div className="text-xs">
                    <span className="text-slate-500 block text-[10px] font-bold uppercase">Total Value</span>
                    <span className="font-black text-emerald-400">
                      ₹{(item.stockLevel * item.price).toLocaleString()}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
                    <button
                      onClick={() => handleAdjustStock(item, -1)}
                      className="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 font-black text-xs flex items-center justify-center transition-colors"
                      title="Decrease by 1"
                    >
                      -
                    </button>
                    <span className="px-2 text-xs font-mono font-bold text-slate-200">{item.stockLevel}</span>
                    <button
                      onClick={() => handleAdjustStock(item, 1)}
                      className="w-7 h-7 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 font-black text-xs flex items-center justify-center transition-colors"
                      title="Increase by 1"
                    >
                      +
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Add Item Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-md rounded-3xl border shadow-2xl p-6 ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-base">Add Product to Stock</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddItem} className="mt-4 space-y-3.5">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Item Description
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Havells 2.5 Sqmm Copper Wire"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Category
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
                    Supplier / Brand
                  </label>
                  <input
                    type="text"
                    value={supplierName}
                    onChange={(e) => setSupplierName(e.target.value)}
                    placeholder="Havells India Ltd"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Initial Stock
                  </label>
                  <input
                    type="number"
                    value={stockLevel}
                    onChange={(e) => setStockLevel(e.target.value)}
                    className={`w-full px-3 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Min Alert
                  </label>
                  <input
                    type="number"
                    value={minAlertLevel}
                    onChange={(e) => setMinAlertLevel(e.target.value)}
                    className={`w-full px-3 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Price (₹)
                  </label>
                  <input
                    type="number"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    className={`w-full px-3 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
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
                  Add Product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
