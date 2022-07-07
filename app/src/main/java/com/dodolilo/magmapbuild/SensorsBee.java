package com.dodolilo.magmapbuild;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * 一个实现传感器检查、注册、采集、注销、保存文件功能的类.
 * 需要使用加速度计、陀螺仪、磁力计、方向四元数这4个传感器.
 */
public class SensorsBee {
    private Context context;

    /**
     * 采样频率： 200 (Hz).
     */
    private final int SAMPLEING_FREQUENCY = 200;

    /**
     * 采样线程睡眠时间 = 1000 / 200 (ms) = 5 (ms)
     */
    private final long SAMPLEING_THREAD_SLEEP_MS = 1000 / SAMPLEING_FREQUENCY;

    /**
     * 传感器采样延迟 = 1,000,000 / 200 (us) = 5000 (us)
     */
    private final int SAMPLING_PERIOD_US = 1000000 / SAMPLEING_FREQUENCY;

    private SensorManager sensorManager;

    /**
     * 加速度计
     */
    private Sensor accSensor;
    private SensorEventListener accSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 陀螺仪
     */
    private Sensor gyroSensor;
    private SensorEventListener gyroSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 磁力计
     */
    private Sensor magSensor;
    private SensorEventListener magSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 方向四元数
     */
    private Sensor quatSensor;
    private SensorEventListener quatSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public SensorsBee(Context context) {
        this.context = context;

    }

    /**
     * 初始化对应Sensor对象，检查所需的所有传感器的可用性.
     */
    private boolean checkAndInitSensors() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        quatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);

        //检查该手机的传感器是否可用，如存在不可用的，则初始化失败，弹出提示
        StringBuilder lackedSensorsMessage = new StringBuilder();
        lackedSensorsMessage.append(accSensor == null ? "No Accelerometer!\n" : "");
        lackedSensorsMessage.append(gyroSensor == null ? "No Gyroscope!\n" : "");
        lackedSensorsMessage.append(magSensor == null ? "No Magnetic Field!\n" : "");
        lackedSensorsMessage.append(quatSensor == null ? "No Game Rotation Vector!\n" : "");
        if (lackedSensorsMessage.length() != 0) {
            MessageBuilder.showMessage(context, "Init Failed", lackedSensorsMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * 供外部启动传感器进行数据采集.
     * @return false 如果任何一个传感器启动失败.
     */
    public boolean startSensorRecord() {

        return true;
    }

    /**
     * 供外部停止传感器数据采集.
     * @return false 如果任何一个传感器停止失败.
     */
    public boolean stopSensorRecord() {

        return true;
    }

    /**
     * 供外部重置传感器.
     * 注销、重新注册传感器.
     * @return false 如果任何一个传感器重置失败.
     */
    public boolean resetSensors() {

        return true;
    }

    /**
     * 注册所有传感器.
     * @return false 如果任何一个传感器注册失败.
     */
    private boolean registerSensors() {
        StringBuilder registerFailedMessage = new StringBuilder();
        if (!sensorManager.registerListener(accSensorListener, accSensor, SAMPLING_PERIOD_US)) {
            registerFailedMessage.append("Acceleromenter Register Failed!\n");
        }
        if (!sensorManager.registerListener(gyroSensorListener, gyroSensor, SAMPLING_PERIOD_US)) {
            registerFailedMessage.append("Gyroscope Register Failed!\n");
        }
        if (!sensorManager.registerListener(magSensorListener, magSensor, SAMPLING_PERIOD_US)) {
            registerFailedMessage.append("MagneticField Register Failed!\n");
        }
        if (!sensorManager.registerListener(quatSensorListener, quatSensor, SAMPLING_PERIOD_US)) {
            registerFailedMessage.append("GameRotationVector Register Failed!\n");
        }
        if (registerFailedMessage.length() != 0) {
            MessageBuilder.showMessage(context, "Init Failed", registerFailedMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * 注销所有传感器.
     * @return false 如果任何一个传感器注销失败.
     */
    private boolean unregisterSensors() {

        return true;
    }
}
