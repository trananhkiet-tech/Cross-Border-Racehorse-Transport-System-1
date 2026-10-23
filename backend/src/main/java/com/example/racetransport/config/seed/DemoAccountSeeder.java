package com.example.racetransport.config.seed;

import com.example.racetransport.config.AppProperties;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.CustomerProfile;
import com.example.racetransport.user.entity.CustomerType;
import com.example.racetransport.user.entity.EmployeeProfile;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.CustomerProfileRepository;
import com.example.racetransport.user.repository.EmployeeProfileRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Chỉ dành cho nhánh main: tạo 9 tài khoản demo (5 role) để đăng nhập thử khi chưa có module nghiệp vụ.
 * Khi merge user5, tệp này bị XOÁ vì DemoDataSeeder (user5) đã tạo cùng bộ tài khoản kèm dữ liệu nghiệp vụ.
 * Chỉ chạy ở profile dev, và chỉ khi bảng app_user trống.
 */
@Component
@Profile("dev")
public class DemoAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountSeeder.class);
    public static final String DEMO_PASSWORD = "Demo@123";

    private final AppProperties props;
    private final TransactionTemplate tx;
    private final AppUserRepository users;
    private final CustomerProfileRepository customers;
    private final EmployeeProfileRepository employees;
    private final PasswordEncoder encoder;

    public DemoAccountSeeder(AppProperties props, PlatformTransactionManager txManager, AppUserRepository users,
                             CustomerProfileRepository customers, EmployeeProfileRepository employees,
                             PasswordEncoder encoder) {
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
        this.users = users;
        this.customers = customers;
        this.employees = employees;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seed().demoData() || users.count() > 0) {
            return;
        }
        tx.executeWithoutResult(status -> seedUsers());
        log.warn("Đã tạo tài khoản demo (profile dev). Mật khẩu mọi tài khoản: {}", DEMO_PASSWORD);
    }

    private void seedUsers() {
        customer("customer", "customer@irtms.vn", "Trần Thu Hà", "0903 118 226", "Sao Mai Racing Stable", CustomerType.STABLE,
                "12 Nguyễn Văn Trỗi, Phú Nhuận, TP.HCM");
        customer("customer2", "customer2@irtms.vn", "Lâm Quốc Việt", "0912 456 789", "Phương Nam Equestrian", CustomerType.COMPANY,
                "88 Võ Văn Kiệt, Quận 1, TP.HCM");
        employee("manager", "manager@irtms.vn", "Phạm Quốc Bảo", Role.LOGISTICS_MANAGER, "LM-001", "Điều hành vận tải", null, null);
        employee("specialist", "specialist@irtms.vn", "Lê Thu Trang", Role.TRANSPORT_SPECIALIST, "TS-001", "Hồ sơ & tuân thủ", null, null);
        employee("coordinator", "coordinator@irtms.vn", "Võ Minh Khang", Role.FLEET_ROUTE_COORDINATOR, "FC-001", "Đội xe & tuyến", null, null);
        employee("driver", "driver@irtms.vn", "Lê Hoàng Nam", Role.DRIVER_ESCORT, "DR-001", "Vận hành", StaffRole.DRIVER, "79C-118226");
        employee("escort", "escort@irtms.vn", "Nguyễn Minh Anh", Role.DRIVER_ESCORT, "ES-001", "Vận hành", StaffRole.ESCORT, null);
        employee("driver2", "driver2@irtms.vn", "Trần Văn Lộc", Role.DRIVER_ESCORT, "DR-002", "Vận hành", StaffRole.DRIVER, "79C-224466");
        employee("escort2", "escort2@irtms.vn", "Đặng Thu Hương", Role.DRIVER_ESCORT, "ES-002", "Vận hành", StaffRole.TEAM_LEADER, "79C-335577");
        employee("escort3", "escort3@irtms.vn", "Hồ Gia Huy", Role.DRIVER_ESCORT, "ES-003", "Vận hành", StaffRole.ESCORT, null);
    }

    private void customer(String key, String email, String name, String phone, String company, CustomerType type, String address) {
        AppUser u = user(email, name, phone, Role.CUSTOMER);
        CustomerProfile p = new CustomerProfile();
        p.setUser(u);
        p.setCompanyName(company);
        p.setCustomerType(type);
        p.setAddress(address);
        p.setCountry("VN");
        customers.save(p);
    }

    private void employee(String key, String email, String name, Role role, String code, String dept, StaffRole staffRole,
                          String license) {
        AppUser u = user(email, name, "0900 000 " + code.substring(code.length() - 3), role);
        EmployeeProfile p = new EmployeeProfile();
        p.setUser(u);
        p.setEmployeeCode(code);
        p.setDepartment(dept);
        p.setStaffRole(staffRole);
        p.setLicenseNumber(license);
        p.setLicenseExpiry(license == null ? null : LocalDate.of(2029, 6, 30));
        p.setAirEscortCertified(staffRole == StaffRole.ESCORT || staffRole == StaffRole.TEAM_LEADER);
        p.setEmergencyContact("Phòng điều hành IRTMS – 028 3838 0000");
        p.setBaseLocation("TP. Hồ Chí Minh");
        employees.save(p);
    }

    private AppUser user(String email, String name, String phone, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setEnabled(true);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        return users.save(u);
    }
}
