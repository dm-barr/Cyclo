package unc.edu.pe.cyclo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import unc.edu.pe.cyclo.databinding.ActivityRegisterBinding;
import unc.edu.pe.cyclo.model.Usuario;
import unc.edu.pe.cyclo.repository.UsuarioRepository;

public class Register extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private UsuarioRepository usuarioRepo;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        usuarioRepo = new UsuarioRepository();

        setupPasswordValidation();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvIniciarSesion.setOnClickListener(v -> finish());
        binding.btnRegistrarme.setOnClickListener(v -> registerUser());

        binding.btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                binding.etPassword.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                binding.etPassword.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye);
            }
            binding.etPassword.setSelection(binding.etPassword.getText().length());
        });
    }

    private void setupPasswordValidation() {
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                boolean tieneOcho = password.length() >= 8;
                boolean tieneMayuscula = false;
                boolean tieneNumero = false;
                for (char c : password.toCharArray()) {
                    if (Character.isUpperCase(c)) tieneMayuscula = true;
                    if (Character.isDigit(c)) tieneNumero = true;
                }
                boolean cumple = tieneOcho && tieneMayuscula && tieneNumero;

                int colorVerde  = 0xFF2E7D32;
                int colorGris   = 0xFF999999;
                int colorIconoGris = 0xFFCCCCCC;

                binding.iconOcho.setImageResource(tieneOcho ? R.drawable.ic_check_circle : R.drawable.ic_circle_outline);
                binding.iconOcho.setColorFilter(tieneOcho ? colorVerde : colorIconoGris);
                binding.tvOcho.setTextColor(tieneOcho ? colorVerde : colorGris);

                binding.iconMayuscula.setImageResource(cumple ? R.drawable.ic_check_circle : R.drawable.ic_circle_outline);
                binding.iconMayuscula.setColorFilter(cumple ? colorVerde : colorIconoGris);
                binding.tvMayuscula.setTextColor(cumple ? colorVerde : colorGris);

                binding.btnRegistrarme.setEnabled(cumple);
                binding.btnRegistrarme.setAlpha(cumple ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void registerUser() {
        String nombre = binding.etNombre.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (nombre.isEmpty()) {
            binding.etNombre.setError("Nombre requerido");
            return;
        }
        if (email.isEmpty()) {
            binding.etEmail.setError("Correo requerido");
            return;
        }

        binding.btnRegistrarme.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Usuario nuevoUsuario = new Usuario(user.getUid(), nombre, email);
                            usuarioRepo.guardarUsuario(user.getUid(), nuevoUsuario)
                                    .addOnSuccessListener(aVoid -> {
                                        user.sendEmailVerification();
                                        Toast.makeText(this, "¡Registro exitoso! Verifica tu correo para ingresar.", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(Register.this, Login.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        binding.btnRegistrarme.setEnabled(true);
                                        Toast.makeText(this, "Error al guardar perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        binding.btnRegistrarme.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
