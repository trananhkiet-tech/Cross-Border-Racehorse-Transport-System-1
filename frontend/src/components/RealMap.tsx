import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { RouteStop } from '../types/api';
import { StopTypeLabels, type TransportMode } from '../types/enums';

/**
 * Bản đồ thế giới thực (Leaflet + OpenStreetMap), thao tác như Google Maps: kéo, cuộn chuột để phóng/thu,
 * vệ tinh/đường phố. Tuyến đường bộ được dẫn theo đường thật qua OSRM; nếu dịch vụ không phản hồi thì vẽ
 * đường thẳng nét đứt giữa các điểm dừng. Vị trí xe cập nhật theo `vehicle` (tracking tự làm mới).
 * URL tile/OSRM cấu hình qua VITE_MAP_TILE_URL, VITE_MAP_SAT_URL, VITE_OSRM_URL (nên tự dựng server khi chạy thật).
 */
const TILE_URL = import.meta.env.VITE_MAP_TILE_URL ?? 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const SAT_URL = import.meta.env.VITE_MAP_SAT_URL ?? 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
const OSRM_URL = import.meta.env.VITE_OSRM_URL ?? 'https://router.project-osrm.org';

export interface VehiclePosition { lat: number; lng: number; label?: string; at?: string }

interface Props {
  stops: RouteStop[];
  alternative?: RouteStop[];
  currentStopId?: number | null;
  doneStopIds?: number[];
  vehicle?: VehiclePosition | null;
  blocked?: boolean;
  height?: number;
  transportMode?: TransportMode;
  incidentStopIds?: number[];
}

type LL = [number, number];
const hasLL = (s: RouteStop) => s.latitude != null && s.longitude != null;

async function osrmRoute(points: LL[], signal: AbortSignal): Promise<{ line: LL[]; km: number; min: number } | null> {
  if (points.length < 2) return null;
  const coords = points.map(([la, ln]) => `${ln},${la}`).join(';');
  try {
    const r = await fetch(`${OSRM_URL}/route/v1/driving/${coords}?overview=full&geometries=geojson`, { signal });
    if (!r.ok) return null;
    const j = await r.json();
    const rt = j?.routes?.[0];
    if (!rt) return null;
    return { line: (rt.geometry.coordinates as [number, number][]).map(([ln, la]) => [la, ln] as LL), km: rt.distance / 1000, min: rt.duration / 60 };
  } catch {
    return null;
  }
}

function pin(cls: string, text: string): L.DivIcon {
  return L.divIcon({ className: 'ir-rm-pin-wrap', html: `<span class="ir-rm-pin ${cls}">${text}</span>`, iconSize: [26, 26], iconAnchor: [13, 13] });
}

export function RealMap({ stops, alternative, currentStopId, doneStopIds = [], vehicle, blocked, height = 380, transportMode = 'ROAD', incidentStopIds = [] }: Props) {
  const el = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const layer = useRef<L.LayerGroup | null>(null);
  const vehicleMk = useRef<L.Marker | null>(null);
  const [info, setInfo] = useState<{ km: number; min: number } | null>(null);
  const [routing, setRouting] = useState<'idle' | 'loading' | 'ok' | 'fallback'>('idle');
  const [follow, setFollow] = useState(false);
  const located = stops.filter(hasLL);
  const key = located.map((s) => `${s.id}:${s.latitude},${s.longitude}`).join('|') + '#' + (alternative ?? []).filter(hasLL).map((s) => `${s.latitude},${s.longitude}`).join('|') + `#${currentStopId ?? ''}#${doneStopIds.join(',')}#${blocked ? 1 : 0}#${transportMode}`;

  // Khởi tạo bản đồ một lần
  useEffect(() => {
    if (!el.current || map.current) return undefined;
    const m = L.map(el.current, { zoomControl: true, scrollWheelZoom: true, worldCopyJump: true }).setView([16, 105], 4);
    const street = L.tileLayer(TILE_URL, { maxZoom: 19, attribution: '© OpenStreetMap' });
    const sat = L.tileLayer(SAT_URL, { maxZoom: 19, attribution: 'Ảnh vệ tinh © Esri' });
    street.addTo(m);
    L.control.layers({ 'Đường phố': street, 'Vệ tinh': sat }, undefined, { position: 'topright' }).addTo(m);
    L.control.scale({ imperial: false }).addTo(m);
    layer.current = L.layerGroup().addTo(m);
    map.current = m;
    return () => { m.remove(); map.current = null; layer.current = null; vehicleMk.current = null; };
  }, []);

  // Vẽ điểm dừng + tuyến
  useEffect(() => {
    const m = map.current;
    const g = layer.current;
    if (!m || !g || located.length < 2) return undefined;
    g.clearLayers();
    const ctrl = new AbortController();
    const pts: LL[] = located.map((s) => [s.latitude as number, s.longitude as number]);
    const currentIdx = currentStopId ? located.findIndex((s) => s.id === currentStopId) : -1;
    const doneUntil = Math.max(currentIdx, ...located.map((s, i) => (doneStopIds.includes(s.id) ? i : -1)));
    located.forEach((s, i) => {
      const incident = incidentStopIds.includes(s.id);
      const cls = incident ? 'is-incident' : i === 0 ? 'is-origin' : i === located.length - 1 ? 'is-dest' : i <= doneUntil ? 'is-done' : 'is-way';
      L.marker(pts[i], { icon: pin(cls, incident ? '!' : String(s.stopOrder)), title: s.name })
        .bindPopup(`<b>${s.stopOrder}. ${s.name}</b><br/>${StopTypeLabels[s.stopType]}${s.address ? `<br/>${s.address}` : ''}`)
        .addTo(g);
    });
    const alt = (alternative ?? []).filter(hasLL).map((s) => [s.latitude as number, s.longitude as number] as LL);
    if (alt.length > 1) L.polyline(alt, { color: '#C9A45C', weight: 4, dashArray: '8 8', opacity: 0.9 }).bindTooltip('Tuyến thay thế').addTo(g);
    m.fitBounds(L.latLngBounds([...pts, ...alt]), { padding: [40, 40] });

    const drawLine = (line: LL[], dashed: boolean) => {
      L.polyline(line, { color: '#102D4F', weight: 5, opacity: 0.85, dashArray: dashed ? '10 8' : undefined }).addTo(g);
      if (doneUntil > 0) {
        // phần đã đi: tô màu nổi bật theo tỷ lệ số điểm dừng đã qua
        const part = dashed ? pts.slice(0, doneUntil + 1) : line.slice(0, Math.max(2, Math.round((line.length * doneUntil) / (located.length - 1))));
        L.polyline(part, { color: blocked ? '#B42323' : '#2E7D32', weight: 6, opacity: 0.95 }).addTo(g);
      }
    };
    if (transportMode === 'AIR') {
      drawLine(pts, true);
      setRouting('fallback');
      setInfo(null);
    } else {
      setRouting('loading');
      setInfo(null);
      osrmRoute(pts, ctrl.signal).then((r) => {
        if (ctrl.signal.aborted) return;
        if (r) { drawLine(r.line, false); setInfo({ km: r.km, min: r.min }); setRouting('ok'); } else { drawLine(pts, true); setRouting('fallback'); }
      });
    }
    return () => ctrl.abort();
  }, [key]); // eslint-disable-line react-hooks/exhaustive-deps

  // Vị trí xe: cập nhật tại chỗ, không vẽ lại cả bản đồ
  useEffect(() => {
    const m = map.current;
    if (!m) return;
    if (!vehicle) { vehicleMk.current?.remove(); vehicleMk.current = null; return; }
    const ll: LL = [vehicle.lat, vehicle.lng];
    const icon = L.divIcon({ className: 'ir-rm-pin-wrap', html: `<span class="ir-rm-vehicle${blocked ? ' is-blocked' : ''}">🚚</span>`, iconSize: [34, 34], iconAnchor: [17, 17] });
    if (!vehicleMk.current) vehicleMk.current = L.marker(ll, { icon, zIndexOffset: 1000 }).addTo(m);
    else { vehicleMk.current.setLatLng(ll); vehicleMk.current.setIcon(icon); }
    vehicleMk.current.bindTooltip(`${vehicle.label ?? 'Vị trí hiện tại'}${vehicle.at ? ` · ${new Date(vehicle.at).toLocaleTimeString('vi-VN')}` : ''}`);
    if (follow) m.panTo(ll);
  }, [vehicle?.lat, vehicle?.lng, vehicle?.at, blocked, follow]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="ir-realmap" style={{ height }}>
      <div ref={el} className="ir-realmap-canvas" role="application" aria-label="Bản đồ tuyến vận chuyển" />
      <div className="ir-realmap-status app-text-xs">
        {routing === 'loading' && 'Đang dẫn tuyến theo đường thật…'}
        {routing === 'ok' && info && `Theo đường thật: ${Math.round(info.km).toLocaleString('vi-VN')} km · ${Math.floor(info.min / 60)} giờ ${Math.round(info.min % 60)} phút lái`}
        {routing === 'fallback' && (transportMode === 'AIR' ? 'Đường bay thẳng giữa các điểm' : 'Không dẫn được theo đường thật – hiển thị đường thẳng nét đứt')}
        {vehicle && (
          <label className="ir-realmap-follow"><input type="checkbox" checked={follow} onChange={(e) => setFollow(e.target.checked)} /> Theo xe</label>
        )}
      </div>
    </div>
  );
}
