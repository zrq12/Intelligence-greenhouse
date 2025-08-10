package com.example.a5_14;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class LineChartActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_chart);

        lineChart = findViewById(R.id.line_chart);

        // Retrieve data and update the chart
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<MainActivity.EnvironmentalData> dataList = getEnvironmentalData(dbHelper);
        updateChart(dataList);
    }

    private List<MainActivity.EnvironmentalData> getEnvironmentalData(DatabaseHelper dbHelper) {
        List<MainActivity.EnvironmentalData> dataList = new ArrayList<>();
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
            MainActivity.EnvironmentalData data = new MainActivity.EnvironmentalData();
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

    private void updateChart(List<MainActivity.EnvironmentalData> dataList) {
        List<Entry> entriesTemp = new ArrayList<>();
        List<Entry> entriesHumi = new ArrayList<>();
        List<Entry> entriesSoilTemp = new ArrayList<>();
        List<Entry> entriesSoilHum = new ArrayList<>();
        List<Entry> entriesPh = new ArrayList<>();
        List<Entry> entriesLight = new ArrayList<>();
        List<Entry> entriesCo2 = new ArrayList<>();

        for (MainActivity.EnvironmentalData data : dataList) {
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
}
