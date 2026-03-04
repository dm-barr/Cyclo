package unc.edu.pe.cyclo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // ✅ Recargar estado antes de verificar email
                currentUser.reload().addOnCompleteListener(task -> {
                    if (currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, Mapa.class));
                    } else {
                        mAuth.signOut();
                        startActivity(new Intent(MainActivity.this, Login.class));
                    }
                    finish();
                });
            } else {
                startActivity(new Intent(MainActivity.this, Login.class));
                finish();
            }
        }, 2000);
    }
}