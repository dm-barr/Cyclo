package unc.edu.pe.cyclo.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import unc.edu.pe.cyclo.model.Usuario;

public class UsuarioRepository {
    private final FirebaseFirestore db;
    private static final String COLECCION = "usuarios";

    public UsuarioRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    public Task<Void> guardarUsuario(String uid, Usuario usuario) {
        return db.collection(COLECCION).document(uid).set(usuario);
    }
    public Task<DocumentSnapshot> obtenerUsuario(String uid) {
        return db.collection(COLECCION).document(uid).get();
    }
    public Task<Void> actualizarNivel(String uid, int nuevoNivel) {
        return db.collection(COLECCION).document(uid)
                .update("nivel", nuevoNivel);
    }
    public Task<Void> incrementarEstadisticas(String uid, int puntos, double co2) {
        return db.collection(COLECCION).document(uid)
                .update(
                        "puntos", FieldValue.increment(puntos),
                        "entregas", FieldValue.increment(1),
                        "co2Reducido", FieldValue.increment(co2)
                );
    }
}
