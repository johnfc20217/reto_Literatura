package com.eys.literatura.model;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "autores")
public class Autor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;

    @Column(name = "fecha_de_nacimiento")
    private String fechaNacimiento;

    @Column(name = "fecha_fallecimiento")
    private String fechaFallecimiento;

    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Libro> libros;

    public Autor() {}

    public Autor(DatosAutor datosAutor) {
        this.nombre = datosAutor.nombre();
        this.fechaNacimiento = datosAutor.fechaNacimiento();
        this.fechaFallecimiento = datosAutor.fechaFallecimiento();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFechaDeNacimiento() { return fechaNacimiento; }
    public void setFechaDeNacimiento(String fechaDeNacimiento) { this.fechaNacimiento = fechaDeNacimiento; }

    public String getFechaFallecimiento() { return fechaFallecimiento; }
    public void setFechaFallecimiento(String fechaFallecimiento) { this.fechaFallecimiento = fechaFallecimiento; }

    public List<Libro> getLibros() { return libros; }
    public void setLibros(List<Libro> libros) { this.libros = libros; }
}

