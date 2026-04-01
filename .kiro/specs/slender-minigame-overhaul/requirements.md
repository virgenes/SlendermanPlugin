# Requirements Document

## Introduction

Este documento describe los requisitos para la revisión completa del plugin de Minecraft "Stop It Slender". El alcance incluye la corrección de tres bugs críticos reportados por usuarios (sistema de páginas, configuración de maxPlayers y brújula de Slenderman) y la implementación de nuevas funcionalidades: efectos visuales/sonoros al recoger páginas, sistema de perks funcional, brújula mejorada y validaciones robustas al guardar/cargar arenas.

## Glossary

- **Arena**: Instancia de una partida del minijuego, con su propio estado, jugadores y configuración.
- **ArenaManager** (GameManager): Componente responsable de cargar, guardar, crear y destruir arenas.
- **Arena_Editor** (EditorMenu): Interfaz gráfica de administración para configurar una arena.
- **Page_System**: Subsistema responsable de spawnear, rastrear y recoger páginas durante la partida.
- **Perk_System**: Subsistema de habilidades pasivas/activas equipables por jugadores antes de la partida.
- **Slenderman**: Rol asignado a un jugador que actúa como el antagonista de la partida.
- **Survivor**: Rol asignado a los jugadores que deben recoger las 8 páginas para ganar.
- **Compass**: Item especial del Slenderman que apunta al superviviente más cercano.
- **Terror_Radius**: Efecto de sonido ambiental que se activa cuando un superviviente está cerca del Slenderman.
- **Validator**: Componente que verifica la integridad de la configuración de una arena antes de guardar o iniciar.

---

## Requirements

### Requirement 1: Corrección del sistema de páginas (spawn seguro)

**User Story:** Como administrador de servidor, quiero que las páginas se spawneen correctamente en el mapa, para que la partida no falle con un error cuando la lista de ubicaciones de páginas está vacía o mal configurada.

#### Acceptance Criteria

1. WHEN `spawnPage()` es invocado y la lista de ubicaciones de páginas está vacía, THE `Page_System` SHALL registrar un mensaje de error en consola y cancelar el spawn sin lanzar una excepción.
2. WHEN `spawnPage()` es invocado y la lista de ubicaciones de páginas tiene al menos una entrada, THE `Page_System` SHALL seleccionar una ubicación aleatoria válida y dropear el item de página en esa ubicación.
3. THE `Page_System` SHALL usar un método de selección aleatoria que no lance `IllegalArgumentException` cuando el bound es 0.

---

### Requirement 2: Corrección del pickup de páginas (solo supervivientes)

**User Story:** Como jugador superviviente, quiero ser el único que puede recoger páginas, para que el Slenderman o espectadores no puedan interferir con el conteo de páginas.

#### Acceptance Criteria

1. WHEN un jugador intenta recoger un item de página, THE `Page_System` SHALL verificar el rol del jugador ANTES de remover el item del mundo.
2. IF el jugador que intenta recoger la página tiene rol distinto a `SURVIVOR`, THEN THE `Page_System` SHALL cancelar el evento sin remover el item del mundo.
3. WHEN un `SURVIVOR` recoge una página, THE `Page_System` SHALL remover el item, incrementar el contador de páginas y disparar el evento `SlenderSurvivorPickupPageEvent`.
4. THE `Page_System` SHALL garantizar que el item de página no sea removido del mundo si el pickup es cancelado.

---

### Requirement 3: Corrección de maxPlayers (carga desde YAML)

**User Story:** Como administrador de servidor, quiero que el valor de `maxPlayers` se cargue correctamente desde el archivo YAML, para que la arena no muestre valores incorrectos como 0 o negativos.

#### Acceptance Criteria

1. WHEN `ArenaManager` carga una arena desde un archivo YAML y el campo `GameSettings.MaxPlayers` no está presente o es 0, THE `ArenaManager` SHALL asignar un valor por defecto de 8 y registrar una advertencia en consola.
2. WHEN `ArenaManager` carga una arena desde un archivo YAML y el campo `GameSettings.MinPlayers` no está presente o es 0, THE `ArenaManager` SHALL asignar un valor por defecto de 2 y registrar una advertencia en consola.
3. THE `Arena_Editor` SHALL impedir que `maxPlayers` sea reducido por debajo de 2 al hacer click derecho.
4. THE `Arena_Editor` SHALL impedir que `minPlayers` sea reducido por debajo de 1 al hacer click derecho.
5. THE `Arena_Editor` SHALL impedir que `minPlayers` sea mayor que `maxPlayers` al incrementar su valor.
6. WHEN `getAvailableArena()` busca una arena disponible, THE `ArenaManager` SHALL filtrar arenas donde el número de jugadores actuales sea estrictamente menor que `maxPlayers` (usando `<` en lugar de `!=`).

---

### Requirement 4: Corrección del efecto de ceguera de supervivientes

**User Story:** Como jugador superviviente, quiero que el efecto de ceguera inicial sea temporal y no cause daño, para que la experiencia de inicio de partida sea correcta.

#### Acceptance Criteria

1. WHEN la partida inicia y los supervivientes son enviados a sus posiciones, THE `Arena` SHALL aplicar el efecto `BLINDNESS` con una duración de 5 segundos (100 ticks) y amplifier 0.
2. THE `Arena` SHALL garantizar que el amplifier del efecto `BLINDNESS` no exceda 0 para evitar daño colateral.

---

### Requirement 5: Corrección del NullPointerException en endGame (radiusTask)

**User Story:** Como administrador de servidor, quiero que la partida termine sin errores aunque el Terror Radius esté desactivado, para que el servidor no lance excepciones en consola.

#### Acceptance Criteria

1. WHEN `endGame()` es invocado y `USE_TERROR_RADIUS` es `false`, THE `Arena` SHALL omitir la cancelación de `radiusTask` sin lanzar `NullPointerException`.
2. WHEN `endGame()` es invocado y `radiusTask` fue inicializado, THE `Arena` SHALL cancelar `radiusTask` correctamente.
3. IF `radiusTask` es `null` al momento de llamar `endGame()`, THEN THE `Arena` SHALL continuar la ejecución de `endGame()` sin interrupciones.

---

### Requirement 6: Efectos visuales y sonoros al recoger páginas

**User Story:** Como jugador superviviente, quiero recibir feedback visual y sonoro al recoger una página, para que la experiencia de juego sea más inmersiva.

#### Acceptance Criteria

1. WHEN un `SURVIVOR` recoge una página exitosamente, THE `Page_System` SHALL reproducir el sonido `ENTITY_EXPERIENCE_ORB_PICKUP` al jugador que recogió la página.
2. WHEN un `SURVIVOR` recoge una página exitosamente, THE `Page_System` SHALL enviar un título al jugador indicando el número de páginas recogidas en el formato `"Página X/8"`.
3. WHEN un `SURVIVOR` recoge una página exitosamente, THE `Page_System` SHALL reproducir el sonido `BLOCK_NOTE_BLOCK_PLING` a todos los jugadores de la arena.
4. WHEN se recogen todas las 8 páginas, THE `Page_System` SHALL reproducir el sonido `UI_TOAST_CHALLENGE_COMPLETE` a todos los jugadores de la arena antes de llamar a `endGame()`.

---

### Requirement 7: Brújula del Slenderman apunta al superviviente más cercano

**User Story:** Como jugador Slenderman, quiero que mi brújula apunte al superviviente más cercano, para poder rastrear a los supervivientes de forma efectiva.

#### Acceptance Criteria

1. WHEN el `Slenderman` usa la brújula (click derecho), THE `Compass` SHALL calcular el superviviente con menor distancia euclidiana al `Slenderman` entre todos los jugadores con rol `SURVIVOR`.
2. WHEN el `Slenderman` usa la brújula y hay al menos un superviviente vivo, THE `Compass` SHALL actualizar `player.setCompassTarget()` con la ubicación del superviviente más cercano.
3. WHEN el `Slenderman` usa la brújula y no hay supervivientes vivos, THE `Compass` SHALL enviar un mensaje al `Slenderman` indicando que no hay supervivientes disponibles.
4. WHEN el `Slenderman` usa la brújula exitosamente, THE `Compass` SHALL reproducir el sonido `ENTITY_ENDERMAN_TELEPORT` al `Slenderman`.

---

### Requirement 8: Validaciones al guardar arenas

**User Story:** Como administrador de servidor, quiero recibir mensajes de error claros al guardar una arena mal configurada, para poder corregir los problemas antes de que los jugadores intenten unirse.

#### Acceptance Criteria

1. WHEN `saveGame()` es invocado y `slenderManSpawnLocation` es `null`, THE `Validator` SHALL cancelar el guardado y enviar al administrador el mensaje: `"[SlenderMan] Error: Falta la ubicación de spawn del Slenderman."`.
2. WHEN `saveGame()` es invocado y `survivorsLocations` está vacía, THE `Validator` SHALL cancelar el guardado y enviar al administrador el mensaje: `"[SlenderMan] Error: Se requiere al menos una ubicación de spawn para supervivientes."`.
3. WHEN `saveGame()` es invocado y `pagesLocations` está vacía, THE `Validator` SHALL cancelar el guardado y enviar al administrador el mensaje: `"[SlenderMan] Error: Se requiere al menos una ubicación de spawn para páginas."`.
4. WHEN `saveGame()` es invocado y `maxPlayers` es menor que `minPlayers`, THE `Validator` SHALL cancelar el guardado y enviar al administrador el mensaje: `"[SlenderMan] Error: maxPlayers no puede ser menor que minPlayers."`.
5. WHEN `saveGame()` es invocado y la configuración es válida, THE `Validator` SHALL permitir el guardado y confirmar con el mensaje de éxito existente.
6. WHEN `Arena_Editor` ejecuta la acción `SAVE`, THE `Arena_Editor` SHALL invocar las validaciones del `Validator` y mostrar los errores al administrador en el chat antes de cerrar el menú.

---

### Requirement 9: Sistema de perks funcional

**User Story:** Como jugador, quiero poder equipar perks antes de la partida y que estos tengan efecto durante el juego, para que haya variedad estratégica en cada partida.

#### Acceptance Criteria

1. THE `Perk_System` SHALL permitir a los jugadores equipar un perk de superviviente y un perk de Slenderman desde el menú de perks en el lobby.
2. WHEN la partida inicia y los roles son asignados, THE `Perk_System` SHALL entregar el item del perk equipado al jugador en el slot 4 de su inventario.
3. WHEN un `SURVIVOR` usa su perk (click derecho en el item del slot 4), THE `Perk_System` SHALL invocar `usePerk(player)` del perk equipado.
4. WHEN el `Slenderman` usa su perk (click derecho en el item del slot 4), THE `Perk_System` SHALL invocar `usePerk(player)` del perk equipado.
5. IF un jugador no tiene perk equipado, THEN THE `Perk_System` SHALL asignar el perk `NONE` por defecto, que no tiene efecto al usarse.
6. WHEN la partida termina, THE `Perk_System` SHALL limpiar los efectos activos de todos los perks.

---

### Requirement 10: Validación al iniciar partida (arena lista)

**User Story:** Como administrador de servidor, quiero que una arena no pueda iniciar si no está completamente configurada, para evitar errores en tiempo de ejecución durante la partida.

#### Acceptance Criteria

1. WHEN `start()` es invocado en una arena, THE `Arena` SHALL verificar que `slenderManSpawnLocation` no sea `null` antes de proceder.
2. WHEN `start()` es invocado en una arena, THE `Arena` SHALL verificar que `survivorsLocations` tenga al menos tantas entradas como jugadores supervivientes.
3. WHEN `start()` es invocado en una arena, THE `Arena` SHALL verificar que `pagesLocations` tenga al menos una entrada.
4. IF alguna de las validaciones de `start()` falla, THEN THE `Arena` SHALL cancelar el inicio, enviar un mensaje de error a todos los jugadores de la arena y transicionar al estado `WAITING`.
