package unc.edu.pe.cyclo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import unc.edu.pe.cyclo.databinding.ActivityMapaBinding;
import unc.edu.pe.cyclo.model.PuntoAcopio;

public class Mapa extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMapaBinding binding;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Lista de puntos para búsqueda y encontrar el más cercano
    private final List<PuntoAcopio> listaPuntos = new ArrayList<>();
    private final List<Marker> listaMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupBottomNavigation();
        setupControls();
        setupBusqueda();
    }

    private void setupBottomNavigation() {
        binding.navMapa.setOnClickListener(v -> { /* Ya estamos aquí */ });

        binding.navEscanear.setOnClickListener(v ->
                startActivity(new Intent(Mapa.this, Escanear.class)));

        binding.navImpacto.setOnClickListener(v ->
                startActivity(new Intent(Mapa.this, Perfil.class)));
    }

    private void setupControls() {
        // FAB QR
        binding.fabQR.setOnClickListener(v ->
                startActivity(new Intent(Mapa.this, Escanear.class)));

        // Botón mi ubicación
        binding.btnMyLocation.setOnClickListener(v -> centrarEnMiUbicacion());

        // Botones zoom
        binding.btnZoomIn.setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn());
        });

        binding.btnZoomOut.setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut());
        });

        // Card recomendado → ir a Escanear
        binding.cardRecomendado.setOnClickListener(v ->
                startActivity(new Intent(Mapa.this, Escanear.class)));

        // Botón cómo llegar → abre Google Maps con el punto más cercano
        binding.btnComoLlegar.setOnClickListener(v -> abrirNavegacion());
    }

    private void setupBusqueda() {
        binding.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPuntos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrarPuntos(String query) {
        if (mMap == null) return;

        // Limpiar todos los markers
        for (Marker marker : listaMarkers) {
            marker.remove();
        }
        listaMarkers.clear();

        // Volver a agregar solo los que coincidan
        for (PuntoAcopio punto : listaPuntos) {
            if (query.isEmpty() ||
                    punto.getNombre().toLowerCase().contains(query.toLowerCase())) {
                agregarMarker(punto);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Click en marker muestra info y actualiza la tarjeta inferior
        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            // Centrar cámara en el marker clickeado
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16));

            // Obtener el ID del punto clickeado que guardamos en el Tag
            String idClickeado = (String) marker.getTag();
            if (idClickeado != null) {
                actualizarCardSeleccionado(idClickeado);
            }
            return true;
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            activarMiUbicacion();
        } else {
            solicitarPermisoUbicacion();
        }

        // Centrar en Cajamarca
        LatLng cajamarca = new LatLng(-7.157, -78.523);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cajamarca, 14));

        cargarPuntosAcopio();
    }

    private void cargarPuntosAcopio() {
        db.collection("puntosAcopio")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaPuntos.clear();
                    listaMarkers.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        PuntoAcopio punto = document.toObject(PuntoAcopio.class);
                        punto.setId(document.getId());
                        listaPuntos.add(punto);
                        agregarMarker(punto);
                    }

                    // Mostrar punto más cercano en la card
                    mostrarPuntoMasCercano();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al cargar puntos: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void agregarMarker(PuntoAcopio punto) {
        if (mMap == null) return;
        LatLng latLng = new LatLng(punto.getLatitud(), punto.getLongitud());
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(punto.getNombre())
                .snippet(punto.getDireccion())
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_GREEN)));
        if (marker != null) {
            marker.setTag(punto.getId()); // Guardamos el ID para usarlo después
            listaMarkers.add(marker);
        }
    }

    private void mostrarPuntoMasCercano() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null || listaPuntos.isEmpty()) return;

                    PuntoAcopio masCercano = null;
                    float menorDistancia = Float.MAX_VALUE;

                    for (PuntoAcopio punto : listaPuntos) {
                        float[] resultado = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                punto.getLatitud(), punto.getLongitud(),
                                resultado);
                        if (resultado[0] < menorDistancia) {
                            menorDistancia = resultado[0];
                            masCercano = punto;
                        }
                    }

                    if (masCercano != null) {
                        // Actualizar card con el punto real más cercano
                        binding.cardRecomendado.setTag(masCercano.getId());
                        // Buscar TextViews dentro del card
                        // (índices según el XML: LinearLayout hijo)
                        int distanciaMetros = (int) menorDistancia;
                        String distanciaTexto = distanciaMetros >= 1000
                                ? String.format("A %.1f km de tu ubicación",
                                distanciaMetros / 1000.0)
                                : "A " + distanciaMetros + "m de tu ubicación";

                        // Actualizar los TextViews del card usando binding
                        // Los IDs están en el XML del card
                        actualizarCardRecomendado(masCercano.getNombre(), distanciaTexto);
                    }
                });
    }

    private void actualizarCardRecomendado(String nombre, String distancia) {
        // Buscar los TextViews dentro del cardRecomendado
        // Según el XML: LinearLayout > LinearLayout > TextView[0]=RECOMENDADO,
        //               TextView[1]=nombre, TextView[2]=distancia
        try {
            android.widget.LinearLayout innerLayout =
                    (android.widget.LinearLayout) binding.cardRecomendado.getChildAt(0);
            if (innerLayout != null) {
                android.widget.TextView tvNombre =
                        (android.widget.TextView) innerLayout.getChildAt(1);
                android.widget.TextView tvDistancia =
                        (android.widget.TextView) innerLayout.getChildAt(2);
                if (tvNombre != null) tvNombre.setText(nombre);
                if (tvDistancia != null) tvDistancia.setText(distancia);
            }
        } catch (Exception e) {
            // Si falla el acceso por índice, no es crítico
        }
    }

    private void abrirNavegacion() {
        if (listaPuntos.isEmpty()) return;

        // Usar el punto del tag del card si existe
        Object tag = binding.cardRecomendado.getTag();
        PuntoAcopio destino = null;

        if (tag != null) {
            String id = (String) tag;
            for (PuntoAcopio p : listaPuntos) {
                if (p.getId().equals(id)) {
                    destino = p;
                    break;
                }
            }
        }

        if (destino == null) destino = listaPuntos.get(0);

        // Abrir Google Maps con navegación
        String uri = "google.navigation:q=" + destino.getLatitud()
                + "," + destino.getLongitud();
        Intent intent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Si no tiene Google Maps, abrir en browser
            String webUri = "https://www.google.com/maps/dir/?api=1&destination="
                    + destino.getLatitud() + "," + destino.getLongitud();
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse(webUri)));
        }
    }

    private void centrarEnMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && mMap != null) {
                            LatLng actual = new LatLng(
                                    location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(actual, 16));
                        }
                    });
        } else {
            solicitarPermisoUbicacion();
        }
    }

    private void activarMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void solicitarPermisoUbicacion() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activarMiUbicacion();
                mostrarPuntoMasCercano();
            } else {
                Toast.makeText(this,
                        "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void actualizarCardSeleccionado(String id) {
        PuntoAcopio seleccionado = null;
        for (PuntoAcopio p : listaPuntos) {
            if (p.getId().equals(id)) {
                seleccionado = p;
                break;
            }
        }

        if (seleccionado != null) {
            // 1. Actualizar el tag para que el botón "Cómo llegar" sepa a dónde ir
            binding.cardRecomendado.setTag(seleccionado.getId());

            // 2. Cambiar el título superior a "PUNTO SELECCIONADO" en color azul para diferenciarlo
            try {
                android.widget.LinearLayout innerLayout = (android.widget.LinearLayout) binding.cardRecomendado.getChildAt(0);
                if (innerLayout != null) {
                    android.widget.TextView tvTitulo = (android.widget.TextView) innerLayout.getChildAt(0);
                    tvTitulo.setText("PUNTO SELECCIONADO");
                    tvTitulo.setTextColor(android.graphics.Color.parseColor("#1976D2")); // Azul
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. Calcular la distancia exacta a este nuevo punto y actualizar los textos
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                PuntoAcopio finalSeleccionado = seleccionado;
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        float[] resultado = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                finalSeleccionado.getLatitud(), finalSeleccionado.getLongitud(),
                                resultado);

                        int distanciaMetros = (int) resultado[0];
                        String distanciaTexto = distanciaMetros >= 1000
                                ? String.format(java.util.Locale.getDefault(), "A %.1f km de tu ubicación", distanciaMetros / 1000.0)
                                : "A " + distanciaMetros + "m de tu ubicación";

                        actualizarCardRecomendado(finalSeleccionado.getNombre(), distanciaTexto);
                    } else {
                        actualizarCardRecomendado(finalSeleccionado.getNombre(), "Distancia desconocida");
                    }
                });
            } else {
                actualizarCardRecomendado(seleccionado.getNombre(), "Distancia desconocida");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
