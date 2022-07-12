package com.dodolilo.magmapbuild;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;


public class CsvDataTools {

    /**
     * 将多个传感器数据数组转为可供csv文件存储的字符串格式.
     * 注意每个数值之间都需要插入','进行间隔，结尾要用'\n'结束.
     *
     * @param accValues
     * @param gyroValues
     * @param magValues
     * @param quatValues
     * @return
     */
    public static String convertValuesToSavingFormat(
            float[] accValues,
            float[] gyroValues,
            float[] magValues,
            float[] quatValues
    ) {
        StringBuilder oneLineValues = new StringBuilder();

        oneLineValues.append(System.currentTimeMillis());
        for (float data : accValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        for (float data : gyroValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        for (float data : magValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        for (float data : quatValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        oneLineValues.append('\n');

        return oneLineValues.toString();
    }


    /**
     * 向Android<b>应用外部存储空间</b>中保存csv文件.
     *
     * @param csvFileName 文件名，应该以".csv"结尾，不需要外部给外存路径
     * @param csvData     写入csv文件的数据
     * @param context     上下文
     * @return true 如果保存文件成功
     */
    public static boolean saveCsvToExternalStorage(String csvFileName, StringBuilder csvData, Context context) {
        //检查文件名是否以“.csv"结尾
        if (!csvFileName.endsWith(".csv")) {
            csvFileName = csvFileName.concat(".csv");
        }

        //检查外存是否可写
        if (!isExternalStorageWritable()) {
            MessageBuilder.showMessage(context, "Write Error", "External Storage Not Writable!");
            return false;
        }

        //写文件，选择外部存储cache文件夹
        File externalCacheFile = new File(context.getExternalCacheDir(), csvFileName);
        try (OutputStream os = new FileOutputStream(externalCacheFile)) {
            os.write(csvData.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            MessageBuilder.showMessage(context, "Write Error", e.getMessage());
            return false;
        }

        MessageBuilder.showMessage(context, "Save Succeed to", externalCacheFile.getName());
        return true;
    }

    /**
     * 检查手机<b>应用外部存储空间</b>是否可用.
     *
     * @return true 外部存储空间可用
     */
    private static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 向Android手机<b>应用内部存储空间</b>中写csv文件.
     *
     * @param csvFileName 文件名，应该以".csv"结尾，不需要外部给内存路径
     * @param csvData     写入csv文件的数据
     * @param context     上下文
     * @return true 如果存储成功
     */
    public static boolean saveCsvToInternalStorage(String csvFileName, StringBuilder csvData, Context context) {


        return true;
    }

    /**
     * 从Android手机<b>应用内部存储空间</b>中读出指定文件名的csv文件.
     *
     * @param csvFileName 文件名，应该以".csv"结尾，不需要外部给内存路径
     * @param context     上下文
     * @return 读取到的文件内容StringBuilder，方便外部再做操作
     */
    public static StringBuilder readCsvFromInternalStorage(String csvFileName, Context context) {
        // TODO: 2022/7/12 文件不存在则... 
        
        return null;
    }
}
