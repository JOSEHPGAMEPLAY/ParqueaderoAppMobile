# Escáner de Placas - Control de Parqueadero Móvil

Una aplicación móvil Android nativa y moderna diseñada para automatizar la identificación de vehículos, el control de accesos y la gestión financiera de cobros dentro de estacionamientos o parqueaderos.

La aplicación ofrece un enfoque **offline-first (fuera de línea)** de alto rendimiento, permitiendo que la captura, reconocimiento de matrículas y el cálculo de tarifas ocurran de forma 100% local en el dispositivo del operador. Cuenta con un motor de sincronización jerárquico avanzado para respaldar la información en la nube de manera periódica hacia un backend centralizado.

---

## 📱 ¿De qué trata la aplicación?

La aplicación está diseñada para ser utilizada por operadores de parqueaderos en su jornada diaria. Permite registrar el ingreso y salida de automóviles mediante tres modalidades de entrada:

1. **Cámara en Vivo en Tiempo Real**: Utilizando tecnologías de escaneo continuo con ahorro inteligente de batería (la cámara permanece apagada hasta que el operador decide encenderla dinámicamente).
2. **Galería de Imágenes**: Carga de fotografías de vehículos capturadas previamente por el operador.
3. **Ingreso Manual**: Registro directo de placas a través del teclado en caso de matrículas obstruidas, sucias o ilegibles.

### Funcionalidades Clave

* **Reconocimiento de Matrículas (ALPR - Automatic License Plate Recognition)**: Extracción automática de caracteres alfanuméricos mediante inteligencia artificial ejecutada directamente en el chip local del dispositivo móvil (sin latencia de red ni consumo de datos).
* **Validación de Patentes y Restricción Geográfica**: Filtros rigurosos y formatos específicos adaptados a matrículas de **Colombia** (formato de 6 dígitos alfanuméricos de color amarillo para automóviles y 7 dígitos para motocicletas) y **Venezuela** (formatos clásicos y Mercosur), eliminando falsos positivos o caracteres erráticos.
* **Motor Tarifador de Cobros Automatizado**: Cálculo del precio del estacionamiento basado en el tiempo exacto transcurrido, segmentado por horas completas y fracciones (con tolerancias y redondeos paramétricos), admitiendo tarifas distintas según el escenario de registro ("Con Ticket" o "Sin Ticket / Registro Manual").
* **Clasificador Determínístico de Vehículos**: Generación coherente y uniforme de marca y color para cada vehículo mediante técnicas de hashing locales basadas en la matrícula, evitando la necesidad de un pesado modelo de visión por computadora para rellenar campos informativos complementarios.
* **Sistema de Notas y Historial de Comentarios**: Permite añadir observaciones por cada vehículo (ej. "Golpe leve en puerta trasera"). Cuenta con un analizador de sintaxis que procesa las anotaciones localmente, asigna marcas de tiempo, registra al operador encargado y mantiene estados de sincronización individuales por comentario.
* **Sincronización Jerárquica e Híbrida (Nube - Local)**: Sincronización en lotes hacia una API REST remota, implementando un sistema de **Doble ID** (IDs Locales de SQLite y IDs Remotos generados por MongoDB) y control de sincronización granular. Un registro no se marca como sincronizado en su totalidad hasta que finalice su estancia en el parqueadero y todos sus comentarios hayan sido respaldados exitosamente.

---

## 🛠️ Tecnologías Utilizadas

La arquitectura de software de la aplicación se ha construido siguiendo rigurosamente las guías recomendadas por Google para el desarrollo de Android moderno:

* **Lenguaje**: **Kotlin** moderno con soporte nativo de Coroutines y flujos reactivos (Flow / StateFlow) para garantizar operaciones no bloqueantes.
* **Interfaz de Usuario**: **Jetpack Compose** aplicando el sistema de diseño moderno **Material Design 3 (M3)**. Ofrece insets de pantalla completa (*edge-to-edge*), soporte para temas dinámicos claros/oscuros automáticos, transiciones estéticas y retroalimentación táctil (*ripple effects*).
* **Base de Datos Local (Persistencia)**: **Room Database** sobre un motor **SQLite** nativo. Permite un acceso rápido a datos indexados, persistiendo el historial de transacciones, tarifas, comentarios y metadatos de acceso de forma permanente.
* **Reconocimiento Óptico de Caracteres (OCR)**: Integración con **Google ML Kit (Text Recognition)** en su variante *on-device*. Proporciona reconocimiento de texto veloz y preciso a nivel local en milisegundos.
* **Control de Cámara**: **CameraX** de Jetpack. Permite configurar resoluciones óptimas, gestionar enfoques táctiles e interactuar con el hardware físico de captura de manera eficiente.
* **Arquitectura de Software**: Patrón **MVVM (Model-View-ViewModel)**. Garantiza la separación limpia de responsabilidades:
  * **View (Jetpack Compose)**: Renderiza las pantallas observando de forma segura los flujos de estado del ViewModel.
  * **ViewModel (`ScanViewModel`)**: Centraliza las lógicas del negocio, el procesamiento de OCR, los cálculos del motor tarifario y la orquestación de la base de datos.
  * **Model / Repositorio (`ScanRepository`)**: Abstrae las fuentes de datos físicas (Room local y API REST remota).
* **Consumo de Servicios Web (Sincronización Backend)**: **Retrofit 2** con convertidor Gson para la comunicación REST segura con el servidor remoto.
* **Manejo de Imágenes**: **Coil (Coroutines Image Loader)** para la carga y renderizado óptimo en memoria de miniaturas de vehículos capturados.

---

## 🔄 Protocolo de Sincronización Avanzado (Backend REST API)

La aplicación soporta un modelo de integración robusto con un servidor backend centralizado (ej. Node.js + Express + MongoDB).

```
   ┌───────────────────────┐                    ┌───────────────────────┐
   │                       │   (Batch Sync)     │                       │
   │  Dispositivo Android  ├───────────────────>│      Servidor API     │
   │   (Room DB - SQLite)  │   POST /sync       │   (Express - Node.js) │
   │                       │                    │                       │
   │  Mantiene:            │<───────────────────┤  Retorna:             │
   │  - ID Local           │   (ID Mappings)    │  - ID Mapeado Remoto  │
   │  - Estado isSynced    │                    │    (de MongoDB _id)   │
   └───────────────────────┘                    └───────────────────────┘
```

### Características del Motor de Sync

1. **Estructura Jerárquica**: Los datos se agrupan en balances de fecha por día (`dailyRecords`), conteniendo colecciones de transacciones de parqueo (`parkingRecords`), las cuales a su vez contienen listas anidadas de comentarios nuevos (`comments`).
2. **Doble ID (Mapeo Bidireccional)**: Evita la duplicación de datos. El backend procesa el lote, realiza upserts lógicos basados en el identificador local y genera o actualiza el registro en MongoDB, devolviendo un mapa de resolución de ID local a ID remoto (`idMappings`).
3. **Resiliencia Offline**: En caso de no contar con conexión a internet, el operador puede simular la sincronización en el dispositivo para verificar flujos operacionales. Cuando la red se restablece, el motor REST sincroniza automáticamente las transacciones pendientes reemplazando los identificadores de simulación por los reales proporcionados por el backend.

*Para detalles de integración, modelos Mongoose y ejemplos JSON de payloads de request y response, consulta la especificación técnica en [backend_integration_requirements.md](backend_integration_requirements.md).*

---

## 📁 Estructura del Proyecto

```text
/
├── app/                          # Código fuente del módulo de Android
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # Manifiesto de Android (Permisos de cámara y almacenamiento)
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt   # Interfaz principal, Jetpack Compose UI y control de CameraX
│   │   │   ├── ScanViewModel.kt  # ViewModel, motor OCR local, tarifador y estado de sincronización
│   │   │   ├── ScanDatabase.kt   # Entidades de Room, DAO y persistencia de SQLite local
│   │   │   ├── BitmapUtils.kt    # Rotación, redimensionamiento y almacenamiento temporal de imágenes
│   │   │   └── ui/theme/         # Tema Material Design 3, paletas dinámicas y tipografías
│   │   └── res/                  # Recursos XML, íconos vectoriales y cadenas strings.xml
├── assets/                       # Recursos visuales del proyecto
├── backend_integration_requirements.md # Especificación completa del Backend Node.js y MongoDB
├── MANUAL_TECNICO.md             # Guía detallada de arquitectura, flujo de hilos y algoritmos locales
├── build.gradle.kts              # Script de compilación del proyecto de Gradle
└── settings.gradle.kts           # Configuración de repositorios y módulos del proyecto
```

---

## 🚀 Compilación y Ejecución

### Requisitos Previos
* **Android Studio Ladybug** o superior.
* **JDK 17** configurado en las variables de entorno de tu IDE.
* Dispositivo físico Android (recomendado para usar CameraX en vivo) o emulador Android con soporte para cámara simulada.

### Pasos para Compilar
1. Clona el repositorio del proyecto en tu máquina local.
2. Abre Android Studio y selecciona **Open an existing project**, seleccionando la carpeta raíz.
3. Deja que Gradle descargue las dependencias y sincronice el proyecto automáticamente.
4. Conecta tu teléfono Android mediante depuración USB o enciende el emulador.
5. Haz clic en el botón **Run** (`Shift + F10`) en Android Studio para compilar e instalar la aplicación en el dispositivo.

---

## 🔒 Permisos Requeridos

La aplicación solicita dinámicamente los siguientes accesos para asegurar su correcto funcionamiento:
* `android.permission.CAMERA`: Requerido para capturar fotos de patentes en vivo en el módulo de escaneo.
* `android.permission.READ_MEDIA_IMAGES` / `WRITE_EXTERNAL_STORAGE`: Utilizados de forma segura para seleccionar imágenes de la galería local y persistir capturas temporales del parqueadero.
* `android.permission.INTERNET`: Necesario para realizar solicitudes de sincronización de lotes con el backend remoto.
