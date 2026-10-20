import React, { useEffect } from 'react';
import { useTheme } from './theme';
import { Zap, ArrowRight, Loader2 } from 'lucide-react';

interface SplashScreenProps {
  onEnterPortal: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onEnterPortal }) => {
  const { isRevolt, accentColor, logoSrc } = useTheme();

  useEffect(() => {
    const timer = setTimeout(() => {
      onEnterPortal();
    }, 1800);
    return () => clearTimeout(timer);
  }, [onEnterPortal]);

  return (
    <div
      onClick={onEnterPortal}
      className="fixed inset-0 z-50 flex flex-col items-center justify-center cursor-pointer select-none overflow-hidden bg-gradient-to-b from-[#020617] via-[#0F172A] to-[#020617] p-6 text-white"
    >
      {/* Background glow */}
      <div
        className="absolute w-96 h-96 rounded-full blur-[120px] pointer-events-none opacity-25"
        style={{ backgroundColor: accentColor }}
      />

      {/* Animated Brand Shield */}
      <div className="relative group transition-all duration-700 transform hover:scale-105">
        <div
          className="w-28 h-28 rounded-3xl p-1 flex items-center justify-center border transition-all duration-500 shadow-2xl relative overflow-hidden backdrop-blur-xl animate-pulse"
          style={{
            borderColor: accentColor,
            boxShadow: `0 0 35px ${accentColor}40`
          }}
        >
          <img
            src={logoSrc}
            alt="Logo"
            className="w-full h-full object-cover rounded-2xl"
            onError={(e) => {
              // Fallback if image not ready
              (e.currentTarget as HTMLElement).style.display = 'none';
            }}
          />
        </div>
      </div>

      <div className="mt-8 text-center max-w-md">
        <p
          className="text-xs font-black tracking-[0.25em] uppercase mb-1"
          style={{ color: accentColor }}
        >
          {isRevolt ? 'REVOLT ENTERPRISE' : 'RATTAN ELECTRICALS'}
        </p>

        <div className="flex items-center justify-center gap-1.5 my-2">
          <h1 className="text-4xl font-black tracking-tight text-white">
            {isRevolt ? 'ReVolt' : 'ReLec'}
          </h1>
          <span
            className="w-2 h-2 rounded-full inline-block animate-ping"
            style={{ backgroundColor: accentColor }}
          />
        </div>

        <p className="text-xs text-slate-400 leading-relaxed mt-3 px-4">
          {isRevolt
            ? 'Smart Business. Total Control. High-voltage operations, synchronized electrical billing, service board & inventory.'
            : 'Enterprise ERP suite with real-time billing, service board, customer records, live inventory, and multi-role operations.'}
        </p>

        {/* Action Button */}
        <div className="mt-8 flex flex-col items-center gap-3">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onEnterPortal();
            }}
            className="flex items-center justify-center gap-2 px-8 py-3.5 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-white font-extrabold text-sm tracking-wider shadow-lg shadow-emerald-500/30 transition-all transform hover:-translate-y-0.5 active:translate-y-0"
          >
            <Zap className="w-4 h-4 fill-white" />
            <span>LET'S GO</span>
            <ArrowRight className="w-4 h-4" />
          </button>

          <div className="flex items-center gap-2 text-slate-500 text-[11px] mt-2">
            <Loader2 className="w-3.5 h-3.5 animate-spin text-emerald-400" />
            <span>Initializing secure local ledger...</span>
          </div>
        </div>
      </div>
    </div>
  );
};
