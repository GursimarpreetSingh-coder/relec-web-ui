import React, { useState, useEffect } from 'react';
import { useTheme } from '../theme';
import { db, DEFAULT_SUPPORT_INFO } from '../../data/database';
import { generateQrDataUrl } from '../../util/qrGenerator';
import type { StoreSupportInfo } from '../../types/models';
import {
  Headphones,
  Phone,
  MessageCircle,
  AlertTriangle,
  Clock,
  MapPin,
  QrCode,
  Printer,
  Save,
  CheckCircle,
  ExternalLink,
  ShieldAlert,
  X
} from 'lucide-react';

export const SupportDeskScreen: React.FC = () => {
  const { accentColor, isDarkTheme, brandName, logoSrc } = useTheme();

  const [supportInfo, setSupportInfo] = useState<StoreSupportInfo>(DEFAULT_SUPPORT_INFO);
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>('');
  const [showStandeeModal, setShowStandeeModal] = useState(false);
  const [savedSuccess, setSavedSuccess] = useState(false);

  // Load support info from DB
  useEffect(() => {
    async function load() {
      const rec = await db.settings.get('storeSupportInfo');
      if (rec && rec.value) {
        setSupportInfo(rec.value);
        const qrUrl = await generateQrDataUrl(rec.value.appDownloadUrl || window.location.origin + '/?view=customer');
        setQrCodeDataUrl(qrUrl);
      } else {
        const qrUrl = await generateQrDataUrl(DEFAULT_SUPPORT_INFO.appDownloadUrl);
        setQrCodeDataUrl(qrUrl);
      }
    }
    load();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    await db.settings.put({ key: 'storeSupportInfo', value: supportInfo });
    const qrUrl = await generateQrDataUrl(supportInfo.appDownloadUrl);
    setQrCodeDataUrl(qrUrl);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2500);
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Support Desk & Counter Standee Hub</h2>
          <p className="text-xs text-slate-500">
            Control customer contact details, 24/7 emergency hotline, and printable counter standee QR code
          </p>
        </div>

        <button
          onClick={() => setShowStandeeModal(true)}
          className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all self-start sm:self-auto"
          style={{ backgroundColor: accentColor }}
        >
          <Printer className="w-4 h-4" />
          <span>Print Shop Counter Standee</span>
        </button>
      </div>

      {savedSuccess && (
        <div className="p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-400 flex items-center gap-2">
          <CheckCircle className="w-4 h-4" />
          <span>Support Desk details & QR code destination successfully updated!</span>
        </div>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Support Desk Configuration Form */}
        <form onSubmit={handleSave} className={`lg:col-span-2 p-6 rounded-3xl border shadow-xl space-y-4 ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <h3 className="font-extrabold text-sm text-slate-100 uppercase tracking-wider mb-2">
            Store Contact Details & Working Hours
          </h3>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
              Store Support Desk Title
            </label>
            <input
              type="text"
              required
              value={supportInfo.storeName}
              onChange={(e) => setSupportInfo({ ...supportInfo, storeName: e.target.value })}
              className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                Helpdesk Phone
              </label>
              <input
                type="tel"
                required
                value={supportInfo.phone}
                onChange={(e) => setSupportInfo({ ...supportInfo, phone: e.target.value })}
                className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                WhatsApp Chat Number
              </label>
              <input
                type="tel"
                required
                value={supportInfo.whatsapp}
                onChange={(e) => setSupportInfo({ ...supportInfo, whatsapp: e.target.value })}
                className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                24/7 Emergency Helpline
              </label>
              <input
                type="text"
                required
                value={supportInfo.emergencyHelpline}
                onChange={(e) => setSupportInfo({ ...supportInfo, emergencyHelpline: e.target.value })}
                className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                Store Operating Hours
              </label>
              <input
                type="text"
                required
                value={supportInfo.workingHours}
                onChange={(e) => setSupportInfo({ ...supportInfo, workingHours: e.target.value })}
                className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
              Shop Physical Address
            </label>
            <input
              type="text"
              required
              value={supportInfo.address}
              onChange={(e) => setSupportInfo({ ...supportInfo, address: e.target.value })}
              className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
              Target Customer Portal / QR Destination URL
            </label>
            <input
              type="url"
              required
              value={supportInfo.appDownloadUrl}
              onChange={(e) => setSupportInfo({ ...supportInfo, appDownloadUrl: e.target.value })}
              placeholder="https://your-domain.com/?view=customer"
              className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
              Electrical Safety Guidelines
            </label>
            <textarea
              rows={4}
              value={supportInfo.safetyGuidelines}
              onChange={(e) => setSupportInfo({ ...supportInfo, safetyGuidelines: e.target.value })}
              className={`w-full px-4 py-2.5 rounded-2xl text-xs border focus:outline-none leading-relaxed ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
          </div>

          <button
            type="submit"
            className="w-full py-3 rounded-2xl font-extrabold text-xs text-white shadow-lg flex items-center justify-center gap-2"
            style={{ backgroundColor: accentColor }}
          >
            <Save className="w-4 h-4" />
            <span>Save & Update QR Standee</span>
          </button>
        </form>

        {/* Live Scannable Standee Preview */}
        <div className={`p-6 rounded-3xl border shadow-xl flex flex-col items-center justify-between text-center ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}>
          <div>
            <div
              className="w-16 h-16 rounded-2xl p-1 mx-auto mb-3 border shadow-md flex items-center justify-center"
              style={{ borderColor: `${accentColor}50` }}
            >
              <img src={logoSrc} alt="Logo" className="w-full h-full object-cover rounded-xl" />
            </div>

            <h3 className="font-black text-sm text-slate-100">{supportInfo.storeName}</h3>
            <p className="text-[11px] text-slate-500 mt-0.5">Scan to Book Repairs or Track Job</p>

            {/* QR Image */}
            <div className="my-5 p-3 rounded-2xl bg-white border border-slate-200 shadow-xl inline-block">
              {qrCodeDataUrl ? (
                <img src={qrCodeDataUrl} alt="QR Code" className="w-44 h-44 object-contain" />
              ) : (
                <div className="w-44 h-44 flex items-center justify-center text-slate-400">Generating...</div>
              )}
            </div>

            <p className="text-[11px] font-mono text-slate-400 line-clamp-1 max-w-[200px] mx-auto">
              {supportInfo.appDownloadUrl}
            </p>
          </div>

          <div className="w-full mt-6 space-y-2">
            <button
              onClick={() => setShowStandeeModal(true)}
              className="w-full py-2.5 rounded-xl border border-slate-700 hover:bg-slate-800 text-xs font-bold text-slate-200 flex items-center justify-center gap-2 transition-colors"
            >
              <Printer className="w-4 h-4 text-blue-400" />
              <span>Preview Printable Standee</span>
            </button>
            <a
              href={supportInfo.appDownloadUrl}
              target="_blank"
              rel="noreferrer"
              className="w-full py-2.5 rounded-xl bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 text-xs font-bold flex items-center justify-center gap-2 transition-colors"
            >
              <ExternalLink className="w-3.5 h-3.5" />
              <span>Test Customer URL</span>
            </a>
          </div>
        </div>
      </div>

      {/* Printable Counter Standee Poster Modal */}
      {showStandeeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
          <div className="w-full max-w-md rounded-3xl bg-white text-slate-900 p-8 shadow-2xl relative border-4 border-slate-900 print:p-0 print:border-none print:shadow-none">
            <button
              onClick={() => setShowStandeeModal(false)}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 print:hidden"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Standee Content */}
            <div className="text-center">
              <div className="w-16 h-16 rounded-2xl mx-auto mb-2 overflow-hidden border border-slate-200">
                <img src={logoSrc} alt="Logo" className="w-full h-full object-cover" />
              </div>

              <h2 className="text-xl font-black uppercase tracking-tight text-slate-900">
                {supportInfo.storeName}
              </h2>
              <p className="text-xs font-bold text-blue-600 uppercase tracking-widest mt-0.5">
                Rattan Electricals Customer Portal
              </p>

              {/* Scannable Frame */}
              <div className="my-5 p-4 rounded-3xl bg-slate-50 border-2 border-dashed border-slate-300 inline-block">
                {qrCodeDataUrl && (
                  <img src={qrCodeDataUrl} alt="Standee QR" className="w-48 h-48 object-contain mx-auto" />
                )}
                <p className="text-xs font-black uppercase tracking-wider text-slate-800 mt-2">
                  Scan to Book Electrician
                </p>
                <p className="text-[10px] text-slate-500">Live Repair Tracking · Zero Sign Up</p>
              </div>

              {/* Contact Footer */}
              <div className="mt-2 pt-3 border-t-2 border-slate-900 text-xs text-left space-y-1">
                <p><strong>Emergency Hotline:</strong> {supportInfo.emergencyHelpline}</p>
                <p><strong>WhatsApp Desk:</strong> +91 {supportInfo.whatsapp}</p>
                <p><strong>Working Hours:</strong> {supportInfo.workingHours}</p>
                <p><strong>Shop Location:</strong> {supportInfo.address}</p>
              </div>

              {/* Print Action */}
              <div className="mt-6 flex justify-center print:hidden">
                <button
                  onClick={() => window.print()}
                  className="px-6 py-2.5 rounded-xl bg-slate-900 text-white font-bold text-xs flex items-center gap-2 shadow-lg hover:bg-slate-800"
                >
                  <Printer className="w-4 h-4" />
                  <span>Print Counter Poster</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
