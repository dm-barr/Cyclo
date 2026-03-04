package unc.edu.pe.cyclo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import unc.edu.pe.cyclo.databinding.ActivityEntregaBinding;
import unc.edu.pe.cyclo.viewmodel.EntregaViewModel;

public class Entrega extends AppCompatActivity {

    private ActivityEntregaBinding binding;
    private EntregaViewModel viewModel;
    private FirebaseAuth mAuth;
    private String puntoAcopioId;
    private String tipoMaterial = "vidrio";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntregaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(EntregaViewModel.class);

        // Recibir datos del QR escaneado
        puntoAcopioId = getIntent().getStringExtra("puntoAcopioId");
        if (puntoAcopioId == null) puntoAcopioId = "default";

        setupObservers();
        setupListeners();
        cargarInfoPunto();
    }

    private void setupObservers() {
        // Entrega registrada con éxito
        viewModel.entregaExitosa.observe(this, exitosa -> {
            if (exitosa) {
                String pesoStr = binding.etPeso.getText().toString().trim();
                double peso = Double.parseDouble(pesoStr);
                int puntos = viewModel.calcularPuntos(peso, tipoMaterial);

                // 1. Armamos el mensaje personalizado
                String mensaje = "¡Gracias por cuidar Cajamarca! Has ganado +" + puntos + " puntos.";

                // 2. Mostramos la ventana moderna
                mostrarDialogoExito(mensaje, puntos);
            }
        });

        // QR ya usado en las últimas 24h
        viewModel.qrYaUsado.observe(this, yaUsado -> {
            if (yaUsado) {
                // ¡Adiós Toast aburrido, hola ventana azul moderna!
                mostrarDialogoQrUsado();
                binding.btnConfirmar.setEnabled(true);
            }
        });

        // Error general
        viewModel.errorLiveData.observe(this, error -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            binding.btnConfirmar.setEnabled(true);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnConfirmar.setOnClickListener(v -> registrarEntrega());

        // Selección de material
        binding.btnVidrio.setOnClickListener(v ->
                seleccionarMaterial("vidrio",
                        binding.btnVidrio, binding.btnPlastico, binding.btnPapel));

        binding.btnPlastico.setOnClickListener(v ->
                seleccionarMaterial("plastico",
                        binding.btnPlastico, binding.btnVidrio, binding.btnPapel));

        binding.btnPapel.setOnClickListener(v ->
                seleccionarMaterial("papel",
                        binding.btnPapel, binding.btnVidrio, binding.btnPlastico));
    }

    private void cargarInfoPunto() {
        // Carga nombre y dirección del punto desde Firestore
        FirebaseFirestore.getInstance()
                .collection("puntosAcopio")
                .document(puntoAcopioId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("nombre");
                        String direccion = doc.getString("direccion");
                        binding.tvNombrePunto.setText(
                                nombre != null ? nombre : "EcoPlaza Cajamarca Central");
                        binding.tvDireccion.setText(
                                direccion != null ? direccion : "Cajamarca, Perú");
                    }
                });
    }

    private void registrarEntrega() {
        String pesoStr = binding.etPeso.getText().toString().trim();

        if (pesoStr.isEmpty()) {
            binding.etPeso.setError("Ingresa el peso");
            return;
        }

        double peso;
        try {
            peso = Double.parseDouble(pesoStr);
        } catch (NumberFormatException e) {
            binding.etPeso.setError("Peso inválido");
            return;
        }

        if (peso <= 0) {
            binding.etPeso.setError("El peso debe ser mayor a 0");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // Deshabilitar botón para evitar doble registro
        binding.btnConfirmar.setEnabled(false);

        // ViewModel verifica las 24h y registra si está ok
        viewModel.verificarYRegistrar(user.getUid(), puntoAcopioId, tipoMaterial, peso);
    }

    private void seleccionarMaterial(String tipo, LinearLayout seleccionado,
                                     LinearLayout otro1, LinearLayout otro2) {
        tipoMaterial = tipo;

        seleccionado.setBackgroundResource(R.drawable.bg_material_selected);
        actualizarColorMaterial(seleccionado, true);

        otro1.setBackgroundResource(R.drawable.bg_material_unselected);
        actualizarColorMaterial(otro1, false);

        otro2.setBackgroundResource(R.drawable.bg_material_unselected);
        actualizarColorMaterial(otro2, false);
    }

    private void actualizarColorMaterial(LinearLayout layout, boolean seleccionado) {
        TextView tv = (TextView) layout.getChildAt(1);
        ImageView iv = (ImageView) layout.getChildAt(0);

        int colorIcono = seleccionado
                ? ContextCompat.getColor(this, R.color.green_primary)
                : ContextCompat.getColor(this, R.color.gray);

        int colorTexto = seleccionado
                ? ContextCompat.getColor(this, R.color.blackentrega)
                : ContextCompat.getColor(this, R.color.gray);

        tv.setTextColor(colorTexto);
        iv.setColorFilter(colorIcono);
    }

    private void mostrarDialogoExito(String mensaje, int puntosGanados) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_entrega_exitosa);

        // Fondo transparente para que se vean las esquinas redondeadas
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false); // Obliga al usuario a tocar el botón verde

        TextView tvMensaje = dialog.findViewById(R.id.tvMensajePuntos);
        Button btnIrImpacto = dialog.findViewById(R.id.btnIrAlImpacto);

        tvMensaje.setText(mensaje);

        // Al hacer clic en el botón de la ventana emergente:
        btnIrImpacto.setOnClickListener(v -> {
            dialog.dismiss(); // 1. Cierra la ventana emergente

            // 2. Te lleva a la pantalla de Perfil llevándose los puntos ganados
            Intent intent = new Intent(Entrega.this, Perfil.class);
            intent.putExtra("puntosGanados", puntosGanados);
            startActivity(intent);

            // 3. Cierra la pantalla de Entrega para que no puedas volver atrás
            finish();
        });

        dialog.show();
    }

    private void mostrarDialogoQrUsado() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_qr_usado);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false); // Obliga a tocar el botón

        Button btnEntendido = dialog.findViewById(R.id.btnEntendidoQr);

        btnEntendido.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Cerramos esta pantalla para que regrese al Mapa y busque otro punto
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}