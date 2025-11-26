# Parcial 2 — Aplicaciones Móviles
## Proyecto: Mis Gastos Diarios
### Comisión: ACN4BV
### Integrantes: Nicolás Auger — Javier Latorre
### Profesor: Sergio Daniel Medina
### Año: 2025

---

## 1. Introducción

Mis Gastos Diarios es una aplicación mobile desarrollada en Android con Kotlin. Su propósito es permitir al usuario gestionar de forma sencilla sus gastos diarios, definiendo un presupuesto, registrando consumos y visualizando un resumen informativo tanto a nivel diario como mensual.

La aplicación implementa múltiples pantallas, navegación entre actividades, Firebase Authentication, descarga de imagen desde URL y uso de layout, recursos organizados y un comportamiento dinámico que cumple con los requisitos establecidos para el Parcial 2 de la materia.

---

## 2. Pantallas de la Aplicación

### 2.1. LoginActivity

**Función principal:**
Permite al usuario iniciar sesión con una cuenta existente o crear una nueva cuenta para acceder a la aplicación, utilizando Firebase Authentication.

**Elementos:**
*   Campo de correo electrónico (`EditText`).
*   Campo de contraseña (`EditText`).
*   Botón **"Ingresar"**: Para iniciar sesión con credenciales existentes.
*   Botón **"Registrarse"**: Para crear una nueva cuenta con el correo y contraseña ingresados.

**Flujo general:**
*   **Inicio de sesión exitoso:** Si las credenciales son válidas, el usuario es redirigido a `MainActivity`.
*   **Registro exitoso:** Si se crea una nueva cuenta, el usuario inicia sesión automáticamente y es redirigido a `MainActivity`.
*   **Error:** Si las credenciales son incorrectas, la contraseña es demasiado débil o el correo ya existe, se muestra un mensaje descriptivo al usuario mediante un `Toast`.

---

### 2.2. MainActivity (Pantalla Principal)

Es la pantalla central y más completa del sistema. Sus funcionalidades principales incluyen:

1.  **Descarga de imagen de encabezado (Glide):**
    *   Al iniciar la pantalla, se descarga y muestra una imagen de banner desde una URL remota utilizando la librería Glide.
    *   Esto demuestra la capacidad de la aplicación para consumir recursos desde internet.

2.  **Presupuesto diario:**
    *   El usuario define un presupuesto inicial que habilita los campos para la carga de gastos.
    *   El valor se almacena localmente mediante SharedPreferences para persistencia.

3.  **Registro de gastos:**
    *   Un formulario permite al usuario registrar un gasto ingresando monto, categoría (mediante un `Spinner`) y una nota opcional.
    *   Al agregar un gasto, se actualizan en tiempo real la lista, los resúmenes y el gráfico.

4.  **Lista dinámica de gastos (RecyclerView):**
    *   Muestra los gastos registrados de forma eficiente. Cada ítem presenta un ícono de categoría, el monto, la nota y un botón para su eliminación directa.
    *   Los ítems son interactivos y permiten la navegación a una pantalla de detalle.

5.  **Gráfico por categoría:**
    *   Se construye dinámicamente mediante código. Representa la distribución del gasto por categoría en relación con el presupuesto total ingresado, mostrando barras de progreso y porcentajes.

6.  **Menú flotante (FAB):**
    *   Ofrece un menú contextual con acciones rápidas:
        *   Limpiar todos los gastos del día.
        *   Recalcular totales (función de depuración/sincronización).
        *   Acceder a la pantalla de resumen mensual.
        *   Cerrar la sesión del usuario (Logout).

---

### 2.3. ExpenseDetailActivity

Presenta la información detallada de un gasto seleccionado.

**Datos mostrados:**
*   Categoría
*   Monto
*   Nota (o "Sin nota" cuando está vacía)

**Acciones disponibles:**
*   Editar el gasto mediante un cuadro de diálogo.
*   Eliminar el gasto.
*   Volver sin realizar cambios.

**Resultados:**
Devuelve a `MainActivity` si el gasto fue editado, eliminado o si no hubo modificaciones, para que la pantalla principal actualice su estado.

---

### 2.4. MonthlySummaryActivity

Pantalla destinada al resumen mensual de la actividad.

**Funcionamiento:**
*   La aplicación guarda automáticamente el presupuesto, el total gastado y el saldo al cambiar el día.
*   Los datos se almacenan en `SharedPreferences`.
*   La pantalla muestra una lista con los resúmenes de días anteriores, incluyendo:
    *   Fecha
    *   Presupuesto del día
    *   Total gastado
    *   Saldo resultante
*   El día actual se identifica visualmente como "Activo".

---

## 3. Comportamiento Dinámico Implementado

1.  **Lista de gastos dinámica:** La lista se actualiza en tiempo real al agregar, editar o eliminar gastos, sin necesidad de recargar la pantalla.
2.  **Recalculado automático:** El presupuesto, el total gastado, el saldo restante y los colores de advertencia se actualizan inmediatamente al modificar cualquier dato relevante.
3.  **Gráfico generado por código:** Las barras de progreso del gráfico se crean y actualizan en tiempo de ejecución en función del porcentaje que representa cada categoría sobre el presupuesto total.
4.  **Persistencia diaria automatizada:** La aplicación registra automáticamente los datos consolidados del día anterior al detectar un cambio de fecha, asegurando un historial coherente.
5.  **Navegación entre pantallas:** Se implementa un flujo de navegación lógico y completo: `Login` → `Main` → `Detalle` / `Resumen mensual` → `Logout`.

---

## 4. Firebase Authentication

Se utiliza **Firebase Auth** como backend para la gestión de usuarios, implementando las siguientes funcionalidades:
*   Registro de nuevas cuentas mediante correo electrónico y contraseña.
*   Inicio de sesión de usuarios existentes.
*   Manejo de sesiones persistentes (el usuario no necesita volver a loguearse cada vez que abre la app).
*   Cierre de sesión seguro.

Esta integración cumple con uno de los requisitos opcionales del Parcial 2.

---

## 5. Organización de Recursos y Diseño

El proyecto sigue las mejores prácticas de organización de recursos en Android para garantizar un código limpio y mantenible:
*   **`strings.xml`**: Centraliza todos los textos de la interfaz de usuario.
*   **`colors.xml`**: Define la paleta de colores de la aplicación, incluyendo colores específicos asociados a cada categoría de gasto.
*   **`dimens.xml`**: Contiene valores de márgenes, tamaños de texto y espaciados para mantener una consistencia visual.
*   **`drawable`**: Incluye los íconos vectoriales correspondientes a cada categoría de gasto.
*   **Layouts**: Se emplean `ConstraintLayout` para interfaces complejas y `LinearLayout` para agrupaciones simples, según lo requerido en cada caso.

---

## 6. Conclusión

El proyecto "Mis Gastos Diarios" cumple exitosamente con todos los requerimientos obligatorios y opcionales del Parcial 2, demostrando un manejo sólido de múltiples pantallas, navegación entre actividades, uso de `ConstraintLayout` y `LinearLayout`, comportamiento dinámico, organización de recursos, persistencia de datos con `SharedPreferences` y autenticación de usuarios con un servicio de backend como Firebase.

Las funcionalidades implementadas, como la descarga de imágenes desde URL y la generación de contenido dinámico, no solo satisfacen la consigna, sino que también sientan las bases para un producto escalable y robusto.
