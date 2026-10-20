import React, { useState } from 'react';
import { useTheme } from './theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db } from '../data/database';
import { geminiClient } from '../data/geminiClient';
import { voiceRecognizer } from '../util/voiceRecognition';
import type { ServiceRequest, Priority, UserSession } from '../types/models';
import {
  Wrench,
  PlusCircle,
  Receipt,
  Phone,
  MessageCircle,
  AlertTriangle,
  Clock,
  MapPin,
  Calendar,
  CheckCircle,
  Mic,
  Sun,
  Moon,
  LogOut,
  Camera,
  ExternalLink,
  ShieldCheck,
  ChevronRight,
  Printer,
  Download
} from 'lucide-react';

interface CustomerPublicPortalProps {
  session?: UserSession | null;
  onNavigateBack: () => void;
}

export const CustomerPublicPortal: React.FC<CustomerPublicPortalProps> = ({
  session,
  onNavigateBack
}) => {
  const { accentColor, isDarkTheme, toggleDarkTheme, brandName, logoSrc, isRevolt } = useTheme();

  const services = useLiveQuery(() => db.services.toArray()) || [];
  const bills = useLiveQuery(() => db.bills.toArray()) || [];
  const supportRec = useLiveQuery(() => db.settings.get('storeSupportInfo'));
  const supportInfo = supportRec?.value || {
    storeName: 'Rattan Electricals Helpdesk',
    phone: '9876543210',
    whatsapp: '9876543210',
    address: 'Shop #12, Main Electrical Market, Near Clock Tower',
    workingHours: 'Mon - Sat (9:00 AM – 8:30 PM)',
    emergencyHelpline: '+91 98765 43210',
    safetyGuidelines: 'In case of smoke or burning smell, instantly switch off the Main MCB.\nNever touch appliance switches with wet hands or barefoot on wet floors.',
    appDownloadUrl: window.location.origin + '/?view=customer'
  };

  const [activeTab, setActiveTab] = useState<number>(0); // 0: My Jobs, 1: Book Repair, 2: My Bills, 3: Support
  const [customerPhoneFilter, setCustomerPhoneFilter] = useState(session?.phone || '9417255831');

  // Book repair form
  const [custName, setCustName] = useState(session?.displayName || '');
  const [custPhone, setCustPhone] = useState(session?.phone || '');
  const [custAddress, setCustAddress] = useState('');
  const [problem, setProblem] = useState('');
  const [category, setCategory] = useState('Wiring Check');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [preferredDateTime, setPreferredDateTime] = useState('Today 11:30 AM');
  const [photoUri, setPhotoUri] = useState<string | null>(null);

  // Voice AI Complaint Assistant
  const [isVoiceListening, setIsVoiceListening] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState('');
  const [bookingSuccessMsg, setBookingSuccessMsg] = useState('');

  // Filter customer jobs and bills
  const customerCleanPhone = customerPhoneFilter.replace(/\D/g, '');
  const myJobs = services.filter(s =>
    customerCleanPhone ? s.customerPhone.replace(/\D/g, '').endsWith(customerCleanPhone.slice(-10)) : true
  );
  const myBills = bills.filter(b =>
    customerCleanPhone ? b.customerPhone.replace(/\D/g, '').endsWith(customerCleanPhone.slice(-10)) : true
  );

  // Handle Photo input
  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => setPhotoUri(event.target?.result as string);
      reader.readAsDataURL(file);
    }
  };

  // AI Voice Complaint capture
  const handleVoiceComplaint = () => {
    setIsVoiceListening(true);
    setVoiceStatus('Listening... Describe electrical spark, fan, MCB, or wiring problem:');
    voiceRecognizer.startListening(
      async (transcript) => {
        setIsVoiceListening(false);
        setVoiceStatus('Extracting problem with Gemini Multilingual AI...');
        setProblem(transcript);
        const parsed = await geminiClient.parseMultilingualVoiceIntent(transcript);
        if (parsed.category) setCategory(parsed.category);
        setVoiceStatus('Voice complaint details auto-populated!');
        setTimeout(() => setVoiceStatus(''), 3000);
      },
      (err) => {
        setIsVoiceListening(false);
        setVoiceStatus(err);
      },
      () => setIsVoiceListening(false)
    );
  };

  // Submit service booking
  const handleBookService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!custName.trim() || !custPhone.trim() || !problem.trim()) return;

    const newTicket: ServiceRequest = {
      id: 'S_' + Date.now().toString(36),
      customerName: custName.trim(),
      customerPhone: custPhone.trim(),
      customerAddress: custAddress.trim() || 'Customer Site Address',
      problemDescription: problem.trim(),
      category,
      priority,
      assignedWorkerId: null,
      assignedWorkerName: null,
      status: 'CREATED',
      preferredDateTime,
      photoUri,
      dateCreated: new Date().toISOString().split('T')[0],
      statusHistory: [
        {
          status: 'CREATED',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          notes: 'Customer booked via Self-Service Online Portal'
        }
      ]
    };

    await db.services.put(newTicket);
    setCustomerPhoneFilter(custPhone.trim());
    setBookingSuccessMsg(`Repair request booked successfully! Tracking ID: #${newTicket.id}`);
    setActiveTab(0); // switch to My Jobs to watch tracking pipeline

    // Reset
    setProblem('');
    setPhotoUri(null);
  };

  // Stage progress helper
  const getStageStep = (status: ServiceRequest['status']) => {
    switch (status) {
      case 'CREATED': return 1;
      case 'ASSIGNED': return 2;
      case 'IN_PROGRESS': return 3;
      case 'COMPLETED': return 4;
      case 'CLOSED': return 5;
      default: return 1;
    }
  };

  return (
    <div className={`min-h-screen flex flex-col transition-colors duration-300 ${isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'}`}>
      {/* Top Header */}
      <header className={`sticky top-0 z-40 px-4 sm:px-6 py-3 border-b backdrop-blur-xl flex items-center justify-between transition-colors ${isDarkTheme ? 'bg-slate-950/85 border-slate-800/80 shadow-md' : 'bg-white/90 border-slate-200 shadow-sm'}`}>
        <div className="flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-xl p-0.5 border flex items-center justify-center overflow-hidden shrink-0 shadow-sm"
            style={{ borderColor: `${accentColor}50`, backgroundColor: isRevolt ? 'transparent' : '#FFFFFF' }}
          >
            <img src={logoSrc} alt="Logo" className="w-full h-full object-cover rounded-lg" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-black text-base tracking-tight leading-none">
                {supportInfo.storeName}
              </span>
              <span
                className="px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-wider text-white"
                style={{ backgroundColor: accentColor }}
              >
                PUBLIC DESK
              </span>
            </div>
            <p className="text-[11px] font-medium text-slate-500 mt-0.5">
              Rattan Electricals Customer Service Portal
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* APK Direct Download Button */}
          <a
            href="./downloads/relec-pro-erp.apk"
            download="relec-pro-erp.apk"
            className="p-2 rounded-xl border transition-all bg-emerald-500/10 border-emerald-500/30 text-emerald-500 hover:bg-emerald-500/20"
            title="Download Android Mobile App (APK)"
          >
            <Download className="w-4 h-4" />
          </a>

          <button
            onClick={toggleDarkTheme}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-blue-400' : 'bg-slate-100 border-slate-200 text-blue-600'}`}
          >
            {isDarkTheme ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
          <button
            onClick={onNavigateBack}
            className="p-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 hover:text-white transition-all text-xs font-bold flex items-center gap-1.5"
            title="Exit / Staff Login"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Staff Sign In</span>
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 sm:p-6 space-y-6 pb-24">
        {/* Segmented Tabs Bar matching Jetpack Compose */}
        <div className={`grid grid-cols-4 p-1 rounded-2xl text-xs font-bold border ${isDarkTheme ? 'bg-slate-900 border-slate-800' : 'bg-slate-200/80 border-slate-300'}`}>
          <button
            onClick={() => setActiveTab(0)}
            className={`py-2.5 rounded-xl transition-all flex items-center justify-center gap-1.5 ${activeTab === 0 ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-200'}`}
            style={{ color: activeTab === 0 ? accentColor : undefined }}
          >
            <Wrench className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">My Jobs</span>
            <span className="sm:hidden">Jobs</span>
          </button>

          <button
            onClick={() => setActiveTab(1)}
            className={`py-2.5 rounded-xl transition-all flex items-center justify-center gap-1.5 ${activeTab === 1 ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-200'}`}
            style={{ color: activeTab === 1 ? accentColor : undefined }}
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Book Repair</span>
            <span className="sm:hidden">Book</span>
          </button>

          <button
            onClick={() => setActiveTab(2)}
            className={`py-2.5 rounded-xl transition-all flex items-center justify-center gap-1.5 ${activeTab === 2 ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-200'}`}
            style={{ color: activeTab === 2 ? accentColor : undefined }}
          >
            <Receipt className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">My Bills</span>
            <span className="sm:hidden">Bills</span>
          </button>

          <button
            onClick={() => setActiveTab(3)}
            className={`py-2.5 rounded-xl transition-all flex items-center justify-center gap-1.5 ${activeTab === 3 ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-200'}`}
            style={{ color: activeTab === 3 ? accentColor : undefined }}
          >
            <Phone className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Support</span>
            <span className="sm:hidden">Help</span>
          </button>
        </div>

        {bookingSuccessMsg && (
          <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-400 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckCircle className="w-4 h-4" />
              <span>{bookingSuccessMsg}</span>
            </div>
            <button onClick={() => setBookingSuccessMsg('')} className="text-slate-400 hover:text-white">✕</button>
          </div>
        )}

        {/* ======================================================== */}
        {/* TAB 0: LIVE REPAIR TRACKING PIPELINE                     */}
        {/* ======================================================== */}
        {activeTab === 0 && (
          <div className="space-y-6">
            {/* Mobile Filter Bar */}
            <div className={`p-4 rounded-2xl border flex items-center justify-between gap-3 ${isDarkTheme ? 'bg-slate-900/60 border-slate-800' : 'bg-white border-slate-200'}`}>
              <div className="flex items-center gap-2 flex-1">
                <span className="text-xs font-bold text-slate-500">Filter by Mobile:</span>
                <input
                  type="tel"
                  value={customerPhoneFilter}
                  onChange={(e) => setCustomerPhoneFilter(e.target.value)}
                  placeholder="e.g. 9417255831"
                  className={`px-3 py-1.5 rounded-xl text-xs font-mono border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>
              <button
                onClick={() => setActiveTab(1)}
                className="px-3 py-1.5 rounded-xl text-xs font-bold text-white shadow-md"
                style={{ backgroundColor: accentColor }}
              >
                + Book New
              </button>
            </div>

            {myJobs.length === 0 ? (
              <div className={`p-12 text-center rounded-3xl border ${isDarkTheme ? 'bg-slate-900/40 border-slate-800' : 'bg-white border-slate-200'}`}>
                <Wrench className="w-8 h-8 mx-auto text-slate-500 mb-2" />
                <h3 className="font-extrabold text-sm text-slate-200">No Active Repair Tickets Found</h3>
                <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                  No electrical job cards currently registered under mobile {customerPhoneFilter}. Tap "Book Repair" to schedule an electrician.
                </p>
                <button
                  onClick={() => setActiveTab(1)}
                  className="mt-4 px-6 py-2.5 rounded-xl text-xs font-black text-white shadow-lg"
                  style={{ backgroundColor: accentColor }}
                >
                  Book Electrician Now
                </button>
              </div>
            ) : (
              myJobs.map(job => {
                const step = getStageStep(job.status);
                const stepsList = ['Created', 'Assigned', 'In Progress', 'Completed'];

                return (
                  <div
                    key={job.id}
                    className={`p-6 rounded-3xl border shadow-xl space-y-5 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
                  >
                    {/* Header */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${
                            job.priority === 'EMERGENCY' ? 'bg-red-500/20 text-red-400 animate-pulse' :
                            'bg-blue-500/20 text-blue-400'
                          }`}>
                            {job.priority} Priority
                          </span>
                          <span className="text-xs text-slate-400 font-bold">· {job.category}</span>
                        </div>
                        <h3 className="text-base font-extrabold text-slate-100 mt-1">
                          {job.problemDescription}
                        </h3>
                      </div>

                      <span className={`self-start sm:self-auto px-3 py-1 rounded-xl text-xs font-black uppercase tracking-wider ${
                        job.status === 'COMPLETED' ? 'bg-emerald-500/20 text-emerald-400' :
                        job.status === 'IN_PROGRESS' ? 'bg-blue-500/20 text-blue-400' :
                        'bg-amber-500/20 text-amber-400'
                      }`}>
                        {job.status.replace('_', ' ')}
                      </span>
                    </div>

                    {/* Interactive Visual Stage Pipeline */}
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        {stepsList.map((st, idx) => {
                          const isDone = step >= idx + 1;
                          const isCurrent = step === idx + 1;
                          return (
                            <div key={st} className="flex flex-col items-center flex-1 text-center">
                              <div
                                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-black transition-all ${isDone ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/30' : 'bg-slate-800 text-slate-500'}`}
                              >
                                {isDone ? '✓' : idx + 1}
                              </div>
                              <span className={`text-[10px] font-bold mt-1 ${isCurrent ? 'text-emerald-400' : isDone ? 'text-slate-300' : 'text-slate-600'}`}>
                                {st}
                              </span>
                            </div>
                          );
                        })}
                      </div>

                      <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                        <div
                          className="h-full bg-emerald-500 transition-all duration-700"
                          style={{ width: `${Math.min(100, (step / 4) * 100)}%` }}
                        />
                      </div>
                    </div>

                    {/* Technician & Appointment Info */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs bg-slate-950/60 p-3.5 rounded-2xl border border-slate-800/80">
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-500 block">Assigned Technician</span>
                        <p className="font-extrabold text-slate-200 mt-0.5">
                          {job.assignedWorkerName ? `⚡ ${job.assignedWorkerName} (Rattan Electricals)` : 'Searching nearest available electrician...'}
                        </p>
                      </div>
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-500 block">Scheduled Time</span>
                        <p className="font-semibold text-slate-300 mt-0.5">{job.preferredDateTime}</p>
                      </div>
                      <div className="sm:col-span-2">
                        <span className="text-[10px] uppercase font-bold text-slate-500 block">Service Address</span>
                        <p className="text-slate-400 mt-0.5">{job.customerAddress}</p>
                      </div>
                    </div>

                    {/* Latest Stage Notes */}
                    {job.statusHistory.length > 0 && (
                      <div className="text-xs text-slate-500 italic flex items-center gap-2">
                        <Clock className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                        <span>Latest Update: {job.statusHistory[job.statusHistory.length - 1].notes} ({job.statusHistory[job.statusHistory.length - 1].timestamp})</span>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        )}

        {/* ======================================================== */}
        {/* TAB 1: ZERO BARRIER BOOK SERVICE REPAIR                   */}
        {/* ======================================================== */}
        {activeTab === 1 && (
          <div className={`p-6 sm:p-8 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
            <div className="mb-5">
              <h2 className="text-xl font-black text-slate-100">Schedule Electrical Service</h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Our licensed electricians arrive with certified tools and genuine replacement parts.
              </p>
            </div>

            <form onSubmit={handleBookService} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                    Your Name
                  </label>
                  <input
                    type="text"
                    required
                    value={custName}
                    onChange={(e) => setCustName(e.target.value)}
                    placeholder="e.g. Anita Sharma"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                    Mobile Number (+91)
                  </label>
                  <input
                    type="tel"
                    required
                    maxLength={10}
                    value={custPhone}
                    onChange={(e) => setCustPhone(e.target.value)}
                    placeholder="9417255831"
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Home / Business Address
                </label>
                <input
                  type="text"
                  required
                  value={custAddress}
                  onChange={(e) => setCustAddress(e.target.value)}
                  placeholder="H.No 142, Sector 15, Chandigarh"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              {/* Problem Description with AI Voice Assistant Button */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Electrical Issue Description
                  </label>
                  <button
                    type="button"
                    onClick={handleVoiceComplaint}
                    className="text-xs font-bold flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-red-500/15 text-red-400 hover:bg-red-500/25 transition-all"
                  >
                    <Mic className={`w-3.5 h-3.5 ${isVoiceListening ? 'animate-pulse text-red-500' : ''}`} />
                    <span>{isVoiceListening ? 'Listening...' : 'Voice Dictate'}</span>
                  </button>
                </div>

                {voiceStatus && (
                  <p className="text-[11px] text-amber-400 mb-1.5 animate-fadeIn">
                    {voiceStatus}
                  </p>
                )}

                <textarea
                  rows={3}
                  required
                  value={problem}
                  onChange={(e) => setProblem(e.target.value)}
                  placeholder="e.g. Main MCB tripping repeatedly when AC is switched on. Burning smell from DB board."
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                    Repair Category
                  </label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  >
                    <option value="Wiring Check">Wiring Check / Short Circuit</option>
                    <option value="DB Board Repair">DB Board Repair / MCB Tripping</option>
                    <option value="Fan Installation">Fan Installation / Replacement</option>
                    <option value="Motor Repair">Water Pump Motor Repair</option>
                    <option value="Lighting Fittings">Lighting Fittings / LED Panels</option>
                    <option value="Inverter Setup">Inverter & Battery Wiring</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                    Urgency Priority
                  </label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as any)}
                    className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  >
                    <option value="LOW">Low (Routine Checkup)</option>
                    <option value="MEDIUM">Medium (General Service)</option>
                    <option value="HIGH">High (Urgent Repair)</option>
                    <option value="EMERGENCY">Emergency (Sparking / No Power)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Preferred Appointment Time
                </label>
                <input
                  type="text"
                  value={preferredDateTime}
                  onChange={(e) => setPreferredDateTime(e.target.value)}
                  placeholder="e.g. Today 04:00 PM or Tomorrow morning"
                  className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
              </div>

              {/* Photo Upload Attachment */}
              <div>
                <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Attach Photo of Damaged Board / Switch (Optional)
                </label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handlePhotoUpload}
                  className="text-xs text-slate-400 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-bold file:bg-slate-800 file:text-slate-200 hover:file:bg-slate-700"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3.5 rounded-2xl font-black text-sm text-white tracking-wider shadow-xl transition-all transform hover:-translate-y-0.5 active:translate-y-0"
                style={{ backgroundColor: accentColor, boxShadow: `0 8px 25px ${accentColor}40` }}
              >
                SUBMIT & DISPATCH ELECTRICIAN
              </button>
            </form>
          </div>
        )}

        {/* ======================================================== */}
        {/* TAB 2: CUSTOMER INVOICES & BILLS                          */}
        {/* ======================================================== */}
        {activeTab === 2 && (
          <div className="space-y-4">
            {myBills.length === 0 ? (
              <div className={`p-12 text-center rounded-3xl border ${isDarkTheme ? 'bg-slate-900/40 border-slate-800' : 'bg-white border-slate-200'}`}>
                <Receipt className="w-8 h-8 mx-auto text-slate-500 mb-2" />
                <h3 className="font-extrabold text-sm text-slate-200">No Invoices Found</h3>
                <p className="text-xs text-slate-500 mt-1">
                  No electrical billing records registered for mobile {customerPhoneFilter}.
                </p>
              </div>
            ) : (
              myBills.map(bill => (
                <div
                  key={bill.id}
                  className={`p-6 rounded-3xl border shadow-xl space-y-4 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-[10px] font-mono font-bold text-slate-500">{bill.invoiceNumber}</span>
                      <h3 className="text-base font-black text-slate-100 mt-0.5">Rattan Electricals Tax Invoice</h3>
                      <p className="text-xs text-slate-400">Date: {bill.date} · Payment: {bill.paymentMethod}</p>
                    </div>

                    <span className="px-3 py-1 rounded-xl text-xs font-black uppercase tracking-wider bg-emerald-500/20 text-emerald-400">
                      {bill.paymentStatus}
                    </span>
                  </div>

                  {/* Items List */}
                  <div className="space-y-1.5 pt-2 border-t border-slate-800 text-xs">
                    {bill.items.map((it, idx) => (
                      <div key={idx} className="flex justify-between py-1 border-b border-slate-800/40 text-slate-300">
                        <span>{it.name} (x{it.qty})</span>
                        <span className="font-mono font-bold">₹{it.total.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>

                  <div className="pt-2 flex justify-between items-center text-sm font-black text-slate-100">
                    <span>Grand Total:</span>
                    <span className="text-base font-black text-emerald-400 font-mono">
                      ₹{bill.totalAmount.toFixed(2)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* ======================================================== */}
        {/* TAB 3: CONTACT DESK & SAFETY GUIDELINES                  */}
        {/* ======================================================== */}
        {activeTab === 3 && (
          <div className="space-y-6">
            {/* Quick Action Dialers */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <a
                href={`tel:${supportInfo.phone}`}
                className="p-5 rounded-3xl bg-blue-600/15 border border-blue-500/30 text-blue-400 flex flex-col items-center justify-center text-center gap-2 hover:bg-blue-600/25 transition-all shadow-lg"
              >
                <Phone className="w-6 h-6" />
                <span className="font-extrabold text-sm">Call Shop Helpdesk</span>
                <span className="text-xs font-mono text-slate-300">+91 {supportInfo.phone}</span>
              </a>

              <a
                href={`https://wa.me/91${supportInfo.whatsapp}?text=Hello%20Rattan%20Electricals,%20I%20need%20assistance.`}
                target="_blank"
                rel="noreferrer"
                className="p-5 rounded-3xl bg-emerald-600/15 border border-emerald-500/30 text-emerald-400 flex flex-col items-center justify-center text-center gap-2 hover:bg-emerald-600/25 transition-all shadow-lg"
              >
                <MessageCircle className="w-6 h-6" />
                <span className="font-extrabold text-sm">WhatsApp Assistance</span>
                <span className="text-xs font-mono text-slate-300">Chat with Technician</span>
              </a>

              <a
                href={`tel:${supportInfo.emergencyHelpline}`}
                className="p-5 rounded-3xl bg-red-600/15 border border-red-500/30 text-red-400 flex flex-col items-center justify-center text-center gap-2 hover:bg-red-600/25 transition-all shadow-lg"
              >
                <AlertTriangle className="w-6 h-6" />
                <span className="font-extrabold text-sm">24/7 Breakdown Hotline</span>
                <span className="text-xs font-mono text-slate-300">{supportInfo.emergencyHelpline}</span>
              </a>
            </div>

            {/* Shop Details */}
            <div className={`p-6 rounded-3xl border shadow-xl space-y-4 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
              <h3 className="font-black text-sm uppercase tracking-wider text-slate-400">
                Store Location & Hours
              </h3>
              <div className="space-y-3 text-xs text-slate-300">
                <div className="flex items-start gap-2.5">
                  <MapPin className="w-4 h-4 text-blue-400 shrink-0 mt-0.5" />
                  <div>
                    <strong className="block text-slate-200">Rattan Electricals Enterprise Traders</strong>
                    <span>{supportInfo.address}</span>
                  </div>
                </div>

                <div className="flex items-start gap-2.5">
                  <Clock className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <div>
                    <strong className="block text-slate-200">Operating Counter Hours</strong>
                    <span>{supportInfo.workingHours}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Electrical Safety Checklist */}
            <div className={`p-6 rounded-3xl border shadow-xl ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
              <div className="flex items-center gap-2 mb-3">
                <ShieldCheck className="w-5 h-5 text-amber-400" />
                <h3 className="font-black text-sm text-slate-100">Certified Electrical Safety Guidelines</h3>
              </div>
              <p className="text-xs text-slate-400 whitespace-pre-line leading-relaxed">
                {supportInfo.safetyGuidelines}
              </p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
