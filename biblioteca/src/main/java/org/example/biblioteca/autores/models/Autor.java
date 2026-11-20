package org.example.biblioteca.autores.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.biblioteca.libros.models.Libro;

import java.util.ArrayList;
import java.util.List;

/** SOLUCION · TAREA 1.e: el lado inverso. */
@Entity
@Table(name = "autores")
@Getter
@Setter
@NoArgsConstructor
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String nacionalidad;

    // 1.e · lado INVERSO: no crea columna; la clave ajena la lleva Libro.autor
    @OneToMany(mappedBy = "autor")
    private List<Libro> libros = new ArrayList<>();
}
