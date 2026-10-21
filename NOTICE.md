# NOTICE — blankTV

## Licencia

blankTV se distribuye bajo la **GNU General Public License v3.0** (ver `LICENSE`).

## Atribución y relación con OwnTV

blankTV **estudia** la arquitectura del proyecto [OwnTV](https://github.com/ahXN00/OwnTV) de
`ahXN00` (y de su librería `OwnTV_Core`), ambos bajo GPL-3.0, y se escribe como **código original
desde cero**: no se copia, adapta ni transforma código fuente, recursos, textos ni assets de
OwnTV en este repositorio. La transferencia es de ideas arquitectónicas, documentadas en
`docs/ANALISIS_OWNTV.md`.

No obstante, y en el espíritu del copyleft:

1. blankTV **elige GPL-3.0** como licencia propia, de modo que cualquier aprendizaje derivado de
   software GPL regresa a la comunidad bajo los mismos términos.
2. Se reconoce públicamente a **ahXN00 / OwnTV** como la referencia arquitectónica que inspiró el
   diseño (separación de módulos, parsing en streaming, estrategia de reproducción y enfoque
   TV-first con D-pad).
3. Los únicos archivos binarios no generados por este proyecto son el **Gradle Wrapper**
   (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`), que es software de
   [Gradle](https://gradle.org) bajo **Apache License 2.0**, incluido sin modificaciones para que
   el proyecto sea construible desde el primer clon.

## Marca

El nombre, el ícono, el banner y el wordmark de **blankTV** son originales de este proyecto y no
derivan de la marca OwnTV ni de sus logos.

## Terceros

- Jetpack, AndroidX y Media3 — Apache-2.0 (Google)
- Kotlin — Apache-2.0 (JetBrains)
- OkHttp, Coil — Apache-2.0
- Room/KSP — Apache-2.0
