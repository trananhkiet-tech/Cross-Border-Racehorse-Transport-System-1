package com.example.racetransport.horse.dto;

import com.example.racetransport.horse.entity.HorseSex;
import com.example.racetransport.horse.entity.HorseStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class HorseDtos {

    private HorseDtos() { }

    public record HorseRequest(
            @NotBlank @Size(max = 150) String registeredName,
            @Size(max = 100) String breed,
            @PastOrPresent(message = "Ngày sinh không được ở tương lai") LocalDate dateOfBirth,
            HorseSex sex,
            @Size(max = 60) String color,
            @Min(100) @Max(220) Integer heightCm,
            @NotBlank @Pattern(regexp = "^[0-9A-Za-z]{9,20}$", message = "Microchip gồm 9–20 ký tự chữ/số")
            String microchipNumber,
            @NotBlank @Size(max = 50) String passportNumber,
            @Size(min = 2, max = 2) String passportCountry,
            @Size(max = 1000) String specialRequirements,
            @Size(max = 1000) String notes) { }

    public record HorseResponse(Long id, Long ownerId, String ownerName, String registeredName, String breed,
                                LocalDate dateOfBirth, Integer ageYears, HorseSex sex, String color,
                                Integer heightCm, String microchipNumber, String passportNumber,
                                String passportCountry, String specialRequirements, String notes,
                                HorseStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) { }

    public record StallTypeResponse(Long id, String code, String name, Integer widthCm, String description,
                                    BigDecimal surcharge, String currency) { }
}
