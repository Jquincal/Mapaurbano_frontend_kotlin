# Estado de integración al 18/10/2026

Se incorporó el contrato de backend `907e0c3`: reportes propios, detalle e imagen privados, paginación, cámara/galería y limpieza de sesiones. Los problemas históricos se conservan en [historico-correcciones-previas.md](historico-correcciones-previas.md); no describen el estado vigente.

La validación automatizada se ejecuta con `./scripts/validate.sh`. Queda por completar la comprobación manual de esta versión en teléfono: crear con foto, abrir detalle, cerrar sesión y entrar con otra cuenta. La documentación no considera esa prueba aprobada hasta realizarla.

El panel Angular tiene su propio trabajo de adaptación a cookies, CSRF y DTO administrativos. El hecho de que la API administrativa funcione no confirma que la interfaz web esté completamente adaptada.
