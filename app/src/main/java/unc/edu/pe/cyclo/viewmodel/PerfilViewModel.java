package unc.edu.pe.cyclo.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;

import unc.edu.pe.cyclo.model.Usuario;
import unc.edu.pe.cyclo.repository.EntregaRepository;
import unc.edu.pe.cyclo.repository.UsuarioRepository;

public class PerfilViewModel extends AndroidViewModel {

    private final UsuarioRepository usuarioRepo;
    private final EntregaRepository entregaRepo;

    public MutableLiveData<Usuario> usuarioLiveData = new MutableLiveData<>();
    public MutableLiveData<Integer> progresoMensual = new MutableLiveData<>();
    public MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public PerfilViewModel(@NonNull Application application) {
        super(application);
        usuarioRepo = new UsuarioRepository();
        entregaRepo = new EntregaRepository();
    }

    public void cargarUsuario(String uid) {
        usuarioRepo.obtenerUsuario(uid)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Usuario u = doc.toObject(Usuario.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            usuarioLiveData.setValue(u);
                            verificarYActualizarNivel(uid, u);
                        }
                    }
                })
                .addOnFailureListener(e -> errorLiveData.setValue(e.getMessage()));
    }

    private void verificarYActualizarNivel(String uid, Usuario u) {
        int nivelCalculado = u.calcularNivel();
        if (nivelCalculado != u.getNivel()) {
            usuarioRepo.actualizarNivel(uid, nivelCalculado);
        }
    }

    public void cargarProgresoMensual(String uid, long inicioMes) {
        entregaRepo.obtenerEntregasMes(uid, inicioMes)
                .addOnSuccessListener(query -> {
                    double totalKg = 0;
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Double peso = doc.getDouble("peso");
                        if (peso != null) totalKg += peso;
                    }
                    double meta = 25.0;
                    int porcentaje = (int) Math.min((totalKg / meta) * 100, 100);
                    progresoMensual.setValue(porcentaje);
                })
                .addOnFailureListener(e -> progresoMensual.setValue(0));
    }
}
