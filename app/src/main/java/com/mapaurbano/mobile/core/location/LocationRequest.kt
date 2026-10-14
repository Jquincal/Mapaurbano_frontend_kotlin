package com.mapaurbano.mobile.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mapaurbano.mobile.core.model.MapPoint
import java.util.Locale

sealed interface LocationRequestResult {
    data class Success(val point: MapPoint) : LocationRequestResult
    data object PermissionDenied : LocationRequestResult
    data object ServicesDisabled : LocationRequestResult
    data object Unavailable : LocationRequestResult
    data object Failure : LocationRequestResult
}

data class LocationRequestController(
    val isLoading: Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberLocationRequest(
    onResult: (LocationRequestResult) -> Unit,
): LocationRequestController {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val provider = remember(applicationContext) { AndroidLocationProvider(applicationContext) }
    val latestOnResult by rememberUpdatedState(onResult)
    var isLoading by remember { mutableStateOf(false) }

    val requestCurrentLocation = remember(provider) {
        {
            isLoading = true
            provider.requestCurrentLocation { result ->
                isLoading = false
                latestOnResult(result)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestCurrentLocation()
        } else {
            latestOnResult(LocationRequestResult.PermissionDenied)
        }
    }

    val request = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            requestCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    return LocationRequestController(isLoading = isLoading, request = request)
}

fun LocationRequestResult.errorMessageOrNull(): String? = when (this) {
    is LocationRequestResult.Success -> null
    LocationRequestResult.PermissionDenied ->
        "No se concedió acceso a la ubicación. Podés elegir el punto manualmente."
    LocationRequestResult.ServicesDisabled ->
        "La ubicación del teléfono está desactivada. Activala o elegí el punto manualmente."
    LocationRequestResult.Unavailable ->
        "No pudimos obtener una ubicación actual. Intentá nuevamente o elegí el punto manualmente."
    LocationRequestResult.Failure ->
        "Ocurrió un problema al consultar la ubicación. El mapa sigue disponible."
}

private class AndroidLocationProvider(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(onResult: (LocationRequestResult) -> Unit) {
        if (!appContext.hasLocationPermission()) {
            onResult(LocationRequestResult.PermissionDenied)
            return
        }
        if (!appContext.isLocationEnabled()) {
            onResult(LocationRequestResult.ServicesDisabled)
            return
        }

        val cancellationToken = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    onResult(LocationRequestResult.Unavailable)
                } else {
                    val label = String.format(
                        Locale.US,
                        "%.5f, %.5f",
                        location.latitude,
                        location.longitude,
                    )
                    onResult(
                        LocationRequestResult.Success(
                            MapPoint(
                                x = 0.5f,
                                y = 0.5f,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                label = label,
                            ),
                        ),
                    )
                }
            }
            .addOnFailureListener { onResult(LocationRequestResult.Failure) }
    }

    private fun Context.hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun Context.isLocationEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }
}
