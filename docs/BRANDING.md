# Marca: qué cambia y qué no

BlankTV usa **el diseño de OwnTV sin cambios**. Lo único propio es el nombre. Este documento deja por
escrito qué se renombró, qué se dejó igual a propósito y cómo regenerarlo todo, para que un re-vendor
desde upstream no deshaga el trabajo en silencio.

## Lo que se renombró

| Dónde | Antes | Ahora | Herramienta |
| --- | --- | --- | --- |
| Nombre visible de la app (`app_name`) | OwnTV | **BlankTV** | `tools/brand/rebrand_strings.py` |
| Textos visibles en las 45 variantes de idioma | OwnTV | **BlankTV** | idem |
| Wordmark de la app (`owntv_wordmark_own/tv.xml`) | own + tv trazado | **blank + TV** desde tipografía | `tools/brand/wordmark_paths.py` |
| Banner de TV, splash, logos del README | OwnTV | **BlankTV** | `tools/brand/render_brand.py` (`android`, `extras`, `logos`) |
| `applicationId` | `tv.own.owntv` | `tv.blanktv` | `app/build.gradle.kts` |
| Clase `Application` | `OwnTVApp` | `BlanktvApp` | `app/src/main/java/tv/own/owntv/BlanktvApp.kt` |
| Repositorio del actualizador | `ahXN00/OwnTV` | `TalaveraSama/Blanktv` | `BlanktvApp.onCreate` |
| Prefijos de propiedades de build | `owntv.keystore…`, `OWNTV_EDGE_KEY` | `blanktv.…`, `BLANKTV_EDGE_KEY` | `app/build.gradle.kts` |

**1.447 valores** de texto en 187 archivos, contando todos los idiomas.

El isotipo (la tarjeta con el triángulo de *play*, sus ocho colores, la versión de puntos del icono
«Pixel», los splashes animados y el monograma) **no se toca**: es el diseño, y el nombre no está
dentro. Solo cambiaron las imágenes donde aparece la palabra.

### Cambios en código (no solo texto)

Cuatro literales del motor se ven en pantalla o en el disco, así que sí cambiaron:

| Archivo | Constante | Por qué |
| --- | --- | --- |
| `core/…/sync/local/LocalSyncManager.kt` | `UNKNOWN_DEVICE_NAME` | es el nombre con el que este aparato aparece al emparejar por Wi-Fi |
| `core/…/storage/StorageAccess.kt` | carpeta interna de descargas | aparece en la ruta de los archivos que el usuario busca |
| `core/…/util/CrashRecorder.kt` | cabecera del informe de fallos | el usuario lo exporta y lo lee |

Los tres están declarados como literales técnicos revisados en `tools/i18n/safe_literals.txt` (con
`classify-safe`), no como deuda de traducción.

Y dos que **no** cambiaron, con motivo:

| Archivo | Constante | Motivo |
| --- | --- | --- |
| `core/…/subtitles/OpenSubtitlesClient.kt` | `USER_AGENT = "OwnTV"` | OpenSubtitles exige que el User-Agent esté registrado en su API; cambiarlo puede dejar la búsqueda de subtítulos sin servicio |
| `core/…/update/UpdateManager.kt`, `…/weather/WeatherRepository.kt` | `User-Agent` de red | no se ve en ninguna pantalla y no identifica al usuario; tocarlo solo añade ruido al diff |

## Lo que NO se renombró, y por qué

### Nombres de recursos y de claves

`content_exit_owntv`, `setup_set_up_owntv`, `owntv_icon_eggshell`, `owntv_splash_petrol`… siguen
igual. Son identificadores, no texto: los usan ~1400 llamadas de Kotlin, los 26 locales de Weblate y
cualquier merge futuro con upstream. Renombrarlos solo añade riesgo sin que nadie los vea.

### Paquetes y namespace (`tv.own.owntv`)

Renombrar el paquete son ~1800 archivos de `package`/`import`, el `namespace` de los dos módulos, los
nombres de archivos de recursos y las rutas de los esquemas de Room. Cero beneficio visible: el
usuario ve BlankTV, el sistema ve `tv.blanktv` (el `applicationId`). Si algún día hace falta, es una
operación propia con su propia rama.

### Nombres de estilo (`Theme.OwnTV`, `AppIcon`, `CorePixelMark`…)

Internos. Cambiarlos obliga a tocar el motor y el generador de marca a la vez, y no se ven en ningún
sitio.

### Servicios que son de upstream — **decisión deliberada**

Ocho cadenas conservan la palabra «OwnTV» porque describen servicios que **no son de BlankTV**:

| Cadena | Servicio real |
| --- | --- |
| `settings_metadata_shared_worker_description` | el Worker compartido de TMDB de OwnTV |
| `settings_metadata_clear_advanced_message` | idem |
| `settings_metadata_fair_share` | la política de cuota compartida de ese Worker |
| `settings_open_subtitles_access_priority` | el proxy de OpenSubtitles que corre en ese Worker |
| `settings_language_help_translate` | el proyecto de traducción en Hosted Weblate |
| `settings_language_contribution_description` | idem |
| `settings_language_contribution_qr_description` | idem |

Renombrarlas haría que la app afirmara operar un servicio ajeno y mandaría al usuario a una página
que no tiene nada que ver con BlankTV. La lista está en `KEEP_UPSTREAM` dentro de
`tools/brand/rebrand_strings.py`; si algún día BlankTV levanta su propio Worker, se quitan de ahí y
se vuelve a ejecutar el script.

### Comentarios de código y mensajes de log

Los `// …` y los `Log.d("OwnTVHome", …)` no se ven en pantalla y cambiarlos ensucia los diffs contra
upstream. Los tags de logcat siguen siendo los de upstream a propósito: así una búsqueda en el código
del motor sigue sirviendo.

## Regenerar la marca

```bash
# 1. Textos (idempotente; --check falla si queda algo sin renombrar)
python3 tools/brand/rebrand_strings.py
python3 tools/brand/rebrand_strings.py --check

# 2. Wordmark como vectoriales, desde la tipografía que ya viaja en la app
python3 tools/brand/wordmark_paths.py \
    --font app/src/main/res/font/plus_jakarta_sans_variable.ttf \
    --out core/src/main/res/drawable

# 3. Iconos, banners, splashes y logos (necesita Pillow)
python3 tools/brand/render_brand.py android --font app/src/main/res/font/plus_jakarta_sans_variable.ttf --out core/src/main/res
python3 tools/brand/render_brand.py extras  --font app/src/main/res/font/plus_jakarta_sans_variable.ttf --out extras
python3 tools/brand/render_brand.py logos   --font app/src/main/res/font/plus_jakarta_sans_variable.ttf --out extras
```

La tipografía es *Plus Jakarta Sans* en peso 800, la misma que usa el diseño para los títulos, así que
la palabra no introduce una fuente nueva en el APK.

### Por qué hay un generador de wordmark propio

Upstream guarda su palabra como contornos trazados en `tools/brand/pixel/wordmark.json`: solo existen
las letras de «own» y «tv». No hay forma de sacar de ahí una `B` o una `k`. `wordmark_paths.py` dibuja
la palabra nueva desde la fuente y la mete en el mismo lienzo de 988 × 182, con la misma línea base y
las mismas alturas (x-height 138, mayúsculas 179), que es lo que `Wordmark()` espera para componer las
dos mitades y tintarlas con el color de acento.

Dos detalles que costaron un par de iteraciones y conviene no romper:

- Los contornos se emiten en cúbicas (`Qu2CuPen`), no en cuadráticas, para que cualquier renderizador
  de SVG pueda comprobarlos.
- `fillType="nonZero"`, no `evenOdd`: los contornos de una fuente real se solapan (la «a» y la «k» de
  Jakarta lo hacen) y con `evenOdd` aparecen agujeros dentro de la letra. Upstream usaba `evenOdd`
  porque sus formas estaban trazadas, no generadas.

## Lo que el usuario ve distinto

Nada más que el nombre. El mismo menú, las mismas pantallas, los mismos ajustes, los mismos ocho
colores de icono, la misma reproducción.
