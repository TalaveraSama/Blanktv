# Análisis de arquitectura — OwnTV (`ahXN00/OwnTV`)

> Documento de trabajo. Describe cómo está construido OwnTV y qué decisiones tomamos de ahí para
> **blankTV**. blankTV es código **original escrito desde cero**; este análisis es la única
> transferencia de conocimiento entre ambos proyectos (ver `NOTICE.md`).

Fecha del análisis: 2026-10-21 · commit analizado: rama `main` de `ahXN00/OwnTV`.

---

## 1. Qué es OwnTV

Reproductor IPTV nativo para **Android TV** (Kotlin + Jetpack Compose for TV). Es *solo* un
reproductor: el usuario aporta sus fuentes (M3U, Xtream, Stalker/MAC). Licencia **GPL-3.0**.

No es un solo repo: es un sistema de **cuatro repositorios**.

| Repo | Licencia | Rol | Tamaño aprox. |
|---|---|---|---|
| `ahXN00/OwnTV` | GPL-3.0 | Shell de UI de Android TV | 254 archivos Kotlin, ~70k LOC |
| `ahXN00/OwnTV_Core` | GPL-3.0 | El motor: Room, parsers, EPG, sync, backup, strings (26 idiomas), recursos de marca | ~276 MB |
| `ahXN00/OwnTV_libmpv` | MIT | Scripts de build de libmpv para Android | ~4 MB |
| `ahXN00/OwnTV_Mobile` | GPL-3.0 | App de móvil/tablet sobre el mismo core | — |

**Conclusión estructural nº1:** el repo `OwnTV` **no compila solo**. Consume los artefactos
publicados `tv.own.owntv:core:1.0.68` y `tv.own.owntv:player-core:1.0.68` desde el Maven público
`https://ahxn00.github.io/OwnTV_Core/maven`. La mitad de la lógica real vive en `OwnTV_Core`.

## 2. Layout del shell (lo que sí está en el repo)

```
tv.own.owntv/
├── player/      HUD del reproductor, superficies, mini-player   (19 kt)
├── ui/          theme + componentes reutilizables + stage       (60 kt)
├── features/    setup, shell, live, movies, series, search,
│                downloads, epg, profiles, settings, sports…     (~160 kt)
├── di/          módulos Koin                                     (2 kt)
└── core/i18n/   contenido localizado + claves RTL                (2 kt)
```

Todo lo demás bajo `tv.own.owntv.core.*` (database, settings, sync, sports, customize, live,
theme, model, repository, epg, parser, metadata, recording, nav, brand, storage, download,
companion, setup, launcher, player, util, i18n) **viene del artefacto remoto**, no del repo.

## 3. Stack técnico

| Área | Elección en OwnTV |
|---|---|
| Lenguaje | Kotlin 2.4.20 (sin plugin `kotlin-android`; lo aporta AGP 9) |
| Build | AGP 9.4.0 / Gradle 9.7.1 / KSP2 2.3.11 |
| UI | Compose for TV (`androidx.tv:tv-material` 1.1.0), BOM 2026.08.00 |
| Media | **libmpv** (FFmpeg) + **ExoPlayer/Media3** 1.11.1, con fallback automático |
| DB | Room 2.8.5 (27 entidades, FTS4) + Paging 3.5.1 |
| DI | Koin 4.2.2 |
| Red / imágenes | OkHttp 5 · Coil 3.6.0 |
| Mínimos | minSdk 26 · targetSdk 36 · compileSdk 37 · appId `tv.own.owntv` |

## 4. Decisiones de diseño que vale la pena entender

1. **Dos motores de reproducción** detrás de una interfaz `PlaybackEngine`, con una escalera de
   fallback de cuatro peldaños: mpv por compatibilidad, ExoPlayer para Live instantáneo. El preview
   de Live se *promociona* a pantalla completa sin recargar.
2. **Parsing en streaming.** M3U se lee línea a línea y el JSON de Xtream con `JsonReader`: un
   payload de 50k canales nunca se bufferiza entero. El sync es por chunks, cancelable y con
   progreso.
3. **Room + Paging 3 con ventana acotada** para que listas de 50k elementos no crezcan en memoria.
4. **EPG XMLTV** parseado en streaming (con gzip) a una ventana rodante que se poda.
5. **DI por referencias de constructor** (`viewModelOf(::X)`) en vez de listas posicionales de
   `get()`: con 15–24 parámetros, una lista posicional puede intercambiar dos dependencias del
   mismo tipo sin error de compilación.
6. **Gate anti-strings-hardcodeados** en el build (`tools/i18n/check_hardcoded_strings.py`) que
   falla la compilación si hay texto de usuario literal en Kotlin. CI lo refuerza.
7. **ABIs como product flavors** (`standard` arm / `x86_64` emulador) para partir el APK a la mitad
   (~49 MB vs 104 MB universal) y evitar "parse error" en descargas truncadas.
8. **Un `MainActivity` por color de ícono** (9 activities, una habilitada a la vez vía
   `AppIconSwitcher`), todas con los mismos intent-filters para que `owntv://` siempre resuelva.
9. **Permisos repartidos entre módulos:** red y foreground-service los declara `:core`;
   `MODIFY_AUDIO_SETTINGS` lo declara `:player-core`. El manifiesto del app solo añade lo suyo.

## 5. Riesgos / deudas observadas en OwnTV

- Dependencia dura de artefactos publicados por un tercero (si el Maven de `ahxn00.github.io`
  cae o cambia de versión, el fork no compila).
- Cadena de toolchain extremadamente nueva (AGP 9 / Kotlin 2.4 / compileSdk 37): cualquier entorno
  de CI o IDE algo atrasado se queda fuera.
- El repo de core (276 MB) hace inviable un fork completo ligero.
- Marca, íconos, banners y **todas** las traducciones viven en core: renombrar el shell sin
  renombrar core deja la app con recursos ajenos.

## 6. Qué tomamos para blankTV (y qué no)

**Tomamos (como ideas, reimplementadas):**
- Separación clara `data / domain / view / ui / features`.
- Parsing M3U en streaming, nunca bufferizar el playlist entero.
- Imports por chunks para no bloquear la UI con el write-lock de SQLite.
- Un único `ExoPlayer` por proceso, reutilizado por todas las pantallas + `MediaSessionService`
  para las teclas de transporte.
- HUD propio para TV en vez del controller táctil de `PlayerView`; zapping con CHANNEL_UP/DOWN.
- Foco visible + escala como primitiva D-pad (`FocusBox`).
- Gate de no-hardcodear strings de usuario (todos nuestros textos viven en `strings.xml`).

**No tomamos (a propósito):**
- Koin / DI framework → grafo manual de 5 objetos (`AppContainer`). Menos magia, mismo resultado.
- Doble motor mpv+ExoPlayer → solo Media3 por ahora (ver `ROADMAP.md`).
- `androidx.tv:tv-material` → Compose Foundation + Material3 con manejo de foco propio, para
  minimizar superficie de API no verificable en este entorno.
- 9 activities de ícono → una sola activity.
- Dependencia de artefactos de terceros → todo el código vive en este repo.

## 7. Mapa de equivalencias OwnTV → blankTV

| OwnTV | blankTV |
|---|---|
| `tv.own.owntv` (appId) | `app.blanktv.tv` |
| `OwnTVApp` | `BlankTvApp` |
| `di/AppModule.kt` (Koin) | `di/AppContainer.kt` (manual) |
| `core/database` (27 entidades, en core remoto) | `data/db` (3 entidades, local) |
| parser M3U/Xtream (core remoto) | `data/parser/M3uParser.kt` |
| `player/` HUD + 2 motores | `player/PlaybackController.kt` + `features/player/PlayerScreen.kt` |
| `features/*` | `features/*` (home, live, catalogue, favourites, search, settings, player) |
| Maven `tv.own.owntv:core` | inexistente — todo local |
