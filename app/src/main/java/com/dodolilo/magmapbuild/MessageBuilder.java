package com.dodolilo.magmapbuild;

import android.app.AlertDialog;
import android.content.Context;

/**
 * 一个用于提供简单的UI消息提醒方法的工具类.
 */
public class MessageBuilder {
    /**
     * 弹出提醒的消息窗口.
     * 只提供“确认”按钮，点击后不做任何事.
     * @param context
     * @param title
     * @param message
     */
    public static void showMessage(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
