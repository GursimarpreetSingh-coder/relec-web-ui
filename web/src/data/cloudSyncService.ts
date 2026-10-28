import { db } from './database';
import type {
  Customer,
  ServiceRequest,
  Worker,
  InventoryItem,
  Bill,
  Expense,
  PurchaseRecord,
  AttendanceRecord
} from '../types/models';

export interface CloudSyncState {
  isSyncing: boolean;
  lastSyncTime: string | null;
  status: 'idle' | 'syncing' | 'success' | 'error';
  errorMessage: string | null;
  mode: 'production' | 'sandbox';
  endpoint: string;
  syncedCounts: {
    customers: number;
    services: number;
    inventory: number;
    bills: number;
    workers: number;
    expenses: number;
  };
}

const DEFAULT_CLOUD_ENDPOINT = 'https://relec-pro-erp-default-rtdb.asia-southeast1.firebasedatabase.app';
const SYNC_URL_STORAGE_KEY = 'relect_cloud_sync_url';
const SYNC_MODE_STORAGE_KEY = 'relect_cloud_sync_mode';

class CloudSyncService {
  private endpoint: string;
  private mode: 'production' | 'sandbox' = 'production';
  private isSyncing = false;
  private lastSyncTime: string | null = null;
  private status: 'idle' | 'syncing' | 'success' | 'error' = 'idle';
  private errorMessage: string | null = null;
  private listeners: Array<(state: CloudSyncState) => void> = [];
  private autoSyncTimer: any = null;

  constructor() {
    this.endpoint = localStorage.getItem(SYNC_URL_STORAGE_KEY) || DEFAULT_CLOUD_ENDPOINT;
    const storedMode = localStorage.getItem(SYNC_MODE_STORAGE_KEY);
    if (storedMode === 'sandbox' || storedMode === 'production') {
      this.mode = storedMode;
    }
    this.lastSyncTime = localStorage.getItem('relect_last_sync_time');

    // Auto-sync polling every 45 seconds if in production mode
    this.startAutoSync();
  }

  public getEndpoint(): string {
    return this.endpoint;
  }

  public setEndpoint(url: string) {
    const clean = url.trim().replace(/\/+$/, '');
    this.endpoint = clean || DEFAULT_CLOUD_ENDPOINT;
    localStorage.setItem(SYNC_URL_STORAGE_KEY, this.endpoint);
    this.notify();
  }

  public getMode(): 'production' | 'sandbox' {
    return this.mode;
  }

  public setMode(mode: 'production' | 'sandbox') {
    this.mode = mode;
    localStorage.setItem(SYNC_MODE_STORAGE_KEY, mode);
    this.notify();
  }

  public getState(): CloudSyncState {
    return {
      isSyncing: this.isSyncing,
      lastSyncTime: this.lastSyncTime,
      status: this.status,
      errorMessage: this.errorMessage,
      mode: this.mode,
      endpoint: this.endpoint,
      syncedCounts: {
        customers: 0,
        services: 0,
        inventory: 0,
        bills: 0,
        workers: 0,
        expenses: 0
      }
    };
  }

  public subscribe(listener: (state: CloudSyncState) => void): () => void {
    this.listeners.push(listener);
    listener(this.getState());
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private notify() {
    const state = this.getState();
    this.listeners.forEach(l => l(state));
  }

  private startAutoSync() {
    if (this.autoSyncTimer) clearInterval(this.autoSyncTimer);
    this.autoSyncTimer = setInterval(() => {
      if (!this.isSyncing && this.mode === 'production') {
        this.syncNow(false).catch(() => {});
      }
    }, 45000);
  }

  /**
   * Performs full bidirectional sync between local Dexie DB and Cloud Ledger.
   */
  public async syncNow(forceNotify = true): Promise<{ success: boolean; message: string }> {
    if (this.isSyncing) {
      return { success: false, message: 'Synchronization already in progress.' };
    }

    this.isSyncing = true;
    this.status = 'syncing';
    this.errorMessage = null;
    if (forceNotify) this.notify();

    try {
      const channel = this.mode === 'production' ? 'production_ledger' : 'sandbox_ledger';
      const targetUrl = `${this.endpoint}/${channel}.json`;

      // 1. Pull latest cloud data
      let remoteData: any = null;
      try {
        const response = await fetch(targetUrl, {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        });
        if (response.ok) {
          remoteData = await response.json();
        }
      } catch (err) {
        console.warn('Cloud sync fetch failed, continuing with local data:', err);
      }

      // 2. Read local data from Dexie
      const localCustomers = await db.customers.toArray();
      const localServices = await db.services.toArray();
      const localInventory = await db.inventory.toArray();
      const localBills = await db.bills.toArray();
      const localWorkers = await db.workers.toArray();
      const localExpenses = await db.expenses.toArray();
      const localPurchases = await db.purchases.toArray();
      const localAttendance = await db.attendance.toArray();

      // 3. Merge remote data into local database if remote exists
      if (remoteData && typeof remoteData === 'object') {
        if (Array.isArray(remoteData.customers)) {
          for (const c of remoteData.customers) {
            if (c && c.id) await db.customers.put(c);
          }
        }
        if (Array.isArray(remoteData.services)) {
          for (const s of remoteData.services) {
            if (s && s.id) await db.services.put(s);
          }
        }
        if (Array.isArray(remoteData.inventory)) {
          for (const i of remoteData.inventory) {
            if (i && i.id) await db.inventory.put(i);
          }
        }
        if (Array.isArray(remoteData.bills)) {
          for (const b of remoteData.bills) {
            if (b && b.id) await db.bills.put(b);
          }
        }
        if (Array.isArray(remoteData.workers)) {
          for (const w of remoteData.workers) {
            if (w && w.id) await db.workers.put(w);
          }
        }
        if (Array.isArray(remoteData.expenses)) {
          for (const e of remoteData.expenses) {
            if (e && e.id) await db.expenses.put(e);
          }
        }
        if (Array.isArray(remoteData.purchases)) {
          for (const p of remoteData.purchases) {
            if (p && p.id) await db.purchases.put(p);
          }
        }
        if (Array.isArray(remoteData.attendance)) {
          for (const a of remoteData.attendance) {
            if (a && a.id) await db.attendance.put(a);
          }
        }
      }

      // 4. Read merged state and push to Cloud Ledger
      const mergedCustomers = await db.customers.toArray();
      const mergedServices = await db.services.toArray();
      const mergedInventory = await db.inventory.toArray();
      const mergedBills = await db.bills.toArray();
      const mergedWorkers = await db.workers.toArray();
      const mergedExpenses = await db.expenses.toArray();
      const mergedPurchases = await db.purchases.toArray();
      const mergedAttendance = await db.attendance.toArray();

      const payload = {
        customers: mergedCustomers,
        services: mergedServices,
        inventory: mergedInventory,
        bills: mergedBills,
        workers: mergedWorkers,
        expenses: mergedExpenses,
        purchases: mergedPurchases,
        attendance: mergedAttendance,
        updatedAt: new Date().toISOString(),
        updatedBy: 'web-portal'
      };

      try {
        await fetch(targetUrl, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      } catch (pushErr) {
        console.warn('Cloud sync push failed, local changes preserved:', pushErr);
      }

      const nowStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      this.lastSyncTime = nowStr;
      localStorage.setItem('relect_last_sync_time', nowStr);
      this.status = 'success';
      this.isSyncing = false;
      this.notify();

      return {
        success: true,
        message: `Synced ${mergedCustomers.length} Customers, ${mergedServices.length} Jobs, ${mergedBills.length} Invoices, ${mergedInventory.length} Items.`
      };
    } catch (err: any) {
      console.error('Cloud synchronization error:', err);
      this.status = 'error';
      this.errorMessage = err.message || 'Sync failed';
      this.isSyncing = false;
      this.notify();
      return { success: false, message: this.errorMessage || 'Sync failed' };
    }
  }
}

export const cloudSyncService = new CloudSyncService();
