package unc.edu.pe.cyclo.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import unc.edu.pe.cyclo.model.EntregaModel;
import unc.edu.pe.cyclo.repository.EntregaRepository;
import unc.edu.pe.cyclo.repository.UsuarioRepository;

public class EntregaViewModel extends AndroidViewModel {

    private final EntregaRepository entregaRepo;
    private final UsuarioRepository usuarioRepo;
    private final FirebaseFirestore db;

    public MutableLiveData<Boolean> entregaExitosa = new MutableLiveData<>();
    public MutableLiveData<Boolean> qrYaUsado = new MutableLiveData<>();
    public MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public EntregaViewModel(@NonNull Application application) {
        super(application);
        entregaRepo = new EntregaRepository();
        usuarioRepo = new UsuarioRepository();
        db = FirebaseFirestore.getInstance();
    }

    public void verificarYRegistrar(String userId, String puntoAcopioId,
                                    String tipoMaterial, double peso) {
        entregaRepo.verificarUso24h(userId, puntoAcopioId)
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        qrYaUsado.setValue(true); // Ya usó este QR hoy
                    } else {
                        registrarEntrega(userId, puntoAcopioId, tipoMaterial, peso);
                    }
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    private void registrarEntrega(String userId, String puntoAcopioId,
                                  String tipoMaterial, double peso) {
        int puntos = calcularPuntos(peso, tipoMaterial);
        double co2 = calcularCO2(peso, tipoMaterial);

        EntregaModel entrega = new EntregaModel(userId, puntoAcopioId,
                tipoMaterial, peso, puntos, co2);

        WriteBatch batch = db.batch();

        DocumentReference entregaRef = db.collection("entregas").document();
        entrega.setId(entregaRef.getId());
        batch.set(entregaRef, entrega);

        DocumentReference userRef = db.collection("usuarios").document(userId);
        batch.update(userRef,
                "puntos", FieldValue.increment(puntos),
                "entregas", FieldValue.increment(1),
                "co2Reducido", FieldValue.increment(co2)
        );

        batch.commit()
                .addOnSuccessListener(v -> entregaExitosa.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    public int calcularPuntos(double peso, String tipo) {
        int base = (int) (peso * 10);
        switch (tipo) {
            case "plastico": return (int) (base * 1.2);
            case "papel":    return (int) (base * 1.1);
            default:         return base;
        }
    }

    public double calcularCO2(double peso, String tipo) {
        switch (tipo) {
            case "plastico": return peso * 1.5;
            case "papel":    return peso * 0.9;
            default:         return peso * 0.3;
        }
    }
}
