package com.example.racetransport.horse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.horse.dto.HorseDtos.HorseRequest;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.horse.repository.StallTypeRepository;
import com.example.racetransport.horse.service.HorseService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Customer A không đọc/sửa được ngựa của Customer B. */
class HorseOwnershipTest {

    private HorseRepository horses;
    private RequestHorseRepository requestHorses;
    private HorseService service;
    private AppUser a;
    private AppUser b;
    private Horse horseOfA;

    @BeforeEach
    void setUp() {
        horses = mock(HorseRepository.class);
        requestHorses = mock(RequestHorseRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new HorseService(horses, mock(StallTypeRepository.class), users, requestHorses, TestData.clock());
        a = TestData.user(1, Role.CUSTOMER, "Customer A");
        b = TestData.user(2, Role.CUSTOMER, "Customer B");
        horseOfA = TestData.horse(10, a, "Silver Arrow");
        when(horses.findById(10L)).thenReturn(Optional.of(horseOfA));
        when(users.getReferenceById(any())).thenReturn(a);
    }

    @Test
    void otherCustomerCannotViewHorse() {
        assertThrows(ForbiddenException.class, () -> service.get(TestData.principal(b), 10L));
    }

    @Test
    void otherCustomerCannotUpdateHorse() {
        HorseRequest req = new HorseRequest("Hacked", null, null, null, null, null, "985112004517330", "VN-X", null, null, null);
        assertThrows(ForbiddenException.class, () -> service.update(TestData.principal(b), 10L, req));
    }

    @Test
    void ownerAndStaffCanView() {
        assertEquals("Silver Arrow", service.get(TestData.principal(a), 10L).registeredName());
        AppUser manager = TestData.user(3, Role.LOGISTICS_MANAGER, "Manager");
        assertEquals("Silver Arrow", service.get(TestData.principal(manager), 10L).registeredName());
    }

    @Test
    void duplicateMicrochipIsRejected() {
        when(horses.existsByMicrochipNumberIgnoreCase("985112004517330")).thenReturn(true);
        HorseRequest req = new HorseRequest("New", null, null, null, null, null, "985112004517330", "VN-NEW", null, null, null);
        assertThrows(ConflictException.class, () -> service.create(TestData.principal(a), req));
    }

    @Test
    void cannotArchiveHorseInActiveRequest() {
        when(requestHorses.existsActiveRequestForHorse(10L)).thenReturn(true);
        assertThrows(InvalidStateException.class, () -> service.archive(TestData.principal(a), 10L));
        assertEquals(HorseStatus.ACTIVE, horseOfA.getStatus());
    }
}
