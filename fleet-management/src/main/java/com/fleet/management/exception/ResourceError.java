package com.fleet.management.exception;

import java.util.Map;

/**
 * Mensajes de error de recurso no encontrado estandarizados para la aplicación.
 *
 * <p>Centraliza y traduce los mensajes de {@link ResourceNotFoundException} para
 * garantizar consistencia: mismo formato, nombres de entidades en español, y
 * formato coherente con {@link BusinessError}.
 *
 * <p>Convención de mensajes:
 * <ul>
 *   <li>Nombre de entidad en español singular con artículo: "el usuario", "la empresa".</li>
 *   <li>Formato: "No se encontró {entidad} con {campo} '{valor}'".</li>
 *   <li>Valor entre comillas simples para facilitar identificación.</li>
 * </ul>
 */
public final class ResourceError {

    private ResourceError() {
    }

    /**
     * Mapa de nombres de recurso (inglés/técnico) a nombre legible en español.
     */
    private static final Map<String, String> RESOURCE_NAMES = Map.ofEntries(
            Map.entry("User", "el usuario"),
            Map.entry("Role", "el rol"),
            Map.entry("Permission", "el permiso"),
            Map.entry("Empresa", "la empresa"),
            Map.entry("Vehiculo", "el vehículo"),
            Map.entry("Chofer", "el chofer"),
            Map.entry("ChoferCategoria", "la categoría del chofer"),
            Map.entry("CategoriaLicencia", "la categoría de licencia"),
            Map.entry("TipoVehiculo", "el tipo de vehículo"),
            Map.entry("Marca", "la marca"),
            Map.entry("TipoCombustible", "el tipo de combustible"),
            Map.entry("Recorrido", "el recorrido"),
            Map.entry("TarjetaCombustible", "la tarjeta de combustible"),
            Map.entry("Currency", "la moneda"),
            Map.entry("Subscription", "la suscripción"),
            Map.entry("Plan", "el plan"),
            Map.entry("Payment", "el pago"),
            Map.entry("Feature", "el feature"),
            Map.entry("Provincia", "la provincia"),
            Map.entry("Municipio", "el municipio")
    );

    /**
     * Mapa de nombres de campo (inglés/técnico) a nombre legible en español.
     */
    private static final Map<String, String> FIELD_NAMES = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("email", "email"),
            Map.entry("name", "nombre"),
            Map.entry("codigo", "código"),
            Map.entry("isoCode", "código ISO"),
            Map.entry("numero", "número"),
            Map.entry("matricula", "matrícula"),
            Map.entry("nombre", "nombre"),
            Map.entry("empresaId", "id de empresa"),
            Map.entry("planId", "id de plan")
    );

    /**
     * Crea un ResourceNotFoundException con mensaje estandarizado.
     *
     * <p>Formato: "No se encontró {entidad} con {campo} '{valor}'".
     *
     * @param resourceName nombre del recurso (ej: "User", "Empresa")
     * @param fieldName     nombre del campo (ej: "id", "email")
     * @param fieldValue    valor buscado
     * @return ResourceNotFoundException con mensaje coherente
     */
    public static ResourceNotFoundException notFound(String resourceName, String fieldName, Object fieldValue) {
        String entidad = RESOURCE_NAMES.getOrDefault(resourceName, resourceName);
        String campo = FIELD_NAMES.getOrDefault(fieldName, fieldName);
        return new ResourceNotFoundException(
                String.format("No se encontró %s con %s: '%s'", entidad, campo, fieldValue));
    }

    // =========================================================================
    // Métodos factory específicos por entidad (más legibles en el código)
    // =========================================================================

    public static ResourceNotFoundException usuarioNoEncontradoPorId(Long id) {
        return notFound("User", "id", id);
    }

    public static ResourceNotFoundException usuarioNoEncontradoPorEmail(String email) {
        return notFound("User", "email", email);
    }

    public static ResourceNotFoundException empresaNoEncontradaPorId(Long id) {
        return notFound("Empresa", "id", id);
    }

    public static ResourceNotFoundException empresaNoEncontradaPorCodigo(String codigo) {
        return notFound("Empresa", "codigo", codigo);
    }

    public static ResourceNotFoundException vehiculoNoEncontradoPorId(Long id) {
        return notFound("Vehiculo", "id", id);
    }

    public static ResourceNotFoundException choferNoEncontradoPorId(Long id) {
        return notFound("Chofer", "id", id);
    }

    public static ResourceNotFoundException recorridoNoEncontradoPorId(Long id) {
        return notFound("Recorrido", "id", id);
    }

    public static ResourceNotFoundException tarjetaCombustibleNoEncontradaPorId(Long id) {
        return notFound("TarjetaCombustible", "id", id);
    }

    public static ResourceNotFoundException tarjetaCombustibleNoEncontradaPorNumero(String numero) {
        return notFound("TarjetaCombustible", "numero", numero);
    }

    public static ResourceNotFoundException subscriptionNoEncontradaPorId(Long id) {
        return notFound("Subscription", "id", id);
    }

    public static ResourceNotFoundException subscriptionNoEncontradaPorEmpresa(Long empresaId) {
        return notFound("Subscription", "empresaId", empresaId);
    }

    public static ResourceNotFoundException planNoEncontradoPorId(Long id) {
        return notFound("Plan", "id", id);
    }

    public static ResourceNotFoundException planNoEncontradoPorNombre(String nombre) {
        return notFound("Plan", "nombre", nombre);
    }

    public static ResourceNotFoundException paymentNoEncontradoPorId(Long id) {
        return notFound("Payment", "id", id);
    }

    public static ResourceNotFoundException paymentNoEncontradoPorEmpresa(Long empresaId) {
        return notFound("Payment", "empresaId", empresaId);
    }

    public static ResourceNotFoundException rolNoEncontradoPorId(Long id) {
        return notFound("Role", "id", id);
    }

    public static ResourceNotFoundException rolNoEncontradoPorNombre(String nombre) {
        return notFound("Role", "name", nombre);
    }

    public static ResourceNotFoundException permissionNoEncontradoPorId(Long id) {
        return notFound("Permission", "id", id);
    }

    public static ResourceNotFoundException permissionNoEncontradoPorNombre(String nombre) {
        return notFound("Permission", "name", nombre);
    }

    public static ResourceNotFoundException featureNoEncontradoPorId(Long id) {
        return notFound("Feature", "id", id);
    }

    public static ResourceNotFoundException marcaNoEncontradaPorId(Long id) {
        return notFound("Marca", "id", id);
    }

    public static ResourceNotFoundException tipoVehiculoNoEncontradoPorId(Long id) {
        return notFound("TipoVehiculo", "id", id);
    }

    public static ResourceNotFoundException tipoCombustibleNoEncontradoPorId(Long id) {
        return notFound("TipoCombustible", "id", id);
    }

    public static ResourceNotFoundException categoriaLicenciaNoEncontradaPorId(Long id) {
        return notFound("CategoriaLicencia", "id", id);
    }

    public static ResourceNotFoundException categoriaLicenciaNoEncontradaPorCodigo(String codigo) {
        return notFound("CategoriaLicencia", "codigo", codigo);
    }

    public static ResourceNotFoundException currencyNoEncontradaPorId(Long id) {
        return notFound("Currency", "id", id);
    }

    public static ResourceNotFoundException currencyNoEncontradaPorIsoCode(String isoCode) {
        return notFound("Currency", "isoCode", isoCode);
    }

    public static ResourceNotFoundException provinciaNoEncontradaPorId(Long id) {
        return notFound("Provincia", "id", id);
    }

    public static ResourceNotFoundException municipioNoEncontradoPorId(Long id) {
        return notFound("Municipio", "id", id);
    }

    public static ResourceNotFoundException choferCategoriaNoEncontradaPorId(Long id) {
        return notFound("ChoferCategoria", "id", id);
    }
}
