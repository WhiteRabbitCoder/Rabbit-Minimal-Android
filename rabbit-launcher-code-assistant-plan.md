# Rabbit Launcher — Plan de desarrollo para asistente de código

> Objetivo: servir como guía operativa para un asistente de código tipo Claude Code o Codex que vaya a trabajar sobre un fork de **Focus Launcher** para convertirlo en **Rabbit Launcher**.

---

## 1) Resumen del proyecto

Rabbit Launcher es un launcher minimalista para Android con:
- Home centrado en información útil.
- Navegación por gestos desde Home.
- Pantalla de IA con mascota animada **Pix**.
- Widgets nativos compactos como Spotify y Pomodoro.
- Estética oscura, limpia y personalizable.
- Modo opcional pixel art.

La prioridad es construir una base funcional y luego expandirla sin perder simplicidad.

---

## 2) Objetivo del asistente de código

El asistente debe ayudar a:
1. Entender el estado actual del fork.
2. Identificar la arquitectura existente.
3. Proponer cambios pequeños y seguros.
4. Implementar la nueva UI sin romper el comportamiento base del launcher.
5. Mantener el código limpio, modular y fácil de iterar.
6. Evitar sobreingeniería.

El asistente no debe asumir que todo el launcher se rehace desde cero. La estrategia es **adaptar** la base existente.

---

## 3) Principios de trabajo

### 3.1 Prioridades
- Primero estabilidad.
- Luego claridad de arquitectura.
- Después personalización visual.
- Finalmente funciones avanzadas.

### 3.2 Reglas
- No introducir dependencias innecesarias.
- No refactorizar archivos grandes sin necesidad.
- No mezclar cambios visuales con cambios de lógica compleja si se pueden separar.
- No romper el flujo principal del launcher.
- No cambiar más de una responsabilidad importante por tarea.
- No agregar animaciones costosas si aún no existe la base funcional.

### 3.3 Forma de avanzar
Trabajar por fases pequeñas:
- inspección
- diseño de estructura
- implementación
- validación
- ajuste visual
- pruebas

---

## 4) Qué debe entender primero el asistente

Antes de escribir código, el asistente debe localizar y describir:

- Estructura general del proyecto.
- Punto de entrada de la app.
- Sistema de navegación actual.
- Pantallas existentes.
- Estado del Home.
- Cómo se renderizan listas, cards y elementos principales.
- Dónde viven temas, colores y tipografía.
- Cómo se gestionan datos locales.
- Qué partes están en Compose y cuáles no.
- Cómo se manejan gestos, navegación y estados.

Si el asistente no encuentra algo, debe decirlo explícitamente y proponer una forma segura de seguir.

---

## 5) Resultado esperado del fork

El fork debe evolucionar hacia estas pantallas / módulos:

1. **Home**
   - reloj grande
   - fecha
   - información contextual
   - apps frecuentes
   - widgets compactos
   - estilo oscuro limpio

2. **App Drawer**
   - lista vertical
   - índice alfabético
   - buscador
   - modo texto primero

3. **Búsqueda universal**
   - apps
   - settings
   - shortcuts
   - contactos
   - futuras extensiones

4. **AI Screen**
   - chat
   - accesos rápidos
   - mascota Pix animada
   - interacción con la IA

5. **Google Discovery**
   - integración nativa
   - sin reimplementación completa

---

## 6) Roadmap técnico recomendado

### Fase 1 — Auditoría del fork
Objetivo: entender el punto de partida.

Tareas:
- Mapear módulos y paquetes.
- Identificar pantallas existentes.
- Encontrar componentes reutilizables.
- Detectar dependencias críticas.
- Marcar código legado que conviene dejar intacto al inicio.

Entrega:
- Resumen de arquitectura actual.
- Lista de archivos clave.
- Riesgos técnicos.

---

### Fase 2 — Base visual Rabbit
Objetivo: transformar la estética sin romper funcionalidad.

Tareas:
- Cambiar temas y colores base.
- Ajustar tipografía.
- Crear estilo oscuro consistente.
- Preparar tokens visuales.
- Rediseñar Home con una composición más limpia.

Entrega:
- Home visualmente alineado con Rabbit Launcher.
- Estilos centralizados.

---

### Fase 3 — Home contextual
Objetivo: convertir el Home en la pantalla principal real del producto.

Tareas:
- Reloj grande.
- Fecha.
- Progreso del día.
- Progreso del año.
- Estado de batería y clima.
- Próximo evento.
- Apps más usadas.
- Widgets compactos.

Entrega:
- Home funcional y claro.
- Prioridad de información útil.

---

### Fase 4 — App Drawer y búsqueda
Objetivo: mejorar el acceso a apps sin ruido visual.

Tareas:
- Lista vertical por texto.
- Índice alfabético lateral.
- Toggle de orden alfabético / uso reciente.
- Buscador integrado.
- Resultados rápidos y fluidos.

Entrega:
- Drawer usable y rápido.
- Búsqueda consistente.

---

### Fase 5 — AI Screen
Objetivo: crear la experiencia diferencial.

Tareas:
- Pantalla de chat.
- Zona superior con accesos rápidos.
- Input de texto.
- Botón de voz.
- Estados de la mascota Pix.
- Soporte para respuestas contextualizadas.

Entrega:
- AI Screen funcional.
- Estructura lista para integrar backend o API.

---

### Fase 6 — Pix
Objetivo: dar identidad emocional al producto.

Tareas:
- Crear el avatar pixel art.
- Definir estados:
  - idle
  - escuchando
  - pensando
  - respondiendo
  - celebrando
  - durmiendo
- Hacer que cambie según el estado de la IA.
- Mantener animaciones ligeras.

Entrega:
- Mascota integrada en la UI.
- Sistema de estados reutilizable.

---

### Fase 7 — Widgets nativos
Objetivo: dar utilidad sin salir del launcher.

Tareas:
- Widget de música.
- Widget de Pomodoro.
- Zona flexible para widgets libres.
- Soporte de render simple y compacto.

Entrega:
- Home más funcional.
- Menos necesidad de apps externas.

---

## 7) Cómo debe trabajar el asistente en cada tarea

### Formato recomendado por tarea
Para cada cambio, el asistente debe responder internamente con esta secuencia:

1. **Qué archivo o módulo tocará**
2. **Qué problema resuelve**
3. **Qué riesgo introduce**
4. **Cómo lo validará**
5. **Qué dejará para después**

### Regla práctica
Si una tarea no se puede terminar en un solo cambio pequeño, dividirla.

Ejemplo:
- primero estructura
- luego UI
- luego animación
- luego integración

---

## 8) Prompt operativo para el asistente de código

Este texto puede pegarse tal cual como instrucción base del agente:

> Eres un asistente de desarrollo trabajando sobre un fork de Focus Launcher para convertirlo en Rabbit Launcher. Tu prioridad es entender la base existente antes de modificarla. Mantén los cambios pequeños, seguros y modulares. No rompas el flujo principal del launcher. Si una parte del proyecto no está clara, inspecciónala primero y explica lo que encontraste. Trabaja por fases: auditoría, base visual, Home contextual, drawer y búsqueda, AI Screen, mascota Pix y widgets nativos. Prefiere claridad sobre complejidad. Evita introducir dependencias innecesarias. Documenta decisiones importantes. Cuando propongas cambios, explica qué archivos tocarías, por qué, y cómo validarías que todo sigue funcionando.

---

## 9) Checklist de exploración inicial

El asistente debe responder a estas preguntas antes de tocar diseño importante:

- ¿Cómo arranca la app?
- ¿Dónde está la navegación principal?
- ¿Cómo se renderiza el Home?
- ¿Existe un sistema de temas centralizado?
- ¿Cómo se modelan apps y favoritos?
- ¿Cómo se dibuja el drawer?
- ¿Cómo se gestiona la búsqueda?
- ¿Qué soporta ya la base sobre gestos?
- ¿Qué partes son reutilizables para Rabbit?
- ¿Qué parte conviene dejar intacta por ahora?

---

## 10) Cambios visuales recomendados para Rabbit

### Dirección de UI
- Fondo negro o muy oscuro.
- Texto claro y grande.
- Mucho espacio vacío.
- Tarjetas suaves y discretas.
- Acents verdes o menta.
- Transiciones suaves.
- Estética minimalista con opción pixel art.

### Componentes visuales
- Reloj principal protagonista.
- Apps frecuentes sin iconos por defecto.
- Tarjetas compactas para widgets.
- Pix visible solo donde aporta valor.
- AI Screen con jerarquía clara.

### Anti-patrones
- Saturar la pantalla con iconos.
- Meter demasiados colores.
- Llenar el Home de elementos decorativos.
- Introducir sombras agresivas.
- Ocultar información útil.

---

## 11) Reglas para la mascota Pix

Pix no es un adorno. Pix es parte de la experiencia.

### Debe:
- reaccionar a la IA.
- tener estados claros.
- aportar identidad.
- ser simple de renderizar.
- funcionar bien en pixel art.

### No debe:
- distraer demasiado.
- ocupar toda la pantalla.
- depender de animaciones pesadas.
- romper legibilidad del chat.

### Estados mínimos
- Idle.
- Escuchando.
- Pensando.
- Respondiendo.
- Celebrando.
- Durmiendo.

---

## 12) Alcance del MVP

### MVP 1
- Fork funcional.
- Home rediseñado.
- Drawer usable.
- Búsqueda básica.
- AI Screen mock funcional.
- Pix estático o con animación simple.

### MVP 2
- Contextual data en Home.
- Uso de apps frecuentes.
- Widget de Pomodoro.
- Widget de música.
- Pix por estados.
- Integración IA real.

### MVP 3
- Refinamiento visual.
- Personalización.
- Más widgets.
- Mejor integración de hábitos y tiempo.

---

## 13) Criterios de calidad

El asistente debe considerar que el cambio está bien hecho si:

- El launcher sigue abriendo y navegando correctamente.
- El Home se siente limpio y útil.
- La UI mantiene coherencia visual.
- El código agregado se entiende sin esfuerzo.
- Las nuevas piezas son reutilizables.
- Pix no rompe el rendimiento.
- Las pantallas nuevas no dependen de hacks innecesarios.

---

## 14) Qué hacer si aparece una duda técnica

Si el asistente encuentra una incertidumbre, debe:

1. Decir qué no está claro.
2. Mostrar dónde está mirando.
3. Proponer 1 o 2 opciones.
4. Elegir la más segura si debe continuar.

No debe improvisar si eso compromete la arquitectura.

---

## 15) Orden sugerido de implementación

1. Inspeccionar el fork.
2. Entender la navegación.
3. Centralizar tema y tokens visuales.
4. Rediseñar Home.
5. Implementar drawer y búsqueda.
6. Crear AI Screen base.
7. Integrar Pix.
8. Añadir widgets nativos.
9. Pulir animaciones.
10. Preparar para integración de IA real.

---

## 16) Nota para mantener el formato Markdown seguro

Este documento está escrito para evitar problemas típicos de escape:
- No usar comillas innecesarias.
- No mezclar bloques de código con texto plano sin necesidad.
- No depender de caracteres escapados para explicar el contenido.
- Mantener listas simples y títulos claros.

---

## 17) Mensaje final para el asistente

Construye Rabbit Launcher como un producto con identidad propia, pero sin pelear contra la base existente. El objetivo no es rehacer todo. El objetivo es convertir un launcher funcional en una experiencia calmada, inteligente y memorable.

---

## 18) Próximo archivo recomendado

Después de este documento, el siguiente archivo útil sería uno de estos:

- `architecture-notes.md`
- `implementation-roadmap.md`
- `assistant-prompt.md`
- `task-breakdown.md`

Ese siguiente archivo debería entrar ya en detalles de:
- carpetas
- componentes
- estados
- prioridades
- tareas del primer sprint
