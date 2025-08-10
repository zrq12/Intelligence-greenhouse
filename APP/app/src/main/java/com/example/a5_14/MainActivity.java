

/*
package com.example.a5_14;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView m_temp;
    private TextView m_humi;
    private TextView m_co2;
    private TextView m_soiltemp;
    private TextView m_soilhum;
    private TextView m_light;
    private TextView m_ph;
    private LineChart lineChart;

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private String host = "tcp://broker.emqx.io";
    private String userName = "test";
    private String passWord = "123456789";
    private String mqtt_id = "1234";
    private String mqtt_sub_topic = "mqtt135";
    private String mqtt_pub_topic = "Control";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public static class EnvironmentalData {
        private String name;
        private String temp;
        private String humi;
        private String soiltemp;
        private String soilhum;
        private String ph;
        private String light;
        private String co2;
        private long timestamp;

        // Getters and Setters...

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public String getHumi() {
            return humi;
        }

        public void setHumi(String humi) {
            this.humi = humi;
        }

        public String getSoiltemp() {
            return soiltemp;
        }

        public void setSoiltemp(String soiltemp) {
            this.soiltemp = soiltemp;
        }

        public String getSoilhum() {
            return soilhum;
        }

        public void setSoilhum(String soilhum) {
            this.soilhum = soilhum;
        }

        public String getPh() {
            return ph;
        }

        public void setPh(String ph) {
            this.ph = ph;
        }

        public String getLight() {
            return light;
        }

        public void setLight(String light) {
            this.light = light;
        }

        public String getCo2() {
            return co2;
        }

        public void setCo2(String co2) {
            this.co2 = co2;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();

        m_temp = findViewById(R.id.m_temp);
        m_humi = findViewById(R.id.m_humi);
        m_co2 = findViewById(R.id.m_co2);
        m_soiltemp = findViewById(R.id.m_soiltemp);
        m_soilhum = findViewById(R.id.m_soilhum);
        m_light = findViewById(R.id.m_light);
        m_ph = findViewById(R.id.m_ph);
        lineChart = findViewById(R.id.line_chart);

        setupSwitches();
        hideSystemUI();
        setupChart();
        Mqtt_init();
        startReconnect();

        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 3:  // MQTT 收到消息回传
                        String jsonString = msg.obj.toString();
                        EnvironmentalData data = parseJsonToEnvironmentalData(jsonString);
                        if (data != null) {
                            saveEnvironmentalData(data);  // 保存数据到数据库
                            List<EnvironmentalData> dataList = getEnvironmentalData(); // 获取所有数据
                            updateChart(dataList);  // 更新图表
                            updateUI(data);  // 更新UI
                        }
                        System.out.println(msg.obj.toString());
                        break;
                    case 30:  // 连接失败
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   // 连接成功
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(mqtt_sub_topic, 1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void setupSwitches() {
        // 获取灌溉开关的引用
        Switch irrigationSwitch = findViewById(R.id.switch_water);
        irrigationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                }
            }
        });

        // 获取灯光开关的引用
        Switch lightSwitch = findViewById(R.id.switch_lignt);
        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                }
            }
        });

        // 获取加热开关的引用
        Switch warmSwitch = findViewById(R.id.switch_warm);
        warmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                }
            }
        });

        // 获取遮阳帘开关的引用
        Switch sunSwitch = findViewById(R.id.switch_sun);
        sunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                }
            }
        });

        // 获取通风开关的引用
        Switch cloudSwitch = findViewById(R.id.switch_cloud);
        cloudSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                }
            }
        });

        // 获取自动控制开关的引用
        Switch autoSwitch = findViewById(R.id.switch_auto);
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 这里可以添加自动控制逻辑
            }
        });
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false);
    }

    private void updateChart(List<EnvironmentalData> dataList) {
        List<Entry> entriesTemp = new ArrayList<>();
        List<Entry> entriesHumi = new ArrayList<>();
        List<Entry> entriesSoilTemp = new ArrayList<>();
        List<Entry> entriesSoilHum = new ArrayList<>();
        List<Entry> entriesPh = new ArrayList<>();
        List<Entry> entriesLight = new ArrayList<>();
        List<Entry> entriesCo2 = new ArrayList<>();

        for (EnvironmentalData data : dataList) {
            float x = data.getTimestamp();
            if (data.getTemp() != null) {
                entriesTemp.add(new Entry(x, Float.parseFloat(data.getTemp())));
            }
            if (data.getHumi() != null) {
                entriesHumi.add(new Entry(x, Float.parseFloat(data.getHumi())));
            }
            if (data.getSoiltemp() != null) {
                entriesSoilTemp.add(new Entry(x, Float.parseFloat(data.getSoiltemp())));
            }
            if (data.getSoilhum() != null) {
                entriesSoilHum.add(new Entry(x, Float.parseFloat(data.getSoilhum())));
            }
            if (data.getPh() != null) {
                entriesPh.add(new Entry(x, Float.parseFloat(data.getPh())));
            }
            if (data.getLight() != null) {
                entriesLight.add(new Entry(x, Float.parseFloat(data.getLight())));
            }
            if (data.getCo2() != null) {
                entriesCo2.add(new Entry(x, Float.parseFloat(data.getCo2())));
            }
        }

        LineDataSet dataSetTemp = new LineDataSet(entriesTemp, "Temperature");
        LineDataSet dataSetHumi = new LineDataSet(entriesHumi, "Humidity");
        LineDataSet dataSetSoilTemp = new LineDataSet(entriesSoilTemp, "Soil Temperature");
        LineDataSet dataSetSoilHum = new LineDataSet(entriesSoilHum, "Soil Humidity");
        LineDataSet dataSetPh = new LineDataSet(entriesPh, "pH");
        LineDataSet dataSetLight = new LineDataSet(entriesLight, "Light");
        LineDataSet dataSetCo2 = new LineDataSet(entriesCo2, "CO2");

        LineData lineData = new LineData(dataSetTemp, dataSetHumi, dataSetSoilTemp, dataSetSoilHum, dataSetPh, dataSetLight, dataSetCo2);
        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh
    }

    private void updateUI(EnvironmentalData data) {
        if (data.getTemp() != null) {
            m_temp.setText("温度: " + data.getTemp() + " ℃ ");
        }
        if (data.getHumi() != null) {
            m_humi.setText("湿度: " + data.getHumi() + " % ");
        }
        if (data.getCo2() != null) {
            m_co2.setText("二氧化碳浓度: " + data.getCo2() + " ppm ");
        }
        if (data.getSoilhum() != null) {
            m_soilhum.setText("土壤湿度: " + data.getSoilhum() + " % ");
        }
        if (data.getSoiltemp() != null) {
            m_soiltemp.setText("土壤温度: " + data.getSoiltemp() + " ℃ ");
        }
        if (data.getLight() != null) {
            m_light.setText("光照强度: " + data.getLight() + " lux ");
        }
        if (data.getPh() != null) {
            m_ph.setText("酸碱度: " + data.getPh() + "  ");
        }
    }

    private List<EnvironmentalData> getEnvironmentalData() {
        List<EnvironmentalData> dataList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_NAME,
                DatabaseHelper.COLUMN_TEMP,
                DatabaseHelper.COLUMN_HUMI,
                DatabaseHelper.COLUMN_SOILTEMP,
                DatabaseHelper.COLUMN_SOILHUM,
                DatabaseHelper.COLUMN_PH,
                DatabaseHelper.COLUMN_LIGHT,
                DatabaseHelper.COLUMN_CO2,
                DatabaseHelper.COLUMN_TIMESTAMP
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_TIMESTAMP + " ASC"
        );

        while (cursor.moveToNext()) {
            EnvironmentalData data = new EnvironmentalData();
            data.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
            data.setTemp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEMP)));
            data.setHumi(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HUMI)));
            data.setSoiltemp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SOILTEMP)));
            data.setSoilhum(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SOILHUM)));
            data.setPh(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PH)));
            data.setLight(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LIGHT)));
            data.setCo2(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CO2)));
            data.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)));
            dataList.add(data);
        }
        cursor.close();

        return dataList;
    }

    // JSON数据解析
    private EnvironmentalData parseJsonToEnvironmentalData(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            EnvironmentalData data = new EnvironmentalData();

            if (jsonObject.has("name")) {
                data.setName(jsonObject.getString("name"));
            }
            if (jsonObject.has("Temp")) {
                data.setTemp(jsonObject.getString("Temp"));
            }
            if (jsonObject.has("Humi")) {
                data.setHumi(jsonObject.getString("Humi"));
            }
            if (jsonObject.has("Soiltemp")) {
                data.setSoiltemp(jsonObject.getString("Soiltemp"));
            }
            if (jsonObject.has("Soilhum")) {
                data.setSoilhum(jsonObject.getString("Soilhum"));
            }
            if (jsonObject.has("Ph")) {
                data.setPh(jsonObject.getString("Ph"));
            }
            if (jsonObject.has("Light")) {
                data.setLight(jsonObject.getString("Light"));
            }
            if (jsonObject.has("Co2")) {
                data.setCo2(jsonObject.getString("Co2"));
            }
            data.setTimestamp(System.currentTimeMillis());

            return data;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 保存数据到数据库
    private void saveEnvironmentalData(EnvironmentalData data) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, data.getName());
        values.put(DatabaseHelper.COLUMN_TEMP, data.getTemp());
        values.put(DatabaseHelper.COLUMN_HUMI, data.getHumi());
        values.put(DatabaseHelper.COLUMN_SOILTEMP, data.getSoiltemp());
        values.put(DatabaseHelper.COLUMN_SOILHUM, data.getSoilhum());
        values.put(DatabaseHelper.COLUMN_PH, data.getPh());
        values.put(DatabaseHelper.COLUMN_LIGHT, data.getLight());
        values.put(DatabaseHelper.COLUMN_CO2, data.getCo2());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, data.getTimestamp()); // 保存时间戳

        long newRowId = database.insert(DatabaseHelper.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    // MQTT初始化
    private void Mqtt_init() {
        try {
            client = new MqttClient(host, mqtt_id, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setUserName(userName);
            options.setPassword(passWord.toCharArray());
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message) throws Exception {
                    System.out.println("Message arrived on topic: " + topicName);
                    System.out.println("messageArrived----------");
                    Message msg = handler.obtainMessage(3, message.toString());
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!client.isConnected()) {
                        MqttConnectOptions options = new MqttConnectOptions();
                        client.connect(options);
                        handler.sendEmptyMessage(31);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(30);
                }
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    // 订阅函数 (下发任务/命令)
    private void publishmessageplus(String topic, String message2) {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
*/

/*
package com.example.a5_14;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView m_temp;
    private TextView m_humi;
    private TextView m_co2;
    private TextView m_soiltemp;
    private TextView m_soilhum;
    private TextView m_light;
    private TextView m_ph;
    private LineChart lineChart;

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private String host = "tcp://192.168.67.95";
    private String userName = "test";
    private String passWord = "123456789";
    private String mqtt_id = "1234";
    private String mqtt_sub_topic = "mqtt135";
    private String mqtt_pub_topic = "Control";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public static class EnvironmentalData {
        private String name;
        private String temp;
        private String humi;
        private String soiltemp;
        private String soilhum;
        private String ph;
        private String light;
        private String co2;
        private long timestamp;

        // Getters and Setters...

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public String getHumi() {
            return humi;
        }

        public void setHumi(String humi) {
            this.humi = humi;
        }

        public String getSoiltemp() {
            return soiltemp;
        }

        public void setSoiltemp(String soiltemp) {
            this.soiltemp = soiltemp;
        }

        public String getSoilhum() {
            return soilhum;
        }

        public void setSoilhum(String soilhum) {
            this.soilhum = soilhum;
        }

        public String getPh() {
            return ph;
        }

        public void setPh(String ph) {
            this.ph = ph;
        }

        public String getLight() {
            return light;
        }

        public void setLight(String light) {
            this.light = light;
        }

        public String getCo2() {
            return co2;
        }

        public void setCo2(String co2) {
            this.co2 = co2;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();

        m_temp = findViewById(R.id.m_temp);
        m_humi = findViewById(R.id.m_humi);
        m_co2 = findViewById(R.id.m_co2);
        m_soiltemp = findViewById(R.id.m_soiltemp);
        m_soilhum = findViewById(R.id.m_soilhum);
        m_light = findViewById(R.id.m_light);
        m_ph = findViewById(R.id.m_ph);
        lineChart = findViewById(R.id.line_chart);

        setupSwitches();
        hideSystemUI();
        setupChart();
        Mqtt_init();
        startReconnect();

        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 3:  // MQTT 收到消息回传
                        String jsonString = msg.obj.toString();
                        EnvironmentalData data = parseJsonToEnvironmentalData(jsonString);
                        if (data != null) {
                            saveEnvironmentalData(data);  // 保存数据到数据库
                            List<EnvironmentalData> dataList = getEnvironmentalData(); // 获取所有数据
                            updateChart(dataList);  // 更新图表
                            updateUI(data);  // 更新UI
                        }
                        System.out.println(msg.obj.toString());
                        break;
                    case 30:  // 连接失败
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   // 连接成功
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(mqtt_sub_topic, 1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void setupSwitches() {
        // 获取灌溉开关的引用
        Switch irrigationSwitch = findViewById(R.id.switch_water);
        irrigationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                }
            }
        });

        // 获取灯光开关的引用
        Switch lightSwitch = findViewById(R.id.switch_lignt);
        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                }
            }
        });

        // 获取加热开关的引用
        Switch warmSwitch = findViewById(R.id.switch_warm);
        warmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                }
            }
        });

        // 获取遮阳帘开关的引用
        Switch sunSwitch = findViewById(R.id.switch_sun);
        sunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                }
            }
        });

        // 获取通风开关的引用
        Switch cloudSwitch = findViewById(R.id.switch_cloud);
        cloudSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                }
            }
        });

        // 获取自动控制开关的引用
        Switch autoSwitch = findViewById(R.id.switch_auto);
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 这里可以添加自动控制逻辑
            }
        });
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false);
    }

    private void updateChart(List<EnvironmentalData> dataList) {
        List<Entry> entriesTemp = new ArrayList<>();
        List<Entry> entriesHumi = new ArrayList<>();
        List<Entry> entriesSoilTemp = new ArrayList<>();
        List<Entry> entriesSoilHum = new ArrayList<>();
        List<Entry> entriesPh = new ArrayList<>();
        List<Entry> entriesLight = new ArrayList<>();
        List<Entry> entriesCo2 = new ArrayList<>();

        for (EnvironmentalData data : dataList) {
            float x = data.getTimestamp();
            if (data.getTemp() != null && !data.getTemp().trim().isEmpty()) {
                entriesTemp.add(new Entry(x, Float.parseFloat(data.getTemp().trim())));
            }
            if (data.getHumi() != null && !data.getHumi().trim().isEmpty()) {
                entriesHumi.add(new Entry(x, Float.parseFloat(data.getHumi().trim())));
            }
            if (data.getSoiltemp() != null && !data.getSoiltemp().trim().isEmpty()) {
                entriesSoilTemp.add(new Entry(x, Float.parseFloat(data.getSoiltemp().trim())));
            }
            if (data.getSoilhum() != null && !data.getSoilhum().trim().isEmpty()) {
                entriesSoilHum.add(new Entry(x, Float.parseFloat(data.getSoilhum().trim())));
            }
            if (data.getPh() != null && !data.getPh().trim().isEmpty()) {
                entriesPh.add(new Entry(x, Float.parseFloat(data.getPh().trim())));
            }
            if (data.getLight() != null && !data.getLight().trim().isEmpty()) {
                entriesLight.add(new Entry(x, Float.parseFloat(data.getLight().trim())));
            }
            if (data.getCo2() != null && !data.getCo2().trim().isEmpty()) {
                entriesCo2.add(new Entry(x, Float.parseFloat(data.getCo2().trim())));
            }
        }

        LineDataSet dataSetTemp = new LineDataSet(entriesTemp, "Temperature");
        LineDataSet dataSetHumi = new LineDataSet(entriesHumi, "Humidity");
        LineDataSet dataSetSoilTemp = new LineDataSet(entriesSoilTemp, "Soil Temperature");
        LineDataSet dataSetSoilHum = new LineDataSet(entriesSoilHum, "Soil Humidity");
        LineDataSet dataSetPh = new LineDataSet(entriesPh, "pH");
        LineDataSet dataSetLight = new LineDataSet(entriesLight, "Light");
        LineDataSet dataSetCo2 = new LineDataSet(entriesCo2, "CO2");

        LineData lineData = new LineData(dataSetTemp, dataSetHumi, dataSetSoilTemp, dataSetSoilHum, dataSetPh, dataSetLight, dataSetCo2);
        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh
    }

    private void updateUI(EnvironmentalData data) {
        if (data.getTemp() != null) {
            m_temp.setText("温度: " + data.getTemp() + " ℃ ");
        }
        if (data.getHumi() != null) {
            m_humi.setText("湿度: " + data.getHumi() + " % ");
        }
        if (data.getCo2() != null) {
            m_co2.setText("二氧化碳浓度: " + data.getCo2() + " ppm ");
        }
        if (data.getSoilhum() != null) {
            m_soilhum.setText("土壤湿度: " + data.getSoilhum() + " % ");
        }
        if (data.getSoiltemp() != null) {
            m_soiltemp.setText("土壤温度: " + data.getSoiltemp() + " ℃ ");
        }
        if (data.getLight() != null) {
            m_light.setText("光照强度: " + data.getLight() + " lux ");
        }
        if (data.getPh() != null) {
            m_ph.setText("酸碱度: " + data.getPh() + "  ");
        }
    }

    private List<EnvironmentalData> getEnvironmentalData() {
        List<EnvironmentalData> dataList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_NAME,
                DatabaseHelper.COLUMN_TEMP,
                DatabaseHelper.COLUMN_HUMI,
                DatabaseHelper.COLUMN_SOILTEMP,
                DatabaseHelper.COLUMN_SOILHUM,
                DatabaseHelper.COLUMN_PH,
                DatabaseHelper.COLUMN_LIGHT,
                DatabaseHelper.COLUMN_CO2,
                DatabaseHelper.COLUMN_TIMESTAMP
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_TIMESTAMP + " ASC"
        );

        while (cursor.moveToNext()) {
            EnvironmentalData data = new EnvironmentalData();
            data.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
            data.setTemp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEMP)));
            data.setHumi(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HUMI)));
            data.setSoiltemp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SOILTEMP)));
            data.setSoilhum(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SOILHUM)));
            data.setPh(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PH)));
            data.setLight(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LIGHT)));
            data.setCo2(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CO2)));
            data.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)));
            dataList.add(data);
        }
        cursor.close();

        return dataList;
    }

    // JSON数据解析
    private EnvironmentalData parseJsonToEnvironmentalData(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            EnvironmentalData data = new EnvironmentalData();

            if (jsonObject.has("name")) {
                data.setName(jsonObject.getString("name"));
            }
            if (jsonObject.has("Temp")) {
                data.setTemp(jsonObject.getString("Temp"));
            }
            if (jsonObject.has("Humi")) {
                data.setHumi(jsonObject.getString("Humi"));
            }
            if (jsonObject.has("Soiltemp")) {
                data.setSoiltemp(jsonObject.getString("Soiltemp"));
            }
            if (jsonObject.has("Soilhum")) {
                data.setSoilhum(jsonObject.getString("Soilhum"));
            }
            if (jsonObject.has("Ph")) {
                data.setPh(jsonObject.getString("Ph"));
            }
            if (jsonObject.has("Light")) {
                data.setLight(jsonObject.getString("Light"));
            }
            if (jsonObject.has("Co2")) {
                data.setCo2(jsonObject.getString("Co2"));
            }
            data.setTimestamp(System.currentTimeMillis());

            return data;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 保存数据到数据库
    private void saveEnvironmentalData(EnvironmentalData data) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, data.getName());
        values.put(DatabaseHelper.COLUMN_TEMP, data.getTemp());
        values.put(DatabaseHelper.COLUMN_HUMI, data.getHumi());
        values.put(DatabaseHelper.COLUMN_SOILTEMP, data.getSoiltemp());
        values.put(DatabaseHelper.COLUMN_SOILHUM, data.getSoilhum());
        values.put(DatabaseHelper.COLUMN_PH, data.getPh());
        values.put(DatabaseHelper.COLUMN_LIGHT, data.getLight());
        values.put(DatabaseHelper.COLUMN_CO2, data.getCo2());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, data.getTimestamp()); // 保存时间戳

        long newRowId = database.insert(DatabaseHelper.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    // MQTT初始化
    private void Mqtt_init() {
        try {
            client = new MqttClient(host, mqtt_id, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setUserName(userName);
            options.setPassword(passWord.toCharArray());
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message) throws Exception {
                    System.out.println("Message arrived on topic: " + topicName);
                    System.out.println("messageArrived----------");
                    Message msg = handler.obtainMessage(3, message.toString());
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!client.isConnected()) {
                        MqttConnectOptions options = new MqttConnectOptions();
                        client.connect(options);
                        handler.sendEmptyMessage(31);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(30);
                }
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow(); // 先停止之前的调度器
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                } else {
                    scheduler.shutdown(); // 连接成功后停止调度器
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    // 订阅函数 (下发任务/命令)
    private void publishmessageplus(String topic, String message2) {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
*/
/*
package com.example.a5_14;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView m_temp;
    private TextView m_humi;
    private TextView m_co2;
    private TextView m_soiltemp;
    private TextView m_soilhum;
    private TextView m_light;
    private TextView m_ph;

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private String host = "tcp://192.168.67.95";
    private String userName = "test";
    private String passWord = "123456789";
    private String mqtt_id = "1234";
    private String mqtt_sub_topic = "mqtt135";
    private String mqtt_pub_topic = "Control";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    private Switch irrigationSwitch;
    private Switch lightSwitch;
    private Switch warmSwitch;
    private Switch sunSwitch;
    private Switch cloudSwitch;
    private Switch autoSwitch;
    private boolean isAutoControlEnabled = false;



    public static class EnvironmentalData {
        private String name;
        private String temp;
        private String humi;
        private String soiltemp;
        private String soilhum;
        private String ph;
        private String light;
        private String co2;
        private long timestamp;

        // Getters and Setters...

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public String getHumi() {
            return humi;
        }

        public void setHumi(String humi) {
            this.humi = humi;
        }

        public String getSoiltemp() {
            return soiltemp;
        }

        public void setSoiltemp(String soiltemp) {
            this.soiltemp = soiltemp;
        }

        public String getSoilhum() {
            return soilhum;
        }

        public void setSoilhum(String soilhum) {
            this.soilhum = soilhum;
        }

        public String getPh() {
            return ph;
        }

        public void setPh(String ph) {
            this.ph = ph;
        }

        public String getLight() {
            return light;
        }

        public void setLight(String light) {
            this.light = light;
        }

        public String getCo2() {
            return co2;
        }

        public void setCo2(String co2) {
            this.co2 = co2;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();

        m_temp = findViewById(R.id.m_temp);
        m_humi = findViewById(R.id.m_humi);
        m_co2 = findViewById(R.id.m_co2);
        m_soiltemp = findViewById(R.id.m_soiltemp);
        m_soilhum = findViewById(R.id.m_soilhum);
        m_light = findViewById(R.id.m_light);
        m_ph = findViewById(R.id.m_ph);

        irrigationSwitch = findViewById(R.id.switch_water);
        lightSwitch = findViewById(R.id.switch_lignt);
        warmSwitch = findViewById(R.id.switch_warm);
        sunSwitch = findViewById(R.id.switch_sun);
        cloudSwitch = findViewById(R.id.switch_cloud);
        autoSwitch = findViewById(R.id.switch_auto);

        setupSwitches();
        hideSystemUI();
        Mqtt_init();
        startReconnect();

        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 3:  // MQTT 收到消息回传
                        String jsonString = msg.obj.toString();
                        EnvironmentalData data = parseJsonToEnvironmentalData(jsonString);
                        if (data != null) {
                            saveEnvironmentalData(data);  // 保存数据到数据库
                            updateUI(data);  // 更新UI
                        }
                        System.out.println(msg.obj.toString());
                        break;
                    case 30:  // 连接失败
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   // 连接成功
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(mqtt_sub_topic, 1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        // Setup button to navigate to LineChartActivity
        Button buttonViewChart = findViewById(R.id.button_view_chart);
        buttonViewChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LineChartActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSwitches() {
        // 自动控制开关
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAutoControlEnabled = isChecked;
                setManualSwitchesEnabled(!isChecked);
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "自动控制已开启", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "自动控制已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });

    // 灌溉开关
    irrigationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isAutoControlEnabled) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                }
            }
        }
    });

    // 灯光开关
    lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isAutoControlEnabled) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                }
            }
        }
    });

    // 加热开关
    warmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isAutoControlEnabled) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                }
            }
        }
    });

    // 遮阳帘开关
    sunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isAutoControlEnabled) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                }
            }
        }
    });

    // 通风开关
    cloudSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isAutoControlEnabled) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                }
            }
        }
    });
}

    private void setManualSwitchesEnabled(boolean enabled) {
        irrigationSwitch.setEnabled(enabled);
        lightSwitch.setEnabled(enabled);
        warmSwitch.setEnabled(enabled);
        sunSwitch.setEnabled(enabled);
        cloudSwitch.setEnabled(enabled);
    }

    private void autoControl(EnvironmentalData data) {
        float temp = Float.parseFloat(data.getTemp());
        float humi = Float.parseFloat(data.getHumi());
        float soilHum = Float.parseFloat(data.getSoilhum());
        float light = Float.parseFloat(data.getLight());
        float co2 = Float.parseFloat(data.getCo2());

        if (temp > 30) {
            cloudSwitch.setChecked(true);
            publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
        } else if (temp < 20) {
            warmSwitch.setChecked(true);
            publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
        } else {
            cloudSwitch.setChecked(false);
            publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
            warmSwitch.setChecked(false);
            publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
        }

        if (light > 20000) {
            sunSwitch.setChecked(true);
            publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
        } else if (light < 10000) {
            lightSwitch.setChecked(true);
            publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
        } else {
            sunSwitch.setChecked(false);
            publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
            lightSwitch.setChecked(false);
            publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
        }

        if (soilHum < 50) {
            irrigationSwitch.setChecked(true);
            publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
        } else {
            irrigationSwitch.setChecked(false);
            publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
        }

        if (co2 > 50 || co2 < 20 || humi > 70) {
            cloudSwitch.setChecked(true);
            publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
        } else {
            cloudSwitch.setChecked(false);
            publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
        }
    }

    /*private void setupSwitches() {
        // 获取灌溉开关的引用
        Switch irrigationSwitch = findViewById(R.id.switch_water);
        irrigationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                }
            }
        });

        // 获取灯光开关的引用
        Switch lightSwitch = findViewById(R.id.switch_lignt);
        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                }
            }
        });

        // 获取加热开关的引用
        Switch warmSwitch = findViewById(R.id.switch_warm);
        warmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                }
            }
        });

        // 获取遮阳帘开关的引用
        Switch sunSwitch = findViewById(R.id.switch_sun);
        sunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                }
            }
        });

        // 获取通风开关的引用
        Switch cloudSwitch = findViewById(R.id.switch_cloud);
        cloudSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else {
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                }
            }
        });

        // 获取自动控制开关的引用
        Switch autoSwitch = findViewById(R.id.switch_auto);
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 这里可以添加自动控制逻辑
            }
        });
    }*/
/*
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void updateUI(EnvironmentalData data) {
        if (data.getTemp() != null) {
            m_temp.setText("温度: " + data.getTemp() + " ℃ ");
        }
        if (data.getHumi() != null) {
            m_humi.setText("湿度: " + data.getHumi() + " % ");
        }
        if (data.getCo2() != null) {
            m_co2.setText("二氧化碳浓度: " + data.getCo2() + " ppm ");
        }
        if (data.getSoilhum() != null) {
            m_soilhum.setText("土壤湿度: " + data.getSoilhum() + " % ");
        }
        if (data.getSoiltemp() != null) {
            m_soiltemp.setText("土壤温度: " + data.getSoiltemp() + " ℃ ");
        }
        if (data.getLight() != null) {
            m_light.setText("光照强度: " + data.getLight() + " lux ");
        }
        if (data.getPh() != null) {
            m_ph.setText("酸碱度: " + data.getPh() + "  ");
        }
    }

    // JSON数据解析
    private EnvironmentalData parseJsonToEnvironmentalData(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            EnvironmentalData data = new EnvironmentalData();

            if (jsonObject.has("name")) {
                data.setName(jsonObject.getString("name"));
            }
            if (jsonObject.has("Temp")) {
                data.setTemp(jsonObject.getString("Temp"));
            }
            if (jsonObject.has("Humi")) {
                data.setHumi(jsonObject.getString("Humi"));
            }
            if (jsonObject.has("Soiltemp")) {
                data.setSoiltemp(jsonObject.getString("Soiltemp"));
            }
            if (jsonObject.has("Soilhum")) {
                data.setSoilhum(jsonObject.getString("Soilhum"));
            }
            if (jsonObject.has("Ph")) {
                data.setPh(jsonObject.getString("Ph"));
            }
            if (jsonObject.has("Light")) {
                data.setLight(jsonObject.getString("Light"));
            }
            if (jsonObject.has("Co2")) {
                data.setCo2(jsonObject.getString("Co2"));
            }
            data.setTimestamp(System.currentTimeMillis());

            return data;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 保存数据到数据库
    private void saveEnvironmentalData(EnvironmentalData data) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, data.getName());
        values.put(DatabaseHelper.COLUMN_TEMP, data.getTemp());
        values.put(DatabaseHelper.COLUMN_HUMI, data.getHumi());
        values.put(DatabaseHelper.COLUMN_SOILTEMP, data.getSoiltemp());
        values.put(DatabaseHelper.COLUMN_SOILHUM, data.getSoilhum());
        values.put(DatabaseHelper.COLUMN_PH, data.getPh());
        values.put(DatabaseHelper.COLUMN_LIGHT, data.getLight());
        values.put(DatabaseHelper.COLUMN_CO2, data.getCo2());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, data.getTimestamp()); // 保存时间戳

        long newRowId = database.insert(DatabaseHelper.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    // MQTT初始化
    private void Mqtt_init() {
        try {
            client = new MqttClient(host, mqtt_id, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setUserName(userName);
            options.setPassword(passWord.toCharArray());
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message) throws Exception {
                    System.out.println("Message arrived on topic: " + topicName);
                    System.out.println("messageArrived----------");
                    Message msg = handler.obtainMessage(3, message.toString());
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!client.isConnected()) {
                        MqttConnectOptions options = new MqttConnectOptions();
                        client.connect(options);
                        handler.sendEmptyMessage(31);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(30);
                }
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow(); // 先停止之前的调度器
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                } else {
                    scheduler.shutdown(); // 连接成功后停止调度器
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    // 订阅函数 (下发任务/命令)
    private void publishmessageplus(String topic, String message2) {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
*/

package com.example.a5_14;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView m_test;

    private TextView m_temp;

    private TextView m_control;
    private TextView m_humi;
    private TextView m_co2;
    private TextView m_soiltemp;
    private TextView m_soilhum;
    private TextView m_light;
    private TextView m_ph;

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private String host = "tcp://114.55.89.187";
    private String userName = "mqttx_e0167616454554";
    private String passWord = "123456789";
    private String mqtt_id = "mqttx_e01676167878";
    private String mqtt_sub_topic = "mqtt135";
    private String mqtt_pub_topic = "Control";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    private Switch irrigationSwitch;
    private Switch lightSwitch;
    private Switch warmSwitch;
    private Switch sunSwitch;
    private Switch cloudSwitch;
    private Switch autoSwitch;
    private Switch plantSwitch;
    private boolean isAutoControlEnabled = false;
    private boolean isCloudOn = false;
    private boolean isWarmOn = false;
    private boolean isWaterOn = false;
    private boolean isSunOn = false;
    private boolean isLedOn = false;

    public static class EnvironmentalData {
        private String name;
        private String temp;
        private String humi;
        private String soiltemp;
        private String soilhum;
        private String ph;
        private String light;
        private String co2;
        private long timestamp;




        // Getters and Setters...

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public String getHumi() {
            return humi;
        }

        public void setHumi(String humi) {
            this.humi = humi;
        }

        public String getSoiltemp() {
            return soiltemp;
        }

        public void setSoiltemp(String soiltemp) {
            this.soiltemp = soiltemp;
        }

        public String getSoilhum() {
            return soilhum;
        }

        public void setSoilhum(String soilhum) {
            this.soilhum = soilhum;
        }

        public String getPh() {
            return ph;
        }

        public void setPh(String ph) {
            this.ph = ph;
        }

        public String getLight() {
            return light;
        }

        public void setLight(String light) {
            this.light = light;
        }

        public String getCo2() {
            return co2;
        }

        public void setCo2(String co2) {
            this.co2 = co2;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();

        m_test= findViewById(R.id.m_test);
        Drawable test=getResources().getDrawable(R.drawable.test);
        test.setBounds(0,0,70,70);//必须设置图片的大小否则没有作用
        m_test.setCompoundDrawables(test,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_control= findViewById(R.id.m_control);
        Drawable control=getResources().getDrawable(R.drawable.test);
        control.setBounds(0,0,70,70);//必须设置图片的大小否则没有作用
        m_control.setCompoundDrawables(control,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_temp = findViewById(R.id.m_temp);
        Drawable left=getResources().getDrawable(R.drawable.temp);
        left.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_temp.setCompoundDrawables(left,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_humi = findViewById(R.id.m_humi);
        Drawable humi=getResources().getDrawable(R.drawable.humi);
        humi.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_humi.setCompoundDrawables(humi,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_co2 = findViewById(R.id.m_co2);
        Drawable co=getResources().getDrawable(R.drawable.co2);
        co.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_co2.setCompoundDrawables(co,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_soiltemp = findViewById(R.id.m_soiltemp);
        Drawable soiltemp=getResources().getDrawable(R.drawable.soiltemp);
        soiltemp.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_soiltemp.setCompoundDrawables(soiltemp,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_soilhum = findViewById(R.id.m_soilhum);
        Drawable soilhum=getResources().getDrawable(R.drawable.soilhumi);
        soilhum.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_soilhum.setCompoundDrawables(soilhum,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_light = findViewById(R.id.m_light);
        Drawable light=getResources().getDrawable(R.drawable.light);
        light.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_light.setCompoundDrawables(light,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        m_ph = findViewById(R.id.m_ph);
        Drawable ph=getResources().getDrawable(R.drawable.ph);
        ph.setBounds(0,0,50,50);//必须设置图片的大小否则没有作用
        m_ph.setCompoundDrawables(ph,null ,null,null);//设置图片left这里如果是右边就放到第二个参数里面依次对应

        irrigationSwitch = findViewById(R.id.switch_water);
        Drawable water=getResources().getDrawable(R.drawable.water);
        water.setBounds(0,0,40,40);
        irrigationSwitch.setCompoundDrawables(water,null,null,null);


        lightSwitch = findViewById(R.id.switch_lignt);
        Drawable lignt=getResources().getDrawable(R.drawable.cosun);
        lignt.setBounds(0,0,40,40);
        lightSwitch.setCompoundDrawables(lignt,null,null,null);

        warmSwitch = findViewById(R.id.switch_warm);
        Drawable warm=getResources().getDrawable(R.drawable.warm);
        warm.setBounds(0,0,40,40);
        warmSwitch.setCompoundDrawables(warm,null,null,null);

        sunSwitch = findViewById(R.id.switch_sun);
        Drawable sun=getResources().getDrawable(R.drawable.sun);
        sun.setBounds(0,0,40,40);
        sunSwitch.setCompoundDrawables(sun,null,null,null);

        cloudSwitch = findViewById(R.id.switch_cloud);
        Drawable cloud=getResources().getDrawable(R.drawable.cloud);
        cloud.setBounds(0,0,40,40);
        cloudSwitch.setCompoundDrawables(cloud,null,null,null);

        autoSwitch = findViewById(R.id.switch_auto);
        Drawable auto=getResources().getDrawable(R.drawable.auto);
        auto.setBounds(0,0,40,40);
        autoSwitch.setCompoundDrawables(auto,null,null,null);

        plantSwitch= findViewById(R.id.switch_plant);
        Drawable plant=getResources().getDrawable(R.drawable.plant);
        plant.setBounds(0,0,40,40);
        plantSwitch.setCompoundDrawables(plant,null,null,null);


        setupSwitches();
        hideSystemUI();
        Mqtt_init();
        startReconnect();

        Button buttonImagePicker = findViewById(R.id.button_image_picker);
        buttonImagePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
                startActivity(intent);
            }
        });

        // 跳转到实时识别页面
        Button buttonRealTimeRecognition = findViewById(R.id.button_real_time_recognition);
        buttonRealTimeRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoStreamActivity.class);
                startActivity(intent);
            }
        });

        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 3:  // MQTT 收到消息回传
                        String jsonString = msg.obj.toString();
                        EnvironmentalData data = parseJsonToEnvironmentalData(jsonString);
                        if (data != null) {
                            saveEnvironmentalData(data);  // 保存数据到数据库
                            updateUI(data);  // 更新UI
                            if (isAutoControlEnabled) {
                                autoControl(data);
                            }
                            publishEnvironmentalData(data);  // 发布环境数据
                        }
                        System.out.println(msg.obj.toString());
                        break;
                    case 30:  // 连接失败
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   // 连接成功
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(mqtt_sub_topic, 1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        // Setup button to navigate to LineChartActivity
        Button buttonViewChart = findViewById(R.id.button_view_chart);
        buttonViewChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LineChartActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSwitches() {
        // 自动控制开关
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAutoControlEnabled = isChecked;
                setManualSwitchesEnabled(!isChecked);
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "自动控制已开启", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "自动控制已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 灌溉开关
        irrigationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isAutoControlEnabled) {
                    if (isChecked) {
                        publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                    } else {
                        publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                    }
                }
            }
        });

        // 灯光开关
        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isAutoControlEnabled) {
                    if (isChecked) {
                        publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                    } else {
                        publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                    }
                }
            }
        });

        // 加热开关
        warmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isAutoControlEnabled) {
                    if (isChecked) {
                        publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                    } else {
                        publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                    }
                }
            }
        });

        // 遮阳帘开关
        sunSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isAutoControlEnabled) {
                    if (isChecked) {
                        publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                    } else {
                        publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                    }
                }
            }
        });

        // 通风开关
        cloudSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isAutoControlEnabled) {
                    if (isChecked) {
                        publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                    } else {
                        publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                    }
                }
            }
        });
    }

    private void setManualSwitchesEnabled(boolean enabled) {
        irrigationSwitch.setEnabled(enabled);
        lightSwitch.setEnabled(enabled);
        warmSwitch.setEnabled(enabled);
        sunSwitch.setEnabled(enabled);
        cloudSwitch.setEnabled(enabled);
    }


    private void publishEnvironmentalData(EnvironmentalData data) {
        try {
            JSONArray jsonArray = new JSONArray();

            if (data.getTemp() != null && !data.getTemp().trim().isEmpty()) {
                JSONObject jsonObjectTemp = new JSONObject();
                jsonObjectTemp.put("id", "temperature");
                jsonObjectTemp.put("value", data.getTemp());
                jsonArray.put(jsonObjectTemp);
            }

            if (data.getHumi() != null && !data.getHumi().trim().isEmpty()) {
                JSONObject jsonObjectHumi = new JSONObject();
                jsonObjectHumi.put("id", "humidity");
                jsonObjectHumi.put("value", data.getHumi());
                jsonArray.put(jsonObjectHumi);
            }

            if (data.getSoiltemp() != null && !data.getSoiltemp().trim().isEmpty()) {
                JSONObject jsonObjectSoilTemp = new JSONObject();
                jsonObjectSoilTemp.put("id", "soil_temperature");
                jsonObjectSoilTemp.put("value", data.getSoiltemp());
                jsonArray.put(jsonObjectSoilTemp);
            }

            if (data.getSoilhum() != null && !data.getSoilhum().trim().isEmpty()) {
                JSONObject jsonObjectSoilHum = new JSONObject();
                jsonObjectSoilHum.put("id", "soil_humidity");
                jsonObjectSoilHum.put("value", data.getSoilhum());
                jsonArray.put(jsonObjectSoilHum);
            }

            if (data.getLight() != null && !data.getLight().trim().isEmpty()) {
                JSONObject jsonObjectLight = new JSONObject();
                jsonObjectLight.put("id", "light");
                jsonObjectLight.put("value", data.getLight());
                jsonArray.put(jsonObjectLight);
            }

            if (data.getCo2() != null && !data.getCo2().trim().isEmpty()) {
                JSONObject jsonObjectCo2 = new JSONObject();
                jsonObjectCo2.put("id", "co2");
                jsonObjectCo2.put("value", data.getCo2());
                jsonArray.put(jsonObjectCo2);
            }

            if (data.getPh() != null && !data.getPh().trim().isEmpty()) {
                JSONObject jsonObjectPh = new JSONObject();
                jsonObjectPh.put("id", "ph");
                jsonObjectPh.put("value", data.getPh());
                jsonArray.put(jsonObjectPh);
            }

            if (jsonArray.length() > 0) {
                String messagePayload = jsonArray.toString();
                publishmessageplus("/43/1/property/post", messagePayload);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /*private void autoControl(EnvironmentalData data) {
        try {
            // 检查并解析温度数据
            if (data.getTemp() != null && !data.getTemp().trim().isEmpty()) {
                float temp = Float.parseFloat(data.getTemp().trim());
                if (temp > 37) {
                    Log.d(TAG, "Temperature is above 30. Turning on cloud.");
                    cloudSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else if (temp < 20) {
                    Log.d(TAG, "Temperature is below 20. Turning on warm.");
                    warmSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                } else {
                    Log.d(TAG, "Temperature is normal. Turning off cloud and warm.");
                    cloudSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                    warmSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                }
            } else {
                Log.d(TAG, "Temperature data is null or empty. Skipping temperature control.");
            }

            // 检查并解析湿度数据
            if (data.getHumi() != null && !data.getHumi().trim().isEmpty()) {
                float humi = Float.parseFloat(data.getHumi().trim());
                // 根据湿度进行相应的控制
                if (humi > 70) {
                    Log.d(TAG, "Humidity is above 70. Turning on cloud.");
                    cloudSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else {
                    Log.d(TAG, "Humidity is normal. Turning off cloud.");
                    cloudSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                }
            } else {
                Log.d(TAG, "Humidity data is null or empty. Skipping humidity control.");
            }

            // 检查并解析土壤湿度数据
            if (data.getSoilhum() != null && !data.getSoilhum().trim().isEmpty()) {
                float soilHum = Float.parseFloat(data.getSoilhum().trim());
                if (soilHum < 10) {
                    Log.d(TAG, "Soil humidity is below 50. Turning on water.");
                    irrigationSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                } else {
                    Log.d(TAG, "Soil humidity is normal. Turning off water.");
                    irrigationSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                }
            } else {
                Log.d(TAG, "Soil humidity data is null or empty. Skipping soil humidity control.");
            }

            // 检查并解析光照强度数据
            if (data.getLight() != null && !data.getLight().trim().isEmpty()) {
                float light = Float.parseFloat(data.getLight().trim());
                if (light > 1000) {
                    Log.d(TAG, "Light is above 20000. Turning on sun.");
                    sunSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                } else if (light < 100) {
                    Log.d(TAG, "Light is below 10000. Turning on led.");
                    lightSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                } else {
                    Log.d(TAG, "Light is normal. Turning off sun and led.");
                    sunSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                    lightSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                }
            } else {
                Log.d(TAG, "Light data is null or empty. Skipping light control.");
            }

            // 检查并解析二氧化碳浓度数据
            if (data.getCo2() != null && !data.getCo2().trim().isEmpty()) {
                float co2 = Float.parseFloat(data.getCo2().trim());
                if (co2 > 50 || co2 < 20) {
                    Log.d(TAG, "CO2 is out of range. Turning on cloud.");
                    cloudSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                } else {
                    Log.d(TAG, "CO2 is normal. Turning off cloud.");
                    cloudSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                }
            } else {
                Log.d(TAG, "CO2 data is null or empty. Skipping CO2 control.");
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse sensor data", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in autoControl", e);
        }
    }*/

    private void delayedPublish(final String topic, final String message, long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                publishmessageplus(topic, message);
            }
        }, delayMillis);
    }


    private void autoControl(EnvironmentalData data) {
        try {
            // 检查并解析温度数据
            /*if (data.getTemp() != null && !data.getTemp().trim().isEmpty()) {
                float temp = Float.parseFloat(data.getTemp().trim());
                if (temp > 37 && !isCloudOn) {
                    Log.d(TAG, "Temperature is above 37. Turning on cloud.");
                    cloudSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                    isCloudOn = true;
                    isWarmOn = false;
                } else if (temp < 20 && !isWarmOn) {
                    Log.d(TAG, "Temperature is below 20. Turning on warm.");
                    warmSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"warm_on\"]");
                    isWarmOn = true;
                    isCloudOn = false;
                } else if (temp >= 20 && temp <= 37) {
                    Log.d(TAG, "Temperature is normal. Turning off cloud and warm.");
                    if (isCloudOn) {
                        cloudSwitch.setChecked(false);
                        publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                        isCloudOn = false;
                    }
                    if (isWarmOn) {
                        warmSwitch.setChecked(false);
                        publishmessageplus(mqtt_pub_topic, "[\"warm_off\"]");
                        isWarmOn = false;
                    }
                }
            }*/

            // 检查并解析湿度数据
            /*if (data.getHumi() != null && !data.getHumi().trim().isEmpty()) {
                float humi = Float.parseFloat(data.getHumi().trim());
                if (humi > 70 && !isCloudOn) {
                    Log.d(TAG, "Humidity is above 70. Turning on cloud.");
                    cloudSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                    isCloudOn = true;
                } else if (humi <= 70 && isCloudOn) {
                    Log.d(TAG, "Humidity is normal. Turning off cloud.");
                    cloudSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                    isCloudOn = false;
                }
            }*/

            // 检查并解析土壤湿度数据
            if (data.getSoilhum() != null && !data.getSoilhum().trim().isEmpty()) {
                float soilHum = Float.parseFloat(data.getSoilhum().trim());
                if (soilHum < 15 && !isWaterOn) {
                    Log.d(TAG, "Soil humidity is below 10. Turning on water.");
                    irrigationSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"water_on\"]");
                    isWaterOn = true;

                } else if (soilHum >= 15 && isWaterOn) {
                    Log.d(TAG, "Soil humidity is normal. Turning off water.");
                    irrigationSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"water_off\"]");
                    isWaterOn = false;

                }
            }

            // 检查并解析光照强度数据
            if (data.getLight() != null && !data.getLight().trim().isEmpty()) {
                float light = Float.parseFloat(data.getLight().trim());
                if (light > 1000 && !isSunOn) {
                    Log.d(TAG, "Light is above 1000. Turning on sun.");
                    sunSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"sun_on\"]");
                    isSunOn = true;
                    isLedOn = false;
                } else if (light < 100 && !isLedOn) {
                    Log.d(TAG, "Light is below 100. Turning on led.");
                    lightSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"led_on\"]");
                    isLedOn = true;
                    isSunOn = false;
                } else if (light >= 100 && light <= 1000) {
                    Log.d(TAG, "Light is normal. Turning off sun and led.");
                    if (isSunOn) {
                        sunSwitch.setChecked(false);
                        publishmessageplus(mqtt_pub_topic, "[\"sun_off\"]");
                        isSunOn = false;
                    }
                    if (isLedOn) {
                        lightSwitch.setChecked(false);
                        publishmessageplus(mqtt_pub_topic, "[\"led_off\"]");
                        isLedOn = false;
                    }
                }
            }

            /*// 检查并解析二氧化碳浓度数据
            if (data.getCo2() != null && !data.getCo2().trim().isEmpty()) {
                float co2 = Float.parseFloat(data.getCo2().trim());
                if ((co2 > 50 || co2 < 20) && !isCloudOn) {
                    Log.d(TAG, "CO2 is out of range. Turning on cloud.");
                    cloudSwitch.setChecked(true);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_on\"]");
                    isCloudOn = true;
                } else if (co2 >= 20 && co2 <= 50 && isCloudOn) {
                    Log.d(TAG, "CO2 is normal. Turning off cloud.");
                    cloudSwitch.setChecked(false);
                    publishmessageplus(mqtt_pub_topic, "[\"cloud_off\"]");
                    isCloudOn = false;
                }
            }*/
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse sensor data", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in autoControl", e);
        }
    }




    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void updateUI(EnvironmentalData data) {
        if (data.getTemp() != null) {
            m_temp.setText("   温度: " + data.getTemp() + " ℃ ");
        }
        if (data.getHumi() != null) {
            m_humi.setText("   湿度: " + data.getHumi() + " % ");
        }
        if (data.getCo2() != null) {
            m_co2.setText("   二氧化碳浓度: " + data.getCo2() + " ppm ");
        }
        if (data.getSoilhum() != null) {
            m_soilhum.setText("   土壤湿度: " + data.getSoilhum() + " % ");
        }
        if (data.getSoiltemp() != null) {
            m_soiltemp.setText("   土壤温度: " + data.getSoiltemp() + " ℃ ");
        }
        if (data.getLight() != null) {
            m_light.setText("   光照强度: " + data.getLight() + " lux ");
        }
        if (data.getPh() != null) {
            m_ph.setText("   酸碱度: " + data.getPh() + "  ");
        }
    }

    // JSON数据解析
    private EnvironmentalData parseJsonToEnvironmentalData(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            EnvironmentalData data = new EnvironmentalData();

            if (jsonObject.has("name")) {
                data.setName(jsonObject.getString("name"));
            }
            if (jsonObject.has("Temp")) {
                data.setTemp(jsonObject.getString("Temp"));
            }
            if (jsonObject.has("Humi")) {
                data.setHumi(jsonObject.getString("Humi"));
            }
            if (jsonObject.has("Soiltemp")) {
                data.setSoiltemp(jsonObject.getString("Soiltemp"));
            }
            if (jsonObject.has("Soilhum")) {
                data.setSoilhum(jsonObject.getString("Soilhum"));
            }
            if (jsonObject.has("Ph")) {
                data.setPh(jsonObject.getString("Ph"));
            }
            if (jsonObject.has("Light")) {
                data.setLight(jsonObject.getString("Light"));
            }
            if (jsonObject.has("Co2")) {
                data.setCo2(jsonObject.getString("Co2"));
            }
            data.setTimestamp(System.currentTimeMillis());

            return data;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 保存数据到数据库
    private void saveEnvironmentalData(EnvironmentalData data) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, data.getName());
        values.put(DatabaseHelper.COLUMN_TEMP, data.getTemp());
        values.put(DatabaseHelper.COLUMN_HUMI, data.getHumi());
        values.put(DatabaseHelper.COLUMN_SOILTEMP, data.getSoiltemp());
        values.put(DatabaseHelper.COLUMN_SOILHUM, data.getSoilhum());
        values.put(DatabaseHelper.COLUMN_PH, data.getPh());
        values.put(DatabaseHelper.COLUMN_LIGHT, data.getLight());
        values.put(DatabaseHelper.COLUMN_CO2, data.getCo2());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, data.getTimestamp()); // 保存时间戳

        long newRowId = database.insert(DatabaseHelper.TABLE_NAME, null, values);
        /*if (newRowId == -1) {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
        }*/
    }

    // MQTT初始化
    private void Mqtt_init() {
        try {
            client = new MqttClient(host, mqtt_id, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setUserName(userName);
            options.setPassword(passWord.toCharArray());
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message) throws Exception {
                    System.out.println("Message arrived on topic: " + topicName);
                    System.out.println("messageArrived----------");
                    Message msg = handler.obtainMessage(3, message.toString());
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!client.isConnected()) {
                        MqttConnectOptions options = new MqttConnectOptions();
                        client.connect(options);
                        handler.sendEmptyMessage(31);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(30);
                }
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow(); // 先停止之前的调度器
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                } else {
                    scheduler.shutdown(); // 连接成功后停止调度器
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    // 订阅函数 (下发任务/命令)
    private void publishmessageplus(String topic, String message2) {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}