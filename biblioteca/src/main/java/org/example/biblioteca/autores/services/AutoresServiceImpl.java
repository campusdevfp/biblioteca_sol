package org.example.biblioteca.autores.services;

import org.example.biblioteca.autores.dto.AutorResponse;
import org.example.biblioteca.autores.exceptions.AutorNotFoundException;
import org.example.biblioteca.autores.mappers.AutorMapper;
import org.example.biblioteca.autores.repositories.AutoresRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SOLUCION · TAREA 1.f en findById. */
@Service
public class AutoresServiceImpl implements AutoresService {

    private final AutoresRepository repositorio;
    private final AutorMapper mapper;

    public AutoresServiceImpl(AutoresRepository repositorio, AutorMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    /** En el listado no se cuentan los libros: numeroDeLibros va a null. */
    @Override
    @Transactional(readOnly = true)
    public Page<AutorResponse> findAll(Pageable pageable) {
        return repositorio.findAll(pageable).map(a -> mapper.toResponse(a, null));
    }

    @Override
    @Transactional(readOnly = true)
    public AutorResponse findById(Long id) {
        var autor = repositorio.findById(id)
                .orElseThrow(() -> new AutorNotFoundException(id));

        // 1.f · el lado inverso: los libros del autor, sin los borrados
        long numeroDeLibros = autor.getLibros().stream()
                .filter(libro -> !libro.isDeleted())
                .count();
        return mapper.toResponse(autor, numeroDeLibros);
    }
}
