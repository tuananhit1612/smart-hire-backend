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
public class CompanySocialLink {

    @Column(length = 50)
    private String platform;

    @Column(length = 500)
    private String url;
}
