import Dexie, { type Table } from 'dexie';
import type {
  Customer,
  ServiceRequest,
  Worker,
  InventoryItem,
  PurchaseRecord,
  Bill,
  Expense,
  AttendanceRecord,
  UserEntity,
  OtpRecord,
  AuditLogEntry,
  StoreSupportInfo
} from '../types/models';

export class ReLecDatabase extends Dexie {
  customers!: Table<Customer, string>;
  services!: Table<ServiceRequest, string>;
  workers!: Table<Worker, string>;
  inventory!: Table<InventoryItem, string>;
  purchases!: Table<PurchaseRecord, string>;
  bills!: Table<Bill, string>;
  expenses!: Table<Expense, string>;
  attendance!: Table<AttendanceRecord, string>;
  users!: Table<UserEntity, string>;
  otpRecords!: Table<OtpRecord, string>;
  auditLogs!: Table<AuditLogEntry, string>;
  settings!: Table<{ key: string; value: any }, string>;

  constructor() {
    super('ReLecERP_Database');
    this.version(1).stores({
      customers: 'id, name, phone, joinedDate',
      services: 'id, customerName, customerPhone, assignedWorkerId, status, priority, dateCreated',
      workers: 'id, name, phone, status, designation, isApproved, isActive',
      inventory: 'id, name, category, stockLevel',
      purchases: 'id, itemName, supplierName, date',
      bills: 'id, customerName, customerPhone, date, paymentStatus',
      expenses: 'id, category, date',
      attendance: 'id, workerId, date, isClockedIn',
      users: 'id, email, phone, role, isApproved, isActive',
      otpRecords: 'id, target, purpose, isUsed, expiresAt',
      auditLogs: 'id, timestamp, action',
      settings: 'key'
    });
  }
}

export const db = new ReLecDatabase();

// --- Crypto SHA-256 with Salt ---
export async function hashPasswordWithSalt(password: string, salt: string): Promise<string> {
  const enc = new TextEncoder();
  const data = enc.encode(`${salt}:${password}`);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// --- Default Store Support Info ---
export const DEFAULT_SUPPORT_INFO: StoreSupportInfo = {
  storeName: "Rattan Electricals Helpdesk",
  phone: "9876543210",
  whatsapp: "9876543210",
  address: "Shop #12, Main Electrical Market, Near Clock Tower",
  workingHours: "Mon - Sat (9:00 AM – 8:30 PM)",
  emergencyHelpline: "+91 98765 43210",
  safetyGuidelines: `In case of smoke or burning smell, instantly switch off the Main MCB.\nNever touch appliance switches with wet hands or barefoot on wet floors.\nDo not overload multi-plug strips with heavy loads like geysers or heaters.\nAlways ensure copper earthing wire is connected to your power socket.`,
  appDownloadUrl: "./downloads/relec-pro-erp.apk"
};

// --- Seed Data Sets matching Kotlin Code ---
export const SEED_CUSTOMERS: Customer[] = [
  { id: "C1", name: "Rajesh Kumar", phone: "9812045612", address: "H.No 142, Sector 15, Chandigarh", joinedDate: "2026-03-10" },
  { id: "C2", name: "Anita Sharma", phone: "9417255831", address: "Sco 42, Phase 3B2, Mohali", joinedDate: "2026-04-22" },
  { id: "C3", name: "Gurpreet Singh", phone: "9872134590", address: "Green Avenue, Airport Road, Amritsar", joinedDate: "2026-01-15" },
  { id: "C4", name: "Amit Patel", phone: "8146312456", address: "Sector 22-C, Chandigarh", joinedDate: "2026-05-18" },
  { id: "C5", name: "Priya Bansal", phone: "9501567234", address: "Street No. 3, Civil Lines, Ludhiana", joinedDate: "2026-06-05" }
];

export const SEED_SERVICES: ServiceRequest[] = [
  {
    id: "S1",
    customerName: "Anita Sharma",
    customerPhone: "9417255831",
    customerAddress: "Sco 42, Phase 3B2, Mohali",
    problemDescription: "Main MCB tripping repeatedly when AC is switched on. Burning smell from DB board.",
    category: "DB Board Repair",
    priority: "HIGH",
    assignedWorkerId: "W1",
    assignedWorkerName: "Sukhvinder Singh",
    status: "IN_PROGRESS",
    preferredDateTime: "2026-07-01 11:30 AM",
    dateCreated: "2026-07-01",
    statusHistory: [
      { status: "CREATED", timestamp: "2026-07-01 09:00 AM", notes: "Booking created via Counter QR Desk" },
      { status: "ASSIGNED", timestamp: "2026-07-01 09:45 AM", notes: "Assigned to Sukhvinder Singh" },
      { status: "IN_PROGRESS", timestamp: "2026-07-01 11:30 AM", notes: "Technician arrived on site, inspecting DB wiring" }
    ]
  },
  {
    id: "S2",
    customerName: "Rajesh Kumar",
    customerPhone: "9812045612",
    customerAddress: "H.No 142, Sector 15, Chandigarh",
    problemDescription: "Fitting 3 new Orient ceiling fans in bedrooms and drawing room.",
    category: "Fan Installation",
    priority: "MEDIUM",
    assignedWorkerId: "W2",
    assignedWorkerName: "Baljinder Singh",
    status: "ASSIGNED",
    preferredDateTime: "2026-07-02 02:00 PM",
    dateCreated: "2026-07-01",
    statusHistory: [
      { status: "CREATED", timestamp: "2026-07-01 10:15 AM", notes: "Service booked" },
      { status: "ASSIGNED", timestamp: "2026-07-01 11:00 AM", notes: "Assigned to Baljinder Singh" }
    ]
  },
  {
    id: "S3",
    customerName: "Gurpreet Singh",
    customerPhone: "9872134590",
    customerAddress: "Green Avenue, Airport Road, Amritsar",
    problemDescription: "Water pump motor capacitor has burst. Motor humming but not pumping water.",
    category: "Motor Repair",
    priority: "EMERGENCY",
    assignedWorkerId: null,
    assignedWorkerName: null,
    status: "CREATED",
    preferredDateTime: "2026-07-01 09:00 AM",
    dateCreated: "2026-07-01",
    statusHistory: [
      { status: "CREATED", timestamp: "2026-07-01 08:30 AM", notes: "Emergency breakdown flagged" }
    ]
  },
  {
    id: "S4",
    customerName: "Amit Patel",
    customerPhone: "8146312456",
    customerAddress: "Sector 22-C, Chandigarh",
    problemDescription: "Inverter backup line connection wiring check.",
    category: "Wiring Check",
    priority: "LOW",
    assignedWorkerId: "W1",
    assignedWorkerName: "Sukhvinder Singh",
    status: "COMPLETED",
    preferredDateTime: "2026-06-29 04:00 PM",
    dateCreated: "2026-06-29",
    statusHistory: [
      { status: "CREATED", timestamp: "2026-06-29 02:00 PM", notes: "Ticket created" },
      { status: "ASSIGNED", timestamp: "2026-06-29 02:30 PM", notes: "Assigned to Sukhvinder" },
      { status: "IN_PROGRESS", timestamp: "2026-06-29 04:00 PM", notes: "Inspection in progress" },
      { status: "COMPLETED", timestamp: "2026-06-29 05:45 PM", notes: "Neutral loop resolved and inverter operational" }
    ]
  }
];

export const SEED_WORKERS: Worker[] = [
  { id: "W1", name: "Sukhvinder Singh", phone: "9821034455", salary: 24000.0, designation: "Senior Electrician", status: "WORKING", joiningDate: "2024-04-01", isVerified: true, isApproved: true, isActive: true },
  { id: "W2", name: "Baljinder Singh", phone: "9501562211", salary: 18000.0, designation: "Electrician", status: "ASSIGNED_NOT_STARTED", joiningDate: "2025-01-15", isVerified: true, isApproved: true, isActive: true },
  { id: "W3", name: "Harpreet Singh", phone: "8146399002", salary: 12000.0, designation: "Helper Technician", status: "IDLE", joiningDate: "2025-10-10", isVerified: true, isApproved: true, isActive: true }
];

export const SEED_INVENTORY: InventoryItem[] = [
  { id: "I1", name: "Havells 1.5 Sqmm Wire (Red)", category: "Wires & Cables", stockLevel: 4, minAlertLevel: 10, supplierName: "Havells India Ltd", price: 1450.0 },
  { id: "I2", name: "Anchor Modular Switch 6A", category: "Switches & Sockets", stockLevel: 85, minAlertLevel: 20, supplierName: "Anchor Panasonic", price: 22.0 },
  { id: "I3", name: "Luminous 16A Single Pole MCB", category: "Switchgears", stockLevel: 12, minAlertLevel: 15, supplierName: "Luminous Power", price: 160.0 },
  { id: "I4", name: "Orient 48\" Ceiling Fan Black", category: "Fans & Cooling", stockLevel: 3, minAlertLevel: 5, supplierName: "Orient Electric", price: 2100.0 },
  { id: "I5", name: "Crompton 1HP Tullu Water Pump", category: "Motors & Pumps", stockLevel: 15, minAlertLevel: 3, supplierName: "Crompton Greaves", price: 4800.0 },
  { id: "I6", name: "Keltron 2.5uF Fan Capacitor", category: "Capacitors", stockLevel: 150, minAlertLevel: 50, supplierName: "Keltron Ltd", price: 35.0 }
];

export const SEED_PURCHASES: PurchaseRecord[] = [
  { id: "P1", itemName: "Anchor Modular Switch 6A", quantity: 100, supplierName: "Anchor Panasonic", cost: 1800.0, date: "2026-06-28" },
  { id: "P2", itemName: "Luminous 16A Single Pole MCB", quantity: 30, supplierName: "Luminous Power", cost: 3900.0, date: "2026-06-25" },
  { id: "P3", itemName: "Orient 48\" Ceiling Fan Black", quantity: 10, supplierName: "Orient Electric", cost: 17500.0, date: "2026-06-20" }
];

export const SEED_BILLS: Bill[] = [
  {
    id: "B1",
    customerName: "Amit Patel",
    customerPhone: "8146312456",
    invoiceNumber: "INV-2026-1001",
    storeName: "Rattan Electricals",
    items: [
      { name: "Anchor Modular Switch 6A", qty: 5, rate: 25.0, total: 125.0 },
      { name: "Keltron 2.5uF Fan Capacitor", qty: 1, rate: 45.0, total: 45.0 }
    ],
    laborCharges: 350.0,
    gstPercent: 18.0,
    discountAmount: 0.0,
    totalAmount: 613.6,
    date: "2026-06-29",
    isDownloaded: true,
    paymentMethod: "Cash",
    isMathematicallyValid: true,
    paymentStatus: "Paid",
    balanceDue: 0.0
  }
];

export const SEED_EXPENSES: Expense[] = [
  { id: "E1", category: "Tea & Refreshments", title: "Evening tea & snacks for staff", amount: 85.0, quantity: 1, date: "2026-07-01", voiceSummary: "Chai for 4 technicians" },
  { id: "E2", category: "Travel & Fuel", title: "Site check-in Phase 3B2 Mohali", amount: 150.0, quantity: 1, date: "2026-07-01", voiceSummary: "Conveyance auto fare" },
  { id: "E3", category: "Shop Maintenance", title: "Catalog rack cleaning supplies", amount: 120.0, quantity: 1, date: "2026-06-30", voiceSummary: "Cleaning brooms and duster" }
];

export const SEED_ATTENDANCE: AttendanceRecord[] = [
  { id: "A1", workerId: "W1", workerName: "Sukhvinder Singh", clockInTime: "09:00 AM", clockOutTime: "06:00 PM", date: "2026-06-30", isClockedIn: false },
  { id: "A2", workerId: "W2", workerName: "Baljinder Singh", clockInTime: "09:15 AM", clockOutTime: "06:15 PM", date: "2026-06-30", isClockedIn: false },
  { id: "A3", workerId: "W1", workerName: "Sukhvinder Singh", clockInTime: "08:55 AM", clockOutTime: null, date: "2026-07-01", isClockedIn: true }
];

// --- Database Initializer ---
export async function initializeDatabase() {
  const usersCount = await db.users.count();
  if (usersCount === 0) {
    // 1. Seed Master Admin (gursimarpsr@gmail.com / Admin@123)
    const adminSalt = Math.random().toString(36).substring(2, 10);
    const adminHash = await hashPasswordWithSalt("Admin@123", adminSalt);
    const masterAdmin: UserEntity = {
      id: "ADMIN_MASTER_GURSIMAR",
      email: "gursimarpsr@gmail.com",
      phone: "9872134590",
      displayName: "Gursimar Singh",
      passwordHash: adminHash,
      salt: adminSalt,
      role: "Admin",
      isApproved: true,
      isActive: true,
      failedAttempts: 0,
      lockUntilTimestamp: 0,
      createdAt: new Date().toISOString()
    };
    await db.users.put(masterAdmin);

    // 2. Seed Worker (worker@relect.com / Worker@123)
    const workerSalt = Math.random().toString(36).substring(2, 10);
    const workerHash = await hashPasswordWithSalt("Worker@123", workerSalt);
    const workerUser: UserEntity = {
      id: "WORKER_HARPREET",
      email: "worker@relect.com",
      phone: "9821034455",
      displayName: "Harpreet Singh",
      passwordHash: workerHash,
      salt: workerSalt,
      role: "Worker",
      isApproved: true,
      isActive: true,
      failedAttempts: 0,
      lockUntilTimestamp: 0,
      createdAt: new Date().toISOString()
    };
    await db.users.put(workerUser);

    // Log Seed Audit
    await db.auditLogs.put({
      id: "AUDIT_SEED",
      userId: masterAdmin.id,
      userName: "System",
      action: "SYSTEM_SEED",
      details: "Initialized Master Admin gursimarpsr@gmail.com & Worker worker@relect.com in Dexie IndexedDB",
      timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
    });
  } else {
    // Ensure Master Admin account exists and is not locked
    const master = await db.users.get("ADMIN_MASTER_GURSIMAR");
    if (master && (master.failedAttempts > 0 || master.lockUntilTimestamp > 0)) {
      await db.users.update("ADMIN_MASTER_GURSIMAR", {
        failedAttempts: 0,
        lockUntilTimestamp: 0,
        isActive: true,
        isApproved: true
      });
    }
  }

  // Check if store info exists
  const existingSupport = await db.settings.get('storeSupportInfo');
  if (!existingSupport) {
    await db.settings.put({ key: 'storeSupportInfo', value: DEFAULT_SUPPORT_INFO });
  }

  // One-time automatic purge of legacy mock data so the app starts clean & authentic
  const hasPurgedLegacy = localStorage.getItem('has_purged_mock_v3') === 'true';
  if (!hasPurgedLegacy) {
    localStorage.setItem('has_purged_mock_v3', 'true');
    localStorage.setItem('is_toy_data_cleared', 'true');
    await clearAllToyData();
  }
}

// --- Data Management Operations ---

export async function clearAllToyData() {
  localStorage.setItem('is_toy_data_cleared', 'true');
  await db.transaction('rw', [db.customers, db.services, db.workers, db.inventory, db.purchases, db.bills, db.expenses, db.attendance], async () => {
    await db.customers.clear();
    await db.services.clear();
    await db.workers.clear();
    await db.inventory.clear();
    await db.purchases.clear();
    await db.bills.clear();
    await db.expenses.clear();
    await db.attendance.clear();
  });
}

export async function resetMasterAdminPassword(newPassword: string): Promise<boolean> {
  try {
    const salt = Math.random().toString(36).substring(2, 10);
    const hash = await hashPasswordWithSalt(newPassword, salt);
    const master = await db.users.get("ADMIN_MASTER_GURSIMAR");
    if (master) {
      await db.users.update("ADMIN_MASTER_GURSIMAR", {
        passwordHash: hash,
        salt,
        failedAttempts: 0,
        lockUntilTimestamp: 0,
        isActive: true,
        isApproved: true
      });
    } else {
      await db.users.put({
        id: "ADMIN_MASTER_GURSIMAR",
        email: "gursimarpsr@gmail.com",
        phone: "9872134590",
        displayName: "Gursimar Singh",
        passwordHash: hash,
        salt,
        role: "Admin",
        isApproved: true,
        isActive: true,
        failedAttempts: 0,
        lockUntilTimestamp: 0,
        createdAt: new Date().toISOString()
      });
    }
    return true;
  } catch (e) {
    console.error('Failed to reset admin password:', e);
    return false;
  }
}

export async function restoreDemoData() {
  localStorage.removeItem('is_toy_data_cleared');
  await db.transaction('rw', [db.customers, db.services, db.workers, db.inventory, db.purchases, db.bills, db.expenses, db.attendance], async () => {
    await db.customers.clear();
    await db.services.clear();
    await db.workers.clear();
    await db.inventory.clear();
    await db.purchases.clear();
    await db.bills.clear();
    await db.expenses.clear();
    await db.attendance.clear();

    await db.customers.bulkPut(SEED_CUSTOMERS);
    await db.services.bulkPut(SEED_SERVICES);
    await db.workers.bulkPut(SEED_WORKERS);
    await db.inventory.bulkPut(SEED_INVENTORY);
    await db.purchases.bulkPut(SEED_PURCHASES);
    await db.bills.bulkPut(SEED_BILLS);
    await db.expenses.bulkPut(SEED_EXPENSES);
    await db.attendance.bulkPut(SEED_ATTENDANCE);
  });
}

// --- Specific Entity Helpers ---

export async function addPurchaseRecordAndSyncStock(record: PurchaseRecord) {
  await db.purchases.put(record);
  const matched = await db.inventory.where('name').equalsIgnoreCase(record.itemName).first();
  if (matched) {
    await db.inventory.update(matched.id, {
      stockLevel: matched.stockLevel + record.quantity
    });
  } else {
    await db.inventory.put({
      id: "INV_" + Date.now().toString(36),
      name: record.itemName,
      category: "General",
      stockLevel: record.quantity,
      minAlertLevel: 5,
      supplierName: record.supplierName,
      price: record.quantity > 0 ? record.cost / record.quantity : 0
    });
  }
}

export async function addBillAndDecrementStock(bill: Bill) {
  await db.bills.put(bill);
  for (const item of bill.items) {
    const matched = await db.inventory.where('name').equalsIgnoreCase(item.name).first();
    if (matched) {
      const newStock = Math.max(0, matched.stockLevel - item.qty);
      await db.inventory.update(matched.id, { stockLevel: newStock });
    }
  }
}
