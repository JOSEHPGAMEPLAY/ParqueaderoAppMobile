# Checklist de publicación en Google Play

Estado del artefacto: **v1.1.0 / versionCode 2** (release de GitHub `v1.1.0`, commit `0e4ac56`).

Checklist operativo para subir **Escáner de Placas** a Google Play Console. Cada punto indica qué hacer y, cuando aplica, el dato ya verificado en este repositorio.

---

## 0. Datos del artefacto (verificados)

| Dato | Valor |
| --- | --- |
| `applicationId` | `com.aistudio.platescanner.jvrqpl` |
| `versionName` | `1.1.0` |
| `versionCode` | `2` |
| `minSdk` | 24 (Android 7.0) |
| `targetSdk` | 36 (Android 16) |
| Permisos | `INTERNET`, `CAMERA` (cámara `required=false`) |
| Firma del APK publicado | esquema **v2** (sin v1/v3) |
| Certificado de carga (SHA-256) | `3A:2E:83:6B:7D:C2:23:46:C4:35:51:81:DE:D6:13:D3:1F:33:BA:BD:B6:02:18:8D:4B:72:4F:96:5E:4E:C9:40` |
| Certificado de carga (SHA-1) | `04:D8:7D:3D:78:AC:D6:16:0E:DE:78:44:EC:52:B2:15:16:A8:E2:FE` |
| Certificado (DN) | `CN=Jose Vera, OU=Development, O=Personal, L=Pamplona, ST=Norte de Santander, C=CO` |
| Tamaño APK release | 14,91 MB |
| Alineación 16 KB (`zipalign -c -P 16 -v 4`) | **OK** (`Verification successful`) |
| Motor de cobro | servidor (`AppConfig`), la app solo replica la fórmula |

---

## 1. Bloqueantes que hay que resolver antes de subir

### 1.1 Play no acepta APK: hace falta un AAB

Google Play solo admite **Android App Bundle** para apps nuevas. El `.apk` del release de GitHub sirve para instalar a mano, no para la tienda.

- [x] CI publica el AAB como artefacto `app-release-aab` (job `release`, `:app:bundleRelease`).
- [ ] Descargar el AAB: GitHub → Actions → run de `main` → *Artifacts* → `app-release-aab`.

### 1.2 El revisor no puede entrar con la URL por defecto

La app arranca apuntando a `http://10.0.2.2:3000/` (backend local del emulador). Un revisor de Google nunca podrá iniciar sesión ni registrar un parqueo, y el envío se rechaza por "app inaccesible".

Dos caminos, usar al menos uno:

- [ ] **Recomendado:** compilar el AAB apuntando al backend público:
      `./gradlew :app:bundleRelease -PapiBaseUrl=https://TU-BACKEND.vercel.app/`
      (en CI: definir la variable de repositorio `PRODUCTION_API_URL`; el workflow ya la inyecta).
- [ ] Alternativa: en *App access* describir los pasos manuales y dar credenciales de operador
      (abrir la app → ajustes → *Base URL de la API Web* → pegar la URL HTTPS).

Requisitos del backend para la revisión:

- [ ] HTTPS válido y `GET /api/config` respondiendo sin 5xx.
- [ ] Usuario de prueba con rol `operator` (o `admin`) y contraseña vigente.
- [ ] Datos de demo cargados: al menos una tarifa y un parqueo cerrado, para que el revisor vea algo además de pantallas vacías.

### 1.3 Clave de firma

- [ ] Confirmar si la app ya existe en Play con otra clave. Si **no** existe, la primera subida inscribe el *upload key* (SHA-256 de la tabla) en **Play App Signing** y desde ahí Google firma las descargas.
- [ ] Guardar respaldo del keystore (`apks/my-upload-key/`) fuera del repo y de la máquina. Sin él, no se puede publicar actualizaciones.
- [ ] Opcional pero recomendable: activar esquema v3 en `signingConfig` (`enableV3Signing = true`) para soportar rotación de clave. Hoy el APK sale solo con v2.

---

## 2. Cuenta y creación de la app

- [ ] Cuenta de desarrollador de Google Play activa (pago único de USD 25).
- [ ] Verificación de identidad aprobada (para persona natural: documento; para organización: D-U-N-S).
- [ ] Perfil de pagos configurado (aunque la app sea gratuita).
- [ ] *Create app*: nombre **Escáner de Placas**, idioma predeterminado **español (Latinoamérica)**, tipo **App**, **Gratis**.
- [ ] Aceptar la *Developer Program Policies* y la *US export law*.

> Si la cuenta es **personal y fue creada después del 13 de noviembre de 2023**, Google exige una prueba cerrada con **12 testers durante 14 días seguidos** antes de habilitar producción. Planear ese mes de espera.

---

## 3. Ficha de tienda (*Store listing*)

- [ ] Nombre de la app: `Escáner de Placas` (máx. 30 caracteres).
- [ ] Descripción corta (máx. 80): propuesta de valor, p. ej.
      `Control de parqueadero con escaneo de placas, tarifas configurables y modo sin conexión.`
- [ ] Descripción completa (máx. 4000): qué resuelve, escaneo de placas, tarifas por ticket/sin ticket, sincronización, modo colaborativo entre equipos.
- [ ] Ícono 512×512 PNG de 32 bits (sin transparencia).
- [ ] Gráfico destacado 1024×500 PNG/JPG.
- [ ] Mínimo 2 capturas de teléfono (recomendado 4–6): escaneo, registro activo, tarifas, sincronización.
- [ ] Clasificación de contenido y etiquetas: *Business* / *Productivity*, etiqueta "Utilidades".
- [ ] Correo de contacto de soporte y (opcional) sitio web.

---

## 4. Formularios obligatorios de la consola

### 4.1 Seguridad de los datos (*Data safety*)

Declarar según lo que la app realmente hace (los datos van al backend propio, no a terceros):

- [ ] Se recopila **información personal**: nombre de usuario y contraseña (autenticación de operadores).
- [ ] Se recopila **contenido generado por el usuario**: placas de vehículos y comentarios de los registros.
- [ ] Los datos se transmiten **cifrados en tránsito** (HTTPS en producción; el APK solo permite HTTP a hosts locales de desarrollo).
- [ ] No se comparte con terceros; no hay SDK de publicidad ni analítica de terceros.
- [ ] Se ofrece **eliminación de datos**: indicar el canal (correo de soporte) para solicitar borrado.
- [ ] La app **no** recopila ubicación, contactos, SMS ni archivos multimedia (la cámara se usa en vivo, sin guardar fotos).

### 4.2 Política de privacidad

- [ ] Publicar una política de privacidad accesible por URL pública (obligatoria para todas las apps). Debe mencionar: datos de cuenta, placas y comentarios, finalidad, retención, canal de borrado y contacto.

### 4.3 Contenido y clasificación

- [ ] Cuestionario IARC de clasificación de contenido (app de productividad, sin contenido sensible → normalmente "Para todos").
- [ ] Declaración de **anuncios**: no contiene anuncios.
- [ ] Declaración de **app dirigida a menores**: no.
- [ ] Declaración de **app de noticias**: no.
- [ ] Declaración de **app gubernamental**: no.
- [ ] Declaración de **acceso a la app** (`App access`): "todas las funciones requieren iniciar sesión" + credenciales e instrucciones del punto 1.2.
- [ ] Permisos sensibles: solo `CAMERA`; no aplica el formulario de permisos de SMS/registro de llamadas.

---

## 5. Subida y pruebas

- [ ] Crear la release en **Prueba interna** y subir `app-release.aab`.
- [ ] Confirmar que Play acepta el `versionCode 2` (si ya se subió otro artefacto, incrementar a 3 o más).
- [ ] Instalar desde el enlace de prueba interna en al menos dos teléfonos reales (Android 7 y Android 14/15/16).
- [ ] Verificar en dispositivo real:
  - [ ] permiso de cámara solicitado y escaneo de placa correcto;
  - [ ] login contra el backend HTTPS de producción;
  - [ ] registro de entrada y salida con el cobro que calcula el servidor;
  - [ ] modo offline y sincronización posterior;
  - [ ] modo colaborativo solo si el administrador lo habilitó.
- [ ] Revisar *Pre-launch report* (crashes, ANR, accesibilidad) y corregir antes de seguir.

---

## 6. Producción

- [ ] Resolver los bloqueantes de la sección 1.
- [ ] Promover la release de prueba cerrada a **Producción** (o subir el AAB final a producción).
- [ ] Configurar *staged rollout* al 20 % durante 2–3 días; luego 100 %.
- [ ] Definir país/regiones de distribución (Colombia y los países donde operen los parqueaderos).
- [ ] Enviar a revisión. Tiempo típico: de horas a 7 días la primera vez.

---

## 7. Después de publicar

- [ ] Etiquetar el commit exacto que se publicó (`v1.1.0` o el que corresponda) en `ParqueaderoAppMobile`.
- [ ] Documentar la URL de producción usada y la variable `PRODUCTION_API_URL` en el repositorio.
- [ ] Monitorear *Android vitals* (crashes y ANR) durante la primera semana.
- [ ] Para cada nueva versión: subir `versionCode`, compilar `bundleRelease`, repetir secciones 5 y 6.

---

## 8. Resumen de riesgos conocidos

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Subir el `.apk` en vez del `.aab` | Rechazo inmediato de la consola | Usar el artefacto `app-release-aab` |
| URL base por defecto `10.0.2.2` | El revisor no puede iniciar sesión | `-PapiBaseUrl=https://…` o instrucciones en *App access* |
| Backend de producción caído o sin usuarios demo | Rechazo por app inaccesible | Probar `GET /api/config` y un login real antes de enviar |
| Pérdida del keystore | Imposible publicar actualizaciones | Respaldo fuera del repo |
| Cuenta personal nueva | 14 días de prueba cerrada con 12 testers | Planear el cronograma |
| Firma solo v2 | Sin rotación de clave | Activar v3 en `signingConfig` |
