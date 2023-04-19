package com.dodolilo.magmapbuild;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidParameterException;

/**
 * 管理向服务器发送数据的类.
 * 使用静态构造工厂控制其使用（使名字更容易理解）.
 */
class SentDataBySocket {
    //BufferedWriter缓冲区大小: 160char/行 * max200行/秒 * 60秒
    private static final int BUFFER_SIZE = 160 * 200 * 60;

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

    private long delay = 500;

    private Context context;

    private Activity activity;

    private Socket socket = null;

    private Toast toast = null;

    /**
     * 建立socket连接的时间上限，单位（ms）
     */
    private static final int CONNECT_TIME_OUT = 2000;
    
    private static final int SERVER_RESPONE_TIME_OUT = 2000;

    private static final String SERVER_RESPONSE = "MMPS";

    /**
     * 表示本数据传输类的当前数据传输状态.
     * 其状态变换原因可能为：外部主动启动、停止数据传输，传输数据时发生异常...
     */
    private enum DataSentState {
        SENTING_DATA,
        FINISHED_SENT
    }

    private DataSentState state = DataSentState.FINISHED_SENT;

    public void setDataToSent(StringBuilder dataToSent) throws InvalidParameterException {
        if (dataToSent == null) {
            throw new InvalidParameterException("Param dataToSent is null");
        }
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
        this.activity = (Activity) context;
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
    ) throws InvalidParameterException {
        SentDataBySocket sd = new SentDataBySocket();
        sd.setServerIP(serverIP);
        sd.setPort(port);
        sd.setDataToSent(dataToSent);
        sd.setInitalDalay(initalDalay);
        sd.setDelay(delay);
        sd.setContext(context);
        return sd;
    }

    public static SentDataBySocket sentDataWithFixedDelay(
            String serverIP,
            int port,
            StringBuilder dataToSent,
            Context context
    ) throws InvalidParameterException {
        SentDataBySocket sd = new SentDataBySocket();
        sd.setServerIP(serverIP);
        sd.setPort(port);
        sd.setDataToSent(dataToSent);
        sd.setContext(context);
        return sd;
    }


    /**
     * 开启数据发送线程.并不阻止采数程序的继续运行.
     * 当无法发送数据时，让外界知晓。
     */
    public void startSentData() {
        if (state == DataSentState.SENTING_DATA) {
            Log.e("startSentData()", "已有线程正在发送数据，不要重复启动，只允许一个发送数据的线程.");
            return;
        }

        //NOTE：这句状态量的设置不要放在线程中！否则，如果用户点击按钮很快，会导致状态变化未按预期顺序发生！
        state = DataSentState.SENTING_DATA;

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
            socket = null;

            while (state == DataSentState.SENTING_DATA) {
                //使用额外变量记录是否连接成功，避免close()失败导致isClosed()错误
                boolean connect_succeed = false;
                BufferedReader bfReader = null;
                //连接socket，这里不使用finally或try-with-resources是因为该socket后面还要用
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(serverIP, port), CONNECT_TIME_OUT);

                    //开启IO input流，等待服务器响应，如果超过时间未响应，则认为连接失败！
                    socket.setSoTimeout(SERVER_RESPONE_TIME_OUT);
                    bfReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String serverRespone = bfReader.readLine();

                    if (serverRespone == null) {
                        throw new IOException("Null response, wrong connection.");
                    }

                    if (SERVER_RESPONSE.equals(serverRespone)) {
                        connect_succeed = true;
                        activity.runOnUiThread(() -> Toast.makeText(context, "服务器响应成功", Toast.LENGTH_SHORT).show());
                    }

                } catch (IOException e) {
                    connect_succeed = false;
                    if (socket != null && socket.isConnected()) {
                        activity.runOnUiThread(() -> Toast.makeText(context, "服务器超时未响应", Toast.LENGTH_SHORT).show());
                    } else {
                        activity.runOnUiThread(() -> Toast.makeText(context, "服务器连接失败", Toast.LENGTH_SHORT).show());
                    }

                    e.printStackTrace();
                    try {
                        if (bfReader != null) {
                            bfReader.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                //连接socket和进入该代码块分离，可能导致无法进入该代码块中的socket.close()!
                //所以需要在最后额外增加socket.close()！
                if (state == DataSentState.SENTING_DATA && socket != null && socket.isConnected() && !socket.isClosed() && connect_succeed) {
                    //如果没有“离开机房” 且 socket连接成功，则尝试发送数据
                    //socket连接成功、sendUrgentData没异常，也不能代表可以发送了
                    try (BufferedWriter bfWriter = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream()), BUFFER_SIZE)) {
                        while (state == DataSentState.SENTING_DATA) {
                            socket.sendUrgentData(0xFF); //测试是否还能连上服务器
                            int nextIndex = dataToSent.length(); //提前记录，不要多次调用.length()
                            bfWriter.write(dataToSent.substring(lastIndex, nextIndex));
                            bfWriter.flush();
                            Thread.sleep(delay);
                            //认为数据发生成功了，认为lastIndex前的数据都成功发送出去了
                            lastIndex = nextIndex;
                        }
                        //离开机房、离开循环，先将剩余的数据发出去，再发送一行END标识符
                        //这里睡一段时间，以保证先结束的SensorBee写入数据到dataToSent
                        Thread.sleep(delay);
                        bfWriter.write(dataToSent.substring(lastIndex, dataToSent.length()));
                        bfWriter.flush();
                        bfWriter.write("END\n");
                        bfWriter.flush();
                    } catch (Exception e) {
                        //出现意外，断开连接，将状态置为SOCKET_EXCEPTION，好让外部知晓.
                        Log.e("Socket Error", "connection failed...");
                        activity.runOnUiThread(() -> Toast.makeText(context, "服务器连接断开", Toast.LENGTH_SHORT).show());
                        e.printStackTrace();
                    } finally {
                        //最后断开连接
                        try {
                            if (bfReader != null) {
                                bfReader.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                //过2.5s后再重连
                if (state == DataSentState.SENTING_DATA) {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //退出循环
            if (socket != null) {
                try {
                    socket.close();
                    activity.runOnUiThread(() -> Toast.makeText(context, "服务器连接结束", Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //结束发送数据.
    public void finishSentData() {
        state = DataSentState.FINISHED_SENT;
    }



}
