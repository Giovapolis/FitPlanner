# Configuración de Android Studio

## Por qué aparece el error

Android Studio 2026.1.4 arranca con su JBR integrado (Java 25), pero **Gradle 8.14.5 no puede ejecutarse con Java 25**. El proyecto está fijado a una combinación verificada:

- Gradle 8.14.5
- AGP 8.13.2
- Kotlin 1.9.24
- Compose Compiler 1.5.14
- JDK 17

El JBR 25 puede seguir siendo el JDK de la IDE. Lo que debe cambiar es el **Gradle JDK**, no el JDK de arranque de Android Studio.

## Requisito

Instala o localiza un JDK 17. En este equipo:

```
C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
```

Comprueba en una terminal de Windows:

```bat
java -version
```

Debe mostrar `17.x`.

## Paso a paso

1. Abre el proyecto `C:\Users\Giovapolis\FitPlanner`.
2. Ve a **File > Settings** (`Ctrl+Alt+S`).
3. Entra en **Build, Execution, Deployment > Build Tools > Gradle**.
4. En **Gradle JDK**, abre el desplegable.
5. Elige una de estas dos opciones:
   - **Daemon toolchain**: recomendado. El proyecto ya incluye `gradle/gradle-daemon-jvm.properties` con `toolchainVersion=17`, así que Gradle y la IDE usan el mismo JDK.
   - **17**: selecciona la entrada llamada `17` (Temurin).
   - Si `17` no aparece, pulsa **Add JDK...** y selecciona `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`.
6. Pulsa **Apply** y **OK**.
7. Ve a **File > Sync Project with Gradle Files**.
8. Si el error persiste, cierra el proyecto y vuelve a abrirlo.
9. Si sigue persistiendo, ejecuta en la terminal del proyecto:

```bat
gradlew.bat --stop
gradlew.bat --version
```

La segunda orden debe mostrar:

```
JVM: 17.0.20.1
```

10. Compila:

```bat
gradlew.bat :app:assembleDebug
```

## Qué no hacer

- No selecciones `jbr-25` ni “Embedded JDK” como Gradle JDK.
- No actualices AGP, Kotlin, Compose, Room o Gradle desde el Upgrade Assistant.
- No cambies el JDK de arranque de Android Studio a 17: la IDE necesita Java 21 o superior; el proyecto necesita JDK 17 para Gradle.
- No uses el Gradle del sistema; usa siempre `gradlew.bat`.

## Verificación rápida

En Android Studio, abre **Terminal** y ejecuta:

```bat
gradlew.bat --version
```

Debe aparecer `Gradle 8.14.5` y:

```text
Daemon JVM: Compatible with Java 17 (from gradle/gradle-daemon-jvm.properties)
```

Si aparece `25.x`, el Gradle JDK sigue mal configurado.

## Error `file is deleted, can't be loaded`

Ese error no viene del código: es la caché de ficheros de Android Studio (VFS) apuntando a un artefacto de build que se borró o regeneró durante un sync. Suele aparecer si se compila desde terminal mientras la IDE está abierta o si se limpian carpetas `build`.

Solución:

1. Detén los daemons:

```bat
gradlew.bat --stop
```

2. En Android Studio: **File > Invalidate Caches...**.
3. Marca **Clear file system cache and Local History** y pulsa **Invalidate and Restart**.
4. Al abrir, ejecuta **File > Sync Project with Gradle Files**.
5. Compila de nuevo:

```bat
gradlew.bat :app:assembleDebug
```

Si persiste:

1. Cierra Android Studio.
2. Elimina `C:\Users\Giovapolis\FitPlanner\.idea\caches`.
3. Elimina `C:\Users\Giovapolis\FitPlanner\.gradle`.
4. Opcionalmente elimina las carpetas `build` del proyecto.
5. Abre el proyecto y sincroniza.

No borres `.idea/gradle.xml` salvo que quieras volver a configurar el Gradle JDK.

## Opción global (solo si todos tus proyectos usan JDK 17)

Si quieres forzar el JDK 17 para todos los builds de este usuario, crea o edita:

```
C:\Users\Giovapolis\.gradle\gradle.properties
```

y añade:

```properties
org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot
```

Esto es específico de esta máquina y no debe subirse al repositorio. Si más adelante usas un proyecto que exija otro JDK, elimina esta línea o cambia el valor.

## Si Android Studio vuelve a cambiar el JDK

El valor se guarda por proyecto en `.idea/gradle.xml` como `gradleJvm`. Si la IDE lo restablece:

1. Repite la selección en **Settings > Build Tools > Gradle**.
2. Cierra y reabre el proyecto.
3. Comprueba con `gradlew.bat --version`.

No edites `.idea/gradle.xml` a mano mientras Android Studio está abierto: la IDE puede sobrescribirlo.
