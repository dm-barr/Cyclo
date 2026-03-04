package unc.edu.pe.cyclo.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import unc.edu.pe.cyclo.model.Canje;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import unc.edu.pe.cyclo.model.Recompensa;

public class RecompensasViewModel extends AndroidViewModel {
    private final FirebaseFirestore db;

    public MutableLiveData<List<Recompensa>> listaRecompensas = new MutableLiveData<>();
    public MutableLiveData<Integer> misPuntos = new MutableLiveData<>();
    public MutableLiveData<String[]> reciboLiveData = new MutableLiveData<>();
    public MutableLiveData<Integer> puntosFaltantesLiveData = new MutableLiveData<>();
    public MutableLiveData<String> errorLiveData = new MutableLiveData<>();


    public RecompensasViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public void cargarPuntosUsuario(String uid) {
        db.collection("usuarios").document(uid).addSnapshotListener((doc, e) -> {
            if (e != null) { return; }
            if (doc != null && doc.exists()) {
                Long puntos = doc.getLong("puntos");
                misPuntos.setValue(puntos != null ? puntos.intValue() : 0);
            }
        });
    }
    public void cargarRecompensas() {
        db.collection("recompensas").get().addOnSuccessListener(query -> {
            List<Recompensa> recompensas = new ArrayList<>();
            for (QueryDocumentSnapshot doc : query) {
                Recompensa r = doc.toObject(Recompensa.class);
                r.setId(doc.getId());
                recompensas.add(r);
            }
            listaRecompensas.setValue(recompensas);
        }).addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }
    private String generarCodigoUnico() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codigo = new StringBuilder("CYC-");
        Random rnd = new Random();
        for (int i = 0; i < 5; i++) {
            codigo.append(caracteres.charAt(rnd.nextInt(caracteres.length())));
        }
        return codigo.toString();
    }

    public void canjearRecompensa(String uid, Recompensa recompensa) {
        Integer puntosActuales = misPuntos.getValue();

        // 1. Validación de saldo suficiente
        if (puntosActuales == null || puntosActuales < recompensa.getCostoPuntos()) {
            int faltantes = recompensa.getCostoPuntos() - (puntosActuales != null ? puntosActuales : 0);
            puntosFaltantesLiveData.setValue(faltantes);
            return;
        }

        // 2. Preparación de la transacción
        String codigoGenerado = generarCodigoUnico();

        // Creamos el objeto utilizando el modelo Canje.java
        Canje nuevoCanje = new Canje(
                uid,
                recompensa.getId(),
                recompensa.getTitulo(),
                codigoGenerado
        );

        // 3. Inicio de operación atómica (WriteBatch)
        WriteBatch batch = db.batch();

        // Referencia para actualizar los puntos del usuario (Resta)
        DocumentReference userRef = db.collection("usuarios").document(uid);
        batch.update(userRef, "puntos", FieldValue.increment(-recompensa.getCostoPuntos()));

        // Referencia para crear el nuevo registro de canje
        DocumentReference canjeRef = db.collection("canjes").document();
        nuevoCanje.setId(canjeRef.getId()); // Sincronizamos el ID del documento con el objeto

        // Guardamos el objeto directamente en Firestore
        batch.set(canjeRef, nuevoCanje);

        // 4. Ejecución del Batch
        batch.commit()
                .addOnSuccessListener(v -> {
                    // Notificamos a la vista que el canje fue exitoso
                    reciboLiveData.setValue(new String[]{codigoGenerado, recompensa.getTitulo()});
                })
                .addOnFailureListener(e -> {
                    errorLiveData.setValue("Error al procesar el canje: " + e.getMessage());
                });
    }
}