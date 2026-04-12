package com.smarthire.backend.features.company.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyBenefit {

    @Column(length = 50)
    private String id; // Lấy theo ID tự tạo của frontend

    @Column(length = 50)
    private String icon;

    @Column(length = 200)
    private String title;

    @Column(length = 500)
    private String description;
}
