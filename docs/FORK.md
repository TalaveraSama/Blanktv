# BlankTV — cómo está montado este repositorio

BlankTV es una app de IPTV para Android TV (y pantallas táctiles) construida **sobre el motor de
OwnTV**. El diseño, la estructura de pantallas y la lógica son las de OwnTV; la marca es propia.
Este documento explica de dónde viene cada pieza, por qué están todas en un mismo repo, y qué
consecuencias tiene.

## De dónde viene cada cosa

| Parte | Origen | Versión | Licencia |
| --- | --- | --- | --- |
| `:app` | [ahXN00/OwnTV](https://github.com/ahXN00/OwnTV), commit `cead977` | v4.x, commit del 2026-10-17 | GPL-3.0 |
| `:core`, `:player-core` | [ahXN00/OwnTV_Core](https://github.com/ahXN00/OwnTV_Core), commit `c14e944` | `core-1.0.68` | GPL-3.0 |
| `tv.own.owntv:libmpv` | [ahXN00/OwnTV_libmpv](https://github.com/ahXN00/OwnTV_libmpv) | `2026.10.3` | MIT |

Los README originales están sin tocar en `docs/UPSTREAM_APP_README.md`, `docs/UPSTREAM_CORE_README.md`
y `docs/UPSTREAM_CONTRIBUTING.md`. Ver también `NOTICE.md`.

## Por qué un solo repositorio

Upstream son **tres repos**: la app pide `tv.own.owntv:core` y `:player-core` a un Maven propio, y un
release del core abre un pull request en cada app para subir el pin. Ese montaje tiene sentido cuando
hay dos apps (TV y móvil) consumiendo el mismo motor.

Aquí hay una sola app, así que la app compila el motor **desde el código fuente de este mismo repo**:

```kotlin
// app/build.gradle.kts
implementation(project(":core"))
implementation(project(":player-core"))
```

Consecuencias, todas buenas para una app única:

- No hay pin que se pueda quedar atrás, ni dos artefactos que puedan desincronizarse.
- Un cambio en el motor y el cambio de pantalla que lo usa se compilan juntos: si rompen, rompen aquí.
- Los tests del motor y los de la app se ejecutan en el mismo `./gradlew build`.
- Las herramientas de `tools/i18n/` validan las **dos** mitades del árbol a la vez, que es justo lo que
  sus scripts ya esperaban (`SRC_ROOTS` incluye `app/`, `core/` y `player-core/`).

Lo único que sigue viniendo de fuera es **libmpv**, el motor nativo (mpv + FFmpeg): es un `.aar`
grande, con código C++, y no se compila en este repo. Se resuelve desde el Maven público de OwnTV
declarado en `settings.gradle.kts`. Para reemplazarlo por un build propio, ver `docs/FORK.md` más
abajo.

## Identidad de la app

| Dato | Valor |
| --- | --- |
| `applicationId` | `tv.blanktv` |
| `namespace` | `tv.own.owntv` |
| Nombre visible | BlankTV (`app_name` en el motor, ver `docs/BRANDING.md`) |
| Repositorio de releases | `TalaveraSama/Blanktv` (`CoreBuildInfo.releaseRepo`) |

`applicationId` distinto es deliberado: BlankTV se instala **al lado** de OwnTV, no lo actualiza ni
lo puede degradar, y los dos no comparten datos.

El *namespace* y los paquetes Kotlin siguen siendo `tv.own.owntv.*`. Renombrarlos son ~1800 archivos
de `package`/`import` y una reescritura de todos los archivos de recursos sin ningún beneficio
funcional. La app es BlankTV por fuera; por dentro sigue hablando el idioma del motor. `docs/BRANDING.md`
explica qué se cambió y qué se dejó igual a propósito.

## Qué se comparte con upstream (y por qué importa)

BlankTV **no tiene backend propio**. Estas partes siguen apuntando a servicios de OwnTV:

| Servicio | Qué pasa si upstream lo cierra | Dónde se arregla |
| --- | --- | --- |
| Servicio de metadatos TMDB (Cloudflare Worker) | Los detalles de películas/series y el *trending* dejan de cargar; el resto de la app funciona | Clave TMDB propia (ajustes) o un Worker propio — ver `docs/FORK.md` |
| Proxy de OpenSubtitles | La búsqueda de subtítulos deja de funcionar | Cuenta de OpenSubtitles propia en ajustes |
| Proyecto de traducción (Hosted Weblate) | Las traducciones ya empaquetadas siguen ahí; solo se pierde el enlace para contribuir | Nada que arreglar: es informativo |
| Maven de OwnTV (`libmpv`) | La app no compila | Build propio de libmpv o un `.aar` vendorizado |

El **actualizador integrado** sí está redirigido: `BlanktvApp` declara `CoreBuildInfo.releaseRepo =
"TalaveraSama/Blanktv"`, así que la app solo ofrece sus propias releases y nunca instala un APK de
OwnTV encima.

## Actualizar desde upstream

Los tres árboles son copias, no submódulos: no hay merge automático. El procedimiento es el de
siempre, pero conviene hacerlo en este orden:

1. **Motor**: copiar `core/` y `player-core/` desde el release nuevo de OwnTV_Core, y su
   `gradle/libs.versions.toml` (comparando versiones de Room/Media3, que la app también usa).
2. Volver a aplicar la marca: `python3 tools/brand/rebrand_strings.py` y regenerar la tipografía
   (`tools/brand/wordmark_paths.py`, `render_brand.py`). El `--check` del primero falla si te olvidas.
3. **App**: copiar `app/` y `baselineprofile/` desde OwnTV, revisar el diff de `app/build.gradle.kts`
   (firma y flavors son los mismos: ver abajo) y volver a renombrar `OwnTVApp` → `BlanktvApp` y el
   `applicationId`.
4. Pasar la puerta completa antes de dar nada por bueno:

```bash
python3 tools/i18n/validate_strings.py
python3 tools/i18n/check_hardcoded_strings.py verify --bootstrap
python3 tools/i18n/gen_supported_locales.py check
python3 tools/i18n/check_number_locale.py
python3 tools/i18n/check_text_overflow.py
python3 tools/i18n/test_i18n_tools.py
python3 tools/brand/rebrand_strings.py --check
./gradlew :app:assembleDebug
```

Los archivos `tools/i18n/hardcoded_baseline.txt` y `safe_literals.txt` son la unión de los dos repos
de origen; tras un re-vendor, `prune-safe` los deja en su sitio.

## Diferencias de build respecto a upstream

- **Flavors de ABI conservados**: igual que upstream, `standard` (arm64-v8a + armeabi-v7a) y `x86_64`.
  No hay APK universal; para un emulador x86_64 se usa ese flavor. El `.so` de mpv/FFmpeg es la mayor
  parte del peso, así que un APK por ABI sigue siendo la forma de instalar en una TV de 8 GB sin
  descargas truncadas.
- **Sin firma de CI**: `KEYSTORE_FILE` y compañía se leen del entorno o de
  `~/.gradle/blanktv-signing.properties`. Sin nada configurado, el release sale sin firmar y compila
  igual.
- **Sin trabajos de publicación**: `.github/workflows/` trae `build.yml` e `i18n.yml` (CI de
  librería, sin tareas de release). No hay workflow que publique en el Maven de OwnTV ni que abra PRs
  en sus apps. Tampoco hay workflow que compile el APK: la primera compilación la lanzas tú.

## Pendiente conocido

- **No se ha compilado todavía** en este entorno: no hay JDK 21 ni Android SDK. Lo que sí está
  verificado es todo lo verificable sin ellos (ver la sección anterior y el resumen del commit).
- La auditoría estática de referencias cruzadas (imports de `:app` hacia símbolos del motor) sale
  limpia, pero eso no sustituye a un `assembleDebug`: hay diferencias entre los dos árboles de
  upstream que solo aparecen al compilar (por ejemplo, tests del toolkit que describen pantallas de
  una versión anterior). La primera compilación es la tarea inmediata.
