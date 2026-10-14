# Plan de implementación: mapa interactivo con GPS

## Objetivo

Reemplazar los mapas ilustrados por un mapa real que permita mover y ampliar la vista, consultar reportes, centrar la cámara en la ubicación actual y elegir las coordenadas de un nuevo reporte.

Este trabajo corresponde únicamente al frontend Android. Cualquier corrección del backend debe ser realizada o aprobada por Juan/Mauro.

## Resultado de implementación

El 7 de octubre de 2026 se integró MapLibre Native con el estilo Liberty de OpenFreeMap en el mapa principal y en el selector del formulario. Se agregó selección por toque, ubicación actual con permiso en contexto, estados de error y filtrado de coordenadas inválidas. La compilación y 11 pruebas unitarias finalizaron correctamente; falta validar los gestos y el GPS en el Redmi. El backend `cb698f8` ya devuelve las coordenadas persistentes correctas, por lo que los marcadores remotos quedaron desbloqueados.

## Estado al iniciar el plan

- `MapScreen` usa `StitchMiniMap`, una ilustración sin teselas ni gestos geográficos.
- `LocationPickerScreen` usa `SimulatedMap` y tres ubicaciones fijas de demostración.
- **Usar mi ubicación** no consulta el GPS; solo muestra un aviso.
- `MapPoint` contiene coordenadas reales y también valores artificiales `x`/`y` usados para dibujar la ilustración.
- El manifiesto solo declara acceso a Internet; todavía no declara ubicación aproximada o precisa.
- El backend ya crea reportes, pero `GET /api/v1/reports` devuelve sus coordenadas como `0.0, 0.0`. PostgreSQL sí conserva el punto correcto.

## Decisión del proveedor de mapas

**Decisión:** usar **OpenFreeMap con MapLibre Native para Android**, el mismo origen cartográfico que utiliza la web.

OpenFreeMap publica un estilo compatible con MapLibre para aplicaciones móviles:

```text
https://tiles.openfreemap.org/styles/liberty
```

La instancia pública no requiere registro ni API key y permite uso comercial. Se debe conservar la atribución de OpenStreetMap/OpenMapTiles; MapLibre la muestra automáticamente. OpenFreeMap no ofrece garantía de disponibilidad ni soporte personalizado, por lo que la aplicación debe contemplar un estado de error si las teselas no cargan.

### Integración Android elegida

Usar el SDK estable de **MapLibre Native Android** dentro de un `AndroidView` controlado por Compose. No basar la primera versión en MapLibre Compose porque su API todavía se declara inestable y puede cambiar entre versiones menores.

La clase `InteractiveMap` debe encapsular por completo `MapView`, su ciclo de vida y los objetos propios del SDK. De este modo, el resto de la aplicación recibe y entrega modelos propios (`MapPoint`, reportes y eventos) y no queda acoplado a MapLibre.

Antes de migrar las dos pantallas se hará una prueba técnica pequeña en el Redmi para confirmar:

- carga del estilo Liberty en Mendoza;
- funcionamiento de desplazamiento y zoom;
- estabilidad al navegar fuera de la pantalla y volver;
- consumo de memoria aceptable;
- atribución visible;
- mensaje controlado cuando no hay Internet.

## Arquitectura propuesta

Mantener el flujo actual `UI → ViewModel → repositorio/servicio`:

```text
MapScreen / LocationPickerScreen
              ↓ eventos y estado
          AppViewModel
              ↓
        LocationProvider
              ↓
 FusedLocationProviderClient
```

### Componentes nuevos

- `LocationProvider`: interfaz propia para pedir una única ubicación actual.
- `AndroidLocationProvider`: implementación con `FusedLocationProviderClient.getCurrentLocation()`.
- `LocationUiState`: estado sin solicitar, solicitando permiso, buscando ubicación, disponible, permiso rechazado, GPS desactivado o error.
- `InteractiveMap`: componente Compose reutilizable que encapsula el `MapView` de MapLibre Native.
- `MapViewLifecycle`: adaptación del ciclo de vida para crear, iniciar, pausar, detener y destruir el mapa junto con la pantalla Compose.
- Constante de configuración para el estilo de OpenFreeMap; no se necesita API key.

La latitud y longitud deben convertirse en la única fuente geográfica real. Los campos artificiales `x` e `y` de `MapPoint` se mantendrán solo durante la migración y se eliminarán cuando ya no exista ningún mapa ilustrado.

## Comportamiento previsto

### Mapa principal

- Abrir inicialmente centrado en el área urbana de Mendoza.
- Permitir desplazamiento, zoom, rotación y gestos táctiles.
- Crear un marcador por cada reporte con coordenadas válidas.
- Diferenciar visualmente los estados pendiente, en proceso y resuelto.
- Al tocar un marcador, seleccionar el reporte y mostrar su resumen actual.
- Mantener visible el mapa aunque la lista esté vacía.
- El botón **Centrar mapa** solicita permiso en contexto y mueve la cámara a la ubicación del teléfono.
- No solicitar ni declarar ubicación en segundo plano.

### Selector de ubicación del reporte

- Abrir centrado en el punto ya guardado en el borrador.
- Si todavía no existe una selección del usuario, comenzar en Mendoza sin afirmar que esa sea su ubicación.
- Permitir tocar el mapa o arrastrar un marcador para elegir el punto.
- **Usar mi ubicación** solicita permiso y coloca el marcador en la posición obtenida por GPS.
- Mantener un campo de referencia textual editable; no depender de geocodificación para poder enviar el reporte.
- Confirmar solamente coordenadas finitas y dentro del rango admitido por el backend.
- Conservar el punto anterior si el usuario cancela.

### Permisos y GPS

- Declarar `ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION`.
- Solicitar ambos permisos únicamente después de pulsar una acción que necesite ubicación.
- Aceptar ubicación aproximada; la selección manual siempre debe permanecer disponible.
- Si el permiso fue rechazado, explicar por qué se solicita y permitir continuar manualmente.
- Si el usuario marcó **No volver a preguntar**, ofrecer abrir los ajustes de la aplicación.
- Si la ubicación del dispositivo está apagada, ofrecer activar el servicio mediante el flujo de ajustes cuando sea posible.
- Solicitar una sola ubicación fresca y finalizar; no mantener actualizaciones continuas que consuman batería.

## Etapas de implementación

### 0. Resolver la dependencia del backend — completado

Juan/Mauro corrigieron la conversión de `location` en `cb698f8`. Android no inventa ni reemplaza coordenadas `0.0, 0.0`; el backend local actualizado conserva y devuelve los valores reales después de reiniciar.

La integración del selector GPS avanzó en paralelo; los marcadores remotos quedaron habilitados cuando el backend `cb698f8` comenzó a devolver el punto correcto.

### 1. Preparar el proveedor del mapa

- Incorporar MapLibre Native Android mediante el catálogo de versiones.
- Crear el adaptador `InteractiveMap` con `AndroidView` y ciclo de vida seguro.
- Cargar el estilo Liberty desde OpenFreeMap.
- Mostrar un mapa real centrado en Mendoza, todavía sin GPS ni marcadores.
- Ejecutar la prueba técnica en el Redmi antes de migrar las pantallas completas.

### 2. Crear la capa de ubicación

- Implementar `LocationProvider` y un fake para pruebas.
- Agregar permisos al manifiesto.
- Modelar todos los estados de permiso y ubicación.
- Integrar la solicitud en contexto desde Compose.

### 3. Migrar el mapa principal

- Reemplazar `StitchMiniMap` por `InteractiveMap`.
- Convertir reportes en marcadores por latitud y longitud.
- Conservar selección, tarjeta de resumen, filtros y botón para reportar.
- Implementar el centrado GPS sin bloquear el uso manual del mapa.

### 4. Migrar el selector del formulario

- Eliminar `SimulatedMap` y las ubicaciones fijas de demostración.
- Agregar marcador seleccionable o arrastrable.
- Conectar **Usar mi ubicación** al proveedor real.
- Guardar el punto confirmado en `ReportDraft` sin perder el resto del formulario.

### 5. Limpiar el modelo de demostración

- Eliminar los campos `x`/`y` cuando ya no tengan consumidores.
- Eliminar textos y opciones que indiquen mapa o GPS simulado.
- Mantener datos demo solamente dentro de previews y fakes de pruebas.

### 6. Validar sin emulador

- Ejecutar pruebas unitarias, Lint y compilación con memoria limitada.
- Instalar el APK en el Redmi mediante Zorin Connect.
- Realizar las pruebas GPS directamente en el teléfono.

## Estrategia de pruebas

### Unitarias

- Ubicación disponible, aproximada, nula y con error.
- Permiso concedido, rechazado y rechazado permanentemente.
- El punto elegido se conserva al volver al formulario.
- Un error de GPS no modifica el punto anterior ni el resto del borrador.
- Los reportes sin coordenadas válidas no crean marcadores engañosos.

### UI con componentes falsos

- Mapa vacío visible.
- Marcadores seleccionables.
- Estados de permiso y GPS con acciones claras.
- Selección manual disponible sin permiso.
- Confirmar y cancelar producen el estado esperado.

### Pruebas manuales en el Redmi

1. GPS activo y permiso preciso.
2. Permiso aproximado.
3. Permiso rechazado una vez.
4. Permiso rechazado permanentemente.
5. GPS apagado.
6. Sin conexión a Internet.
7. Selección manual lejos de la ubicación actual.
8. Creación del reporte y verificación de su marcador después de reiniciar la aplicación.

## Definición de terminado

- Los dos mapas simulados fueron reemplazados.
- El mapa responde a gestos y conserva el diseño visual de la aplicación.
- El GPS se solicita solo por acción del usuario y nunca en segundo plano.
- El formulario admite ubicación GPS o manual.
- Los marcadores remotos usan coordenadas reales del backend.
- Se conserva la atribución exigida por OpenStreetMap/OpenMapTiles.
- Pruebas, Lint y APK finalizan correctamente.
- El recorrido completo funciona en el Redmi sin usar emulador.
