package unc.edu.pe.cyclo.model;

public class EntregaModel {
    private String id;
    private String userId;
    private String puntoAcopioId;
    private String tipoMaterial;
    private double peso;
    private int puntosGanados;
    private double co2Reducido;
    private long fecha;

    public EntregaModel() {} // Requerido por Firestore

    public EntregaModel(String userId, String puntoAcopioId, String tipoMaterial,
                        double peso, int puntosGanados, double co2Reducido) {
        this.userId = userId;
        this.puntoAcopioId = puntoAcopioId;
        this.tipoMaterial = tipoMaterial;
        this.peso = peso;
        this.puntosGanados = puntosGanados;
        this.co2Reducido = co2Reducido;
        this.fecha = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPuntoAcopioId() { return puntoAcopioId; }
    public void setPuntoAcopioId(String puntoAcopioId) { this.puntoAcopioId = puntoAcopioId; }

    public String getTipoMaterial() { return tipoMaterial; }
    public void setTipoMaterial(String tipoMaterial) { this.tipoMaterial = tipoMaterial; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public int getPuntosGanados() { return puntosGanados; }
    public void setPuntosGanados(int puntosGanados) { this.puntosGanados = puntosGanados; }

    public double getCo2Reducido() { return co2Reducido; }
    public void setCo2Reducido(double co2Reducido) { this.co2Reducido = co2Reducido; }

    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }
}
