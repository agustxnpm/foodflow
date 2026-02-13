# FoodFlow Desktop - Guía de Desarrollo y Despliegue

## Arquitectura Desktop

FoodFlow es una aplicación de escritorio que combina:
- **Frontend**: React + Vite (puerto 3000 en desarrollo)
- **Backend**: Spring Boot + SQLite (puerto 8080)
- **Desktop Wrapper**: Tauri (Rust)

El backend se ejecuta como un **sidecar process** embebido en la aplicación desktop.

---

## Desarrollo Local

### 1. Modo Web (sin Tauri)

```bash
# Terminal 1 - Backend
cd comandas-backend
./mvnw spring-boot:run

# Terminal 2 - Frontend
cd comandas-frontend
npm run dev
```

Abrir: http://localhost:3000

---

### 2. Modo Desktop (con Tauri)

```bash
cd comandas-frontend
npm run tauri:dev
```

Esto automáticamente:
1. Compila el frontend (Vite)
2. Inicia la aplicación Tauri
3. Lanza el backend Spring Boot como sidecar
4. Abre una ventana desktop nativa

**Nota**: El backend tarda ~5 segundos en iniciar. El frontend esperará hasta que esté disponible.

---

## Construcción para Producción

### 1. Compilar Backend

```bash
cd comandas-backend
./mvnw clean package -DskipTests
```

Esto genera:
- `target/comandas-backend-0.0.1-SNAPSHOT.jar`

### 2. Copiar JAR a Tauri

```bash
cp comandas-backend/target/comandas-backend-0.0.1-SNAPSHOT.jar \
   comandas-frontend/src-tauri/binaries/backend.jar
```

### 3. Construir Desktop App

```bash
cd comandas-frontend
npm run tauri:build
```

Esto genera (según plataforma):
- **Windows**: `src-tauri/target/release/bundle/msi/FoodFlow_0.1.0_x64_es-ES.msi`
- **Linux**: `src-tauri/target/release/bundle/appimage/FoodFlow_0.1.0_amd64.AppImage`
- **macOS**: `src-tauri/target/release/bundle/dmg/FoodFlow_0.1.0_x64.dmg`

---

## Estructura de Archivos Críticos

```
comandas-frontend/
├── src-tauri/
│   ├── binaries/
│   │   ├── backend.jar          # Spring Boot executable
│   │   ├── backend.bat          # Windows launcher
│   │   └── backend.sh           # Linux/macOS launcher
│   ├── src/
│   │   ├── main.rs              # Entry point
│   │   └── lib.rs               # Lógica de sidecar
│   ├── tauri.conf.json          # Configuración Tauri
│   └── Cargo.toml               # Dependencias Rust
└── src/
    ├── api/
    │   └── client.ts            # Axios configurado para /api
    └── ...
```

---

## Configuraciones Clave

### tauri.conf.json

```json
{
  "identifier": "com.meiser.foodflow",
  "bundle": {
    "externalBin": ["binaries/backend"],
    "resources": ["binaries/*"]
  },
  "allowlist": {
    "shell": {
      "sidecar": true
    },
    "http": {
      "scope": ["http://localhost:8080/**"]
    }
  }
}
```

### Proceso Sidecar (lib.rs)

El código Rust:
1. Detecta la plataforma (Windows/Linux/macOS)
2. Ejecuta `backend.bat` o `backend.sh`
3. Captura logs del backend
4. Mata el proceso cuando se cierra la app

---

## Seguridad

- El backend **solo** escucha en `localhost:8080`
- No hay acceso desde red externa
- La comunicación frontend-backend es local
- SQLite se guarda en el directorio de la aplicación

---

## Resolución de Problemas

### Backend no inicia

```bash
# Verificar Java instalado
java -version

# Probar backend manualmente
cd comandas-frontend/src-tauri/binaries
java -jar backend.jar
```

### Puerto 8080 ocupado

```bash
# Linux/macOS
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Logs del backend

En modo desarrollo, los logs aparecen en la consola de Tauri:
```
[Backend STDOUT] Started ComandasApplication in 3.456 seconds
```

---

## Despliegue a Restaurante

1. Compartir el archivo `.msi` / `.AppImage` / `.dmg`
2. El usuario instala haciendo doble clic
3. Ejecutar desde el menú de aplicaciones
4. Todo funciona offline (no requiere internet)

---

## Próximos Pasos

- [ ] Configurar auto-actualización (Tauri Updater)
- [ ] Agregar splash screen mientras carga backend
- [ ] Implementar health check visual
- [ ] Configurar logging persistente
- [ ] Crear instalador con logo personalizado

---

## Comandos Rápidos

```bash
# Desarrollo
npm run tauri:dev

# Producción
./mvnw clean package -DskipTests
cp ../comandas-backend/target/*.jar src-tauri/binaries/backend.jar
npm run tauri:build

# Solo compilar sin ejecutar
cargo build --manifest-path src-tauri/Cargo.toml
```
