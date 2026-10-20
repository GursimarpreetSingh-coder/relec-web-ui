import React, { useState } from 'react';
import { useTheme } from '../theme';
import { useLiveQuery } from 'dexie-react-hooks';
import { db, addBillAndDecrementStock } from '../../data/database';
import type { Bill, BillItem, Customer } from '../../types/models';
import {
  Receipt,
  Plus,
  Printer,
  Trash2,
  X,
  CheckCircle,
  AlertCircle,
  DollarSign,
  User,
  Calendar,
  Layers,
  Share2
} from 'lucide-react';

export const BillingScreen: React.FC = () => {
  const { accentColor, isDarkTheme, brandName } = useTheme();

  const bills = useLiveQuery(() => db.bills.toArray()) || [];
  const inventory = useLiveQuery(() => db.inventory.toArray()) || [];
  const customers = useLiveQuery(() => db.customers.toArray()) || [];

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedBillForPrint, setSelectedBillForPrint] = useState<Bill | null>(null);

  // Bill Creator Form State
  const [customerName, setCustomerName] = useState('Amit Patel');
  const [customerPhone, setCustomerPhone] = useState('8146312456');
  const [paymentMethod, setPaymentMethod] = useState('Cash');
  const [paymentStatus, setPaymentStatus] = useState('Paid');
  const [laborCharges, setLaborCharges] = useState(350);
  const [gstPercent, setGstPercent] = useState(18);
  const [discountAmount, setDiscountAmount] = useState(0);

  const [lineItems, setLineItems] = useState<BillItem[]>([
    { name: 'Anchor Modular Switch 6A', qty: 5, rate: 25.0, total: 125.0 },
    { name: 'Keltron 2.5uF Fan Capacitor', qty: 1, rate: 45.0, total: 45.0 }
  ]);

  // Calculations
  const itemsSubtotal = lineItems.reduce((acc, it) => acc + it.total, 0);
  const taxableAmount = itemsSubtotal + Number(laborCharges || 0) - Number(discountAmount || 0);
  const taxAmount = (taxableAmount * Number(gstPercent || 0)) / 100;
  const grandTotal = taxableAmount + taxAmount;

  const handleAddLineItem = () => {
    const firstInv = inventory[0];
    setLineItems([
      ...lineItems,
      {
        name: firstInv ? firstInv.name : 'Custom Electrical Part',
        qty: 1,
        rate: firstInv ? firstInv.price : 100,
        total: firstInv ? firstInv.price : 100
      }
    ]);
  };

  const handleUpdateLineItem = (index: number, field: keyof BillItem, value: any) => {
    const updated = [...lineItems];
    const current = { ...updated[index], [field]: value };
    if (field === 'qty' || field === 'rate') {
      current.total = Number(current.qty || 0) * Number(current.rate || 0);
    }
    if (field === 'name') {
      const match = inventory.find(i => i.name === value);
      if (match) {
        current.rate = match.price;
        current.total = current.qty * match.price;
      }
    }
    updated[index] = current;
    setLineItems(updated);
  };

  const handleRemoveLineItem = (index: number) => {
    setLineItems(lineItems.filter((_, i) => i !== index));
  };

  const handleSaveBill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerName || lineItems.length === 0) return;

    const newBill: Bill = {
      id: 'B_' + Date.now().toString(36),
      invoiceNumber: 'INV-' + new Date().getFullYear() + '-' + Math.floor(1000 + Math.random() * 9000),
      storeName: 'Rattan Electricals',
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      items: lineItems,
      laborCharges: Number(laborCharges) || 0,
      gstPercent: Number(gstPercent) || 18,
      discountAmount: Number(discountAmount) || 0,
      totalAmount: grandTotal,
      date: new Date().toISOString().split('T')[0],
      isDownloaded: false,
      paymentMethod,
      isMathematicallyValid: true,
      paymentStatus,
      balanceDue: paymentStatus === 'Paid' ? 0 : grandTotal
    };

    await addBillAndDecrementStock(newBill);
    setShowCreateModal(false);
  };

  const handleDeleteBill = async (id: string) => {
    if (confirm('Delete this tax bill record?')) {
      await db.bills.delete(id);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight">Finances & Billing Ledger</h2>
          <p className="text-xs text-slate-500">
            {bills.length} GST compliant invoices and retail cash memos generated
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-bold text-white shadow-md transition-all self-start sm:self-auto"
          style={{ backgroundColor: accentColor }}
        >
          <Plus className="w-4 h-4" />
          <span>Generate Tax Invoice</span>
        </button>
      </div>

      {/* Invoices List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {bills.length === 0 ? (
          <div className="col-span-full py-16 text-center text-xs text-slate-500">
            No invoices generated yet. Click "Generate Tax Invoice" to create one.
          </div>
        ) : (
          bills.map(bill => (
            <div
              key={bill.id}
              className={`p-5 rounded-3xl border shadow-lg transition-all flex flex-col justify-between ${isDarkTheme ? 'bg-slate-900/90 border-slate-800' : 'bg-white border-slate-200'}`}
            >
              <div>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2.5 rounded-2xl bg-emerald-500/15 text-emerald-400">
                      <Receipt className="w-5 h-5" />
                    </div>
                    <div>
                      <span className="text-[10px] font-mono font-bold text-slate-500">
                        {bill.invoiceNumber || 'INV-2026-REG'}
                      </span>
                      <h3 className="font-extrabold text-sm text-slate-100 leading-snug">{bill.customerName}</h3>
                    </div>
                  </div>

                  <span className={`px-2.5 py-1 rounded-xl text-[10px] font-black uppercase tracking-wider ${bill.paymentStatus === 'Paid' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-amber-500/20 text-amber-400'}`}>
                    {bill.paymentStatus}
                  </span>
                </div>

                {/* Rows Summary */}
                <div className="mt-4 space-y-1 text-xs">
                  <div className="flex justify-between text-slate-400">
                    <span>Items Count:</span>
                    <span className="font-bold text-slate-200">{bill.items.length} products</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Labor / Installation:</span>
                    <span>₹{bill.laborCharges.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>GST ({bill.gstPercent}%):</span>
                    <span>₹{(((bill.items.reduce((a, b) => a + b.total, 0) + bill.laborCharges - bill.discountAmount) * bill.gstPercent) / 100).toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Date:</span>
                    <span>{bill.date}</span>
                  </div>
                </div>

                <div className="mt-3 pt-3 border-t border-slate-800 flex justify-between items-center">
                  <span className="text-xs text-slate-400 font-bold">Total Payable:</span>
                  <span className="text-base font-black text-emerald-400">
                    ₹{bill.totalAmount.toLocaleString('en-IN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
                  </span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between gap-2">
                <button
                  onClick={() => setSelectedBillForPrint(bill)}
                  className="flex-1 py-2 rounded-xl text-xs font-bold bg-slate-800 hover:bg-slate-700 text-slate-200 flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Printer className="w-3.5 h-3.5 text-blue-400" />
                  <span>View / Print Bill</span>
                </button>

                <button
                  onClick={() => handleDeleteBill(bill.id)}
                  className="p-2 rounded-xl text-slate-500 hover:text-red-400 hover:bg-red-500/10"
                  title="Delete Invoice"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Create Invoice Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
          <div className={`w-full max-w-2xl rounded-3xl border shadow-2xl p-6 max-h-[90vh] overflow-y-auto ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-slate-100' : 'bg-white border-slate-200 text-slate-900'}`}>
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <h3 className="font-extrabold text-base">Generate GST Tax Invoice</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveBill} className="mt-4 space-y-4">
              {/* Customer Row */}
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
                    placeholder="Amit Patel"
                    className={`w-full px-4 py-2 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Customer Mobile
                  </label>
                  <input
                    type="tel"
                    required
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                    placeholder="8146312456"
                    className={`w-full px-4 py-2 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
              </div>

              {/* Line Items Table */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Itemized Product Lines ({lineItems.length})
                  </label>
                  <button
                    type="button"
                    onClick={handleAddLineItem}
                    className="text-xs font-bold text-blue-400 hover:underline flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Item</span>
                  </button>
                </div>

                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {lineItems.map((item, idx) => (
                    <div
                      key={idx}
                      className="p-2.5 rounded-2xl border border-slate-800 bg-slate-950/70 flex items-center gap-2 text-xs"
                    >
                      <input
                        type="text"
                        value={item.name}
                        onChange={(e) => handleUpdateLineItem(idx, 'name', e.target.value)}
                        placeholder="Item name"
                        className="flex-1 px-3 py-1.5 rounded-xl border border-slate-800 bg-slate-900 text-white text-xs focus:outline-none"
                      />
                      <input
                        type="number"
                        min="1"
                        value={item.qty}
                        onChange={(e) => handleUpdateLineItem(idx, 'qty', Number(e.target.value))}
                        className="w-16 px-2 py-1.5 rounded-xl border border-slate-800 bg-slate-900 text-white text-xs text-center focus:outline-none"
                        placeholder="Qty"
                      />
                      <input
                        type="number"
                        value={item.rate}
                        onChange={(e) => handleUpdateLineItem(idx, 'rate', Number(e.target.value))}
                        className="w-20 px-2 py-1.5 rounded-xl border border-slate-800 bg-slate-900 text-white text-xs text-center focus:outline-none"
                        placeholder="Rate"
                      />
                      <span className="w-20 text-right font-mono font-bold text-emerald-400 text-xs">
                        ₹{item.total.toFixed(2)}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveLineItem(idx)}
                        className="p-1 rounded text-slate-500 hover:text-red-400"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Adjustments: Labor, GST, Discount */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Labor Charges (₹)
                  </label>
                  <input
                    type="number"
                    value={laborCharges}
                    onChange={(e) => setLaborCharges(Number(e.target.value))}
                    className={`w-full px-3 py-2 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    GST Percent (%)
                  </label>
                  <input
                    type="number"
                    value={gstPercent}
                    onChange={(e) => setGstPercent(Number(e.target.value))}
                    className={`w-full px-3 py-2 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Discount (₹)
                  </label>
                  <input
                    type="number"
                    value={discountAmount}
                    onChange={(e) => setDiscountAmount(Number(e.target.value))}
                    className={`w-full px-3 py-2 rounded-2xl text-xs border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>
              </div>

              {/* Total Summary Breakdown */}
              <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 space-y-1.5 text-xs">
                <div className="flex justify-between text-slate-400">
                  <span>Items Subtotal:</span>
                  <span className="font-mono text-slate-200">₹{itemsSubtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Labor Charges:</span>
                  <span className="font-mono text-slate-200">₹{Number(laborCharges).toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Tax Amount (GST {gstPercent}%):</span>
                  <span className="font-mono text-slate-200">₹{taxAmount.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm font-black pt-2 border-t border-slate-800">
                  <span className="text-slate-100">Grand Total:</span>
                  <span className="text-emerald-400 font-mono">₹{grandTotal.toFixed(2)}</span>
                </div>
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="w-1/2 py-2.5 rounded-xl border border-slate-700 text-xs font-bold text-slate-400"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="w-1/2 py-2.5 rounded-xl text-xs font-bold text-white shadow-md"
                  style={{ backgroundColor: accentColor }}
                >
                  Save & Decrement Stock
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Printable Tax Invoice Modal */}
      {selectedBillForPrint && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-xl rounded-3xl bg-white text-slate-900 p-8 max-h-[95vh] overflow-y-auto shadow-2xl relative font-sans print:p-0 print:m-0 print:shadow-none">
            <button
              onClick={() => setSelectedBillForPrint(null)}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 print:hidden"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Bill Header */}
            <div className="text-center pb-4 border-b-2 border-slate-900">
              <h2 className="text-xl font-black uppercase tracking-tight text-slate-900">
                RATTAN ELECTRICALS ENTERPRISE TRADERS
              </h2>
              <p className="text-xs text-slate-600 font-medium">
                Shop #12, Main Electrical Market, Near Clock Tower · Helpline: +91 98765-43210
              </p>
              <p className="text-[11px] font-mono text-slate-500 font-bold mt-0.5">
                GSTIN: 03AAACR4821F1Z8 · TAX INVOICE
              </p>
            </div>

            {/* Invoice Meta */}
            <div className="my-4 grid grid-cols-2 text-xs">
              <div>
                <p><strong>Customer:</strong> {selectedBillForPrint.customerName}</p>
                <p><strong>Mobile:</strong> +91 {selectedBillForPrint.customerPhone}</p>
              </div>
              <div className="text-right">
                <p><strong>Invoice No:</strong> {selectedBillForPrint.invoiceNumber || 'INV-2026-REG'}</p>
                <p><strong>Date:</strong> {selectedBillForPrint.date}</p>
                <p><strong>Payment:</strong> {selectedBillForPrint.paymentMethod} ({selectedBillForPrint.paymentStatus})</p>
              </div>
            </div>

            {/* Items Table */}
            <table className="w-full text-xs text-left my-4 border-collapse">
              <thead>
                <tr className="border-b-2 border-slate-900 font-bold">
                  <th className="py-2">Item Description</th>
                  <th className="py-2 text-center">Qty</th>
                  <th className="py-2 text-right">Rate (₹)</th>
                  <th className="py-2 text-right">Amount (₹)</th>
                </tr>
              </thead>
              <tbody>
                {selectedBillForPrint.items.map((it, idx) => (
                  <tr key={idx} className="border-b border-slate-200">
                    <td className="py-2 font-medium">{it.name}</td>
                    <td className="py-2 text-center">{it.qty}</td>
                    <td className="py-2 text-right font-mono">{it.rate.toFixed(2)}</td>
                    <td className="py-2 text-right font-mono font-bold">{it.total.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Summary */}
            <div className="mt-4 pt-2 border-t-2 border-slate-900 text-xs space-y-1">
              <div className="flex justify-between">
                <span>Labor / Fitting Charges:</span>
                <span className="font-mono">₹{selectedBillForPrint.laborCharges.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span>CGST (9%) + SGST (9%):</span>
                <span className="font-mono">
                  ₹{(((selectedBillForPrint.items.reduce((a, b) => a + b.total, 0) + selectedBillForPrint.laborCharges - selectedBillForPrint.discountAmount) * selectedBillForPrint.gstPercent) / 100).toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-base font-black pt-2 border-t border-slate-900">
                <span>Grand Total:</span>
                <span className="font-mono">₹{selectedBillForPrint.totalAmount.toFixed(2)}</span>
              </div>
            </div>

            {/* Signatory */}
            <div className="mt-8 pt-6 flex justify-between items-end text-xs">
              <div>
                <p className="text-[10px] text-slate-500 font-medium">Thank you for your business!</p>
                <p className="text-[10px] text-slate-500">Goods once sold can be exchanged within 7 days.</p>
              </div>
              <div className="text-center">
                <div className="w-32 border-b border-slate-400 mb-1" />
                <span className="font-bold text-[10px] uppercase">Authorized Signatory</span>
              </div>
            </div>

            {/* Print Button */}
            <div className="mt-6 flex justify-end gap-2 print:hidden">
              <button
                onClick={() => window.print()}
                className="px-6 py-2.5 rounded-xl bg-slate-900 text-white font-bold text-xs flex items-center gap-2 shadow-lg"
              >
                <Printer className="w-4 h-4" />
                <span>Print Tax Invoice</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
