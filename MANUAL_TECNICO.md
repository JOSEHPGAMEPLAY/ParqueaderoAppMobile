# MANUAL TÉCNICO: ESPECTRO COMPLETO Y ARQUITECTURA OFFLINE OPTIMIZADA
## Aplicación: Escáner de Placas y Control de Parqueadero (100% Local / Android)

---

## 1. Descripción General del Sistema

La aplicación **Escáner de Placas** es una herramienta de software móvil diseñada para automatizar la identificación de vehículos y la gestión financiera de cobros dentro de un parqueadero o de áreas de control de accesos. 

El sistema permite capturar la matrícula de un automóvil mediante tres flujos operativos independientes y **100% fuera de línea (offline)**:
1. **Cámara en Tiempo Real**: Captura dinámica nativa utilizando CameraX.
2. **Galería Local**: Carga de imágenes estáticas previas de vehículos.
3. **Módulo de Entrada Manual**: Registro directo por teclado en situaciones donde las placas estén cubiertas o sean ilegibles.

### Funcionalidades Primordiales:
* **Lectura Automática de Matrículas (ALPR - Automatic License Plate Recognition)**: Extracción ultra rápida de caracteres alfanuméricos mediante reconocimiento óptico de caracteres (OCR) ejecutado en el chip local del dispositivo.
* **Procesamiento de Alto Rendimiento 100% Offline**: Eliminación completa de peticiones de red y modelos multimodales remotos (como Gemini API) para garantizar latencia de milisegundos, máximo ahorro de datos móviles, privacidad de datos vehiculares y un funcionamiento idóneo incluso en sótanos de estacionamiento sin señal celular.
* **Clasificador Determínístico de Atributos**: Generación matemática coherente de marcas y colores de vehículos en el dispositivo mediante técnicas de hashing para poblar la base de datos de manera uniforme.
* **Motor Tributario y de Cobros (Tarifador)**: Cálculo automático de montos adeudados con base en el tiempo transcurrido (horas completas más una fracción que solo se cobra al superar el umbral configurado) y tipologías tarifarias configurables desde la web (Con Ticket vs. Registro Manual). Las tarifas, el mínimo de horas, los minutos gratis y la moneda se descargan de la configuración central y se cachean para operar sin conexión.
* **Base de Datos Local y Control de Historial**: Gestión de inventario de vehículos activos del parqueadero (Vehículos Adentro) y bitácora de salidas persistida en SQLite mediante Room.

---

## 2. Diagrama de la Arquitectura de Software

La aplicación está implementada siguiendo rigurosamente el patrón arquitectónico estructurado **MVVM (Model-View-ViewModel)** adaptado a prácticas modernas de desarrollo de Android con **Jetpack Compose** y **Kotlin Coroutines / Flows**.

```
  ┌─────────────────────────────────────────────────────────────┐
  │                        CAPA DE VISTA                        │
  │                     (Jetpack Compose UI)                    │
  │  - MainActivity.kt (MainScreen, CameraPreview, Panels)     │
  │  - Theme.kt / Color.kt / Type.kt (Diseño Material M3)      │
  └──────────────▲──────────────────────────────┬───────────────┘
                 │ (StateFlow / UI State)       │ (Eventos / Acciones)
  ┌──────────────┴──────────────────────────────▼───────────────┐
  │                     CAPA DE VIEWMODEL                       │
  │                       (ScanViewModel)                       │
  │  - Conservación del Estado Reactivo                        │
  │  - Orquestación de Motor OCR Local On-Device                │
  │  - Clasificador Heurístico e Inteligencia del Negocio      │
  │  - Lógicas Financieras del Parqueadero                      │
  └──────────────▲──────────────────────────────┬───────────────┘
                 │ (Datos Reactivos / Flow)     │ (Operaciones Suspendidas)
  ┌──────────────┴──────────────────────────────▼───────────────┐
  │                       CAPA DE MODELO                        │
  │                                                             │
  │   ┌─────────────────────────────────────────────────────┐   │
  │   │                PERSISTENCIA LOCAL                   │   │
  │   │                  (Room Database)                    │   │
  │   │  - ScanDatabase.kt                                  │   │
  │   │  - ScanResult (Entity)                              │   │
  │   │  - ScanResultDao                                    │   │
  │   │  - ScanResultRepository                             │   │
  │   └─────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────┘
```

---

## 3. Estructura de Archivos del Proyecto

La composición del módulo de Android se estructura de la siguiente manera:

```text
/app/src/main/
├── AndroidManifest.xml                  # Declaración de actividades, usos de cámara, almacenamiento privado.
├── java/com/example/
│   ├── MainActivity.kt                  # UI Principal en Jetpack Compose, controladores de cámara y galerías.
│   ├── ScanViewModel.kt                 # State Manager, orquestador de OCR local, base de datos y lógicas financieras.
│   ├── ScanDatabase.kt                  # Definición de la entidad, DAO, Repositorio y Base de Datos Room.
│   ├── BitmapUtils.kt                   # Conversor de ImageProxy a Bitmap con rotación y gestión de caché en disco.
│   ├── GeminiApiService.kt              # Capas de datos Moshi heredadas (Deprecadas de red, mantenidas para compatibilidad).
│   └── ui/theme/
│       ├── Color.kt                     # Esfera y paleta cromática Material 3 (Personalizada).
│       ├── Theme.kt                     # Inicializador de temas diurno y nocturno automáticos.
│       └── Type.kt                      # Configuración tipográfica jerárquica.
└── res/
    ├── drawable/                        # Recursos de íconos vectoriales dinámicos.
    ├── values/
    │   ├── strings.xml                  # Almacén de cadenas de texto internacionalizables.
    │   └── colors.xml                   # Paletas estáticas XML de legado.
    └── xml/                             # Configuraciones de reglas de backup de datos.
```

---

## 4. Descripción Detallada de los Componentes

### A. Capa de Presentación (Vista)

#### `MainActivity.kt`
Contiene el punto de entrada de la ejecución de la aplicación y declara las pantallas de Jetpack Compose integrando `Scaffold` con insets de pantalla completa (`enableEdgeToEdge()`).
* **`MainScreen`**: El contenedor central que observa el estado reactivo del `ViewModel` a través de `.collectAsStateWithLifecycle()`. Implementa un sistema refinado de dos pestañas segmentadas que maximiza la simplicidad:
  1. **Escanear Placa**: Tab central único y unificado que combina las acciones de captura. Permite activar la cámara o seleccionar de galería de forma rápida sobre la misma pantalla.
  2. **Tarifas/Prueba**: Modificadores de tarifas en tiempo real y panel de simulación de pruebas con generador dinámico de placas sintéticas pintadas en canvases de memoria.
* **Sistema de Optimización de Batería (Cámara On-Demand)**: El hardware de la cámara permanece completamente inactivo (apagado) por defecto para ahorrar batería. El usuario lo activa intencionalmente con el botón `"Cámara En Vivo"`, y puede desactivarlo rápidamente haciendo clic en el botón flotante `"Apagar"`.
* **Filtros por Países Restringidos (Colombia & Venezuela)**: Todos los algoritmos y selectores manuales en la interfaz del simulador y registro manual están restrictivamente limitados a Colombia (caras amarillas de 6 dígitos alfanuméricos u 7 para motos) y Venezuela (formato clásico y Mercosur), eliminando ruido o detecciones erráticas de otras naciones.
* **Botón de Ingreso Manual**: Lanza un modal de entrada de datos con validaciones estrictas y un selector dropdown de países para el ingreso directo por teclado.
* **`CameraPreview`**: Configura un `AndroidView` que interactúa con la API de CameraX (`ProcessCameraProvider`), enlazando el ciclo de vida de la actividad con el análisis y la toma final de capturas. Consume hilos concurrentes especializados del sistema operativo para evitar la congelación del frame visual principal.

### B. Capa de Arquitectura de Negocio (ViewModel)

#### `ScanViewModel.kt`
Este componente gestiona el estado de toda la UI encapsulado dentro del flujo unidireccional `ScanUiState` (que puede variar entre `Idle`, `Processing`, `Success` y `Error`).
* **`processImage`**: Es la función asíncrona principal. Lanza un hilo de ejecución usando `viewModelScope.launch` que ejecuta de forma secuencial y ordenada:
  1. El reconocimiento OCR superveloz a nivel local con el motor nativo de Google ML Kit en el dispositivo. No usa internet.
  2. El formateador determinístico y clasificador offline `generateDeterministicDetails` que mapea y asocia la matrícula hallada a un tipo de coche, marca, color y precisión sin enviar datos a la red de forma exclusiva para Colombia o Venezuela.
  3. El guardado persistente físico de la imagen capturada en la subcarpeta del directorio privado del sistema organizada por días de manera estructurada (`/files/scanned_plates/YYYY-MM-DD/`) para optimizar el almacenamiento del historial de forma escalable (previniendo cuellos de botella en el sistema de archivos de Android).
  4. El alta reactiva de un registro tipo `"ACTIVE"` (vehículo dentro del parqueadero) en la base de datos Room. El listado en pantalla se agrupa dinámicamente y de forma ágil por días en base a estos encabezados temporales para mantener tiempos de renderizado y cargas óptimos de memoria (O(1) layout updates).
* **`calculateParkingFee`**: Calcula matemáticamente el cobro según el minutero del sistema, el tiempo transcurrido en tiempo real y los parámetros de tarifas vigentes.

### C. Capa de Datos Locales (Model / Room Database)

#### `ScanDatabase.kt`
* **`ScanResult`**: Entidad/Tabla persistente en SQLite que conserva el estado histórico de los registros.
  * `id`: Clave primaria autoincremental de tipo numérico `Long`.
  * `plateText`: Cadena de texto que contiene los dígitos de la matrícula normalizados a mayúsculas y formateados uniformemente.
  * `imagePath`: Ruta física de la imagen almacenada.
  * `timestamp`: Fecha/Hora exacta en milisegundos de entrada (Modificable en la UI para pruebas rápidas de tarifas).
  * `exitTime`: Fecha/Hora exacta de salida (nulo mientras esté activo).
  * `paidAmount`: Cobro finalizado.
  * `status`: Estado actual de control interno: `"ACTIVE"` (Dentro del lote) o `"COMPLETED"` (Checkout realizado).
* **`ScanResultDao`**: Interfaz de acceso que expone consultas reactivas que retornan `Flow<List<ScanResult>>` notificando instantáneamente la adición, eliminación o edición de vehículos sin forzar consultas manuales repetitivas.
* **`ScanResultRepository`**: Abstracción arquitectónica intermedia que aísla de forma segura el origen de datos local.

### D. Capa de Red (Depreación de APIs Online)

#### `GeminiApiService.kt`
* Se conserva únicamente la clase de datos estructurados de transferencia `ParsedPlateInfo`. Los clientes de comunicación de red con servidores remotos de Google e interfaces de Retrofit 2 se encuentran inactivos por compatibilidad del compilador y están completamente puenteados por los algoritmos on-device nativos de ML Kit para proveer una solución robusta y local.

---

## 5. El Algoritmo de Reconocimiento de Placas (Flujo 100% Offline)

El corazón tecnológico clave del sistema radica en su funcionamiento autónomo. El sistema prescinde de latencias de red para realizar un análisis sumamente óptimo:

```
                  ┌───────────────────────────────┐
                  │   Captura de Fotografía       │
                  │   (Cámara / Galería Recibida) │
                  └──────────────┬────────────────┘
                                 │
                                 ▼
                  ┌───────────────────────────────┐
                  │       Local ML Kit OCR        │
                  │    - On-Device, Instantáneo   │
                  │    - Lectura de texto crudo   │
                  └──────────────┬────────────────┘
                                 │
                                 ▼
                  ┌───────────────────────────────┐
                  │    Parser de Patentes Local   │
                  │    - Limpieza de caracteres  │
                  │    - Análisis heurístico      │
                  │    - Mapeo RegEx geográfico   │
                  └──────────────┬────────────────┘
                                 │
                                 ▼
                  ┌───────────────────────────────┐
                  │  Clasificador Determinístico  │
                  │  - Asigna color, marca y país │
                  │  - Generación local por Hash  │
                  │  - Confianza computada local  │
                  └──────────────┬────────────────┘
                                 │
                                 ▼
         ┌─────────────────────────────────────────────────────────────────────┐
         │  Asignación y Normalización de Placa formateada                     │
         │  Guardado en SQLite (Room) + Caché de Foto                          │
         │  Actualización Reactiva de pantalla en Tiempo Real                  │
         └─────────────────────────────────────────────────────────────────────┘
```

### DETALLES DE IMPLEMENTACIÓN:

#### 1. Análisis Heurístico Local de Exclusiones y Formateo
En el archivo `ScanViewModel.kt`, la función `extractPlateInfoFromOcr` analiza las líneas de texto localizadas por ML Kit.
* **Filtro de Ruido**: Remueve palabras que causan errores de lectura debido a los logos de los marcos de las placas como `"COLOMBIA"`, `"VENEZUELA"`, `"GOBIERNO"`, `"MINISTERIO"`, `"REPUBLICA"`, `"BOLIVARIANA"`.
* **Identificación Geográfica**: Mapea marcas geográficas para asignar el país de origen de forma inteligente si en la placa o el marco se lee un texto clave (`BOGOTA` -> Colombia, `CARACAS` -> Venezuela, `CDMX` -> México, `MADRID` -> España).
* **Filtro RegEx Alfanumérico por Regiones**:
  * **Colombia de Uso Privado**: Recomienda patrones de 6 caracteres tipo `[A-Z]{3}[0-9]{3}` (ej. `LKM 519`) agregando el espaciado correcto mediante la lógica:
    ```kotlin
    "${raw.take(3)} ${raw.substring(3)}"
    ```
  * **Colombia de Motocicleta**: Identifica patrones tipo `[A-Z]{3}[0-9]{2}[A-Z]` (ej. `AAA 12A`) ajustando el formateo.
  * **Venezuela Mercosur**: Analiza patrones tipo `[A-Z]{2}[0-9]{3}[A-Z]{2}` (ej. `AB 123 CD`).
  * **Venezuela Clásica / Brasil Estilo Mercosur**: Identifica patrones tipo `[A-Z]{3}[0-9]{4}` (ej. `AAA 1234`).

#### 2. Clasificación Heurística por Hash en el Dispositivo
Para poblar campos valiosos del vehículo (marca y color) sin recurrir a costosas peticiones y análisis de visión artificial en línea, el sistema calcula un código hash a partir de la placa:
```kotlin
val cleanText = plateText.uppercase().replace(Regex("[^A-Z0-9]"), "")
val hashCode = kotlin.math.abs(cleanText.hashCode())
val selectedBrand = brands[hashCode % brands.size]
val selectedColor = colors[hashCode % colors.size]
```
Esto genera una asociación unívoca de por vida entre una placa y sus descripciones, haciendo que, de forma enteramente offline, el mismo coche escaneado en diferentes días siempre se identifique consistentemente bajo la misma marca y gama cromática, mejorando la coherencia y usabilidad del parqueadero.

---

## 6. Módulo Tarifario y Lógica del Parqueadero

La gestión de cobro funciona mediante un sistema parametrizable que se almacena de forma persistente en `SharedPreferences` para soportar actualizaciones instantáneas desde la pestaña de simulación/tarifas.

### Tarifas Configurables (COP):
1. **Tarifas CON TICKET (Lectura Física del boleto electrónico)**:
   * **Tarifa Hora Completa**: $2,000 COP (Por defecto).
   * **Tarifa Fracción / Media Hora**: $1,000 COP (Por defecto).
2. **Tarifas SIN TICKET (Ingreso Manual alternativo)**:
   * **Tarifa Hora Completa**: $4,000 COP (Por defecto).
   * **Tarifa Fracción / Media Hora**: $2,000 COP (Por defecto).

### Lógica de Cálculo de Tarifa (`calculateParkingFee`):
1. Si el tiempo transcurrido es menor o igual a **0 minutos**, el cobro es `$0.0`.
2. Si el vehículo ha estado estacionado entre **1 y 30 minutos**, se le cobra exactamente la tarifa de la fracción (**Media Hora**).
3. Si el vehículo ha estado estacionado entre **31 y 60 minutos**, se le cobra el valor equivalente a una **Hora Completa**.
4. Si el vehículo supera los **60 minutos**:
   * Se divide el tiempo total calculando las horas completas totales transcurridas.
   * Se calcula el residuo de los minutos sobrantes:
     * Si los minutos restantes son menores o iguales a 0: cobro adicional = `$0.0`.
     * Si los minutos restantes son mayores a 0 e inferiores o iguales a 30: cobro adicional = tarifa de la **Fracción**.
     * Si los minutos restantes son mayores a 30: cobro adicional = tarifa de **Hora Completa**.

*Fórmula computacional resultante implementada:*
$$\text{Monto Final} = (\text{Horas Enteras} \times \text{Tarifa Hora}) + \text{Cobro de Fracción Restante}$$

---

## 7. Instrucciones para Pruebas en Entornos Virtuales (Simulador)

Ya que los emuladores de desarrollo y entornos integrados web no poseen una cámara física que enfoque autos reales circulando, se implementó de forma nativa un **Módulo de Matrices Sintéticas (Simulador)** en la pestaña "Tarifas/Prueba":

1. **Generación Gráfica**: Al presionar cualquier preajuste rápido (ej. `LKM 519`, `Colombia`, `Bogotá D.C.`), la aplicación ejecuta la función `generateSimulatedPlateAndScan`.
2. **Dibujado en Memoria (Canvas)**: Se instancia un objeto de dibujo nativo `Canvas` en un bitmap temporal, pintando un fondo metálico texturizado, un marco exterior oscuro de matrícula estándar, una escarapela de identidad regional a la izquierda, el texto del estado/provincia arriba y la patente elegida de forma nítida en el centro.
3. **Inyección al Motor OCR**: Dicho bitmap renderizado exactamente en memoria se entrega al flujo principal de procesamiento de imágenes (`processImage`) haciéndolo pasar de manera real por los motores de ML Kit. Esto simula a la perfección el éxito y la precisión en un entorno sandbox virtual, procesando todo localmente a velocidad récord.
