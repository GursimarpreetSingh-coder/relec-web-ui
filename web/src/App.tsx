import React, { useState, useEffect } from 'react';
import { ThemeProvider, useTheme } from './ui/theme';
import { SplashScreen } from './ui/SplashScreen';
import { LoginScreen } from './ui/LoginScreen';
import { AdminDashboardView } from './ui/AdminDashboardView';
import { WorkerDashboardView } from './ui/WorkerDashboardView';
import { CustomerPublicPortal } from './ui/CustomerPublicPortal';
import { initializeDatabase } from './data/database';
import { authService } from './data/authService';
import type { UserSession } from './types/models';

const AppContent: React.FC = () => {
  const [showSplash, setShowSplash] = useState<boolean>(true);
  const [session, setSession] = useState<UserSession | null>(null);
  const [isCustomerDirectView, setIsCustomerDirectView] = useState<boolean>(false);

  useEffect(() => {
    // 1. Initialize Dexie DB
    initializeDatabase().catch(console.error);

    // 2. Check URL Deep Link / QR Counter standee access
    const searchParams = new URLSearchParams(window.location.search);
    const viewParam = searchParams.get('view');
    if (viewParam && viewParam.toLowerCase() === 'customer') {
      setIsCustomerDirectView(true);
      setShowSplash(false);
      return;
    }

    // 3. Restore existing session
    const current = authService.getCurrentSession();
    if (current) {
      setSession(current);
    }
  }, []);

  const handleLoginSuccess = (role: 'Admin' | 'Worker' | 'Customer', newSession: UserSession) => {
    setSession(newSession);
  };

  const handleLogout = () => {
    authService.clearSession();
    setSession(null);
    setIsCustomerDirectView(false);
    // remove query param if any
    if (window.location.search) {
      window.history.replaceState({}, '', window.location.pathname);
    }
  };

  // Direct Customer View via Counter Standee QR Scan
  if (isCustomerDirectView) {
    return (
      <CustomerPublicPortal
        session={session}
        onNavigateBack={() => {
          setIsCustomerDirectView(false);
          window.history.replaceState({}, '', window.location.pathname);
        }}
      />
    );
  }

  // Animated Startup Splash
  if (showSplash) {
    return <SplashScreen onEnterPortal={() => setShowSplash(false)} />;
  }

  // Not logged in -> Show Multi-Role Login Portal
  if (!session) {
    return (
      <LoginScreen
        onLoginSuccess={handleLoginSuccess}
      />
    );
  }

  // Role Routing based on verified session authority
  if (session.role === 'Admin') {
    return <AdminDashboardView onLogout={handleLogout} />;
  }

  if (session.role === 'Worker') {
    return <WorkerDashboardView onLogout={handleLogout} />;
  }

  return (
    <CustomerPublicPortal
      session={session}
      onNavigateBack={handleLogout}
    />
  );
};

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
};

export default App;
