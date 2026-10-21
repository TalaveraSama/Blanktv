# BlankTV

App de IPTV para **Android TV** (y pantallas táctiles) con **el diseño de OwnTV** y nombre propio.

El motor y la app vienen de [ahXN00/OwnTV_Core](https://github.com/ahXN00/OwnTV_Core) (`core-1.0.68`)
y [ahXN00/OwnTV](https://github.com/ahXN00/OwnTV) (commit `cead977`), bajo GPL-3.0. Aquí están los
tres módulos en un solo repositorio, con la marca cambiada: misma estructura de pantallas, mismo
comportamiento, mismo diseño. Ver [`NOTICE.md`](NOTICE.md) para la procedencia exacta y
[`docs/BRANDING.md`](docs/BRANDING.md) para qué se renombró y qué se dejó igual a propósito.

> ⚠️ Igual que upstream: aquí no hay canales, listas, suscripciones ni contenido. Es un reproductor;
> las fuentes las pone cada usuario.

## Módulos

| Módulo | Namespace | Qué es |
| --- | --- | --- |
| `:app` | `tv.own.owntv` | La app: Compose for TV, navegación, todas las pantallas, HUD del reproductor, ajustes, guía e inicio. |
| `:player-core` | `tv.own.owntv.playercore` | Motor de reproducción: libmpv + Media3/ExoPlayer, escalera de fallback, watchdogs, diagnóstico. |
| `:core` | `tv.own.owntv.core` | Room y migraciones, sincronización, parsers M3U/Xtream/Stalker, EPG, backup, perfiles, descargas, ajustes, metadatos y **todos los textos**. |
| `:baselineprofile` | — | Módulo de test que graba el arranque en frío y genera el perfil que empaqueta `:app`. No se distribuye. |

`:app` compila el motor **desde el código fuente de este repo** (`implementation(project(":core"))`),
no desde un artefacto publicado: no hay versión que se pueda quedar atrás. Solo libmpv, el motor
nativo mpv/FFmpeg, se resuelve como dependencia binaria.

## Identidad

| Dato | Valor |
| --- | --- |
| Nombre | BlankTV |
| `applicationId` | `tv.blanktv` (se instala junto a OwnTV, no lo actualiza) |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 |
| Sabores de ABI | `standard` (arm64-v8a, armeabi-v7a) y `x86_64` (emulador) |

## Compilar

Requiere **JDK 21** y **Android SDK** (platform 37). Indica la ruta del SDK:

```properties
# local.properties
sdk.dir=/ruta/a/Android/Sdk
```

```bash
./gradlew :app:assembleStandardDebug          # APK de depuración para TV / caja arm
./gradlew :app:assembleX86_64Debug            # APK para emulador x86_64
./gradlew :app:assembleStandardRelease        # release (sin firmar si no hay keystore, ver abajo)
./gradlew build                               # todo, incluidos tests y lint
```

Firma de release: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD` desde el entorno,
o un `~/.gradle/blanktv-signing.properties` fuera del repo:

```properties
blanktv.keystoreFile=/ruta/a/blanktv.keystore
blanktv.keystorePassword=…
blanktv.keyAlias=…
blanktv.keyPassword=…
```

Sin nada configurado, el build funciona igual y el APK sale sin firmar.

## Verificación sin Android SDK

La puerta de i18n y de marca se puede correr con solo Python:

```bash
python3 tools/i18n/validate_strings.py
python3 tools/i18n/check_hardcoded_strings.py verify --bootstrap
python3 tools/i18n/gen_supported_locales.py check
python3 tools/i18n/check_number_locale.py
python3 tools/i18n/check_text_overflow.py
python3 tools/i18n/test_i18n_tools.py
python3 tools/brand/rebrand_strings.py --check
```

Los tests instrumentados nunca se ejecutan contra una TV real con datos: instalar el APK de test borra
catálogo, listas, perfiles e historial.

## Lo que BlankTV todavía comparte con OwnTV

BlankTV no tiene backend propio. Los metadatos TMDB, la búsqueda de subtítulos y el enlace de
traducciones siguen apuntando a servicios de OwnTV (con su cuota compartida). El actualizador
integrado **sí** está redirigido a este repositorio, así que la app nunca instalará una release de
OwnTV. El detalle y cómo sustituir cada servicio está en [`docs/FORK.md`](docs/FORK.md).

## Traducciones

Las cadenas son las de OwnTV, traducidas por su comunidad en Hosted Weblate. BlankTV no tiene un
proyecto de traducción propio. El bloque siguiente lo genera `tools/i18n/gen_supported_locales.py` y
no debe editarse a mano.

<!-- i18n-contribution:start -->
## Help translate OwnTV

If your language is already available, contribute interface translations across OwnTV's six Android resource components on [Hosted Weblate](https://hosted.weblate.org/projects/owntv/). If it is not listed, [open a language request ticket](https://github.com/ahXN00/OwnTV/issues/new?template=feature_request.yml&title=%5BLanguage%5D%20Add%20) first. A maintainer will review the request, register the locale, and prepare its base translation files on Hosted Weblate. Once the language appears on Hosted Weblate, you can start translating it there. See the [language contributor guide](tools/i18n/README.md) for identifiers, validation, and promotion policy.
<!-- i18n-contribution:end -->

## Estructura

```
app/            la app (pantallas, HUD del reproductor, ajustes, guía, inicio)
player-core/    motor de reproducción (mpv + ExoPlayer, fallback, watchdogs)
core/           datos, sync, parsers, EPG, backup, ajustes y todos los textos
core/schemas/   esquemas de Room (historial de migraciones)
baselineprofile/  grabación del arranque para el perfil de compilación
tools/i18n/     validadores y utilidades de traducción
tools/brand/    generadores de la marca (texto, wordmark, iconos, banners)
docs/           procedencia, marca y estructura del repo
```

## Créditos

Todo el diseño y el código son de **OwnTV** y sus colaboradores: ver `NOTICE.md`.

- **Weblate** aloja las traducciones del proyecto gratis por ser software libre.
- **TMDB** aporta metadatos y tráilers. Este producto usa la API de TMDB pero no está respaldado ni
  certificado por TMDB.
- **OpenSubtitles** da soporte a la búsqueda y descarga de subtítulos.
- **libmpv / mpv (FFmpeg)**, construido para OwnTV desde
  [jarnedemeulemeester/libmpv-android](https://github.com/jarnedemeulemeester/libmpv-android), y
  **Media3 / ExoPlayer**.
- **Room · Koin · OkHttp · Kotlin Coroutines · WorkManager · Coil** y el resto del ecosistema open
  source de Kotlin / AndroidX.

## Licencia

**GNU General Public License v3.0** — ver [`LICENSE`](LICENSE) y [`NOTICE.md`](NOTICE.md).

Puedes usar, estudiar, modificar y redistribuir este código, incluso comercialmente, pero cualquier
versión redistribuida debe seguir bajo GPLv3 y su código fuente debe estar disponible.
