package unc.edu.pe.cyclo;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Locale;

import unc.edu.pe.cyclo.adapter.RecompensaAdapter;
import unc.edu.pe.cyclo.databinding.ActivityRecompensasBinding;
import unc.edu.pe.cyclo.viewmodel.RecompensasViewModel;

public class Activity_recompensas extends AppCompatActivity {

    private ActivityRecompensasBinding binding;
    private RecompensasViewModel viewModel;
    private RecompensaAdapter adapter;
    private FirebaseAuth mAuth;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecompensasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(RecompensasViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUid = user.getUid();
            setupRecyclerView();
            setupObservers();

            // Le pedimos al ViewModel que descargue los datos de Firebase
            viewModel.cargarPuntosUsuario(currentUid);
            viewModel.cargarRecompensas();
        } else {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        adapter = new RecompensaAdapter(recompensa -> {
            // Acción al hacer clic en el botón "Canjear"
            viewModel.canjearRecompensa(currentUid, recompensa);
        });
        binding.rvRecompensas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecompensas.setAdapter(adapter);
    }

    private void setupObservers() {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.getDefault());

        viewModel.misPuntos.observe(this, puntos -> {
            binding.tvMisPuntos.setText(nf.format(puntos));
        });

        viewModel.listaRecompensas.observe(this, recompensas -> {
            adapter.setRecompensas(recompensas);
        });

        // ESTO ES LO NUEVO: Escucha cuando llega el recibo y muestra el Ticket Modal
        viewModel.reciboLiveData.observe(this, datos -> {
            String codigoCupón = datos[0];
            String tituloPremio = datos[1];
            mostrarTicketCupón(codigoCupón, tituloPremio);
        });
        viewModel.puntosFaltantesLiveData.observe(this, faltantes -> {
            mostrarDialogoErrorPuntos(faltantes);
        });

        viewModel.errorLiveData.observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    // EL MÉTODO QUE DIBUJA EL TICKET FLOTANTE
    private void mostrarTicketCupón(String codigo, String titulo) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(unc.edu.pe.cyclo.R.layout.dialog_comprobante);

        // Hace que el fondo del diálogo sea transparente para que se vean las esquinas redondeadas
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false); // Obliga al usuario a darle al botón de cerrar

        android.widget.TextView tvCodigo = dialog.findViewById(unc.edu.pe.cyclo.R.id.tvCodigoCupón);
        android.widget.TextView tvTitulo = dialog.findViewById(unc.edu.pe.cyclo.R.id.tvPremioTitulo);
        android.widget.Button btnCerrar = dialog.findViewById(unc.edu.pe.cyclo.R.id.btnCerrarTicket);

        tvCodigo.setText(codigo);
        tvTitulo.setText(titulo);

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    //metodo para mostrar error por falta de puntos
    private void mostrarDialogoErrorPuntos(int puntosFaltantes) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(unc.edu.pe.cyclo.R.layout.dialog_error_puntos);

        // Fondo transparente para que se vean las esquinas redondeadas
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true); // El usuario puede cerrarlo tocando fuera

        android.widget.TextView tvMensaje = dialog.findViewById(unc.edu.pe.cyclo.R.id.tvMensajeError);
        android.widget.Button btnCerrar = dialog.findViewById(unc.edu.pe.cyclo.R.id.btnCerrarError);

        // Mensaje dinámico y personalizado
        tvMensaje.setText("No tienes puntos suficientes. Te faltan " + puntosFaltantes + " puntos para este premio.");

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}