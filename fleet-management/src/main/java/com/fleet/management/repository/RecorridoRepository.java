package com.fleet.management.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.model.Recorrido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecorridoRepository extends JpaRepository<Recorrido, Long> {

    Page<Recorrido> findByVehiculoId(Long vehiculoId, Pageable pageable);

    Optional<Recorrido> findByVehiculoIdAndFecha(Long vehiculoId, LocalDate fecha);

    boolean existsByVehiculoIdAndFecha(Long vehiculoId, LocalDate fecha);

    @Query("SELECT r FROM Recorrido r WHERE r.vehiculo.id = :vehiculoId " +
           "AND r.fecha BETWEEN :desde AND :hasta ORDER BY r.fecha ASC")
    Page<Recorrido> findByVehiculoIdAndFechaBetween(@Param("vehiculoId") Long vehiculoId,
                                                    @Param("desde") LocalDate desde,
                                                    @Param("hasta") LocalDate hasta,
                                                    Pageable pageable);

    boolean existsByVehiculoIdAndFechaAfter(Long vehiculoId, LocalDate fecha);

    @Query("SELECT r FROM Recorrido r WHERE r.vehiculo.id = :vehiculoId AND r.fecha <= :fecha ORDER BY r.fecha ASC")
    List<Recorrido> findByVehiculoIdAndFechaLessThanEqualOrderByFechaAsc(@Param("vehiculoId") Long vehiculoId,
                                                                         @Param("fecha") LocalDate fecha);

    Page<Recorrido> findAllByActivoTrue(Pageable pageable);

    @Query("SELECT r.vehiculo.id AS vehiculoId, " +
           "v.matricula AS matricula, " +
           "v.modelo AS modelo, " +
           "m.nombre AS marcaNombre, " +
           "tc.codigo AS tipoCombustibleCodigo, " +
           "e.nombre AS empresaNombre, " +
           "CAST(SUM(r.kilometros) AS BigDecimal) AS kmTotal, " +
           "COALESCE(SUM(r.litrosAbastecidos), 0) AS litrosTotal, " +
           "CAST(SUM(r.kilometros * v.indiceConsumo / 100) AS BigDecimal) AS consumoTeorico " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "JOIN v.marca m " +
           "JOIN v.tipoCombustible tc " +
           "JOIN v.empresa e " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId " +
           "AND (:tipoVehiculoId IS NULL OR v.tipoVehiculo.id = :tipoVehiculoId) " +
           "AND (:marcaId IS NULL OR v.marca.id = :marcaId) " +
           "AND (:tipoCombustibleId IS NULL OR v.tipoCombustible.id = :tipoCombustibleId) " +
           "GROUP BY r.vehiculo.id, v.matricula, v.modelo, m.nombre, tc.codigo, e.nombre")
    List<ConsumoVehiculoProjection> consumoPorVehiculo(@Param("empresaId") Long empresaId,
                                                      @Param("desde") LocalDate desde,
                                                      @Param("hasta") LocalDate hasta,
                                                      @Param("tipoVehiculoId") Long tipoVehiculoId,
                                                      @Param("marcaId") Long marcaId,
                                                      @Param("tipoCombustibleId") Long tipoCombustibleId);

    @Query("SELECT COUNT(DISTINCT r.vehiculo.id) " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId " +
           "AND (:tipoVehiculoId IS NULL OR v.tipoVehiculo.id = :tipoVehiculoId) " +
           "AND (:marcaId IS NULL OR v.marca.id = :marcaId) " +
           "AND (:tipoCombustibleId IS NULL OR v.tipoCombustible.id = :tipoCombustibleId)")
    long countConsumoPorVehiculo(@Param("empresaId") Long empresaId,
                                 @Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta,
                                 @Param("tipoVehiculoId") Long tipoVehiculoId,
                                 @Param("marcaId") Long marcaId,
                                 @Param("tipoCombustibleId") Long tipoCombustibleId);

    // --- Abastecimiento Report ---

    @Query("SELECT r.vehiculo.id AS vehiculoId, " +
           "v.matricula AS matricula, " +
           "v.modelo AS modelo, " +
           "m.nombre AS marcaNombre, " +
           "tc.codigo AS tipoCombustibleCodigo, " +
           "COUNT(r) AS totalAbastecimientos, " +
           "SUM(r.litrosAbastecidos) AS totalLitros, " +
           "MIN(r.fecha) AS fechaPrimera, " +
           "MAX(r.fecha) AS fechaUltima " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "JOIN v.marca m " +
           "JOIN v.tipoCombustible tc " +
           "WHERE r.activo = true " +
           "AND r.litrosAbastecidos IS NOT NULL AND r.litrosAbastecidos > 0 " +
           "AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId " +
           "AND (:vehiculoId IS NULL OR r.vehiculo.id = :vehiculoId) " +
           "AND (:lugarAbastecimiento IS NULL OR r.lugarAbastecimiento = :lugarAbastecimiento) " +
           "GROUP BY r.vehiculo.id, v.matricula, v.modelo, m.nombre, tc.codigo")
    List<AbastecimientoVehiculoProjection> abastecimientoPorVehiculo(@Param("empresaId") Long empresaId,
                                                                      @Param("vehiculoId") Long vehiculoId,
                                                                      @Param("lugarAbastecimiento") String lugarAbastecimiento,
                                                                      @Param("desde") LocalDate desde,
                                                                      @Param("hasta") LocalDate hasta);

    @Query("SELECT r.vehiculo.id AS vehiculoId, " +
           "r.lugarAbastecimiento AS lugarAbastecimiento, " +
           "COUNT(r) AS total " +
           "FROM Recorrido r " +
           "WHERE r.activo = true " +
           "AND r.litrosAbastecidos IS NOT NULL AND r.litrosAbastecidos > 0 " +
           "AND r.fecha BETWEEN :desde AND :hasta " +
           "AND r.vehiculo.empresa.id = :empresaId " +
           "AND r.lugarAbastecimiento IS NOT NULL " +
           "AND (:vehiculoId IS NULL OR r.vehiculo.id = :vehiculoId) " +
           "AND (:lugarAbastecimiento IS NULL OR r.lugarAbastecimiento = :lugarAbastecimiento) " +
           "GROUP BY r.vehiculo.id, r.lugarAbastecimiento")
    List<AbastecimientoLugarProjection> lugarMasFrecuentePorVehiculo(@Param("empresaId") Long empresaId,
                                                                      @Param("vehiculoId") Long vehiculoId,
                                                                      @Param("lugarAbastecimiento") String lugarAbastecimiento,
                                                                      @Param("desde") LocalDate desde,
                                                                      @Param("hasta") LocalDate hasta);

    // --- Consumo por Tipo de Combustible (SQL Nativo) ---

    @Query(value = "SELECT tc.denominacion AS tipo_combustible, " +
           "COALESCE(SUM(r.consumo), 0) AS volumen_consumido, " +
           "COALESCE(SUM(r.litros_abastecidos), 0) AS volumen_abastecido, " +
           "COALESCE(SUM(r.importe_abastecido), 0) AS costo_estimado, " +
           "COUNT(r.id) AS cantidad_recorridos " +
           "FROM recorridos r " +
           "INNER JOIN vehiculos v ON r.vehiculo_id = v.id " +
           "INNER JOIN tipos_combustible tc ON v.tipo_combustible_id = tc.id " +
           "WHERE r.activo = true " +
           "AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa_id = :empresaId " +
           "AND (:tipoVehiculoId IS NULL OR v.tipo_vehiculo_id = :tipoVehiculoId) " +
           "GROUP BY tc.denominacion " +
           "ORDER BY volumen_consumido DESC",
           nativeQuery = true)
    List<ConsumoPorCombustibleProjection> consumoPorTipoCombustible(
            @Param("empresaId") Long empresaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("tipoVehiculoId") Long tipoVehiculoId);

    @Query(value = "SELECT tc.denominacion AS tipo_combustible, " +
           "COALESCE(SUM(r.consumo), 0) AS volumen_consumido, " +
           "COALESCE(SUM(r.litros_abastecidos), 0) AS volumen_abastecido, " +
           "COALESCE(SUM(r.importe_abastecido), 0) AS costo_estimado, " +
           "COUNT(r.id) AS cantidad_recorridos " +
           "FROM recorridos r " +
           "INNER JOIN vehiculos v ON r.vehiculo_id = v.id " +
           "INNER JOIN tipos_combustible tc ON v.tipo_combustible_id = tc.id " +
           "WHERE r.activo = true " +
           "AND r.fecha BETWEEN :desdeAnterior AND :hastaAnterior " +
           "AND v.empresa_id = :empresaId " +
           "AND (:tipoVehiculoId IS NULL OR v.tipo_vehiculo_id = :tipoVehiculoId) " +
           "GROUP BY tc.denominacion",
           nativeQuery = true)
    List<ConsumoPorCombustibleProjection> consumoPorTipoCombustiblePeriodoAnterior(
            @Param("empresaId") Long empresaId,
            @Param("desdeAnterior") LocalDate desdeAnterior,
            @Param("hastaAnterior") LocalDate hastaAnterior,
            @Param("tipoVehiculoId") Long tipoVehiculoId);

    // --- Dashboard Ejecutivo ---

    @Query("SELECT COALESCE(SUM(r.importeAbastecido), 0) " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId")
    Double sumCostoCombustible(@Param("empresaId") Long empresaId,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    @Query("SELECT COALESCE(SUM(r.kilometros), 0) " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId")
    Long sumKmTotales(@Param("empresaId") Long empresaId,
                      @Param("desde") LocalDate desde,
                      @Param("hasta") LocalDate hasta);

    @Query("SELECT COALESCE(SUM(r.consumo), 0) " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId")
    BigDecimal sumConsumoTotal(@Param("empresaId") Long empresaId,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    @Query("SELECT r " +
           "FROM Recorrido r " +
           "JOIN r.vehiculo v " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta " +
           "AND v.empresa.id = :empresaId")
    List<Recorrido> findRecorridosPorPeriodo(@Param("empresaId") Long empresaId,
                                             @Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta);
}
