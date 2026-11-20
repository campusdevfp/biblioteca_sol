# Cómo se resuelve el examen de la biblioteca, paso a paso

Esta es la resolución del examen práctico de RA5 y RA6. Para cada tarea vas a ver **qué pide**, **cómo se hace**, **el código** y **cómo comprobar que funciona** antes de pasar a la siguiente.

"La estrategia que funciona en un examen así"
    1. **Lee el código que te dan antes de escribir nada**, sobre todo `LibroMapper` y `LibrosService`. Te dicen los nombres de los métodos y cómo se usan.
    2. **Hazlo en orden**: 1 → 2 → 3 → 4 → 5 → 6 → 7. Cada tarea se apoya en la anterior.
    3. **Arranca después de cada tarea.** Un error encontrado a los 5 minutos se arregla en 1; encontrado al final, te puede costar el examen.
    4. **Si algo no te sale, deja el `throw` y sigue.** Un proyecto que no compila es un 0.

## Paso 0 · Antes de escribir una línea (5 min)

1. Abre la carpeta `biblioteca` en IntelliJ y activa el *annotation processing* si te lo pide (Lombok).
2. Arranca `BibliotecaApplication`.
3. En Postman o Bruno, importa `biblioteca.postman_collection.json` y lanza las dos peticiones de ejemplo: el `ping` y la lista de autores. Las dos tienen que dar `200`.

**Por qué:** si el entorno falla, que sea ahora y no a la hora de examen.

## Paso 1 · La entidad `Libro` y la relación (tarea 1)

**Qué pide.** Que `Libro` sea una entidad JPA que genere la tabla `libros` tal como dice la tabla del enunciado, y que la relación con `Autor` sea **bidireccional**.

**Cómo se hace: cada fila de la tabla del enunciado es una anotación.**

| El enunciado dice… | Se escribe… |
|---|---|
| tabla `libros` | `@Entity` y `@Table(name = "libros")` |
| clave primaria que genera la base de datos | `@Id` y `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| obligatorio | `nullable = false` |
| único | `unique = true` |
| texto de 200 | `length = 200` |
| 10 dígitos y 2 decimales | `precision = 10, scale = 2` |
| la columna se llama distinto que el campo | `name = "anio_publicacion"` |
| clave ajena `autor_id`, el autor solo se carga cuando se usa | `@ManyToOne(fetch = FetchType.LAZY, optional = false)` y `@JoinColumn(name = "autor_id", nullable = false)` |
| no cambia después de insertar | `updatable = false` |
| se pone a la hora actual en cada modificación | un método con `@PreUpdate` |

```java
@Entity
@Table(name = "libros")
@Getter @Setter @NoArgsConstructor
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "anio_publicacion", nullable = false)
    private Integer anioPublicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)     // lado PROPIETARIO: tiene la FK
    @JoinColumn(name = "autor_id", nullable = false)
    private Autor autor;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate                                               // Hibernate lo llama antes de cada UPDATE
    void alActualizar() { this.updatedAt = LocalDateTime.now(); }
}
```

**1.e · El lado inverso, en `Autor`.** La clave ajena ya está en `libros.autor_id`. El lado inverso **no crea nada nuevo**: con `mappedBy` le dices a JPA «esta relación ya la guarda el campo `autor` de `Libro`».

```java
@OneToMany(mappedBy = "autor")              // sin cascade: borrar un autor no borra sus libros
private List<Libro> libros = new ArrayList<>();
```

**1.f · `numeroDeLibros`, usando el lado inverso.** Se recorre la lista de libros del autor con un stream y se cuentan los que no están borrados:

```java
long numeroDeLibros = autor.getLibros().stream()
        .filter(libro -> !libro.isDeleted())
        .count();
return mapper.toResponse(autor, numeroDeLibros);
```

Funciona porque `findById` es `@Transactional`: la lista `libros` es perezosa y se carga justo al usarla.

**Cómo lo compruebas:**

- Descomenta el bloque de libros de `data.sql` y arranca: **tienen que entrar los 8 sin error**.
- En la consola de H2 (`http://localhost:8080/h2-console`), `SHOW TABLES` muestra solo `AUTORES` y `LIBROS`.
- `GET /api/v1/autores/1` devuelve `numeroDeLibros: 3`.

**Errores típicos:**

| Lo que pasa | Por qué |
|---|---|
| No arranca: `Could not determine recommended JdbcType for Autor` | Has puesto `@Entity` pero te falta el `@ManyToOne` |
| No arranca: `mappedBy reference an unknown target entity property` | En `mappedBy` va el nombre del **campo** de `Libro`: `"autor"` |
| Aparece una tabla `AUTORES_LIBROS` | Te falta el `mappedBy` |
| `data.sql` falla con `Column "IS_DELETED" not found` | Te falta `name = "is_deleted"` |

## Paso 2 · El repositorio (tarea 2)

**Qué pide.** Un repositorio de Spring Data con el CRUD, las búsquedas con `Specification` y dos consultas **derivadas del nombre del método**.

**Cómo se hace:**

- `JpaRepository<Libro, Long>` te da el CRUD (`save`, `findById`, `findAll`…).
- `JpaSpecificationExecutor<Libro>` te da `findAll(Specification, Pageable)`, que usarás en los filtros.
- Las consultas derivadas **no se escriben**: Spring las construye leyendo el nombre.

```java
public interface LibrosRepository
        extends JpaRepository<Libro, Long>, JpaSpecificationExecutor<Libro> {

    // findBy + Id + And + DeletedFalse  →  WHERE id = ? AND is_deleted = false
    Optional<Libro> findByIdAndDeletedFalse(Long id);

    // existsBy + Isbn  →  ¿hay alguna fila con ese isbn?
    boolean existsByIsbn(String isbn);
}
```

**Fíjate:** `existsByIsbn` **no** lleva `DeletedFalse`, porque R1 dice «aunque uno esté borrado».

**Cómo lo compruebas:** arranca. Si un nombre de método está mal escrito, Spring falla **al arrancar** y te dice qué propiedad no encuentra.

## Paso 3 · Los DTO de entrada (tarea 3)

**Qué pide.** Validar `LibroRequest` y crear `StockRequest`.

**Cómo se hace: cada regla de la tabla es una anotación**, y cada una lleva su `message`.

| Regla | Anotación |
|---|---|
| Texto obligatorio y no vacío | `@NotBlank` (con `@NotNull` pasaría `""`) |
| Número o id obligatorio | `@NotNull` |
| Máximo 200 caracteres | `@Size(max = 200)` |
| Cumple un patrón | `@Pattern(regexp = "…")`. En Java, `\d` se escribe `\\d` |
| Mayor que 0 | `@Positive` |
| 0 o más | `@PositiveOrZero` |
| Entre 1450 y 2100, incluidos | `@Min(1450)` y `@Max(2100)` |

```java
public record LibroRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede pasar de 200 caracteres")
        String titulo,

        @NotBlank(message = "El ISBN es obligatorio")
        @Pattern(regexp = "97[89]-\\d{1,5}-\\d{1,7}-\\d{1,7}-\\d",
                 message = "El ISBN debe tener el formato 978-84-233-2518-4")
        String isbn,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio tiene que ser mayor que 0")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "El año de publicación es obligatorio")
        @Min(value = 1450, message = "El año no puede ser anterior a 1450")
        @Max(value = 2100, message = "El año no puede ser posterior a 2100")
        Integer anioPublicacion,

        @NotNull(message = "El autor es obligatorio")
        Long autorId
) {}
```

```java
public record StockRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = -50, message = "No se pueden retirar más de 50 unidades de golpe")
        @Max(value = 50, message = "No se pueden añadir más de 50 unidades de golpe")
        Integer cantidad
) {}
```

!!! warning "Las anotaciones solas no hacen nada"
    Solo se comprueban si el parámetro del controlador lleva **`@Valid`** (paso 6). Si te lo olvidas, no hay ningún error: simplemente no se valida nada.

**Cómo lo compruebas:** en el paso 6, un POST con el título vacío tiene que dar `400` y el campo `titulo` dentro de `campos`.

## Paso 4 · La excepción propia (tarea 4)

**Qué pide.** Una excepción que responda `409` por sí sola, con el mensaje exacto de R4.

**Cómo se hace:**

- **No comprobada:** hereda de `RuntimeException`, así que no hay que declararla con `throws`.
- **El `409`:** `@ResponseStatus(HttpStatus.CONFLICT)`. Spring la traduce sola, sin tocar el `GlobalExceptionHandler`.
- **El mensaje** se monta en el constructor. `Math.abs` quita el signo: si se retiran −5, el mensaje dice 5.

```java
@ResponseStatus(HttpStatus.CONFLICT)
public class LibroSinStockException extends RuntimeException {
    public LibroSinStockException(String titulo, int stock, int cantidad) {
        super("El libro '%s' tiene %d unidades: no se pueden retirar %d"
                .formatted(titulo, stock, Math.abs(cantidad)));
    }
}
```

## Paso 5 · El servicio (tarea 5)

**Qué pide.** Implementar los métodos de `LibrosService` aplicando las reglas R1–R4 en el orden del enunciado.

### 5.1 · Primero, la inyección y dos métodos de apoyo

Las reglas R3 (404) y R2 (400) se repiten en varios métodos. Se escriben **una vez**, en dos métodos privados:

```java
private final LibrosRepository repositorio;          // 5.0 · se añade al constructor
private final AutoresRepository autoresRepository;
private final LibroMapper mapper;

/** R3 · 404 si no existe o está borrado. */
private Libro buscarLibro(Long id) {
    return repositorio.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new LibroNotFoundException(id));
}

/** R2 · 400 si el autor no existe. */
private Autor buscarAutor(Long autorId) {
    return autoresRepository.findById(autorId)
            .orElseThrow(() -> new LibroBadRequestException("El autor con id " + autorId + " no existe"));
}
```

### 5.2 · El listado con filtros: una `Specification`

La idea: una condición **base** que se aplica siempre (no borrados) y **un `if` por cada filtro** que llegue, que se **suma** a lo anterior con `spec.and(...)`.

```java
@Override
@Transactional(readOnly = true)
public Page<LibroResponse> findAll(Optional<String> titulo, Optional<String> autor,
                                   Optional<BigDecimal> precioMax, Pageable pageable) {

    Specification<Libro> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));   // base

    if (titulo.isPresent()) {                                  // contiene, sin mayúsculas
        spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("titulo")), "%" + titulo.get().toLowerCase() + "%"));
    }
    if (autor.isPresent()) {                                   // navega la relación: JOIN solo
        spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("autor").get("nombre")), "%" + autor.get().toLowerCase() + "%"));
    }
    if (precioMax.isPresent()) {                               // menor o igual
        spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("precio"), precioMax.get()));
    }
    return repositorio.findAll(spec, pageable).map(mapper::toResponse);   // filtra y pagina la BD
}
```

!!! danger "El error que más se repite"
    Escribir `spec = (root, query, cb) -> …` en lugar de `spec = spec.and(…)`. Así cada filtro **sustituye** al anterior y, con dos filtros, solo se aplica el último.

### 5.3 · Leer, crear, modificar, borrar y el stock

```java
@Override
@Transactional(readOnly = true)
public LibroResponse findById(Long id) {
    return mapper.toResponse(buscarLibro(id));                            // R3
}

@Override
@Transactional
public LibroResponse save(LibroRequest request) {
    if (repositorio.existsByIsbn(request.isbn())) {                        // R1
        throw new LibroConflictException("Ya existe un libro con el ISBN " + request.isbn());
    }
    Autor autor = buscarAutor(request.autorId());                          // R2
    return mapper.toResponse(repositorio.save(mapper.toEntity(request, autor)));
}

@Override
@Transactional
public LibroResponse update(Long id, LibroRequest request) {
    Libro libro = buscarLibro(id);                                         // R3
    boolean cambiaIsbn = !libro.getIsbn().equals(request.isbn());
    if (cambiaIsbn && repositorio.existsByIsbn(request.isbn())) {          // R1, solo si cambia
        throw new LibroConflictException("Ya existe un libro con el ISBN " + request.isbn());
    }
    mapper.actualizar(libro, request, buscarAutor(request.autorId()));     // R2
    return mapper.toResponse(libro);                                       // sin save()
}

@Override
@Transactional
public void deleteById(Long id) {
    buscarLibro(id).setDeleted(true);                                      // R3 y borrado lógico
}

@Override
@Transactional
public LibroResponse ajustarStock(Long id, int cantidad) {
    Libro libro = buscarLibro(id);                                         // R3
    int nuevo = libro.getStock() + cantidad;
    if (nuevo < 0) {                                                       // R4
        throw new LibroSinStockException(libro.getTitulo(), libro.getStock(), cantidad);
    }
    libro.setStock(nuevo);
    return mapper.toResponse(libro);
}
```

**Tres cosas que hay que entender:**

1. **¿Por qué el PUT comprueba R1 solo si cambia el ISBN?** Si no, un PUT que deja el mismo ISBN chocaría **consigo mismo** y daría `409`.
2. **¿Por qué no hay `save()` en `update`, `deleteById` ni `ajustarStock`?** Dentro de un `@Transactional`, la entidad que has buscado está **gestionada**: los `set…` se guardan solos al terminar el método. Es el *dirty checking*.
3. **¿Por qué `@Transactional` en todo?** Sin transacción, los cambios no se guardan. Y al leer `libro.getAutor().getNombre()`, que es LAZY, salta `LazyInitializationException`. Ojo con el import: `org.springframework.transaction.annotation.Transactional`.

## Paso 6 · El controlador (tarea 6)

**Qué pide.** Los seis endpoints, con sus códigos HTTP. **Sin lógica**: cada método recibe la petición, llama al servicio y devuelve la respuesta.

| Quieres responder | Escribes |
|---|---|
| `200` con datos | `ResponseEntity.ok(…)` |
| `201` con la cabecera `Location` | `ResponseEntity.created(URI.create("/api/v1/libros/" + id)).body(…)` |
| `204` sin cuerpo | `ResponseEntity.noContent().build()` |
| `400`, `404`, `409` | **Nada:** los generan las excepciones y el `GlobalExceptionHandler` |

```java
@GetMapping
public ResponseEntity<Page<LibroResponse>> findAll(
        @RequestParam Optional<String> titulo,
        @RequestParam Optional<String> autor,
        @RequestParam Optional<BigDecimal> precioMax,
        @PageableDefault(size = 10, sort = "titulo") Pageable pageable) {
    return ResponseEntity.ok(servicio.findAll(titulo, autor, precioMax, sanear(pageable)));
}

@GetMapping("/{id}")
public ResponseEntity<LibroResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(servicio.findById(id));
}

@PostMapping
public ResponseEntity<LibroResponse> save(@Valid @RequestBody LibroRequest request) {
    LibroResponse creado = servicio.save(request);
    return ResponseEntity.created(URI.create("/api/v1/libros/" + creado.id())).body(creado);
}

@PutMapping("/{id}")
public ResponseEntity<LibroResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody LibroRequest request) {
    return ResponseEntity.ok(servicio.update(id, request));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    servicio.deleteById(id);
    return ResponseEntity.noContent().build();
}

@PatchMapping("/{id}/stock")
public ResponseEntity<LibroResponse> ajustarStock(@PathVariable Long id,
                                                  @Valid @RequestBody StockRequest request) {
    return ResponseEntity.ok(servicio.ajustarStock(id, request.cantidad()));
}
```

**Cómo lo compruebas, con Postman o Bruno:**

| Petición | Tiene que dar |
|---|---|
| `GET /api/v1/libros` | `200`, 8 libros, el primero *Cinco horas con Mario* (orden por título) |
| `GET /api/v1/libros?autor=delibes&precioMax=13` | `200` y 2 libros |
| `POST` con un libro correcto | `201` y la cabecera `Location` |
| `POST` con el título vacío | `400` y `titulo` dentro de `campos` |
| `PATCH /api/v1/libros/3/stock` con `{"cantidad": -1}` | `409` con el mensaje de R4 |
| `DELETE /api/v1/libros/2` y después `GET /api/v1/libros/2` | `204` y después `404` |

## Paso 7 · Las pruebas (tarea 7)

**Qué pide.** Las 8 peticiones del enunciado en la colección, con sus nombres y sus tests.

1. Crea cada petición **debajo de las dos de ejemplo**, sin carpetas, con el nombre exacto del enunciado.
2. En la pestaña *Tests*, el test del código y, cuando se pide, el del cuerpo:

    ```javascript
    pm.test("Código 201", function () { pm.response.to.have.status(201); });
    pm.test("autor", function () { pm.expect(pm.response.json().autor).to.eql("Miguel Delibes"); });
    pm.test("tamaño", function () { pm.expect(pm.response.json().content.length).to.eql(3); });
    ```

3. **Reinicia la aplicación y lanza la colección entera.** Si la lanzas dos veces seguidas sin reiniciar, salen rojos falsos: el libro 2 ya está borrado y el ISBN nuevo ya existe.
4. Exporta en formato **Postman v2.1**, con el mismo nombre de fichero.

## Antes de entregar: la lista final

- [ ] *Build → Rebuild Project* sin errores.
- [ ] Reinicio y las 8 peticiones salen en verde.
- [ ] No he tocado ningún fichero marcado como HECHO.
- [ ] El ZIP se llama `nombre_alumno_ra5_ra6.zip`, lleva `biblioteca/` y la colección, y **no** lleva `target/`, `.idea/` ni `data/`.
