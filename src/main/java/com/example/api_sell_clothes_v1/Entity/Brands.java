package com.example.api_sell_clothes_v1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "brands")
public class Brands {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "name")
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private Boolean status;

}
