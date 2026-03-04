package unc.edu.pe.cyclo.model;

public class Usuario {
    private String uid;
    private String nombre;
    private String email;
    private int nivel;
    private int puntos;
    private int entregas;
    private double co2Reducido;
    private String ubicacion;
    private long fechaRegistro;
    private String fotoUrl;

    public Usuario() {}

    public Usuario(String uid, String nombre, String email) {
        this.uid = uid;
        this.nombre = nombre;
        this.email = email;
        this.nivel = 1;
        this.puntos = 0;
        this.entregas = 0;
        this.co2Reducido = 0.0;
        this.ubicacion = "Cajamarca, Perú";
        this.fechaRegistro = System.currentTimeMillis();
    }


    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }

    public int getEntregas() { return entregas; }
    public void setEntregas(int entregas) { this.entregas = entregas; }

    public double getCo2Reducido() { return co2Reducido; }
    public void setCo2Reducido(double co2Reducido) { this.co2Reducido = co2Reducido; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public long getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(long fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public int calcularNivel() {
        if (puntos >= 5000) return 5;
        if (puntos >= 2000) return 4;
        if (puntos >= 800)  return 3;
        if (puntos >= 200)  return 2;
        return 1;
    }
}
