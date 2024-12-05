package com.eys.literatura.principal;

import com.eys.literatura.model.*;
import com.eys.literatura.repository.AutorRepository;
import com.eys.literatura.repository.LibroRepository;
import com.eys.literatura.service.ConsumoAPI;
import com.eys.literatura.service.ConvierteDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
public class Principal {
    private static final String URL_BASE = "https://gutendex.com/books/";

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private ConvierteDatos conversor;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    private final Scanner teclado;
    private boolean ejecutando;

    public Principal() {
        this.teclado = new Scanner(System.in);
        this.ejecutando = true;
    }

    public void muestraElMenu() {
        while (ejecutando) {
            try {
                mostrarOpciones();
                int opcion = leerOpcion();
                procesarOpcion(opcion);
            } catch (Exception e) {
                System.out.println("Error al procesar la opción: " + e.getMessage());
                teclado.nextLine(); // Limpiar el buffer
            }
        }
    }

    private void mostrarOpciones() {
        System.out.println("\n *** SOFTWARE DE ADMINISTRACION DE LIBROS ***");
        System.out.println("1- Busca libro por título");
        System.out.println("2- Lista libros registrados");
        System.out.println("3- Lista autores registrados");
        System.out.println("4- Lista autores vivos en un determinado año");
        System.out.println("5- Lista libros por idioma");
        System.out.println("0- Salir");
        System.out.println("====================================");
        System.out.print("Seleccione una opción: ");
    }

    private int leerOpcion() {
        int opcion = teclado.nextInt();
        teclado.nextLine(); // Consumir el salto de línea
        return opcion;
    }

    private void procesarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> buscaLibroPorTitulo();
            case 2 -> listaLibrosRegistrados();
            case 3 -> listaAutoresRegistrados();
            case 4 -> listaAutoresVivosPorAnio();
            case 5 -> listaLibrosPorIdioma();
            case 0 -> salir();
            default -> System.out.println("Opción no válida. Por favor, intente nuevamente.");
        }
    }

    private void listaAutoresRegistrados() {
        System.out.println("\n=== AUTORES REGISTRADOS ===");
        List<Autor> autores = autorRepository.findAutoresConLibros();
        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados en la base de datos.");
            return;
        }

        for (Autor autor : autores) {
            String fechaNacimiento = autor.getFechaDeNacimiento() != null ? autor.getFechaDeNacimiento() : "No disponible";
            String fechaFallecimiento = autor.getFechaFallecimiento() != null ? autor.getFechaFallecimiento() : "No disponible";

            System.out.printf("Autor: %s | Fecha de nacimiento: %s | Fecha de fallecimiento: %s%n",
                    autor.getNombre(), fechaNacimiento, fechaFallecimiento);

            System.out.println("Libros:");
            autor.getLibros().forEach(libro -> System.out.printf("- %s (Idioma: %s, Descargas: %s)%n",
                    libro.getTitulo(), libro.getIdioma(), libro.getNumeroDeDescargas()));
            System.out.println("--------------------------------------------------");
        }
    }

    private void buscaLibroPorTitulo() {
        try {
            System.out.println("\n=== BÚSQUEDA DE LIBRO ===");
            System.out.print("Ingrese el título del libro: ");
            String tituloLibro = teclado.nextLine().trim();

            if (tituloLibro.isEmpty()) {
                System.out.println("El título no puede estar vacío.");
                return;
            }

            // Buscar primero en la base de datos local
            Optional<Libro> libroEnBD = libroRepository.findByTituloContainingIgnoreCase(tituloLibro);
            if (libroEnBD.isPresent()) {
                System.out.println("\nLibro encontrado en la base de datos:");
                System.out.println(libroEnBD.get());
                return;
            }

            // Si no está en la BD, buscar en la API
            System.out.println("Buscando en la API externa...");
            String urlBusqueda = URL_BASE + "?search=" + URLEncoder.encode(tituloLibro, StandardCharsets.UTF_8);
            String json = consumoAPI.obtenerDatos(urlBusqueda);

            if (json == null || json.isEmpty()) {
                System.out.println("No se recibió respuesta de la API");
                return;
            }

            Datos datosBusqueda = conversor.obtenerDatos(json, Datos.class);

            if (datosBusqueda.resultados() == null || datosBusqueda.resultados().isEmpty()) {
                System.out.println("No se encontraron resultados para: " + tituloLibro);
                return;
            }

            procesarResultadosBusqueda(datosBusqueda.resultados(), tituloLibro);

        } catch (Exception e) {
            System.out.println("Error durante la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarResultadosBusqueda(List<DatosLibros> resultados, String tituloLibro) {
        boolean encontrado = false;
        for (DatosLibros datosLibro : resultados) {
            if (datosLibro.titulo().toUpperCase().contains(tituloLibro.toUpperCase())) {
                guardaLibroYAutor(datosLibro);
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            System.out.println("No se encontró ningún libro que coincida exactamente con: " + tituloLibro);
        }
    }

    private void guardaLibroYAutor(DatosLibros datosLibro) {
        try {
            if (datosLibro.autor() == null || datosLibro.autor().isEmpty()) {
                System.out.println("El libro no tiene autor registrado.");
                return;
            }

            // Guardar o recuperar el autor
            DatosAutor datosAutor = datosLibro.autor().get(0);
            Autor autor = autorRepository.findByNombre(datosAutor.nombre())
                    .orElseGet(() -> {
                        Autor nuevoAutor = new Autor(datosAutor);
                        System.out.println("Guardando nuevo autor: " + datosAutor.nombre());
                        return autorRepository.save(nuevoAutor);
                    });

            // Verificar si el libro ya existe
            if (libroRepository.findByTituloContainingIgnoreCase(datosLibro.titulo()).isPresent()) {
                System.out.println("El libro ya existe en la base de datos.");
                return;
            }

            // Guardar el nuevo libro
            Libro libro = new Libro(datosLibro, autor);
            Libro libroGuardado = libroRepository.save(libro);
            System.out.println("\nLibro guardado exitosamente:");
            System.out.println(libroGuardado);

        } catch (Exception e) {
            System.out.println("Error al guardar el libro y autor: " + e.getMessage());
        }
    }

    private void listaLibrosRegistrados() {
        System.out.println("\n=== LIBROS REGISTRADOS ===");
        List<Libro> libros = libroRepository.findAll();
        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
            return;
        }
        libros.forEach(System.out::println);
    }

    private void listaAutoresVivosPorAnio() {
        try {
            System.out.println("\n=== AUTORES VIVOS POR AÑO ===");
            System.out.print("Ingrese el año para consultar: ");
            int anio = Integer.parseInt(teclado.nextLine().trim());

            if (anio < 0 || anio > 2024) {
                System.out.println("Por favor, ingrese un año válido.");
                return;
            }

            System.out.println("Autores vivos en " + anio + ":");
            List<Autor> autores = autorRepository.findAll();
            boolean encontrados = false;

            for (Autor autor : autores) {
                if (estaVivoEnAnio(autor, anio)) {
                    System.out.println("- " + autor.getNombre() +
                            " (Nacimiento: " + autor.getFechaDeNacimiento() + ")");
                    encontrados = true;
                }
            }

            if (!encontrados) {
                System.out.println("No se encontraron autores vivos para el año especificado.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Por favor, ingrese un año válido en formato numérico.");
        } catch (Exception e) {
            System.out.println("Error al procesar la consulta: " + e.getMessage());
        }
    }

    private boolean estaVivoEnAnio(Autor autor, int anio) {
        String fechaNacimiento = autor.getFechaDeNacimiento();
        if (fechaNacimiento == null || fechaNacimiento.isEmpty()) {
            return false;
        }
        try {
            int anioNacimiento = Integer.parseInt(fechaNacimiento);
            return anioNacimiento <= anio;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void listaLibrosPorIdioma() {
        System.out.println("\n=== LIBROS POR IDIOMA ===");
        System.out.println("Idiomas disponibles: ES (Español), EN (Inglés), FR (Francés), PT (Portugués)");
        System.out.print("Ingrese el código del idioma: ");

        String idioma = teclado.nextLine().trim().toUpperCase();
        if (!esIdiomaValido(idioma)) {
            System.out.println("Idioma no válido. Use: ES, EN, FR o PT");
            return;
        }

        List<Libro> libros = libroRepository.findAll();
        List<Libro> librosFiltrados = libros.stream()
                .filter(libro -> libro.getIdioma().equalsIgnoreCase(idioma))
                .toList();

        if (librosFiltrados.isEmpty()) {
            System.out.println("No se encontraron libros en " + obtenerNombreIdioma(idioma));
            return;
        }

        System.out.println("\nLibros en " + obtenerNombreIdioma(idioma) + ":");
        librosFiltrados.forEach(libro ->
                System.out.printf("- %s (Autor: %s)%n",
                        libro.getTitulo(),
                        libro.getAutor().getNombre())
        );
    }

    private boolean esIdiomaValido(String idioma) {
        return idioma.matches("^(ES|EN|FR|PT)$");
    }

    private String obtenerNombreIdioma(String codigo) {
        return switch (codigo) {
            case "ES" -> "Español";
            case "EN" -> "Inglés";
            case "FR" -> "Francés";
            case "PT" -> "Portugués";
            default -> codigo;
        };
    }

    private void salir() {
        System.out.println("\n¡Gracias por usar el sistema! Hasta pronto.");
        ejecutando = false;
    }
}