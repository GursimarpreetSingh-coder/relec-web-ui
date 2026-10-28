import React, { useState, useEffect } from 'react';
import { useTheme } from './theme';
import { authService } from '../data/authService';
import { cloudSyncService, type CloudSyncState } from '../data/cloudSyncService';
import { DashboardScreen } from './sections/DashboardScreen';
import { CustomersScreen } from './sections/CustomersScreen';
import { ServicesScreen } from './sections/ServicesScreen';
import { WorkersScreen } from './sections/WorkersScreen';
import { InventoryScreen } from './sections/InventoryScreen';
import { BillingScreen } from './sections/BillingScreen';
import { ExpensesScreen } from './sections/ExpensesScreen';
import { ReportsScreen } from './sections/ReportsScreen';
import { SupportDeskScreen } from './sections/SupportDeskScreen';
import { TeamApprovalsScreen } from './sections/TeamApprovalsScreen';
import { SettingsScreen } from './sections/SettingsScreen';
import { GlobalScannerModal } from './components/GlobalScannerModal';
import { AIVoiceEntryModal } from './components/AIVoiceEntryModal';
import {
  LayoutDashboard,
  Wrench,
  Users,
  Receipt,
  MoreHorizontal,
  Scan,
  Mic,
  Sun,
  Moon,
  LogOut,
  Package,
  CreditCard,
  BarChart3,
  Headphones,
  ShieldCheck,
  Settings,
  X,
  Download,
  RefreshCw,
  Sparkles
} from 'lucide-react';

interface AdminDashboardViewProps {
  onLogout: () => void;
}

export const AdminDashboardView: React.FC<AdminDashboardViewProps> = ({ onLogout }) => {
  const {
    brandName,
    isRevolt,
    accentColor,
    isDarkTheme,
    toggleDarkTheme,
    logoSrc
  } = useTheme();

  const [currentSection, setCurrentSection] = useState<string>('Dashboard');
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState<boolean>(false);
  const [showScannerModal, setShowScannerModal] = useState<boolean>(false);
  const [showVoiceModal, setShowVoiceModal] = useState<boolean>(false);

  const currentSession = authService.getCurrentSession();
  const isGuestSandbox = currentSession?.isGuestSandbox || currentSession?.mode === 'sandbox';

  const [syncState, setSyncState] = useState<CloudSyncState>(cloudSyncService.getState());
  const [syncNotification, setSyncNotification] = useState<string>('');

  useEffect(() => {
    return cloudSyncService.subscribe((state) => {
      setSyncState(state);
    });
  }, []);

  const handleManualSync = async () => {
    const res = await cloudSyncService.syncNow();
    setSyncNotification(res.message);
    setTimeout(() => setSyncNotification(''), 4000);
  };

  // More menu items
  const moreModules = [
    { label: 'Workers & Electricians', icon: Users, section: 'Workers' },
    { label: 'Stock & Inventory', icon: Package, section: 'Inventory' },
    { label: 'Operational Expenses', icon: CreditCard, section: 'Expenses' },
    { label: 'Financial Analytics', icon: BarChart3, section: 'Reports' },
    { label: 'Support Desk & QR Standee', icon: Headphones, section: 'SupportDesk' },
    { label: 'Team Approvals & RBAC', icon: ShieldCheck, section: 'TeamApprovals' },
    { label: 'Settings & Cloud Ledger', icon: Settings, section: 'Settings' }
  ];

  return (
    <div className={`min-h-screen flex flex-col transition-colors duration-300 ${isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'}`}>
      {/* Sandbox / Production Security Notice Bar */}
      {isGuestSandbox ? (
        <div className="bg-amber-500/15 border-b border-amber-500/30 px-4 py-2 flex items-center justify-between text-xs text-amber-400">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-amber-400 shrink-0" />
            <span className="font-semibold">
              <strong>GUEST SANDBOX DEMO:</strong> Real store database is secured by Super Admin. You can freely test all features in this isolated playground.
            </span>
          </div>
          <button
            onClick={onLogout}
            className="text-[11px] font-bold underline hover:text-white px-2 py-0.5 rounded bg-amber-500/20"
          >
            Switch to Owner Login
          </button>
        </div>
      ) : (
        <div className="bg-emerald-500/10 border-b border-emerald-500/20 px-4 py-1.5 flex items-center justify-between text-[11px] text-emerald-400 font-semibold">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>LIVE PRODUCTION CLOUD LEDGER — Synced with Android Mobile App ({currentSession?.displayName || 'Gursimar Singh'})</span>
          </div>
          {syncNotification && <span className="text-emerald-300 font-bold">{syncNotification}</span>}
        </div>
      )}

      {/* Top Application Bar matching Jetpack Compose */}
      <header className={`sticky top-0 z-40 px-4 sm:px-6 py-2.5 border-b backdrop-blur-xl flex items-center justify-between transition-colors ${isDarkTheme ? 'bg-slate-950/85 border-slate-800/80 shadow-md' : 'bg-white/90 border-slate-200 shadow-sm'}`}>
        {/* Brand Logo & Title */}
        <div className="flex items-center gap-2.5">
          <div
            className="w-9 h-9 rounded-xl p-0.5 border flex items-center justify-center overflow-hidden shrink-0 shadow-sm"
            style={{
              borderColor: `${accentColor}50`,
              backgroundColor: isRevolt ? 'transparent' : '#FFFFFF'
            }}
          >
            <img src={logoSrc} alt="Logo" className="w-full h-full object-cover rounded-lg" />
          </div>

          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-black text-base sm:text-lg tracking-tight leading-none">
                {brandName}
              </span>
              <span
                className="px-1.5 py-0.5 rounded text-[8px] font-black uppercase tracking-wider text-white"
                style={{ backgroundColor: accentColor }}
              >
                PRO ADMIN
              </span>
            </div>
            <p className="text-[10px] font-semibold text-slate-500 tracking-wide mt-0.5">
              Rattan Electricals
            </p>
          </div>
        </div>

        {/* Action Controls matching Android size 30dp square buttons */}
        <div className="flex items-center gap-1.5 sm:gap-2">
          {/* Cloud Sync Button */}
          <button
            onClick={handleManualSync}
            disabled={syncState.isSyncing}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-blue-400 hover:bg-slate-800' : 'bg-slate-100 border-slate-200 text-blue-600 hover:bg-slate-200'}`}
            title={syncState.lastSyncTime ? `Last synced: ${syncState.lastSyncTime}. Click to sync with Mobile App.` : 'Sync with Mobile App & Cloud Ledger'}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${syncState.isSyncing ? 'animate-spin text-emerald-400' : ''}`} />
          </button>

          {/* AI Scanner Button */}
          <button
            onClick={() => setShowScannerModal(true)}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-amber-400 hover:bg-slate-800' : 'bg-slate-100 border-slate-200 text-amber-600 hover:bg-slate-200'}`}
            title="Scan Object or Invoice with AI"
          >
            <Scan className="w-3.5 h-3.5" />
          </button>

          {/* AI Speech Desk Button */}
          <button
            onClick={() => setShowVoiceModal(true)}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-red-400 hover:bg-slate-800' : 'bg-slate-100 border-slate-200 text-red-600 hover:bg-slate-200'}`}
            title="Speech Voice Command Desk"
          >
            <Mic className="w-3.5 h-3.5" />
          </button>

          {/* Mobile APK Download Button */}
          <a
            href="./downloads/relec-pro-erp.apk"
            download="relec-pro-erp.apk"
            className="p-2 rounded-xl border transition-all bg-emerald-500/10 border-emerald-500/30 text-emerald-500 hover:bg-emerald-500/20"
            title="Download Android Mobile App (APK)"
          >
            <Download className="w-3.5 h-3.5" />
          </a>

          {/* Theme Toggle */}
          <button
            onClick={toggleDarkTheme}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-amber-400 hover:bg-slate-800' : 'bg-slate-100 border-slate-200 text-amber-600 hover:bg-slate-200'}`}
            title="Toggle Dark/Light Mode"
          >
            {isDarkTheme ? <Sun className="w-3.5 h-3.5" /> : <Moon className="w-3.5 h-3.5" />}
          </button>

          {/* Logout Button */}
          <button
            onClick={onLogout}
            className="p-2 rounded-xl border border-red-500/20 bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-all ml-0.5"
            title="Sign Out"
          >
            <LogOut className="w-3.5 h-3.5" />
          </button>
        </div>
      </header>

      {/* Main Screen Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 pb-28">
        {currentSection === 'Dashboard' && (
          <DashboardScreen
            onNavigateToSection={(s) => setCurrentSection(s)}
            onOpenScanner={() => setShowScannerModal(true)}
            onOpenVoice={() => setShowVoiceModal(true)}
          />
        )}
        {currentSection === 'Services' && <ServicesScreen />}
        {currentSection === 'Customers' && <CustomersScreen />}
        {currentSection === 'Billing' && <BillingScreen />}
        {currentSection === 'Workers' && <WorkersScreen />}
        {currentSection === 'Inventory' && <InventoryScreen />}
        {currentSection === 'Expenses' && <ExpensesScreen />}
        {currentSection === 'Reports' && <ReportsScreen />}
        {currentSection === 'SupportDesk' && <SupportDeskScreen />}
        {currentSection === 'TeamApprovals' && <TeamApprovalsScreen />}
        {currentSection === 'Settings' && <SettingsScreen onLogout={onLogout} />}
      </main>

      {/* Bottom Sticky Navigation Bar matching Jetpack Compose Material 3 */}
      <nav className={`fixed bottom-0 left-0 right-0 z-40 border-t backdrop-blur-2xl transition-colors ${isDarkTheme ? 'bg-slate-950/92 border-slate-800/80' : 'bg-white/95 border-slate-200 shadow-lg'}`}>
        <div className="max-w-lg mx-auto grid grid-cols-5 py-2 px-2">
          {/* 1. Dashboard */}
          <button
            onClick={() => setCurrentSection('Dashboard')}
            className={`flex flex-col items-center justify-center py-1 rounded-xl transition-all ${currentSection === 'Dashboard' ? 'font-extrabold' : 'text-slate-500 hover:text-slate-300'}`}
            style={{ color: currentSection === 'Dashboard' ? accentColor : undefined }}
          >
            <LayoutDashboard className="w-4 h-4 mb-1" />
            <span className="text-[10px]">Dashboard</span>
          </button>

          {/* 2. Jobs (Services) */}
          <button
            onClick={() => setCurrentSection('Services')}
            className={`flex flex-col items-center justify-center py-1 rounded-xl transition-all ${currentSection === 'Services' ? 'font-extrabold' : 'text-slate-500 hover:text-slate-300'}`}
            style={{ color: currentSection === 'Services' ? accentColor : undefined }}
          >
            <Wrench className="w-4 h-4 mb-1" />
            <span className="text-[10px]">Jobs</span>
          </button>

          {/* 3. Customers */}
          <button
            onClick={() => setCurrentSection('Customers')}
            className={`flex flex-col items-center justify-center py-1 rounded-xl transition-all ${currentSection === 'Customers' ? 'font-extrabold' : 'text-slate-500 hover:text-slate-300'}`}
            style={{ color: currentSection === 'Customers' ? accentColor : undefined }}
          >
            <Users className="w-4 h-4 mb-1" />
            <span className="text-[10px]">Customers</span>
          </button>

          {/* 4. Finances */}
          <button
            onClick={() => setCurrentSection('Billing')}
            className={`flex flex-col items-center justify-center py-1 rounded-xl transition-all ${currentSection === 'Billing' ? 'font-extrabold' : 'text-slate-500 hover:text-slate-300'}`}
            style={{ color: currentSection === 'Billing' ? accentColor : undefined }}
          >
            <Receipt className="w-4 h-4 mb-1" />
            <span className="text-[10px]">Finances</span>
          </button>

          {/* 5. More Menu */}
          <button
            onClick={() => setIsMoreMenuOpen(true)}
            className={`flex flex-col items-center justify-center py-1 rounded-xl transition-all ${['Workers', 'Inventory', 'Expenses', 'Reports', 'SupportDesk', 'TeamApprovals', 'Settings'].includes(currentSection) ? 'font-extrabold' : 'text-slate-500 hover:text-slate-300'}`}
            style={{ color: ['Workers', 'Inventory', 'Expenses', 'Reports', 'SupportDesk', 'TeamApprovals', 'Settings'].includes(currentSection) ? accentColor : undefined }}
          >
            <MoreHorizontal className="w-4 h-4 mb-1" />
            <span className="text-[10px]">More</span>
          </button>
        </div>
      </nav>

      {/* Modal Bottom Sheet for "More" Menu items */}
      {isMoreMenuOpen && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-lg rounded-t-3xl sm:rounded-3xl border shadow-2xl p-6 ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-sm uppercase tracking-wider text-slate-400">
                ERP Management Modules
              </h3>
              <button
                onClick={() => setIsMoreMenuOpen(false)}
                className="p-1 rounded-full text-slate-400 hover:text-slate-200"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 my-4">
              {moreModules.map((item) => {
                const Icon = item.icon;
                const isSelected = currentSection === item.section;
                return (
                  <button
                    key={item.section}
                    onClick={() => {
                      setCurrentSection(item.section);
                      setIsMoreMenuOpen(false);
                    }}
                    className={`p-3.5 rounded-2xl border text-left transition-all flex flex-col justify-between h-24 ${isSelected ? 'border-blue-500 bg-blue-500/10' : 'border-slate-800 bg-slate-950/50 hover:bg-slate-950'}`}
                  >
                    <Icon className="w-5 h-5" style={{ color: isSelected ? accentColor : '#94A3B8' }} />
                    <span className="text-xs font-bold leading-tight">{item.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Modals for AI */}
      {showScannerModal && (
        <GlobalScannerModal
          onClose={() => setShowScannerModal(false)}
          onNavigateToSection={(s) => setCurrentSection(s)}
        />
      )}

      {showVoiceModal && (
        <AIVoiceEntryModal
          onClose={() => setShowVoiceModal(false)}
          onNavigateToSection={(s) => setCurrentSection(s)}
        />
      )}
    </div>
  );
};
