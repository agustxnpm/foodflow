# Historias de Usuario — Sistema de Gestión de Local Gastronómico

---

# 1️⃣ Gestión de Mesas (Salón)

---

## HU-02 – Ver estado de mesas #2

**Como usuario**  
Quiero ver todas las mesas del local y su estado  
Para saber cuáles están libres y cuáles tienen pedidos abiertos

### Criterios de aceptación

- Se muestran todas las mesas del local
- Cada mesa indica si está LIBRE o ABIERTA

---

## HU-03 – Abrir mesa #3

**Como usuario**  
Quiero abrir una mesa libre  
Para comenzar a cargar un pedido

### Criterios de aceptación

- Solo se pueden abrir mesas libres
- Al abrirse, se crea un pedido en estado ABIERTO
- La mesa debe pertenecer al local (local_id) del usuario que realiza la acción
- Una mesa solo puede tener un único pedido en estado ABIERTO a la vez
- El pedido creado debe quedar vinculado de forma inmutable al MesaId y al LocalId correspondientes

---

## HU-06 – Ver pedido de una mesa #6

**Como usuario del local (mozo / administrador)**  
Quiero consultar el detalle del pedido activo de una mesa específica  
Para informar a los clientes sobre su consumo actual y verificar que los productos cargados sean correctos.

### Criterios de Aceptación

**AC1 – Visualización de Ítems:**
- Nombre del producto (snapshot capturado al agregar).
- Cantidad.
- Precio unitario (snapshot capturado al agregar).
- Subtotal por línea (cantidad×precio_unitario).
- Observaciones específicas (ej: "sin sal").

**AC2 – Cálculo del Total Parcial:**
- Mostrar el Total Parcial del pedido.

**AC3 – Información de Contexto:**
- Número de la mesa.
- ID del pedido.
- Fecha y hora de apertura.

**AC4 – Validación de Mesa Ocupada:**
- Si la mesa está LIBRE → informar que no tiene pedido activo.

**AC5 – Aislamiento Multi-tenancy:**
- Solo pedidos del mismo LocalId.

### Casos de Error Esperados
- Mesa sin pedido → 404 Not Found
- Mesa inexistente o ajena → 404 o 403

---

## HU-04 – Cerrar mesa #4

**Como usuario**  
Quiero cerrar una mesa  
Para finalizar la atención y liberar la mesa

### Criterios de aceptación

- Solo mesas con pedido abierto
- Pedido pasa a estado CERRADO
- Mesa vuelve a LIBRE
- Validación por LocalId
- Debe registrarse Medio de Pago
- Registro de timestamp
- No permitir pedido vacío

**AC1:** verificar pedido ABIERTO  
**AC2:** transición a CERRADO  
**AC3:** registrar MedioPago  
**AC4:** si falla, mesa sigue ABIERTA

---

## HU-12 – Cierre de Mesa y Liquidación Final #12

**Como usuario**  
Quiero procesar el pago y cerrar la mesa  
Para finalizar la atención, asegurar el cobro y liberar la mesa.

### Criterios de Aceptación Evolucionados

- Validación de integridad (no vacío / no cerrado)
- Recalcular promociones
- Pagos múltiples
- Transacción atómica (pedido + mesa)
- Snapshot contable (subtotal, descuentos, total)

---

## HU-15: Crear Mesa #15

**Como dueño/administrador del local**  
Quiero crear nuevas mesas

### Criterios
- Número único por local
- Entero positivo > 0
- Estado inicial LIBRE
- Vinculada a LocalId
- Sin límite práctico (hasta 999)



---

## HU-16: Eliminar Mesa #16

**Como dueño/administrador del local**  
Quiero eliminar mesas que ya no existen físicamente

### Criterios
- Solo mesas LIBRES
- No eliminar última mesa
- Debe pertenecer al local
- Eliminación física



---

# 2️⃣ Gestión de Pedidos / Comandas

---

## HU-05 – Agregar productos a un pedido #5

**Como usuario**  
Quiero agregar productos a un pedido abierto

### Criterios
- Repetir productos
- Cantidad y observaciones
- Guardar precio unitario snapshot

---

## HU-07 – Mantener pedido abierto #7

- Permanece abierto hasta cierre manual
- Permite seguir agregando productos

---

## HU-20: Eliminar producto de un pedido #20

- Solo en pedido ABIERTO
- Identificado por ItemPedidoId
- Recalcular promociones (HU-10)
- Recalcular descuento global (HU-14)
- Actualizar totales

---

## HU-21: Modificar cantidad de un producto #21

- Solo en pedido ABIERTO
- Cantidad > 0
- Mantener snapshot de precio
- Re-evaluar promociones
- Recalcular descuentos
- Recalcular total global

---

## HU-30 — Re-apertura y Corrección de Comandas #30

- Solo pedidos CERRADOS
- Resetear montos finales
- Anular pagos
- Mesa vuelve OCUPADA
- Auditoría opcional

---

# 3️⃣ Productos, Variantes y Extras

---

## HU 19 - ABM de productos #19

Atributos:
- Nombre
- Precio Base
- Estado
- Color Hex

Reglas:
- Multi-tenancy
- Nombre único
- Snapshot en pedidos
- No eliminar si está en pedidos activos

---

## HU-26: Gestión de Agregados (Extras) #26

- Ítem puede tener extras
- Precio = base + extras
- Snapshot de precios
- Modificable
- Recalcula promociones y descuentos
- Visible en ticket

---

## HU-27: Botonera de variantes #27

- Agrupar productos por variante
- Registrar producto final elegido
- Observación automática opcional
- Impacto en precio y promociones

---

# 4️⃣ Promociones y Descuentos

---

## HU-08 – Crear promoción #8

Tipos:
- Simple
- NxM
- Condicional
- Precio pack

Reglas:
- Vigencia por fecha/hora/día
- Prioridad
- Asociada al LocalId

---

## HU-09 – Asociar promoción a productos #9

- Definir Trigger
- Definir Target
- Soportar combos mixtos

---

## HU-10 – Aplicar promociones automáticamente #10

- Detectar al agregar producto
- Mostrar descuento al cliente
- Cocina ve cantidad real
- Snapshot inmutable

### Bundle
- Cálculo de paquete
- Recurrencia
- Independiente del precio futuro

---

## HU-14 – Descuento inmediato por porcentaje #14

- Producto o total
- Manual
- Visible
- No modifica precio base
- Auditoría de usuario y fecha

---

# 5️⃣ Stock e Inventario

---

## HU-22 – Control de stock #22

**Como encargado del local**  
Quiero llevar control de existencias

### AC
- Producto con stockActual y controlaStock
- Descontar al cerrar mesa
- Advertir si stock = 0
- Reponer al reabrir pedido
- Ajuste manual

### Estrategia
Usar `StockService` desacoplado del Pedido.

---

# 6️⃣ Caja y Reportes

---

## HU-13 – Cierre de Jornada, Gastos y Consumo Interno #13

Funciones:
- Consumo interno (A CUENTA)
- Venta neta real
- Registro de egresos (monto, motivo, timestamp, voucher)
- Balance de caja:
  - Ventas efectivo
  - Gastos
  - Efectivo a entregar

### Reporte
- Ventas por medio de pago
- Gastos detallados
- Consumo interno

---

# 7️⃣ Infraestructura del Sistema

---

## HU-25 – Inicialización y Contexto de Local Único #25

- LOCAL_ID fijo en `application.yml`
- Seed automático al iniciar
- Crear datos base
- `LocalContextProvider`
- Multi-tenancy por local_id
- Reconstrucción automática tras reset de DB
