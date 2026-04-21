## Análisis del sistema

### 1. ¿Qué problema resuelve?

FoodFlow resuelve la **desorganización operativa de locales gastronómicos** (bares, cafeterías, parrillas, pizzerías) que gestionan pedidos en papel, calculan cuentas manualmente, pierden el control de promociones y no tienen visibilidad sobre el cierre de caja diario. Elimina errores de cálculo, reduce tiempos de atención y le da al dueño/mozo una herramienta digital para operar su local completo desde una sola pantalla.

### 2. ¿Qué tipo de aplicación es?

Es un **sistema POS (Point of Sale) + gestión de comandas** pensado como SaaS multi-tenant. Combina la operación de salón (mesas, pedidos, cocina), la administración de catálogo/promociones, y el cierre contable diario en una sola plataforma.

### 3. Funcionalidades principales

- **Gestión de salón**: visualización de mesas en tiempo real, apertura/cierre, estados claros (libre/ocupada)
- **Comanda digital**: carga rápida de productos con variantes, extras, observaciones libres ("sin cebolla"), envío a cocina con ticket imprimible
- **Motor de promociones automáticas**: 2×1, combos condicionales ("comprá hamburguesa, gaseosa al 50%"), packs, descuentos por porcentaje o monto fijo, con triggers temporales (horarios, días), por contenido del pedido o monto mínimo
- **Descuentos manuales**: por ítem individual o sobre el total, con registro de razón y auditoría
- **Catálogo de productos**: categorías con color visual, variantes (Simple/Doble/Triple), extras configurables, control de stock
- **Cierre de mesa con múltiples medios de pago**: efectivo, tarjeta, transferencia, QR, cuenta corriente
- **Jornada de caja**: apertura con fondo inicial, registro de egresos/ingresos manuales, cierre con balance automático, reporte PDF descargable
- **Reapertura de pedidos**: permite corregir errores sin perder trazabilidad
- **Historial y reportes**: ventas por producto, historial de jornadas, pedidos cerrados consultables

### 4. Reglas de negocio destacables

- **El total siempre se calcula desde los ítems base + descuentos acumulables, nunca al revés** — garantiza consistencia histórica
- **Snapshot de precios**: al agregar un producto al pedido, se congela el precio de ese momento. Cambios futuros de catálogo no alteran pedidos existentes
- **Motor de reglas con prioridades**: si dos promociones aplican al mismo producto, gana la de mayor prioridad. Se evalúan con lógica AND entre criterios
- **Descuentos en capas**: primero promociones automáticas (inmutables), luego descuentos manuales por ítem, luego descuento global — en ese orden, sin conflictos
- **Una mesa = máximo un pedido abierto simultáneamente**
- **Jornada de caja con fecha operativa inteligente**: si el local cierra a las 3 AM, la jornada sigue siendo del día anterior (hora de corte configurable a las 06:00)
- **Los extras nunca participan del cálculo de descuentos de promociones** — se suman siempre al total

### 5. Partes técnicamente complejas

- El **motor de reglas de promociones** es el componente más sofisticado: evalúa 4 estrategias distintas × 3 tipos de triggers × alcance por producto o categoría, con resolución de conflictos por prioridad y recálculo automático cuando se modifica un pedido
- El **recálculo de promociones** al eliminar o modificar ítems: limpia todos los descuentos y los re-evalúa desde cero para mantener consistencia
- La **arquitectura de snapshots contables**: al cerrar un pedido se congela subtotal, descuentos y total como datos inmutables para auditoría
- El **cierre de caja** con balance automático (fondo + ventas + ingresos - egresos)

### 6. Arquitectura

- **Arquitectura hexagonal** con 4 capas: dominio (puro, sin frameworks), aplicación (orquestación), infraestructura (JPA, persistencia), presentación (REST)
- **Domain-Driven Design** con aggregates definidos (Pedido, Promoción, Mesa, JornadaCaja), value objects inmutables, excepciones de dominio explícitas
- **Multi-tenancy por fila**: aislamiento total de datos entre locales
- **~40 casos de uso** como unidades funcionales independientes
- **~320 tests automatizados** de dominio y casos de uso

### 7. Tecnologías

- **Backend**: Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Flyway
- **Frontend**: React, TypeScript, Tailwind CSS, React Query, Zustand, Vite
- **Desktop**: Tauri (empaquetado nativo)
- **Infraestructura**: Docker Compose, PostgreSQL

### 8. Valor que aporta

- **Elimina errores de cálculo** en cuentas y promociones — todo se calcula automáticamente
- **Reduce tiempo de atención** — la carga de productos es con botonera visual
- **Automatiza promociones** — el mozo no necesita recordar ofertas, el sistema las aplica solo
- **Trazabilidad completa** — cada descuento, pago y movimiento de caja queda registrado
- **Cierre de caja confiable** — balance automático con reporte descargable, sin cálculos manuales
- **Consistencia histórica** — los pedidos pasados siempre reflejan los precios reales del momento

### 9. Rol del desarrollador

Responsable único de diseño, arquitectura, desarrollo full-stack, modelado de dominio, testing y documentación del sistema completo.

---

## Entregable para portfolio

### A. Títulos posibles

1. **FoodFlow — Sistema de Comandas y Punto de Venta para Gastronomía**
2. **FoodFlow — Plataforma de Gestión Operativa para Locales Gastronómicos**
3. **FoodFlow — POS con Motor de Promociones para Bares y Restaurantes**

---

### B. Descripción corta

Sistema de comandas y punto de venta diseñado para locales gastronómicos medianos. Permite gestionar mesas, pedidos, promociones automáticas, múltiples medios de pago y cierre de caja diario desde una sola interfaz táctil.

---

### C. Descripción completa (orientada a clientes)

FoodFlow es una plataforma que digitaliza la operación diaria de bares, cafeterías, parrillas y restaurantes medianos.

El sistema permite al mozo o encargado **ver el estado del salón en tiempo real**, abrir mesas, cargar pedidos de forma rápida con una botonera visual de productos, y enviar la comanda a cocina con un solo toque. Los productos se organizan por categorías con colores personalizados, y soportan variantes (por ejemplo, hamburguesa simple, doble o triple) y extras configurables (bacon, huevo, etc.).

Uno de los componentes más potentes es el **motor de promociones automáticas**. El dueño del local puede crear reglas como "2×1 en cervezas los viernes de noche", "comprás una hamburguesa y la gaseosa sale al 50%" o "2 empanadas por $3.000". El sistema aplica estas promociones automáticamente al cargar el pedido — sin que el mozo tenga que recordarlas ni calcularlas. También permite aplicar descuentos manuales cuando sea necesario, con registro del motivo.

Al cerrar una mesa, el sistema calcula el total final considerando todos los descuentos acumulados, y permite cobrar con **múltiples medios de pago** en una misma cuenta (efectivo, tarjeta, transferencia, QR o cuenta corriente). Si hubo un error, el pedido puede reabrirse y corregirse sin perder la información original.

El módulo de **caja** permite abrir la jornada declarando el fondo inicial, registrar egresos e ingresos manuales durante el día, y al cierre genera un **balance automático** con reporte PDF descargable: cuánto entró, cuánto salió, cuántos pedidos se cerraron y cuál es el saldo esperado en caja.

Todo el sistema está diseñado para funcionar en **pantallas táctiles** y hardware modesto, con una interfaz minimalista en rojo y negro que prioriza la velocidad de uso. Los datos de cada local están completamente aislados, preparando el sistema para funcionar como plataforma multi-local.

---

### D. Responsabilidades

- Diseño de arquitectura del sistema completo (backend + frontend + base de datos)
- Modelado del dominio gastronómico con reglas de negocio validadas con operadores reales
- Desarrollo del motor de promociones con 4 estrategias, 3 tipos de triggers y resolución automática de conflictos
- Implementación de API REST con +30 endpoints documentados
- Diseño del modelo de datos relacional con migraciones versionadas
- Desarrollo de interfaz POS táctil con React y TypeScript
- Módulo de cierre de caja con cálculo de balance automático y reportes PDF
- Sistema de snapshots contables para garantizar integridad de datos históricos
- Estrategia multi-tenant con aislamiento de datos por local
- Escritura de +320 tests automatizados de dominio y casos de uso
- Documentación técnica y funcional del sistema

---

### E. Tecnologías

| Área | Tecnologías |
|------|------------|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL |
| Frontend | React, TypeScript, Tailwind CSS, React Query, Zustand |
| Escritorio | Tauri (empaquetado nativo multiplataforma) |
| Testing | JUnit 5, Mockito (+320 tests) |
| Infraestructura | Docker Compose, Flyway (migraciones SQL) |
| Arquitectura | Hexagonal, Domain-Driven Design, TDD |

---

### F. Resultado / Impacto

- **Eliminación de errores de cálculo** en cuentas y promociones: todo se calcula automáticamente con reglas validadas
- **Reducción del tiempo de atención por mesa**: la carga de productos y el cierre se resuelven en pocos toques
- **Cero dependencia de la memoria del mozo** para aplicar ofertas: el motor de promociones actúa solo
- **Cierre de caja confiable en minutos**: balance automático con desglose completo, sin cálculos manuales en cuaderno
- **Trazabilidad total**: cada pedido, descuento, pago y movimiento de caja queda registrado y es consultable
- **Datos históricos consistentes**: un pedido del mes pasado siempre refleja los precios y promociones que efectivamente se aplicaron
- **Preparado para escalar**: la arquitectura multi-tenant y el diseño modular permiten incorporar nuevos locales sin modificar el sistema

--- 

prompt: 
Este es un sistema real que desarrollé como backend principal.
Actúa como un desarrollador senior y analista de producto.

Analiza este repositorio completo y extrae toda la información necesaria para crear una descripción profesional de portfolio orientada a clientes freelance (no a desarrolladores).

Quiero que identifiques y expliques:

1. ¿Qué problema resuelve el sistema?
2. ¿Qué tipo de aplicación es? (ej: sistema de gestión de pedidos, POS, etc.)
3. ¿Cuáles son las principales funcionalidades?
4. ¿Qué reglas de negocio importantes implementa? (ej: cálculos, validaciones, lógica compleja)
5. ¿Qué partes del sistema son más complejas o destacables técnicamente?
6. ¿Qué arquitectura utiliza? (capas, DDD, etc.)
7. ¿Qué tecnologías usa?
8. ¿Qué valor aporta el sistema? (eficiencia, reducción de errores, automatización, etc.)
9. ¿Qué rol cumple el desarrollador en este proyecto? (asumir que fui el principal responsable)

Luego, con esa información, genera:

A. 3 posibles títulos para portfolio (claros y profesionales)
B. Una descripción corta (2–3 líneas)
C. Una descripción completa orientada a clientes (no muy técnica)
D. Una lista de responsabilidades (bullet points)
E. Una lista de tecnologías
F. Una sección de "resultado" o impacto del sistema

IMPORTANTE:
- No describas código ni clases directamente
- Traducí lo técnico a valor de negocio
- Escribí de forma clara, profesional y natural (no robótica)
- Pensá que lo va a leer un cliente que no sabe programar

El objetivo es que el resultado pueda usarse directamente en Freelancer o LinkedIn.
#codebase 