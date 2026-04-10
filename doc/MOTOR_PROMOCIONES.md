# 🔥 Motor de Promociones — FoodFlow

> **Resumen Ejecutivo Técnico**  
> Última actualización: Febrero 2026  
> Scope: Promociones automáticas (HU-08/09/10), Descuentos manuales (HU-14), Recálculo (HU-20/21)

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Arquitectura del Motor](#2-arquitectura-del-motor)
3. [Modelo de Dominio](#3-modelo-de-dominio)
4. [Estrategias de Promoción (Beneficio)](#4-estrategias-de-promoción-beneficio)
5. [Criterios de Activación (Triggers)](#5-criterios-de-activación-triggers)
6. [Sistema de Alcance (Scope): Triggers vs Targets](#6-sistema-de-alcance-scope-triggers-vs-targets)
7. [Flujo de Evaluación Completo](#7-flujo-de-evaluación-completo)
8. [Descuentos Manuales](#8-descuentos-manuales)
9. [Patrón Snapshot](#9-patrón-snapshot)
10. [Recálculo de Promociones (HU-20/21)](#10-recálculo-de-promociones-hu-2021)
11. [Prioridades y Resolución de Conflictos](#11-prioridades-y-resolución-de-conflictos)
12. [API REST — Endpoints](#12-api-rest--endpoints)
13. [Modelo de Persistencia](#13-modelo-de-persistencia)
14. [Guía Práctica: Configuración por Tipo](#14-guía-práctica-configuración-por-tipo)
15. [Matriz de Decisión: ¿Cuándo usar cada tipo?](#15-matriz-de-decisión-cuándo-usar-cada-tipo)
16. [Reglas de Negocio Críticas](#16-reglas-de-negocio-críticas)
17. [Diagrama de Clases Simplificado](#17-diagrama-de-clases-simplificado)

---

## 1. Visión General

El Motor de Promociones de FoodFlow es un sistema de **descuentos automáticos** diseñado para locales gastronómicos. Su responsabilidad es evaluar, en tiempo real, qué beneficio aplica cuando un producto se agrega a un pedido.

### Principios fundamentales

| Principio | Descripción |
|-----------|-------------|
| **Snapshot inmutable** | El descuento se calcula UNA vez al agregar el ítem. Cambios posteriores en la promo NO afectan ítems ya creados. |
| **El total se calcula desde los ítems** | `Total = Σ (precioUnitario × cantidad - montoDescuento) + extras`. Nunca al revés. |
| **Promociones ≠ Descuentos Manuales** | Dos mecanismos independientes. Las promos son automáticas, los descuentos manuales son decisión del operador. |
| **Extras aislados** | Los extras (agregados a un producto) NUNCA participan en el cálculo de descuentos de promociones. |
| **Prioridad resuelve conflictos** | Si múltiples promos aplican al mismo producto, gana la de mayor prioridad. |

### Historias de Usuario cubiertas

| HU | Nombre | Estado |
|----|--------|--------|
| HU-08 | CRUD de Promociones | ✅ Completa |
| HU-09 | Asociar Productos a Promociones (Scope) | ✅ Completa |
| HU-10 | Aplicar Promociones Automáticamente | ✅ Completa |
| HU-14 | Descuento Manual Inmediato (Porcentaje) | ✅ Completa |
| HU-20 | Recálculo al Eliminar Ítem | ✅ Completa |
| HU-21 | Recálculo al Modificar Ítem | ✅ Completa |

---

## 2. Arquitectura del Motor

```
┌─────────────────────────────────────────────────────────────────────┐
│                       PRESENTATION LAYER                            │
│  PromocionController (REST)                                         │
│  POST /crear  │  PUT /editar  │  PUT /scope  │  DELETE /eliminar    │
└────────────────────────┬────────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────────┐
│                       APPLICATION LAYER                             │
│  CrearPromocionUseCase │ EditarPromocionUseCase │ AsociarScopeUseCase│
│  EliminarPromocionUseCase │ ConsultarPromocionUseCase               │
│  ListarPromocionesUseCase │ AplicarDescuentoManualUseCase           │
│                                                                     │
│  DTOs: CrearPromocionCommand, PromocionResponse, AsociarScopeCommand│
└────────────────────────┬────────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────────┐
│                        DOMAIN LAYER                                 │
│                                                                     │
│  ┌─────────────────┐   ┌──────────────────┐   ┌──────────────────┐ │
│  │   Promocion      │   │ EstrategiaPromo  │   │ CriterioActiv.  │ │
│  │  (Aggregate Root)│──▶│ (sealed iface)   │   │ (sealed iface)  │ │
│  └────────┬─────────┘   └──────────────────┘   └──────────────────┘ │
│           │                                                         │
│  ┌────────▼─────────┐   ┌──────────────────┐   ┌──────────────────┐ │
│  │ AlcancePromocion │   │ ItemPromocion    │   │ContextoValidacion│ │
│  │   (Value Object) │──▶│ (Value Object)   │   │   (Value Object) │ │
│  └──────────────────┘   └──────────────────┘   └──────────────────┘ │
│                                                                     │
│  ┌──────────────────┐   ┌──────────────────┐                       │
│  │ MotorReglasServ. │   │ DescuentoManual  │                       │
│  │ (Domain Service) │   │  (Value Object)  │                       │
│  └──────────────────┘   └──────────────────┘                       │
│                                                                     │
│  Interfaces: PromocionRepository                                    │
└────────────────────────┬────────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────────┐
│                      INFRASTRUCTURE LAYER                           │
│  PromocionJpaEntity │ PromocionScopeJpaEntity │ PromocionMapper     │
│  PromocionJpaRepository │ PromocionScopeJpaRepository               │
│  PromocionRepositoryImpl                                            │
└─────────────────────────────────────────────────────────────────────┘
```

### Patrones de diseño utilizados

| Patrón | Dónde se aplica | Para qué |
|--------|----------------|----------|
| **Sealed Interface** | `EstrategiaPromocion`, `CriterioActivacion` | Polimorfismo exhaustivo y type-safe. El compilador garantiza que todos los casos estén cubiertos. |
| **Specification / Composite Trigger** | `CriterioActivacion` + `puedeActivarse()` | Cada criterio evalúa su condición de forma independiente. La promoción compone N criterios con lógica AND. |
| **Snapshot** | `ItemPedido.montoDescuento`, `nombrePromocion`, `promocionId` | El descuento calculado se guarda como foto inmutable al momento de agregar el ítem. |
| **Builder** | `ContextoValidacion.builder()` | Facilita la construcción del contexto de evaluación con campos opcionales. |
| **Domain Service** | `MotorReglasService` | Lógica compleja que no pertenece a una sola entidad: evalúa promos, resuelve conflictos, calcula descuentos. |

---

## 3. Modelo de Dominio

### Promocion (Aggregate Root)

La `Promocion` es la entidad raíz. Contiene toda la información necesaria para definir, evaluar y aplicar un beneficio.

```
Promocion
├── id: PromocionId (UUID)
├── localId: LocalId (UUID, multi-tenancy)
├── nombre: String (único por local)
├── descripcion: String (nullable)
├── prioridad: int (>= 0, mayor = más importante)
├── estado: EstadoPromocion (ACTIVA | INACTIVA)
├── estrategia: EstrategiaPromocion → "¿QUÉ beneficio otorgo?"
├── triggers: List<CriterioActivacion> → "¿CUÁNDO aplico?" (AND logic)
└── alcance: AlcancePromocion → "¿A QUÉ productos aplico?" (TRIGGER/TARGET)
```

### Enumeraciones del dominio

```java
TipoEstrategia   → DESCUENTO_DIRECTO | CANTIDAD_FIJA | COMBO_CONDICIONAL | PRECIO_FIJO_CANTIDAD
ModoDescuento     → PORCENTAJE | MONTO_FIJO
EstadoPromocion   → ACTIVA | INACTIVA
TipoCriterio      → TEMPORAL | CONTENIDO | MONTO_MINIMO
TipoAlcance       → PRODUCTO | CATEGORIA
RolPromocion      → TRIGGER | TARGET
```

### Separación de responsabilidades — Las 3 preguntas

Cada componente del aggregate responde una pregunta distinta:

| Componente | Pregunta que responde | Ejemplo |
|------------|----------------------|---------|
| **Triggers** (criterios de activación) | "¿CUÁNDO y BAJO QUÉ CONDICIONES aplico?" | "Solo los martes de 18 a 22hs, si el pedido supera $10.000" |
| **Estrategia** (beneficio) | "¿QUÉ BENEFICIO otorgo?" | "20% de descuento", "2x1", "Pack de 2 por $22.000" |
| **Alcance** (scope) | "¿A QUÉ PRODUCTOS aplico?" | "A las empanadas (TARGET), activada por hamburguesas (TRIGGER)" |

---

## 4. Estrategias de Promoción (Beneficio)

Las estrategias están modeladas como una **sealed interface** con 4 implementaciones `record` (inmutables, validadas en construcción).

### 4.1. DESCUENTO_DIRECTO

> Aplica un porcentaje o un monto fijo de descuento directo sobre el producto target.

**Parámetros:**
- `modo`: `PORCENTAJE` | `MONTO_FIJO`
- `valor`: El porcentaje (0.01–100) o el monto fijo (> 0)

**Validaciones en construcción:**
- `valor` > 0
- Si `modo = PORCENTAJE`: `valor` ≤ 100

#### Ejemplo 1: Porcentaje

> "20% de descuento en empanadas"

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `DESCUENTO_DIRECTO` |
| `modo` | `PORCENTAJE` |
| `valor` | `20` |

**Cálculo:** Si una empanada cuesta $2.000 y el cliente pide 3:
```
subtotal    = $2.000 × 3 = $6.000
descuento   = $6.000 × 20 / 100 = $1.200
total ítem  = $6.000 - $1.200 = $4.800
```

#### Ejemplo 2: Monto fijo

> "$500 de descuento en pizza grande"

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `DESCUENTO_DIRECTO` |
| `modo` | `MONTO_FIJO` |
| `valor` | `500` |

**Cálculo:** Si una pizza cuesta $5.000 y el cliente pide 2:
```
subtotal    = $5.000 × 2 = $10.000
descuento   = $500 × 2 = $1.000  (se aplica por unidad)
total ítem  = $10.000 - $1.000 = $9.000
```

> ⚠️ El descuento por monto fijo nunca puede superar el subtotal del ítem.

---

### 4.2. CANTIDAD_FIJA (NxM)

> Llevás N unidades, pagás M. Las unidades "gratis" se calculan por ciclos completos.

**Parámetros:**
- `cantidadLlevas`: N (>= 1)
- `cantidadPagas`: M (>= 1)
- **Invariante:** `cantidadLlevas` > `cantidadPagas` (si no, no hay beneficio)

#### Ejemplo 1: 2×1

> "2×1 en cervezas artesanales"

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `CANTIDAD_FIJA` |
| `cantidadLlevas` | `2` |
| `cantidadPagas` | `1` |

**Cálculo:** Cerveza a $3.000:

| Cantidad | Ciclos | Unidades gratis | Descuento |
|----------|--------|-----------------|-----------|
| 1 | 0 | 0 | $0 |
| 2 | 1 | 1 | $3.000 |
| 3 | 1 | 1 | $3.000 |
| 4 | 2 | 2 | $6.000 |
| 5 | 2 | 2 | $6.000 |
| 6 | 3 | 3 | $9.000 |

**Fórmula:**
```
ciclosCompletos = cantidad ÷ cantidadLlevas  (división entera)
unidadesGratis  = ciclosCompletos × (cantidadLlevas - cantidadPagas)
descuento       = precioBase × unidadesGratis
```

#### Ejemplo 2: 3×2

> "3×2 en empanadas"

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `CANTIDAD_FIJA` |
| `cantidadLlevas` | `3` |
| `cantidadPagas` | `2` |

**Cálculo:** Empanada a $2.000:

| Cantidad | Ciclos | Unidades gratis | Descuento |
|----------|--------|-----------------|-----------|
| 1 | 0 | 0 | $0 |
| 2 | 0 | 0 | $0 |
| 3 | 1 | 1 | $2.000 |
| 4 | 1 | 1 | $2.000 |
| 6 | 2 | 2 | $4.000 |

---

### 4.3. COMBO_CONDICIONAL

> "Si comprás X (trigger), obtenés Y (target) con Z% de descuento."

Es la **única estrategia que requiere productos TRIGGER en el alcance** (las otras solo necesitan TARGETs).

**Parámetros:**
- `cantidadMinimaTrigger`: Cuántas unidades del trigger deben estar en el pedido (>= 1)
- `porcentajeBeneficio`: El % de descuento que se aplica sobre el target (0.01–100)

**Validaciones en construcción:**
- `cantidadMinimaTrigger` >= 1
- `porcentajeBeneficio` > 0 y <= 100

#### Ejemplo: Hamburguesa + Gaseosa

> "Si comprás 1 hamburguesa, la gaseosa tiene 50% off"

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `COMBO_CONDICIONAL` |
| `cantidadMinimaTrigger` | `1` |
| `porcentajeBeneficio` | `50` |

**Alcance (Scope):**
```json
{
  "items": [
    { "referenciaId": "uuid-hamburguesa", "tipo": "PRODUCTO", "rol": "TRIGGER" },
    { "referenciaId": "uuid-gaseosa",     "tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

**Cálculo:** Hamburguesa $8.000, Gaseosa $2.000:
```
1. Cliente agrega hamburguesa → no hay descuento (es TRIGGER, no TARGET)
2. Cliente agrega gaseosa → MotorReglas detecta:
   - ¿Hay ≥ 1 hamburguesa en el pedido? SÍ ✅
   - ¿La gaseosa es TARGET? SÍ ✅
   - Descuento = $2.000 × 50 / 100 = $1.000
   - La gaseosa se cobra $1.000 en vez de $2.000
```

> ⚠️ **El descuento se aplica sobre el SUBTOTAL del target**, no del trigger.

#### Ejemplo 2: Torta + Bebida

> "Comprando torta, cualquier licuado tiene 30% off"

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `COMBO_CONDICIONAL` |
| `cantidadMinimaTrigger` | `1` |
| `porcentajeBeneficio` | `30` |

**Alcance (Scope):**
```json
{
  "items": [
    { "referenciaId": "uuid-torta", "tipo": "PRODUCTO", "rol": "TRIGGER" },
    { "referenciaId": "uuid-licuado-frutilla", "tipo": "PRODUCTO", "rol": "TARGET" },
    { "referenciaId": "uuid-licuado-banana", "tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

---

### 4.4. PRECIO_FIJO_CANTIDAD (Pack)

> N unidades por un precio especial. Las unidades que no completan un ciclo se cobran a precio normal.

**Parámetros:**
- `cantidadActivacion`: Cuántas unidades forman el pack (>= 2)
- `precioPaquete`: Precio total del pack (> 0)

**Validaciones en construcción:**
- `cantidadActivacion` >= 2 (mínimo para ser un "pack")
- `precioPaquete` > 0

#### Ejemplo: 2 Hamburguesas por $22.000

> Precio base de la hamburguesa: $13.000 c/u

| Campo | Valor |
|-------|-------|
| `tipoEstrategia` | `PRECIO_FIJO_CANTIDAD` |
| `cantidadActivacion` | `2` |
| `precioPaquete` | `22000` |

**Cálculo:**

| Cantidad | Ciclos | Costo sin promo | Costo con promo | Descuento |
|----------|--------|-----------------|-----------------|-----------|
| 1 | 0 | $13.000 | $13.000 | $0 |
| 2 | 1 | $26.000 | $22.000 | $4.000 |
| 3 | 1 | $26.000 + $13.000 | $22.000 + $13.000 | $4.000 |
| 4 | 2 | $52.000 | $44.000 | $8.000 |

**Fórmula:**
```
ciclos         = cantidad ÷ cantidadActivacion  (división entera)
costoSinPromo  = ciclos × cantidadActivacion × precioUnitario
costoConPromo  = ciclos × precioPaquete
descuento      = costoSinPromo - costoConPromo
```

> ⚠️ Si `precioPaquete` >= `cantidadActivacion × precioUnitario`, el descuento es $0 (la promo está mal configurada, pero el sistema no rompe).

---

## 5. Criterios de Activación (Triggers)

Los triggers son las **condiciones que deben cumplirse** para que una promoción se evalúe. Están modelados como una sealed interface `CriterioActivacion` con 3 implementaciones.

### 🔑 Regla fundamental: Lógica AND

> Si una promoción tiene N triggers, **TODOS** deben satisfacerse simultáneamente.

```java
// Promocion.puedeActivarse()
return triggers.stream()
    .allMatch(trigger -> trigger.esSatisfechoPor(contexto));
```

### 5.1. TEMPORAL

Valida que el momento actual esté dentro de un rango temporal.

**Parámetros:**
- `fechaDesde` (obligatorio): Fecha de inicio de vigencia
- `fechaHasta` (obligatorio): Fecha de fin de vigencia
- `diasSemana` (opcional): Set de días permitidos. Si se omite → todos los días
- `horaDesde` (opcional): Hora de inicio. Si se omite → todo el día
- `horaHasta` (opcional): Hora de fin. Si se omite → todo el día

**Validaciones:**
- `fechaDesde` ≤ `fechaHasta`
- Si ambos horarios presentes: `horaDesde` < `horaHasta`

**Algoritmo de evaluación:**
```
1. ¿La fecha actual está entre fechaDesde y fechaHasta? → Si no, FALSE
2. ¿El día de la semana actual está en diasSemana?     → Si no, FALSE
3. ¿Hay horario definido?
   a. SÍ: ¿La hora actual está entre horaDesde y horaHasta? → Si no, FALSE
   b. NO: Aplica todo el día
4. TRUE
```

#### Ejemplo: Happy Hour

```json
{
  "tipo": "TEMPORAL",
  "fechaDesde": "2026-01-01",
  "fechaHasta": "2026-12-31",
  "diasSemana": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "horaDesde": "18:00",
  "horaHasta": "21:00"
}
```

#### Ejemplo: Promo de fin de semana todo el día

```json
{
  "tipo": "TEMPORAL",
  "fechaDesde": "2026-03-01",
  "fechaHasta": "2026-03-31",
  "diasSemana": ["SATURDAY", "SUNDAY"]
}
```

> Sin `horaDesde`/`horaHasta` = aplica las 24 horas.

---

### 5.2. CONTENIDO

Valida que determinados productos estén presentes en el pedido actual.

**Parámetros:**
- `productosRequeridos`: Set de UUIDs de productos (mínimo 1)

**Algoritmo de evaluación:**
```
TODOS los productos requeridos deben estar en el pedido actual.
```

> ⚠️ Este trigger valida **presencia**, no cantidad. Para validar cantidad mínima del trigger en un combo, se usa la estrategia `COMBO_CONDICIONAL` con su campo `cantidadMinimaTrigger`.

#### Ejemplo

```json
{
  "tipo": "CONTENIDO",
  "productosRequeridos": ["uuid-hamburguesa-clasica"]
}
```

> "Esta promo solo aplica si el pedido ya contiene una hamburguesa clásica."

---

### 5.3. MONTO_MINIMO

Valida que el subtotal del pedido supere un umbral.

**Parámetros:**
- `montoMinimo`: BigDecimal > 0

**Algoritmo de evaluación:**
```
¿totalPedido >= montoMinimo? → TRUE/FALSE
```

#### Ejemplo

```json
{
  "tipo": "MONTO_MINIMO",
  "montoMinimo": 15000
}
```

> "Esta promo solo aplica si el pedido supera los $15.000."

---

### Combinaciones de Triggers (AND)

Una promoción puede combinar múltiples triggers. Todos deben cumplirse.

#### Ejemplo compuesto

> "20% en postres, solo los viernes de 20 a 23hs, si el pedido supera $10.000"

```json
{
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31",
      "diasSemana": ["FRIDAY"],
      "horaDesde": "20:00",
      "horaHasta": "23:00"
    },
    {
      "tipo": "MONTO_MINIMO",
      "montoMinimo": 10000
    }
  ]
}
```

Evaluación: `CriterioTemporal.esSatisfechoPor(ctx) AND CriterioMontoMinimo.esSatisfechoPor(ctx)` → ambos deben ser `true`.

---

## 6. Sistema de Alcance (Scope): Triggers vs Targets

El **alcance** (`AlcancePromocion`) define **qué productos participan** en la promoción y **con qué rol**.

### Los dos roles

| Rol | Significado | ¿Quién lo necesita? |
|-----|------------|---------------------|
| **TARGET** | Producto que **recibe el beneficio** (descuento). | **Todas** las estrategias |
| **TRIGGER** | Producto cuya **presencia activa** el beneficio en otro. | **Solo** `COMBO_CONDICIONAL` |

### Tipos de referencia

| Tipo | Significado |
|------|------------|
| `PRODUCTO` | Referencia directa a un producto por UUID |
| `CATEGORIA` | Referencia a una categoría completa (futuro, no implementado en MVP) |

### Modelo

```
AlcancePromocion
└── items: List<ItemPromocion>
    ├── ItemPromocion
    │   ├── id: ItemPromocionId
    │   ├── referenciaId: UUID (producto o categoría)
    │   ├── tipo: TipoAlcance (PRODUCTO | CATEGORIA)
    │   └── rol: RolPromocion (TRIGGER | TARGET)
    ├── ItemPromocion
    │   └── ...
    └── ...
```

### Validaciones del alcance

- No se permiten **duplicados** de `referenciaId` dentro del mismo alcance
- Cada producto referenciado debe **existir** y **pertenecer al mismo local** (validado en el use case)

---

### ¿Cuándo necesito TRIGGERS en el scope?

#### ✅ NECESARIO: Solo para `COMBO_CONDICIONAL`

La estrategia combo requiere saber:
1. **TRIGGER**: "¿Qué producto activa la promo?" → Hamburguesa
2. **TARGET**: "¿Qué producto recibe el descuento?" → Gaseosa

```json
{
  "items": [
    { "referenciaId": "uuid-hamburguesa", "tipo": "PRODUCTO", "rol": "TRIGGER" },
    { "referenciaId": "uuid-gaseosa",     "tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

**El MotorReglasService verifica:**
```
1. ¿El producto que se está agregando es TARGET de esta promo? → SÍ (gaseosa)
2. ¿Hay al menos cantidadMinimaTrigger unidades del TRIGGER en el pedido? → SÍ (hay 1 hamburguesa)
3. Entonces: aplicar porcentajeBeneficio sobre la gaseosa
```

#### ❌ NO NECESARIO: Para las otras 3 estrategias

Para `DESCUENTO_DIRECTO`, `CANTIDAD_FIJA` y `PRECIO_FIJO_CANTIDAD`, solo se necesitan **TARGETs**:

```json
{
  "items": [
    { "referenciaId": "uuid-cerveza", "tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

> Si se definen TRIGGERs para una estrategia que no es COMBO_CONDICIONAL, simplemente se ignoran.

### Tabla resumen: ¿Qué necesita cada estrategia?

| Estrategia | ¿Necesita TRIGGERs? | ¿Necesita TARGETs? | Ejemplo de alcance |
|-----------|---------------------|---------------------|-------------------|
| `DESCUENTO_DIRECTO` | ❌ No | ✅ Sí (obligatorio) | Solo TARGET: empanadas |
| `CANTIDAD_FIJA` | ❌ No | ✅ Sí (obligatorio) | Solo TARGET: cervezas |
| `COMBO_CONDICIONAL` | ✅ **Sí (obligatorio)** | ✅ Sí (obligatorio) | TRIGGER: hamburguesa, TARGET: gaseosa |
| `PRECIO_FIJO_CANTIDAD` | ❌ No | ✅ Sí (obligatorio) | Solo TARGET: hamburguesas |

> ⚠️ **Sin TARGETs definidos, la promoción nunca se aplica.** El MotorReglasService verifica `alcance.tieneTargets()` y `alcance.esProductoTarget(productoId)` antes de evaluar.

---

## 7. Flujo de Evaluación Completo

Cuando un producto se agrega a un pedido, el `MotorReglasService.aplicarReglas()` ejecuta el siguiente algoritmo:

```
┌─────────────────────────────────────────────┐
│     ENTRADA: producto, cantidad, pedido,    │
│              promocionesActivas, fechaHora  │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Construir       │
              │ ContextoValid.  │ ← fecha, hora, día, productos en pedido, total
              └────────┬────────┘
                       │
                       ▼
          ┌────────────────────────┐
          │ Para cada promo activa │ ← stream() sobre promocionesActivas
          └────────────┬───────────┘
                       │
          ┌────────────▼───────────┐
          │ FILTRO 1: Estado       │
          │ ¿promo.estado == ACTIVA?│
          └────────────┬───────────┘
                       │ SÍ
          ┌────────────▼───────────┐
          │ FILTRO 2: Alcance      │
          │ ¿Tiene targets?        │
          │ ¿Producto es TARGET?   │
          └────────────┬───────────┘
                       │ SÍ
          ┌────────────▼───────────┐
          │ FILTRO 3: Triggers     │
          │ ¿puedeActivarse(ctx)?  │ ← AND de todos los CriterioActivacion
          └────────────┬───────────┘
                       │ SÍ
          ┌────────────▼───────────┐
          │ FILTRO 4: Combo check  │
          │ Si es COMBO_CONDICIONAL│
          │ ¿TRIGGERs presentes   │
          │  en el pedido con      │
          │  cantidad mínima?      │
          └────────────┬───────────┘
                       │ SÍ
          ┌────────────▼───────────┐
          │ CALCULAR DESCUENTO     │ ← switch(estrategia) con fórmula específica
          └────────────┬───────────┘
                       │
          ┌────────────▼───────────┐
          │ FILTRO 5: Descuento > 0│ ← Evita promos que no generan beneficio real
          └────────────┬───────────┘
                       │ SÍ
          ┌────────────▼───────────┐
          │ Candidata válida       │ ← PromocionEvaluada(promo, montoDescuento)
          └────────────┬───────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ RESOLVER CONFLICTO │
              │ max(prioridad)     │ ← La de mayor prioridad gana
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ GENERAR ItemPedido │
              │ con snapshot del   │
              │ descuento          │
              └─────────────────┘
```

### ContextoValidacion

El contexto se construye dinámicamente desde el estado actual del pedido:

```java
ContextoValidacion contexto = ContextoValidacion.builder()
    .fecha(fechaHora.toLocalDate())       // Para CriterioTemporal
    .hora(fechaHora.toLocalTime())        // Para CriterioTemporal
    .productosEnPedido(pedido.getItems()) // Para CriterioContenido
    .totalPedido(pedido.calcularSubtotalItems()) // Para CriterioMontoMinimo
    .build();
```

---

## 8. Descuentos Manuales

Los descuentos manuales son un mecanismo **independiente** de las promociones automáticas. Representan decisiones del operador (mozo/mostrador).

### Modelo: `DescuentoManual` (Value Object)

```
DescuentoManual
├── porcentaje: BigDecimal (0-100)
├── razon: String (motivo, puede estar vacío)
├── usuarioId: UUID (auditoría)
└── fechaAplicacion: LocalDateTime (auditoría)
```

### Características clave

| Aspecto | Comportamiento |
|---------|---------------|
| **Tipo de cálculo** | Dinámico: solo guarda porcentaje, recalcula monto cada vez |
| **Aplicación** | DESPUÉS de las promociones automáticas |
| **Alcance** | Por ítem individual o global (sobre el pedido completo) |
| **Persistencia** | Columnas directas en `items_pedido` y `pedidos` |
| **Auditoría** | Registra quién y cuándo lo aplicó |

### Diferencia con promociones automáticas

| Aspecto | Promoción Automática | Descuento Manual |
|---------|---------------------|-----------------|
| Quién lo aplica | El sistema (MotorReglasService) | El operador humano |
| Cuándo se calcula | Al agregar ítem (snapshot) | Dinámicamente al consultar |
| Persistencia | `montoDescuento` fijo en ItemPedido | `porcentaje` en columnas desc_manual_* |
| Puede cambiar | NO (inmutable) | SÍ (recalcula si cambia la base) |
| Base de cálculo | Precio base × cantidad | Precio base - descuento_promo (lo que queda) |

### Orden de aplicación

```
Precio base
    ↓ (1) Promoción automática (snapshot inmutable)
    ↓ (2) Descuento manual por ítem (dinámico, sobre el resultado de 1)
    ↓ (3) Descuento manual global (dinámico, sobre el total del pedido)
= Total final
```

---

## 9. Patrón Snapshot

> "El cálculo se hace UNA vez al agregar. Si mañana cambio el porcentaje de la promo, el ítem guardado HOY no debe cambiar su montoDescuento."

### ¿Qué se guarda en el snapshot?

Cuando el `MotorReglasService` encuentra una promoción ganadora, el `ItemPedido` se crea con:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `montoDescuento` | `BigDecimal` | El monto calculado (no el porcentaje) |
| `nombrePromocion` | `String` | Nombre de la promo para el ticket |
| `promocionId` | `UUID` | ID para auditoría y trazabilidad |

### ¿Por qué snapshot y no referencia viva?

1. **Consistencia histórica**: Un pedido de ayer debe verse exactamente como se cobró
2. **Independencia**: Si elimino/edito la promo, los pedidos anteriores no cambian
3. **Auditoría**: Puedo reconstruir cómo se calculó cada ítem
4. **Performance**: No necesito re-evaluar promos al consultar pedidos antiguos

### Columnas en base de datos

```sql
-- items_pedido (005_add_promocion_fields_to_items_pedido.sql)
ALTER TABLE items_pedido ADD COLUMN monto_descuento DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE items_pedido ADD COLUMN nombre_promocion VARCHAR(150);
ALTER TABLE items_pedido ADD COLUMN promocion_id UUID;
```

---

## 10. Recálculo de Promociones (HU-20/21)

### ¿Cuándo se recalcula?

Cuando un ítem se **elimina** (HU-20) o se **modifica** (HU-21) del pedido, las promociones de **todos los ítems** deben re-evaluarse porque:

1. Un combo puede **romperse** al eliminar su trigger (ej: borrar la hamburguesa rompe el combo con la gaseosa)
2. Los ciclos NxM **cambian** al modificar cantidades
3. La prioridad puede **resolverse diferente** con el nuevo estado del pedido

### Algoritmo de recálculo

```
1. Pedido.limpiarPromocionesItems()  → Todos los ítems vuelven a montoDescuento=0
2. MotorReglasService.aplicarPromociones(pedido, promos, fechaHora)
   → Para CADA ítem del pedido:
     a. Evaluar qué promoción aplica (usando el mismo algoritmo de evaluación)
     b. Calcular el descuento usando precioUnitario snapshot del ítem
     c. Aplicar la promoción ganadora sobre el ítem
3. Persistir el pedido actualizado
```

> ⚠️ El recálculo usa el `precioUnitario` snapshot (el precio que se guardó cuando se creó el ítem), NO el precio actual del producto en el catálogo.

---

## 11. Prioridades y Resolución de Conflictos

### Regla

> Si múltiples promociones aplican al mismo producto al mismo tiempo, **gana la de mayor prioridad** (número más alto).

```java
.max(Comparator.comparingInt(evaluada -> evaluada.promocion().getPrioridad()))
```

### Ejemplo de conflicto

| Promo | Tipo | Beneficio | Prioridad |
|-------|------|-----------|-----------|
| "Happy Hour Cervezas" | DESCUENTO_DIRECTO 30% | $900 | 5 |
| "2×1 Cervezas" | CANTIDAD_FIJA 2×1 | $3.000 | 10 |

Resultado: Gana **"2×1 Cervezas"** (prioridad 10 > 5).

### ¿Qué pasa si tienen la misma prioridad?

El comportamiento es **no determinístico** (depende del orden del stream). Se recomienda asignar prioridades únicas por local.

---

## 12. API REST — Endpoints

### Base URL: `/api/v1/locales/{localId}/promociones`

| Método | Endpoint | Use Case | Descripción |
|--------|----------|----------|-------------|
| `GET` | `/` | ListarPromociones | Lista todas. Query param `?estado=ACTIVA` opcional |
| `GET` | `/{id}` | ConsultarPromocion | Detalle de una promoción |
| `POST` | `/` | CrearPromocion | Crea con estrategia + triggers |
| `PUT` | `/{id}` | EditarPromocion | Actualización parcial (nombre, descripción, prioridad, triggers) |
| `DELETE` | `/{id}` | EliminarPromocion | Soft delete (desactiva) |
| `PUT` | `/{id}/scope` | AsociarScope | Define/reemplaza el alcance completo |

### Request: Crear Promoción

```json
{
  "nombre": "2×1 en Cervezas Artesanales",
  "descripcion": "Llevás 2, pagás 1. Solo viernes de noche.",
  "prioridad": 10,
  "tipoEstrategia": "CANTIDAD_FIJA",
  "cantidadFija": {
    "cantidadLlevas": 2,
    "cantidadPagas": 1
  },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31",
      "diasSemana": ["FRIDAY"],
      "horaDesde": "20:00",
      "horaHasta": "23:59"
    }
  ]
}
```

### Response: PromocionResponse

```json
{
  "id": "a1b2c3...",
  "nombre": "2×1 en Cervezas Artesanales",
  "descripcion": "Llevás 2, pagás 1. Solo viernes de noche.",
  "prioridad": 10,
  "estado": "ACTIVA",
  "estrategia": {
    "tipo": "CANTIDAD_FIJA",
    "modoDescuento": null,
    "valorDescuento": null,
    "cantidadLlevas": 2,
    "cantidadPagas": 1,
    "cantidadMinimaTrigger": null,
    "porcentajeBeneficio": null,
    "cantidadActivacion": null,
    "precioPaquete": null
  },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31",
      "diasSemana": ["FRIDAY"],
      "horaDesde": "20:00",
      "horaHasta": "23:59",
      "productosRequeridos": null,
      "montoMinimo": null
    }
  ],
  "alcance": {
    "items": []
  }
}
```

### Request: Asociar Scope

```json
{
  "items": [
    { "referenciaId": "uuid-cerveza-ipa", "tipo": "PRODUCTO", "rol": "TARGET" },
    { "referenciaId": "uuid-cerveza-stout", "tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

---

## 13. Modelo de Persistencia

### Tabla: `promociones`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | UUID PK | Identificador |
| `local_id` | UUID NOT NULL | Multi-tenancy |
| `nombre` | VARCHAR(150) | Unique con local_id |
| `descripcion` | VARCHAR(500) | Opcional |
| `prioridad` | INT | >= 0 |
| `estado` | VARCHAR(20) | ACTIVA / INACTIVA |
| `tipo_estrategia` | VARCHAR(30) | Discriminador |
| `modo_descuento` | VARCHAR(20) | Solo para DESCUENTO_DIRECTO |
| `valor_descuento` | DECIMAL(10,2) | Solo para DESCUENTO_DIRECTO |
| `cantidad_llevas` | INT | Solo para CANTIDAD_FIJA |
| `cantidad_pagas` | INT | Solo para CANTIDAD_FIJA |
| `cantidad_minima_trigger` | INT | Solo para COMBO_CONDICIONAL |
| `porcentaje_beneficio` | DECIMAL(5,2) | Solo para COMBO_CONDICIONAL |
| `triggers_json` | TEXT | Array JSON de criterios |

> **Decisión de diseño**: La estrategia se "aplana" en columnas de la tabla principal (en vez de usar herencia de tablas). Es simple, performante y suficiente para 4 tipos.

> **Decisión de diseño**: Los triggers se serializan como JSON (campo `triggers_json`) porque su estructura es polimórfica y se lee/escribe siempre como bloque completo.

### Tabla: `promocion_productos_scope`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | UUID PK | Identificador |
| `promocion_id` | UUID FK → promociones | CASCADE delete |
| `referencia_id` | UUID | Producto o categoría |
| `tipo_alcance` | VARCHAR(20) | PRODUCTO / CATEGORIA |
| `rol` | VARCHAR(20) | TRIGGER / TARGET |

**Constraint**: `UNIQUE(promocion_id, referencia_id)` — Un producto no puede aparecer dos veces en la misma promo.

### Snapshot en `items_pedido`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `monto_descuento` | DECIMAL(10,2) DEFAULT 0 | Snapshot del descuento calculado |
| `nombre_promocion` | VARCHAR(150) | Nombre de la promo para el ticket |
| `promocion_id` | UUID | ID de la promo (auditoría) |
| `desc_manual_porcentaje` | DECIMAL(5,2) | Descuento manual por ítem |
| `desc_manual_razon` | VARCHAR(255) | Motivo |
| `desc_manual_usuario_id` | UUID | Quién lo aplicó |
| `desc_manual_fecha` | TIMESTAMP | Cuándo |

### Descuento global en `pedidos`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `desc_global_porcentaje` | DECIMAL(5,2) | Descuento global |
| `desc_global_razon` | VARCHAR(255) | Motivo |
| `desc_global_usuario_id` | UUID | Quién |
| `desc_global_fecha` | TIMESTAMP | Cuándo |

---

## 14. Guía Práctica: Configuración por Tipo

### 📋 Caso 1: "20% off en empanadas, todo el mes de marzo"

**Paso 1: Crear la promoción**
```json
POST /api/v1/locales/{localId}/promociones
{
  "nombre": "Marzo Empanadas 20%",
  "prioridad": 5,
  "tipoEstrategia": "DESCUENTO_DIRECTO",
  "descuentoDirecto": { "modo": "PORCENTAJE", "valor": 20 },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-03-01",
      "fechaHasta": "2026-03-31"
    }
  ]
}
```

**Paso 2: Asociar productos**
```json
PUT /api/v1/locales/{localId}/promociones/{id}/scope
{
  "items": [
    { "referenciaId": "uuid-empanada-carne", "tipo": "PRODUCTO", "rol": "TARGET" },
    { "referenciaId": "uuid-empanada-jyq",   "tipo": "PRODUCTO", "rol": "TARGET" },
    { "referenciaId": "uuid-empanada-verdura","tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

---

### 📋 Caso 2: "2×1 en cervezas, viernes de noche"

**Paso 1: Crear**
```json
{
  "nombre": "2×1 Cervezas Viernes",
  "prioridad": 10,
  "tipoEstrategia": "CANTIDAD_FIJA",
  "cantidadFija": { "cantidadLlevas": 2, "cantidadPagas": 1 },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31",
      "diasSemana": ["FRIDAY"],
      "horaDesde": "20:00",
      "horaHasta": "23:59"
    }
  ]
}
```

**Paso 2: Scope** → Solo TARGETs (las cervezas que participan).

---

### 📋 Caso 3: "Comprando hamburguesa, gaseosa al 50%"

**Paso 1: Crear**
```json
{
  "nombre": "Combo Hamburguesa + Gaseosa",
  "prioridad": 8,
  "tipoEstrategia": "COMBO_CONDICIONAL",
  "comboCondicional": { "cantidadMinimaTrigger": 1, "porcentajeBeneficio": 50 },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31"
    }
  ]
}
```

**Paso 2: Scope** → ⚠️ **NECESITA TRIGGERs Y TARGETs**
```json
{
  "items": [
    { "referenciaId": "uuid-hamburguesa", "tipo": "PRODUCTO", "rol": "TRIGGER" },
    { "referenciaId": "uuid-gaseosa",     "tipo": "PRODUCTO", "rol": "TARGET" }
  ]
}
```

---

### 📋 Caso 4: "2 hamburguesas por $22.000"

**Paso 1: Crear**
```json
{
  "nombre": "Pack 2 Hamburguesas",
  "prioridad": 7,
  "tipoEstrategia": "PRECIO_FIJO_CANTIDAD",
  "precioFijoPorCantidad": { "cantidadActivacion": 2, "precioPaquete": 22000 },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31"
    }
  ]
}
```

**Paso 2: Scope** → Solo TARGETs.

---

### 📋 Caso 5: "10% en todo el pedido si supera $15.000, solo fines de semana"

**Paso 1: Crear**
```json
{
  "nombre": "Descuento por monto los finde",
  "prioridad": 3,
  "tipoEstrategia": "DESCUENTO_DIRECTO",
  "descuentoDirecto": { "modo": "PORCENTAJE", "valor": 10 },
  "triggers": [
    {
      "tipo": "TEMPORAL",
      "fechaDesde": "2026-01-01",
      "fechaHasta": "2026-12-31",
      "diasSemana": ["SATURDAY", "SUNDAY"]
    },
    {
      "tipo": "MONTO_MINIMO",
      "montoMinimo": 15000
    }
  ]
}
```

**Paso 2: Scope** → TARGETs: todos los productos que participan del descuento.

---

## 15. Matriz de Decisión: ¿Cuándo usar cada tipo?

| Necesidad del negocio | Estrategia recomendada | Triggers típicos | Scope |
|----------------------|----------------------|-------------------|-------|
| "X% off en un producto" | `DESCUENTO_DIRECTO` (PORCENTAJE) | TEMPORAL | Solo TARGETs |
| "$X off en un producto" | `DESCUENTO_DIRECTO` (MONTO_FIJO) | TEMPORAL | Solo TARGETs |
| "Llevá N, pagá M" | `CANTIDAD_FIJA` | TEMPORAL | Solo TARGETs |
| "Comprá X, Y tiene Z% off" | `COMBO_CONDICIONAL` | TEMPORAL + (opcionalmente CONTENIDO) | TRIGGERs + TARGETs |
| "N unidades por $X" | `PRECIO_FIJO_CANTIDAD` | TEMPORAL | Solo TARGETs |
| "Descuento solo si el pedido supera $X" | Cualquiera | TEMPORAL + MONTO_MINIMO | Según estrategia |
| "Solo ciertos días/horarios" | Cualquiera | TEMPORAL (con diasSemana/hora) | Según estrategia |
| "Descuento puntual decidido por el mozo" | N/A → **Descuento Manual** | N/A | Por ítem o global |

---

## 16. Reglas de Negocio Críticas

### Inmutables — Nunca deben violarse

1. **El precio base nunca se modifica retroactivamente.** Un ItemPedido guarda el precio del momento en que se creó.

2. **El montoDescuento de una promoción es un snapshot.** Se calcula una vez y no cambia, salvo recálculo explícito (HU-20/21).

3. **Los extras NO participan en descuentos de promociones.** El descuento se calcula SOLO sobre `precioUnitario × cantidad`.

4. **Los descuentos manuales se aplican DESPUÉS de las promociones automáticas.** La base gravable del descuento manual es el precio ya descontado por la promo.

5. **El total se calcula desde los ítems, nunca al revés.** `Total = Σ items - Σ descuentos`.

6. **Lógica AND en triggers.** Si una promo tiene 3 triggers, los 3 deben cumplirse.

7. **Mayor prioridad gana.** Si 2 promos aplican al mismo producto, la de número más alto prevalece.

8. **Sin TARGETs en el scope, la promo no aplica.** El sistema verifica `alcance.tieneTargets()` y `alcance.esProductoTarget()`.

9. **Para COMBO_CONDICIONAL, sin TRIGGERs presentes en el pedido, no hay beneficio.** El sistema verifica cantidad mínima del trigger.

10. **Multi-tenancy estricto.** Toda promoción pertenece a un `localId`. No hay cruce de datos entre locales.

---

## 17. Diagrama de Clases Simplificado

```
                     ┌───────────────────────────┐
                     │       Promocion            │
                     │  (Aggregate Root)          │
                     ├───────────────────────────┤
                     │ - id: PromocionId          │
                     │ - localId: LocalId         │
                     │ - nombre: String           │
                     │ - descripcion: String      │
                     │ - prioridad: int           │
                     │ - estado: EstadoPromocion  │
                     ├───────────────────────────┤
                     │ + puedeActivarse(ctx)      │
                     │ + activar() / desactivar() │
                     │ + definirAlcance(alcance)  │
                     └─────┬────────┬────────┬───┘
                           │        │        │
              ┌────────────┘        │        └────────────┐
              ▼                     ▼                     ▼
┌──────────────────────┐ ┌───────────────────┐ ┌──────────────────────┐
│ EstrategiaPromocion  │ │CriterioActivacion │ │  AlcancePromocion    │
│   «sealed»           │ │   «sealed»        │ │  (Value Object)      │
├──────────────────────┤ ├───────────────────┤ ├──────────────────────┤
│ DescuentoDirecto     │ │ CriterioTemporal  │ │ items: List<ItemProm>│
│ CantidadFija         │ │ CriterioContenido │ │ + getTriggers()      │
│ ComboCondicional     │ │ CriterioMontoMin. │ │ + getTargets()       │
│ PrecioFijoCantidad   │ │                   │ │ + esProductoTarget() │
└──────────────────────┘ └───────────────────┘ └──────────┬───────────┘
                                                          │
                                                          ▼
                                               ┌──────────────────┐
                                               │  ItemPromocion   │
                                               │ (Value Object)   │
                                               ├──────────────────┤
                                               │ referenciaId: UUID│
                                               │ tipo: TipoAlcance│
                                               │ rol: RolPromocion│
                                               └──────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                    MotorReglasService                                │
│                    (Domain Service)                                  │
├─────────────────────────────────────────────────────────────────────┤
│ + aplicarReglas(pedido, producto, cant, obs, promos, fecha)         │
│ + aplicarReglasConExtras(pedido, producto, cant, obs, extras, ...)  │
│ + aplicarPromociones(pedido, promos, fecha)  ← recálculo HU-20/21  │
│ - evaluarPromocion(promo, producto, pedido, ctx)                   │
│ - calcularDescuento(promo, producto, cantidad)                     │
│ - verificarTriggersComboPresentesEnPedido(...)                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Glosario

| Término | Definición |
|---------|-----------|
| **Aggregate Root** | Entidad raíz que define la frontera de consistencia transaccional |
| **Sealed Interface** | Interfaz cerrada de Java 17+ que solo permite implementaciones explícitas |
| **Snapshot** | Foto inmutable de un valor en un momento determinado |
| **Trigger** | Condición que debe cumplirse para que una promoción aplique |
| **Target** | Producto que recibe el beneficio del descuento |
| **Alcance / Scope** | Conjunto de productos/categorías asociados a una promoción con sus roles |
| **Motor de Reglas** | Domain Service que evalúa y aplica promociones automáticamente |
| **Multi-tenancy por fila** | Aislamiento de datos por `local_id` en cada tabla |

---

> **Nota final:** Este documento refleja el estado actual del motor de promociones. Cualquier modificación al dominio debe actualizar este documento para mantener consistencia con el código fuente.
