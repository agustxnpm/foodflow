export default function Button({ children, variant = 'primary', className = '', ...props }) {
  const baseClass = 'btn-base';
  const variantClass = variant === 'primary' ? 'btn-primary' : 'btn-secondary';
  
  return (
    <button className={`${baseClass} ${variantClass} ${className}`} {...props}>
      {children}
    </button>
  );
}
