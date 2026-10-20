import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import type { ServiceRequest, ServiceStatus, Priority } from '../../types/models';
import {
  Wrench,
  Search,
  Plus,
  Clock,
  User,
  Phone,
  MapPin,
  Calendar,
  AlertTriangle,
  CheckCircle,
  X,
  Trash2,
  ChevronRight
} from 'lucide-react';

export const ServicesScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();

  const services = useLiveQuery(() => db.services.toArray()) || [];
  const workers = useLiveQuery(() => db.workers.toArray()) || [];

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);

  // New ticket form
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [customerAddress, setCustomerAddress] = useState('');
  const [problemDescription, setProblemDescription] = useState('');
  const [category, setCategory] = useState('Wiring Check');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [preferredDateTime, setPreferredDateTime] = useState('2026-07-02 10:00 AM');
  const [assignedWorkerId, setAssignedWorkerId] = useState<string>('');

  const filtered = services.filter(s => {
    const matchStatus = statusFilter === 'ALL' || s.status === statusFilter;
    const matchPriority = priorityFilter === 'ALL' || s.priority === priorityFilter;
    const matchSearch =
      s.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.customerPhone.includes(searchQuery) ||
      s.problemDescription.toLowerCase().includes(searchQuery.toLowerCase());
    return matchStatus && matchPriority && matchSearch;
  });

  const handleCreateService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerName || !customerPhone || !problemDescription) return;

    const assignedWorker = workers.find(w => w.id === assignedWorkerId);

    const newTicket: ServiceRequest = {
      id: 'S_' + Date.now().toString(36),
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      customerAddress: customerAddress.trim() || 'Local Customer Address',
      problemDescription: problemDescription.trim(),
      category,
      priority,
      assignedWorkerId: assignedWorker?.id || null,
      assignedWorkerName: assignedWorker?.name || null,
      status: assignedWorker ? 'ASSIGNED' : 'CREATED',
      preferredDateTime,
      dateCreated: new Date().toISOString().split('T')[0],
      statusHistory: [
        {
          status: 'CREATED',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          notes: 'Ticket registered at Admin Desk'
        }
      ]
    };

    if (assignedWorker) {
      newTicket.statusHistory.push({
        status: 'ASSIGNED',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        notes: `Assigned to ${assignedWorker.name}`
      });
    }

    await db.services.put(newTicket);
    setShowAddModal(false);

    // Reset
    setCustomerName('');
    setCustomerPhone('');
    setCustomerAddress('');
    setProblemDescription('');
  };

  const handleUpdateStatus = async (ticket: ServiceRequest, newStatus: ServiceStatus) => {
    const history = [...ticket.statusHistory];
    history.push({
      status: newStatus,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      notes: `Stage marked as ${newStatus}`
    });

    await db.services.update(ticket.id, {
      status: newStatus,
      statusHistory: history
    });
  };

  const handleAssignWorker = async (ticket: ServiceRequest, workerId: string) => {
    const worker = workers.find(w => w.id === workerId);
    if (!worker) return;

    const history = [...ticket.statusHistory];
    history.push({
      status: 'ASSIGNED',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      notes: `Reassigned to ${worker.name}`
    });

    await db.services.update(ticket.id, {
      assignedWorkerId: worker.id,
      assignedWorkerName: worker.name,
      status: 'ASSIGNED',
      statusHistory: history
    });
  };

  const handleDeleteService = async (id: string) => {
    if (confirm('Delete this service job ticket?')) {
      await db.services.delete(id);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Service Dispatch Board</h2>
          <p className="text-xs text-slate-500">
            {services.length} repair tickets tracked across active field stages
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-56">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search problem or customer..."
              className={`w-full pl-9 pr-4 py-2 rounded-2xl text-xs border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-white' : 'bg-white border-slate-200 text-slate-900'}`}
            />
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all shrink-0"
            style={{ backgroundColor: accentColor }}
          >
            <Plus className="w-4 h-4" />
            <span>New Ticket</span>
          </button>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-wrap gap-2 items-center text-xs font-bold">
        {['ALL', 'CREATED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CLOSED'].map(st => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`px-3 py-1.5 rounded-xl border transition-all ${statusFilter === st ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white border-blue-500 shadow-sm' : 'border-slate-800/80 text-slate-500 hover:text-slate-300'}`}
          >
            {st.replace('_', ' ')}
          </button>
        ))}

        <div className="ml-auto flex items-center gap-1.5">
          <span className="text-[11px] text-slate-500">Priority:</span>
          {['ALL', 'EMERGENCY', 'HIGH', 'MEDIUM'].map(pr => (
            <button
              key={pr}
              onClick={() => setPriorityFilter(pr)}
              className={`px-2 py-1 rounded-lg text-[10px] uppercase font-bold border ${priorityFilter === pr ? 'bg-slate-800 text-white border-slate-600' : 'text-slate-500 border-transparent hover:border-slate-800'}`}
            >
              {pr}
            </button>
          ))}
        </div>
      </div>

      {/* Tickets List */}
      <div className="space-y-4">
        {filtered.length === 0 ? (
          <div className="py-16 text-center text-xs text-slate-500">
            No service tickets found matching current filters.
          </div>
        ) : (
          filtered.map(ticket => (
            <div
              key={ticket.id}
              className={`p-5 rounded-3xl border shadow-lg transition-all ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200 dark:border-slate-800/80">
                <div className="flex items-center gap-3">
                  <div className={`p-2.5 rounded-xl ${
                    ticket.priority === 'EMERGENCY' ? 'bg-red-500/20 text-red-500 ring-2 ring-red-500/40 animate-pulse' :
                    ticket.priority === 'HIGH' ? 'bg-amber-500/20 text-amber-500' :
                    'bg-blue-500/20 text-blue-500'
                  }`}>
                    <AlertTriangle className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${
                        ticket.priority === 'EMERGENCY' ? 'bg-red-500/20 text-red-400' :
                        ticket.priority === 'HIGH' ? 'bg-amber-500/20 text-amber-400' :
                        'bg-blue-500/20 text-blue-400'
                      }`}>
                        {ticket.priority} PRIORITY
                      </span>
                      <span className="text-xs font-bold text-slate-400">· {ticket.category}</span>
                    </div>
                    <h3 className="font-extrabold text-sm text-slate-100 mt-1">
                      {ticket.problemDescription}
                    </h3>
                  </div>
                </div>

                {/* Status Badge */}
                <div className="flex items-center gap-2 self-start sm:self-auto">
                  <span className={`px-3 py-1 rounded-xl text-xs font-black uppercase tracking-wider ${
                    ticket.status === 'COMPLETED' ? 'bg-emerald-500/20 text-emerald-400' :
                    ticket.status === 'IN_PROGRESS' ? 'bg-blue-500/20 text-blue-400' :
                    ticket.status === 'ASSIGNED' ? 'bg-amber-500/20 text-amber-400' :
                    'bg-slate-800 text-slate-400'
                  }`}>
                    {ticket.status.replace('_', ' ')}
                  </span>
                  <button
                    onClick={() => handleDeleteService(ticket.id)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-red-400"
                    title="Delete Ticket"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Details Row */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 my-4 text-xs text-slate-400">
                <div className="flex items-center gap-2">
                  <User className="w-3.5 h-3.5 text-slate-500" />
                  <span className="text-slate-200 font-bold">{ticket.customerName}</span>
                  <span className="text-slate-500">({ticket.customerPhone})</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="w-3.5 h-3.5 text-slate-500" />
                  <span className="line-clamp-1">{ticket.customerAddress}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="w-3.5 h-3.5 text-slate-500" />
                  <span>Scheduled: {ticket.preferredDateTime}</span>
                </div>
              </div>

              {/* Assignee & Stage Progression Controls */}
              <div className="pt-3 border-t border-slate-200 dark:border-slate-800/80 flex flex-wrap items-center justify-between gap-3">
                {/* Technician Picker */}
                <div className="flex items-center gap-2 text-xs">
                  <span className="font-bold text-slate-500">Technician:</span>
                  <select
                    value={ticket.assignedWorkerId || ''}
                    onChange={(e) => handleAssignWorker(ticket, e.target.value)}
                    className={`px-3 py-1.5 rounded-xl border text-xs font-semibold focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-slate-200' : 'bg-slate-100 border-slate-300 text-slate-800'}`}
                  >
                    <option value="">Unassigned</option>
                    {workers.map(w => (
                      <option key={w.id} value={w.id}>
                        {w.name} ({w.designation})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Stage Progression Buttons */}
                <div className="flex items-center gap-1.5">
                  {ticket.status === 'CREATED' && (
                    <button
                      onClick={() => handleUpdateStatus(ticket, 'ASSIGNED')}
                      className="px-3 py-1.5 rounded-xl bg-amber-500/20 text-amber-400 hover:bg-amber-500/30 text-xs font-bold"
                    >
                      Mark Assigned
                    </button>
                  )}
                  {ticket.status === 'ASSIGNED' && (
                    <button
                      onClick={() => handleUpdateStatus(ticket, 'IN_PROGRESS')}
                      className="px-3 py-1.5 rounded-xl bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 text-xs font-bold"
                    >
                      Start In-Progress
                    </button>
                  )}
                  {ticket.status === 'IN_PROGRESS' && (
                    <button
                      onClick={() => handleUpdateStatus(ticket, 'COMPLETED')}
                      className="px-3 py-1.5 rounded-xl bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30 text-xs font-bold"
                    >
                      Mark Complete
                    </button>
                  )}
                  {ticket.status === 'COMPLETED' && (
                    <button
                      onClick={() => handleUpdateStatus(ticket, 'CLOSED')}
                      className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-400 hover:bg-slate-700 text-xs font-bold"
                    >
                      Close Ticket
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add New Service Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-lg rounded-3xl border shadow-2xl p-6 max-h-[90vh] overflow-y-auto ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-base">Book New Electrical Service Ticket</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateService} className="mt-4 space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Customer Name
                  </label>
                  <input
                    type="text"
                    required
                    value={customerName}
                    onChange={(e) => setCustomerName(e.target.value)}
                    placeholder="e.g. Rajesh Kumar"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Mobile Phone
                  </label>
                  <input
                    type="tel"
                    required
                    maxLength={10}
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                    placeholder="9812045612"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Site / Service Address
                </label>
                <input
                  type="text"
                  value={customerAddress}
                  onChange={(e) => setCustomerAddress(e.target.value)}
                  placeholder="H.No 142, Sector 15, Chandigarh"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Problem Description
                </label>
                <textarea
                  rows={2}
                  required
                  value={problemDescription}
                  onChange={(e) => setProblemDescription(e.target.value)}
                  placeholder="e.g. Main MCB tripping repeatedly when AC is switched on"
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
                    <option value="Wiring Check">Wiring Check</option>
                    <option value="DB Board Repair">DB Board Repair</option>
                    <option value="Fan Installation">Fan Installation</option>
                    <option value="Motor Repair">Motor Repair</option>
                    <option value="Lighting Fittings">Lighting Fittings</option>
                    <option value="Inverter Setup">Inverter Setup</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Priority
                  </label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as any)}
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="EMERGENCY">Emergency (Flashing Alert)</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Preferred Date & Time
                  </label>
                  <input
                    type="text"
                    value={preferredDateTime}
                    onChange={(e) => setPreferredDateTime(e.target.value)}
                    placeholder="2026-07-02 10:00 AM"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Assign Technician
                  </label>
                  <select
                    value={assignedWorkerId}
                    onChange={(e) => setAssignedWorkerId(e.target.value)}
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  >
                    <option value="">Leave Unassigned</option>
                    {workers.map(w => (
                      <option key={w.id} value={w.id}>{w.name} ({w.designation})</option>
                    ))}
                  </select>
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
                  Create Ticket
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
