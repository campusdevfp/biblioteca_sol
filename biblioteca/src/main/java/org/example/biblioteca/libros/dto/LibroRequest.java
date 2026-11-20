package org.example.biblioteca.libros.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** SOLUCION · TAREA 3.a · lo que ENTRA en el POST y en el PUT. */
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
