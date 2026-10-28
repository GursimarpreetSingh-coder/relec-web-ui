import React, { useState } from 'react';
import { useTheme } from './theme';
import { authService } from '../data/authService';
import type { UserSession } from '../types/models';
import {
  ShieldCheck,
  Wrench,
  User,
  Lock,
  Mail,
  Phone,
  Eye,
  EyeOff,
  Sun,
  Moon,
  Sparkles,
  CheckCircle,
  AlertCircle,
  ArrowRight,
  KeyRound,
  RotateCcw,
  Download,
  X,
  Layers,
  Database,
  Zap
} from 'lucide-react';

interface LoginScreenProps {
  onLoginSuccess: (role: 'Admin' | 'Worker' | 'Customer', session: UserSession) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
  const {
    brandTheme,
    setBrandTheme,
    isDarkTheme,
    toggleDarkTheme,
    accentColor,
    isRevolt,
    logoSrc,
    bannerSrc
  } = useTheme();

  const [selectedRole, setSelectedRole] = useState<'Admin' | 'Worker' | 'Customer'>('Admin');
  const [authMode, setAuthMode] = useState<'SIGN_IN' | 'SIGN_UP' | 'FORGOT_PASSWORD'>('SIGN_IN');

  // Sign In state
  const [loginIdentifier, setLoginIdentifier] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Customer OTP state
  const [customerName, setCustomerName] = useState('Anita Sharma');
  const [customerPhone, setCustomerPhone] = useState('9417255831');
  const [customerOtp, setCustomerOtp] = useState('');
  const [isOtpSent, setIsOtpSent] = useState(false);
  const [dispatchedOtpCode, setDispatchedOtpCode] = useState('');

  // Sign Up state
  const [signupName, setSignupName] = useState('');
  const [signupEmail, setSignupEmail] = useState('');
  const [signupPhone, setSignupPhone] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupRole, setSignupRole] = useState<'Admin' | 'Worker' | 'Customer'>('Customer');

  // Forgot password state
  const [resetTarget, setResetTarget] = useState('');
  const [resetOtp, setResetOtp] = useState('');
  const [resetNewPass, setResetNewPass] = useState('');
  const [resetOtpSent, setResetOtpSent] = useState(false);

  // UI feedback
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const clearMessages = () => {
    setErrorMsg('');
    setSuccessMsg('');
  };

  // Switch role handler: auto-adjust demo credentials
  const handleRoleChange = (role: 'Admin' | 'Worker' | 'Customer') => {
    setSelectedRole(role);
    clearMessages();
    if (role === 'Admin') {
      setLoginIdentifier('gursimarpsr@gmail.com');
      setLoginPassword('Admin@123');
    } else if (role === 'Worker') {
      setLoginIdentifier('worker@relect.com');
      setLoginPassword('Worker@123');
    }
  };

  // Sign in submit
  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    clearMessages();
    setIsLoading(true);

    const result = await authService.login(loginIdentifier, loginPassword, selectedRole);
    setIsLoading(false);

    if (result.success && result.session) {
      onLoginSuccess(result.session.role, result.session);
    } else {
      setErrorMsg(result.error || 'Authentication failed');
    }
  };

  // Instant 1-Click Master Admin Login
  const handleInstantAdminLogin = async () => {
    clearMessages();
    setIsLoading(true);
    const result = await authService.instantMasterAdminLogin();
    setIsLoading(false);

    if (result.success && result.session) {
      onLoginSuccess('Admin', result.session);
    } else {
      setErrorMsg(result.error || 'Failed to initialize Master Admin.');
    }
  };

  // Guest Sandbox Login for GitHub Visitors
  const handleEnterSandbox = async (loadSample: boolean) => {
    clearMessages();
    setIsLoading(true);
    const result = await authService.loginAsGuestSandbox(loadSample);
    setIsLoading(false);

    if (result.success && result.session) {
      onLoginSuccess('Admin', result.session);
    } else {
      setErrorMsg(result.error || 'Failed to enter sandbox mode.');
    }
  };

  // Set / Reset Admin Password Directly
  const [showAdminResetModal, setShowAdminResetModal] = useState(false);
  const [newAdminPasswordInput, setNewAdminPasswordInput] = useState('');

  const handleSaveAdminPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    clearMessages();
    if (!newAdminPasswordInput.trim()) {
      setErrorMsg('Please enter a new password.');
      return;
    }
    setIsLoading(true);
    const res = await authService.updateMasterAdminPassword(newAdminPasswordInput.trim());
    setIsLoading(false);

    if (res.success) {
      setSuccessMsg(res.message);
      setLoginPassword(newAdminPasswordInput.trim());
      setShowAdminResetModal(false);
      // Auto-login immediately
      const autoRes = await authService.login(loginIdentifier, newAdminPasswordInput.trim(), 'Admin');
      if (autoRes.success && autoRes.session) {
        onLoginSuccess('Admin', autoRes.session);
      }
    } else {
      setErrorMsg(res.message);
    }
  };

  // Customer send OTP
  const handleSendCustomerOtp = async () => {
    clearMessages();
    if (!customerPhone || customerPhone.replace(/\D/g, '').length < 10) {
      setErrorMsg('Please enter a valid 10-digit mobile number.');
      return;
    }
    setIsLoading(true);
    const res = await authService.generateAndSendOtp(customerPhone, 'LOGIN');
    setIsLoading(false);

    if (res.success) {
      setIsOtpSent(true);
      setDispatchedOtpCode(res.codeOrError);
      setCustomerOtp(res.codeOrError); // auto-fill for frictionless UX
      setSuccessMsg(`Verification OTP ${res.codeOrError} sent to +91 ${customerPhone}!`);
    } else {
      setErrorMsg(res.codeOrError);
    }
  };

  // Customer verify OTP
  const handleVerifyCustomerOtp = async () => {
    clearMessages();
    setIsLoading(true);
    const res = await authService.verifyCustomerOtpAndLogin(customerPhone, customerName, customerOtp);
    setIsLoading(false);

    if (res.success) {
      onLoginSuccess('Customer', res.session);
    } else {
      setErrorMsg(res.error);
    }
  };

  // Sign up submit
  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    clearMessages();
    setIsLoading(true);

    const res = await authService.signup(signupName, signupEmail, signupPhone, signupPassword, signupRole);
    setIsLoading(false);

    if (res.success) {
      onLoginSuccess(res.session.role, res.session);
    } else {
      if (res.error.includes('pending approval')) {
        setSuccessMsg(res.error);
        setTimeout(() => setAuthMode('SIGN_IN'), 3000);
      } else {
        setErrorMsg(res.error);
      }
    }
  };

  // Forgot password OTP
  const handleSendResetOtp = async () => {
    clearMessages();
    if (!resetTarget) {
      setErrorMsg('Enter your email or phone number.');
      return;
    }
    setIsLoading(true);
    const res = await authService.generateAndSendOtp(resetTarget, 'RESET_PASSWORD');
    setIsLoading(false);

    if (res.success) {
      setResetOtpSent(true);
      setResetOtp(res.codeOrError);
      setSuccessMsg(`Reset code ${res.codeOrError} generated for ${resetTarget}.`);
    } else {
      setErrorMsg(res.codeOrError);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    clearMessages();
    setIsLoading(true);
    const res = await authService.resetPasswordWithOtp(resetTarget, resetOtp, resetNewPass);
    setIsLoading(false);

    if (res.success) {
      setSuccessMsg(res.message);
      setTimeout(() => setAuthMode('SIGN_IN'), 2000);
    } else {
      setErrorMsg(res.message);
    }
  };

  return (
    <div className={`min-h-screen flex flex-col justify-center items-center p-4 transition-colors duration-300 relative overflow-hidden ${isDarkTheme ? 'bg-slate-950 text-slate-100' : 'bg-slate-50 text-slate-900'}`}>
      {/* Ambient Theme Background Image */}
      <div
        className="fixed inset-0 pointer-events-none opacity-[0.08] dark:opacity-[0.14] bg-cover bg-center filter blur-xl scale-110 transition-all duration-700"
        style={{ backgroundImage: `url(${bannerSrc})` }}
      />

      {/* Top Bar Controls */}
      <div className="w-full max-w-md flex items-center justify-between mb-4 px-2 z-10">
        {/* Brand Switcher Pill */}
        <div className={`flex items-center p-1 rounded-xl text-xs font-bold border ${isDarkTheme ? 'bg-slate-900/80 border-slate-800' : 'bg-white border-slate-200 shadow-sm'}`}>
          <button
            onClick={() => setBrandTheme('cyber_blue')}
            className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 ${brandTheme === 'cyber_blue' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
          >
            <span className="w-2 h-2 rounded-full bg-blue-300"></span>
            <span>ReLec Blue</span>
          </button>
          <button
            onClick={() => setBrandTheme('revolt_crimson')}
            className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 ${brandTheme === 'revolt_crimson' ? 'bg-red-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
          >
            <span className="w-2 h-2 rounded-full bg-red-300"></span>
            <span>RE-VOLT Red</span>
          </button>
        </div>

        <div className="flex items-center gap-2">
          {/* APK Direct Download Button */}
          <a
            href="./downloads/relec-pro-erp.apk"
            download="relec-pro-erp.apk"
            className="px-2.5 py-1.5 rounded-xl border text-xs font-bold flex items-center gap-1.5 transition-all bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/20 shadow-sm"
            title="Download Android APK"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Mobile App APK</span>
            <span className="sm:hidden">APK</span>
          </a>

          {/* Theme Toggle */}
          <button
            onClick={toggleDarkTheme}
            className={`p-2 rounded-xl border transition-all ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-amber-400 hover:bg-slate-800' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-100 shadow-sm'}`}
            title="Toggle Dark/Light Mode"
          >
            {isDarkTheme ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Main Authentication Card */}
      <div className={`w-full max-w-md sm:max-w-lg lg:max-w-xl rounded-3xl border backdrop-blur-xl transition-all shadow-2xl relative overflow-hidden z-10 ${isDarkTheme ? 'bg-slate-900/95 border-slate-800 shadow-black/60' : 'bg-white/95 border-slate-200 shadow-xl'}`}>
        {/* Glowing Top Accent Border */}
        <div
          className="absolute top-0 left-0 right-0 h-1.5 z-20"
          style={{ backgroundColor: accentColor }}
        />

        {/* Rectangular Brand Banner Artwork */}
        <div className="relative h-32 w-full overflow-hidden border-b border-white/10 group">
          <img
            src={bannerSrc}
            alt="Theme Banner"
            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/40 to-transparent" />
          
          {/* Logo Icon Centered Over Banner */}
          <div className="absolute -bottom-5 left-1/2 -translate-x-1/2 z-10">
            <div
              className="w-16 h-16 rounded-2xl p-1 border shadow-xl relative overflow-hidden flex items-center justify-center transition-all"
              style={{
                borderColor: `${accentColor}80`,
                backgroundColor: isDarkTheme ? '#0F172A' : '#FFFFFF',
                boxShadow: `0 4px 20px ${accentColor}40`
              }}
            >
              <img src={logoSrc} alt="Logo" className="w-full h-full object-cover rounded-xl" />
            </div>
          </div>
        </div>

        {/* Card Body */}
        <div className="p-6 sm:p-8 pt-7">
          {/* Brand Header Text */}
          <div className="flex flex-col items-center text-center mb-6">
            <h2 className="text-2xl font-black tracking-tight">
              {isRevolt ? 'RE-VOLT Enterprise' : 'ReLec PRO ERP'}
            </h2>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mt-0.5">
              Rattan Electricals Business Portal
            </p>
          </div>

          {/* Multi-Role Segmented Tabs */}
          {authMode === 'SIGN_IN' && (
          <div className={`grid grid-cols-3 p-1 rounded-2xl mb-6 text-xs font-bold border ${isDarkTheme ? 'bg-slate-950 border-slate-800' : 'bg-slate-100 border-slate-200'}`}>
            <button
              onClick={() => handleRoleChange('Admin')}
              className={`py-2 rounded-xl flex items-center justify-center gap-1.5 transition-all ${selectedRole === 'Admin' ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'}`}
              style={{ color: selectedRole === 'Admin' ? accentColor : undefined }}
            >
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Admin</span>
            </button>
            <button
              onClick={() => handleRoleChange('Worker')}
              className={`py-2 rounded-xl flex items-center justify-center gap-1.5 transition-all ${selectedRole === 'Worker' ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'}`}
              style={{ color: selectedRole === 'Worker' ? accentColor : undefined }}
            >
              <Wrench className="w-3.5 h-3.5" />
              <span>Worker</span>
            </button>
            <button
              onClick={() => handleRoleChange('Customer')}
              className={`py-2 rounded-xl flex items-center justify-center gap-1.5 transition-all ${selectedRole === 'Customer' ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm font-extrabold' : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'}`}
              style={{ color: selectedRole === 'Customer' ? accentColor : undefined }}
            >
              <User className="w-3.5 h-3.5" />
              <span>Customer</span>
            </button>
          </div>
        )}

        {/* Notification / Feedback Alerts */}
        {errorMsg && (
          <div className="mb-4 p-3 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-500 dark:text-red-400 text-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span className="leading-snug">{errorMsg}</span>
          </div>
        )}

        {successMsg && (
          <div className="mb-4 p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 text-xs flex items-start gap-2">
            <CheckCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span className="leading-snug">{successMsg}</span>
          </div>
        )}

        {/* ======================================================== */}
        {/* VIEW 1: SIGN IN MODE (Admin, Worker, Customer)           */}
        {/* ======================================================== */}
        {authMode === 'SIGN_IN' && selectedRole !== 'Customer' && (
          <form onSubmit={handleSignIn} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                {selectedRole} Login ID / Email
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
                <input
                  type="text"
                  required
                  value={loginIdentifier}
                  onChange={(e) => setLoginIdentifier(e.target.value)}
                  placeholder={selectedRole === 'Admin' ? 'gursimarpsr@gmail.com' : 'worker@relect.com'}
                  className={`w-full pl-10 pr-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 focus:border-blue-500 text-white' : 'bg-slate-50 border-slate-200 focus:border-blue-500 text-slate-900'}`}
                />
              </div>
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                  Password
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setAuthMode('FORGOT_PASSWORD');
                    setResetTarget(loginIdentifier);
                  }}
                  className="text-xs font-semibold hover:underline"
                  style={{ color: accentColor }}
                >
                  Forgot Password?
                </button>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={loginPassword}
                  onChange={(e) => setLoginPassword(e.target.value)}
                  placeholder="Enter your password"
                  className={`w-full pl-10 pr-10 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 focus:border-blue-500 text-white' : 'bg-slate-50 border-slate-200 focus:border-blue-500 text-slate-900'}`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-200"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Quick Demo Credentials Pill */}
            <div className={`p-2.5 rounded-xl border text-[11px] flex items-center justify-between ${isDarkTheme ? 'bg-slate-950/60 border-slate-800/80 text-slate-400' : 'bg-slate-100 border-slate-200 text-slate-600'}`}>
              <div>
                <span className="font-bold text-slate-300">Preset: </span>
                <span>{selectedRole === 'Admin' ? 'gursimarpsr@gmail.com' : 'worker@relect.com'}</span>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (selectedRole === 'Admin') {
                    setLoginIdentifier('gursimarpsr@gmail.com');
                    setLoginPassword('Admin@123');
                  } else {
                    setLoginIdentifier('worker@relect.com');
                    setLoginPassword('Worker@123');
                  }
                }}
                className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-white hover:bg-slate-700"
              >
                Auto-Fill
              </button>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 rounded-2xl font-black text-sm text-white tracking-wide shadow-lg transition-all transform hover:-translate-y-0.5 active:translate-y-0 flex items-center justify-center gap-2"
              style={{ backgroundColor: accentColor, boxShadow: `0 8px 20px ${accentColor}35` }}
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>SIGN IN TO {selectedRole.toUpperCase()}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            {/* 1-Click Master Admin Instant Access for Owner */}
            {selectedRole === 'Admin' && (
              <div className="pt-2 space-y-2">
                <button
                  type="button"
                  onClick={handleInstantAdminLogin}
                  disabled={isLoading}
                  className="w-full py-2.5 px-3 rounded-2xl border font-bold text-xs flex items-center justify-center gap-2 transition-all hover:brightness-110 active:scale-[0.98] shadow-sm"
                  style={{
                    borderColor: `${accentColor}50`,
                    backgroundColor: `${accentColor}15`,
                    color: accentColor
                  }}
                >
                  <Zap className="w-4 h-4" />
                  <span>Master Admin Instant Access</span>
                </button>

                <div className="flex justify-center">
                  <button
                    type="button"
                    onClick={() => {
                      setNewAdminPasswordInput('');
                      setShowAdminResetModal(true);
                    }}
                    className="text-[11px] font-semibold text-slate-500 hover:text-slate-300 flex items-center gap-1 transition-colors"
                  >
                    <KeyRound className="w-3.5 h-3.5" />
                    <span>Set or Reset Admin Password</span>
                  </button>
                </div>
              </div>
            )}
          </form>
        )}

        {/* ======================================================== */}
        {/* VIEW 2: CUSTOMER ZERO-BARRIER PHONE OTP DESK              */}
        {/* ======================================================== */}
        {authMode === 'SIGN_IN' && selectedRole === 'Customer' && (
          <div className="space-y-4">
            <div className="p-3 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-xs text-blue-400 leading-relaxed">
              <strong>Zero-barrier Customer Desk:</strong> Enter your mobile number to instantly track repair status or book an electrician without complex registration.
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Your Full Name
              </label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
                <input
                  type="text"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  placeholder="e.g. Anita Sharma"
                  className={`w-full pl-10 pr-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 focus:border-blue-500 text-white' : 'bg-slate-50 border-slate-200 focus:border-blue-500 text-slate-900'}`}
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Mobile Number (+91)
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Phone className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
                  <input
                    type="tel"
                    maxLength={10}
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                    placeholder="9417255831"
                    className={`w-full pl-10 pr-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 focus:border-blue-500 text-white' : 'bg-slate-50 border-slate-200 focus:border-blue-500 text-slate-900'}`}
                  />
                </div>
                <button
                  type="button"
                  onClick={handleSendCustomerOtp}
                  disabled={isLoading}
                  className="px-4 py-2.5 rounded-2xl text-xs font-black text-white shrink-0 shadow-md transition-all"
                  style={{ backgroundColor: accentColor }}
                >
                  {isOtpSent ? 'Resend' : 'Get OTP'}
                </button>
              </div>
            </div>

            {isOtpSent && (
              <div>
                <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                  Enter 6-Digit OTP Code
                </label>
                <div className="relative">
                  <KeyRound className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
                  <input
                    type="text"
                    maxLength={6}
                    value={customerOtp}
                    onChange={(e) => setCustomerOtp(e.target.value)}
                    placeholder="e.g. 524189"
                    className={`w-full pl-10 pr-4 py-2.5 rounded-2xl text-sm font-mono tracking-widest border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 focus:border-blue-500 text-white' : 'bg-slate-50 border-slate-200 focus:border-blue-500 text-slate-900'}`}
                  />
                </div>
              </div>
            )}

            <button
              type="button"
              onClick={isOtpSent ? handleVerifyCustomerOtp : handleSendCustomerOtp}
              disabled={isLoading}
              className="w-full py-3 rounded-2xl font-black text-sm text-white tracking-wide shadow-lg transition-all transform hover:-translate-y-0.5 active:translate-y-0 flex items-center justify-center gap-2"
              style={{ backgroundColor: accentColor, boxShadow: `0 8px 20px ${accentColor}35` }}
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : isOtpSent ? (
                <>
                  <span>VERIFY & OPEN CUSTOMER DESK</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              ) : (
                <>
                  <span>SEND OTP TO LOGIN</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        )}

        {/* ======================================================== */}
        {/* VIEW 3: SIGN UP REGISTRATION                             */}
        {/* ======================================================== */}
        {authMode === 'SIGN_UP' && (
          <form onSubmit={handleSignUp} className="space-y-3.5">
            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Full Legal Name
              </label>
              <input
                type="text"
                required
                value={signupName}
                onChange={(e) => setSignupName(e.target.value)}
                placeholder="e.g. Baljinder Singh"
                className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Email Address
              </label>
              <input
                type="email"
                value={signupEmail}
                onChange={(e) => setSignupEmail(e.target.value)}
                placeholder="baljinder@relect.com"
                className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Mobile Number (+91)
              </label>
              <input
                type="tel"
                maxLength={10}
                value={signupPhone}
                onChange={(e) => setSignupPhone(e.target.value)}
                placeholder="9501562211"
                className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Registering Role
              </label>
              <select
                value={signupRole}
                onChange={(e) => setSignupRole(e.target.value as any)}
                className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              >
                <option value="Customer">Customer (Instant Access)</option>
                <option value="Worker">Technician / Worker (Requires Admin Approval)</option>
                <option value="Admin">Administrator (Requires Superior Admin Approval)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Password (min. 6 characters)
              </label>
              <input
                type="password"
                required
                value={signupPassword}
                onChange={(e) => setSignupPassword(e.target.value)}
                placeholder="Create a strong password"
                className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none transition-all ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 rounded-2xl font-black text-sm text-white tracking-wide shadow-lg transition-all flex items-center justify-center gap-2"
              style={{ backgroundColor: accentColor }}
            >
              {isLoading ? 'Registering...' : 'CREATE ACCOUNT'}
            </button>
          </form>
        )}

        {/* ======================================================== */}
        {/* VIEW 4: FORGOT PASSWORD                                  */}
        {/* ======================================================== */}
        {authMode === 'FORGOT_PASSWORD' && (
          <form onSubmit={handleResetPassword} className="space-y-4">
            <p className="text-xs text-slate-400 leading-relaxed">
              Enter your registered email address or phone number to receive a 6-digit password reset OTP.
            </p>

            <div>
              <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                Account Email or Phone
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  required
                  value={resetTarget}
                  onChange={(e) => setResetTarget(e.target.value)}
                  placeholder="gursimarpsr@gmail.com"
                  className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                />
                <button
                  type="button"
                  onClick={handleSendResetOtp}
                  className="px-4 py-2.5 rounded-2xl text-xs font-bold text-white shrink-0"
                  style={{ backgroundColor: accentColor }}
                >
                  Send OTP
                </button>
              </div>
            </div>

            {resetOtpSent && (
              <>
                <div>
                  <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                    6-Digit OTP Code
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={resetOtp}
                    onChange={(e) => setResetOtp(e.target.value)}
                    placeholder="Enter code"
                    className={`w-full px-4 py-2.5 rounded-2xl text-sm font-mono tracking-widest border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1">
                    New Password
                  </label>
                  <input
                    type="password"
                    required
                    value={resetNewPass}
                    onChange={(e) => setResetNewPass(e.target.value)}
                    placeholder="Enter new password"
                    className={`w-full px-4 py-2.5 rounded-2xl text-sm border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-3 rounded-2xl font-black text-sm text-white tracking-wide shadow-lg"
                  style={{ backgroundColor: accentColor }}
                >
                  RESET PASSWORD
                </button>
              </>
            )}
          </form>
        )}

        {/* Public GitHub Visitor Sandbox Card */}
        <div className={`mt-5 p-3.5 rounded-2xl border text-center transition-all ${isDarkTheme ? 'bg-slate-950/70 border-slate-800' : 'bg-slate-50 border-slate-200'}`}>
          <div className="flex items-center justify-center gap-1.5 mb-1 text-[11px] font-black uppercase tracking-wider text-amber-500">
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
            <span>GitHub Visitor Demo Sandbox</span>
          </div>
          <p className="text-[11px] text-slate-500 mb-3 leading-tight">
            Visiting from GitHub? Test the entire ERP in an isolated sandbox. The store owner's live production database is secure and protected.
          </p>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => handleEnterSandbox(false)}
              disabled={isLoading}
              className="py-2 px-2.5 rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 text-xs font-bold transition-all flex items-center justify-center gap-1.5"
            >
              <Layers className="w-3.5 h-3.5" />
              <span>Clean Sandbox</span>
            </button>
            <button
              type="button"
              onClick={() => handleEnterSandbox(true)}
              disabled={isLoading}
              className="py-2 px-2.5 rounded-xl border border-blue-500/30 bg-blue-500/10 text-blue-400 hover:bg-blue-500/20 text-xs font-bold transition-all flex items-center justify-center gap-1.5"
            >
              <Database className="w-3.5 h-3.5" />
              <span>Sample Dataset</span>
            </button>
          </div>
        </div>

        {/* Footer Toggle between Modes */}
        <div className="mt-6 pt-4 border-t border-slate-200 dark:border-slate-800/80 text-center text-xs text-slate-500">
          {authMode === 'SIGN_IN' ? (
            <p>
              Don't have an account?{' '}
              <button
                onClick={() => {
                  setAuthMode('SIGN_UP');
                  clearMessages();
                }}
                className="font-bold hover:underline"
                style={{ color: accentColor }}
              >
                Register Here
              </button>
            </p>
          ) : (
            <p>
              Already have an account?{' '}
              <button
                onClick={() => {
                  setAuthMode('SIGN_IN');
                  clearMessages();
                }}
                className="font-bold hover:underline"
                style={{ color: accentColor }}
              >
                Sign In
              </button>
            </p>
          )}
        </div>
      </div>
      {/* End Card Body */}
    </div>

    {/* Admin Password Reset Modal */}
    {showAdminResetModal && (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
        <div className={`w-full max-w-sm rounded-3xl p-6 border shadow-2xl ${isDarkTheme ? 'bg-slate-900 border-slate-800 text-white' : 'bg-white border-slate-200 text-slate-900'}`}>
          <div className="flex justify-between items-center mb-4">
            <div className="flex items-center gap-2">
              <KeyRound className="w-5 h-5" style={{ color: accentColor }} />
              <h3 className="font-extrabold text-base">Set Admin Password</h3>
            </div>
            <button
              onClick={() => setShowAdminResetModal(false)}
              className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <p className="text-xs text-slate-400 mb-4 leading-relaxed">
            Choose any custom password you prefer for the Master Admin account (<span className="text-white font-mono">gursimarpsr@gmail.com</span>). It will be saved securely to the database.
          </p>

          <form onSubmit={handleSaveAdminPassword} className="space-y-4">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-1">
                New Password
              </label>
              <input
                type="text"
                required
                value={newAdminPasswordInput}
                onChange={(e) => setNewAdminPasswordInput(e.target.value)}
                placeholder="e.g. MyPassword@2026"
                className={`w-full px-3.5 py-2.5 rounded-xl text-sm border focus:outline-none ${isDarkTheme ? 'bg-slate-950 border-slate-800 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
              />
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAdminResetModal(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-700 text-xs font-bold hover:bg-slate-800 text-slate-300"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isLoading}
                className="flex-1 py-2.5 rounded-xl text-xs font-bold text-white shadow-md transition-all hover:brightness-110"
                style={{ backgroundColor: accentColor }}
              >
                {isLoading ? 'Saving...' : 'Save & Log In'}
              </button>
            </div>
          </form>
        </div>
      </div>
    )}
  </div>
);
};
