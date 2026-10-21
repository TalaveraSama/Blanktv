# NOTICE — procedencia y licencia

## Origen del código

Este repositorio contiene una copia del código fuente de dos proyectos de OwnTV, importadas el
2026-10-21:

| Proyecto | Repositorio | Versión | Commit | Licencia |
| --- | --- | --- | --- | --- |
| OwnTV (app de TV) — `:app`, `:baselineprofile` | https://github.com/ahXN00/OwnTV | v4.x | `cead977803d90bd62ece69b4140e373f01feddca` (2026-10-17) | GPL-3.0 |
| OwnTV Core — `:core`, `:player-core` | https://github.com/ahXN00/OwnTV_Core | `core-1.0.68` | `c14e944b8d17e5cbb559d116afb30dc3b34864cb` (2026-10-17) | GPL-3.0 |

Autoría: ahXN00 y colaboradores de OwnTV (ver el historial de commits de cada repositorio original).

Los README originales se conservan sin cambios en `docs/UPSTREAM_APP_README.md`,
`docs/UPSTREAM_CORE_README.md` y `docs/UPSTREAM_CONTRIBUTING.md`; el historial de versiones heredado,
en `CHANGELOG.md`.

Además, el motor nativo `tv.own.owntv:libmpv` (versión `2026.10.3`) se resuelve como dependencia
binaria desde el repositorio Maven público de OwnTV, desde
[ahXN00/OwnTV_libmpv](https://github.com/ahXN00/OwnTV_libmpv), bajo licencia MIT (mpv/FFmpeg conservan
sus propias licencias). No se compila aquí.

## Qué se importó y qué no

Se copiaron los módulos `:core`, `:player-core`, `:app` y `:baselineprofile`, el catálogo de versiones,
el wrapper de Gradle, las herramientas de `tools/`, los assets de `extras/` y la documentación.

Del `.github/workflows/` del upstream solo se conservan `build.yml` e `i18n.yml` (CI de la librería).
**No** se importaron `publish.yml`, `release-core.yml`, `bump-consumers.yml` ni `translations.yml`:
esa automatización es específica de OwnTV —publica artefactos en su Maven, corta releases y abre pull
requests en sus apps— y no tiene sentido en este repositorio.

**Modificaciones respecto al upstream** (la GPLv3 exige señalarlas): la marca visible pasó de OwnTV a
BlankTV y el `applicationId` es `tv.blanktv`. El detalle completo, incluido qué se dejó igual a
propósito, está en `docs/BRANDING.md`; la estructura del repo, en `docs/FORK.md`.

## Obligaciones de la GPLv3

OwnTV Core está bajo GPLv3. En la práctica, para este repositorio y para cualquier app construida
sobre él:

- El código derivado debe seguir distribuyéndose bajo GPLv3.
- Quien reciba un binario debe poder obtener el código fuente correspondiente, incluidos los cambios
  propios.
- Deben conservarse los avisos de copyright y de licencia, y quedar claro qué se modificó y cuándo.

Nada de lo anterior convierte el proyecto en titular de los derechos de terceros, ni autoriza a
sugerir que el upstream respalda esta distribución.

## Marcas y contenido

OwnTV, su logotipo y los assets de `extras/brand/` pertenecen a su proyecto de origen. Los logotipos
de `extras/partner-logos/` (Weblate, TMDB, OpenSubtitles) pertenecen a sus respectivos titulares y se
usan solo como referencia de atribución.

Este repositorio no incluye ni distribuye canales, listas, suscripciones o contenido multimedia.
