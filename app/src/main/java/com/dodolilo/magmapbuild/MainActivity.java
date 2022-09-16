package com.dodolilo.magmapbuild;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    /**
     * UI组件声明
     */
    private EditText edtPersonId;
    private EditText edtSampNum;
    private EditText edtServerIP;
    private EditText edtServerPort;
    private Button btMinusPersonId;
    private Button btAddPersonId;
    private Button btMinusSampNum;
    private Button btAddSampNum;
    private Button btStartSampling;
    private Button btMarkPoint;
    private Spinner spinnerSelectPoint;

    /**
     * 数据采集类对象
     */
    private SensorsBee sensorsBee;

    /**
     * 保持传感器数据的共享容器引用.
     */
    private StringBuilder sensorsData;

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
    private String fileNameMark;

    /**
     * 定位系统服务器ip地址.
     */
    private String serverIP = "10.255.18.240";

    /**
     * 定位系统服务器端口号.
     */
    private int serverPort = 9090;

    /**
     * 本APP作为客户端的socket连接.初始为未连接的socket
     */
    private Socket clientSocket = null;

    private SentDataBySocket dataSentor = null;

    /**
     * 建立socket连接的时间上限，单位（ms）
     */
    private static final int CONNECT_TIME_OUT = 3000;

    /**
     * 数据发送延迟，单位（ms）
     */
    private static final int DATA_SENTING_DELAY = 1000;

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
        edtPersonId = findViewById(R.id.edtPersonID);
        edtSampNum = findViewById(R.id.edtSampNum);
        edtServerIP = findViewById(R.id.edtServerIP);
        edtServerPort = findViewById(R.id.edtServerPort);
        btMinusPersonId = findViewById(R.id.btMinusPersonID);
        btAddPersonId = findViewById(R.id.btAddPersonID);
        btMinusSampNum = findViewById(R.id.btMinusSampNum);
        btAddSampNum = findViewById(R.id.btAddSampNum);
        btStartSampling = findViewById(R.id.btStartSampling);
        btMarkPoint = findViewById(R.id.btMarkPoint);
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
     * <p>
     * 此回调是 Activity 接收的最后一个回调。
     * 通常，实现 onDestroy() 是为了确保
     * 在销毁 Activity 或包含该 Activity 的进程时释放该 Activity 的所有资源。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorsBee.stopSensorRecord();
        if (dataSentor != null) {
            dataSentor.finishSentData();
        }
        // TODO: 2022/7/11 在app死掉前，1、将sensorBee中的上次序号保存到文件，
    }

    private void setComponentsListeners() {
        //设置IP、PORT输入框的监听器
        edtServerIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO: 2022/9/14 检查ip的合法性
                serverIP = s.toString();
            }
        });
        edtServerPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO: 2022/9/14 检查port的合法性 
                serverPort = Integer.parseInt(s.toString());
            }
        });


        //设置“开始采集”、“停止采集”按钮的监听器
        btStartSampling.setOnClickListener(v -> {
            if (sensorsBee.isRecording()) {
                //停止采数，停止发送程序，保存打点文件，将按钮文本改为”开始采数“，将按钮颜色改为绿色
                sensorsBee.stopSensorRecord();
                if (dataSentor != null) {
                    dataSentor.finishSentData();
                }

                String pointMarkFileName = fileNameMark.concat("_points");
                CsvDataTools.saveCsvToExternalStorage(pointMarkFileName, CsvDataTools.FileSaveType.CSV, pointMarkStrBd.toString(), this);

                btStartSampling.setText(R.string.button_start_record);
                btStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.start_green));
            } else {
                //开始采数，将按钮文本改为”停止采数“，将按钮颜色改为红色
                fileNameMark = "TEST_".concat(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINA).format(new Date()));
                pointMarkStrBd = new StringBuilder();
                sensorsData = new StringBuilder();  //将共享数据容器DI到sensorsBee与dataSentor中

                if (!sensorsBee.startSensorRecord(fileNameMark.concat("_sensors"), sensorsData)) {
                    MessageBuilder.showMessageWithOK(this, "SensorsBee Start Error", "SensorsBee Start Failed");
                    return;
                }
                //如果是在socket连接成功的情况下，则启动数据发送，否则不管
                dataSentor = SentDataBySocket.sentDataWithFixedDelay(serverIP, serverPort, sensorsData, DATA_SENTING_DELAY, DATA_SENTING_DELAY, this);
                try {
                    dataSentor.startSentData();
                } catch (IOException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                btStartSampling.setText(R.string.button_stop_record);
                btStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.stop_red));
            }
        });

        //动态创建下拉列表，并注册监听器
        ArrayList<String> pointNameList = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : pointsMap.entrySet()) {
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
        btMarkPoint.setOnClickListener(v -> {
            if (selectedPointName == null) {
                MessageBuilder.showMessageWithOK(this, "提示", "未选择点");
            } else {
                // 打点逻辑
                if (sensorsBee.isRecording()) {
                    float[] pointXY = pointsMap.get(selectedPointName);
                    pointMarkStrBd.append(CsvDataTools.convertPointToCsvFormat(pointXY));
                    Toast.makeText(
                            this,
                            "打点 " + selectedPointName + "：" + pointXY[0] + ", " + pointXY[1],
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    MessageBuilder.showMessageWithOK(this, "提示", "未开始采集，无法打点");
                }
            }
        });

        // TODO: 2022/7/13 设置其他组件的监听器
    }

}