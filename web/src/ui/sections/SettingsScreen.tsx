import React, { useState, useEffect } from 'react';
import { useTheme } from '../theme';
import { authService } from '../../data/authService';
import { geminiClient } from '../../data/geminiClient';
import { clearAllToyData, restoreDemoData, initializeSandboxDatabase } from '../../data/database';
import { cloudSyncService, type CloudSyncState } from '../../data/cloudSyncService';
import {
  Settings,
  Palette,
  Moon,
  Sun,
  Sparkles,
  Key,
  Database,
  Trash2,
  RotateCcw,
  LogOut,
  CheckCircle,
  MessageSquare,
  RefreshCw,
  Cloud,
  ShieldCheck,
  Globe
} from 'lucide-react';

interface SettingsScreenProps {
  onLogout: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onLogout }) => {
  const {
    brandTheme,
    setBrandTheme,
    isDarkTheme,
    toggleDarkTheme,
    accentColor,
    setAccentColor,
    isRevolt
  } = useTheme();

  const currentSession = authService.getCurrentSession();

  // Gemini API key state
  const [apiKey, setApiKey] = useState(
    localStorage.getItem('gemini_api_key') || ''
  );
  const [keySaved, setKeySaved] = useState(false);

  // SMS settings
  const [smsProvider, setSmsProvider] = useState(
    localStorage.getItem('sms_provider') || 'fast2sms'
  );
  const [fast2smsKey, setFast2smsKey] = useState(
    localStorage.getItem('fast2sms_api_key') || ''
  );

  const [feedbackMsg, setFeedbackMsg] = useState('');

  // Cloud Sync state
  const [syncState, setSyncState] = useState<CloudSyncState>(cloudSyncService.getState());
  const [cloudSyncUrl, setCloudSyncUrl] = useState(cloudSyncService.getEndpoint());
  const [syncInProgress, setSyncInProgress] = useState(false);

  useEffect(() => {
    return cloudSyncService.subscribe((state) => {
      setSyncState(state);
      setCloudSyncUrl(state.endpoint);
    });
  }, []);

  const handleSaveCloudEndpoint = () => {
    cloudSyncService.setEndpoint(cloudSyncUrl);
    setFeedbackMsg('Cloud Sync Endpoint updated successfully!');
    setTimeout(() => setFeedbackMsg(''), 3000);
  };

  const handleTriggerCloudSync = async () => {
    setSyncInProgress(true);
    const result = await cloudSyncService.syncNow();
    setSyncInProgress(false);
    setFeedbackMsg(result.message);
    setTimeout(() => setFeedbackMsg(''), 4000);
  };

  const handleResetSandbox = async () => {
    if (confirm('Clear sandbox test data?')) {
      await initializeSandboxDatabase(false);
      setFeedbackMsg('Sandbox cleared successfully.');
      setTimeout(() => setFeedbackMsg(''), 2500);
    }
  };

  const accentPalette = [
    { label: 'Cyber Blue', hex: '#0284c7' },
    { label: 'Crimson Red', hex: '#ef4444' },
    { label: 'Electric Orange', hex: '#f97316' },
    { label: 'Cyber Purple', hex: '#8b5cf6' },
    { label: 'Emerald Green', hex: '#10b981' }
  ];

  const handleSaveApiKey = () => {
    geminiClient.setApiKey(apiKey);
    setKeySaved(true);
    setFeedbackMsg('Gemini API Key updated successfully!');
    setTimeout(() => {
      setKeySaved(false);
      setFeedbackMsg('');
    }, 2500);
  };

  const handleSaveSms = () => {
    localStorage.setItem('sms_provider', smsProvider);
    localStorage.setItem('fast2sms_api_key', fast2smsKey);
    setFeedbackMsg('SMS Gateway configuration saved!');
    setTimeout(() => setFeedbackMsg(''), 2500);
  };

  const handleClearToyData = async () => {
    if (confirm('Are you sure you want to clear simulated test data? Your real custom entries will be preserved.')) {
      await clearAllToyData();
      setFeedbackMsg('Simulated toy data wiped successfully.');
    }
  };

  const handleRestoreDemoData = async () => {
    if (confirm('Restore complete sample electrical ERP database (Customers, Services, Stock, Bills)?')) {
      await restoreDemoData();
      setFeedbackMsg('Demo database records restored!');
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12 max-w-4xl">
      <div>
        <h2 className="text-xl sm:text-2xl font-black tracking-tight">System Configuration & Preferences</h2>
        <p className="text-xs text-slate-500">
          Manage brand identity, custom accent colors, AI vision tokens, and local database ledger
        </p>
      </div>

      {feedbackMsg && (
        <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-400 flex items-center gap-2">
          <CheckCircle className="w-4 h-4 shrink-0" />
          <span>{feedbackMsg}</span>
        </div>
      )}

      {/* Brand Visual Identity */}
      <div className={`p-6 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <h3 className="font-extrabold text-sm text-slate-100 flex items-center gap-2 mb-4">
          <Palette className="w-4 h-4 text-blue-400" />
          <span>Dual-Brand Dynamic Aesthetic</span>
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div
            onClick={() => setBrandTheme('cyber_blue')}
            className={`p-4 rounded-2xl border-2 cursor-pointer transition-all flex items-center justify-between ${brandTheme === 'cyber_blue' ? 'border-blue-500 bg-blue-500/10 shadow-lg' : 'border-slate-800 hover:border-slate-700'}`}
          >
            <div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-blue-500" />
                <h4 className="font-black text-sm">ReLec PRO ERP</h4>
              </div>
              <p className="text-xs text-slate-400 mt-1">Industrial Cyber Blue Theme tailored for general electrical business.</p>
            </div>
            {brandTheme === 'cyber_blue' && <CheckCircle className="w-5 h-5 text-blue-400" />}
          </div>

          <div
            onClick={() => setBrandTheme('revolt_crimson')}
            className={`p-4 rounded-2xl border-2 cursor-pointer transition-all flex items-center justify-between ${brandTheme === 'revolt_crimson' ? 'border-red-500 bg-red-500/10 shadow-lg' : 'border-slate-800 hover:border-slate-700'}`}
          >
            <div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-red-500" />
                <h4 className="font-black text-sm">RE-VOLT Enterprise</h4>
              </div>
              <p className="text-xs text-slate-400 mt-1">Crimson Red High-Voltage Theme tailored for heavy repairs & switchgear.</p>
            </div>
            {brandTheme === 'revolt_crimson' && <CheckCircle className="w-5 h-5 text-red-400" />}
          </div>
        </div>

        {/* Custom Accent Palette */}
        <div className="mt-5 pt-4 border-t border-slate-800">
          <label className="block text-xs font-bold text-slate-400 mb-2">
            Custom Highlight Accent Color:
          </label>
          <div className="flex flex-wrap gap-2">
            {accentPalette.map(item => (
              <button
                key={item.hex}
                onClick={() => setAccentColor(item.hex)}
                className={`px-3 py-1.5 rounded-xl border text-xs font-bold flex items-center gap-2 transition-all ${accentColor === item.hex ? 'border-white bg-slate-800 text-white shadow-md' : 'border-slate-800 text-slate-400 hover:border-slate-700'}`}
              >
                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: item.hex }} />
                <span>{item.label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Gemini AI API Configuration */}
      <div className={`p-6 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <h3 className="font-extrabold text-sm text-slate-100 flex items-center gap-2 mb-2">
          <Sparkles className="w-4 h-4 text-amber-400" />
          <span>Google Gemini AI Vision & Multilingual NLP</span>
        </h3>
        <p className="text-xs text-slate-400 leading-relaxed mb-4">
          Powers optical character recognition for electrical bills, camera hardware classification, and voice entity extraction. An offline rule-based parser operates automatically even without an API key.
        </p>

        <div className="flex gap-2">
          <div className="relative flex-1">
            <Key className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="Enter GEMINI_API_KEY (AI Studio)"
              className={`w-full pl-10 pr-4 py-2.5 rounded-2xl text-xs font-mono border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
          </div>
          <button
            onClick={handleSaveApiKey}
            className="px-5 py-2.5 rounded-2xl text-xs font-bold text-white shadow-md shrink-0"
            style={{ backgroundColor: accentColor }}
          >
            Save Key
          </button>
        </div>
      </div>

      {/* SMS Gateway Settings */}
      <div className={`p-6 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <h3 className="font-extrabold text-sm text-slate-100 flex items-center gap-2 mb-2">
          <MessageSquare className="w-4 h-4 text-emerald-400" />
          <span>SMS & OTP Dispatch Gateway</span>
        </h3>
        <p className="text-xs text-slate-400 leading-relaxed mb-4">
          Configure Fast2SMS or Twilio webhooks for live phone SMS verification. In default test mode, 6-digit OTPs are automatically displayed on screen.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Gateway Provider</label>
            <select
              value={smsProvider}
              onChange={(e) => setSmsProvider(e.target.value)}
              className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            >
              <option value="fast2sms">Fast2SMS Gateway (India)</option>
              <option value="twilio">Twilio Cloud Telephony</option>
              <option value="simulation">Simulated Screen OTP (Test)</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">API Key / Token</label>
            <input
              type="text"
              value={fast2smsKey}
              onChange={(e) => setFast2smsKey(e.target.value)}
              placeholder="e.g. YOUR_FAST2SMS_KEY"
              className={`w-full px-4 py-2.5 rounded-2xl text-xs font-mono border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
          </div>
        </div>

        <button
          onClick={handleSaveSms}
          className="px-4 py-2 rounded-xl text-xs font-bold bg-slate-800 hover:bg-slate-700 text-slate-200"
        >
          Save Gateway Settings
        </button>
      </div>

      {/* Cloud Ledger & Real-Time Sync (Android ↔ Web) */}
      <div className={`p-6 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <div className="flex items-center justify-between mb-2">
          <h3 className="font-extrabold text-sm text-slate-100 flex items-center gap-2">
            <Cloud className="w-4 h-4 text-blue-400" />
            <span>Cloud Ledger & Real-Time Sync (Mobile App ↔ Website)</span>
          </h3>
          <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider ${
            syncState.mode === 'production'
              ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
              : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
          }`}>
            {syncState.mode === 'production' ? '● Production Cloud Active' : '● Demo Sandbox Mode'}
          </span>
        </div>

        <p className="text-xs text-slate-400 leading-relaxed mb-4">
          Connects the Android mobile app and this web platform in real-time. Changes made on either device automatically synchronize. Unapproved guest visitors are isolated to Sandbox mode to safeguard your production data.
        </p>

        {/* Sync Controls */}
        <div className="space-y-3">
          <div>
            <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              Cloud Sync REST Endpoint URL (Firebase / Cloud Ledger)
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={cloudSyncUrl}
                onChange={(e) => setCloudSyncUrl(e.target.value)}
                placeholder="https://your-project.firebaseio.com"
                className={`flex-1 px-4 py-2 rounded-xl text-xs font-mono border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
              <button
                onClick={handleSaveCloudEndpoint}
                className="px-4 py-2 rounded-xl text-xs font-bold border border-slate-700 bg-slate-800 text-slate-200 hover:bg-slate-700"
              >
                Save URL
              </button>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <button
              onClick={handleTriggerCloudSync}
              disabled={syncInProgress}
              className="px-4 py-2.5 rounded-xl text-xs font-bold text-white shadow-md flex items-center gap-2 transition-all hover:brightness-110 active:scale-95"
              style={{ backgroundColor: accentColor }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${syncInProgress ? 'animate-spin' : ''}`} />
              <span>{syncInProgress ? 'Synchronizing Cloud...' : 'Sync Cloud Ledger Now'}</span>
            </button>

            {syncState.lastSyncTime && (
              <span className="text-[11px] text-slate-400">
                Last Synced: <strong className="text-slate-200">{syncState.lastSyncTime}</strong>
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Database Management */}
      <div className={`p-6 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <h3 className="font-extrabold text-sm text-slate-100 flex items-center gap-2 mb-2">
          <Database className="w-4 h-4 text-purple-400" />
          <span>Local Dexie IndexedDB Ledger</span>
        </h3>
        <p className="text-xs text-slate-400 leading-relaxed mb-4">
          All records persist directly on your device with zero cloud dependency. You can reset simulated entries or restore initial demo records anytime.
        </p>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleClearToyData}
            className="px-4 py-2.5 rounded-xl text-xs font-bold bg-red-500/20 hover:bg-red-500/30 text-red-400 flex items-center gap-1.5 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            <span>Clear Simulated Toy Data</span>
          </button>
          <button
            onClick={handleRestoreDemoData}
            className="px-4 py-2.5 rounded-xl text-xs font-bold bg-purple-500/20 hover:bg-purple-500/30 text-purple-400 flex items-center gap-1.5 transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Restore Demo Records</span>
          </button>
        </div>
      </div>

      {/* Active User Session & Logout */}
      <div className={`p-6 rounded-3xl border shadow-xl flex items-center justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
        <div>
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block">Logged in as</span>
          <h4 className="text-base font-black text-slate-100">{currentSession?.displayName || 'User'}</h4>
          <p className="text-xs text-slate-400">{currentSession?.email || currentSession?.phone} · Role: {currentSession?.role}</p>
        </div>

        <button
          onClick={onLogout}
          className="px-5 py-2.5 rounded-2xl bg-red-600 hover:bg-red-500 text-white font-extrabold text-xs flex items-center gap-2 shadow-lg transition-all"
        >
          <LogOut className="w-4 h-4" />
          <span>Sign Out</span>
        </button>
      </div>
    </div>
  );
};
