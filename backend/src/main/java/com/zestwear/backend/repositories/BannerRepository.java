package com.zestwear.backend.repositories;

import com.zestwear.backend.models.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActiveTrueOrderByOrderIndexAsc();
}
