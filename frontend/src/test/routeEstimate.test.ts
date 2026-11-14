import { describe, expect, it } from 'vitest';
import type { RouteStop } from '../types/api';
import { estimateRoute, formatMinutes, haversineKm } from '../utils/routeEstimate';
import { rolesFor } from '../utils/staff';

const stop = (id: number, order: number, type: RouteStop['stopType'], lat: number, lng: number, dwell?: number): RouteStop =>
  ({ id, stopOrder: order, stopType: type, name: `S${id}`, latitude: lat, longitude: lng, plannedDurationMin: dwell });

describe('estimateRoute', () => {
  it('haversine HCM → Hà Nội ≈ 1.140 km đường chim bay', () => {
    expect(haversineKm(10.8231, 106.6297, 21.0278, 105.8342)).toBeGreaterThan(1100);
    expect(haversineKm(10.8231, 106.6297, 21.0278, 105.8342)).toBeLessThan(1200);
  });
  it('cộng thời gian di chuyển và thời gian dừng, bỏ dừng tại điểm đến', () => {
    const est = estimateRoute([stop(1, 1, 'PICKUP', 10, 106), stop(2, 2, 'BORDER', 11, 106, 60), stop(3, 3, 'DESTINATION', 12, 106, 999)])!;
    expect(est.legs).toHaveLength(2);
    expect(est.dwellMin).toBe(60);
    expect(est.totalMin).toBe(est.driveMin + 60);
    expect(est.legs[1].arriveAtMin).toBe(est.totalMin);
  });
  it('cần ít nhất 2 điểm có tọa độ', () => {
    expect(estimateRoute([stop(1, 1, 'PICKUP', 10, 106)])).toBeNull();
  });
  it('đường hàng không dùng tốc độ bay', () => {
    const road = estimateRoute([stop(1, 1, 'AIRPORT', 10, 106), stop(2, 2, 'DESTINATION', 20, 106)], 'ROAD')!;
    const air = estimateRoute([stop(1, 1, 'AIRPORT', 10, 106), stop(2, 2, 'DESTINATION', 20, 106)], 'AIR')!;
    expect(air.driveMin).toBeLessThan(road.driveMin);
  });
  it('formatMinutes', () => {
    expect(formatMinutes(90)).toBe('1 giờ 30 phút');
    expect(formatMinutes(1500)).toBe('1 ngày 1 giờ');
  });
});

describe('rolesFor', () => {
  it('tài xế chỉ làm tài xế; áp tải / trưởng nhóm không lái', () => {
    expect(rolesFor('DRIVER')).toEqual(['DRIVER']);
    expect(rolesFor('ESCORT')).toEqual(['ESCORT', 'TEAM_LEADER']);
    expect(rolesFor('TEAM_LEADER')).toEqual(['ESCORT', 'TEAM_LEADER']);
  });
});
