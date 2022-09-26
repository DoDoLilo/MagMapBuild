package com.dodolilo.magmapbuild;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import net.jcip.annotations.NotThreadSafe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * 管理向服务器发送数据的类.
 * 使用静态构造工厂控制其使用（使名字更容易理解）.
 */
@NotThreadSafe
public class SentDataBySocket {
    //BufferedWriter缓冲区大小: 160char/行 * max200行
    private static final int BUFFER_SIZE = 160 * 200;

    /**
     * 发送数据的内容.需要传入同步了的数据结构的引用？
     * 不需要同步？因为服务器是readLine()，而data中的数据是只增长的，不同步也行.
     */
    private StringBuilder dataToSent = null;

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private String serverIP;

    private int port;

    private long initalDalay = 1000;

    private long delay = 1000;

    private Context context;

    /**
     * 建立socket连接的时间上限，单位（ms）
     */
    private static final int CONNECT_TIME_OUT = 3000;

    /**
     * 表示本数据传输类的当前数据传输状态.
     * 其状态变换原因可能为：外部主动启动、停止数据传输，传输数据时发生异常...
     */
    private enum DataSentState {
        SENTING_DATA,
        SOCKET_EXCEPTION,
        FINISHED_SENT
    }

    private DataSentState state;

    public void setDataToSent(StringBuilder dataToSent) {
        this.dataToSent = dataToSent;
    }

    public void setInitalDalay(long initalDelay) {
        this.initalDalay = initalDelay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private SentDataBySocket() {
    }

    /**
     * 数据发送类的静态工厂.模仿ScheduledExecutorService.scheduleWithFixedDelay()方法.
     * 初始等待时间后，每隔一段固定的时间，会发送data容器中增加的数据.
     *
     * @param dataToSent  发送的数据容器的引用，其中的数据只增长！
     * @param initalDalay 初始等待时间（ms）
     * @param delay       延期性执行任务的延期时间（ms），注意区别period
     * @return SentDataBySocket实例引用
     */
    public static SentDataBySocket sentDataWithFixedDelay(
            String serverIP,
            int port,
            StringBuilder dataToSent,
            long initalDalay,
            long delay,
            Context context
    ) {
        SentDataBySocket sd = new SentDataBySocket();
        sd.setServerIP(serverIP);
        sd.setPort(port);
        sd.setDataToSent(dataToSent);
        sd.setInitalDalay(initalDalay);
        sd.setDelay(delay);
        sd.setContext(context);
        return sd;
    }

    /**
     * 开启数据发送线程.并不阻止采数程序的继续运行.
     * 当无法发送数据时，让外界知晓。
     */
    public void startSentData() throws IOException {
        //抛出异常到外面，不使用context
        if (state == DataSentState.SENTING_DATA) {
            throw new IOException("正在发送数据，不要重复启动...");
        }
        if (dataToSent == null) {
            throw new IOException("无法发送数据...");
        }

        //启动子线程
        new Thread(() -> {
            //对子线程，延迟initalDalay时间
            try {
                Thread.sleep(initalDalay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //开始发送dataToSent里的数据，
            int lastIndex = 0;
            Socket socket = null;
            state = DataSentState.SENTING_DATA;
            while (state != DataSentState.FINISHED_SENT) {//这里包一层while循环，当socket连接断开时，不断尝试重新连接
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(serverIP, port), CONNECT_TIME_OUT);
                    state = DataSentState.SENTING_DATA;
                } catch (IOException e) {
                    Log.e("Socket Error", "can't connnect again...");
                    e.printStackTrace();
                }

                try (BufferedWriter bfWriter = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()), BUFFER_SIZE)) {
                    while (state == DataSentState.SENTING_DATA) {
                        int nextIndex = dataToSent.length(); //提前记录，不要多次调用.length()
                        bfWriter.write(dataToSent.substring(lastIndex, nextIndex));
                        bfWriter.flush();
                        lastIndex = nextIndex;
                        Thread.sleep(delay);
                    }
                    //离开机房、离开循环，先将剩余的数据发出去
                    Thread.sleep(delay); //这里睡一段时间，以保证先结束的SensorBee写入数据到dataToSent
                    bfWriter.write(dataToSent.substring(lastIndex, dataToSent.length()));
                    bfWriter.write("END");
                } catch (Exception e) {
                    //出现意外，断开连接，将状态置为SOCKET_EXCEPTION，好让外部知晓.
                    state = DataSentState.SOCKET_EXCEPTION;
                    e.printStackTrace();
                } finally {
                    //最后断开连接
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();
    }

    //结束发送数据.
    public void finishSentData() {
        state = DataSentState.FINISHED_SENT;
    }

}
