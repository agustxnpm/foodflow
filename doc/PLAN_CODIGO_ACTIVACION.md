# Plan de Implementación — Sistema de Código de Activación

## Objetivo

Eliminar toda identidad hardcodeada (`local-id`, nombre del local, dirección, etc.) para que exista **un solo build distribuible** a todos los clientes. La identidad del tenant y los datos del local se resuelven en **runtime**, no en tiempo de compilación ni en archivos YAML.

### Flujo de primer arranque

```
App inicia → ¿Existe identidad en DB? 
  → NO  → Pantalla de Activación (UUID + datos del local)
  → SÍ  → Flujo normal (license check → jornada → salón)
```

---

## Paso 1 — Entidad JPA `LocalConfigEntity`

**Archivo:** `infrastructure/persistence/entity/LocalConfigEntity.java`

Nueva entidad que persiste la identidad del tenant **y los datos del local** en SQLite.

```java
@Entity
@Table(name = "local_config")
public class LocalConfigEntity {
    @Id
    private String key;       // "local-id"

    @Column(nullable = false)
    private String value;     // UUID como string, o dato del local
}
```

**Filas esperadas después de la activación:**

| key                  | value                                  |
|----------------------|----------------------------------------|
| `local-id`           | `uuid del cliente`                     |
| `nombre-local`       | `"Pizzería Don Julio"`                 |
| `direccion`          | `"Av. San Martín 1234"`                |
| `telefono`           | `"2804-551234"`                        |
| `cuit`               | `"20-12345678-9"`                      |
| `mensaje-bienvenida` | `"¡Gracias por su visita!"`            |

> Diseño key-value para evitar crear una tabla con N columnas que muta cada vez que se agrega un dato del local. Escala sin schema changes.

---

## Paso 2 — Repositorio JPA `LocalConfigJpaRepository`

**Archivo:** `infrastructure/persistence/repository/LocalConfigJpaRepository.java`

```java
public interface LocalConfigJpaRepository extends JpaRepository<LocalConfigEntity, String> {
    // findById("local-id") → Optional<LocalConfigEntity>
}
```

Sin lógica custom — Spring Data resuelve todo.

---

## Paso 3 — Controller de identidad `/api/config/identity`

**Archivo:** `presentation/controller/ConfigController.java`

Dos endpoints:

### `GET /api/config/identity`

- Si existe la fila `local-id` en `local_config` → devuelve `{ configured: true, localId: "uuid", nombreLocal: "...", ... }`
- Si no existe → devuelve `{ configured: false }`
- **No requiere header `X-Local-Id`** (todavía no existe identidad)

### `POST /api/config/identity`

- Recibe body: `{ localId: "uuid", nombreLocal: "...", direccion: "...", telefono: "...", cuit: "...", mensajeBienvenida: "..." }`
- Valida que `localId` sea UUID válido
- Valida que NO exista ya una identidad (idempotencia: si ya existe con el mismo UUID, retorna OK)
- Persiste todas las filas en `local_config`
- Ejecuta el seed del local: crea `LocalEntity` + `MesaEntity` mesa 1 (lógica actual de `MeisenSeedService`)
- Retorna `201 Created` con el `localId`

### Request de ejemplo:
```json
{
  "localId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "nombreLocal": "Pizzería Don Julio",
  "direccion": "Av. San Martín 1234",
  "telefono": "2804-551234",
  "cuit": "20-12345678-9",
  "mensajeBienvenida": "¡Gracias por elegirnos!"
}
```

---

## Paso 4 — Adaptar `FixedLocalContextProvider`

**Archivo:** `infrastructure/config/FixedLocalContextProvider.java`

**Antes:** Lee `MeisenProperties.getLocalId()` (YAML).  
**Después:** Lee de `LocalConfigJpaRepository.findById("local-id")`.

- Si no encuentra la fila → lanza excepción clara: `"Local no configurado. Ejecutar activación."`
- Cachea el valor en memoria (el local-id nunca cambia en runtime)

---

## Paso 5 — Adaptar `MeisenSeedService`

**Archivo:** `infrastructure/seed/MeisenSeedService.java`

**Antes:** `CommandLineRunner` que ejecuta en arranque leyendo de `MeisenProperties`.  
**Después:** Ya no es `CommandLineRunner`. Se convierte en un **servicio invocable** desde `ConfigController.POST`.

Lógica:
1. Recibe `UUID localId` y datos del local como parámetros
2. Crea `LocalEntity` si no existe (misma lógica actual)
3. Crea `MesaEntity` mesa 1 si no existe

Esto garantiza que el seed solo corre **después** de la activación, no en arranque ciego.

---

## Paso 6 — Adaptar consumidores de `MeisenProperties.LocalProperties`

Los siguientes archivos leen datos del local (nombre, dirección, CUIT, etc.) desde `MeisenProperties`:

- `TicketImpresionMapper.java`
- `EnviarComandaCocinaUseCase.java`
- `GenerarTicketVentaUseCase.java`

**Antes:** Inyectan `MeisenProperties` y leen `getLocal().getNombreLocal()`, etc.  
**Después:** Leen de `LocalConfigJpaRepository` (misma tabla key-value del paso 1).

Se crea un servicio auxiliar `LocalConfigService` en infraestructura que encapsula la lectura de `local_config` y cachea los valores:

```java
@Service
public class LocalConfigService {
    // Lee todas las filas de local_config y las expone
    public String getNombreLocal() { ... }
    public String getDireccion() { ... }
    // etc.
}
```

Los casos de uso que necesitan datos del local reciben este servicio (o un puerto) en lugar de `MeisenProperties`.

---

## Paso 7 — Limpiar `MeisenProperties` y YAML

### `MeisenProperties.java`

**Se elimina completamente.** Ya no se necesita:
- `localId` → viene de DB (`local_config` key `local-id`)
- `LocalProperties` (nombre, dirección, etc.) → vienen de DB (`local_config` keys dinámicas)

### `application-offline.yml`

**Se elimina toda la sección `app.context`:**

```yaml
# ANTES
app:
  context:
    local-id: 123e4567-e89b-12d3-a456-426614174000
    local:
      nombre-local: "Mi Local"
      direccion: ""
      telefono: ""
      cuit: ""
      mensaje-bienvenida: "¡Gracias por su visita!"

# DESPUÉS
# (sección eliminada — todo viene de la base de datos)
```

**Impacto:** Ningún dato del local queda hardcodeado. Todo es configurable por el cliente desde la pantalla de activación y, en el futuro, editable desde una pantalla de ajustes.

---

## Paso 8 — Frontend: `PantallaActivacion.tsx`

**Archivo:** `features/activacion/PantallaActivacion.tsx`

Pantalla de primer arranque. Se muestra cuando `GET /api/config/identity` devuelve `{ configured: false }`.

### Contenido:

1. **Campo de texto: Código de Activación** — el cliente pega el UUID que le dimos
2. **Campos del local:**
   - Nombre del local (obligatorio)
   - Dirección (opcional)
   - Teléfono (opcional)
   - CUIT (opcional)
   - Mensaje de bienvenida (opcional, placeholder: "¡Gracias por su visita!")
3. **Botón "Activar"** → `POST /api/config/identity` con todos los campos
4. Si éxito → recarga la app (el interceptor ya puede inyectar `X-Local-Id`)
5. Si error → mensaje claro ("Código inválido", "Error de conexión", etc.)

### Diseño:
- Centrada, limpia, sin navbar
- Paleta roja/negra del sistema
- Campos grandes (touch-friendly)
- Logo de FoodFlow arriba

---

## Paso 9 — Frontend: Modificar `apiClient.ts`

**Archivo:** `lib/apiClient.ts`

**Antes:** El interceptor lee `import.meta.env.VITE_LOCAL_ID` (build-time) y lo inyecta como `X-Local-Id`.  
**Después:**

1. En arranque, el frontend hace `GET /api/config/identity`
2. Si `configured: true` → guarda `localId` en memoria (variable de módulo o Zustand)
3. El interceptor inyecta ese `localId` dinámico como `X-Local-Id`
4. Si `configured: false` → no inyecta header (solo el endpoint de config funciona sin él)

**Excepción:** Las llamadas a `/api/config/identity` no llevan `X-Local-Id` (son pre-activación).

---

## Paso 10 — Frontend: Modificar `useLicencia.ts`

**Archivo:** `hooks/useLicencia.ts`

**Antes:** Usa `import.meta.env.VITE_LOCAL_ID` para buscar en el Gist.  
**Después:** Recibe el `localId` dinámico (del mismo store/variable que usa `apiClient`).

Si no hay `localId` (app no activada), el hook retorna `null` — no intenta validar licencia.

---

## Paso 11 — Frontend: Modificar `AppRouter.tsx`

**Archivo:** `routes/AppRouter.tsx`

Nuevo flujo de gatekeeping:

```
App inicia
  → GET /api/config/identity
    → { configured: false } → <PantallaActivacion />
    → { configured: true }  → guardar localId
      → useLicencia(localId)
        → BLOQUEADO → <PantallaBloqueoLicencia />
        → POR_VENCER → banner + app normal
        → ACTIVO → app normal
```

### Hook `useIdentidad()`

Nuevo hook que encapsula `GET /api/config/identity`:

```typescript
// Retorna { configured, localId, nombreLocal, ... } | { configured: false }
const { data, isLoading } = useIdentidad();
```

Se usa en `AppRouter` para decidir qué pantalla mostrar antes que cualquier otra lógica.

---

## Paso 12 — Limpiar variables de entorno y CI

### `.env` (frontend)

```env
# ANTES
VITE_LOCAL_ID=123e4567-e89b-12d3-a456-426614174000
VITE_API_URL=http://localhost:8080/api
VITE_LICENSE_GIST_URL=https://gist.githubusercontent.com/...

# DESPUÉS
VITE_API_URL=http://localhost:8080/api
VITE_LICENSE_GIST_URL=https://gist.githubusercontent.com/...
```

Se elimina `VITE_LOCAL_ID` — ya no existe.

### `DevTimeTravelPanel.tsx`

Adaptar para leer `localId` dinámico en lugar de `import.meta.env.VITE_LOCAL_ID`.

### `.github/workflows/build-windows.yml`

Eliminar el secret `VITE_LOCAL_ID` del paso de build. Ya no se necesita.

---

## Resumen de archivos afectados

### Backend — Crear
| Archivo | Descripción |
|---------|------------|
| `LocalConfigEntity.java` | Entidad JPA key-value |
| `LocalConfigJpaRepository.java` | Repositorio Spring Data |
| `ConfigController.java` | GET/POST `/api/config/identity` |
| `LocalConfigService.java` | Lectura cacheada de datos del local |

### Backend — Modificar
| Archivo | Cambio |
|---------|--------|
| `FixedLocalContextProvider.java` | Leer de DB en vez de YAML |
| `MeisenSeedService.java` | De `CommandLineRunner` a servicio invocable |
| `TicketImpresionMapper.java` | Usar `LocalConfigService` en vez de `MeisenProperties` |
| `EnviarComandaCocinaUseCase.java` | Usar `LocalConfigService` en vez de `MeisenProperties` |
| `GenerarTicketVentaUseCase.java` | Usar `LocalConfigService` en vez de `MeisenProperties` |
| `ApplicationConfig.java` | Actualizar wiring de beans |
| `application-offline.yml` | Eliminar sección `app.context` |

### Backend — Eliminar
| Archivo | Razón |
|---------|-------|
| `MeisenProperties.java` | Reemplazado por `LocalConfigService` + DB |

### Frontend — Crear
| Archivo | Descripción |
|---------|------------|
| `PantallaActivacion.tsx` | Pantalla de primer arranque |
| `useIdentidad.ts` | Hook para `GET /api/config/identity` |

### Frontend — Modificar
| Archivo | Cambio |
|---------|--------|
| `apiClient.ts` | Interceptor con `localId` dinámico |
| `useLicencia.ts` | Recibir `localId` dinámico |
| `AppRouter.tsx` | Agregar gate de identidad |
| `DevTimeTravelPanel.tsx` | Leer `localId` dinámico |
| `.env` | Eliminar `VITE_LOCAL_ID` |

### CI/CD — Modificar
| Archivo | Cambio |
|---------|--------|
| `build-windows.yml` | Eliminar secret `VITE_LOCAL_ID` |

---

## Orden de implementación recomendado

```
Backend primero (el frontend depende de los endpoints):

  1. LocalConfigEntity + Repository          (paso 1-2) --Hecho
  2. ConfigController GET/POST               (paso 3) --Hecho
  3. LocalConfigService                      (paso 6) --hecho 
  4. Adaptar FixedLocalContextProvider        (paso 4) -- hecho
  5. Adaptar MeisenSeedService               (paso 5) --hecho
  6. Adaptar consumidores de LocalProperties  (paso 6) --hecho
  7. Limpiar MeisenProperties + YAML         (paso 7)--hecho
  8. Tests backend --hecho

Frontend después:

  9.  useIdentidad hook                      (paso 11)--hecho
  10. PantallaActivacion                     (paso 8)--hecho
  11. Adaptar apiClient.ts                   (paso 9)--hecho
  12. Adaptar useLicencia.ts                 (paso 10)--hecho
  13. Adaptar AppRouter.tsx                  (paso 11)--hecho
  14. Limpiar .env, DevTimeTravel, CI        (paso 12)--hecho
  15. Tests frontend / E2E manual
```

---

## Resultado final

- **Un solo build** sirve para todos los clientes
- **Cero configuración en YAML** — todo vive en la DB
- **Datos del local editables** por el cliente (nombre, dirección, teléfono, mensaje)
- La identidad se resuelve en **runtime**, no en compilación
- El flujo de primer arranque es claro y guiado
- Compatible con el sistema de licencias por Gist ya implementado
