package org.example.biblioteca.libros.services;

import org.example.biblioteca.autores.models.Autor;
import org.example.biblioteca.autores.repositories.AutoresRepository;
import org.example.biblioteca.libros.dto.LibroRequest;
import org.example.biblioteca.libros.dto.LibroResponse;
import org.example.biblioteca.libros.exceptions.LibroBadRequestException;
import org.example.biblioteca.libros.exceptions.LibroConflictException;
import org.example.biblioteca.libros.exceptions.LibroNotFoundException;
import org.example.biblioteca.libros.exceptions.LibroSinStockException;
import org.example.biblioteca.libros.mappers.LibroMapper;
import org.example.biblioteca.libros.models.Libro;
import org.example.biblioteca.libros.repositories.LibrosRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/** SOLUCION · TAREA 5 */
@Service
public class LibrosServiceImpl implements LibrosService {

    private final LibrosRepository repositorio;
    private final AutoresRepository autoresRepository;
    private final LibroMapper mapper;

    public LibrosServiceImpl(LibrosRepository repositorio,
                             AutoresRepository autoresRepository,
                             LibroMapper mapper) {
        this.repositorio = repositorio;
        this.autoresRepository = autoresRepository;
        this.mapper = mapper;
    }

    // ─────────────────────────── LECTURAS ───────────────────────────

    /** 5.b · tres filtros opcionales sobre la consulta paginada. */
    @Override
    @Transactional(readOnly = true)
    public Page<LibroResponse> findAll(Optional<String> titulo,
                                       Optional<String> autor,
                                       Optional<BigDecimal> precioMax,
                                       Pageable pageable) {

        // base: solo los NO borrados. Se aplica siempre
        Specification<Libro> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));

        if (titulo.isPresent()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("titulo")),
                            "%" + titulo.get().toLowerCase() + "%"));
        }

        if (autor.isPresent()) {                     // navega la relacion: JOIN automatico
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("autor").get("nombre")),
                            "%" + autor.get().toLowerCase() + "%"));
        }

        if (precioMax.isPresent()) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("precio"), precioMax.get()));
        }

        return repositorio.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LibroResponse findById(Long id) {
        return mapper.toResponse(buscarLibro(id));
    }

    // ─────────────────────────── ESCRITURAS ─────────────────────────

    /** POST · R1 y R2 */
    @Override
    @Transactional
    public LibroResponse save(LibroRequest request) {
        if (repositorio.existsByIsbn(request.isbn())) {                       // R1
            throw new LibroConflictException("Ya existe un libro con el ISBN " + request.isbn());
        }
        Autor autor = buscarAutor(request.autorId());                          // R2

        Libro guardado = repositorio.save(mapper.toEntity(request, autor));
        return mapper.toResponse(guardado);
    }

    /** PUT · R1 solo si CAMBIA el ISBN, y R2 */
    @Override
    @Transactional
    public LibroResponse update(Long id, LibroRequest request) {
        Libro libro = buscarLibro(id);

        boolean cambiaIsbn = !libro.getIsbn().equals(request.isbn());
        if (cambiaIsbn && repositorio.existsByIsbn(request.isbn())) {         // R1
            throw new LibroConflictException("Ya existe un libro con el ISBN " + request.isbn());
        }
        Autor autor = buscarAutor(request.autorId());                          // R2

        mapper.actualizar(libro, request, autor);
        return mapper.toResponse(libro);           // sin save(): dirty checking
    }

    /** DELETE · R3 · borrado LOGICO */
    @Override
    @Transactional
    public void deleteById(Long id) {
        Libro libro = buscarLibro(id);
        libro.setDeleted(true);                    // dirty checking: UPDATE al terminar
    }

    /** PATCH /stock · R4 */
    @Override
    @Transactional
    public LibroResponse ajustarStock(Long id, int cantidad) {
        Libro libro = buscarLibro(id);

        int nuevo = libro.getStock() + cantidad;
        if (nuevo < 0) {
            throw new LibroSinStockException(libro.getTitulo(), libro.getStock(), cantidad);
        }
        libro.setStock(nuevo);
        return mapper.toResponse(libro);
    }

    // ─────────────────────────── AUXILIARES ─────────────────────────

    /** 404 si no existe O si esta borrado. */
    private Libro buscarLibro(Long id) {
        return repositorio.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new LibroNotFoundException(id));
    }

    /** 400 si el autor no existe. */
    private Autor buscarAutor(Long autorId) {
        return autoresRepository.findById(autorId)
                .orElseThrow(() -> new LibroBadRequestException(
                        "El autor con id " + autorId + " no existe"));
    }
}
