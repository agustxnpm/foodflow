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
      },
      animation: {
        'slide-in-right': 'slide-in-right 0.3s ease-out',
      },
    },
  },
  plugins: [],
}
