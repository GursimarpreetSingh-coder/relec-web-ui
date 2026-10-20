import React, { useState } from 'react';
import { useTheme } from '../theme';
import { geminiClient } from '../../data/geminiClient';
import { voiceRecognizer } from '../../util/voiceRecognition';
import { db } from '../../data/database';
import type { ParsedVoiceIntent } from '../../types/models';
import {
  X,
  Mic,
  MicOff,
  Sparkles,
  CheckCircle,
  PlusCircle,
  ArrowRight,
  Loader2,
  Volume2
} from 'lucide-react';

interface AIVoiceEntryModalProps {
  onClose: () => void;
  onNavigateToSection?: (section: string) => void;
}

export const AIVoiceEntryModal: React.FC<AIVoiceEntryModalProps> = ({ onClose, onNavigateToSection }) => {
  const { accentColor, isDarkTheme } = useTheme();

  const [transcript, setTranscript] = useState('');
  const [isListening, setIsListening] = useState(false);
  const [selectedLang, setSelectedLang] = useState<'en-IN' | 'hi-IN' | 'pa-IN'>('en-IN');
  const [isProcessing, setIsProcessing] = useState(false);
  const [parsedIntent, setParsedIntent] = useState<ParsedVoiceIntent | null>(null);
  const [statusMessage, setStatusMessage] = useState('');

  // Sample voice prompts for testing
  const samplePrompts = [
    'Add 10 Havells 1.5mm wire at 1850 rupees',
    'Chai nashta kharcha 85 rupaye likho',
    'Add 20 Anchor modular switches each 25 rs',
    'Panj Orient ceiling fan daalo rate 2100',
    'Navigate to billing'
  ];

  const handleStartListening = () => {
    setIsListening(true);
    setStatusMessage('Listening to your microphone... Speak clearly');
    voiceRecognizer.setLanguage(selectedLang);
    voiceRecognizer.startListening(
      (text) => {
        setTranscript(text);
        setIsListening(false);
        processTranscript(text);
      },
      (err) => {
        setIsListening(false);
        setStatusMessage(err);
      },
      () => {
        setIsListening(false);
      }
    );
  };

  const handleStopListening = () => {
    voiceRecognizer.stopListening();
    setIsListening(false);
  };

  const processTranscript = async (text: string) => {
    if (!text.trim()) return;
    setIsProcessing(true);
    setStatusMessage('Extracting intent & parameters via Multilingual AI...');

    try {
      const result = await geminiClient.parseMultilingualVoiceIntent(text);
      setParsedIntent(result);
      setStatusMessage(`Detected ${result.detectedLanguage.toUpperCase()} intent: ${result.action}`);
    } catch (err: any) {
      setStatusMessage('Error parsing voice: ' + err.message);
    } finally {
      setIsProcessing(false);
    }
  };

  // Quick Action execution
  const handleExecuteIntent = async () => {
    if (!parsedIntent) return;

    if (parsedIntent.action === 'LOG_EXPENSE') {
      await db.expenses.put({
        id: 'EXP_' + Date.now().toString(36),
        category: parsedIntent.category || 'Tea & Refreshments',
        title: parsedIntent.itemName,
        amount: parsedIntent.totalPrice || parsedIntent.unitPrice,
        quantity: parsedIntent.quantity,
        date: new Date().toISOString().split('T')[0],
        voiceSummary: parsedIntent.rawTranscript
      });
      setStatusMessage('Expense saved to ledger!');
      if (onNavigateToSection) {
        setTimeout(() => {
          onNavigateToSection('Expenses');
          onClose();
        }, 1000);
      }
    } else if (parsedIntent.action === 'NAVIGATE' && parsedIntent.targetScreen) {
      if (onNavigateToSection) {
        onNavigateToSection(parsedIntent.targetScreen);
        onClose();
      }
    } else {
      // Add to inventory default
      await db.inventory.put({
        id: 'INV_' + Date.now().toString(36),
        name: parsedIntent.itemName,
        category: parsedIntent.category,
        stockLevel: parsedIntent.quantity,
        minAlertLevel: 5,
        supplierName: parsedIntent.supplierName,
        price: parsedIntent.unitPrice || (parsedIntent.totalPrice / parsedIntent.quantity) || 100
      });
      setStatusMessage('Item added to Inventory catalog!');
      if (onNavigateToSection) {
        setTimeout(() => {
          onNavigateToSection('Inventory');
          onClose();
        }, 1000);
      }
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
      <div className={`w-full max-w-lg rounded-3xl border shadow-2xl p-6 relative max-h-[90vh] overflow-y-auto ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div
              className="p-2 rounded-xl text-white shadow-md"
              style={{ backgroundColor: accentColor }}
            >
              <Mic className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base leading-tight">Multilingual Speech Command Desk</h3>
              <p className="text-xs text-slate-500">English · Hindi · Punjabi · Hinglish voice parser</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Language selector */}
        <div className="mt-4 flex items-center justify-between">
          <span className="text-xs font-bold text-slate-400">Microphone Language:</span>
          <div className="flex gap-1.5 text-xs font-bold">
            <button
              onClick={() => setSelectedLang('en-IN')}
              className={`px-2.5 py-1 rounded-lg border transition-all ${selectedLang === 'en-IN' ? 'bg-blue-600 text-white border-blue-500' : 'bg-slate-800 border-slate-700 text-slate-400'}`}
            >
              English (IN)
            </button>
            <button
              onClick={() => setSelectedLang('hi-IN')}
              className={`px-2.5 py-1 rounded-lg border transition-all ${selectedLang === 'hi-IN' ? 'bg-blue-600 text-white border-blue-500' : 'bg-slate-800 border-slate-700 text-slate-400'}`}
            >
              हिन्दी
            </button>
            <button
              onClick={() => setSelectedLang('pa-IN')}
              className={`px-2.5 py-1 rounded-lg border transition-all ${selectedLang === 'pa-IN' ? 'bg-blue-600 text-white border-blue-500' : 'bg-slate-800 border-slate-700 text-slate-400'}`}
            >
              ਪੰਜਾਬੀ
            </button>
          </div>
        </div>

        {/* Microphone Pulse Center */}
        <div className="my-6 flex flex-col items-center justify-center">
          <button
            onClick={isListening ? handleStopListening : handleStartListening}
            className={`w-24 h-24 rounded-full flex items-center justify-center text-white shadow-2xl transition-all transform hover:scale-105 active:scale-95 ${isListening ? 'animate-pulse ring-8 ring-red-500/30 bg-red-600' : 'bg-blue-600'}`}
            style={{ backgroundColor: isListening ? '#DC2626' : accentColor }}
          >
            {isListening ? <MicOff className="w-10 h-10" /> : <Mic className="w-10 h-10" />}
          </button>
          <p className="text-xs font-bold text-slate-400 mt-3 text-center">
            {isListening ? 'Listening... Tap to finish' : 'Tap to start speaking voice instruction'}
          </p>
        </div>

        {/* Manual text speech input */}
        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Voice Transcript or Type Command
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={transcript}
              onChange={(e) => setTranscript(e.target.value)}
              placeholder="e.g. Add 10 Havells 1.5mm wire at 1850 rupees"
              className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
            />
            <button
              onClick={() => processTranscript(transcript)}
              disabled={isProcessing || !transcript.trim()}
              className="px-4 py-2.5 rounded-2xl text-xs font-bold text-white shrink-0 shadow-md"
              style={{ backgroundColor: accentColor }}
            >
              {isProcessing ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Parse'}
            </button>
          </div>
        </div>

        {/* Sample Voice Prompts Pills */}
        <div className="mt-4">
          <p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-2">Try saying or tapping:</p>
          <div className="flex flex-wrap gap-1.5">
            {samplePrompts.map((p, idx) => (
              <button
                key={idx}
                onClick={() => {
                  setTranscript(p);
                  processTranscript(p);
                }}
                className={`text-[11px] px-2.5 py-1 rounded-xl border text-left transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-slate-300 hover:border-slate-700' : 'bg-slate-100 border-slate-200 text-slate-700 hover:bg-slate-200'}`}
              >
                "{p}"
              </button>
            ))}
          </div>
        </div>

        {/* Status Message */}
        {statusMessage && (
          <p className="mt-4 text-xs font-medium text-emerald-400 text-center animate-fadeIn">
            {statusMessage}
          </p>
        )}

        {/* Parsed Intent Output Card */}
        {parsedIntent && (
          <div className={`mt-5 p-4 rounded-2xl border ${isDarkTheme ? 'bg-slate-950 border-slate-800' : 'bg-slate-50 border-slate-200'}`}>
            <div className="flex items-center justify-between mb-2">
              <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-emerald-500/20 text-emerald-400">
                Action: {parsedIntent.action}
              </span>
              <span className="text-[10px] font-bold text-slate-500">
                Lang: {parsedIntent.detectedLanguage.toUpperCase()}
              </span>
            </div>

            <h4 className="font-extrabold text-sm text-slate-100">{parsedIntent.itemName}</h4>
            <div className="grid grid-cols-3 gap-2 mt-2 text-xs text-slate-400 font-medium">
              <div>Quantity: <strong className="text-slate-200">{parsedIntent.quantity} {parsedIntent.unit}</strong></div>
              <div>Price: <strong className="text-slate-200">₹{parsedIntent.unitPrice}</strong></div>
              <div>Total: <strong className="text-emerald-400 font-bold">₹{parsedIntent.totalPrice}</strong></div>
            </div>

            <div className="mt-2 text-xs text-slate-400">
              Category: <strong className="text-slate-200">{parsedIntent.category}</strong> · Supplier: <strong className="text-slate-200">{parsedIntent.supplierName}</strong>
            </div>

            <button
              onClick={handleExecuteIntent}
              className="mt-4 w-full py-2.5 rounded-xl font-extrabold text-xs text-white flex items-center justify-center gap-2 shadow-lg"
              style={{ backgroundColor: accentColor }}
            >
              <CheckCircle className="w-4 h-4" />
              <span>
                {parsedIntent.action === 'LOG_EXPENSE' ? 'Confirm & Record Expense' : 'Confirm & Add to System'}
              </span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
