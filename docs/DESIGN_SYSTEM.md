# Sistema visual

## Dirección

Cuaderno de entrenamiento editorial: sobrio, claro y motivador. No es un gimnasio agresivo ni un dashboard SaaS genérico. La interfaz prioriza qué toca hoy, cuánto dura y qué se ha completado.

## Paleta

| Token | Claro | Uso |
|---|---|---|
| Ink | `#151A17` | Texto principal |
| Paper | `#F4F1E8` | Fondo |
| Surface | `#FFFDF7` | Tarjetas |
| Signal Orange | `#C94A24` | Acción y foco |
| Moss | `#3F6B5A` | Recuperación, éxito |
| Steel | `#4F5E66` | Información secundaria |
| Danger | `#B3261E` | Seguridad |

Modo oscuro: fondo `#101412`, superficie `#171C19`, texto `#F2EFE6`, naranja `#FF8A65` y musgo `#9BCDB8`. Se mantiene un solo acento y se evita el morado por defecto de Material.

## Tipografía

- Familia del sistema para minimizar dependencias y peso.
- Escala con display 44/36, títulos 30/24/20/18 y cuerpo 16/14/12.
- Jerarquía por peso + color + tamaño, no solo tamaño.
- Números de temporizador y métricas con alineación estable.

## Espaciado y forma

- Base 4/8 dp.
- Padding de pantalla 20 dp.
- Tarjetas con 14–16 dp internos.
- Radios 12/16/24.
- Profundidad por borde sutil y cambio tonal, sin sombras dramáticas.

## Componentes

- `FpCard`: superficie con borde de baja opacidad.
- `FpPrimaryButton`, `FpSecondaryButton`.
- `StatusPill`.
- `EmptyState`.
- `ExerciseAnimation`: figura vectorial original con pose inicial/final.
- `WeekRail`: 7 días con estados entreno/completado/recuperación/descanso.

## Movimiento

- Animaciones de 150–250 ms para UI.
- La animación de técnica es un ciclo lento de 1,8 s.
- No se animan propiedades de layout; solo opacidad y transformaciones visuales.
- Los estados se comunican también con texto/iconos, no solo color.

## Accesibilidad

- Objetivos táctiles de 44–48 dp.
- `contentDescription` en iconos de acción.
- Contraste alto en texto principal.
- Compatibilidad con escalado de fuente.
- Temporizador legible y con texto, no solo color.
- Avisos de seguridad con icono y texto.
