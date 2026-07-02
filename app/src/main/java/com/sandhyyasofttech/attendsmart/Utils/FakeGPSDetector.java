package com.sandhyyasofttech.attendsmart.Utils;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

public class FakeGPSDetector {
    private static final String TAG = "FakeGPSDetector";
    private final Context context;

    public FakeGPSDetector(Context context) {
        this.context = context;
    }

    public boolean isMockLocation(Location location) {
        if (location == null) return false;

        // Method 1: Check Android's mock setting (works on all versions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (location.isFromMockProvider()) {
                Log.e(TAG, "Mock location detected via isFromMockProvider()");
                return true;
            }
        }

        // Method 2: Check if mock locations are allowed in developer options
        if (isMockSettingsOn()) {
            // Additional checks if mock locations are allowed
            if (hasMockProvider() || areCoordinatesSuspicious(location)) {
                return true;
            }
        }

        // Method 3: Check for suspicious patterns
        return areCoordinatesSuspicious(location);
    }

    private boolean isMockSettingsOn() {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION) != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private boolean hasMockProvider() {
        try {
            // Check if any mock location providers are registered
            Class<?> locationManagerClass = Class.forName("android.location.LocationManager");
            Method getProvidersMethod = locationManagerClass.getMethod("getProviders", boolean.class);
            // This is a simplified check - in production, you'd need proper implementation
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean areCoordinatesSuspicious(Location location) {
        if (location == null) return false;

        // Check 1: Unrealistic speed (faster than 300 km/h)
        if (location.hasSpeed() && location.getSpeed() > 83.3f) { // 300 km/h in m/s
            Log.w(TAG, "Suspicious speed: " + location.getSpeed() + " m/s");
            return true;
        }

        // Check 2: Unrealistic accuracy (too perfect)
        if (location.hasAccuracy() && location.getAccuracy() < 1.0f) {
            Log.w(TAG, "Suspicious accuracy: " + location.getAccuracy() + "m");
            return true;
        }

        // Check 3: Coordinates that never change (stuck)
        // This would need to be implemented with history tracking

        // Check 4: Provider consistency
        if (location.getProvider() != null) {
            String provider = location.getProvider().toLowerCase();
            if (provider.contains("gps") && location.getAccuracy() > 100) {
                // GPS shouldn't have >100m accuracy typically
                Log.w(TAG, "GPS with poor accuracy: " + location.getAccuracy() + "m");
            }
        }

        return false;
    }

    // Additional method to check if location is realistic based on previous locations
    public boolean isLocationJump(Location newLocation, Location lastLocation) {
        if (lastLocation == null || newLocation == null) return false;

        float distance = lastLocation.distanceTo(newLocation);
        long timeDiff = newLocation.getTime() - lastLocation.getTime();

        if (timeDiff <= 0) return false;

        float speed = distance / (timeDiff / 1000.0f); // m/s

        // If speed > 100 m/s (360 km/h), it's likely a jump
        return speed > 100;
    }
}