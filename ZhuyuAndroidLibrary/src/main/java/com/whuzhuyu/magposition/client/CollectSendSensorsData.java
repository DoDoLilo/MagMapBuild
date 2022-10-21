package com.whuzhuyu.magposition.client;

import android.content.Context;

import java.security.InvalidParameterException;

/**
 * 采集并发送数据，封装SensorBee与SentDataBySocket.
 * 使用后必须在Android的onDestroy中调用leavingTheRoom()方法来停止工作.
 * 使用单例模式，避免在重复new CollectSendSensorsData，造成线程方法无法结束，占用服务器唯一通信链接.
 */
public class CollectSendSensorsData {
    /**
     * 单例模式，唯一实例.
     */
    private static CollectSendSensorsData singleInstance = new CollectSendSensorsData();

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
     * 该单例最新的状态：false 不处于可定位场景中；true 处于预设场景中
     */
    private boolean inTheRoom = false;


    /**
     * 返回该类单例，同时强制初始化参数.后续参数变化可用setter改变.
     *
     * @param context    上下文
     * @param serverIP   服务器ip
     * @param serverPort 服务器端口号
     * @param userPhone  当前用户手机号
     * @throws InvalidParameterException 当传入的参数为null或为空字符串时，抛出该异常
     */
    public static CollectSendSensorsData getSingleInstance(Context context, String serverIP, int serverPort, String userPhone) throws InvalidParameterException {
        //在获取“新”实例前，停止“旧”实例的所有线程
        singleInstance.leavingTheRoom();
        //更新单例属性
        singleInstance.setContext(context);
        singleInstance.setServerIP(serverIP);
        singleInstance.setServerPort(serverPort);
        singleInstance.setUserPhone(userPhone);
        singleInstance.setSensorsBee(new SensorsBee(context));
        //返回该单例
        return singleInstance;
    }

    /**
     * 单例模式，私有构造函数，防止外部获取新的实例.
     */
    private CollectSendSensorsData() {
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
        inTheRoom = true;
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
        inTheRoom = false;
    }

    public boolean isInTheRoom() {
        return inTheRoom;
    }


    //-------------------------getters and setters -----------------------------------------
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) throws InvalidParameterException {
        if (context == null) {
            throw new InvalidParameterException("Param context is null");
        }
        this.context = context;
    }

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

    public void setSensorsBee(SensorsBee sensorsBee) throws InvalidParameterException {
        if (sensorsBee == null) {
            throw new InvalidParameterException("Param sensorsBee is null");
        }
        this.sensorsBee = sensorsBee;
    }
}
