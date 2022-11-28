package com.baekseok.meet.util;

import android.os.Build;
import android.util.Log;

// 스레드 안전관리를 위한 도우미 기능
public final class AppRTCUtils {
  private AppRTCUtils() {}

// 예외 발생시 메세지 표시함.
  public static void assertIsTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("오류");
    }
  }

// 스레드 오류 발생 시 문자열 빌드 리턴해줌.
  public static String getThreadInfo() {
    return "@[name=" + Thread.currentThread().getName() + ", id=" + Thread.currentThread().getId()
        + "]";
  }

// 오류 발생시 시스템 속성을 로그에 표시해 줌. Log.d -> debug(상태 확인용)
  public static void logDeviceInfo(String tag) {
    Log.d(tag, "Android SDK: " + Build.VERSION.SDK_INT + ", "
            + "Release: " + Build.VERSION.RELEASE + ", "
            + "Brand: " + Build.BRAND + ", "
            + "Device: " + Build.DEVICE + ", "
            + "Id: " + Build.ID + ", "
            + "Hardware: " + Build.HARDWARE + ", "
            + "Manufacturer: " + Build.MANUFACTURER + ", "
            + "Model: " + Build.MODEL + ", "
            + "Product: " + Build.PRODUCT);
  }
}
