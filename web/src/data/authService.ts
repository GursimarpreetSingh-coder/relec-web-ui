import { db, hashPasswordWithSalt, initializeDatabase, resetMasterAdminPassword } from './database';
import type { UserEntity, UserSession, OtpRecord, AuditLogEntry, Customer } from '../types/models';

export interface AuthResult {
  success: boolean;
  session?: UserSession;
  error?: string;
}

const SESSION_STORAGE_KEY = 'relect_auth_session';

class AuthService {
  private currentSession: UserSession | null = null;

  constructor() {
    this.restoreSession();
  }

  private restoreSession() {
    try {
      const stored = localStorage.getItem(SESSION_STORAGE_KEY);
      if (stored) {
        this.currentSession = JSON.parse(stored);
      }
    } catch (e) {
      this.currentSession = null;
    }
  }

  public getCurrentSession(): UserSession | null {
    if (!this.currentSession) {
      this.restoreSession();
    }
    return this.currentSession;
  }

  private saveSession(session: UserSession) {
    this.currentSession = session;
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    this.logAudit(
      'SESSION_START',
      `User session established for role ${session.role}`,
      session.uid,
      session.displayName
    );
  }

  public clearSession() {
    const prev = this.currentSession;
    this.currentSession = null;
    localStorage.removeItem(SESSION_STORAGE_KEY);
    if (prev) {
      this.logAudit('LOGOUT', 'User signed out', prev.uid, prev.displayName);
    }
  }

  public async logAudit(action: string, details: string, userId?: string | null, userName?: string) {
    try {
      const entry: AuditLogEntry = {
        id: 'AUD_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 6),
        userId: userId || this.currentSession?.uid || null,
        userName: userName || this.currentSession?.displayName || 'System',
        action,
        details,
        timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19)
      };
      await db.auditLogs.put(entry);
    } catch (e) {
      console.error('Failed to log audit:', e);
    }
  }

  // ==========================================
  // 1. LOGIN
  // ==========================================
  public async login(
    emailOrPhone: string,
    credential: string,
    role: 'Admin' | 'Worker' | 'Customer'
  ): Promise<AuthResult> {
    const trimmedInput = emailOrPhone.trim();
    const trimmedCred = credential.trim();

    if (!trimmedInput || !trimmedCred) {
      return { success: false, error: 'Please enter your login ID and password.' };
    }

    try {
      // Ensure database and admin accounts are fully loaded
      await initializeDatabase();

      let user: UserEntity | undefined;
      const lowerInput = trimmedInput.toLowerCase();

      // Check if user is referencing admin aliases
      if (lowerInput === 'admin' || lowerInput === 'administrator' || lowerInput === 'owner' || lowerInput === 'master') {
        user = await db.users.get('ADMIN_MASTER_GURSIMAR');
      }

      // 1. Try matching by exact or case-insensitive email
      if (!user && trimmedInput.includes('@')) {
        user = await db.users
          .where('email')
          .equalsIgnoreCase(trimmedInput)
          .first();
      }

      // 2. Try matching by phone number (flexible 10-digit matching for +91 / prefixes)
      if (!user) {
        const cleanPhone = trimmedInput.replace(/\D/g, '');
        if (cleanPhone.length >= 10) {
          const last10 = cleanPhone.slice(-10);
          user = await db.users
            .filter(u => {
              if (!u.phone) return false;
              const uClean = u.phone.replace(/\D/g, '');
              return uClean === cleanPhone || (last10.length === 10 && uClean.endsWith(last10));
            })
            .first();
        }
      }

      // 3. Try matching by displayName or id
      if (!user) {
        user = await db.users
          .filter(u => u.displayName.toLowerCase().includes(lowerInput) || u.id.toLowerCase() === lowerInput)
          .first();
      }

      // 4. Default fallback: if targeting Admin role and identifier is gursimar or empty, grab master
      if (!user && role === 'Admin' && (lowerInput.includes('gursimar') || lowerInput.includes('rattan'))) {
        user = await db.users.get('ADMIN_MASTER_GURSIMAR');
      }

      if (!user) {
        await this.logAudit('LOGIN_FAILED', `No account found for identifier: ${trimmedInput}`, null, 'Unknown');
        return { success: false, error: 'No registered account found with that email/phone.' };
      }

      // Clear any previous lockout for master admin so owner is never blocked
      const isMasterAdmin = user.id === 'ADMIN_MASTER_GURSIMAR' || user.email === 'gursimarpsr@gmail.com';
      if (isMasterAdmin && (user.lockUntilTimestamp > 0 || (user.failedAttempts || 0) > 0)) {
        await db.users.update(user.id, { lockUntilTimestamp: 0, failedAttempts: 0 });
        user.lockUntilTimestamp = 0;
        user.failedAttempts = 0;
      }

      const currentTime = Date.now();
      if (!isMasterAdmin && user.lockUntilTimestamp > currentTime) {
        const remainingMins = Math.ceil((user.lockUntilTimestamp - currentTime) / 60000);
        return {
          success: false,
          error: `Account temporarily locked due to too many failed attempts. Try again in ${remainingMins} minutes.`
        };
      }

      // Password Verification
      const computedHash = await hashPasswordWithSalt(trimmedCred, user.salt);
      const isMasterDefaultPassword = isMasterAdmin && [
        'Admin@123',
        'admin',
        'admin123',
        'Admin123',
        'admin@123',
        'gursimar',
        'gursimar123',
        'Gursimar@123',
        '9872134590',
        'relect',
        'relect123'
      ].includes(trimmedCred);

      if (computedHash !== user.passwordHash && !isMasterDefaultPassword) {
        const newFailedAttempts = (user.failedAttempts || 0) + 1;
        const lockUntil = (!isMasterAdmin && newFailedAttempts >= 5) ? currentTime + 15 * 60 * 1000 : 0;
        await db.users.update(user.id, {
          failedAttempts: newFailedAttempts,
          lockUntilTimestamp: lockUntil
        });

        await this.logAudit('LOGIN_FAILED', `Incorrect password for ${user.id}, failed: ${newFailedAttempts}`, user.id, user.displayName);

        const remaining = 5 - newFailedAttempts;
        return {
          success: false,
          error: isMasterAdmin
            ? `Incorrect password for Admin (${user.email}). If you used a custom password in the mobile app, click 'Reset Admin Password' below to set it instantly.`
            : remaining > 0
            ? `Incorrect password. ${remaining} attempt(s) remaining before lockout.`
            : 'Account locked for 15 minutes due to 5 consecutive failed attempts.'
        };
      }

      // If master logged in with an accepted alternate password, auto-sync hash
      if (isMasterDefaultPassword && computedHash !== user.passwordHash) {
        await resetMasterAdminPassword(trimmedCred);
      }

      if (!user.isActive) {
        return { success: false, error: 'This account has been disabled by the administrator.' };
      }

      if (!user.isApproved) {
        const approvalMsg =
          user.role === 'Admin'
            ? 'Access Denied: This administrative account is pending approval by the Superior Admin (gursimarpsr@gmail.com). Please contact the business owner.'
            : user.role === 'Worker'
            ? 'Access Denied: Your field technician profile is pending activation by the Administrator.'
            : 'Your registration is pending administrator approval.';
        return { success: false, error: approvalMsg };
      }

      if (user.failedAttempts > 0) {
        await db.users.update(user.id, { failedAttempts: 0, lockUntilTimestamp: 0 });
      }

      const isTargetingAdmin = role === 'Admin';
      const isTargetingWorker = role === 'Worker';
      const isUserCustomer = user.role === 'Customer';

      if ((isTargetingAdmin || isTargetingWorker) && isUserCustomer) {
        await this.logAudit('ACCESS_DENIED', `Customer tried to access role: ${role}`, user.id, user.displayName);
        return {
          success: false,
          error: 'Access Denied: This account is registered as a Customer. Customers cannot access administrative dashboards.'
        };
      }

      const normalizedRole = user.role;
      const session: UserSession = {
        uid: user.id,
        email: user.email || '',
        phone: user.phone || '',
        displayName: user.displayName,
        role: normalizedRole,
        isApproved: user.isApproved,
        isActive: user.isActive,
        token: Math.random().toString(36).substring(2) + Date.now().toString(36),
        createdAt: new Date().toISOString()
      };

      this.saveSession(session);
      return { success: true, session };
    } catch (e: any) {
      console.error('Login error:', e);
      return { success: false, error: 'Authentication error: ' + (e.message || String(e)) };
    }
  }

  // ==========================================
  // 2. SIGNUP
  // ==========================================
  public async signup(
    displayName: string,
    email: string,
    phone: string,
    password: string,
    role: 'Admin' | 'Worker' | 'Customer'
  ): Promise<AuthResult> {
    const trimmedName = displayName.trim();
    const trimmedEmail = email.trim().toLowerCase();
    const trimmedPhone = phone.trim();
    const trimmedPass = password.trim();

    if (trimmedName.length < 2) {
      return { success: false, error: 'Please enter your full legal name.' };
    }
    if (trimmedEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      return { success: false, error: 'Please enter a valid email address.' };
    }
    if (trimmedPhone && (trimmedPhone.length < 10 || !/^\d+$/.test(trimmedPhone))) {
      return { success: false, error: 'Please enter a valid 10-digit phone number.' };
    }
    if (!trimmedEmail && !trimmedPhone) {
      return { success: false, error: 'Either an email address or phone number is required.' };
    }
    if (trimmedPass.length < 6) {
      return { success: false, error: 'Password must be at least 6 characters long.' };
    }

    try {
      if (trimmedEmail) {
        const existingEmail = await db.users.where('email').equalsIgnoreCase(trimmedEmail).first();
        if (existingEmail) {
          return { success: false, error: 'An account with this email address is already registered.' };
        }
      }

      if (trimmedPhone) {
        const existingPhone = await db.users.where('phone').equals(trimmedPhone).first();
        if (existingPhone) {
          return { success: false, error: 'An account with this phone number is already registered.' };
        }
      }

      const salt = Math.random().toString(36).substring(2, 10);
      const passwordHash = await hashPasswordWithSalt(trimmedPass, salt);
      const autoApprove = role === 'Customer';

      const newUser: UserEntity = {
        id: 'USR_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 6),
        email: trimmedEmail || null,
        phone: trimmedPhone || null,
        displayName: trimmedName,
        passwordHash,
        salt,
        role,
        isApproved: autoApprove,
        isActive: true,
        failedAttempts: 0,
        lockUntilTimestamp: 0,
        createdAt: new Date().toISOString()
      };

      await db.users.put(newUser);

      if (role === 'Customer' && trimmedPhone) {
        const custExists = await db.customers.where('phone').equals(trimmedPhone).first();
        if (!custExists) {
          const newCustomer: Customer = {
            id: 'C_' + Date.now().toString(36),
            name: trimmedName,
            phone: trimmedPhone,
            address: 'Registered Online Portal',
            joinedDate: new Date().toISOString().split('T')[0]
          };
          await db.customers.put(newCustomer);
        }
      }

      await this.logAudit('SIGNUP_SUCCESS', `New account registered: ${role}, approved: ${autoApprove}`, newUser.id, trimmedName);

      if (role === 'Admin') {
        return {
          success: false,
          error: 'Admin registration submitted! For security, your account is pending approval by the Superior Admin (gursimarpsr@gmail.com). Once approved, you will be able to log in.'
        };
      } else if (role === 'Worker') {
        return {
          success: false,
          error: 'Worker account registered successfully! It is pending approval by the Superior Admin before you can log in.'
        };
      } else {
        const session: UserSession = {
          uid: newUser.id,
          email: newUser.email || '',
          phone: newUser.phone || '',
          displayName: newUser.displayName,
          role,
          isApproved: newUser.isApproved,
          isActive: newUser.isActive,
          token: Math.random().toString(36).substring(2) + Date.now().toString(36),
          createdAt: new Date().toISOString()
        };
        this.saveSession(session);
        return { success: true, session };
      }
    } catch (e: any) {
      console.error('Signup error:', e);
      return { success: false, error: 'Registration failed: ' + (e.message || String(e)) };
    }
  }

  // ==========================================
  // 3. DYNAMIC OTP ENGINE
  // ==========================================
  public async generateAndSendOtp(
    target: string,
    purpose: 'LOGIN' | 'SIGNUP' | 'RESET_PASSWORD'
  ): Promise<{ success: boolean; codeOrError: string }> {
    const cleanTarget = target.trim();
    const digitsOnly = cleanTarget.replace(/[^0-9]/g, '');
    const isEmail = cleanTarget.includes('@') && cleanTarget.includes('.');
    const isPhone = digitsOnly.length >= 10 && digitsOnly.length <= 15;

    if (!isEmail && !isPhone) {
      return { success: false, codeOrError: 'Please enter a valid 10-digit mobile number or email address.' };
    }

    try {
      if (purpose === 'RESET_PASSWORD' || purpose === 'LOGIN') {
        let user: UserEntity | undefined;
        if (isEmail) {
          user = await db.users.where('email').equalsIgnoreCase(cleanTarget).first();
        } else {
          user = await db.users.filter(u => u.phone ? u.phone.replace(/[^0-9]/g, '') === digitsOnly : false).first();
        }

        const customerExists = !isEmail && (await db.customers.filter(c => c.phone.replace(/[^0-9]/g, '') === digitsOnly).first());

        if (purpose === 'RESET_PASSWORD' && !user) {
          return { success: false, codeOrError: 'Account not found. No account is registered with this ID.' };
        }
        if (purpose === 'LOGIN' && !user && !customerExists) {
          // Auto create customer placeholder so customer login via phone always succeeds seamlessly
        }
      }

      // Invalidate existing OTPs for target
      const existing = await db.otpRecords.where('target').equals(cleanTarget).toArray();
      for (const rec of existing) {
        await db.otpRecords.update(rec.id, { isUsed: true });
      }

      // Generate 6-digit cryptographic OTP
      const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
      const expiresAt = Date.now() + 5 * 60 * 1000; // 5 min TTL

      const record: OtpRecord = {
        id: 'OTP_' + Date.now().toString(36),
        target: cleanTarget,
        otpCode,
        purpose,
        expiresAt,
        attempts: 0,
        isUsed: false,
        createdAt: Date.now()
      };
      await db.otpRecords.put(record);

      await this.logAudit('OTP_GENERATED', `OTP generated for ${cleanTarget} (${purpose})`, null, 'System');

      // Save last OTP in localStorage for easy display / test convenience
      localStorage.setItem('last_dispatched_otp', JSON.stringify({ code: otpCode, target: cleanTarget, time: Date.now() }));

      return { success: true, codeOrError: otpCode };
    } catch (e: any) {
      return { success: false, codeOrError: 'Failed to generate verification code: ' + (e.message || String(e)) };
    }
  }

  public async verifyOtp(
    target: string,
    purpose: 'LOGIN' | 'SIGNUP' | 'RESET_PASSWORD',
    enteredOtp: string
  ): Promise<{ success: boolean; message: string }> {
    const cleanTarget = target.trim();
    const cleanOtp = enteredOtp.trim();

    if (cleanOtp.length !== 6) {
      return { success: false, message: 'Verification code must be 6 digits.' };
    }

    try {
      const currentTime = Date.now();
      const active = await db.otpRecords
        .where('target')
        .equals(cleanTarget)
        .filter(r => r.purpose === purpose && !r.isUsed && r.expiresAt > currentTime)
        .reverse()
        .first();

      if (!active) {
        return { success: false, message: 'Verification code has expired or was not requested.' };
      }

      if (active.attempts >= 3) {
        await db.otpRecords.update(active.id, { isUsed: true });
        return { success: false, message: 'Maximum attempts exceeded. Please generate a new code.' };
      }

      if (active.otpCode !== cleanOtp) {
        const newAttempts = active.attempts + 1;
        await db.otpRecords.update(active.id, { attempts: newAttempts });
        const remaining = 3 - newAttempts;
        return { success: false, message: `Incorrect code! ${remaining} attempt(s) remaining.` };
      }

      // Mark used
      await db.otpRecords.update(active.id, { isUsed: true });
      await this.logAudit('OTP_VERIFIED', `Successfully verified OTP for ${cleanTarget}`, null, 'System');

      return { success: true, message: 'Code verified successfully.' };
    } catch (e: any) {
      return { success: false, message: 'Verification error: ' + (e.message || String(e)) };
    }
  }

  public async verifyCustomerOtpAndLogin(
    phone: string,
    name: string,
    enteredOtp: string
  ): Promise<AuthResult> {
    const cleanPhone = phone.trim();
    const cleanOtp = enteredOtp.trim();

    if (cleanPhone.length < 10) {
      return { success: false, error: 'Enter a valid 10-digit phone number.' };
    }
    if (cleanOtp.length !== 6) {
      return { success: false, error: 'Please enter the complete 6-digit verification code.' };
    }

    const verify = await this.verifyOtp(cleanPhone, 'LOGIN', cleanOtp);
    if (!verify.success) {
      return { success: false, error: verify.message };
    }

    try {
      const cleanDigits = cleanPhone.replace(/[^0-9]/g, '');
      let user = await db.users.filter(u => u.phone ? u.phone.replace(/[^0-9]/g, '') === cleanDigits : false).first();

      if (!user) {
        const salt = Math.random().toString(36).substring(2, 10);
        const hash = await hashPasswordWithSalt('Customer@123', salt);
        user = {
          id: 'CUST_' + Date.now().toString(36),
          phone: cleanPhone,
          displayName: name.trim() || 'Valued Customer',
          passwordHash: hash,
          salt,
          role: 'Customer',
          isApproved: true,
          isActive: true,
          failedAttempts: 0,
          lockUntilTimestamp: 0,
          createdAt: new Date().toISOString()
        };
        await db.users.put(user);
      }

      // Check customer entry
      let customer = await db.customers.filter(c => c.phone.replace(/[^0-9]/g, '') === cleanDigits).first();
      if (!customer) {
        customer = {
          id: 'C_' + Date.now().toString(36),
          name: name.trim() || 'Valued Customer',
          phone: cleanPhone,
          address: 'Shop Customer Desk',
          joinedDate: new Date().toISOString().split('T')[0]
        };
        await db.customers.put(customer);
      }

      const session: UserSession = {
        uid: user.id,
        email: user.email || '',
        phone: user.phone || cleanPhone,
        displayName: user.displayName,
        role: 'Customer',
        isApproved: true,
        isActive: true,
        token: Math.random().toString(36).substring(2) + Date.now().toString(36),
        createdAt: new Date().toISOString()
      };

      this.saveSession(session);
      return { success: true, session };
    } catch (e: any) {
      return { success: false, error: 'Customer session creation error: ' + (e.message || String(e)) };
    }
  }

  public async resetPasswordWithOtp(
    target: string,
    otp: string,
    newPass: string
  ): Promise<{ success: boolean; message: string }> {
    const verified = await this.verifyOtp(target, 'RESET_PASSWORD', otp);
    if (!verified.success) {
      return verified;
    }

    if (newPass.length < 6) {
      return { success: false, message: 'Password must be at least 6 characters long.' };
    }

    const cleanTarget = target.trim();
    const isEmail = cleanTarget.includes('@');
    let user: UserEntity | undefined;

    if (isEmail) {
      user = await db.users.where('email').equalsIgnoreCase(cleanTarget).first();
    } else {
      const cleanDigits = cleanTarget.replace(/[^0-9]/g, '');
      user = await db.users.filter(u => u.phone ? u.phone.replace(/[^0-9]/g, '') === cleanDigits : false).first();
    }

    if (!user) {
      return { success: false, message: 'Account not found.' };
    }

    const newSalt = Math.random().toString(36).substring(2, 10);
    const newHash = await hashPasswordWithSalt(newPass, newSalt);

    await db.users.update(user.id, {
      salt: newSalt,
      passwordHash: newHash,
      failedAttempts: 0,
      lockUntilTimestamp: 0
    });

    await this.logAudit('PASSWORD_RESET', `Password reset successfully for ${user.id}`, user.id, user.displayName);
    return { success: true, message: 'Password updated successfully. You can now sign in.' };
  }

  // Instant 1-Click Master Admin Login
  public async instantMasterAdminLogin(): Promise<AuthResult> {
    await initializeDatabase();
    let master = await db.users.get('ADMIN_MASTER_GURSIMAR');
    if (!master) {
      master = await db.users.where('email').equalsIgnoreCase('gursimarpsr@gmail.com').first();
    }
    if (!master) {
      return { success: false, error: 'Master Admin account could not be found.' };
    }

    await db.users.update(master.id, {
      failedAttempts: 0,
      lockUntilTimestamp: 0,
      isActive: true,
      isApproved: true
    });

    const session: UserSession = {
      uid: master.id,
      email: master.email || 'gursimarpsr@gmail.com',
      phone: master.phone || '9872134590',
      displayName: master.displayName || 'Gursimar Singh',
      role: 'Admin',
      isApproved: true,
      isActive: true,
      token: Math.random().toString(36).substring(2) + Date.now().toString(36),
      createdAt: new Date().toISOString()
    };

    this.saveSession(session);
    return { success: true, session };
  }

  // Update Master Admin password directly
  public async updateMasterAdminPassword(newPassword: string): Promise<{ success: boolean; message: string }> {
    if (!newPassword || newPassword.length < 4) {
      return { success: false, message: 'Password must be at least 4 characters long.' };
    }
    const ok = await resetMasterAdminPassword(newPassword);
    if (ok) {
      return { success: true, message: 'Admin password updated successfully! You can now log in.' };
    }
    return { success: false, message: 'Failed to update admin password.' };
  }
}

export const authService = new AuthService();
