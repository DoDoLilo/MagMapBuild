package com.dodolilo.magmapbuild;

import android.content.Context;

import java.security.InvalidParameterException;

/**
 * 采集并发送数据，封装SensorBee与SentDataBySocket.
 * 使用后必须在Android的onDestroy中调用leavingTheRoom()方法来停止工作.
 * 使用时将要将该对象声明为全局成员变量，并且不允许在没有调用该对象实例.leavingTheRoom()时，将该对象变为无引用对象！
 * 因为JVM的垃圾回收机制是未知的，所以该对象实例启动的线程可能会在其变为无引用对象后仍继续运行！
 */
public class CollectSendSensorsData {
    private Context context;

    /**
     * 服务器IP地址
     */
    private String serverIP;

    /**
     * 服务器端口号
     */
    private int serverPort;

    /**
     * 当前用户手机号码
     */
    private String userPhone;

    /**
     * 数据采集对象
     */
    private SensorsBee sensorsBee;

    /**
     * 数据发送对象
     */
    private SentDataBySocket dataSentor;


    /**
     * @param context    上下文
     * @param serverIP   服务器ip
     * @param serverPort 服务器端口号
     * @param userPhone  当前用户手机号
     * @throws InvalidParameterException 当传入的参数为null或为空字符串时，抛出该异常
     */
    public CollectSendSensorsData(Context context, String serverIP, int serverPort, String userPhone) throws InvalidParameterException {
        if (context == null) {
            throw new InvalidParameterException("Param context is null");
        }
        this.context = context;

        if (serverIP == null || serverIP.equals("")) {
            throw new InvalidParameterException("Param serverIP is null or empty");
        }
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        if (userPhone == null || userPhone.equals("")) {
            throw new InvalidParameterException("Param userPhone is null or empty");
        }
        this.userPhone = userPhone;
        sensorsBee = new SensorsBee(context);
    }

    /**
     * 必须与leavingTheRoom()成对使用，否则启动的线程无法关闭.
     * 只能在用户位于机房入口、将要进入机房的时候使用，其它任何情况启动没有意义.
     * 启动传感器数据采集线程、数据发送线程.
     *
     * @return false 传感器采集启动失败（此时重新尝试启动or认为手机传感器不支持）
     */
    public boolean enteringTheRoom() {
        //共享数据缓存，第一行固定为电话号码
        StringBuilder sharedBuffer = new StringBuilder();
        sharedBuffer.append(userPhone.concat("\n"));
        //启动数据采集
        if (!sensorsBee.startSensorRecord(sharedBuffer)) {
            return false;
        }
        //重新声明数据发送实例，启动数据发送
        dataSentor = SentDataBySocket.sentDataWithFixedDelay(serverIP, serverPort, sharedBuffer, context);
        dataSentor.startSentData();
        return true;
    }

    /**
     * 结束传感器数据采集线程、注销传感器，结束数据发送线程.
     */
    public void leavingTheRoom() {
        if (sensorsBee != null) {
            sensorsBee.stopSensorRecord();
        }
        if (dataSentor != null) {
            dataSentor.finishSentData();
        }
    }

    //-------------------------getters and setters ------------------------------------------
    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) throws InvalidParameterException {
        if (serverIP == null || serverIP.equals("")) {
            throw new InvalidParameterException("Param serverIP is null or empty");
        }
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) throws InvalidParameterException {
        this.serverPort = serverPort;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) throws InvalidParameterException {
        if (userPhone == null || userPhone.equals("")) {
            throw new InvalidParameterException("Param userPhone is null or empty");
        }
        this.userPhone = userPhone;
    }
}
