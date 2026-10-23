import type { Role } from '../types/enums';

export interface MenuItem {
  to: string;
  label: string;
  icon: string;
  /** Khóa đếm số lượng hiển thị trên menu (lấy từ dashboard cards nếu có). */
  countKey?: string;
}
export interface MenuGroup {
  label: string;
  items: MenuItem[];
}

/** Menu sidebar theo role – bám theo Figma IRTMS. Không hiển thị menu vượt quyền. */
export const MENUS: Record<Role, MenuGroup[]> = {
  CUSTOMER: [
    { label: 'Điều hành', items: [{ to: '/dashboard', label: 'Tổng quan', icon: 'bi-grid-1x2' }] },
    {
      label: 'Vận chuyển',
      items: [
        { to: '/horses', label: 'Ngựa của tôi', icon: 'bi-postcard-heart' },
        { to: '/requests', label: 'Yêu cầu vận chuyển', icon: 'bi-clipboard-check', countKey: 'activeRequests' },
        { to: '/trips', label: 'Chuyến đi của tôi', icon: 'bi-truck', countKey: 'activeTrips' },
        { to: '/handovers', label: 'Bàn giao', icon: 'bi-box-arrow-in-down' },
      ],
    },
    {
      label: 'Tài chính',
      items: [
        { to: '/quotes', label: 'Báo giá', icon: 'bi-receipt', countKey: 'quotesAwaiting' },
        { to: '/invoices', label: 'Hoá đơn & thanh toán', icon: 'bi-credit-card' },
      ],
    },
    {
      label: 'Hỗ trợ',
      items: [
        { to: '/claims', label: 'Bồi thường', icon: 'bi-shield-check', countKey: 'openClaims' },
        { to: '/notifications', label: 'Thông báo', icon: 'bi-bell' },
      ],
    },
    { label: 'Tài khoản', items: [{ to: '/profile', label: 'Hồ sơ & cài đặt', icon: 'bi-person-circle' }] },
  ],
  LOGISTICS_MANAGER: [
    { label: 'Điều hành', items: [{ to: '/dashboard', label: 'Tổng quan', icon: 'bi-grid-1x2' }] },
    {
      label: 'Yêu cầu & tuân thủ',
      items: [
        { to: '/requests', label: 'Yêu cầu vận chuyển', icon: 'bi-clipboard-check', countKey: 'awaitingApproval' },
        { to: '/compliance/missing', label: 'Tuân thủ', icon: 'bi-shield-exclamation' },
      ],
    },
    {
      label: 'Đội xe & tuyến',
      items: [
        { to: '/fleet', label: 'Đội xe', icon: 'bi-truck-front' },
        { to: '/routes', label: 'Tuyến đường', icon: 'bi-signpost-2' },
        { to: '/staff', label: 'Nhân sự vận hành', icon: 'bi-people' },
      ],
    },
    {
      label: 'Vận hành',
      items: [
        { to: '/trips', label: 'Chuyến đi', icon: 'bi-truck' },
        { to: '/incidents', label: 'Sự cố', icon: 'bi-exclamation-octagon', countKey: 'openIncidents' },
        { to: '/handovers', label: 'Bàn giao', icon: 'bi-box-arrow-in-down' },
      ],
    },
    {
      label: 'Tài chính',
      items: [
        { to: '/quotes', label: 'Báo giá', icon: 'bi-receipt', countKey: 'toQuote' },
        { to: '/claims', label: 'Bồi thường', icon: 'bi-shield-check', countKey: 'claimsReview' },
        { to: '/invoices', label: 'Hoá đơn', icon: 'bi-file-earmark-text' },
        { to: '/payments', label: 'Thanh toán', icon: 'bi-credit-card' },
        { to: '/pricing', label: 'Bảng giá', icon: 'bi-tags' },
        { to: '/reports', label: 'Báo cáo', icon: 'bi-bar-chart-line' },
      ],
    },
    {
      label: 'Tài khoản',
      items: [
        { to: '/notifications', label: 'Thông báo', icon: 'bi-bell' },
        { to: '/profile', label: 'Hồ sơ & cài đặt', icon: 'bi-person-circle' },
      ],
    },
  ],
  TRANSPORT_SPECIALIST: [
    { label: 'Điều hành', items: [{ to: '/dashboard', label: 'Tổng quan', icon: 'bi-grid-1x2' }] },
    {
      label: 'Hồ sơ & tuân thủ',
      items: [
        { to: '/requests', label: 'Yêu cầu vận chuyển', icon: 'bi-clipboard-check', countKey: 'inReview' },
        { to: '/documents', label: 'Rà soát tài liệu', icon: 'bi-file-earmark-check', countKey: 'pendingDocuments' },
        { to: '/compliance/missing', label: 'Tài liệu còn thiếu', icon: 'bi-file-earmark-x', countKey: 'missingDocuments' },
        { to: '/compliance/requirements', label: 'Quy định nhập khẩu', icon: 'bi-journal-text' },
      ],
    },
    { label: 'Vận hành', items: [{ to: '/trips', label: 'Chuyến đi', icon: 'bi-truck' }, { to: '/incidents', label: 'Sự cố', icon: 'bi-exclamation-octagon' }] },
    {
      label: 'Tài khoản',
      items: [
        { to: '/notifications', label: 'Thông báo', icon: 'bi-bell' },
        { to: '/profile', label: 'Hồ sơ & cài đặt', icon: 'bi-person-circle' },
      ],
    },
  ],
  FLEET_ROUTE_COORDINATOR: [
    { label: 'Điều hành', items: [{ to: '/dashboard', label: 'Tổng quan', icon: 'bi-grid-1x2' }] },
    {
      label: 'Lập kế hoạch',
      items: [
        { to: '/planning', label: 'Lập chuyến', icon: 'bi-calendar2-plus', countKey: 'readyForPlanning' },
        { to: '/trips', label: 'Chuyến đi & phân công', icon: 'bi-truck' },
        { to: '/requests', label: 'Yêu cầu vận chuyển', icon: 'bi-clipboard-check' },
      ],
    },
    {
      label: 'Đội xe & tuyến',
      items: [
        { to: '/fleet', label: 'Đội xe', icon: 'bi-truck-front' },
        { to: '/routes', label: 'Tuyến đường', icon: 'bi-signpost-2' },
        { to: '/staff', label: 'Tài xế & áp tải', icon: 'bi-people' },
      ],
    },
    { label: 'Vận hành', items: [{ to: '/incidents', label: 'Sự cố', icon: 'bi-exclamation-octagon' }] },
    {
      label: 'Tài khoản',
      items: [
        { to: '/notifications', label: 'Thông báo', icon: 'bi-bell' },
        { to: '/profile', label: 'Hồ sơ & cài đặt', icon: 'bi-person-circle' },
      ],
    },
  ],
  DRIVER_ESCORT: [
    { label: 'Điều hành', items: [{ to: '/dashboard', label: 'Tổng quan', icon: 'bi-grid-1x2' }] },
    {
      label: 'Chuyến đi',
      items: [
        { to: '/trips', label: 'Chuyến của tôi', icon: 'bi-truck', countKey: 'assigned' },
        { to: '/incidents', label: 'Sự cố', icon: 'bi-exclamation-octagon', countKey: 'incidents' },
        { to: '/handovers', label: 'Bàn giao', icon: 'bi-box-arrow-in-down' },
      ],
    },
    {
      label: 'Tài khoản',
      items: [
        { to: '/notifications', label: 'Thông báo', icon: 'bi-bell' },
        { to: '/profile', label: 'Hồ sơ & cài đặt', icon: 'bi-person-circle' },
      ],
    },
  ],
};

/** Tiêu đề trang theo đường dẫn (dùng cho breadcrumb topbar). */
export const PAGE_TITLES: { pattern: RegExp; title: string; parent?: { label: string; to: string } }[] = [
  { pattern: /^\/dashboard/, title: 'Tổng quan' },
  { pattern: /^\/horses\/new/, title: 'Thêm ngựa', parent: { label: 'Ngựa', to: '/horses' } },
  { pattern: /^\/horses\/\d+/, title: 'Hồ sơ ngựa', parent: { label: 'Ngựa', to: '/horses' } },
  { pattern: /^\/horses/, title: 'Ngựa' },
  { pattern: /^\/requests\/new/, title: 'Tạo yêu cầu', parent: { label: 'Yêu cầu vận chuyển', to: '/requests' } },
  { pattern: /^\/requests\/\d+\/compliance/, title: 'Checklist tuân thủ', parent: { label: 'Yêu cầu vận chuyển', to: '/requests' } },
  { pattern: /^\/requests\/\d+\/edit/, title: 'Chỉnh sửa yêu cầu', parent: { label: 'Yêu cầu vận chuyển', to: '/requests' } },
  { pattern: /^\/requests\/\d+/, title: 'Chi tiết yêu cầu', parent: { label: 'Yêu cầu vận chuyển', to: '/requests' } },
  { pattern: /^\/requests/, title: 'Yêu cầu vận chuyển' },
  { pattern: /^\/documents\/\d+/, title: 'Chi tiết tài liệu', parent: { label: 'Tài liệu', to: '/documents' } },
  { pattern: /^\/documents/, title: 'Rà soát tài liệu' },
  { pattern: /^\/compliance\/missing/, title: 'Tài liệu còn thiếu' },
  { pattern: /^\/compliance\/requirements/, title: 'Quy định nhập khẩu' },
  { pattern: /^\/quotes\/\d+/, title: 'Chi tiết báo giá', parent: { label: 'Báo giá', to: '/quotes' } },
  { pattern: /^\/quotes/, title: 'Báo giá' },
  { pattern: /^\/invoices\/\d+/, title: 'Hoá đơn', parent: { label: 'Hoá đơn', to: '/invoices' } },
  { pattern: /^\/invoices/, title: 'Hoá đơn & thanh toán' },
  { pattern: /^\/payments/, title: 'Thanh toán' },
  { pattern: /^\/pricing/, title: 'Bảng giá' },
  { pattern: /^\/fleet\/\d+/, title: 'Chi tiết phương tiện', parent: { label: 'Đội xe', to: '/fleet' } },
  { pattern: /^\/fleet/, title: 'Đội xe' },
  { pattern: /^\/routes\/new/, title: 'Tạo tuyến', parent: { label: 'Tuyến đường', to: '/routes' } },
  { pattern: /^\/routes\/\d+/, title: 'Chi tiết tuyến', parent: { label: 'Tuyến đường', to: '/routes' } },
  { pattern: /^\/routes/, title: 'Tuyến đường' },
  { pattern: /^\/staff/, title: 'Nhân sự vận hành' },
  { pattern: /^\/planning/, title: 'Lập chuyến' },
  { pattern: /^\/trips\/\d+\/tracking/, title: 'Theo dõi hành trình', parent: { label: 'Chuyến đi', to: '/trips' } },
  { pattern: /^\/trips\/\d+/, title: 'Chi tiết chuyến', parent: { label: 'Chuyến đi', to: '/trips' } },
  { pattern: /^\/trips/, title: 'Chuyến đi' },
  { pattern: /^\/incidents\/\d+/, title: 'Chi tiết sự cố', parent: { label: 'Sự cố', to: '/incidents' } },
  { pattern: /^\/incidents/, title: 'Sự cố' },
  { pattern: /^\/handovers\/\d+/, title: 'Biên bản bàn giao', parent: { label: 'Bàn giao', to: '/handovers' } },
  { pattern: /^\/handovers/, title: 'Bàn giao' },
  { pattern: /^\/claims\/new/, title: 'Tạo hồ sơ bồi thường', parent: { label: 'Bồi thường', to: '/claims' } },
  { pattern: /^\/claims\/\d+/, title: 'Chi tiết bồi thường', parent: { label: 'Bồi thường', to: '/claims' } },
  { pattern: /^\/claims/, title: 'Bồi thường' },
  { pattern: /^\/reports/, title: 'Báo cáo' },
  { pattern: /^\/notifications/, title: 'Thông báo' },
  { pattern: /^\/profile/, title: 'Hồ sơ & cài đặt' },
];

/** Đường dẫn chi tiết theo loại tham chiếu của thông báo (NotificationService.REF_*). */
export function notificationLink(refType?: string, refId?: number): string | undefined {
  if (!refType || !refId) return undefined;
  const map: Record<string, string> = {
    TRANSPORTATION_REQUEST: '/requests/', DOCUMENT: '/documents/', QUOTE: '/quotes/', INVOICE: '/invoices/',
    TRIP: '/trips/', INCIDENT: '/incidents/', HANDOVER: '/handovers/', CLAIM: '/claims/',
  };
  return map[refType] ? `${map[refType]}${refId}` : undefined;
}
