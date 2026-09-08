package com.fleet.management.exception;

import java.math.BigDecimal;

/**
 * Mensajes de error de negocio estandarizados para toda la aplicación.
 *
 * <p>Centraliza los mensajes de {@link BusinessException} para garantizar:
 * <ul>
 *   <li><b>Consistencia:</b> mismo tono, misma terminología, mismo formato.</li>
 *   <li><b>Trazabilidad:</b> facilita buscar mensajes en logs y en el código fuente.</li>
 *   <li><b>Mantenibilidad:</b> cambios de mensaje en un solo lugar.</li>
 *   <li><b>Coherencia:</b> el usuario recibe mensajes claros y específicos.</li>
 * </ul>
 *
 * <p>Convención de mensajes:
 * <ul>
 *   <li>Describen <b>qué</b> está mal, no <b>por qué</b> falló el código.</li>
 *   <li>Incluyen el valor problemático entre comillas simples (ej: {@code 'ABC123'}).</li>
 *   <li>Usan voz activa: "Ya existe...", "No se puede...", "El saldo...".</li>
 *   <li>Sin acentos en el código (compatibilidad con logs), pero con tildes en
 *       el mensaje final para el usuario.</li>
 * </ul>
 */
public final class BusinessError {

    private BusinessError() {
    }

    // =========================================================================
    // Auth
    // =========================================================================

    public static BusinessException usuarioNoEncontrado(String email) {
        return new BusinessException("No existe un usuario con el email '" + email + "'");
    }

    public static BusinessException usuarioInactivo(String email) {
        return new BusinessException("El usuario con email '" + email + "' está inactivo");
    }

    public static BusinessException noHayUsuarioAutenticado() {
        return new BusinessException("No hay un usuario autenticado en la sesión actual");
    }

    public static BusinessException tokenInvalidoOExpirado() {
        return new BusinessException("El token JWT es inválido o ha expirado. Inicie sesión nuevamente");
    }

    public static BusinessException usuarioAutenticadoNoEncontrado(String email) {
        return new BusinessException("El usuario autenticado con email '" + email + "' no fue encontrado en la base de datos. Posible inconsistencia de datos");
    }

    public static BusinessException contrasenaAnteriorIncorrecta() {
        return new BusinessException("La contraseña anterior es incorrecta");
    }

    public static BusinessException contrasenasNoCoinciden() {
        return new BusinessException("La nueva contraseña y su confirmación no coinciden");
    }

    public static BusinessException contrasenaIgualActual() {
        return new BusinessException("La nueva contraseña debe ser diferente a la actual");
    }

    // =========================================================================
    // Recorridos
    // =========================================================================

    public static BusinessException mesInvalido(int mes) {
        return new BusinessException("El mes debe estar entre 1 y 12. Se recibió: " + mes);
    }

    public static BusinessException recorridoYaExiste(Long vehiculoId, String fecha) {
        return new BusinessException("Ya existe un recorrido para el vehículo con id " + vehiculoId
                + " en la fecha " + fecha);
    }

    public static BusinessException recorridoFechaAnterior(Long vehiculoId, String fecha) {
        return new BusinessException("No se puede registrar un recorrido con fecha " + fecha
                + " porque ya existe un recorrido posterior para el vehículo con id " + vehiculoId);
    }

    public static BusinessException recorridoCombustibleInsuficiente(BigDecimal combustibleRestante) {
        return new BusinessException("No se puede registrar el recorrido porque consumiría más combustible ("
                + combustibleRestante + " litros) del disponible en el tanque");
    }

    public static BusinessException tarjetaImporteObligatorio() {
        return new BusinessException("Si se especifica una tarjeta de combustible, el importe abastecido es obligatorio y debe ser mayor a cero");
    }

    public static BusinessException tarjetaSaldoInsuficiente() {
        return new BusinessException("El saldo de la tarjeta de combustible no puede quedar en cero o negativo después del descuento");
    }

    public static BusinessException importeSinTarjeta() {
        return new BusinessException("Si se especifica un importe abastecido, también debe especificarse la tarjeta de combustible asociada");
    }

    public static BusinessException recorridoNoCambiarVehiculo() {
        return new BusinessException("No se permite cambiar el vehículo de un recorrido existente");
    }

    public static BusinessException recorridoNoCambiarFecha() {
        return new BusinessException("No se permite cambiar la fecha de un recorrido existente");
    }

    public static BusinessException recorridoModificarConPosterior(Long vehiculoId) {
        return new BusinessException("No se puede modificar el recorrido porque existe un recorrido posterior para el vehículo con id " + vehiculoId);
    }

    public static BusinessException recorridoEliminarConPosterior(Long vehiculoId) {
        return new BusinessException("No se puede eliminar el recorrido porque existe un recorrido posterior para el vehículo con id " + vehiculoId);
    }

    public static BusinessException empresaUsuarioNoDeterminada() {
        return new BusinessException("No se pudo determinar la empresa del usuario autenticado");
    }

    public static BusinessException usuarioSinEmpresa() {
        return new BusinessException("El usuario autenticado no tiene una empresa asociada");
    }

    // =========================================================================
    // Vehículos y suscripciones
    // =========================================================================

    public static BusinessException maxVehiculosSoloMayor(Integer actual) {
        return new BusinessException("La cantidad máxima de vehículos solo puede aumentarse. Valor actual: " + actual);
    }

    public static BusinessException maxVehiculosMenorActual(Integer actuales) {
        return new BusinessException("La cantidad máxima de vehículos no puede ser menor que los vehículos actuales: " + actuales);
    }

    public static BusinessException maxUsuariosSoloMayor(Integer actual) {
        return new BusinessException("La cantidad máxima de usuarios solo puede aumentarse. Valor actual: " + actual);
    }

    public static BusinessException maxUsuariosMenorActual(Integer actuales) {
        return new BusinessException("La cantidad máxima de usuarios no puede ser menor que los usuarios actuales: " + actuales);
    }

    public static BusinessException suscripcionModificadaConcurrente() {
        return new BusinessException("La suscripción fue modificada por otro usuario. Recargue los datos e intente nuevamente");
    }

    public static BusinessException limiteVehiculosAlcanzado(Integer max) {
        return new BusinessException("No se puede agregar el vehículo. Se ha alcanzado el límite de " + max + " vehículos de la suscripción");
    }

    public static BusinessException conteoVehiculosNegativo() {
        return new BusinessException("El conteo de vehículos no puede ser negativo. Posible inconsistencia de datos");
    }

    public static BusinessException limiteUsuariosAlcanzado(Integer max) {
        return new BusinessException("No se puede agregar el usuario. Se ha alcanzado el límite de " + max + " usuarios de la suscripción");
    }

    public static BusinessException conteoUsuariosNegativo() {
        return new BusinessException("El conteo de usuarios no puede ser negativo. Posible inconsistencia de datos");
    }

    public static BusinessException empresaSinSuscripcionActiva() {
        return new BusinessException("La empresa no tiene una suscripción activa. Contacte al administrador");
    }

    public static BusinessException limiteUsuariosCreacionAlcanzado(Integer max) {
        return new BusinessException("No se puede crear el usuario. Se ha alcanzado el límite de " + max + " usuarios de la suscripción");
    }

    // =========================================================================
    // Pagos
    // =========================================================================

    public static BusinessException pagoTipoRequiereSuscripcion(String tipo) {
        return new BusinessException("El tipo de pago '" + tipo + "' requiere especificar una suscripción existente");
    }

    public static BusinessException pagoConsultaEmpresaAjena() {
        return new BusinessException("No tiene permisos para consultar pagos de otra empresa");
    }

    public static BusinessException pagoCancelarEstadoInvalido(String estadoActual) {
        return new BusinessException("Solo se pueden cancelar pagos en estado PENDIENTE o QR_GENERADO. Estado actual: " + estadoActual);
    }

    public static BusinessException pagoReintentarEstadoInvalido(String estadoActual) {
        return new BusinessException("Solo se pueden reintentar pagos en estado FALLIDO. Estado actual: " + estadoActual);
    }

    public static BusinessException pagoMaxReintentosAlcanzados(int max) {
        return new BusinessException("Se ha alcanzado el máximo de " + max + " reintentos permitidos para la generación del QR");
    }

    public static BusinessException pagoModificadoConcurrente() {
        return new BusinessException("El pago fue modificado por otro proceso. Recargue los datos e intente nuevamente");
    }

    public static BusinessException pagoReintentarErrorQR(String causa) {
        return new BusinessException("Error al regenerar el código QR de pago: " + causa);
    }

    public static BusinessException pagoOperacionAjena() {
        return new BusinessException("No tiene permisos para operar sobre este pago. El pago pertenece a otra empresa");
    }

    // =========================================================================
    // Vehículos (update)
    // =========================================================================

    public static BusinessException vehiculoOdometroNoModificable() {
        return new BusinessException("El odómetro no se puede modificar directamente; se actualiza automáticamente al registrar recorridos");
    }

    public static BusinessException vehiculoCombustibleNoModificable() {
        return new BusinessException("El combustible no se puede modificar directamente; se actualiza automáticamente al registrar recorridos");
    }

    // =========================================================================
    // Empresas
    // =========================================================================

    public static BusinessException empresaYaExisteCodigo(String codigo) {
        return new BusinessException("Ya existe una empresa con el código '" + codigo + "'");
    }

    public static BusinessException empresaMunicipioProvinciaIncompatible() {
        return new BusinessException("El municipio seleccionado no pertenece a la provincia indicada");
    }

    public static BusinessException empresaAdminNoModificable() {
        return new BusinessException("La empresa de administración no puede ser modificada ni eliminada");
    }

    public static BusinessException empresaEmailRequeridoParaAdmin() {
        return new BusinessException("La empresa debe tener un email configurado para crear el usuario administrador");
    }

    public static BusinessException empresaEmailYaRegistrado(String email) {
        return new BusinessException("Ya existe un usuario con el email '" + email + "' asociado a esta empresa");
    }

    // =========================================================================
    // Usuarios
    // =========================================================================

    public static BusinessException usuarioYaExisteEmail(String email) {
        return new BusinessException("Ya existe un usuario con el email '" + email + "'");
    }

    // =========================================================================
    // Provincias
    // =========================================================================

    public static BusinessException provinciaYaExisteCodigo(Integer codigo) {
        return new BusinessException("Ya existe una provincia con el código '" + codigo + "'");
    }

    // =========================================================================
    // Reportes
    // =========================================================================

    public static BusinessException reporteFechasObligatorias() {
        return new BusinessException("Las fechas 'desde' y 'hasta' son obligatorias para este reporte");
    }

    public static BusinessException reporteFechaDesdeMayorHasta() {
        return new BusinessException("La fecha 'desde' no puede ser mayor que la fecha 'hasta'");
    }

    // =========================================================================
    // Vehículos (unicidad y límites)
    // =========================================================================

    public static BusinessException vehiculoYaExisteMatricula(String matricula) {
        return new BusinessException("Ya existe un vehículo con la matrícula '" + matricula + "'");
    }

    public static BusinessException vehiculoYaExisteMotor(String numeroMotor) {
        return new BusinessException("Ya existe un vehículo con el número de motor '" + numeroMotor + "'");
    }

    public static BusinessException vehiculoLimiteAlcanzado(Integer max) {
        return new BusinessException("No se puede crear el vehículo. Se ha alcanzado el límite de " + max + " vehículos de la suscripción");
    }

    // =========================================================================
    // Tarjetas de combustible
    // =========================================================================

    public static BusinessException tarjetaYaExisteNumero(String numero) {
        return new BusinessException("Ya existe una tarjeta de combustible con el número '" + numero + "'");
    }

    // =========================================================================
    // Roles y permisos
    // =========================================================================

    public static BusinessException rolYaExisteNombre(String nombre) {
        return new BusinessException("Ya existe un rol con el nombre '" + nombre + "'");
    }

    public static BusinessException featureYaExisteNombre(String nombre) {
        return new BusinessException("Ya existe un feature con el nombre '" + nombre + "'");
    }

    public static BusinessException choferCategoriaYaAsignada(Long categoriaId) {
        return new BusinessException("El chofer ya tiene asignada la categoría de licencia con id " + categoriaId);
    }

    public static BusinessException choferCategoriaYaAsignada(String codigo, String denominacion) {
        return new BusinessException("El chofer ya tiene asignada la categoría de licencia '"
                + codigo + " - " + denominacion + "'");
    }

    // =========================================================================
    // Catálogos maestros (unicidad)
    // =========================================================================

    public static BusinessException marcaYaExisteNombre(String nombre) {
        return new BusinessException("Ya existe una marca con el nombre '" + nombre + "'");
    }

    public static BusinessException currencyYaExisteIso(String isoCode) {
        return new BusinessException("Ya existe una moneda con el código ISO '" + isoCode + "'");
    }

    public static BusinessException tipoCombustibleYaExisteCodigo(String codigo) {
        return new BusinessException("Ya existe un tipo de combustible con el código '" + codigo + "'");
    }

    public static BusinessException planYaExisteNombre(String nombre) {
        return new BusinessException("Ya existe un plan con el nombre '" + nombre + "'");
    }

    public static BusinessException categoriaLicenciaYaExisteCodigo(String codigo) {
        return new BusinessException("Ya existe una categoría de licencia con el código '" + codigo + "'");
    }

    // =========================================================================
    // Más catálogos
    // =========================================================================

    public static BusinessException tipoVehiculoYaExisteNombre(String nombre) {
        return new BusinessException("Ya existe un tipo de vehículo con el nombre '" + nombre + "'");
    }

    public static BusinessException permissionYaExisteNombre(String nombre) {
        return new BusinessException("Ya existe un permiso con el nombre '" + nombre + "'");
    }

    public static BusinessException municipioYaExisteCodigo(Integer codigo) {
        return new BusinessException("Ya existe un municipio con el código '" + codigo + "'");
    }

    public static BusinessException choferYaExisteCarne(String carne) {
        return new BusinessException("Ya existe un chofer con el carné de identidad '" + carne + "'");
    }

    public static BusinessException choferYaExisteLicencia(String licencia) {
        return new BusinessException("Ya existe un chofer con el número de licencia '" + licencia + "'");
    }

    // =========================================================================
    // Enzona (cliente HTTP)
    // =========================================================================

    public static BusinessException enzonaErrorToken(String statusCode) {
        return new BusinessException("Error al obtener el token de autenticación de Enzona. Código HTTP: " + statusCode);
    }

    public static BusinessException enzonaTimeoutConexion() {
        return new BusinessException("No se pudo conectar con el servicio de pagos de Enzona. Tiempo de espera agotado");
    }

    public static BusinessException enzonaErrorConexion() {
        return new BusinessException("No se pudo conectar con el servicio de pagos de Enzona. Verifique la conectividad de red");
    }

    public static BusinessException enzonaErrorCrearQR(String statusCode) {
        return new BusinessException("Error al crear el código QR en Enzona. Código HTTP: " + statusCode);
    }

    public static BusinessException enzonaErrorGenerarQR() {
        return new BusinessException("No se pudo generar el código QR de pago. Intente nuevamente en unos minutos");
    }

    // =========================================================================
    // Reportes (validaciones adicionales)
    // =========================================================================

    public static BusinessException anioInvalido(int anio) {
        return new BusinessException("El año debe ser un valor válido. Se recibió: " + anio);
    }
}
