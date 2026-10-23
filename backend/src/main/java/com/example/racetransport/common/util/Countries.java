package com.example.racetransport.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Danh mục quốc gia DEMO (mã ISO 3166-1 alpha-2). Chỉ phục vụ hiển thị/kiểm tra đầu vào,
 * không phải danh mục pháp lý.
 */
public final class Countries {

    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    static {
        NAMES.put("VN", "Việt Nam");
        NAMES.put("TH", "Thái Lan");
        NAMES.put("KH", "Campuchia");
        NAMES.put("LA", "Lào");
        NAMES.put("MY", "Malaysia");
        NAMES.put("SG", "Singapore");
        NAMES.put("HK", "Hồng Kông");
        NAMES.put("JP", "Nhật Bản");
        NAMES.put("KR", "Hàn Quốc");
        NAMES.put("CN", "Trung Quốc");
        NAMES.put("AU", "Úc");
        NAMES.put("AE", "UAE");
        NAMES.put("GB", "Vương quốc Anh");
        NAMES.put("FR", "Pháp");
        NAMES.put("IE", "Ireland");
        NAMES.put("US", "Hoa Kỳ");
    }

    private Countries() { }

    public static boolean isSupported(String code) {
        return code != null && NAMES.containsKey(code);
    }

    public static String name(String code) {
        return code == null ? null : NAMES.getOrDefault(code, code);
    }

    public static Map<String, String> all() {
        return Map.copyOf(NAMES);
    }
}
