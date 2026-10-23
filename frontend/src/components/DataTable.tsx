import { useMemo, useState, type ReactNode } from 'react';
import { Icon } from './Icon';
import { EmptyState, ErrorState, LoadingState } from './States';

export interface Column<T> {
  key: string;
  header: ReactNode;
  render: (row: T) => ReactNode;
  /** Giá trị dùng để sắp xếp phía client (bảng không phân trang server). */
  sortValue?: (row: T) => string | number | undefined | null;
  align?: 'left' | 'right' | 'center';
  width?: number | string;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[] | undefined;
  rowKey: (row: T) => string | number;
  loading?: boolean;
  error?: string;
  onRetry?: () => void;
  onRowClick?: (row: T) => void;
  rowClassName?: (row: T) => string | undefined;
  toolbar?: ReactNode;
  footer?: ReactNode;
  emptyTitle?: string;
  emptyDescription?: ReactNode;
  emptyIcon?: string;
  dense?: boolean;
  flush?: boolean;
  caption?: string;
}

/** Bảng dữ liệu dùng chung: toolbar, sắp xếp cột phía client, trạng thái loading / lỗi / rỗng. */
export function DataTable<T>({
  columns, rows, rowKey, loading, error, onRetry, onRowClick, rowClassName, toolbar, footer,
  emptyTitle = 'Không có dữ liệu', emptyDescription, emptyIcon, dense, flush, caption,
}: DataTableProps<T>) {
  const [sort, setSort] = useState<{ key: string; dir: 'asc' | 'desc' } | null>(null);

  const sorted = useMemo(() => {
    if (!rows || !sort) return rows;
    const col = columns.find((c) => c.key === sort.key);
    if (!col?.sortValue) return rows;
    const get = col.sortValue;
    return [...rows].sort((a, b) => {
      const va = get(a);
      const vb = get(b);
      if (va === vb) return 0;
      if (va === undefined || va === null) return 1;
      if (vb === undefined || vb === null) return -1;
      const r = typeof va === 'number' && typeof vb === 'number' ? va - vb : String(va).localeCompare(String(vb), 'vi');
      return sort.dir === 'asc' ? r : -r;
    });
  }, [rows, sort, columns]);

  const toggleSort = (key: string) => {
    setSort((s) => (s?.key === key ? (s.dir === 'asc' ? { key, dir: 'desc' } : null) : { key, dir: 'asc' }));
  };

  let body: ReactNode;
  if (loading && !rows) {
    body = (
      <tr>
        <td colSpan={columns.length} className="ir-td-empty">
          <LoadingState compact />
        </td>
      </tr>
    );
  } else if (error) {
    body = (
      <tr>
        <td colSpan={columns.length} className="ir-td-empty">
          <ErrorState message={error} onRetry={onRetry} />
        </td>
      </tr>
    );
  } else if (!sorted || sorted.length === 0) {
    body = (
      <tr>
        <td colSpan={columns.length} className="ir-td-empty">
          <EmptyState compact title={emptyTitle} description={emptyDescription} icon={emptyIcon} />
        </td>
      </tr>
    );
  } else {
    body = sorted.map((row) => (
      <tr
        key={rowKey(row)}
        className={`${onRowClick ? 'app-table-row-link' : ''} ${rowClassName?.(row) ?? ''}`}
        onClick={onRowClick ? () => onRowClick(row) : undefined}
        onKeyDown={onRowClick ? (e) => { if (e.key === 'Enter') onRowClick(row); } : undefined}
        tabIndex={onRowClick ? 0 : undefined}
      >
        {columns.map((c) => (
          <td key={c.key} className={c.className} style={{ textAlign: c.align }}>
            {c.render(row)}
          </td>
        ))}
      </tr>
    ));
  }

  return (
    <div className={`ir-table-wrap${flush ? ' ir-table-flush' : ''}`}>
      {toolbar && <div className="ir-table-toolbar">{toolbar}</div>}
      <div className="ir-table-scroll">
        <table className={`ir-table${dense ? ' ir-table-dense' : ''}`}>
          {caption && <caption className="ir-sr">{caption}</caption>}
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key} style={{ textAlign: c.align, width: c.width }} aria-sort={sort?.key === c.key ? (sort.dir === 'asc' ? 'ascending' : 'descending') : undefined}>
                  {c.sortValue ? (
                    <button type="button" className={`ir-th-sort${sort?.key === c.key ? ' is-sorted' : ''}`} onClick={() => toggleSort(c.key)}>
                      {c.header}
                      <Icon name={sort?.key === c.key ? (sort.dir === 'asc' ? 'bi-sort-up' : 'bi-sort-down') : 'bi-arrow-down-up'} size={12} />
                    </button>
                  ) : (
                    c.header
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>{body}</tbody>
        </table>
      </div>
      {footer && <div className="ir-table-foot">{footer}</div>}
    </div>
  );
}

/** Phân trang server-side (page bắt đầu từ 0 như Spring Data). */
export function Pagination({ page, size, totalElements, totalPages, onPage, onSize }: { page: number; size: number; totalElements: number; totalPages: number; onPage: (p: number) => void; onSize?: (s: number) => void }) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min(totalElements, (page + 1) * size);
  const pages: (number | 'gap')[] = [];
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) pages.push(i);
    else if (pages[pages.length - 1] !== 'gap') pages.push('gap');
  }
  return (
    <nav className="ir-pagination" aria-label="Phân trang">
      <span>
        Hiển thị {from}–{to} / {totalElements}
      </span>
      <div className="ir-pag-right">
        {onSize && (
          <label className="ir-pag-size">
            Số dòng
            <select value={size} onChange={(e) => onSize(Number(e.target.value))}>
              {[10, 20, 50].map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </label>
        )}
        <div className="ir-pag-pages">
          <button type="button" className="ir-pag-btn" disabled={page <= 0} onClick={() => onPage(page - 1)} aria-label="Trang trước">
            <Icon name="bi-chevron-left" />
          </button>
          {pages.map((p, i) =>
            p === 'gap' ? (
              <span key={`g${i}`} className="ir-pag-gap">…</span>
            ) : (
              <button key={p} type="button" className={`ir-pag-btn${p === page ? ' is-active' : ''}`} aria-current={p === page ? 'page' : undefined} onClick={() => onPage(p)}>
                {p + 1}
              </button>
            ),
          )}
          <button type="button" className="ir-pag-btn" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)} aria-label="Trang sau">
            <Icon name="bi-chevron-right" />
          </button>
        </div>
      </div>
    </nav>
  );
}

/** Ô tìm kiếm trong toolbar bảng. */
export function SearchBox({ value, onChange, placeholder = 'Tìm kiếm…', width = 280 }: { value: string; onChange: (v: string) => void; placeholder?: string; width?: number }) {
  return (
    <div className="ir-input ir-input-sm ir-search" style={{ width }}>
      <Icon name="bi-search" className="ir-input-affix" />
      <input type="search" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} aria-label={placeholder} />
    </div>
  );
}

/** Ô "chính + phụ" trong bảng. */
export function Cell({ main, sub, mono }: { main: ReactNode; sub?: ReactNode; mono?: boolean }) {
  return (
    <div className="ir-cell-two">
      <span className={`ir-cell-main${mono ? ' ir-mono' : ''}`}>{main}</span>
      {sub && <span className="ir-cell-sub">{sub}</span>}
    </div>
  );
}
