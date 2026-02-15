export default function Input({ label, error, ...props }) {
  return (
    <div className="flex flex-col gap-1">
      {label && <label className="text-sm text-text-secondary">{label}</label>}
      <input
        className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded text-text-primary focus:border-primary focus:outline-none"
        {...props}
      />
      {error && <span className="text-sm text-red-500">{error}</span>}
    </div>
  );
}
