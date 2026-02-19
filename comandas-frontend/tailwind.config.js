/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#FF0000',
        background: {
          main: '#000000',
          card: '#1a1a1a',
        },
        text: {
          primary: '#ffffff',
          secondary: '#9ca3af',
        },
      },
      fontFamily: {
        mono: ['Consolas', 'Monaco', 'monospace'],
      },
      keyframes: {
        'slide-in-right': {
          '0%': { transform: 'translateX(100%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        fadeIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        'mesa-press': {
          '0%': { transform: 'scale(1)' },
          '40%': { transform: 'scale(0.92)' },
          '70%': { transform: 'scale(1.03)' },
          '100%': { transform: 'scale(1)' },
        },
        'mesa-ring': {
          '0%': { boxShadow: '0 0 0 0 rgba(220,38,38,0.5)' },
          '70%': { boxShadow: '0 0 0 10px rgba(220,38,38,0)' },
          '100%': { boxShadow: '0 0 0 12px rgba(220,38,38,0)' },
        },
        'backdrop-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        'modal-in': {
          '0%': { opacity: '0', transform: 'scale(0.93) translateY(20px)' },
          '100%': { opacity: '1', transform: 'scale(1) translateY(0)' },
        },
      },
      animation: {
        'slide-in-right': 'slide-in-right 0.3s ease-out',
        fadeIn: 'fadeIn 200ms ease-out',
        'mesa-press': 'mesa-press 350ms ease-out',
        'mesa-ring': 'mesa-ring 500ms ease-out',
        'backdrop-in': 'backdrop-in 250ms ease-out',
        'modal-in': 'modal-in 300ms cubic-bezier(0.16,1,0.3,1)',
      },
    },
  },
  plugins: [],
}
