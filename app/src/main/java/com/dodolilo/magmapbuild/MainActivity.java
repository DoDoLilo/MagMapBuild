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
     * 定位系统服务器ip地址.
     */
    private String serverIP = "10.254.7.16";

    /**
     * 定位系统服务器端口号.
     */
    private int serverPort = 2212;

    /**
     * 用户身份标志：手机号.
     */
    private String userPhone = "17369798843";

    private CollectSendSensorsData collectSendSensorsData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1.获取UI组件引用
        edtServerIP = findViewById(R.id.edtServerIP);
        edtServerPort = findViewById(R.id.edtServerPort);
        btStartSampling = findViewById(R.id.btStartSampling);

        //2.构造采数类实例，context即这个Activity对象本身
        collectSendSensorsData = CollectSendSensorsData.getSingleInstance(this, serverIP, serverPort, userPhone);

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
        //停止开启的线程
        collectSendSensorsData.leavingTheRoom();
        //最后再调用super的
        super.onDestroy();
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
                if (s.length() != 0) {
                    serverPort = Integer.parseInt(s.toString());
                }
            }
        });


        //设置“开始采集”、“停止采集”按钮的监听器
        btStartSampling.setOnClickListener(v -> {
            if (collectSendSensorsData.isInTheRoom()) {
                //离开机房：停止数据采集与发送，将按钮文本改为”开始采数“，将按钮颜色改为绿色
                collectSendSensorsData.leavingTheRoom();

                btStartSampling.setText(R.string.button_start_record);
                btStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.start_green));
            } else {
                //进入机房：更新参数后开始采数与发送，将按钮文本改为”停止采数“，若启动成功，则按钮颜色变为红色
                collectSendSensorsData.setServerIP(serverIP);
                collectSendSensorsData.setServerPort(serverPort);
                collectSendSensorsData.setUserPhone(userPhone);
                collectSendSensorsData.enteringTheRoom();

                btStartSampling.setText(R.string.button_stop_record);
                btStartSampling.setBackgroundColor(ContextCompat.getColor(this, R.color.stop_red));
            }
        });
    }

}