# FitPlanner AI

Aplicación Android nativa de entrenamiento personalizado, offline-first, construida con Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt y WorkManager.

> FitPlanner AI es una herramienta educativa. No sustituye a un médico, fisioterapeuta, entrenador ni servicio de urgencias. Si aparece dolor, mareo, desmayo, dolor de pecho o dificultad respiratoria inusual, detente y consulta.

## Estado

- Proyecto funcional y compilable.
- Onboarding, cribado de salud, catálogo, generación de planes, sesión activa, historial, progreso, revisión semanal, recordatorios y capa de IA opcional implementados.
- Build debug generado en `app/build/outputs/apk/debug/app-debug.apk`.
- No hay emulador ni dispositivo conectado en este entorno, por lo que la verificación automática es de compilación, tests JVM/Robolectric y lint. Falta una pasada visual en dispositivo real.

## Funcionalidades

- Perfil físico, objetivo, nivel, actividad y unidades.
- Cribado de lesiones, síntomas de alarma y recomendaciones médicas.
- Disponibilidad semanal por día con duración y tipo de sesión.
- Perfiles de equipamiento separados para casa y gimnasio.
- Catálogo de 27 ejercicios con búsqueda, filtros, favoritos, ficha técnica y animación vectorial original.
- Motor determinista de rutinas según días, nivel, objetivo, equipo, restricciones y tiempo.
- Planificador semanal con revisión, avisos y sustitución de ejercicios.
- Sesión activa con series, repeticiones, peso, RPE, descanso, pausa, saltos y finalización.
- Dashboard con estado vacío real, cumplimiento, volumen muscular y próximo entrenamiento.
- Historial inmutable de semanas y revisión con ajustes prudentes.
- Recordatorios con WorkManager.
- IA opcional compatible con OpenAI, desactivada por defecto y validada por el motor local.
- Datos locales; sin cuentas, analítica ni nube obligatoria.

## Stack

| Componente | Versión |
|---|---|
| Gradle | 8.14.5 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 1.9.24 |
| Compose BOM | 2024.06.00 |
| Compose Compiler | 1.5.14 |
| Room | 2.6.1 |
| Hilt | 2.52 |
| Navigation Compose | 2.9.8 |
| DataStore | 1.2.0 |
| WorkManager | 2.10.0 |
| JDK | 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |

## Estructura

```
:app                  Aplicación, navegación, Hilt y WorkManager
:core:model           Modelos y enums de dominio
:core:domain          Motor de reglas, validadores y cálculo de progreso
:core:data            Room, DataStore, repositorios, seeds, IA y recordatorios
:core:designsystem    Tema, componentes y animación de técnica
:feature:onboarding   Asistente de configuración
:feature:dashboard    Inicio
:feature:planner      Planificación y generación
:feature:exercises    Catálogo y fichas
:feature:workout      Sesión activa
:feature:progress     Progreso e historial
:feature:profile      Perfil, equipamiento, ajustes e IA
```

Más detalle en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Compilación

Requisitos:

- Windows con **JDK 17** (Temurin o equivalente). No uses Java 25 para Gradle.
- Android SDK con platform 35 y build-tools 35.
- El wrapper de Gradle incluido.

Si Android Studio muestra “Gradle 8.14.5 is incompatible with the Gradle JVM version 25”, sigue la guía paso a paso: [docs/ANDROID_STUDIO.md](docs/ANDROID_STUDIO.md). El JDK de arranque de la IDE puede ser 25; el **Gradle JDK** debe ser 17.

`local.properties` debe apuntar al SDK local:

```properties
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

Desde WSL se puede usar el wrapper de Windows:

```bash
cmd.exe /c "cd /d C:\Users\Giovapolis\FitPlanner && gradlew.bat :app:assembleDebug"
```

APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Tests y calidad

```bash
cmd.exe /c "cd /d C:\Users\Giovapolis\FitPlanner && gradlew.bat :core:domain:test :core:data:testDebugUnitTest :app:lintDebug :app:assembleDebug"
```

Incluye:

- Tests del motor de reglas, seguridad, duración y ajuste semanal.
- Tests de serialización Moshi y del catálogo.
- Tests de Room/DataStore con Robolectric.
- Lint sin errores bloqueantes.

Más detalle en [docs/TESTING.md](docs/TESTING.md).

## IA opcional

La app funciona completa sin IA. Para activarla:

1. Abre **Perfil > Sugerencias con IA**.
2. Activa el interruptor.
3. Configura URL base, modelo y API key.
4. Acepta el consentimiento de envío anonimizado.

Solo se envían objetivo, nivel, días, minutos, equipamiento, división, slugs de ejercicios y etiquetas de restricción. Nunca se envían nombre, edad exacta, peso ni notas médicas. La respuesta se valida con el motor local y se descarta si no cumple equipo, restricciones o duración. La clave se guarda cifrada en el dispositivo. Para un producto multiusuario real se necesitaría un proxy de servidor; no incluyas claves compartidas en un APK distribuido.

## Seguridad

- Síntomas de alarma o recomendación médica bloquean la generación automática.
- Las lesiones declaradas filtran ejercicios y muestran advertencias, sin prometer seguridad clínica.
- No se hacen diagnósticos.
- No se suben cargas automáticamente: los aumentos son sugerencias conservadoras y requieren confirmación.
- El historial completado no se sobrescribe al ajustar semanas futuras.

Ver [docs/SAFETY.md](docs/SAFETY.md).

## Limitaciones conocidas

- El contenido técnico y las animaciones son originales y simplificados; no sustituyen una demostración profesional.
- No hay vídeos/GIF externos ni enlaces de técnica por ejercicio; se evita inventar fuentes.
- No se ha ejecutado en un dispositivo físico en este entorno.
- La IA opcional requiere red y una clave del usuario.
- No hay exportación/importación de datos todavía.
- El plan semanal se guarda como snapshot JSON dentro de Room para preservar historial; las entidades principales sí están normalizadas.

## Licencias y contenido

- Código y animaciones vectoriales: originales de este proyecto.
- No se incluyen imágenes, GIF o vídeos de terceros.
- Las fuentes tipográficas usan la familia del sistema para evitar dependencias externas.
