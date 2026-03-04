package unc.edu.pe.cyclo.model;

public class Recompensa {
    private String id;
    private String titulo;
    private String descripcion;
    private int costoPuntos;
    private int stock;
    private String imagenUrl;

    public Recompensa() {} // Constructor vacío requerido por Firestore

    public Recompensa(String titulo, String descripcion, int costoPuntos, int stock, String imagenUrl) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.costoPuntos = costoPuntos;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCostoPuntos() { return costoPuntos; }
    public void setCostoPuntos(int costoPuntos) { this.costoPuntos = costoPuntos; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}