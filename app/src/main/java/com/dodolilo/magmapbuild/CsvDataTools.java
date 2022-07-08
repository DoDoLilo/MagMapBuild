package com.dodolilo.magmapbuild;

public class CsvDataTools {

    /**
     * 将多个传感器数据数组转为可供csv文件存储的字符串格式.
     * 注意每个数值之间都需要插入','进行间隔，结尾要用'\n'结束.
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
        for (float data: accValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        for (float data: gyroValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        for (float data: magValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        for (float data: quatValues) {
            oneLineValues.append(',');
            oneLineValues.append(data);
        }
        oneLineValues.append('\n');

        return oneLineValues.toString();
    }

}
