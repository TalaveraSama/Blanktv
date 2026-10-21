# BlankTV

Repositorio de BlankTV, construido sobre el motor de **OwnTV Core**.

El código de este repositorio procede de [ahXN00/OwnTV_Core](https://github.com/ahXN00/OwnTV_Core)
(release `core-1.0.68`, commit `c14e944`): la mitad "sin pantallas" de OwnTV — base de datos,
sincronización, parsers, EPG, copias de seguridad, perfiles, descargas, ajustes, cadenas de texto y
el motor de reproducción dual. No es código propio de BlankTV: es la base sobre la que se construye.

- Origen y obligaciones de licencia: [`NOTICE.md`](NOTICE.md)
- README original del proyecto upstream, sin cambios: [`docs/UPSTREAM_CORE_README.md`](docs/UPSTREAM_CORE_README.md)
- Historial de versiones heredado: [`CHANGELOG.md`](CHANGELOG.md)

> ⚠️ Igual que upstream, este repositorio no incluye canales, listas, suscripciones ni contenido.
> Es infraestructura de reproductor; las fuentes son responsabilidad de quien usa la app.

## Los dos módulos

| Módulo | Namespace | Qué es |
| --- | --- | --- |
| `:core` | `tv.own.owntv.core` | Base de datos Room y migraciones, sync, parsers (M3U / Xtream / Stalker), EPG, backup, perfiles, descargas, ajustes, metadatos y todas las cadenas de texto. |
| `:player-core` | `tv.own.owntv.playercore` | Motor de reproducción: libmpv + Media3/ExoPlayer, escalera de fallback, watchdogs, `LiveTuneController` y diagnóstico de streams. Depende de `:core`. |

Ninguno de los dos dibuja nada: usan Compose solo como *runtime* de estado, sin `compose-ui`,
`compose-foundation`, `androidx.tv.*` ni navegación.

## Contenido

### Reproducción (`:player-core`)

- Motor dual: libmpv (FFmpeg) para máxima compatibilidad de códecs y ExoPlayer (Media3) para Live TV
  casi instantánea, con fallback automático entre ambos.
- HLS, MPEG-DASH y MPEG-TS crudo; Widevine / ClearKey cuando el canal publica servidor de licencias.
- Watchdogs que asumen que el stream es hostil: sin-frame, recuperación de stalls, reseteo de
  superficie, reconexión y log de diagnóstico.
- Renderizado directo 4K HDR, frame-rate matching, reporte de resolución y códec, boost de volumen,
  desplazamiento de subtítulos e idiomas.
- Geometría de timeline para catch-up/rewind. Motores *hero* y *preview* para pantallas de
  exploración.

### Datos y sync (`:core`)

- Room con toda la cadena de migraciones y paging.
- Parsing y sincronización de M3U, Xtream Codes y Stalker/Ministra (portal MAC), con upserts
  incrementales y relinkado de favoritos, historial y progreso.
- EPG (XMLTV), catch-up y modelo de datos de la guía.
- Backup/restore (incluye backup cifrado de contraseñas) y transferencia entre dispositivos.
- Sync local entre dos dispositivos en la misma Wi-Fi, sin cuenta ni nube.
- Perfiles, descargas, ajustes, metadatos TMDB, clima y comprobación de actualizaciones.

### Cadenas y traducciones (`:core`)

26 locales empaquetados y las herramientas que los protegen en `tools/i18n/`. Los cuatro
validadores deben pasar antes de tocar cualquier cadena:

```bash
python tools/i18n/validate_strings.py
python tools/i18n/check_hardcoded_strings.py verify --bootstrap
python tools/i18n/gen_supported_locales.py check
python tools/i18n/check_text_overflow.py
```

## Traducciones

Las traducciones se gestionan en Hosted Weblate, que aloja el proyecto gratis por ser software libre.
Si tu idioma ya está disponible, contribuye allí; si no aparece en la lista, abre primero un ticket
de petición de idioma.

- Traducir: [Hosted Weblate](https://hosted.weblate.org/projects/owntv/)
- Pedir un idioma nuevo: [open a language request ticket](https://github.com/ahXN00/OwnTV/issues/new?template=feature_request.yml&title=%5BLanguage%5D%20Add%20)

Las cadenas nuevas no se consideran terminadas hasta que existen en todos los locales empaquetados.

## Compilar

Requiere **JDK 21** y **Android SDK** (compileSdk 37, minSdk 26). Indica la ruta del SDK:

```properties
# local.properties
sdk.dir=/ruta/a/Android/Sdk
```

```bash
./gradlew :core:assembleRelease :player-core:assembleRelease   # ambos AAR
./gradlew :core:testDebugUnitTest :player-core:testDebugUnitTest
./gradlew build                                                # todo, incluido lint
./gradlew :core:assembleDebugAndroidTest                       # compila el APK de test, no instala
```

Los tests instrumentados necesitan dispositivo o emulador y **nunca** se ejecutan contra una TV real
con datos reales: instalar el APK de test borra catálogo, listas, perfiles e historial.

## Consumir los módulos desde una app

Los artefactos se publican como `tv.own.owntv:core` y `tv.own.owntv:player-core`. El repositorio
Maven de upstream es público y no requiere cuenta ni token:

```kotlin
// settings.gradle.kts
maven {
    url = uri("https://ahxn00.github.io/OwnTV_Core/maven")
    content { includeGroup("tv.own.owntv") }
}

// build.gradle.kts
implementation("tv.own.owntv:core:1.0.68")
implementation("tv.own.owntv:player-core:1.0.68")
```

### Desarrollo contra el código fuente

```properties
# gradle.properties de la app
owntv.corePath=/ruta/a/Blanktv
```

La app incluye este repo como *composite build*, así que un cambio aquí se ve en el siguiente build
sin publicar nada. Sin esa propiedad, se resuelve el artefacto publicado.

### Hooks que la app debe aportar

Se asignan en el `onCreate` de la aplicación, antes de arrancar Koin (`CrashRecorder` lee la versión
antes de que exista el contenedor):

| Hook | Aporta |
| --- | --- |
| `CoreBuildInfo` | `versionName`, `versionCode`, `edgeKey`, `devTools`, `debug`, `diagnosticBuild` |
| `CrashRecorder.diagnostics` | el log de diagnóstico en vivo |
| `LiveSessionLimit.report` | rarezas por proveedor de stream |
| `SubtitleFontAssets.resourceOf` | las fuentes de subtítulos empaquetadas por la app |
| `AppIconSwitcher.mainActivityClass` | el `MainActivity` de la app |

`onCreate` debe retornar de inmediato si `AppIconSwitcher.isRestartProcess(this)` es verdadero. Tras
Koin, llamar a `AppIconSwitcher.start()` y `PlaybackStartup.start(...)`. Los dos motores se construyen
con `ownTVPlayer()` y `livePreviewEngine()` dentro del módulo Koin de la app.

Un hook más lo aporta la pantalla de reproducción, porque es una señal viva:

| Hook | Aporta |
| --- | --- |
| `WatchSession` | qué lista está en pantalla — `open(sourceId)` / `close(sourceId)` |

La app también necesita `android.nonTransitiveRClass=false` en su `gradle.properties` si referencia
las cadenas de core como `R.string.*` directo.

## Estructura

```
core/           :core      — datos, sync, parsers, EPG, backup, settings, strings
core/schemas/   esquemas exportados de Room (historial de migraciones)
player-core/    :player-core — libmpv + ExoPlayer, fallback, watchdogs, diagnóstico
tools/i18n/     validadores y utilidades de traducción
extras/         assets de marca y logos de partners
docs/           documentación heredada del upstream
```

## Créditos

Todo el código de este repositorio es de **OwnTV Core** y sus colaboradores: ver `NOTICE.md`.

- **Weblate** aloja las traducciones del proyecto de forma gratuita por ser software libre.
- **TMDB** aporta metadatos y tráilers de películas y series. Este producto usa la API de TMDB pero
  no está respaldado ni certificado por TMDB.
- **OpenSubtitles** da soporte a la búsqueda y descarga de subtítulos.
- **libmpv / mpv (FFmpeg)**, construido para OwnTV desde
  [jarnedemeulemeester/libmpv-android](https://github.com/jarnedemeulemeester/libmpv-android), y
  **Media3 / ExoPlayer**.
- **Room · Koin · OkHttp · Kotlin Coroutines · WorkManager** y el resto del ecosistema open source de
  Kotlin / AndroidX.

## Licencia

**GNU General Public License v3.0** — ver [`LICENSE`](LICENSE) y [`NOTICE.md`](NOTICE.md).

Puedes usar, estudiar, modificar y redistribuir esta librería, incluso comercialmente, pero cualquier
versión redistribuida debe seguir bajo GPLv3 y su código fuente debe estar disponible.
