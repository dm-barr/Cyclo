package unc.edu.pe.cyclo.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import unc.edu.pe.cyclo.model.EntregaModel;

public class EntregaRepository {

    private final FirebaseFirestore db;
    private static final String COLECCION = "entregas";

    public EntregaRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> registrarEntrega(EntregaModel entrega) {
        String id = db.collection(COLECCION).document().getId();
        entrega.setId(id);
        return db.collection(COLECCION).document(id).set(entrega);
    }

    public Task<QuerySnapshot> verificarUso24h(String userId, String puntoAcopioId) {
        long hace24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        return db.collection(COLECCION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("puntoAcopioId", puntoAcopioId)
                .whereGreaterThanOrEqualTo("fecha", hace24h)
                .get();
    }
    public Task<QuerySnapshot> obtenerEntregasMes(String userId, long inicioMes) {
        return db.collection(COLECCION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("fecha", inicioMes)
                .get();
    }
}
