# Implementation Plan: Slender Minigame Overhaul

## Overview

Corrección de 8 bugs críticos e implementación de 4 nuevas funcionalidades en el plugin "Stop It Slender". Las tareas siguen el orden de menor a mayor dependencia: primero la infraestructura de validación, luego los bugs de núcleo, después las nuevas funcionalidades, y finalmente el sistema de perks.

## Tasks

- [x] 1. Crear `ArenaValidator` con lógica de validación centralizada
  - Crear `Slender/slender-plugin/src/main/java/me/dreamdevs/slender/game/ArenaValidator.java`
  - Implementar `validate(Arena arena)`: verificar `slenderManSpawnLocation != null`, `survivorsLocations` no vacía, `pagesLocations` no vacía, `maxPlayers >= minPlayers`; retornar lista de mensajes de error en español
  - Implementar `validateForStart(Arena arena)`: verificar además que `survivorsLocations.size() >= survivorCount`
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 10.1, 10.2, 10.3_

  - [ ]* 1.1 Escribir property test para `ArenaValidator` — Property 8: Arena inválida no se guarda ni inicia
    - **Property 8: Arena inválida no se guarda ni inicia**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 10.1, 10.2, 10.3**

- [x] 2. Corregir bugs en `Arena`: `spawnPage()`, BLINDNESS y `endGame()` NPE
  - En `spawnPage()`: añadir guard clause al inicio — si `getPagesLocations().isEmpty()` llamar `Util.sendPluginMessage(...)` y retornar
  - En `sendPlayersToGame()`: cambiar `new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, Integer.MAX_VALUE)` por `new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)`
  - En `endGame()`: envolver `this.radiusTask.cancel()` con `if (this.radiusTask != null)`
  - En `start()`: invocar `ArenaValidator.validateForStart(this)` al inicio; si hay errores, enviarlos con `sendMessage()` y hacer `setArenaState(ArenaState.WAITING); return`
  - _Requirements: 1.1, 1.2, 1.3, 4.1, 4.2, 5.1, 5.2, 5.3, 10.1, 10.2, 10.3, 10.4_

  - [ ]* 2.1 Escribir property test para `spawnPage()` — Property 1: dropea en ubicación de la lista
    - **Property 1: spawnPage con lista no vacía dropea en ubicación de la lista**
    - **Validates: Requirements 1.2**

  - [ ]* 2.2 Escribir property test para BLINDNESS — Property 7: amplifier 0 y duración <= 100
    - **Property 7: BLINDNESS inicial tiene duración 100 ticks y amplifier 0**
    - **Validates: Requirements 4.1, 4.2**

  - [ ]* 2.3 Escribir property test para `start()` inválido — Property 12: estado WAITING tras fallo
    - **Property 12: Estado WAITING tras fallo de validación en start()**
    - **Validates: Requirements 10.4**

  - [ ]* 2.4 Escribir unit test `endGame_radiusTaskNull_doesNotThrow`
    - Verificar que `endGame()` no lanza NPE cuando `radiusTask == null`
    - _Requirements: 5.1, 5.3_

  - [ ]* 2.5 Escribir unit test `spawnPage_emptyList_doesNotThrow`
    - Verificar que `spawnPage()` no lanza excepción con lista vacía
    - _Requirements: 1.1_

- [x] 3. Corregir `GameListeners.pickupEvent()`: verificar rol antes de remover item
  - Mover la verificación `if (arena.getPlayers().get(gamePlayer.getPlayer()) != Role.SURVIVOR) return;` ANTES de `event.getItem().remove()`
  - Añadir efectos tras incrementar `collectedPages`: sonido personal `ENTITY_EXPERIENCE_ORB_PICKUP`, título `"Página X/8"`, sonido broadcast `BLOCK_NOTE_BLOCK_PLING`
  - Añadir sonido `UI_TOAST_CHALLENGE_COMPLETE` a todos cuando `collectedPages == 8`, antes de llamar `endGame()`
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 6.1, 6.2, 6.3, 6.4_

  - [ ]* 3.1 Escribir property test — Property 2: Non-SURVIVOR no puede remover páginas
    - **Property 2: Non-SURVIVOR no puede remover páginas del mundo**
    - **Validates: Requirements 2.1, 2.2, 2.4**

  - [ ]* 3.2 Escribir property test — Property 3: SURVIVOR incrementa contador al recoger página
    - **Property 3: SURVIVOR incrementa contador al recoger página**
    - **Validates: Requirements 2.3**

  - [ ]* 3.3 Escribir property test — Property 14: título contiene el número correcto
    - **Property 14: Título de página contiene el número correcto**
    - **Validates: Requirements 6.2**

  - [ ]* 3.4 Escribir unit test `allPages_collected_callsEndGame`
    - Verificar que al llegar a 8 páginas se llama `endGame()` y se reproduce el sonido de victoria
    - _Requirements: 6.4_

- [x] 4. Corregir `PlayerInteractListener`: brújula apunta al superviviente más cercano
  - Añadir null check `if (gamePlayer.getArena() == null) return;` antes de usar la brújula
  - Reemplazar `findAny()` por `.min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(slender.getLocation())))` para obtener el superviviente más cercano
  - Si `nearest == null`, enviar `Langauge.COMPASS_NO_SURVIVORS.toString()` al jugador
  - Añadir `player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)` al usar la brújula exitosamente
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 4.1 Escribir property test — Property 13: brújula apunta al superviviente con menor distancia euclidiana
    - **Property 13: Brújula apunta al superviviente con menor distancia euclidiana**
    - **Validates: Requirements 7.1, 7.2**

  - [ ]* 4.2 Escribir unit test `compass_noSurvivors_sendsMessage`
    - Verificar que se envía mensaje cuando no hay supervivientes
    - _Requirements: 7.3_

- [x] 5. Corregir `GameManager`: valores por defecto en `loadGame()` y operador en `getAvailableArena()`
  - En `loadGame()`: tras leer `MaxPlayers`, si `<= 0` asignar 8 y loguear advertencia; tras leer `MinPlayers`, si `<= 0` asignar 2 y loguear advertencia
  - En `getAvailableArena()`: cambiar `arena.getSurvivorsAmount() != arena.getMaxPlayers()` por `arena.getPlayers().size() < arena.getMaxPlayers()`
  - Modificar `saveGame(Arena arena)` para aceptar un `@Nullable Player admin` opcional; invocar `ArenaValidator.validate(arena)` al inicio y si hay errores enviarlos al admin y retornar sin persistir
  - _Requirements: 3.1, 3.2, 3.6, 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ]* 5.1 Escribir property test — Property 4: valores por defecto al cargar YAML con campos ausentes o cero
    - **Property 4: Valores por defecto al cargar YAML con campos ausentes o cero**
    - **Validates: Requirements 3.1, 3.2**

  - [ ]* 5.2 Escribir property test — Property 6: `getAvailableArena()` excluye arenas llenas
    - **Property 6: getAvailableArena excluye arenas llenas**
    - **Validates: Requirements 3.6**

  - [ ]* 5.3 Escribir property test — Property 9: round-trip guardar/cargar preserva valores
    - **Property 9: Arena válida se puede guardar y recargar con los mismos valores**
    - **Validates: Requirements 8.5**

  - [ ]* 5.4 Escribir unit test `saveGame_validArena_persists`
    - Verificar que una arena válida se persiste correctamente en YAML
    - _Requirements: 8.5_

- [x] 6. Corregir `EditorMenu`: límites mínimos en `MINIMUM_PLAYERS` y `MAXIMUM_PLAYERS`
  - En `MINIMUM_PLAYERS` click izquierdo: solo incrementar si `next <= arena.getMaxPlayers()`
  - En `MINIMUM_PLAYERS` click derecho: solo decrementar si `arena.getMinPlayers() > 1`
  - En `MAXIMUM_PLAYERS` click derecho: solo decrementar si `arena.getMaxPlayers() > 2`
  - En caso `SAVE`: pasar el jugador admin a `saveGame(arena, player)` para mostrar errores de validación
  - _Requirements: 3.3, 3.4, 3.5, 8.6_

  - [ ]* 6.1 Escribir property test — Property 5: invariante `minPlayers <= maxPlayers` en EditorMenu
    - **Property 5: Invariante minPlayers <= maxPlayers en EditorMenu**
    - **Validates: Requirements 3.3, 3.4, 3.5**

  - [ ]* 6.2 Escribir unit test `editorMenu_save_invokesValidator`
    - Verificar que al hacer SAVE se invoca el validador y se muestran errores al admin
    - _Requirements: 8.6_

- [ ] 7. Checkpoint — Verificar que todos los tests pasan
  - Asegurar que todos los tests pasan, consultar al usuario si surgen dudas.

- [x] 8. Implementar sistema de perks en `GamePlayer` y `Arena`
  - En `GamePlayer`: añadir campo `Map<Role, Perk> perks = new HashMap<>()` e implementar `setPerk(role, perk)` y `getPerk(role)` usando ese mapa
  - En `Arena.sendPlayersToGame()`: descomentar y corregir las líneas de entrega del perk — verificar `getPerk(role) != null` antes de poner el item en slot 4
  - _Requirements: 9.1, 9.2, 9.5_

  - [ ]* 8.1 Escribir property test — Property 10: perk por defecto es NONE cuando no hay perk equipado
    - **Property 10: Perk por defecto es NONE cuando no hay perk equipado**
    - **Validates: Requirements 9.5**

  - [ ]* 8.2 Escribir property test — Property 11: perk item en slot 4 tras `start()`
    - **Property 11: Perk item en slot 4 tras start()**
    - **Validates: Requirements 9.2**

- [x] 9. Implementar activación de perks en `PlayerInteractListener`
  - Añadir handler en `interactEvent()`: si el item en slot 4 coincide con el perk equipado del jugador, llamar `perk.usePerk(player)`
  - Aplicar tanto para `SURVIVOR` como para `SLENDER`
  - _Requirements: 9.3, 9.4_

  - [ ]* 9.1 Escribir unit test `perk_usePerk_calledOnRightClick`
    - Verificar que `usePerk()` es invocado al hacer click derecho en el item del perk
    - _Requirements: 9.3, 9.4_

- [ ] 10. Checkpoint final — Verificar que todos los tests pasan
  - Asegurar que todos los tests pasan, consultar al usuario si surgen dudas.

## Notes

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requisitos específicos para trazabilidad
- Los property tests usan **jqwik** (JUnit 5) con `@Property(tries = 100)` y **MockBukkit** para simular el entorno Bukkit
- Los checkpoints garantizan validación incremental antes de continuar
