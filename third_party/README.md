# `third_party/` — código de terceros

Este directorio contiene código que **no es original de blankTV**. Se distribuye verbatim, bajo
su propia licencia, sin modificaciones.

## `owntv-core/` — OwnTV_Core (GPL-3.0)

| Campo | Valor |
|---|---|
| Upstream | <https://github.com/ahXN00/OwnTV_Core> |
| Commit fijado | `c14e944b8d17e5cbb559d116afb30dc3b34864cb` |
| Licencia | GNU GPL v3.0 (ver `owntv-core/LICENSE`) |
| Módulos | `:core` (database, red, parsers, EPG, sync, backup, strings) · `:player-core` (motores libmpv + ExoPlayer) |
| Tamaño | ~1.055 archivos, 583 Kotlin, ~100k LOC |

### Reglas

1. **Solo lectura.** No se edita ningún archivo dentro de `owntv-core/`. Si se necesita un
   comportamiento distinto, se reimplementa en `app/` o se propone upstream.
2. **No se compila.** El `settings.gradle.kts` de blankTV solo incluye `:app`; el árbol vendado es
   referencia de estudio y fuente de adaptación futura, no una dependencia de build.
3. **Atribución permanente.** Cualquier porción que se adapte a `app/` debe conservar el aviso de
   copyright de OwnTV y licenciarse GPL-3.0 (ver `NOTICE.md` en la raíz).
4. **Actualizaciones.** Para subir de versión: clonar upstream, tomar el commit deseado, reemplazar
   este directorio y actualizar la tabla de arriba y `NOTICE.md`.

### Por qué está aquí

OwnTV publica su motor como fuente GPL-3.0, así que no hace falta —ni procede— ingeniería inversa:
el código real es legible directamente. Vendarlo permite estudiar el motor (Room de 27 entidades,
parsers Xtream/Stalker, EPG, escalera de fallback de reproducción) y adaptar piezas puntuales con
cumplimiento GPL, en lugar de reconstruirlas a ciegas.
