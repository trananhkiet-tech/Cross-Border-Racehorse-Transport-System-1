/** Icon dùng bộ Bootstrap Icons (font cục bộ, không cần CDN). */
export function Icon({ name, className = '', size, title }: { name: string; className?: string; size?: number; title?: string }) {
  return (
    <i
      className={`bi ${name.startsWith('bi-') ? name : `bi-${name}`} ir-icon ${className}`}
      style={size ? { fontSize: size } : undefined}
      aria-hidden={title ? undefined : true}
      title={title}
    />
  );
}
