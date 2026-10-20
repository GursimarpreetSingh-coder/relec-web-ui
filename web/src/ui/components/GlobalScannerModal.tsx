import React, { useState, useRef } from 'react';
import { useTheme } from '../theme';
import { geminiClient } from '../../data/geminiClient';
import { db, addPurchaseRecordAndSyncStock } from '../../data/database';
import type { VisualIdentificationResult } from '../../types/models';
import {
  X,
  Camera,
  Upload,
  Sparkles,
  CheckCircle,
  AlertTriangle,
  Receipt,
  Cpu,
  PlusCircle,
  Loader2
} from 'lucide-react';

interface GlobalScannerModalProps {
  onClose: () => void;
  onNavigateToSection?: (section: string) => void;
}

export const GlobalScannerModal: React.FC<GlobalScannerModalProps> = ({ onClose, onNavigateToSection }) => {
  const { accentColor, isDarkTheme } = useTheme();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [scanResult, setScanResult] = useState<VisualIdentificationResult | null>(null);
  const [errorMsg, setErrorMsg] = useState('');
  const [savedSuccess, setSavedSuccess] = useState(false);

  // Handle image selection
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const base64 = event.target?.result as string;
      setImagePreview(base64);
      triggerScan(base64);
    };
    reader.readAsDataURL(file);
  };

  // Trigger Gemini AI scan
  const triggerScan = async (base64: string) => {
    setIsScanning(true);
    setErrorMsg('');
    setScanResult(null);
    setSavedSuccess(false);

    try {
      const result = await geminiClient.analyzeVisualObjectOrInvoice(base64);
      setScanResult(result);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to scan image with Gemini AI.');
    } finally {
      setIsScanning(false);
    }
  };

  // Quick Action: Add as Inventory Item
  const handleAddToInventory = async () => {
    if (!scanResult) return;
    try {
      await db.inventory.put({
        id: 'INV_' + Date.now().toString(36),
        name: scanResult.title,
        category: scanResult.category,
        stockLevel: scanResult.quantity,
        minAlertLevel: 5,
        supplierName: scanResult.brand,
        price: scanResult.estimatedUnitPrice || scanResult.estimatedTotalPrice / scanResult.quantity
      });
      setSavedSuccess(true);
      if (onNavigateToSection) {
        setTimeout(() => {
          onNavigateToSection('Inventory');
          onClose();
        }, 1200);
      }
    } catch (e: any) {
      setErrorMsg('Failed to save to inventory: ' + e.message);
    }
  };

  // Quick Action: Add as Purchase Record
  const handleSavePurchase = async () => {
    if (!scanResult) return;
    try {
      await addPurchaseRecordAndSyncStock({
        id: 'PUR_' + Date.now().toString(36),
        itemName: scanResult.title,
        quantity: scanResult.quantity,
        supplierName: scanResult.brand,
        cost: scanResult.estimatedTotalPrice,
        date: new Date().toISOString().split('T')[0],
        photoUri: imagePreview
      });
      setSavedSuccess(true);
      if (onNavigateToSection) {
        setTimeout(() => {
          onNavigateToSection('Inventory');
          onClose();
        }, 1200);
      }
    } catch (e: any) {
      setErrorMsg('Failed to save purchase: ' + e.message);
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
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base leading-tight">Gemini Vision AI Scanner</h3>
              <p className="text-xs text-slate-500">Scan electrical products, barcodes, or invoices</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Upload / Capture Section */}
        <div className="mt-5">
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept="image/*"
            className="hidden"
          />

          {!imagePreview ? (
            <div
              onClick={() => fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-3xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all hover:border-blue-500 ${isDarkTheme ? 'border-slate-800 bg-slate-950/50 hover:bg-slate-950' : 'border-slate-300 bg-slate-50 hover:bg-slate-100'}`}
            >
              <div
                className="w-16 h-16 rounded-2xl flex items-center justify-center mb-3 text-white shadow-lg"
                style={{ backgroundColor: accentColor }}
              >
                <Camera className="w-8 h-8" />
              </div>
              <p className="font-bold text-sm">Tap to Take Photo or Upload Bill / Object</p>
              <p className="text-xs text-slate-500 mt-1 text-center">
                Supports MCB breakers, switches, wire coils, motors, fans & tax invoices
              </p>
            </div>
          ) : (
            <div className="relative rounded-2xl overflow-hidden border border-slate-700 bg-black flex items-center justify-center max-h-56">
              <img src={imagePreview} alt="Preview" className="w-full h-full object-contain" />
              <button
                onClick={() => {
                  setImagePreview(null);
                  setScanResult(null);
                }}
                className="absolute top-3 right-3 p-1.5 rounded-full bg-black/70 text-white hover:bg-black"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>

        {/* Scanning State */}
        {isScanning && (
          <div className="py-8 flex flex-col items-center justify-center gap-3">
            <Loader2 className="w-8 h-8 animate-spin" style={{ color: accentColor }} />
            <p className="text-xs font-bold text-slate-400 animate-pulse">
              Gemini Vision neural network analyzing electrical features & bill rows...
            </p>
          </div>
        )}

        {/* Error State */}
        {errorMsg && (
          <div className="mt-4 p-3 rounded-2xl bg-red-500/10 border border-red-500/20 text-xs text-red-400 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Success Alert */}
        {savedSuccess && (
          <div className="mt-4 p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-400 flex items-center gap-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>Record synchronized with local inventory ledger!</span>
          </div>
        )}

        {/* Analysis Result */}
        {scanResult && !isScanning && (
          <div className="mt-5 space-y-4">
            <div className={`p-4 rounded-2xl border ${isDarkTheme ? 'bg-slate-950 border-slate-800' : 'bg-slate-50 border-slate-200'}`}>
              <div className="flex items-center justify-between mb-2">
                <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-black uppercase tracking-wider ${scanResult.isInvoice ? 'bg-amber-500/20 text-amber-400' : 'bg-blue-500/20 text-blue-400'}`}>
                  {scanResult.isInvoice ? <Receipt className="w-3.5 h-3.5" /> : <Cpu className="w-3.5 h-3.5" />}
                  {scanResult.isInvoice ? 'Tax Invoice / Bill Document' : 'Electrical Hardware Item'}
                </span>
                <span className="text-[11px] font-bold text-slate-500">
                  Confidence: {Math.round(scanResult.confidence * 100)}%
                </span>
              </div>

              <h4 className="font-extrabold text-base leading-snug">{scanResult.title}</h4>
              <p className="text-xs text-slate-500 mt-0.5">
                Brand: <strong className="text-slate-300">{scanResult.brand}</strong> · Category: <strong className="text-slate-300">{scanResult.category}</strong>
              </p>

              {scanResult.specifications && (
                <p className="text-xs text-slate-400 mt-2 font-mono bg-slate-900/60 p-2 rounded-xl border border-slate-800">
                  {scanResult.specifications}
                </p>
              )}

              {/* Price Details */}
              <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-900/50 border border-slate-800/80">
                  <span className="text-slate-500 block text-[10px] font-bold uppercase">Estimated Unit Price</span>
                  <span className="text-base font-black text-emerald-400">₹{scanResult.estimatedUnitPrice.toFixed(2)}</span>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-900/50 border border-slate-800/80">
                  <span className="text-slate-500 block text-[10px] font-bold uppercase">Total Value ({scanResult.quantity} {scanResult.unit})</span>
                  <span className="text-base font-black" style={{ color: accentColor }}>₹{scanResult.estimatedTotalPrice.toFixed(2)}</span>
                </div>
              </div>

              {/* Invoice Breakdown if Bill */}
              {scanResult.invoiceDetails && (
                <div className="mt-3 pt-3 border-t border-slate-800">
                  <h5 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Itemized Bill Rows:</h5>
                  <div className="space-y-1.5 max-h-36 overflow-y-auto pr-1">
                    {scanResult.invoiceDetails.items.map((item, idx) => (
                      <div key={idx} className="flex justify-between items-center text-xs py-1 border-b border-slate-800/50">
                        <span className="text-slate-300 font-medium">{item.name} (x{item.quantity})</span>
                        <span className="font-mono font-bold text-slate-200">₹{item.lineTotal.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>

                  <div className="mt-3 flex justify-between items-center font-bold text-xs pt-1">
                    <span>Tax (GST 18%): ₹{scanResult.invoiceDetails.taxAmount.toFixed(2)}</span>
                    <span className="text-sm text-emerald-400 font-black">Grand Total: ₹{scanResult.invoiceDetails.grandTotal.toFixed(2)}</span>
                  </div>

                  {scanResult.invoiceDetails.discrepancyNote && (
                    <p className="text-[11px] text-amber-400 mt-2 p-2 rounded-lg bg-amber-500/10 border border-amber-500/20">
                      {scanResult.invoiceDetails.discrepancyNote}
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Quick Action Buttons */}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <button
                onClick={handleAddToInventory}
                className="py-2.5 px-4 rounded-xl text-xs font-extrabold text-white flex items-center justify-center gap-1.5 shadow-md transition-all"
                style={{ backgroundColor: accentColor }}
              >
                <PlusCircle className="w-4 h-4" />
                <span>Add to Inventory</span>
              </button>
              <button
                onClick={handleSavePurchase}
                className="py-2.5 px-4 rounded-xl text-xs font-extrabold bg-emerald-600 hover:bg-emerald-500 text-white flex items-center justify-center gap-1.5 shadow-md transition-all"
              >
                <CheckCircle className="w-4 h-4" />
                <span>Log as Purchase</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
