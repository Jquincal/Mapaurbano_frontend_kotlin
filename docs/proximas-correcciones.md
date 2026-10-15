# Próximas correcciones

Registro realizado durante la prueba en un Redmi Note 13 Pro 5G contra el backend local del 7 de octubre de 2026.

## 1. Mantener visible el mapa cuando no hay reportes

**Estado:** corregido en Android.

Cuando `GET /api/v1/reports` responde correctamente con `{"items":[]}`, `MapScreen` reemplaza el mapa por una tarjeta de estado vacío. El comportamiento esperado es mantener visible `StitchMiniMap`, sin marcadores, y mostrar el mensaje de estado vacío junto con la acción **Reportar problema**. Los filtros vacíos tampoco deben ocultar el mapa.

**Criterios de aceptación:**

- El mapa interactivo se muestra con cero reportes.
- La vista vacía explica que todavía no hay incidencias registradas.
- La acción para crear el primer reporte permanece visible.
- Aplicar un filtro sin coincidencias no desmonta el mapa.

## 2. Corregir la persistencia de reportes en el backend

**Estado:** corregido e integrado. La escritura llegó en `2c93883` y la lectura persistente de coordenadas en `cb698f8`.

**Verificación local:** `POST /api/v1/reports` respondió HTTP 201. PostgreSQL conservó `status=pending`, `priority=medium`, latitud `-32.8895` y longitud `-68.8458`.

Antes de la corrección, el formulario Android enviaba correctamente `POST /api/v1/reports`, pero PostgreSQL rechazaba el `INSERT` con HTTP 500:

```text
null value in column "status" of relation "reports" violates not-null constraint
```

La fila fallida también contenía `priority=NULL` y `location=NULL`. La causa estaba en los tipos personalizados de Exposed de `CustomTypes.kt`: `notNullValueToDB` convertía los valores a `PGobject`, pero `setParameter` esperaba nuevamente `ReportStatus`, `ReportPriority` o `GeoPoint`.

**Corrección aplicada en backend:**

- Hacer que `PgEnum.setParameter` acepte y reutilice un `PGobject`, además de admitir el enum original.
- Hacer que `GeographyPointColumnType.setParameter` acepte y reutilice un `PGobject`, además de admitir `GeoPoint`.
- Agregar una prueba de integración con PostgreSQL/PostGIS que cree un reporte autenticado y compruebe `status`, `priority`, `location` y el usuario propietario persistidos.
- Traducir cualquier fallo de persistencia a un error API controlado; no devolver un 500 genérico sin un mensaje útil para el cliente.

**Evidencia anterior:** categorías y listado vacío respondían HTTP 200; los intentos de creación finalizaban como HTTP 500 por la misma restricción.

### Corrección de Juan y Mauro integrada: coordenadas `0.0, 0.0`

**Resultado:** resuelto en backend mediante `cb698f8` (`fix: decode persisted report coordinates`). Android conserva el mismo contrato REST y no necesitó adaptaciones adicionales.

**Síntoma confirmado:** después de reiniciar el backend, los reportes aparecen en la API como `latitude=0.0` y `longitude=0.0`. Esto impide colocar sus marcadores en el mapa y hace parecer que las coordenadas se perdieron.

**Evidencia del 7 de octubre de 2026:** una consulta directa y de solo lectura a PostgreSQL, realizada después del reinicio, mostró que las cinco filas conservan sus coordenadas reales. Por ejemplo, `Prueba ubicación/coordenadas` permanece en `-32.9113169, -68.8457851` y `Un pozo` en `-32.92843591716063, -68.8341889752306`. Por lo tanto, el `INSERT` y la persistencia en PostGIS funcionan; el defecto está en la lectura y transformación de la columna `location`.

**Causa localizada:** `GeographyPointColumnType.valueFromDB()` solo interpreta valores de texto que comienzan con `POINT`. PostgreSQL/Exposed devuelve normalmente esa columna como EWKB hexadecimal. Para cualquier formato distinto de `POINT`, el código actual ejecuta silenciosamente `return GeoPoint(0.0, 0.0)`. Luego `ReportRepositoryImpl.toReport()` usa ese valor falso para construir las respuestas REST.

**Cambios aplicados en backend:**

- Corregir `GeographyPointColumnType.valueFromDB()` para decodificar correctamente EWKB/WKB, o proyectar explícitamente `ST_Y(location::geometry)` y `ST_X(location::geometry)` en todas las consultas que reconstruyen un reporte.
- Aplicar la solución a listado público, detalle y reportes del usuario; no solamente a una ruta.
- Eliminar el fallback silencioso `GeoPoint(0.0, 0.0)`. Un formato desconocido debe producir un error controlado y visible en pruebas, nunca coordenadas válidas inventadas.
- Mantener el orden correcto: `latitude = ST_Y(...)` y `longitude = ST_X(...)`.
- Agregar una prueba de integración que cree un reporte con coordenadas no nulas, cierre la conexión o reconstruya el repositorio como en un reinicio y compruebe que las rutas REST devuelven exactamente los mismos valores.
- Verificar en la prueba tanto PostgreSQL como la respuesta JSON de `GET /api/v1/reports`, `GET /api/v1/reports/{id}` y reportes del usuario.
- Revisar también el listado sin límites enviados por el cliente: el sobre mundial `-180..180` convertido a `geography` actualmente puede provocar el error PostGIS `Antipodal (180 degrees long) edge detected`.

**Verificación posterior a la integración:** las pruebas nuevas de decodificación, límites y respuesta de coordenadas finalizaron correctamente. Después de reiniciar el servidor, `GET /api/v1/reports` devolvió los cinco puntos reales y coincidió con `ST_Y/ST_X` en PostgreSQL. El listado sin `bbox` también respondió HTTP 200. Android mantiene la validación que evita dibujar cualquier coordenada inválida, pero ya recibe los valores reales del backend.

## 3. Mejorar el mensaje de error del frontend

**Estado:** corregido en Android.

La aplicación no debe presentar todos los fallos del servidor como problemas de conexión. Debe distinguir al menos:

- sin red o servidor inaccesible;
- validación HTTP 400;
- sesión vencida HTTP 401;
- conflicto HTTP 409;
- límite de solicitudes HTTP 429;
- error interno HTTP 500.

El borrador debe conservarse en todos los fallos recuperables.

La interfaz ahora distingue errores de red, validación, sesión, conflicto, límite de solicitudes y fallos internos. Tanto el mapa como **Mis reportes** muestran el motivo correspondiente y permiten reintentar. El formulario solo limpia el borrador después de una creación exitosa.

## 4. Reemplazar el mapa ilustrado por un mapa interactivo con GPS

**Estado:** implementado en Android y desbloqueado por la corrección de coordenadas del backend; pendiente de validación manual final en el Redmi.

**Plan detallado:** [plan-punto-4-mapa-interactivo.md](plan-punto-4-mapa-interactivo.md).

El reporte ya se crea y se guarda en el backend. `StitchMiniMap` y `SimulatedMap` fueron reemplazados por un mapa real de OpenFreeMap/MapLibre que admite gestos, selección manual y centrado mediante el GPS del teléfono.

**Decisión técnica:** usar OpenFreeMap con MapLibre Native para Android, compartiendo el mismo origen cartográfico que la web. OpenFreeMap no requiere API key; se debe mantener la atribución y contemplar el caso en que su instancia pública no esté disponible.

**Alcance:**

- Mostrar un mapa real que permita desplazamiento, zoom y gestos táctiles.
- Pedir ubicación precisa o aproximada solamente cuando el usuario pulse **Centrar mapa** o elija su ubicación; no se necesita ubicación en segundo plano.
- Centrar el mapa en la ubicación actual cuando el GPS y el permiso estén disponibles.
- Mostrar claramente qué hacer cuando el permiso sea rechazado o la ubicación del teléfono esté desactivada.
- Permitir elegir manualmente el punto del reporte tocando o moviendo un marcador en el mapa.
- Confirmar la latitud y longitud seleccionadas antes de volver al formulario.
- Mostrar los reportes del backend como marcadores y abrir su resumen al tocarlos.
- Mantener el mapa utilizable cuando no existan reportes y diferenciar los estados de carga, ubicación y error.
- Usar las coordenadas persistentes devueltas por el backend corregido en `cb698f8`.

**Verificación automática:** compilación Android correcta, 11 pruebas unitarias sin fallos y APK debug generado. Los puntos `0.0, 0.0` se omiten para no mostrar marcadores engañosos.

**Criterios de aceptación:**

- En el Redmi se puede mover el mapa y hacer zoom con gestos.
- El botón de ubicación centra el mapa en la posición real del teléfono con el GPS activo.
- Se puede crear un reporte seleccionando un punto distinto de la ubicación actual.
- Al reabrir la aplicación, cada reporte aparece en sus coordenadas reales.
- Rechazar el permiso o apagar el GPS no bloquea el formulario: permanece disponible la selección manual.

## 5. Cargar fotografías reales en los reportes

**Estado:** pendiente en Android y sujeto al contrato de imágenes del backend.

La opción de fotografía actual solo simula que existe una imagen. Debe reemplazarse por captura y selección de archivos reales, y enviar los bytes al backend junto con el reporte.

**Alcance:**

- Permitir tomar una fotografía real con la cámara y elegir una imagen mediante el selector de fotos del sistema.
- Usar URI de contenido y almacenamiento temporal seguro; no depender de rutas absolutas del dispositivo.
- Mostrar una vista previa real, permitir reemplazarla o quitarla y conservarla en el borrador mientras la aplicación siga abierta.
- Validar formato, tamaño y dimensiones, y comprimir la imagen antes de enviarla cuando sea necesario.
- Acordar con Juan/Mauro el nombre del campo multipart, formatos admitidos, tamaño máximo y respuesta del backend.
- Enviar la imagen real mediante la API REST y mostrar progreso y errores sin borrar el resto del borrador.
- Eliminar la simulación `hasPhoto` cuando la carga real esté integrada.
- Solicitar el permiso de cámara en contexto; el selector de fotos no debe pedir acceso general a toda la galería.

**Criterios de aceptación:**

- La vista previa coincide con la fotografía tomada o seleccionada.
- El reporte creado conserva la fotografía después de cerrar y volver a abrir la aplicación.
- Una imagen inválida o demasiado grande muestra una explicación y no elimina el texto, categoría ni ubicación del reporte.
- La fotografía enviada puede recuperarse desde el detalle del reporte usando la API REST.

## 6. Evitar que los gestos del mapa desplacen toda la interfaz

**Estado:** corregido en Android; pendiente de validación manual en el Redmi.

Cuando el usuario toca el mapa, éste debe capturar los gestos de desplazamiento y zoom sin mover el contenido vertical de la pantalla. El desplazamiento del resto de la interfaz queda temporalmente bloqueado mientras el mapa está activo y vuelve a habilitarse al tocar fuera del mapa.

**Criterios de aceptación:**

- Arrastrar un dedo dentro del mapa mueve únicamente el mapa.
- El gesto de pinza amplía o reduce el mapa sin desplazar la pantalla.
- Al tocar fuera del mapa se recupera inmediatamente el desplazamiento vertical normal.
- El comportamiento es el mismo en el mapa principal y en el selector de ubicación del formulario.
