# Corrección de integración del 19/10/2026

Los tres hallazgos seguían presentes en `origin/main` (`4cfc824`): el cierre de sesión conservaba reportes, el cliente usaba lecturas públicas retiradas y el selector de fotografías era simulado. La integración anterior estaba publicada únicamente en la rama `codex/android-backend-actual` (`30d301a`).

Esta actualización reúne la integración y refuerza el cierre de sesión:

- La pantalla elimina usuario, reportes, imágenes, filtros, selección y navegación inmediatamente al pulsar cerrar sesión; no espera la red.
- El cliente elimina el token guardado antes de pedir la revocación al servidor. Una revocación fallida no restaura el acceso local; se limita la espera a 10 segundos y se bloquea un nuevo login hasta completar la limpieza.
- Las respuestas pendientes de la sesión anterior se descartan. Un 401 limpia el estado privado antes de esperar la eliminación del token.
- Mapa, listado, detalle e imágenes usan exclusivamente las rutas `/users/me/reports` y sus subrutas con Bearer. No hay lecturas ciudadanas públicas de reportes.
- Galería y cámara proporcionan bytes reales a la parte multipart `image`, con validación de MIME, tamaño y dimensiones.

Las pruebas agregadas verifican limpieza inmediata aun con el servidor demorado, finalización por tiempo máximo y eliminación del token antes del envío de logout, incluso con un HTTP 500. Se conservan las pruebas de cambio de cuenta, 401, respuestas tardías, rutas privadas y multipart.

La prueba física del APK sigue pendiente. Una transferencia mediante Zorin Connect confirma la entrega del archivo, no que el usuario haya instalado o recorrido esa versión. Los resultados locales y del CI deben comprobarse sobre la revisión publicada.
