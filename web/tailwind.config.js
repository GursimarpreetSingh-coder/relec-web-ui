/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        'electric-blue': '#2563EB',
        'cyber-blue': '#0284C7',
        'revolt-crimson': '#DC2626',
        'neon-red': '#EF4444',
        'neon-green': '#10B981',
        'neon-yellow': '#F59E0B',
        'neon-purple': '#8B5CF6',
        'dark-bg': '#0B0F19',
        'dark-surface': '#111827',
        'dark-card': '#1E293B',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
