package com.fleet.management.repository;

import com.fleet.management.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId AND r.activo = true")
    Page<Role> findByPermissionId(@Param("permissionId") Long permissionId, Pageable pageable);
}