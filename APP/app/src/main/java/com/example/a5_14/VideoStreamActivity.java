package com.example.a5_14;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class VideoStreamActivity extends AppCompatActivity {

    private static final String TAG = "VideoStreamActivity";
    private ImageView imageView;
    private MqttClient client;
    private String broker = "tcp://114.55.89.187:1883"; // 请确保此地址正确
    private String topic = "camera/stream";
    private String username = "MQTT2";
    private String password = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_stream);

        imageView = findViewById(R.id.imageView);

        try {
            client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(TAG, "Message arrived from topic: " + topic);
                    try {
                        String frameBase64 = new String(message.getPayload());
                        byte[] frameBytes = Base64.decode(frameBase64, Base64.DEFAULT);
                        Log.d(TAG, "Frame bytes decoded from Base64, size: " + frameBytes.length);

                        Bitmap frameBitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.length);
                        if (frameBitmap != null) {
                            Log.d(TAG, "Frame successfully decoded to bitmap");
                            runOnUiThread(() -> imageView.setImageBitmap(frameBitmap));
                        } else {
                            Log.e(TAG, "Failed to decode frame to bitmap");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing message", e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect();
            client.subscribe(topic);
            Log.d(TAG, "Connected to MQTT broker and subscribed to topic: " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect to MQTT broker", e);
        }
    }
}
