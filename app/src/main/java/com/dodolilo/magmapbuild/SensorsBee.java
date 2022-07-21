package com.dodolilo.magmapbuild;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.util.Log;

/**
 * 一个实现传感器检查、注册、采集、注销、保存文件功能的类.
 * 需要使用加速度计、陀螺仪、磁力计、方向四元数这4个传感器.
 */
public class SensorsBee {
    /**
     * 外部提供的上下文.
     */
    private final Context context;

    /**
     * 打印Log信息的标签.
     */
    private final String TAG = "SensorsBee";

    /**
     * 传感器精度含义数组.
     */
    private final String[] SENSOR_STATUS_ACCURACY = new String[]{
            "SENSOR_STATUS_NO_CONTACT",
            "SENSOR_STATUS_UNRELIABLE",
            "SENSOR_STATUS_ACCURACY_LOW",
            "SENSOR_STATUS_ACCURACY_MEDIUM",
            "SENSOR_STATUS_ACCURACY_HIGH"
    };

    /**
     * 数据采集循环的运行标志.
     */
    private enum BeeStates {
        SENSOR_READING,
        STOP_READING
    }

    private BeeStates loopState = BeeStates.STOP_READING;

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

    /**
     * 为什么这个成员对象可以声明为final？因为它在构造函数中初始化了.
     * "final修饰成员变量，该成员变量必须在创建对象之前（构造函数执行结束之前）进行赋值，否则编译失败".
     */
    private final SensorManager sensorManager;

    /**
     * 加速度计 TYPE_ACCELEROMETER
     */
    private Sensor accSensor;
    private final float[] accValues = new float[3];
    private final SensorEventListener accSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            accValues[0] = event.values[0];
            accValues[1] = event.values[1];
            accValues[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG, "Accuracy of acc changed into ".concat(SENSOR_STATUS_ACCURACY[accuracy + 1]));
        }
    };


    /**
     * 陀螺仪 TYPE_GYROSCOPE
     */
    private Sensor gyroSensor;
    private final float[] gyroValues = new float[3];
    private final SensorEventListener gyroSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            gyroValues[0] = event.values[0];
            gyroValues[1] = event.values[1];
            gyroValues[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG, "Accuracy of gyro changed into ".concat(SENSOR_STATUS_ACCURACY[accuracy + 1]));
        }
    };


    /**
     * 磁力计 TYPE_MAGNETIC_FIELD
     */
    private Sensor magSensor;
    private final float[] magValues = new float[3];
    private final SensorEventListener magSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            magValues[0] = event.values[0];
            magValues[1] = event.values[1];
            magValues[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG, "Accuracy of mag changed into ".concat(SENSOR_STATUS_ACCURACY[accuracy + 1]));
        }
    };

    /**
     * 方向四元数 TYPE_GAME_ROTATION_VECTOR
     */
    private Sensor quatSensor;
    private final float[] quatValues = new float[4];
    private final SensorEventListener quatSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            quatValues[0] = event.values[0];
            quatValues[1] = event.values[1];
            quatValues[2] = event.values[2];
            quatValues[3] = event.values[3];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG, "Accuracy of quat changed into ".concat(SENSOR_STATUS_ACCURACY[accuracy + 1]));
        }
    };

    /**
     * 供采数线程写入所有的传感器数据，以“,”间隔.
     * 不涉及多个线程同时写，所以不需要使用StringBuffer.
     */
    private final StringBuilder allSensorsValuesBuffer = new StringBuilder();

    /**
     * 构造器，依赖注入context.
     *
     * @param context 上下文
     */
    public SensorsBee(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * 检查所需的所有传感器的可用性.
     * @return false 如果任何一个传感器不可用.
     */
    private boolean initSensorsAndCheckAvailable() {
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        quatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        //检查该手机的传感器是否可用，如存在不可用的，则初始化失败，弹出提示
        StringBuilder lackedSensorsMsg = new StringBuilder();
        lackedSensorsMsg.append(accSensor == null ? "No Accelerometer!\n" : "");
        lackedSensorsMsg.append(gyroSensor == null ? "No Gyroscope!\n" : "");
        lackedSensorsMsg.append(magSensor == null ? "No Magnetic Field!\n" : "");
        lackedSensorsMsg.append(quatSensor == null ? "No Game Rotation Vector!\n" : "");
        if (lackedSensorsMsg.length() != 0) {
            MessageBuilder.showMessageWithOK(context, "Init Failed", lackedSensorsMsg.toString());
            return false;
        }

        return true;
    }

    
    /**
     * 供外部启动传感器进行数据采集.
     * 调用此方法会启动数据采集线程，并且将循环状态置为Reading.
     *
     * @return false 如果任何一个传感器启动or注册失败.
     */
    public boolean startSensorRecord(String fileSaveName) {
        //1.重新获取传感器对象引用检查传感器是否可用，注册传感器
        if (!initSensorsAndCheckAvailable() || !registerSensors()) {
            return false;
        }

        //2.启动采数线程
        loopState = BeeStates.SENSOR_READING;
        new Thread(() -> {
            //实际runnable执行代码块，每隔5ms从sensorValues获取数据存到buffer中
            while (loopState == BeeStates.SENSOR_READING) {
                //NOTE：这里的写buffer并非原子写，尽可能写入最新的传感器数据与时间戳
                allSensorsValuesBuffer.append(CsvDataTools.convertSensorValuesToCsvFormat(
                        accValues, gyroValues, magValues, quatValues
                ));

                try {
                    Thread.sleep(SAMPLEING_THREAD_SLEEP_MS);
                } catch (InterruptedException e) {
                    Log.i(TAG, "线程睡眠被打断！");
                    e.printStackTrace();
                }
            }
            //3.结束采集，将传感器缓存数据存储到手机内存空间中，为避免阻塞，直接利用这个Thread
            Looper.prepare();
            CsvDataTools.saveCsvToExternalStorage(fileSaveName, allSensorsValuesBuffer.toString(), context);
            Looper.loop();
        }).start();

        return true;
    }

    /**
     * 供外部停止传感器数据采集.
     */
    public void stopSensorRecord() {
        loopState = BeeStates.STOP_READING;
        unregisterSensors();
    }

    /**
     * 供外部重置传感器.
     * 注销、重新注册传感器.
     *
     * @return false 如果任何一个传感器重置失败.
     */
    public boolean resetSensors() {
        unregisterSensors();
        return registerSensors();
    }

    /**
     * @return true 如果该对象正在数据采集
     */
    public boolean isRecording() {
        return loopState == BeeStates.SENSOR_READING;
    }

    /**
     * 注册所有传感器.
     *
     * @return false 如果任何一个传感器注册失败.
     */
    private boolean registerSensors() {
        // TODO: 2022/7/20 增加读取传感器型号. 
        StringBuilder registerFailedMsg = new StringBuilder();
        if (!sensorManager.registerListener(accSensorListener, accSensor, SAMPLING_PERIOD_US)) {
            registerFailedMsg.append("Acceleromenter Register Failed!\n");
        }
        if (!sensorManager.registerListener(gyroSensorListener, gyroSensor, SAMPLING_PERIOD_US)) {
            registerFailedMsg.append("Gyroscope Register Failed!\n");
        }
        if (!sensorManager.registerListener(magSensorListener, magSensor, SAMPLING_PERIOD_US)) {
            registerFailedMsg.append("MagneticField Register Failed!\n");
        }
        if (!sensorManager.registerListener(quatSensorListener, quatSensor, SAMPLING_PERIOD_US)) {
            registerFailedMsg.append("GameRotationVector Register Failed!\n");
        }
        if (registerFailedMsg.length() != 0) {
            MessageBuilder.showMessageWithOK(context, "Init Failed", registerFailedMsg.toString());
            return false;
        }

        return true;
    }

    /**
     * 注销所有传感器，只能返回void
     */
    private void unregisterSensors() {
        sensorManager.unregisterListener(accSensorListener, accSensor);
        sensorManager.unregisterListener(gyroSensorListener, gyroSensor);
        sensorManager.unregisterListener(magSensorListener, magSensor);
        sensorManager.unregisterListener(quatSensorListener, quatSensor);
    }


}
