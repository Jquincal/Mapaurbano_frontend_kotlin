# Validación local del 18/10/2026

Rama Android: `codex/android-backend-actual`, sobre `4cfc824`. Backend local: `907e0c3`, funcionalidad `34d219e`. No se publicaron cambios ni se ejecutó GitHub Actions remoto.

- `./scripts/validate.sh`: correcto. 21 pruebas, cero fallos y cero errores; Lint, assembleDebug y JaCoCo correctos.
- MockEngine verifica método, rutas privadas, Bearer, cursor opaco, bytes multipart `image` y limpieza del token en 401.
- ViewModel verifica acceso obligatorio, envío único, conservación de borrador por fallo de envío, limpieza ante 401 de lista/detalle/imagen/creación, respuesta tardía tras logout, cambio A/B, paginación sin duplicados y rechazo de adjunto inválido.
- Backend: `./gradlew test installDist --no-daemon`, 34 pruebas correctas. Servicio local con PostgreSQL/PostGIS nativo y migraciones Flyway, incluida V4. No se ejecutó Testcontainers porque Docker no está instalado.
- Prueba HTTP real del entorno: 41 comprobaciones correctas. Incluye dos cuentas, privacidad de detalle/imagen, PNG real, cursor, sesiones ciudadanas y administrativas, CSRF y cambio de estado visible desde el ciudadano.
- Comparación del OpenAPI con las rutas registradas: 36 operaciones sin diferencias de método/ruta.

APK: `app/build/outputs/apk/debug/app-debug.apk`, debug, backend `http://127.0.0.1:8080/api/v1/` (necesita túnel USB). SHA-256: `78feb94eaaed288ad306b1e6a0c33d967dd2425551cabe7ac58f5478e961f673`.

## Verificación física pendiente

ADB no detectó ningún dispositivo al cerrar esta validación. No se instaló este APK ni se acreditó cámara, galería, mapa o cambio de cuenta mediante la interfaz de esta versión. Conectar un teléfono con depuración USB y ejecutar `entorno-local/entorno.sh android` desde el monorepo permite instalarlo y activar el túnel.

Recorrido requerido: A inicia sesión, crea con fotografía, abre mapa/lista/detalle e historial; cierra sesión, B ingresa en el mismo dispositivo sin datos de A; un ID ajeno no es accesible; A vuelve a entrar y comprueba un cambio de estado realizado desde la API administrativa. La web Angular necesita su adaptación propia antes de acreditar ese cambio a través del panel.
