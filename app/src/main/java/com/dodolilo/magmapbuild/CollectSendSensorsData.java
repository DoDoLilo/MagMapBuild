package com.dodolilo.magmapbuild;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.TreeMap;

/**
 * 采集并发送数据，封装SensorBee与SentDataBySocket.
 * 使用后必须在Android的onDestroy中调用leavingTheRoom()方法来停止工作.
 * 使用单例模式，避免在重复new CollectSendSensorsData，造成线程方法无法结束，占用服务器唯一通信链接.
 * 虽然使用了单例模式，但仍无法解决在obj = new CollectSendSensorsData调用enteringRoom()后未调用leavingRoom()将obj置为null，
 * 此时线程根本无法结束，就算有单例模式，所以不允许出现这种使用。
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
    private String serverIP = "10.62.41.45";

    /**
     * 服务器端口号
     */
    private int serverPort = 2212;

    /**
     * 当前用户手机号码
     */
    private String userPhone = "123456789";

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

    public static CollectSendSensorsData getSingleInstance(Context context) {
        singleInstance.setContext(context);
        singleInstance.setSensorsBee(new SensorsBee(context));
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
        //单例模式，在对唯一的对象启动线程时，先调用leavingTheRoom();
        //此时就不会出现一直占用服务器唯一的socket连接的情况了！因为在下一次使用前，保证了旧线程的死亡
        leavingTheRoom();
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

    /**
     * 测试是否能连接上服务器.
     *
     * @param activity 显示UI提醒的activity的实例引用
     */
    private void pretestConnection(Activity activity) {
        new Thread(() -> {
            Socket socket = null;
            Boolean connectSucceed = false;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIP, serverPort), 3000);
                socket.sendUrgentData(0xFF);
                connectSucceed = true;

            } catch (IOException e) {
                connectSucceed = false;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (connectSucceed) {
                    activity.runOnUiThread(() -> MessageBuilder.showMessageWithOK(context, "测试连接", "连接成功"));
//                    activity.runOnUiThread(()-> Toast.makeText(context, "测试连接成功！", Toast.LENGTH_LONG).show());
                } else {
                    activity.runOnUiThread(() -> MessageBuilder.showMessageWithOK(context, "测试连接", "连接失败"));
//                    activity.runOnUiThread(()-> Toast.makeText(context, "测试连接失败！", Toast.LENGTH_LONG).show());
                }
            }
        }).start();
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
