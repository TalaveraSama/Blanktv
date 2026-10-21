# NOTICE — procedencia y licencia

## Origen del código

Este repositorio contiene una copia del código fuente de **OwnTV Core**, importada el 2026-10-21:

| Dato | Valor |
| --- | --- |
| Proyecto | OwnTV Core |
| Repositorio | https://github.com/ahXN00/OwnTV_Core |
| Versión | `core-1.0.68` |
| Commit | `c14e944b8d17e5cbb559d116afb30dc3b34864cb` |
| Fecha del commit | 2026-10-17 |
| Autoría | ahXN00 y colaboradores de OwnTV (ver el historial de commits del repositorio original) |
| Licencia | GNU General Public License v3.0 — ver `LICENSE` |

El README original del proyecto se conserva sin cambios en `docs/UPSTREAM_CORE_README.md`, y el
historial de versiones heredado en `CHANGELOG.md`.

## Qué se importó y qué no

Se copiaron los módulos `:core` y `:player-core`, el catálogo de versiones, el wrapper de Gradle, las
herramientas de `tools/`, los assets de `extras/` y la documentación.

**No** se importó `.github/workflows/`: esa automatización es específica del upstream —publica los
artefactos en el repositorio Maven de OwnTV y abre pull requests en las apps de OwnTV— y no tiene
sentido en este repositorio.

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
