package com.example.racetransport.horse.service;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.horse.dto.HorseDtos.HorseRequest;
import com.example.racetransport.horse.dto.HorseDtos.HorseResponse;
import com.example.racetransport.horse.dto.HorseDtos.StallTypeResponse;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.entity.StallType;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.horse.repository.StallTypeRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ ngựa. Khách hàng chỉ thấy/sửa ngựa của mình; nhân viên nội bộ được xem.
 * Xóa = lưu trữ (ARCHIVED) để không mất lịch sử yêu cầu/chuyến đi.
 */
@Service
public class HorseService {

    private final HorseRepository horses;
    private final StallTypeRepository stallTypes;
    private final AppUserRepository users;
    private final RequestHorseRepository requestHorses;
    private final Clock clock;

    public HorseService(HorseRepository horses, StallTypeRepository stallTypes, AppUserRepository users,
                        RequestHorseRepository requestHorses, Clock clock) {
        this.horses = horses;
        this.stallTypes = stallTypes;
        this.users = users;
        this.requestHorses = requestHorses;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<HorseResponse> list(UserPrincipal actor, Long ownerId, HorseStatus status, String q,
                                            int page, int size) {
        Long effectiveOwner = actor.isCustomer() ? actor.getId() : ownerId;
        String query = q == null || q.isBlank() ? null : q.trim();
        var result = horses.search(effectiveOwner, status, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("registeredName")));
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public HorseResponse get(UserPrincipal actor, Long id) {
        return toResponse(loadVisible(actor, id));
    }

    @Transactional
    public HorseResponse create(UserPrincipal actor, HorseRequest req) {
        Guard.state(actor.isCustomer(), "Chỉ khách hàng tạo hồ sơ ngựa");
        validateCountry(req.passportCountry());
        if (horses.existsByMicrochipNumberIgnoreCase(req.microchipNumber())) {
            throw new ConflictException("Số microchip đã tồn tại trong hệ thống");
        }
        if (horses.existsByPassportNumberIgnoreCase(req.passportNumber())) {
            throw new ConflictException("Số hộ chiếu ngựa đã tồn tại trong hệ thống");
        }
        AppUser owner = users.getReferenceById(actor.getId());
        Horse h = new Horse();
        h.setOwner(owner);
        h.setStatus(HorseStatus.ACTIVE);
        apply(h, req);
        horses.save(h);
        return toResponse(h);
    }

    @Transactional
    public HorseResponse update(UserPrincipal actor, Long id, HorseRequest req) {
        Horse h = loadOwned(actor, id);
        Guard.state(h.getStatus() != HorseStatus.ARCHIVED, "Không thể sửa hồ sơ ngựa đã lưu trữ");
        validateCountry(req.passportCountry());
        if (horses.existsByMicrochipNumberIgnoreCaseAndIdNot(req.microchipNumber(), id)) {
            throw new ConflictException("Số microchip đã tồn tại trong hệ thống");
        }
        if (horses.existsByPassportNumberIgnoreCaseAndIdNot(req.passportNumber(), id)) {
            throw new ConflictException("Số hộ chiếu ngựa đã tồn tại trong hệ thống");
        }
        apply(h, req);
        return toResponse(h);
    }

    /** Lưu trữ thay vì xóa cứng; không cho lưu trữ khi ngựa đang trong yêu cầu còn hiệu lực. */
    @Transactional
    public HorseResponse archive(UserPrincipal actor, Long id) {
        Horse h = loadOwned(actor, id);
        Guard.state(h.getStatus() != HorseStatus.IN_TRANSIT, "Ngựa đang được vận chuyển, không thể lưu trữ");
        Guard.state(!requestHorses.existsActiveRequestForHorse(id),
                "Ngựa đang thuộc một yêu cầu vận chuyển chưa kết thúc");
        h.setStatus(HorseStatus.ARCHIVED);
        return toResponse(h);
    }

    @Transactional(readOnly = true)
    public List<StallTypeResponse> stallTypes() {
        return stallTypes.findByActiveTrueOrderBySortOrderAsc().stream().map(HorseService::toStallResponse).toList();
    }

    // ------------------------------------------------------------- access helpers (dùng chung)
    /** Customer: chỉ ngựa của mình. Nhân viên: mọi ngựa. */
    public Horse loadVisible(UserPrincipal actor, Long id) {
        Horse h = horses.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy ngựa"));
        if (actor.isCustomer() && !h.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem hồ sơ ngựa này");
        }
        return h;
    }

    public Horse loadOwned(UserPrincipal actor, Long id) {
        Horse h = horses.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy ngựa"));
        if (!actor.isCustomer() || !h.getOwner().getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa hồ sơ ngựa này");
        }
        return h;
    }

    private void apply(Horse h, HorseRequest req) {
        h.setRegisteredName(req.registeredName().trim());
        h.setBreed(req.breed());
        h.setDateOfBirth(req.dateOfBirth());
        h.setSex(req.sex());
        h.setColor(req.color());
        h.setHeightCm(req.heightCm());
        h.setMicrochipNumber(req.microchipNumber().trim().toUpperCase());
        h.setPassportNumber(req.passportNumber().trim().toUpperCase());
        h.setPassportCountry(req.passportCountry());
        h.setSpecialRequirements(req.specialRequirements());
        h.setNotes(req.notes());
    }

    private static void validateCountry(String code) {
        if (code != null && !code.isBlank()) {
            Guard.input(Countries.isSupported(code), "Mã quốc gia không được hỗ trợ: " + code);
        }
    }

    public HorseResponse toResponse(Horse h) {
        Integer age = h.getDateOfBirth() == null ? null
                : Period.between(h.getDateOfBirth(), LocalDate.now(clock)).getYears();
        return new HorseResponse(h.getId(), h.getOwner().getId(), h.getOwner().getFullName(), h.getRegisteredName(),
                h.getBreed(), h.getDateOfBirth(), age, h.getSex(), h.getColor(), h.getHeightCm(),
                h.getMicrochipNumber(), h.getPassportNumber(), h.getPassportCountry(), h.getSpecialRequirements(),
                h.getNotes(), h.getStatus(), h.getCreatedAt(), h.getUpdatedAt());
    }

    public static StallTypeResponse toStallResponse(StallType s) {
        return new StallTypeResponse(s.getId(), s.getCode(), s.getName(), s.getWidthCm(), s.getDescription(),
                s.getSurcharge(), s.getCurrency());
    }
}
