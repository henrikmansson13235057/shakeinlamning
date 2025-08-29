package com.example.shakeinlamning;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "ShakeApp";
    private static final float NS2S = 1.0f / 1000000000.0f; // nano till sekunder

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, light;

    private Switch switchUseGyro;
    private SeekBar seekThreshold;
    private TextView txtThresholdLabel, txtAccel, txtGyro, txtLight;
    private ImageView imgRotating;
    private ProgressBar progressMagnitude;
    private Button btnReactive;

    private float threshold = 12.0f;
    private long lastShakeTime = 0;

    private float timestamp;
    private float angleX = 0f, angleY = 0f, angleZ = 0f;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hitta views
        switchUseGyro = findViewById(R.id.switchUseGyro);
        seekThreshold = findViewById(R.id.seekThreshold);
        txtThresholdLabel = findViewById(R.id.txtThresholdLabel);
        imgRotating = findViewById(R.id.imgRotating);
        progressMagnitude = findViewById(R.id.progressMagnitude);
        btnReactive = findViewById(R.id.btnReactive);
        txtAccel = findViewById(R.id.txtAccel);
        txtGyro = findViewById(R.id.txtGyro);
        txtLight = findViewById(R.id.txtLight);

        // Threshold
        seekThreshold.setProgress((int) (threshold * 10));
        txtThresholdLabel.setText("Threshold: " + threshold);

        seekThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold = progress / 10f;
                txtThresholdLabel.setText("Threshold: " + threshold);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Init sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (gyroscope != null)
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        if (light != null)
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            txtAccel.setText(String.format("Accel: x=%.2f, y=%.2f, z=%.2f", ax, ay, az));

            double magnitude = Math.sqrt(ax * ax + ay * ay + az * az);
            progressMagnitude.setProgress((int) (magnitude * 10));

            // Shake-detection
            if (!switchUseGyro.isChecked() && magnitude > threshold) {
                long now = System.currentTimeMillis();
                if (now - lastShakeTime > 1000) {
                    lastShakeTime = now;
                    Toast.makeText(this, "SHAKE detected!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "SHAKE: " + magnitude);
                    btnReactive.setBackgroundColor(Color.RED);
                }
            } else {
                btnReactive.setBackgroundColor(Color.GREEN);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            txtGyro.setText(String.format("Gyro: x=%.2f, y=%.2f, z=%.2f",
                    event.values[0], event.values[1], event.values[2]));

            if (switchUseGyro.isChecked()) {
                if (timestamp != 0) {
                    final float dT = (event.timestamp - timestamp) * NS2S;
                    angleX += event.values[0] * dT * 57.2958f;
                    angleY += event.values[1] * dT * 57.2958f;
                    angleZ += event.values[2] * dT * 57.2958f;


                    imgRotating.setRotationX(angleX);
                    imgRotating.setRotationY(angleY);
                    imgRotating.setRotation(angleZ);

                    // håll värdena inom 0–360
                    angleX %= 360;
                    angleY %= 360;
                    angleZ %= 360;
                }
                timestamp = event.timestamp;

                float speed = Math.abs(event.values[0]) +
                        Math.abs(event.values[1]) +
                        Math.abs(event.values[2]);
                if (speed > threshold) {
                    Toast.makeText(this, "Snabb rotation!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "GYRO threshold: " + speed);
                }
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            txtLight.setText("Light: " + event.values[0] + " lx");
            if (event.values[0] < 10) {
                btnReactive.setText("Mörkt");
            } else {
                btnReactive.setText("Ljust");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
