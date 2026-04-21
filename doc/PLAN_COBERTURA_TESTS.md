# Plan de Cobertura de Tests — FoodFlow Backend

> Generado: 11 de abril de 2026  
> Autor: Auditoría automatizada sobre dominio + application + tests existentes  
> Scope: domain/, application/usecase/, domain/service/

---

## 1. Resumen ejecutivo

### Estado actual

| Métrica | Valor |
|---------|-------|
| Tests existentes | **~320 métodos @Test** en 29 archivos |
| Use cases en producción | 40 |
| Use cases con test directo | 26 (65%) |
| Use cases sin test | **14 (35%)** |
| Modelos de dominio con test | 2 de 23 (Producto, Promocion) |
| Domain services con test | 1 de 3 (MotorReglas) |

### Diagnóstico

La cobertura es **fuerte en promociones, catálogo y flujo operativo de mesas**, pero tiene un hueco estructural en el corazón del sistema: **el Aggregate Root `Pedido` no tiene tests unitarios puros**. Toda la validación de su comportamiento depende de tests de integración que lo ejercitan indirectamente.

Los tres gaps más peligrosos son:

1. **`Pedido` sin PedidoTest.java** — La "Regla de Oro" del cálculo de totales no tiene test unitario.
2. **Ciclo de caja (Abrir/Cerrar Jornada) sin tests** — Todo el flujo contable de apertura y cierre carece de validación.
3. **Corrección de pedido cerrado sin tests** — Funcionalidad de corrección post-cierre (alternativa a reapertura) totalmente blind.

---

## 2. Prioridad de implementación

### PRIORIDAD ALTA — Protegen el core del dominio

| # | Archivo a crear | Tipo | Tests estimados | Justificación |
|---|----------------|------|----------------|---------------|
| 1 | `PedidoTest.java` | Domain Unit | 18-22 | Aggregate Root sin tests. Fórmula de total, cierre, reapertura, invariantes. |
| 2 | `ItemPedidoTest.java` | Domain Unit | 10-12 | Cálculo de precio final, jerarquía de descuentos, merge de ítems. |
| 3 | `DescuentoManualTest.java` | Domain Unit | 8-10 | Value Object con lógica de cálculo. Cap de monto, rango de porcentaje. |
| 4 | `JornadaCajaTest.java` | Domain Unit | 6-8 | Fecha operativa turno noche, cierre irreversible, validaciones. |
| 5 | `AbrirJornadaUseCaseTest.java` | Application Unit | 5-7 | Unicidad de jornada abierta, monto inicial, fecha operativa. |
| 6 | `CerrarJornadaUseCaseTest.java` | Application Unit | 6-8 | Mesas deben estar LIBRE, fórmula de balance, snapshot contable. |
| 7 | `CorregirPedidoCerradoUseCaseTest.java` | Application Unit | 6-8 | Corrección in-place, validación de estado, recalculo de pagos. |

**Subtotal ALTA: ~59-75 tests**

### PRIORIDAD MEDIA — Completan cobertura de reglas de negocio

| # | Archivo a crear | Tipo | Tests estimados | Justificación |
|---|----------------|------|----------------|---------------|
| 8 | `MesaTest.java` | Domain Unit | 5-6 | Transiciones de estado (LIBRE↔ABIERTA), guards en abrir/liberar/reocupar. |
| 9 | `PagoTest.java` | Domain Unit | 3-4 | Validación monto > 0, medio != null. Simple pero no cubierto. |
| 10 | `MovimientoCajaTest.java` | Domain Unit | 4-5 | Formato de comprobante EGR/ING-..., validaciones de construcción. |
| 11 | `CrearCategoriaUseCaseTest.java` | Application Unit | 4-5 | Unicidad case-insensitive, color default, admiteVariantes. |
| 12 | `EditarCategoriaUseCaseTest.java` | Application Unit | 4-5 | Unicidad al editar excluyendo actual, multi-tenancy. |
| 13 | `EliminarCategoriaUseCaseTest.java` | Application Unit | 3-4 | Multi-tenancy, integridad referencial (productos que la referencian). |
| 14 | `CambiarEstadoPromocionUseCaseTest.java` | Application Unit | 3-4 | Multi-tenancy, idempotencia de desactivar ya inactiva. |
| 15 | `EnviarComandaCocinaUseCaseTest.java` | Application Unit | 5-6 | Ítems nuevos vs reimpresión, timestamp de envío, estado mesa. |
| 16 | `RegistrarEgresoUseCaseTest.java` | Application Unit | 3-4 | Monto > 0, generación comprobante, tipo EGRESO. |
| 17 | `RegistrarIngresoUseCaseTest.java` | Application Unit | 3-4 | Monto > 0, generación comprobante, tipo INGRESO. |

**Subtotal MEDIA: ~37-47 tests**

### PRIORIDAD BAJA — Consultas read-only y edge cases

| # | Archivo a crear | Tipo | Tests estimados | Justificación |
|---|----------------|------|----------------|---------------|
| 18 | `ConsultarCategoriasUseCaseTest.java` | Application Unit | 2-3 | Solo lectura, multi-tenancy. |
| 19 | `ConsultarHistorialJornadasUseCaseTest.java` | Application Unit | 3-4 | Rango ≤ 365 días, desde ≤ hasta. |
| 20 | `ConsultarPedidoCerradoUseCaseTest.java` | Application Unit | 3-4 | Estado CERRADO, multi-tenancy. |
| 21 | `ObtenerEstadoJornadaUseCaseTest.java` | Application Unit | 2-3 | Lógica de sugerencia con último balance. |
| 22 | `GestorStockServiceTest.java` | Domain Service Unit | 4-5 | Cubierto indirectamente por StockIntegration, pero sin test puro del servicio. |
| 23 | `ExtraPedidoTest.java` | Domain Unit | 3-4 | Factory esExtra=true, snapshot inmutable. |

**Subtotal BAJA: ~17-23 tests**

**TOTAL ESTIMADO: ~113-145 tests nuevos**

---

## 3. Lista de tests a crear

### 3.1 PedidoTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.domain.model
Tipo: Test unitario puro (sin Spring, sin BD)
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_crear_pedido_con_estado_abierto` | Constructor asigna ABIERTO, genera campos inmutables | Invariante base del Aggregate Root |
| `deberia_rechazar_numero_pedido_cero_o_negativo` | Número > 0 | Validación de construcción |
| `deberia_rechazar_campos_obligatorios_null` | id, localId, mesaId, estado, fechaApertura != null | Fail-fast en construcción |
| `deberia_agregar_producto_correctamente` | Items se suman, snapshot de precio se captura | Operación central del dominio |
| `deberia_rechazar_producto_de_otro_local` | Producto con localId diferente → excepción | AC5: aislamiento multi-tenant |
| `deberia_rechazar_agregar_a_pedido_cerrado` | Estado CERRADO → IllegalStateException | Invariante de modificación |
| `deberia_fusionar_items_con_misma_configuracion` | Mismo producto+obs+extras → incrementa cantidad | Regla de fusión de líneas |
| `deberia_crear_linea_separada_si_observacion_distinta` | Mismo producto, obs diferente → 2 líneas | Observaciones personalizan |
| `deberia_calcular_subtotal_items_sin_descuentos` | Σ(precio × cantidad) sin descuentos | Paso intermedio del cálculo |
| `deberia_calcular_total_con_descuento_global_porcentaje` | Total = subtotal - (subtotal × %) | Regla de Oro del dominio |
| `deberia_calcular_total_con_descuento_global_monto_fijo` | Total = subtotal - min(monto, subtotal) | Cap de monto fijo |
| `deberia_calcular_total_con_descuento_item_y_global` | Jerarquía: promo → desc.item → desc.global | Composición de descuentos |
| `deberia_cerrar_pedido_con_pagos_exactos` | ABIERTO → CERRADO, snapshot contable congelado | Cierre exitoso |
| `deberia_rechazar_cierre_si_pagos_no_coinciden_con_total` | Suma pagos ≠ total → excepción | Invariante contable central |
| `deberia_rechazar_cierre_sin_items` | Pedido vacío → no puede cerrar | Pedido sin sentido |
| `deberia_reabrir_pedido_cerrado` | CERRADO → ABIERTO, limpia pagos y snapshot | HU-14: válvula de escape |
| `deberia_rechazar_reapertura_de_pedido_abierto` | Estado ABIERTO → excepción | Solo CERRADO se reabre |
| `deberia_corregir_pedido_cerrado_in_place` | Modifica cantidades y pagos sin reabrir | Alternativa a reapertura |
| `deberia_limpiar_promociones_al_eliminar_item` | eliminarItem → limpiarPromocionesItems | HU-20/21: recálculo |
| `deberia_limpiar_promociones_al_modificar_cantidad` | actualizarCantidad → limpiarPromocionesItems | HU-20/21: recálculo |
| `deberia_materializar_ajustes_economicos` | obtenerAjustesEconomicos → lista de AjusteEconomico | Auditoría narrativa |
| `deberia_registrar_timestamp_envio_cocina` | marcarComoEnviadoACocina actualiza campo | HU-29 |

### 3.2 ItemPedidoTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.domain.model
Tipo: Test unitario puro
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_crear_item_con_snapshot_de_precio` | Factory captura precio del producto al momento | Patrón Snapshot |
| `deberia_rechazar_cantidad_cero_o_negativa` | cantidad > 0 | Validación de construcción |
| `deberia_rechazar_precio_negativo` | precioUnitario >= 0 | Validación de construcción |
| `deberia_calcular_subtotal_linea_sin_extras` | precio × cantidad | Cálculo base |
| `deberia_calcular_subtotal_linea_con_extras` | (precio + Σextras) × cantidad | HU-05.1/22 |
| `deberia_calcular_precio_final_con_promo_snapshot` | subtotal - montoDescuento | HU-10: promo fija |
| `deberia_calcular_precio_final_con_descuento_manual` | subtotal - descManual.calcularMonto(base) | HU-14: desc dinámico |
| `deberia_calcular_precio_final_completo` | promo + desc.manual combinados | Jerarquía completa |
| `deberia_coincidir_configuracion_misma_obs_mismos_extras` | coincideConfiguracion → true | Fusión de líneas |
| `deberia_no_coincidir_configuracion_con_obs_diferente` | coincideConfiguracion → false | Líneas separadas |
| `deberia_calcular_cantidad_nueva_para_cocina` | cantidad - cantidadEnviadaCocina | HU-29: delta |
| `deberia_heredar_envio_cocina_en_merge` | heredarEnvioCocina preserva cantidad enviada | HU-29: fusión |

### 3.3 DescuentoManualTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.domain.model
Tipo: Test unitario puro
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_crear_descuento_porcentaje_valido` | Construcción exitosa PORCENTAJE | Happy path |
| `deberia_crear_descuento_monto_fijo_valido` | Construcción exitosa MONTO_FIJO | Happy path |
| `deberia_rechazar_porcentaje_mayor_a_100` | valor > 100 → excepción | Rango superior |
| `deberia_rechazar_porcentaje_menor_a_001` | valor < 0.01 → excepción | Rango inferior |
| `deberia_rechazar_monto_fijo_cero_o_negativo` | valor ≤ 0 → excepción | Validación de valor |
| `deberia_rechazar_tipo_null` | tipo = null → excepción | Campo obligatorio |
| `deberia_rechazar_usuario_null` | usuarioId = null → excepción | Auditoría |
| `deberia_calcular_monto_porcentaje_correctamente` | base 1000, 10% → 100 | Cálculo correcto |
| `deberia_capear_monto_fijo_a_base_gravable` | base 500, monto 800 → 500 (no 800) | Evitar totales negativos |
| `deberia_rechazar_base_gravable_negativa` | base < 0 → excepción | Precondición |

### 3.4 JornadaCajaTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.domain.model
Tipo: Test unitario puro
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_crear_jornada_abierta_con_fondo_valido` | Construcción exitosa, estado ABIERTA | Happy path |
| `deberia_rechazar_fondo_negativo` | fondoInicial < 0 → excepción | Validación |
| `deberia_rechazar_campos_null` | id, localId, fondoInicial, fechaApertura | Fail-fast |
| `deberia_cerrar_jornada_abierta` | ABIERTA → CERRADA con snapshot | Transición principal |
| `deberia_rechazar_cerrar_jornada_ya_cerrada` | CERRADA → excepción | Irreversibilidad |
| `deberia_calcular_fecha_operativa_antes_de_las_6` | 2026-04-11 03:00 → 2026-04-10 | Turno noche |
| `deberia_calcular_fecha_operativa_despues_de_las_6` | 2026-04-11 08:00 → 2026-04-11 | Turno diurno |
| `deberia_calcular_fecha_operativa_exactamente_a_las_6` | 2026-04-11 06:00 → 2026-04-11 | Límite exacto |

### 3.5 AbrirJornadaUseCaseTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.application.usecase
Tipo: Test unitario con mocks
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_abrir_jornada_exitosamente` | Crea jornada con fondo + fecha operativa | Happy path |
| `deberia_rechazar_si_ya_hay_jornada_abierta` | JornadaYaAbiertaException | Exclusión mutua |
| `deberia_rechazar_monto_inicial_negativo` | monto < 0 → excepción | Validación de entrada |
| `deberia_calcular_fecha_operativa_automaticamente` | Clock a las 02:00 → fecha = día anterior | Turno noche via Clock |
| `deberia_rechazar_si_existe_jornada_cerrada_misma_fecha` | Fecha operativa duplicada | Unicidad diaria |

### 3.6 CerrarJornadaUseCaseTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.application.usecase
Tipo: Test unitario con mocks
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_cerrar_jornada_con_balance_correcto` | Balance = fondo + ventas_efectivo + ingresos - egresos | Fórmula contable |
| `deberia_rechazar_si_no_hay_jornada_abierta` | Sin jornada → excepción | Precondición |
| `deberia_rechazar_si_existen_mesas_abiertas` | MesasAbiertasException con count | Invariante de cierre |
| `deberia_separar_ventas_reales_de_consumo_interno` | A_CUENTA → consumo, resto → ventas reales | Desglose correcto |
| `deberia_contar_pedidos_cerrados_del_dia` | Solo CERRADOS, rango de fechas correcto | Métricas |
| `deberia_persistir_snapshot_inmutable` | jornada.cerrar() + save atómico | Inmutabilidad financiera |

### 3.7 CorregirPedidoCerradoUseCaseTest.java — PRIORIDAD ALTA

```
Paquete: com.agustinpalma.comandas.application.usecase
Tipo: Test unitario con mocks
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_corregir_cantidades_y_pagos` | Modifica cantidad + reemplaza pagos | Happy path |
| `deberia_rechazar_si_pedido_no_esta_cerrado` | Estado ABIERTO → excepción | Solo CERRADO |
| `deberia_rechazar_si_pedido_no_pertenece_al_local` | Multi-tenancy | Aislamiento |
| `deberia_rechazar_si_pagos_no_coinciden_con_nuevo_total` | Suma pagos ≠ total recalculado | Invariante contable |
| `deberia_recalcular_snapshot_contable` | Montos finales actualizados | Consistencia |
| `deberia_rechazar_si_pedido_no_existe` | PedidoId inexistente | Validación |

### 3.8 MesaTest.java — PRIORIDAD MEDIA

```
Paquete: com.agustinpalma.comandas.domain.model
Tipo: Test unitario puro
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_crear_mesa_con_estado_libre` | Constructor → LIBRE | Invariante inicial |
| `deberia_abrir_mesa_libre` | LIBRE → ABIERTA | Transición válida |
| `deberia_rechazar_abrir_mesa_ya_abierta` | ABIERTA → excepción | Guard |
| `deberia_liberar_mesa_abierta` | ABIERTA → LIBRE | Transición válida |
| `deberia_rechazar_liberar_mesa_ya_libre` | LIBRE → excepción | Guard |
| `deberia_reocupar_mesa_libre` | LIBRE → ABIERTA (reapertura) | HU-14 |

### 3.9 MovimientoCajaTest.java — PRIORIDAD MEDIA

```
Paquete: com.agustinpalma.comandas.domain.model
Tipo: Test unitario puro
```

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_generar_comprobante_egreso_con_formato_correcto` | "EGR-yyyyMMdd-HHmmss-XXXX" | Formato auditable |
| `deberia_generar_comprobante_ingreso_con_formato_correcto` | "ING-yyyyMMdd-HHmmss-XXXX" | Formato auditable |
| `deberia_rechazar_monto_cero` | monto = 0 → excepción | Validación |
| `deberia_rechazar_monto_negativo` | monto < 0 → excepción | Validación |
| `deberia_rechazar_descripcion_vacia` | descripcion vacía → excepción | Campo obligatorio |

### 3.10 CrearCategoriaUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_crear_categoria_exitosamente` | Happy path | CRUD básico |
| `deberia_rechazar_nombre_duplicado_case_insensitive` | "Bebidas" vs "BEBIDAS" → 409 | Unicidad |
| `deberia_asignar_color_default_si_null` | colorHex null → #FFFFFF | Retrocompatibilidad |
| `deberia_rechazar_nombre_vacio` | nombre "" → excepción | Validación |

### 3.11 EditarCategoriaUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_editar_categoria_exitosamente` | Happy path | CRUD |
| `deberia_rechazar_nombre_duplicado_excluyendo_actual` | Otra categoría con mismo nombre → 409 | Unicidad excluyente |
| `deberia_permitir_mantener_mismo_nombre` | No falla si el nombre no cambió | Idempotencia |
| `deberia_rechazar_categoria_de_otro_local` | Multi-tenancy | Aislamiento |

### 3.12 EliminarCategoriaUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_eliminar_categoria_exitosamente` | Happy path | CRUD |
| `deberia_rechazar_categoria_inexistente` | 404 | Validación |
| `deberia_rechazar_categoria_de_otro_local` | Multi-tenancy | Aislamiento |

### 3.13 CambiarEstadoPromocionUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_activar_promocion_inactiva` | INACTIVA → ACTIVA | Transición |
| `deberia_desactivar_promocion_activa` | ACTIVA → INACTIVA | Transición |
| `deberia_rechazar_promocion_de_otro_local` | Multi-tenancy | Aislamiento |

### 3.14 EnviarComandaCocinaUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_enviar_solo_items_nuevos` | soloNuevos=true → filtra delta | HU-29 |
| `deberia_enviar_todos_en_reimpresion` | soloNuevos=false → todos | Reimpresión |
| `deberia_actualizar_timestamp_envio` | marcarComoEnviadoACocina | Auditoría |
| `deberia_rechazar_mesa_libre` | Mesa sin pedido abierto | Precondición |
| `deberia_rechazar_pedido_sin_items` | Pedido vacío → excepción | Comanda vacía |

### 3.15 RegistrarEgresoUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_registrar_egreso_exitosamente` | Happy path con comprobante | CRUD |
| `deberia_rechazar_monto_cero` | monto = 0 → excepción | Validación |
| `deberia_rechazar_descripcion_vacia` | descripcion vacía → excepción | Campo obligatorio |

### 3.16 RegistrarIngresoUseCaseTest.java — PRIORIDAD MEDIA

| Test | Qué valida | Por qué importa |
|------|-----------|-----------------|
| `deberia_registrar_ingreso_exitosamente` | Happy path con comprobante | CRUD |
| `deberia_rechazar_monto_cero` | monto = 0 → excepción | Validación |
| `deberia_crear_movimiento_tipo_ingreso` | tipo = INGRESO (no EGRESO) | Distinción |

### 3.17-3.22 Prioridad BAJA (consultas + edge cases)

Tests de consulta (ConsultarCategorias, ConsultarHistorialJornadas, ConsultarPedidoCerrado, ObtenerEstadoJornada) y GestorStockServiceTest / ExtraPedidoTest.

Detallados en la sección 2 (tabla de prioridad BAJA). Siguen la misma estructura: happy path + multi-tenancy + validación de entrada.

---

## 4. Reglas de negocio críticas sin cobertura

### Categoría ROJA — Pueden causar inconsistencia financiera

| # | Regla | Dónde vive | Estado |
|---|-------|-----------|--------|
| R1 | **Total = Σ(items.precioFinal) - descuentoGlobal** | `Pedido.calcularTotal()` | SIN TEST UNITARIO |
| R2 | **Suma de pagos == total exacto al cerrar** | `Pedido.cerrar()` | Solo en CierreMesaIntegration |
| R3 | **Balance = fondo + ventas_efectivo + ingresos - egresos** | `CerrarJornadaUseCase` | SIN TEST |
| R4 | **MONTO_FIJO se capea a base gravable** | `DescuentoManual.calcularMonto()` | SIN TEST |
| R5 | **Corrección de pedido cerrado no rompe pagos** | `Pedido.corregir()` + `CorregirPedidoCerradoUseCase` | SIN TEST |

### Categoría NARANJA — Pueden causar datos incorrectos

| # | Regla | Dónde vive | Estado |
|---|-------|-----------|--------|
| R6 | Fecha operativa turno noche (< 06:00 → día anterior) | `JornadaCaja.calcularFechaOperativa()` | SIN TEST |
| R7 | Máxima 1 jornada ABIERTA por local | `AbrirJornadaUseCase` | SIN TEST |
| R8 | Todas las mesas LIBRE para cerrar jornada | `CerrarJornadaUseCase` | SIN TEST |
| R9 | Jerarquía: promo(snapshot) → desc.item(dinámico) → desc.global(dinámico) | `ItemPedido.calcularPrecioFinal()` | SIN TEST UNITARIO |
| R10 | Porcentaje de descuento en rango [0.01, 100] | `DescuentoManual` constructor | SIN TEST |

### Categoría AMARILLA — Pueden causar comportamiento inesperado

| # | Regla | Dónde vive | Estado |
|---|-------|-----------|--------|
| R11 | Unicidad nombre categoría (case insensitive) | `CrearCategoriaUseCase` | SIN TEST |
| R12 | Formato comprobante EGR/ING-yyyyMMdd-HHmmss-XXXX | `MovimientoCaja` constructor | SIN TEST |
| R13 | Enviar comanda: solo ítems nuevos (delta) | `EnviarComandaCocinaUseCase` | SIN TEST |
| R14 | Reapertura revierte stock (carga→repone→persiste) | `ReabrirPedidoUseCase` | Parcial (StockIntegration) |
| R15 | Jornada cerrada no se puede volver a cerrar | `JornadaCaja.cerrar()` | SIN TEST |

---

## 5. Orden recomendado de implementación

### Fase 1 — Fundaciones del dominio (semana 1)

```
1. PedidoTest.java              ← El Aggregate Root. Todo depende de esto.
2. ItemPedidoTest.java           ← Cálculos que alimentan al Pedido.
3. DescuentoManualTest.java      ← Value Object con lógica no trivial.
```

**Criterio:** Estos 3 archivos testean la "Regla de Oro" del dominio de punta a punta sin necesitar Spring ni BD.

### Fase 2 — Ciclo de caja (semana 2)

```
4. JornadaCajaTest.java          ← Modelo del ciclo contable.
5. AbrirJornadaUseCaseTest.java  ← Apertura con validaciones.
6. CerrarJornadaUseCaseTest.java ← Cierre con balance y guards.
```

**Criterio:** El ciclo de caja es la segunda funcionalidad más crítica y no tiene ni un solo test.

### Fase 3 — Corrección + Mesa (semana 2-3)

```
7. CorregirPedidoCerradoUseCaseTest.java ← Feature blind.
8. MesaTest.java                          ← Transiciones simples pero sin test.
```

**Criterio:** La corrección es alternativa a reapertura. Mesa es simple pero sus guards protegen invariantes.

### Fase 4 — CRUD de categorías + estado de promos (semana 3)

```
9.  CrearCategoriaUseCaseTest.java
10. EditarCategoriaUseCaseTest.java
11. EliminarCategoriaUseCaseTest.java
12. CambiarEstadoPromocionUseCaseTest.java
```

**Criterio:** Las categorías soportan el catálogo. Ninguna tiene test.

### Fase 5 — Operaciones de caja + cocina (semana 3-4)

```
13. MovimientoCajaTest.java
14. RegistrarEgresoUseCaseTest.java
15. RegistrarIngresoUseCaseTest.java
16. EnviarComandaCocinaUseCaseTest.java
```

**Criterio:** Movimientos de caja manejan dinero y generan comprobantes auditables.

### Fase 6 — Consultas y edge cases (semana 4+)

```
17-22. Consultas read-only, GestorStockServiceTest, ExtraPedidoTest
```

**Criterio:** Riesgo bajo, lógica mínima, pueden esperar.

---

## 6. Riesgos de no cubrir estos gaps

### Riesgo CRÍTICO

| Escenario | Consecuencia | Probabilidad |
|-----------|-------------|-------------|
| Bug en `calcularTotal()` con combinación de descuentos | El cliente paga de más o de menos. Desconfianza operativa inmediata. | **Alta** — múltiples capas de descuento sin test unitario |
| Bug en cierre de jornada con mesas abiertas | Cierre contable inconsistente, balance no cuadra con efectivo real en caja. | **Media** — depende del flujo del operador |
| Corrección de pedido cerrado rompe pagos | Pedido con total X pero pagos sumando Y. Irrecuperable sin reabrir. | **Media** — feature nuevo, sin red de seguridad |

### Riesgo ALTO

| Escenario | Consecuencia | Probabilidad |
|-----------|-------------|-------------|
| Descuento MONTO_FIJO > base gravable no se capea | Total negativo en un pedido. UI muestra "-$500". | **Baja** pero impacto visible |
| Jornada doble (2 abiertas simultáneas) | Datos contables duplicados, balance incalculable. | **Baja** — requiere race condition |
| Fecha operativa incorrecta a las 05:59 | Venta del viernes aparece en reporte del jueves. | **Media** en locales nocturnos |

### Riesgo MEDIO

| Escenario | Consecuencia | Probabilidad |
|-----------|-------------|-------------|
| Categorías con nombre duplicado | UI confusa, filtros rotos. | **Baja** |
| Comprobante de egreso sin formato correcto | Complicación para auditoría manual. | **Baja** |
| Comanda reimpresa envía todos los ítems como nuevos | Cocina prepara duplicados. | **Media** pero detectable rápido |

---

## Apéndice: Convenciones para los nuevos tests

### Nomenclatura

```java
// Tests de dominio → en domain/model/ o domain/service/
// Sin Spring, sin mocks de framework, sin BD
class PedidoTest {
    @Test
    void deberia_calcular_total_con_descuento_global_porcentaje() { }
}

// Tests de use case → en application/usecase/
// Con @ExtendWith(MockitoExtension.class) y mocks de repositorios
class AbrirJornadaUseCaseTest {
    @Test
    void deberia_rechazar_si_ya_hay_jornada_abierta() { }
}
```

### Patrón de test

```java
@Test
void deberia_<comportamiento_esperado>() {
    // Given — setup del escenario
    // When  — ejecutar la acción
    // Then  — verificar resultado
}
```

### Builders reutilizables sugeridos

Para evitar duplicación en la creación de fixtures:

- `PedidoTestBuilder` — builder fluido para armar pedidos con items, descuentos, estados
- `ProductoTestBuilder` — builder para productos con variantes, stock, extras
- `PromocionTestBuilder` — ya existe parcialmente en PromocionesIntegrationTest

Cada builder debería vivir en un paquete `testutil/` o como inner class del test que lo utiliza.
