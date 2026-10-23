import type { RouteStop } from '../types/api';
import { StopTypeLabels } from '../types/enums';
import { Icon } from './Icon';

/**
 * Bản đồ sơ đồ tuyến (không dùng dịch vụ bản đồ bên ngoài): chiếu tọa độ lat/lng của các điểm dừng
 * vào khung SVG. Đủ để thể hiện thứ tự điểm dừng, đoạn đã đi và vị trí hiện tại trong demo.
 */
const STOP_ICON: Record<string, { icon: string; cls: string }> = {
  PICKUP: { icon: 'bi-geo-alt-fill', cls: 'm-origin' },
  DESTINATION: { icon: 'bi-flag-fill', cls: 'm-dest' },
  BORDER: { icon: 'bi-signpost-split', cls: 'm-border' },
  CUSTOMS: { icon: 'bi-file-earmark-check', cls: 'm-border' },
  QUARANTINE: { icon: 'bi-shield-plus', cls: 'm-border' },
  REST_STOP: { icon: 'bi-cup-hot', cls: 'm-rest' },
  STABLE: { icon: 'bi-house-door', cls: 'm-rest' },
  AIRPORT: { icon: 'bi-airplane', cls: 'm-check' },
  CHECKPOINT: { icon: 'bi-check2-square', cls: 'm-check' },
};

interface RouteMapProps {
  stops: RouteStop[];
  /** Id điểm dừng hiện tại (đoạn trước đó được tô "đã đi"). */
  currentStopId?: number | null;
  /** Các điểm dừng đã hoàn thành. */
  doneStopIds?: number[];
  /** Hiển thị biểu tượng phương tiện tại điểm hiện tại. */
  showVehicle?: boolean;
  blocked?: boolean;
  height?: number;
  alternative?: RouteStop[];
  incidentStopIds?: number[];
}

export function RouteMap({ stops, currentStopId, doneStopIds = [], showVehicle, blocked, height = 320, alternative, incidentStopIds = [] }: RouteMapProps) {
  const located = stops.filter((s) => s.latitude !== undefined && s.latitude !== null && s.longitude !== undefined && s.longitude !== null);
  const altLocated = (alternative ?? []).filter((s) => s.latitude != null && s.longitude != null);
  if (located.length < 2) {
    return (
      <div className="ir-map d-flex align-items-center justify-content-center text-center p-4" style={{ height: 160 }}>
        <span className="ir-muted app-text-sm">
          <Icon name="bi-map" /> Chưa đủ tọa độ điểm dừng để vẽ sơ đồ tuyến.
        </span>
      </div>
    );
  }
  const all = [...located, ...altLocated];
  const lats = all.map((s) => s.latitude as number);
  const lngs = all.map((s) => s.longitude as number);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);
  const W = 1000;
  const H = 600;
  const pad = 80;
  const spanLng = Math.max(maxLng - minLng, 0.5);
  const spanLat = Math.max(maxLat - minLat, 0.5);
  const scale = Math.min((W - pad * 2) / spanLng, (H - pad * 2) / spanLat);
  const offX = (W - spanLng * scale) / 2;
  const offY = (H - spanLat * scale) / 2;
  const project = (s: RouteStop) => ({
    x: offX + ((s.longitude as number) - minLng) * scale,
    y: H - (offY + ((s.latitude as number) - minLat) * scale),
  });
  const pts = located.map(project);
  const path = (p: { x: number; y: number }[]) => p.map((q, i) => `${i ? 'L' : 'M'}${q.x.toFixed(1)},${q.y.toFixed(1)}`).join(' ');

  const currentIdx = currentStopId ? located.findIndex((s) => s.id === currentStopId) : -1;
  const doneUntil = Math.max(currentIdx, ...located.map((s, i) => (doneStopIds.includes(s.id) ? i : -1)));

  return (
    <div className="ir-map" style={{ height }}>
      <svg className="ir-map-svg" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMid meet" role="img" aria-label="Sơ đồ tuyến vận chuyển">
        <rect className="ir-map-land" x="0" y="0" width={W} height={H} />
        <path className="ir-map-water" d={`M0,${H - 40} C${W * 0.3},${H - 90} ${W * 0.6},${H - 10} ${W},${H - 70} L${W},${H} L0,${H} Z`} />
        {altLocated.length > 1 && (
          <g className="ir-route-alt">
            <path className="ir-route-casing" d={path(altLocated.map(project))} />
            <path className="ir-route-line" d={path(altLocated.map(project))} />
          </g>
        )}
        <g>
          <path className="ir-route-casing" d={path(pts)} />
          <path className="ir-route-line" d={path(pts)} style={{ strokeDasharray: doneUntil >= 0 ? '10 6' : undefined }} />
        </g>
        {doneUntil > 0 && (
          <g>
            <path className="ir-route-line" d={path(pts.slice(0, doneUntil + 1))} style={blocked ? { stroke: 'var(--route-emergency)' } : undefined} />
          </g>
        )}
      </svg>
      {located.map((s, i) => {
        const p = pts[i];
        const meta = STOP_ICON[s.stopType] ?? { icon: 'bi-circle-fill', cls: 'm-way' };
        const isCurrent = i === currentIdx;
        const incident = incidentStopIds.includes(s.id);
        const left = (p.x / W) * 100;
        const labelLeft = left > 70;
        return (
          <div
            key={s.id}
            className={`ir-marker ${incident ? 'm-incident' : meta.cls}${isCurrent ? ' is-live' : ''}`}
            style={{ left: `${left}%`, top: `${(p.y / H) * 100}%` }}
            title={`${s.stopOrder}. ${s.name} – ${StopTypeLabels[s.stopType]}`}
          >
            <span className="ir-marker-pin">
              <Icon name={incident ? 'bi-exclamation-triangle-fill' : meta.icon} size={13} />
            </span>
            <span className={`ir-marker-label${labelLeft ? ' is-left' : ''}`} style={{ position: 'absolute', left: labelLeft ? 'auto' : 'calc(100% + 6px)', top: '50%', transform: 'translateY(-50%)', whiteSpace: 'nowrap' }}>
              <span className="ir-marker-name">{s.name}</span>
              <span className="ir-marker-sub">{StopTypeLabels[s.stopType]}</span>
            </span>
          </div>
        );
      })}
      {showVehicle && currentIdx >= 0 && (
        <div className={`ir-marker ${blocked ? 'm-delayed' : 'm-vehicle'}`} style={{ left: `${(pts[currentIdx].x / W) * 100}%`, top: `${(pts[currentIdx].y / H) * 100 - 9}%` }} title="Vị trí hiện tại">
          <span className="ir-marker-pin">
            <Icon name="bi-truck" size={15} />
          </span>
        </div>
      )}
      <ul className="ir-map-legend app-text-xs">
        <li><span className="ir-legend-line" /> Tuyến kế hoạch</li>
        {alternative && alternative.length > 1 && <li><span className="ir-legend-line is-alt" /> Tuyến thay thế</li>}
        {doneUntil > 0 && <li><span className="ir-legend-line" style={blocked ? { borderTopColor: 'var(--route-emergency)' } : undefined} /> Đã đi qua</li>}
      </ul>
      <span className="ir-map-attrib">Sơ đồ minh họa – không theo tỷ lệ</span>
    </div>
  );
}
