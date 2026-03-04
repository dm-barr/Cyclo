package unc.edu.pe.cyclo.model;

public class Canje {
    private String id;
    private String userId;
    private String recompensaId;
    private String tituloRecompensa;
    private String codigoCanje;
    private long fecha;

    public Canje() {} // Requerido por Firestore

    public Canje(String userId, String recompensaId, String tituloRecompensa, String codigoCanje) {
        this.userId = userId;
        this.recompensaId = recompensaId;
        this.tituloRecompensa = tituloRecompensa;
        this.codigoCanje = codigoCanje;
        this.fecha = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRecompensaId() { return recompensaId; }
    public void setRecompensaId(String recompensaId) { this.recompensaId = recompensaId; }

    public String getTituloRecompensa() { return tituloRecompensa; }
    public void setTituloRecompensa(String tituloRecompensa) { this.tituloRecompensa = tituloRecompensa; }

    public String getCodigoCanje() { return codigoCanje; }
    public void setCodigoCanje(String codigoCanje) { this.codigoCanje = codigoCanje; }

    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }
}