import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import type { Customer } from '../../types/models';
import {
  Users,
  Search,
  Plus,
  Phone,
  MessageCircle,
  MapPin,
  Calendar,
  Edit2,
  Trash2,
  X,
  Check
} from 'lucide-react';

export const CustomersScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();
  const customers = useLiveQuery(() => db.customers.toArray()) || [];

  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);

  // Form states
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');

  const filtered = customers.filter(c =>
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.phone.includes(searchQuery) ||
    c.address.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSaveCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !phone.trim()) return;

    if (editingCustomer) {
      await db.customers.update(editingCustomer.id, {
        name: name.trim(),
        phone: phone.trim(),
        address: address.trim()
      });
      setEditingCustomer(null);
    } else {
      await db.customers.put({
        id: 'C_' + Date.now().toString(36),
        name: name.trim(),
        phone: phone.trim(),
        address: address.trim() || 'Shop Local Area',
        joinedDate: new Date().toISOString().split('T')[0]
      });
      setShowAddModal(false);
    }

    setName('');
    setPhone('');
    setAddress('');
  };

  const handleDeleteCustomer = async (id: string, custName: string) => {
    if (confirm(`Are you sure you want to delete customer record for ${custName}?`)) {
      await db.customers.delete(id);
    }
  };

  const openEditModal = (cust: Customer) => {
    setEditingCustomer(cust);
    setName(cust.name);
    setPhone(cust.phone);
    setAddress(cust.address);
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header & Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Customer Directory</h2>
          <p className="text-xs text-slate-500">
            {customers.length} registered consumer and business accounts
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-64">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search name or mobile..."
              className={`w-full pl-9 pr-4 py-2 rounded-2xl text-xs border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-white' : 'bg-white border-slate-200 text-slate-900'}`}
            />
          </div>

          <button
            onClick={() => {
              setName('');
              setPhone('');
              setAddress('');
              setShowAddModal(true);
            }}
            className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all shrink-0"
            style={{ backgroundColor: accentColor }}
          >
            <Plus className="w-4 h-4" />
            <span>Add Customer</span>
          </button>
        </div>
      </div>

      {/* Customer Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.length === 0 ? (
          <div className="col-span-full py-12 text-center text-xs text-slate-500">
            No customer accounts found matching "{searchQuery}".
          </div>
        ) : (
          filtered.map(cust => (
            <div
              key={cust.id}
              className={`p-5 rounded-3xl border shadow-lg transition-all hover:scale-[1.01] flex flex-col justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
            >
              <div>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-10 h-10 rounded-2xl flex items-center justify-center font-black text-sm text-white shadow-md"
                      style={{ backgroundColor: accentColor }}
                    >
                      {cust.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="font-extrabold text-sm leading-snug">{cust.name}</h3>
                      <span className="text-xs font-mono text-slate-400">{cust.phone}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => openEditModal(cust)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800/50"
                      title="Edit Customer"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => handleDeleteCustomer(cust.id, cust.name)}
                      className="p-1.5 rounded-lg text-red-400 hover:text-red-300 hover:bg-red-500/10"
                      title="Delete Customer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <div className="mt-4 space-y-2 text-xs text-slate-400">
                  <div className="flex items-center gap-2">
                    <MapPin className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                    <span className="line-clamp-1">{cust.address}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Calendar className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                    <span>Customer since {cust.joinedDate}</span>
                  </div>
                </div>
              </div>

              {/* Action Buttons: Call & WhatsApp */}
              <div className="mt-5 pt-3 border-t border-slate-200 dark:border-slate-800/80 grid grid-cols-2 gap-2">
                <a
                  href={`tel:${cust.phone}`}
                  className={`py-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${isDarkTheme ? 'bg-slate-800 hover:bg-slate-700 text-slate-200' : 'bg-slate-100 hover:bg-slate-200 text-slate-800'}`}
                >
                  <Phone className="w-3.5 h-3.5 text-blue-400" />
                  <span>Call Now</span>
                </a>
                <a
                  href={`https://wa.me/91${cust.phone.replace(/\D/g, '')}?text=Hello%20${encodeURIComponent(cust.name)},%20this%20is%20Rattan%20Electricals.`}
                  target="_blank"
                  rel="noreferrer"
                  className="py-2 rounded-xl text-xs font-bold bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 flex items-center justify-center gap-1.5 transition-colors"
                >
                  <MessageCircle className="w-3.5 h-3.5" />
                  <span>WhatsApp</span>
                </a>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add / Edit Customer Modal */}
      {(showAddModal || editingCustomer) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-md rounded-3xl border shadow-2xl p-6 ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-base">
                {editingCustomer ? 'Edit Customer' : 'Add New Customer'}
              </h3>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  setEditingCustomer(null);
                }}
                className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveCustomer} className="mt-4 space-y-3.5">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Customer Full Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Gurpreet Singh"
                  className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  10-Digit Mobile Number
                </label>
                <input
                  type="tel"
                  required
                  maxLength={10}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="9872134590"
                  className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Service / Residential Address
                </label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="H.No 142, Sector 15, Chandigarh"
                  className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false);
                    setEditingCustomer(null);
                  }}
                  className="w-1/2 py-2.5 rounded-xl border border-slate-700 text-xs font-bold text-slate-400 hover:bg-slate-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="w-1/2 py-2.5 rounded-xl text-xs font-bold text-white shadow-md"
                  style={{ backgroundColor: accentColor }}
                >
                  Save Record
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
