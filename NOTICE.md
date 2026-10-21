# NOTICE — blankTV

## Licencia

blankTV se distribuye bajo la **GNU General Public License v3.0** (ver `LICENSE`).

## Composición del repositorio

El repositorio contiene dos clases de código, y es importante distinguirlas:

### 1. Código original de blankTV (`app/`, `brand/`, `docs/`, builds)

Escrito desde cero para este proyecto. Su diseño se inspira en la arquitectura de OwnTV, estudiada
y documentada en `docs/ANALISIS_OWNTV.md`; no se copió código de ahí para escribirlo.

### 2. Código de terceros vendado verbatim (`third_party/`)

- **`third_party/owntv-core/`** — copia íntegra y sin modificaciones de
  [OwnTV_Core](https://github.com/ahXN00/OwnTV_Core) de `ahXN00`, commit
  `c14e944b8d17e5cbb559d116afb30dc3b34864cb`, bajo **GPL-3.0**.
  Copyright de sus autores originales; se redistribuye respetando dicha licencia.
  Ver `third_party/README.md` para las reglas de uso (solo lectura, no se compila).

Al distribuir fuente GPL-3.0 junto al proyecto, el conjunto se ofrece bajo GPL-3.0, y cualquier
adaptación futura de `owntv-core` dentro de `app/` heredará esa licencia y conservará los avisos
de copyright de OwnTV.

## Atribución

- **ahXN00 / OwnTV / OwnTV_Core** — referencia arquitectónica y autor del código GPL vendado en
  `third_party/owntv-core/`. Este proyecto no está afiliado ni respaldado por sus autores.
- Los únicos binarios no generados por este proyecto son el **Gradle Wrapper** (`gradlew`,
  `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`), software de [Gradle](https://gradle.org)
  bajo **Apache License 2.0**, incluido sin modificaciones.

## Marca

El nombre, el ícono, el banner y el wordmark de **blankTV** son originales de este proyecto y no
derivan de la marca OwnTV ni de sus logos.

## Terceros (dependencias de build)

- Jetpack, AndroidX y Media3 — Apache-2.0 (Google)
- Kotlin — Apache-2.0 (JetBrains)
- OkHttp, Coil — Apache-2.0
- Room / KSP — Apache-2.0
- org.json (solo tests) — licencia propia (JSON Reference Implementation)
