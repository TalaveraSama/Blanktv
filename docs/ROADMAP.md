# Roadmap de blankTV

Estado: **MVP funcional en código, sin compilar aún en este entorno** (ver limitación abajo).

## Limitación conocida

Este repositorio se desarrolló en un sandbox **sin JDK, sin Gradle y sin Android SDK**, y sin
acceso a `services.gradle.org` / `dl.google.com`. Por tanto el código **no ha sido compilado ni
ejecutado** aquí. El primer hito es abrir el proyecto en Android Studio (o una CI con SDK) y dejar
el build en verde.

## v0.1 — MVP (estado actual)

- [x] Proyecto Gradle (AGP 8.7 / Kotlin 2.0 / KSP / Compose BOM)
- [x] Marca propia generada (ícono adaptativo, íconos legacy, banner Android TV, wordmark)
- [x] Room: playlists, channels, favourites
- [x] Parser M3U/M3U8 en streaming + tests unitarios
- [x] Import remoto (URL) con Result + estado busy/status
- [x] Navegación por rail (Home, Live, Movies, Series, Favourites, Search, Settings)
- [x] Reproductor Media3 a pantalla completa con HUD TV y zapping CHANNEL_UP/DOWN
- [x] MediaSessionService para teclas de transporte
- [x] Tema oscuro/claro persistido en DataStore

## v0.2 — Calidad de reproducción

- [ ] Preview inline en Live (segundo reproductor ligero o promoción sin recarga, como OwnTV)
- [ ] Reintentos automáticos + detección de desconexión
- [ ] Pista de audio / subtítulos / zoom de vídeo en el HUD
- [ ] `OkHttpDataSource` con timeouts y reintentos por tipo de error

## v0.3 — Datos

- [ ] EPG XMLTV en streaming (ventana rodante + poda), marcador "Now"
- [ ] Búsqueda FTS4 en Room en vez de `LIKE`
- [ ] Paging 3 para listas > 5k
- [ ] Import de archivo local (SAF) y Xtream API

## v0.4 — Producto TV

- [ ] Migrar UI a `androidx.tv:tv-material` (glow, Card, TvLazyRow) conservando `FocusBox`
- [ ] Canales favoritos en Watch-Next del launcher de Android TV
- [ ] Perfiles y control parental
- [ ] Descargas (VOD) con WorkManager

## v0.5 — Robustez

- [ ] R8 activado con keep rules reales
- [ ] Migrations de Room reales (quitar `fallbackToDestructiveMigration`)
- [ ] CI: build + test + lint en cada push
- [ ] Segundo motor (libmpv) detrás de una interfaz `PlaybackEngine`, si la compatibilidad lo exige

## Criterios de aceptación del primer build verde

1. `./gradlew :app:assembleDebug` pasa en una máquina con JDK 17 + SDK 35.
2. `./gradlew :app:testDebugUnitTest` pasa (tests de `M3uParser`).
3. La app instala en un emulador Android TV y reproduce un M3U público de prueba.
