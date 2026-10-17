package com.wearenavodayan.jnvservice.repository;

import com.wearenavodayan.jnvservice.entity.Jnv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JnvRepository extends JpaRepository<Jnv, UUID> {

    Optional<Jnv> findBySchoolCode(String schoolCode);

    boolean existsBySchoolCode(String schoolCode);

    List<Jnv> findByActiveTrueOrderByStateAscDistrictAscSchoolNameAsc();
}
