package com.dodolilo.magmapbuild;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    /**
     * UI组件声明
     */
    private EditText edtServerIP;
    private EditText edtServerPort;
    private Button btStartSampling;

    /**
     * 数据采集类对象
     */
    private SensorsBee sensorsBee;

    /**
     * 保持传感器数据的共享容器引用.
     */
    private StringBuilder sensorsData;

    /**
     * 定位系统服务器ip地址.
     */
    private String serverIP = "10.254.7.9";

    /**
     * 定位系统服务器端口号.
     */
    private int serverPort = 2212;

    private SentDataBySocket dataSentor = null;

    /**
     * 数据发送延迟，单位（ms）
     */
    private static final int DATA_SENTING_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1.获取UI组件引用
        edtServerIP = findViewById(R.id.edtServerIP);
        edtServerPort = findViewById(R.id.edtServerPort);
        btStartSampling = findViewById(R.id.btStartSampling);

        //2.构造采数类实例，context即这个Activity对象本身
        sensorsBee = new SensorsBee(this);

        //3.注册UI组件监听器
        setComponentsListeners();
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
                if (s.length() != 0) {
                    serverIP = s.toString();
                }
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
                if (s.length() != 0) {
                    serverPort = Integer.parseInt(s.toString());
                }
            }
        });


        //设置“开始采集”、“停止采集”按钮的监听器
        btStartSampling.setOnClickListener(v -> {
            if (sensorsBee.isRecording()) {
                //停止采数，停止发送程序，保存打点文件，将按钮文本改为”开始采数“，将按钮颜色改为绿色
                sensorsBee.stopSensorRecord(); //先停止IMU数据采集
                if (dataSentor != null) {
                    dataSentor.finishSentData(); //再停止Socket发送
                }

                btStartSampling.setText(R.string.button_start_record);
                btStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.start_green));
            } else {
                //开始采数，将按钮文本改为”停止采数“，将按钮颜色改为红色
                sensorsData = new StringBuilder();  //将共享数据容器DI到sensorsBee与dataSentor中

                if (!sensorsBee.startSensorRecord(sensorsData)) {
                    return;
                }
                //启动数据发送
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
    }

}