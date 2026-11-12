package com.example.racetransport.tracking.service;

import static com.example.racetransport.trip.entity.TripStatus.*;

import com.example.racetransport.trip.entity.TripStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Các chuyển trạng thái hợp lệ của chuyến. */
public final class TripStateMachine {

    /** Trạng thái "đang trên đường". */
    public static final Set<TripStatus> EN_ROUTE = EnumSet.of(IN_TRANSIT, AT_CHECKPOINT, AT_BORDER,
            CUSTOMS_CLEARANCE, QUARANTINE, DELAYED);

    /** Trạng thái trước khởi hành. */
    public static final Set<TripStatus> PRE_DEPARTURE = EnumSet.of(SCHEDULED, PREPARING, READY_FOR_DEPARTURE);

    /** Đang hoạt động (chưa kết thúc). */
    public static final Set<TripStatus> ACTIVE = EnumSet.complementOf(EnumSet.of(DELIVERED, CANCELLED));

    private static final Map<TripStatus, Set<TripStatus>> T = new EnumMap<>(TripStatus.class);

    static {
        T.put(SCHEDULED, EnumSet.of(PREPARING, CANCELLED));
        T.put(PREPARING, EnumSet.of(READY_FOR_DEPARTURE, CANCELLED));
        T.put(READY_FOR_DEPARTURE, EnumSet.of(IN_TRANSIT, PREPARING, CANCELLED));
        Set<TripStatus> enRouteTargets = EnumSet.copyOf(EN_ROUTE);
        enRouteTargets.add(ARRIVED);
        for (TripStatus s : EN_ROUTE) {
            Set<TripStatus> targets = EnumSet.copyOf(enRouteTargets);
            targets.remove(s);
            T.put(s, targets);
        }
        T.put(ARRIVED, EnumSet.of(HANDOVER_PENDING));
        T.put(HANDOVER_PENDING, EnumSet.of(DELIVERED));
        T.put(DELIVERED, EnumSet.noneOf(TripStatus.class));
        T.put(CANCELLED, EnumSet.noneOf(TripStatus.class));
    }

    private TripStateMachine() { }

    public static boolean canTransition(TripStatus from, TripStatus to) {
        return T.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<TripStatus> targets(TripStatus from) {
        return T.getOrDefault(from, Set.of());
    }
}
