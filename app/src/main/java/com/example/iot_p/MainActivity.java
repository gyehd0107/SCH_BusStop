// 📁 MainActivity.java
package com.example.iot_p;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private Marker busMarker;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();

    // ✅ 서버 API 주소
    private final String API_PEOPLE_URL = "http://192.168.0.230:8001/bus/current_people";
    private final String API_GPS_URL = "http://192.168.0.230:8001/gps/latest";
    private final String API_OFFBOARD_PREDICT_URL = "http://192.168.0.230:8000/predict/offboard";

    // ✅ 텍스트뷰 참조
    private TextView inText, outText, availableText, routeText;

    // ✅ 주요 정류장 좌표 및 이름
    private final LatLng[] stations = {
            new LatLng(36.772679, 126.933898), // 후문
            new LatLng(36.768231, 126.935381), // 향설3관
            new LatLng(36.767884, 126.932567), // 향설1관
            new LatLng(36.768848, 126.931297), // 도서관
            new LatLng(36.768981, 126.928032)  // 인문대
    };

    private final String[] stationNames = {
            "후문", "향설 3관", "향설 1관", "도서관", "인문대"
    };

    private int routeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_main);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 🧾 텍스트뷰 초기화
        inText = findViewById(R.id.businnum);
        outText = findViewById(R.id.busoutnum);
        availableText = findViewById(R.id.busenablenum);
        routeText = findViewById(R.id.routeText);

        routeText.setText(stationNames[routeIndex] + " ➡ " + stationNames[routeIndex + 1]);

        startPolling();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultPosition = new LatLng(36.772503, 126.934089);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 17f));

        addFixedMarkers();
    }

    private void addFixedMarkers() {
        for (int i = 0; i < stations.length; i++) {
            map.addMarker(new MarkerOptions()
                    .position(stations[i])
                    .title(stationNames[i]));
        }
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateBusMarker();
                updatePeopleInfo();
                updatePredictedOffboard();  // ✅ 하차 예측
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateBusMarker() {
        Request request = new Request.Builder().url(API_GPS_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("GPS", "❌ 위치 요청 실패: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;

                final String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    final double lat = json.getDouble("lat");
                    final double lon = json.getDouble("lon");

                    runOnUiThread(() -> {
                        LatLng newPos = new LatLng(lat, lon);

                        if (busMarker == null) {
                            busMarker = map.addMarker(new MarkerOptions()
                                    .position(newPos)
                                    .title("🚌 실시간 버스")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker55))
                                    .anchor(0.5f, 1.0f));
                        } else {
                            busMarker.setPosition(newPos);
                        }

                        map.animateCamera(CameraUpdateFactory.newLatLng(newPos));

                        if (isNearStation(newPos, stations[(routeIndex + 1) % stations.length])) {
                            routeIndex = (routeIndex + 1) % stations.length;
                            int nextIndex = (routeIndex + 1) % stations.length;
                            routeText.setText(stationNames[routeIndex] + " ➡ " + stationNames[nextIndex]);
                            Log.d("ROUTE", "📍 정류장 도착: " + stationNames[routeIndex]);
                        }
                    });

                } catch (Exception e) {
                    Log.e("GPS", "❗ JSON 파싱 실패: " + e.getMessage());
                }
            }
        });
    }

    private boolean isNearStation(LatLng current, LatLng target) {
        float[] results = new float[1];
        Location.distanceBetween(current.latitude, current.longitude, target.latitude, target.longitude, results);
        return results[0] < 25;
    }

    private void updatePeopleInfo() {
        Request request = new Request.Builder().url(API_PEOPLE_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BUS", "❌ 인원 요청 실패: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;

                final String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    final int in = json.getInt("current_people");
                    final int available = json.getInt("available");

                    runOnUiThread(() -> {
                        inText.setText(in + "명");
                        availableText.setText(available + "명");
                        // ❌ bus_out은 예측값으로 대체되므로 사용 안 함
                    });

                } catch (Exception e) {
                    Log.e("BUS", "❗ 인원 JSON 파싱 실패: " + e.getMessage());
                }
            }
        });
    }

    // ✅ 예측된 하차 인원 호출 및 표시
    private void updatePredictedOffboard() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int weekday = calendar.get(Calendar.DAY_OF_WEEK) - 1;  // 일요일:1 → 0부터 시작
        int stopNumber = routeIndex + 1;  // stop_number는 1~5

        String url = API_OFFBOARD_PREDICT_URL
                + "?stop_number=" + stopNumber
                + "&hour=" + hour
                + "&minute=" + minute
                + "&weekday=" + weekday;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PREDICT", "❌ 하차 예측 요청 실패: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;
                final String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    if (json.has("predicted_offboard")) {
                        final int predicted = json.getInt("predicted_offboard");
                        runOnUiThread(() -> outText.setText(predicted + "명"));
                    } else {
                        Log.e("PREDICT", "⚠️ 예측값 없음: " + body);
                        runOnUiThread(() -> outText.setText("예측 실패"));
                    }
                } catch (Exception e) {
                    Log.e("PREDICT", "❗ 예측 JSON 파싱 실패: " + e.getMessage());
                }
            }
        });

    }
}