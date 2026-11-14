import type { RouteStop } from '../types/api';
import type { TransportMode } from '../types/enums';

/**
 * Ước tính tuyến từ tọa độ các điểm dừng (không gọi dịch vụ bản đồ ngoài):
 *  - khoảng cách đoạn = đường chim bay (haversine) × hệ số đường bộ 1,25;
 *  - tốc độ trung bình: đường bộ chở ngựa 55 km/h; đoạn bay (chế độ AIR, hoặc ROAD_AIR nối sân bay) 700 km/h;
 *  - thời gian dừng lấy từ plannedDurationMin của từng điểm dừng.
 * Đây là ước tính phục vụ lập kế hoạch, không thay cho dẫn đường thực tế.
 */
export const ROAD_FACTOR = 1.25;
export const ROAD_SPEED_KMH = 55;
export const AIR_SPEED_KMH = 700;

export interface RouteLeg {
  from: RouteStop; to: RouteStop; km: number; driveMin: number; dwellMin: number; byAir: boolean;
  /** Phút kể từ lúc khởi hành tới khi đến điểm `to`. */
  arriveAtMin: number;
}
export interface RouteEstimate {
  legs: RouteLeg[]; totalKm: number; driveMin: number; dwellMin: number; totalMin: number; skipped: number;
}

export function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371;
  const rad = (d: number) => (d * Math.PI) / 180;
  const dLat = rad(lat2 - lat1);
  const dLng = rad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
}

export function estimateRoute(stops: RouteStop[], mode: TransportMode = 'ROAD'): RouteEstimate | null {
  const located = stops.filter((s) => s.latitude != null && s.longitude != null).sort((a, b) => a.stopOrder - b.stopOrder);
  if (located.length < 2) return null;
  const legs: RouteLeg[] = [];
  let clock = 0;
  let totalKm = 0;
  let driveMin = 0;
  let dwellMin = 0;
  for (let i = 1; i < located.length; i += 1) {
    const from = located[i - 1];
    const to = located[i];
    const straight = haversineKm(from.latitude as number, from.longitude as number, to.latitude as number, to.longitude as number);
    const byAir = mode === 'AIR' || (mode === 'ROAD_AIR' && from.stopType === 'AIRPORT' && to.stopType === 'AIRPORT');
    const km = byAir ? straight : straight * ROAD_FACTOR;
    const drive = Math.round((km / (byAir ? AIR_SPEED_KMH : ROAD_SPEED_KMH)) * 60);
    const dwell = to.stopType === 'DESTINATION' ? 0 : to.plannedDurationMin ?? 0;
    clock += drive;
    legs.push({ from, to, km, driveMin: drive, dwellMin: dwell, byAir, arriveAtMin: clock });
    clock += dwell;
    totalKm += km;
    driveMin += drive;
    dwellMin += dwell;
  }
  // thời gian dừng tại điểm đầu (nếu có) cộng vào lúc khởi hành thực tế
  return { legs, totalKm, driveMin, dwellMin, totalMin: driveMin + dwellMin, skipped: stops.length - located.length };
}

export function formatMinutes(min: number): string {
  const m = Math.max(0, Math.round(min));
  const d = Math.floor(m / 1440);
  const h = Math.floor((m % 1440) / 60);
  const r = m % 60;
  return [d ? `${d} ngày` : '', h ? `${h} giờ` : '', r || (!d && !h) ? `${r} phút` : ''].filter(Boolean).join(' ');
}
