package org.example.biblioteca.libros.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** SOLUCION · TAREA 4 · 409: no hay unidades suficientes. */
@ResponseStatus(HttpStatus.CONFLICT)
public class LibroSinStockException extends RuntimeException {
    public LibroSinStockException(String titulo, int stock, int cantidad) {
        super("El libro '%s' tiene %d unidades: no se pueden retirar %d"
                .formatted(titulo, stock, Math.abs(cantidad)));
    }
}
