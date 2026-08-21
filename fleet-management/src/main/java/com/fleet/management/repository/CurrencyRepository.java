package com.fleet.management.repository;

import com.fleet.management.model.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByIsoCode(String isoCode);

    boolean existsByIsoCode(String isoCode);

    Page<Currency> findAllByActivoTrue(Pageable pageable);
}
