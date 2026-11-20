package org.example.biblioteca.libros.repositories;

import org.example.biblioteca.libros.models.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** SOLUCION · TAREA 2 */
public interface LibrosRepository
        extends JpaRepository<Libro, Long>,          // 2.a · CRUD de Libro con id Long
                JpaSpecificationExecutor<Libro> {    // 2.a · y busquedas con Specification

    // 2.b · un libro por su id, solo si NO esta borrado
    Optional<Libro> findByIdAndDeletedFalse(Long id);

    // 2.c · ¿hay ya algun libro con ese ISBN?
    boolean existsByIsbn(String isbn);
}
