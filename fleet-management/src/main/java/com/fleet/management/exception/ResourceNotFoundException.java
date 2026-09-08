package com.fleet.management.exception;

import java.util.Map;

/**
 * Excepción lanzada cuando un recurso no se encuentra en la base de datos.
 *
 * <p>El constructor {@code (resourceName, fieldName, fieldValue)} genera un
 * mensaje coherente y traducido al español, centralizando el formato para
 * toda la aplicación.
 *
 * <p>Para nuevos código, preferir usar {@link ResourceError} con sus métodos
 * factory específicos (ej: {@code ResourceError.usuarioNoEncontradoPorId(id)}).
 */
public class ResourceNotFoundException extends RuntimeException {

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

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje coherente y traducido.
     *
     * <p>Formato: "No se encontró {entidad} con {campo} '{valor}'".
     *
     * @param resourceName nombre del recurso (ej: "User", "Empresa")
     * @param fieldName    nombre del campo (ej: "id", "email")
     * @param fieldValue   valor buscado
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(formatMessage(resourceName, fieldName, fieldValue));
    }

    private static String formatMessage(String resourceName, String fieldName, Object fieldValue) {
        String entidad = RESOURCE_NAMES.getOrDefault(resourceName, resourceName);
        String campo = FIELD_NAMES.getOrDefault(fieldName, fieldName);
        return String.format("No se encontró %s con %s: '%s'", entidad, campo, fieldValue);
    }
}
