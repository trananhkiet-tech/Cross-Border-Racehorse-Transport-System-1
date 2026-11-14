/** Chọn điểm 1–5 sao. */
export function StarInput({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <div className="ir-stack" role="group" aria-label={label}>
      <span className="ir-field-label">{label}</span>
      <div style={{ display: 'flex', gap: 4 }}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button key={n} type="button" aria-label={`${label}: ${n} sao`} aria-pressed={n <= value} onClick={() => onChange(n)}
            style={{ background: 'none', border: 0, cursor: 'pointer', fontSize: 24, lineHeight: 1, color: n <= value ? '#f59e0b' : '#cbd5e1' }}>
            ★
          </button>
        ))}
      </div>
    </div>
  );
}
