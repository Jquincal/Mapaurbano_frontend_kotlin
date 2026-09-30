# Mapa Urbano — aplicación Android

Aplicación ciudadana de Mapa Urbano, desarrollada con Kotlin y Jetpack Compose. Es uno de los tres repositorios del sistema:

- backend común: KTOR + API REST;
- frontend web: Angular;
- frontend mobile: este repositorio.

La marca visible en la aplicación es **MuniReport**.

## Estado inicial

La base incluye:

- proyecto Android con AGP 9, Kotlin, Compose Material 3 y Navigation 3;
- cuatro destinos principales: Mapa, Reportar, Mis reportes y Seguimiento;
- navegación con una pila independiente por sección;
- arquitectura inicial `UI → ViewModel → Repository`;
- repositorio falso para ejecutar y demostrar la app sin backend;
- edge-to-edge, soporte de teclado y tamaños táctiles Material 3;
- pruebas unitarias y de navegación Compose;
- workflow de CI para pruebas, lint y APK debug.

El mapa, login, cámara, persistencia local y llamadas REST todavía no están implementados. Los botones correspondientes explicitan ese límite y no simulan operaciones exitosas.

## Requisitos

- JDK 17;
- Android SDK 36;
- Android Studio compatible con AGP 9, o Android CLI;
- dispositivo/emulador Android 8.0 (API 26) o superior.

## Verificación

```bash
./scripts/validate.sh
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
La estrategia y los comandos individuales están en [`docs/testing.md`](docs/testing.md).

## Estructura

```text
app/src/main/java/com/mapaurbano/mobile/
├── data/          # Repositorios y fuentes de datos
├── model/         # Modelos usados por la aplicación
├── navigation/    # Rutas, estado y navegador Navigation 3
└── ui/            # Pantallas, componentes y tema
```

## Integración REST pendiente

La implementación remota deberá reemplazar `FakeReportRepository` sin acoplar las pantallas al cliente HTTP. El contrato esperado conserva el prefijo `/api/v1` y, como mínimo, contempla:

- `GET /reports` y `GET /reports/{id}`;
- `POST /reports`;
- `POST /report-status`;
- registro, login, sesión y `GET /users/me/reports`.

Antes de conectar estos servicios se debe publicar y versionar el contrato OpenAPI del backend Spring Boot.
