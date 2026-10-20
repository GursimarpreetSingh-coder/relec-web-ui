import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../../data/database';
import type { UserEntity, AuditLogEntry } from '../../types/models';
import {
  ShieldCheck,
  CheckCircle,
  XCircle,
  AlertTriangle,
  UserCheck,
  UserX,
  Trash2,
  Clock,
  History,
  Lock,
  Search
} from 'lucide-react';

export const TeamApprovalsScreen: React.FC = () => {
  const { accentColor, isDarkTheme } = useTheme();

  const users = useLiveQuery(() => db.users.toArray()) || [];
  const auditLogs = useLiveQuery(() => db.auditLogs.reverse().limit(100).toArray()) || [];

  const [activeTab, setActiveTab] = useState<'APPROVALS' | 'USERS' | 'AUDIT'>('APPROVALS');
  const [searchQuery, setSearchQuery] = useState('');

  // Pending users (isApproved === false)
  const pendingUsers = users.filter(u => !u.isApproved);
  const approvedUsers = users.filter(u => u.isApproved);

  const handleApproveUser = async (user: UserEntity) => {
    await db.users.update(user.id, { isApproved: true });
    // Also approve in workers table if worker
    if (user.role === 'Worker') {
      const match = await db.workers.filter(w => w.phone === user.phone || w.email === user.email).first();
      if (match) {
        await db.workers.update(match.id, { isApproved: true, isVerified: true });
      }
    }
    await db.auditLogs.put({
      id: 'AUD_' + Date.now().toString(36),
      userId: 'ADMIN_MASTER_GURSIMAR',
      userName: 'Superior Admin',
      action: 'USER_APPROVED',
      details: `Approved ${user.role} account for ${user.displayName} (${user.email || user.phone})`,
      timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
    });
  };

  const handleToggleActive = async (user: UserEntity) => {
    const newStatus = !user.isActive;
    await db.users.update(user.id, { isActive: newStatus });
    await db.auditLogs.put({
      id: 'AUD_' + Date.now().toString(36),
      userId: 'ADMIN_MASTER_GURSIMAR',
      userName: 'Superior Admin',
      action: newStatus ? 'USER_ACTIVATED' : 'USER_SUSPENDED',
      details: `Changed active state to ${newStatus} for ${user.displayName}`,
      timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
    });
  };

  const handleDeleteUser = async (id: string, name: string) => {
    if (confirm(`Permanently remove user credentials for ${name}?`)) {
      await db.users.delete(id);
      await db.auditLogs.put({
        id: 'AUD_' + Date.now().toString(36),
        userId: 'ADMIN_MASTER_GURSIMAR',
        userName: 'Superior Admin',
        action: 'USER_DELETED',
        details: `Deleted user credentials for ${name}`,
        timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
      });
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header */}
      <div>
        <h2 className="text-xl sm:text-2xl font-black tracking-tight">Team Approvals & Access Control</h2>
        <p className="text-xs text-slate-500">
          Governed by Superior Admin (gursimarpsr@gmail.com). Review registrations, toggle privileges & audit history.
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 text-xs font-bold border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('APPROVALS')}
          className={`px-4 py-2 rounded-xl flex items-center gap-2 transition-all ${activeTab === 'APPROVALS' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
        >
          <span>Pending Approvals</span>
          <span className="px-1.5 py-0.5 rounded-full text-[10px] bg-purple-900 text-purple-200 font-extrabold">
            {pendingUsers.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('USERS')}
          className={`px-4 py-2 rounded-xl flex items-center gap-2 transition-all ${activeTab === 'USERS' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
        >
          <span>All Registered Users</span>
          <span className="px-1.5 py-0.5 rounded-full text-[10px] bg-slate-800 text-slate-300 font-extrabold">
            {users.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('AUDIT')}
          className={`px-4 py-2 rounded-xl flex items-center gap-2 transition-all ${activeTab === 'AUDIT' ? 'bg-purple-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
        >
          <History className="w-3.5 h-3.5" />
          <span>Security Audit Trail</span>
        </button>
      </div>

      {/* VIEW 1: PENDING APPROVALS */}
      {activeTab === 'APPROVALS' && (
        <div className="space-y-4">
          {pendingUsers.length === 0 ? (
            <div className={`py-16 text-center rounded-3xl border ${isDarkTheme ? 'bg-slate-900/50 border-slate-800' : 'bg-slate-50 border-slate-200'}`}>
              <CheckCircle className="w-8 h-8 mx-auto text-emerald-400 mb-2 opacity-80" />
              <p className="text-sm font-bold text-slate-300">All Registrations Approved</p>
              <p className="text-xs text-slate-500 mt-0.5">No secondary Admin or Worker requests pending authorization.</p>
            </div>
          ) : (
            pendingUsers.map(user => (
              <div
                key={user.id}
                className={`p-5 rounded-3xl border shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-purple-500/20 text-purple-400">
                      {user.role} Pending Approval
                    </span>
                    <span className="text-xs text-slate-500">· Registered {user.createdAt.split('T')[0]}</span>
                  </div>

                  <h3 className="font-extrabold text-sm text-slate-100 mt-1">{user.displayName}</h3>
                  <div className="flex gap-4 mt-1 text-xs text-slate-400">
                    {user.email && <span>Email: {user.email}</span>}
                    {user.phone && <span>Mobile: +91 {user.phone}</span>}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleApproveUser(user)}
                    className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold text-xs shadow-md transition-all flex items-center gap-1.5"
                  >
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>Approve Access</span>
                  </button>
                  <button
                    onClick={() => handleDeleteUser(user.id, user.displayName)}
                    className="px-4 py-2 rounded-xl bg-red-600/20 hover:bg-red-600/30 text-red-400 font-bold text-xs transition-all"
                  >
                    Reject & Delete
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* VIEW 2: ALL REGISTERED USERS */}
      {activeTab === 'USERS' && (
        <div className="space-y-3">
          {approvedUsers.map(u => (
            <div
              key={u.id}
              className={`p-4 rounded-2xl border flex items-center justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
            >
              <div>
                <div className="flex items-center gap-2">
                  <h4 className="font-bold text-sm text-slate-100">{u.displayName}</h4>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase ${
                    u.role === 'Admin' ? 'bg-blue-500/20 text-blue-400' :
                    u.role === 'Worker' ? 'bg-amber-500/20 text-amber-400' :
                    'bg-slate-800 text-slate-400'
                  }`}>
                    {u.role}
                  </span>
                  {u.email === 'gursimarpsr@gmail.com' && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-black bg-purple-500/20 text-purple-400">
                      SUPERIOR ADMIN
                    </span>
                  )}
                </div>
                <p className="text-xs text-slate-400 mt-0.5">
                  {u.email || ''} {u.phone ? `· +91 ${u.phone}` : ''}
                </p>
              </div>

              <div className="flex items-center gap-2">
                {u.email !== 'gursimarpsr@gmail.com' && (
                  <>
                    <button
                      onClick={() => handleToggleActive(u)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${u.isActive ? 'bg-slate-800 text-emerald-400 hover:bg-slate-700' : 'bg-red-500/20 text-red-400 hover:bg-red-500/30'}`}
                    >
                      {u.isActive ? 'Active' : 'Suspended'}
                    </button>
                    <button
                      onClick={() => handleDeleteUser(u.id, u.displayName)}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-red-400"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* VIEW 3: AUDIT TRAIL */}
      {activeTab === 'AUDIT' && (
        <div className={`p-5 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <h3 className="font-extrabold text-sm text-slate-100 mb-4">Immutable Audit Trail</h3>
          <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
            {auditLogs.map(log => (
              <div
                key={log.id}
                className="p-3 rounded-xl border border-slate-800/80 bg-slate-950/60 text-xs flex items-start justify-between gap-3"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-purple-400 uppercase text-[10px]">
                      {log.action}
                    </span>
                    <span className="text-slate-500">by {log.userName}</span>
                  </div>
                  <p className="text-slate-300 mt-1 font-medium">{log.details}</p>
                </div>
                <span className="text-[10px] text-slate-500 shrink-0 font-mono">
                  {log.timestamp}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
