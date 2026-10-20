export type ServiceStatus = 'CREATED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY';
export type WorkerStatus = 'WORKING' | 'ASSIGNED_NOT_STARTED' | 'IDLE';

export interface Customer {
  id: string;
  name: string;
  phone: string;
  address: string;
  joinedDate: string;
}

export interface StatusChange {
  status: ServiceStatus;
  timestamp: string;
  notes: string;
}

export interface ServiceRequest {
  id: string;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  problemDescription: string;
  category: string;
  priority: Priority;
  assignedWorkerId?: string | null;
  assignedWorkerName?: string | null;
  status: ServiceStatus;
  preferredDateTime: string;
  photoUri?: string | null;
  dateCreated: string;
  statusHistory: StatusChange[];
}

export interface Worker {
  id: string;
  name: string;
  phone: string;
  salary: number;
  designation: string; // Electrician, Helper, Senior Technician
  status: WorkerStatus;
  joiningDate: string;
  currentJobId?: string | null;
  isVerified: boolean;
  isApproved: boolean;
  isActive: boolean;
  email?: string | null;
}

export interface InventoryItem {
  id: string;
  name: string;
  category: string;
  stockLevel: number;
  minAlertLevel: number;
  supplierName: string;
  price: number;
}

export interface PurchaseRecord {
  id: string;
  itemName: string;
  quantity: number;
  supplierName: string;
  cost: number;
  date: string;
  photoUri?: string | null;
}

export interface BillItem {
  name: string;
  qty: number;
  rate: number;
  total: number;
}

export interface Bill {
  id: string;
  customerName: string;
  customerPhone: string;
  items: BillItem[];
  laborCharges: number;
  gstPercent: number;
  discountAmount: number;
  totalAmount: number;
  date: string;
  isDownloaded: boolean;
  storeName?: string;
  invoiceNumber?: string;
  paymentMethod?: string;
  isMathematicallyValid: boolean;
  discrepancyNote?: string | null;
  sentToCustomerAt?: string | null;
  sentVia?: string | null;
  paymentStatus: string; // Paid, Pending, Partial
  balanceDue: number;
}

export interface Expense {
  id: string;
  category: string;
  title: string;
  amount: number;
  quantity: number;
  date: string;
  voiceSummary?: string | null;
}

export interface AttendanceRecord {
  id: string;
  workerId: string;
  workerName: string;
  clockInTime: string;
  clockOutTime?: string | null;
  date: string;
  isClockedIn: boolean;
}

export interface UserEntity {
  id: string;
  email?: string | null;
  phone?: string | null;
  displayName: string;
  passwordHash: string;
  salt: string;
  role: 'Admin' | 'Worker' | 'Customer';
  isApproved: boolean;
  isActive: boolean;
  failedAttempts: number;
  lockUntilTimestamp: number;
  createdAt: string;
}

export interface OtpRecord {
  id: string;
  target: string;
  otpCode: string;
  purpose: 'LOGIN' | 'SIGNUP' | 'RESET_PASSWORD';
  expiresAt: number;
  attempts: number;
  isUsed: boolean;
  createdAt: number;
}

export interface AuditLogEntry {
  id: string;
  userId?: string | null;
  userName: string;
  action: string;
  details: string;
  timestamp: string;
}

export interface StoreSupportInfo {
  storeName: string;
  phone: string;
  whatsapp: string;
  address: string;
  workingHours: string;
  emergencyHelpline: string;
  safetyGuidelines: string;
  appDownloadUrl: string;
}

export interface ScannedInvoiceItem {
  name: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface ScannedInvoiceResult {
  storeName: string;
  invoiceNumber?: string | null;
  date: string;
  time?: string | null;
  items: ScannedInvoiceItem[];
  subtotal: number;
  taxPercent: number;
  taxAmount: number;
  discountAmount: number;
  grandTotal: number;
  paymentMethod?: string | null;
  isMathematicallyValid: boolean;
  discrepancyNote?: string | null;
  confidenceScore: number;
}

export interface VisualIdentificationResult {
  isInvoice: boolean;
  title: string;
  brand: string;
  category: string;
  quantity: number;
  unit: string;
  estimatedUnitPrice: number;
  estimatedTotalPrice: number;
  specifications: string;
  invoiceDetails?: ScannedInvoiceResult | null;
  detectedTags: string[];
  rawDescription: string;
  confidence: number;
}

export type VoiceAction =
  | 'ADD_ITEM'
  | 'UPDATE_QUANTITY'
  | 'SET_PRICE'
  | 'DELETE_ITEM'
  | 'NAVIGATE'
  | 'SAVE_ENTRY'
  | 'ADD_SERVICE_REQUEST'
  | 'LOG_EXPENSE'
  | 'GENERAL_QUERY';

export interface ParsedVoiceIntent {
  action: VoiceAction;
  itemName: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  totalPrice: number;
  category: string;
  supplierName: string;
  targetScreen?: string | null;
  customerPhone?: string | null;
  rawTranscript: string;
  detectedLanguage: string;
  confidence: number;
}

export interface UserSession {
  uid: string;
  email: string;
  phone: string;
  displayName: string;
  role: 'Admin' | 'Worker' | 'Customer';
  isApproved: boolean;
  isActive: boolean;
  token: string;
  createdAt: string;
}
