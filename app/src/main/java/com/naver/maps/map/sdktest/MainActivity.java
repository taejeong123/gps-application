package com.naver.maps.map.sdktest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private TextView currentAddr, weatherText, temperatureText;
    private TextView sunRiseSet, kphText, speedText, elevationText;
    private ImageView weatherImage;

    private String beforeLocationCode = "";
    private String beforeCoordReverse = "";
    private String beforeDateTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
    private int beforeElevation = 0;

    private int minTimeMs = 5000;

    private boolean nightModeEnabled = false;

    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private NaverMap map;
    private MapFragment mapFragment;

    @Nullable
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentAddr = findViewById(R.id.currentAddr);
        weatherText = findViewById(R.id.weatherText);
        temperatureText = findViewById(R.id.temperatureText);
        kphText = findViewById(R.id.kphText);
        speedText = findViewById(R.id.speedText);
        sunRiseSet = findViewById(R.id.sunRiseSet);
        elevationText = findViewById(R.id.elevationText);
        weatherImage = findViewById(R.id.weatherImage);

        FragmentManager fm = getSupportFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }

        mapFragment.getMapAsync(naverMap -> {
            map = naverMap;
            UiSettings uiSettings = naverMap.getUiSettings();
            uiSettings.setScaleBarEnabled(false);
            uiSettings.setZoomControlEnabled(false);
            uiSettings.setLogoGravity(Gravity.TOP | Gravity.RIGHT);
            uiSettings.setLogoMargin(0, 70, 70, 0);
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (PermissionChecker.checkSelfPermission(this, PERMISSIONS[0]) == PermissionChecker.PERMISSION_GRANTED
                && PermissionChecker.checkSelfPermission(this, PERMISSIONS[1]) == PermissionChecker.PERMISSION_GRANTED
                && PermissionChecker.checkSelfPermission(this, PERMISSIONS[2]) == PermissionChecker.PERMISSION_GRANTED
                && locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, 10, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, 10, this);
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (PermissionChecker.checkSelfPermission(this, PERMISSIONS[0]) == PermissionChecker.PERMISSION_GRANTED
            && PermissionChecker.checkSelfPermission(this, PERMISSIONS[1]) == PermissionChecker.PERMISSION_GRANTED
            && PermissionChecker.checkSelfPermission(this, PERMISSIONS[2]) == PermissionChecker.PERMISSION_GRANTED
            && locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, 10, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, 10, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (map == null) { return; }

        LatLng coord = new LatLng(location);
        LocationOverlay locationOverlay = map.getLocationOverlay();
        locationOverlay.setVisible(true);
        locationOverlay.setPosition(coord);
        locationOverlay.setBearing(location.getBearing());

        String timeStr = "", weather = "", temperature = "", address = "", speed = "", slope = "", elevation = "", imageUrl = "";
        int kph = 0;

        try {
            // get current address from naver api
            GetData.GetAddressFromNaver getAddressFromNaver = new GetData.GetAddressFromNaver(coord.latitude, coord.longitude);
            String addrStr = getAddressFromNaver.execute().get();

            String currentLocation = GetData.getJsonToAddr(new JSONObject(addrStr));
            address = currentLocation;

            // get current location code
            String locationCode = GetData.getLocationCode(new JSONObject(addrStr));

            // get weather info
            String weatherInfoStr = "";
            if (!beforeLocationCode.equals(locationCode)) {
                GetData.GetLocationWeather getLocationWeather = new GetData.GetLocationWeather(getBaseContext(), locationCode);
                weatherInfoStr = getLocationWeather.execute().get();

                weather = weatherInfoStr.split("`")[0];
                temperature = weatherInfoStr.split("`")[1];
                imageUrl = weatherInfoStr.split("`")[4];

                // get sun rise and set time
                String sunRiseTimeStr = weatherInfoStr.split("`")[2];
                String sunSetTimeStr = weatherInfoStr.split("`")[3];

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                Date currentDate = sdf.parse(sdf.format(new Date()));
                Date sunRiseDate = sdf.parse(sunRiseTimeStr);
                Date sunSetDate = sdf.parse(sunSetTimeStr);

                int riseCompare = currentDate.compareTo(sunRiseDate);
                int setCompare = currentDate.compareTo(sunSetDate);

                if (riseCompare != setCompare) {
                    // day mode : 주간
                    nightModeEnabled = false;
                    timeStr = "주간";
                } else {
                    // night mode : 야간
                    nightModeEnabled = true;
                    timeStr = "야간";
                }
            }

            // get distance from naver direction 15 api
            String directionResult = getDirection(coord.longitude + "," + coord.latitude);
            if (!directionResult.equals("")) {
                kph = Integer.parseInt(directionResult.split("`")[0]);
                speed = directionResult.split("`")[1];
            }

            // get elevation info from airmap api
            String elevationResult = getElevation(coord.latitude + "," + coord.longitude);
            elevation = elevationResult.split("`")[0] + "m ";
            slope = elevationResult.split("`")[1];

        } catch (Exception e) {
            e.printStackTrace();
        }

        currentAddr.setText(address);
        weatherText.setText(weather);
        temperatureText.setText(temperature);
        sunRiseSet.setText(timeStr);
        if (kph < 150) { kphText.setText(kph + "kph"); }
        speedText.setText(speed);
        elevationText.setText("고도: " + elevation + slope);
        GlideToVectorYou.justLoadImage(MainActivity.this, Uri.parse(imageUrl), weatherImage);

        map.addOnOptionChangeListener(() -> {
            if (nightModeEnabled == map.isNightModeEnabled()) { return; }
            nightModeEnabled = map.isNightModeEnabled();

            map.setBackgroundColor(nightModeEnabled ? NaverMap.DEFAULT_BACKGROUND_COLOR_DARK : NaverMap.DEFAULT_BACKGROUND_COLOR_LIGHT);
            map.setBackgroundResource(nightModeEnabled ? NaverMap.DEFAULT_BACKGROUND_DRWABLE_DARK : NaverMap.DEFAULT_BACKGROUND_DRWABLE_LIGHT);
        });
        map.setNightModeEnabled(nightModeEnabled);
        map.moveCamera(CameraUpdate.scrollTo(coord).animate(CameraAnimation.Easing));

        if (timeStr.equals("") || weather.equals("") || slope.equals("") || address.equals("") || speed.equals("")) {
            return;
        }

        // convert data to json and save json file
        LocationVO locationVO = new LocationVO();
        locationVO.setTime(timeStr);
        locationVO.setWeather(weather);
        locationVO.setSlope(slope);
        locationVO.setCoord(coord);
        locationVO.setAddress(address);
        locationVO.setSpeed(speed);

        JSONObject json = Utils.dataToJson(locationVO);
        Utils.saveJson(json);
    }

    // get distance from naver direction 15 api
    private String getDirection(String coordReverse) throws ExecutionException, InterruptedException, JSONException, ParseException {
        int kph = 0;
        String speedStr = "";

        if (!beforeCoordReverse.equals("")) {
            GetData.GetDirection getDirection = new GetData.GetDirection(beforeCoordReverse, coordReverse);
            String result = getDirection.execute().get();

            String distanceInfo = GetData.getJsonToDistance(new JSONObject(result));
            String currentDateTime = distanceInfo.split(" ")[0];
            String currentDateTimeSplit = currentDateTime.split("T")[1];

            int distance = Integer.parseInt(distanceInfo.split(" ")[1]);
            double km = (double) distance / 1000;

            SimpleDateFormat sdfHms = new SimpleDateFormat("HH:mm:ss");
            Date beforeTime = sdfHms.parse(beforeDateTime);
            Date currentTime = sdfHms.parse(currentDateTimeSplit);

            long diff = currentTime.getTime() - beforeTime.getTime();
            String moveTimeHour = String.format("%.6f", (double) (diff / 1000) / 3600);

            kph = (int) (km / Double.parseDouble(moveTimeHour));

            if (kph > 60) { speedStr = "고속"; }
            else if (kph <= 60) { speedStr = "저속"; }

            beforeDateTime = currentDateTimeSplit;
        }

        beforeCoordReverse = coordReverse;
        if (kph == 0 || speedStr.equals("")) {
            return "";
        }
        return kph + "`" + speedStr;
    }

    // get elevation info from airmap api
    private String getElevation(String coord) throws ExecutionException, InterruptedException {
        GetData.GetElevationFromAPI getElevationFromAPI = new GetData.GetElevationFromAPI(coord);
        int currentElevation = Integer.parseInt(getElevationFromAPI.execute().get());

        String elevationStatus = "";
        if (currentElevation > beforeElevation + 1) { elevationStatus = "오르막길"; }
        else if (currentElevation < beforeElevation - 1) { elevationStatus = "내리막길"; }
        else { elevationStatus = "평지"; }

        beforeElevation = currentElevation;
        return currentElevation + "`" + elevationStatus;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }
}