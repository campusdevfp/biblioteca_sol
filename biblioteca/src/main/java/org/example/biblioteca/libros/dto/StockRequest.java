package org.example.biblioteca.libros.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** SOLUCION · TAREA 3.b · el DTO que se crea desde cero. */
public record StockRequest(

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = -50, message = "No se pueden retirar más de 50 unidades de golpe")
        @Max(value = 50, message = "No se pueden añadir más de 50 unidades de golpe")
        Integer cantidad
) {}
