# Dependencias
Las dos librerías más importantes que utiliza este proyecto son
[JFlex](https://www.jflex.de/) y [CUP](https://www2.cs.tum.edu/projects/cup/)
para el procesamiento del lenguaje. Adicional a estas, se utilizan las siguientes librerías:

- [Vavr](https://vavr.io/): Utilizado para sufrir menos con Java.
- [JUnit](https://junit.org/): Suite de tests excelente.

Cabe mencionar que estoy utilizando el plugin de Maven para CUP creado por
[vbmacher](https://github.com/vbmacher/cup-maven-plugin) por facilidad de compilación.

# Compilación y Testing
Estoy utilizando [Maven](https://maven.apache.org/) como build system y package manager de Java.

## Compilar y Ejecutar con Maven
Para compilar basta con correr:
```bash
mvn compile
```
lo cual automáticamente lee los archivos `*.cup` y `*.flex` y genera las clases
necesarias para el funcionamiento del programa.

Luego, para correr el programa usando Maven, basta con ejecutar:
```bash
mvn exec:java -D exec.mainClass="org.comp.Main"
```

## Packaging
Si se quiere crear y ejecutar un archivo `*.jar`, es necesario copiar las dependencias
con ayuda de Maven:
```bash
mvn dependency:copy-dependencies
```

Luego se crea el `*.jar`. Este comando automáticamente corre los tests:
```bash
mvn package
```

Y finalmente, se puede ejecutar directamente especificando donde se encuentran las dependencias:
```bash
java -cp "target/eli_nosql-1.0-SNAPSHOT.jar:dependency/*" org.comp.Main
```

## Testing
Esto es muy sencillo. Basta con ejecutar:
```bash
mvn test
```

---

# Arquitectura y Funcionamiento de la Base de Datos

Para manejar el almacenamiento en memoria y mantener un control estricto sobre los datos, el núcleo del sistema vive en el paquete `org.comp.db`. Aprovechando al máximo las colecciones inmutables y las estructuras de control funcional de **Vavr** (como `Option`, `Either`, `HashMap` y `HashSet`), la lógica de la base de datos se divide en tres capas principales fuertemente tipadas:

## 1. El Sistema de Tipos (`DbType.java`)
Para evitar los problemas clásicos del tipado dinámico en Java y restringir exactamente qué datos pueden guardarse, se implementó una `sealed interface` llamada `DbType`.

**¿Cómo funciona?**
* **Restricción Estricta:** La interfaz solo permite clases internas específicas mediante la cláusula `permits`: `Integer`, `Float`, `Bool`, `String`, `Array` y `DbObject`. Esto asegura en tiempo de compilación que no se puedan inyectar tipos no soportados.
* **Pattern Matching:** Gracias a las características modernas de Java, operaciones como la serialización a texto en el método `format()` utilizan un `switch` exhaustivo sobre `this`. Esto permite desempaquetar limpiamente cada tipo (ej. `case DbType.Array arr -> ...`) y formatear de forma recursiva colecciones anidadas.
* **Fábricas de Conversión:** Contiene métodos estáticos como `from_primitive` y `from_raw` que envuelven automáticamente objetos de Java (`java.lang.Integer`, `java.lang.String`) en su variante segura de `DbType` usando `Option` en caso de fallos.

## 2. Almacenamiento y Validación (`DbTable.java`)
Esta clase representa una tabla individual. Es el "motor de validación" que garantiza que los datos nunca se corrompan o no coincidan con las expectativas.

**¿Cómo funciona internamente?**
* **Estructura de Datos:** La tabla almacena su nombre (`tableName`), un esquema formal (`HashMap<String, Class<?>> schema`) y sus registros puros (`HashSet<HashMap<String, DbType>> records`).
* **Validación de Doble Contención (`checkRecord`):** Antes de insertar o actualizar un registro, el sistema realiza tres validaciones críticas:
    1.  *Campos Extras:* Verifica que todas las llaves del registro a insertar existan en el esquema (`schema.containsKey(key)`). Si hay llaves de más, rechaza con `RecordError.ExtraFields`.
    2.  *Campos Faltantes:* Opcionalmente (según el flag `checkEquality`), verifica que todas las llaves del esquema estén presentes en el registro. Si falta alguna, lanza `RecordError.MissingField`.
    3.  *Verificación de Tipos Exactos:* Extrae la clase esperada (`classSchema`) y la compara directamente con la clase real del valor provisto (`classRecord`). Si `classSchema != classRecord`, se deniega la operación con `RecordError.BadTyping`.
* **Inmutabilidad Controlada:** Al momento de hacer un `update`, la tabla procesa los registros, los filtra mediante un predicado y hace un `merge` funcional para reemplazar solo los valores actualizados sin mutar los mapas originales.

## 3. El Gestor Central (`Database.java`)
Es la interfaz principal de la aplicación, encargada de orquestar múltiples tablas y proveer una API amigable para consultar y modificar el estado global.

**¿Cómo funciona internamente?**
* **Gestión de Tablas:** Mantiene una lista funcional de Vavr (`List<DbTable> tables`) para retener las tablas registradas. Al agregar una nueva, verifica que las clases del esquema implementen la interfaz `DbType` y comprueba que el nombre de la tabla no exista previamente.
* **Manejo de Errores Seguro:** Utiliza enums fuertemente tipados (`TableError`, `AddError`, `UpdateError`) en conjunción con los monads `Option` y `Either` de Vavr. De esta forma, las lecturas retornan un `Either.left(records)` si tienen éxito o un `Either.right(error)` si algo falla (ej. `TableNotFound`), obligando al llamador a manejar el error sin recurrir a excepciones.
* **Motor de Filtros (`RecordFilterer`):** Para realizar consultas complejas, provee una API fluida ("Fluent API"). Puedes iniciar un filtro y encadenar reglas columna por columna: la clase interna anidada `Field` evalúa los predicados contra los valores extraídos del registro y retorna un nuevo `RecordFilterer` refinado, hasta que finalmente llamas a `collect()` para obtener los resultados.