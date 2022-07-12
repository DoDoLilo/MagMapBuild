package com.dodolilo.magmapbuild;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

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
    private Switch switchResetSensors;
    private Button buttonStartSampling;

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
    private final String internalStorageCacheFileName = "personIdAndSampNumCache.csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化
        //1.获取UI组件引用
        editTextPersonId = findViewById(R.id.editTextPersonID);
        editTextSampNum = findViewById(R.id.editTextSampNum);
        buttonMinusPersonId = findViewById(R.id.buttonMinusPersonID);
        buttonAddPersonId = findViewById(R.id.buttonAddPersonID);
        buttonMinusSampNum = findViewById(R.id.buttonMinusSampNum);
        buttonAddSampNum = findViewById(R.id.buttonAddSampNum);
        switchResetSensors = findViewById(R.id.switchResetSensors);
        buttonStartSampling = findViewById(R.id.buttonStartSampling);

        //2.注册UI组件监听器
        // TODO: 2022/7/12 检查新ID和Num是否和上次的一样，提示用户是否继续采集

        //3.从应用内部存储空间中获取lastPersonId和lastSampNum

        //4.采数类对象，context即这个Activity对象本身
        sensorsBee = new SensorsBee(this);
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
        // TODO: 2022/7/11 在app死掉前，1、将sensorBee中的上次序号保存到文件，2、执行注销传感器的工作，否则后台传感器一直运行。
    }
}