import React, { createContext, useContext, useState, useEffect } from 'react';

export type BrandTheme = 'cyber_blue' | 'revolt_crimson';

interface ThemeContextType {
  brandTheme: BrandTheme;
  setBrandTheme: (theme: BrandTheme) => void;
  isDarkTheme: boolean;
  toggleDarkTheme: () => void;
  accentColor: string;
  setAccentColor: (color: string) => void;
  brandName: string;
  brandSubtitle: string;
  logoSrc: string;
  bannerSrc: string;
  isRevolt: boolean;
}

const ThemeContext = createContext<ThemeContextType | null>(null);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [brandTheme, setBrandThemeState] = useState<BrandTheme>(() => {
    return (localStorage.getItem('active_brand_theme') as BrandTheme) || 'cyber_blue';
  });

  const [isDarkTheme, setIsDarkTheme] = useState<boolean>(() => {
    return localStorage.getItem('is_dark_theme') === 'true';
  });

  const defaultAccent = brandTheme === 'revolt_crimson' ? '#ef4444' : '#0284c7';
  const [accentColor, setAccentColorState] = useState<string>(() => {
    return localStorage.getItem('custom_accent_color') || defaultAccent;
  });

  const setBrandTheme = (theme: BrandTheme) => {
    setBrandThemeState(theme);
    localStorage.setItem('active_brand_theme', theme);
    // update default accent if not custom
    const newDefault = theme === 'revolt_crimson' ? '#ef4444' : '#0284c7';
    setAccentColorState(newDefault);
    localStorage.removeItem('custom_accent_color');
  };

  const setAccentColor = (color: string) => {
    setAccentColorState(color);
    localStorage.setItem('custom_accent_color', color);
  };

  const toggleDarkTheme = () => {
    setIsDarkTheme(prev => {
      const next = !prev;
      localStorage.setItem('is_dark_theme', String(next));
      if (next) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
      return next;
    });
  };

  useEffect(() => {
    if (isDarkTheme) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkTheme]);

  const isRevolt = brandTheme === 'revolt_crimson';
  const brandName = isRevolt ? 'RE-VOLT' : 'ReLec';
  const brandSubtitle = isRevolt ? 'REVOLT ENTERPRISE' : 'RATTAN ELECTRICALS';

  const logoSrc = isRevolt
    ? './assets/revolt_red_icon.png'
    : './assets/relec_blue_icon.png';

  const bannerSrc = isRevolt
    ? './assets/revolt_red_banner.png'
    : './assets/relec_blue_banner.png';

  return (
    <ThemeContext.Provider
      value={{
        brandTheme,
        setBrandTheme,
        isDarkTheme,
        toggleDarkTheme,
        accentColor,
        setAccentColor,
        brandName,
        brandSubtitle,
        logoSrc,
        bannerSrc,
        isRevolt
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
};
