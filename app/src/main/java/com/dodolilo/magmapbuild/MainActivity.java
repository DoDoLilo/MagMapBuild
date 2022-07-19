package com.dodolilo.magmapbuild;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    /**
     * UI组件声明
     */
    private EditText editTextPersonId;
    private EditText editTextSampNum;
    private Button buttonMinusPersonId;
    private Button buttonAddPersonId;
    private Button buttonMinusSampNum;
    private Button buttonAddSampNum;
    private Button buttonStartSampling;
    private Button buttonMarkPoint;
    private Spinner spinnerSelectPoint;

    /**
     * 数据采集类对象
     */
    private SensorsBee sensorsBee;

    /**
     * 上次采数使用的用户编号与采数序号.
     */
    private int lastPersonId;
    private int lastSampNum;

    /**
     * lastPersonId与lastSampNum在<b>应用内部存储空间</b>的存储文件名.
     */
    private final String INTERNAL_CACHE_FILE = "personIdAndSampNumCache.csv";

    /**
     * 保存在<b>应用外部存储空间</b>的”打点文件“.
     */
    private final String EXTERNAL_POINTS_FILE = "points.csv";

    /**
     * 点名称，点坐标
     */
    private Map<String, float[]> pointsMap;

    /**
     * 通过下拉列表选择的点名称.
     */
    private String selectedPointName;

    /**
     * 通过“打点”按钮记录的打点信息："time,x,y\n" * N
     */
    private StringBuilder pointMarkStrBd;

    /**
     * 文件标记，帮助统一传感器文件与打点文件的名称.
     */
    private String fileMark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //0.从外存中读取打点文件，TODO 如果文件不存在则直接退出app
        String pointsStr = CsvDataTools.readCsvFromExternalStorage(EXTERNAL_POINTS_FILE, this);
        if (pointsStr == null) {
            MessageBuilder.showMessageWithOK(this, "App Error", "缺少打点文件points.csv，请手动导入后重启app！");
            return;
        }
        pointsMap = CsvDataTools.changePointsCsvToMap(pointsStr);

        //1.获取UI组件引用
        editTextPersonId = findViewById(R.id.editTextPersonID);
        editTextSampNum = findViewById(R.id.editTextSampNum);
        buttonMinusPersonId = findViewById(R.id.buttonMinusPersonID);
        buttonAddPersonId = findViewById(R.id.buttonAddPersonID);
        buttonMinusSampNum = findViewById(R.id.buttonMinusSampNum);
        buttonAddSampNum = findViewById(R.id.buttonAddSampNum);
        buttonStartSampling = findViewById(R.id.buttonStartSampling);
        buttonMarkPoint = findViewById(R.id.buttonMarkPoint);
        spinnerSelectPoint = findViewById(R.id.spinnerSelectPoint);

        //2.构造采数类实例，context即这个Activity对象本身
        sensorsBee = new SensorsBee(this);

        //3.注册UI组件监听器
        // TODO: 2022/7/12 检查新ID和Num是否和上次的一样，提示用户是否继续采集
        setComponentsListeners();

        //4.TODO :从应用内部存储空间中获取lastPersonId和lastSampNum



    }

    /**
     * 系统会在销毁 Activity 之前调用此回调。
     *
     * 此回调是 Activity 接收的最后一个回调。
     * 通常，实现 onDestroy() 是为了确保
     * 在销毁 Activity 或包含该 Activity 的进程时释放该 Activity 的所有资源。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        sensorsBee.stopSensorRecord();
        // TODO: 2022/7/11 在app死掉前，1、将sensorBee中的上次序号保存到文件，
    }

    private void setComponentsListeners() {
        //设置“开始采集”、“停止采集”按钮的监听器
        buttonStartSampling.setOnClickListener(v -> {
            if (sensorsBee.isRecording()) {
                //停止采数，保存打点文件，将按钮文本改为”开始采数“，将按钮颜色改为绿色
                sensorsBee.stopSensorRecord();
                String pointMarkFileName = fileMark.concat("_points");
                CsvDataTools.saveCsvToExternalStorage(pointMarkFileName, pointMarkStrBd.toString(), this);
                buttonStartSampling.setText(R.string.button_start_record);
                buttonStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.start_green));
            } else {
                //开始采数，将按钮文本改为”停止采数“，将按钮颜色改为红色
                fileMark = "TEST_".concat(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINA).format(new Date()));
                String sensorDataFileName = fileMark.concat("_sensors");
                pointMarkStrBd = new StringBuilder();
                boolean startSucceed = sensorsBee.startSensorRecord(sensorDataFileName);
                if (!startSucceed) {
                    MessageBuilder.showMessageWithOK(this,"Start Error", "Start Failed");
                    return;
                }
                buttonStartSampling.setText(R.string.button_stop_record);
                buttonStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.stop_red));
            }
        });

        //动态创建下拉列表，并注册监听器
        ArrayList<String> pointNameList = new ArrayList<>();
        for (Map.Entry<String, float[]> entry: pointsMap.entrySet()) {
            pointNameList.add(entry.getKey());
        }
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pointNameList);
        stringArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerSelectPoint.setAdapter(stringArrayAdapter);
        spinnerSelectPoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPointName = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //设置”打点“按钮的检测器
        buttonMarkPoint.setOnClickListener(v -> {
            if (selectedPointName == null) {
                MessageBuilder.showMessageWithOK(this, "提示", "未选择点");
            } else {
                // TODO: 2022/7/19 打点逻辑
                if (sensorsBee.isRecording()) {
                    float[] pointXY = pointsMap.get(selectedPointName);
                    pointMarkStrBd.append(CsvDataTools.convertPointToCsvFormat(pointXY));
                } else {
                    MessageBuilder.showMessageWithOK(this, "提示", "未开始采集，无法打点");
                }
            }
        });

        // TODO: 2022/7/13 设置其他组件的监听器
    }

}