package unc.edu.pe.cyclo;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import unc.edu.pe.cyclo.databinding.ActivityLoginBinding;

public class Login extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private boolean passwordVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnConfirmar.setOnClickListener(v -> loginUser());

        binding.tvRegistrate.setOnClickListener(v ->
                startActivity(new Intent(Login.this, Register.class)));
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
        binding.tvOlvidaste.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                binding.etEmail.setError("Ingresa tu correo");
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Correo de recuperación enviado",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });
    }
    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            binding.etEmail.setError("Correo requerido");
            return;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Contraseña requerida");
            return;
        }

        binding.btnConfirmar.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.btnConfirmar.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;
                        user.reload().addOnCompleteListener(reloadTask -> {
                            if (user.isEmailVerified()) {
                                startActivity(new Intent(Login.this, Mapa.class));
                                finish();
                            } else {
                                user.sendEmailVerification();
                                mAuth.signOut();
                                Toast.makeText(this,
                                        "Verifica tu correo electrónico.\n" +
                                                "Te reenviamos el correo de verificación.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Credenciales incorrectas";
                        Toast.makeText(this, "Error al ingresar: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
