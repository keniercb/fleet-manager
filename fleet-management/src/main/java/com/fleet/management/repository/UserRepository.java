package com.fleet.management.repository;

import com.fleet.management.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * FX-18: variante con {@code @EntityGraph} que carga roles y permisos en
     * una sola query, evitando N+1 en {@code CustomUserDetailsService.loadUserByUsername}.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "empresa"})
    Optional<User> findWithRolesAndPermissionsByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findAllByActivoTrue(Pageable pageable);

    Page<User> findAllByActivoTrueAndEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<User> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    Page<User> findByEmpresaIdAndActivoTrueAndEmailContainingIgnoreCase(Long empresaId, String email, Pageable pageable);
}
