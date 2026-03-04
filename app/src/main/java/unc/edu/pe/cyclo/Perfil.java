package unc.edu.pe.cyclo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import unc.edu.pe.cyclo.databinding.ActivityPerfilBinding;
import unc.edu.pe.cyclo.model.Usuario;
import unc.edu.pe.cyclo.viewmodel.PerfilViewModel;

public class Perfil extends AppCompatActivity {

    private ActivityPerfilBinding binding;
    private PerfilViewModel viewModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

        setupObservers();
        setupListeners();
        setupBottomNavigation();
    }

    private void setupObservers() {
        // Observa cambios en los datos del usuario
        viewModel.usuarioLiveData.observe(this, this::mostrarDatos);

        // Observa el progreso mensual
        viewModel.progresoMensual.observe(this, porcentaje -> {
            binding.tvPorcentaje.setText(porcentaje + "%");
            binding.progressMeta.setProgress(porcentaje);
        });

        // Observa errores
        viewModel.errorLiveData.observe(this, error ->
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show());
    }

    private void mostrarDatos(Usuario usuario) {
        if (usuario == null) return;

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());

        binding.tvNombre.setText(usuario.getNombre());
        binding.tvNivel.setText("Reciclador Nivel " + usuario.calcularNivel());
        binding.tvPuntos.setText(numberFormat.format(usuario.getPuntos()));
        binding.tvEntregas.setText(String.valueOf(usuario.getEntregas()));
        binding.tvCO2.setText(String.format(Locale.getDefault(),
                "%.1f kg", usuario.getCo2Reducido()));

        // Cargar foto si existe
        String fotoUrl = usuario.getFotoUrl();
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(fotoUrl)
                    .placeholder(R.drawable.img_profille)
                    .error(R.drawable.img_profille)
                    .into(binding.imgPerfil);
        }
    }

    private void setupListeners() {
        binding.btnSettings.setOnClickListener(v -> mostrarMenuConfiguracion());

        binding.btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show());
        // AQUÍ PEGAS EL NUEVO BOTÓN
        binding.btnIrTienda.setOnClickListener(v -> {
            startActivity(new Intent(Perfil.this, Activity_recompensas.class));
        });
    }

    private void mostrarMenuConfiguracion() {
        String[] opciones = {"Editar perfil", "Cerrar sesión"};

        new AlertDialog.Builder(this)
                .setTitle("Configuración")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, "Editar perfil", Toast.LENGTH_SHORT).show();
                    } else {
                        mAuth.signOut();
                        Intent intent = new Intent(Perfil.this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void setupBottomNavigation() {
        binding.navMapa.setOnClickListener(v -> {
            startActivity(new Intent(Perfil.this, Mapa.class));
            finish();
        });

        binding.navEscanear.setOnClickListener(v -> {
            startActivity(new Intent(Perfil.this, Escanear.class));
            finish();
        });

        binding.navImpacto.setOnClickListener(v -> {
            // Ya estamos en Perfil
        });
    }

    private void cargarDatos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String uid = user.getUid();
        long inicioMes = getInicioMesActual();

        // ViewModel se encarga de llamar a Firestore
        viewModel.cargarUsuario(uid);
        viewModel.cargarProgresoMensual(uid, inicioMes);
    }

    private long getInicioMesActual() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatos(); // Recarga cada vez que vuelves a esta pantalla
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
