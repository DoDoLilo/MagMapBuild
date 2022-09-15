package com.dodolilo.magmapbuild;

import net.jcip.annotations.NotThreadSafe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * 管理向服务器发送数据的类.
 * 使用静态构造工厂控制其使用（使名字更容易理解）.
 */
@NotThreadSafe
public class SentDataBySocket{
    //发送数据的socket连接.
    private Socket socket = null;

    /**
     * 发送数据的内容.需要传入同步了的数据结构的引用？
     * 不需要同步？因为服务器是readLine()，而data中的数据是只增长的，不同步也行.
     */
    private StringBuilder dataToSent = null;

    private long initalDalay = 0;

    private long delay = 0;

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

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setDataToSent(StringBuilder dataToSent) {
        this.dataToSent = dataToSent;
    }

    public void setInitalDalay(long initalDelay) {
        this.initalDalay = initalDelay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    private SentDataBySocket() {}

    /**
     * 数据发送类的静态工厂.模仿ScheduledExecutorService.scheduleWithFixedDelay()方法.
     * 初始等待时间后，每隔一段固定的时间，会发送data容器中增加的数据.
     * @param socket  发送数据的socket连接
     * @param dataToSent  发送的数据容器的引用，其中的数据只增长！
     * @param initalDalay  初始等待时间（ms）
     * @param delay  延期性执行任务的延期时间（ms），注意区别period
     * @return  SentDataBySocket实例引用
     */
    public static SentDataBySocket sentDataWithFixedDelay(
            Socket socket,
            StringBuilder dataToSent,
            long initalDalay,
            long delay
    ) {
        SentDataBySocket sd = new SentDataBySocket();
        sd.setSocket(socket);
        sd.setDataToSent(dataToSent);
        sd.setInitalDalay(initalDalay);
        sd.setDelay(delay);
        return sd;
    }

    /**
     * 开启数据发送线程.并不阻止采数程序的继续运行.
     */
    public void startSentData() throws IOException{
        //抛出异常到外面，不使用context
        if (state == DataSentState.SENTING_DATA) {
            throw new IOException("正在发送数据，不要重复启动...");
        }
        if (socket == null || dataToSent == null) {
            throw new IOException("无法发送数据...");
        }

        //启动子线程
        new Thread(()->{
            //对子线程，延迟initalDalay时间
            try {
                Thread.sleep(initalDalay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //开始发送dataToSent里的数据，
            try (BufferedWriter bfWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                state = DataSentState.SENTING_DATA;
                int lastIndex = 0;
                while (state == DataSentState.SENTING_DATA) {
                    int nextIndex = dataToSent.length(); //提前记录，不要多次调用.length()
                    bfWriter.write(dataToSent.substring(lastIndex, nextIndex));
                    lastIndex = nextIndex;
                    Thread.sleep(delay);
                }
                //离开循环，先将剩余的数据发出去
                bfWriter.write(dataToSent.substring(lastIndex, dataToSent.length()));
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
        }).start();
    }

    //结束发送数据.
    public void finishSentData() {
        state = DataSentState.FINISHED_SENT;
    }

}
