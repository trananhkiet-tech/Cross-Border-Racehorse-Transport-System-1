/**
 * Logo IRTMS: đầu ngựa (trắng ngà) với bờm vàng đồng trên nền Midnight Navy.
 * Màu nền lấy từ biến CSS --logo-bg để dùng được trên cả nền sáng lẫn thanh bên tối.
 */
export function HorseLogo({ size = 38, title = 'IRTMS – Vận chuyển ngựa đua' }: { size?: number; title?: string }) {
  return (
    <svg viewBox="0 0 100 100" width={size} height={size} role="img" aria-label={title} focusable="false">
      <rect width="100" height="100" rx="22" fill="var(--logo-bg, #102D4F)" />
      <rect x="1.5" y="1.5" width="97" height="97" rx="20.5" fill="none" stroke="#C9A45C" strokeOpacity="0.55" strokeWidth="2" />
      <g transform="translate(6 4) scale(.88)">
        <path fill="#F5F3EE" d="M22 94C19 76 21 56 31 38c2-5 4-11 5-18l1-14 9 11c8 0 17 4 23 12 7 9 13 20 19 31 3 6 3 12-2 16-5 4-12 3-16-1-4-4-8-6-14-4-4 2-7 7-7 17z" />
        <path fill="#C9A45C" d="M37 17C30 27 22 44 19 62c-2 11-1 22 1 32h10c-3-17-3-35 4-50 3-8 6-15 8-24z" />
        <path fill="#E9D7AE" d="M39 18l-1-9 6 8z" />
        <path fill="#C9A45C" d="M41 21c4 3 6 8 6 13-4-2-7-5-8-9z" />
        <circle cx="60" cy="38" r="2.6" fill="#102D4F" />
        <ellipse cx="86" cy="73" rx="1.8" ry="2.6" transform="rotate(-30 86 73)" fill="#102D4F" />
        <path fill="none" stroke="#C9A45C" strokeWidth="1.6" strokeLinecap="round" d="M58 56c5 3 9 8 11 14" />
      </g>
    </svg>
  );
}
