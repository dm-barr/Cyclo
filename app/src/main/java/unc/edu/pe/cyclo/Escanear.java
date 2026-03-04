    package unc.edu.pe.cyclo;

    import android.Manifest;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.media.Image;
    import android.os.Bundle;
    import android.view.View;
    import android.view.animation.Animation;
    import android.view.animation.TranslateAnimation;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.OptIn;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.camera.core.Camera;
    import androidx.camera.core.CameraControl;
    import androidx.camera.core.CameraSelector;
    import androidx.camera.core.ExperimentalGetImage;
    import androidx.camera.core.ImageAnalysis;
    import androidx.camera.core.ImageProxy;
    import androidx.camera.core.Preview;
    import androidx.camera.lifecycle.ProcessCameraProvider;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;

    import com.google.common.util.concurrent.ListenableFuture;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.mlkit.vision.barcode.BarcodeScanner;
    import com.google.mlkit.vision.barcode.BarcodeScanning;
    import com.google.mlkit.vision.barcode.common.Barcode;
    import com.google.mlkit.vision.common.InputImage;

    import java.util.concurrent.ExecutionException;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;

    import unc.edu.pe.cyclo.databinding.ActivityEscanearBinding;
    import unc.edu.pe.cyclo.repository.EntregaRepository;

    public class Escanear extends AppCompatActivity {

        private ActivityEscanearBinding binding;
        private ExecutorService cameraExecutor;
        private Camera camera;
        private CameraControl cameraControl;
      
        private boolean qrDetected = false;

        private FirebaseAuth mAuth;
        private EntregaRepository entregaRepository;

        private static final int REQUEST_CODE_PERMISSIONS = 10;
        private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = ActivityEscanearBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            mAuth = FirebaseAuth.getInstance();
            entregaRepository = new EntregaRepository();

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this,
                        "Debes iniciar sesión para escanear",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login.class));
                finish();
                return;
            }

            if (allPermissionsGranted()) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }

            cameraExecutor = Executors.newSingleThreadExecutor();

            setupListeners();
            startScanAnimation();
        }

        private void setupListeners() {
            binding.btnBack.setOnClickListener(v -> finish());

            binding.btnVolverMapa.setOnClickListener(v -> {
                startActivity(new Intent(Escanear.this, Mapa.class));
                finish();
            });
        }

        private void startScanAnimation() {
            TranslateAnimation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_PARENT, -0.4f,
                    Animation.RELATIVE_TO_PARENT, 0.4f
            );
            animation.setDuration(2000);
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
            binding.scanLine.startAnimation(animation);
        }

        private boolean allPermissionsGranted() {
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }

        private void startCamera() {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(
                                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageAnalysis);
                    cameraControl = camera.getCameraControl();

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    Toast.makeText(this,
                            "Error al iniciar cámara", Toast.LENGTH_SHORT).show();
                }
            }, ContextCompat.getMainExecutor(this));
        }

        @OptIn(markerClass = ExperimentalGetImage.class)
        private void analyzeImage(@NonNull ImageProxy imageProxy) {
            if (qrDetected) {
                imageProxy.close();
                return;
            }

            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            BarcodeScanner scanner = BarcodeScanning.getClient();
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !qrDetected) {
                                qrDetected = true;
                                validarQrConFirestore(rawValue);
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        }

        private void validarQrConFirestore(String qrValue) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            entregaRepository.verificarUso24h(user.getUid(), qrValue)
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            // Ya usó este QR hoy
                            runOnUiThread(() -> {
                                qrDetected = false;
                                mostrarDialogoQrUsado();
                            });
                        } else {
                            runOnUiThread(() -> mostrarModalExito(qrValue));
                        }
                    })
                    .addOnFailureListener(e -> {
                        qrDetected = false;
                        runOnUiThread(() ->
                                Toast.makeText(this,
                                        "Error al validar QR: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    });
        }

        private void mostrarModalExito(String qrValue) {
            binding.successModal.setVisibility(View.VISIBLE);
            binding.tvPuntosGanados.setText("¡QR válido! Registrando entrega...");

            // Navegar a Entrega después de 2 segundos
            binding.successModal.postDelayed(() -> {
                Intent intent = new Intent(Escanear.this, Entrega.class);
                intent.putExtra("puntoAcopioId", qrValue);
                startActivity(intent);
                finish();
            }, 2000);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQUEST_CODE_PERMISSIONS) {
                if (allPermissionsGranted()) {
                    startCamera();
                } else {
                    Toast.makeText(this,
                            "Se requiere permiso de cámara para escanear",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (cameraExecutor != null) {
                cameraExecutor.shutdown();
            }
            binding = null;
        }
        private void mostrarDialogoQrUsado() {
            android.app.Dialog dialog = new android.app.Dialog(this);
            dialog.setContentView(unc.edu.pe.cyclo.R.layout.dialog_qr_usado);

            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.setCancelable(false);

            android.widget.Button btnEntendido = dialog.findViewById(unc.edu.pe.cyclo.R.id.btnEntendidoQr);

            btnEntendido.setOnClickListener(v -> {
                dialog.dismiss();
                finish();
            });

            dialog.show();
        }
    }
