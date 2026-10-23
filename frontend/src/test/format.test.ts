import { describe, expect, it } from 'vitest';
import { formatBytes, formatDate, formatDateTime, formatMoney, minutesToText } from '../utils/format';
import { labelOf, toneOf } from '../utils/status';
import { validateFile, MAX_FILE_BYTES } from '../components/Files';

describe('định dạng hiển thị', () => {
  it('tiền tệ: luôn hiển thị mã tiền, thiếu giá trị là "—"', () => {
    expect(formatMoney(1234.5, 'USD')).toBe('USD 1,234.50');
    expect(formatMoney(undefined)).toBe('—');
    expect(formatMoney(null)).toBe('—');
  });
  it('ngày giờ: hiển thị nguyên giá trị, không đổi múi giờ', () => {
    expect(formatDate('2026-03-05')).toBe('05/03/2026');
    expect(formatDateTime('2026-03-05T08:30:00')).toBe('05/03/2026 08:30');
    expect(formatDate(undefined)).toBe('—');
  });
  it('phút và dung lượng', () => {
    expect(minutesToText(0)).toBe('0 phút');
    expect(minutesToText(95)).toBe('1 giờ 35 phút');
    expect(minutesToText(120)).toBe('2 giờ');
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2 KB');
  });
  it('tone và nhãn trạng thái', () => {
    expect(toneOf('DELIVERED')).toBe('success');
    expect(toneOf('CANCELLED')).not.toBe('success');
    expect(toneOf(undefined)).toBe('neutral');
    expect(labelOf('IN_TRANSIT')).toBe('Đang vận chuyển');
    expect(labelOf('KHONG_TON_TAI')).toBe('KHONG_TON_TAI');
  });
});

describe('validateFile (client)', () => {
  const f = (type: string, size: number) => new File([new Uint8Array(size)], 'a.bin', { type });
  it('chấp nhận PDF/ảnh hợp lệ', () => {
    expect(validateFile(f('application/pdf', 100))).toBeUndefined();
    expect(validateFile(f('image/png', 100))).toBeUndefined();
  });
  it('từ chối sai loại, quá lớn, rỗng', () => {
    expect(validateFile(f('application/x-msdownload', 100))).toMatch(/PDF/);
    expect(validateFile(f('application/pdf', MAX_FILE_BYTES + 1))).toMatch(/dung lượng/);
    expect(validateFile(f('application/pdf', 0))).toMatch(/rỗng/);
  });
});
