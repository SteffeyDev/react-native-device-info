package com.learnium.RNDeviceInfo;

import android.Manifest;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.provider.Settings.Secure;
import android.webkit.WebSettings;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.DisplayMetrics;
import android.content.BroadcastReceiver;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.common.logging.FLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;

public class RNDeviceModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

  ReactApplicationContext reactContext;

  WifiInfo wifiInfo;

  final BroadcastReceiver receiver;

  public RNDeviceModule(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;

    final ReactApplicationContext ctx = reactContext;
    receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Configuration newConfig = intent.getParcelableExtra("newConfig");
			Log.d("receiver", String.valueOf(newConfig.orientation));
        
			WritableMap dimParams = Arguments.createMap();
			dimParams.putInt("width", newConfig.screenWidthDp);
			dimParams.putInt("height", newConfig.screenHeightDp);
			if (ctx.hasActiveCatalystInstance()) {
			  ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("ScreenSizeChanged", dimParams);
			}
		}
    };
    ctx.addLifecycleEventListener(this);

    WifiManager manager = (WifiManager) reactContext.getSystemService(Context.WIFI_SERVICE);
    this.wifiInfo = manager.getConnectionInfo();
  }

  @Override
  public void onHostResume() {
    final Activity activity = getCurrentActivity();

    assert activity != null;
    activity.registerReceiver(receiver, new IntentFilter("onConfigurationChanged"));
  }

  @Override
  public void onHostPause() {
    final Activity activity = getCurrentActivity();
    if (activity == null) return;
    try
    {
        activity.unregisterReceiver(receiver);
    }
    catch (java.lang.IllegalArgumentException e) {
        FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
    }
  }

  @Override
  public void onHostDestroy() {
    final Activity activity = getCurrentActivity();
    if (activity == null) return;
    try
    {
        activity.unregisterReceiver(receiver);
    }
    catch (java.lang.IllegalArgumentException e) {
        FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
    }
  }

  @Override
  public String getName() {
    return "RNDeviceInfo";
  }

  private String getCurrentLanguage() {
      Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          return current.toLanguageTag();
      } else {
          StringBuilder builder = new StringBuilder();
          builder.append(current.getLanguage());
          if (current.getCountry() != null) {
              builder.append("-");
              builder.append(current.getCountry());
          }
          return builder.toString();
      }
  }

  private String getCurrentCountry() {
    Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
    return current.getCountry();
  }

  private Boolean isEmulator() {
    return Build.FINGERPRINT.startsWith("generic")
      || Build.FINGERPRINT.startsWith("unknown")
      || Build.MODEL.contains("google_sdk")
      || Build.MODEL.contains("Emulator")
      || Build.MODEL.contains("Android SDK built for x86")
      || Build.MANUFACTURER.contains("Genymotion")
      || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
      || "google_sdk".equals(Build.PRODUCT);
  }

  private Boolean isTablet() {
    int layout = getReactApplicationContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    return layout == Configuration.SCREENLAYOUT_SIZE_LARGE || layout == Configuration.SCREENLAYOUT_SIZE_XLARGE;
  }

  @ReactMethod
  public void isPinOrFingerprintSet(Callback callback) {
    KeyguardManager keyguardManager = (KeyguardManager) this.reactContext.getSystemService(Context.KEYGUARD_SERVICE); //api 16+
    callback.invoke(keyguardManager.isKeyguardSecure());
  }

  @ReactMethod
  public void getIpAddress(Promise p) {
    String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
    p.resolve(ipAddress);
  }

  @ReactMethod
  public void getMacAddress(Promise p) {
    String macAddress = wifiInfo.getMacAddress();
    p.resolve(macAddress);
  }

  private float width() {
    Configuration config = getReactApplicationContext().getResources().getConfiguration();
    return config.screenWidthDp;
  }

  private float height() {
    Configuration config = getReactApplicationContext().getResources().getConfiguration();
    return config.screenHeightDp;
  }

  @ReactMethod
  public void listenToFontSizeChange() {
    WritableMap fontParams = Arguments.createMap();
    double fontScale = getReactApplicationContext().getResources().getConfiguration().fontScale;
    if (fontScale < 1) fontParams.putInt("fontScale", -2);
    else if (fontScale == 1) fontParams.putInt("fontScale", 0);
    else fontParams.putInt("fontScale", 6);
    fontParams.putBoolean("initial", true);
    this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("FontSizeChanged", fontParams);
  }

  @Override
  public @Nullable Map<String, Object> getConstants() {
    HashMap<String, Object> constants = new HashMap<String, Object>();

    PackageManager packageManager = this.reactContext.getPackageManager();
    String packageName = this.reactContext.getPackageName();

    constants.put("appVersion", "not available");
    constants.put("buildVersion", "not available");
    constants.put("buildNumber", 0);

    try {
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      constants.put("appVersion", info.versionName);
      constants.put("buildNumber", info.versionCode);
      constants.put("firstInstallTime", info.firstInstallTime);
      constants.put("lastUpdateTime", info.lastUpdateTime);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    String deviceName = "Unknown";

    try {
      BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
      if(myDevice!=null){
        deviceName = myDevice.getName();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    constants.put("instanceId", InstanceID.getInstance(this.reactContext).getId());
    constants.put("serialNumber", Build.SERIAL);
    constants.put("deviceName", deviceName);
    constants.put("systemName", "Android");
    constants.put("systemVersion", Build.VERSION.RELEASE);
    constants.put("model", Build.MODEL);
    constants.put("brand", Build.BRAND);
    constants.put("deviceId", Build.BOARD);
    constants.put("apiLevel", Build.VERSION.SDK_INT);
    constants.put("deviceLocale", this.getCurrentLanguage());
    constants.put("deviceCountry", this.getCurrentCountry());
    constants.put("uniqueId", Secure.getString(this.reactContext.getContentResolver(), Secure.ANDROID_ID));
    constants.put("systemManufacturer", Build.MANUFACTURER);
    constants.put("bundleId", packageName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      constants.put("userAgent", WebSettings.getDefaultUserAgent(this.reactContext));
    }
    constants.put("timezone", TimeZone.getDefault().getID());
    constants.put("isEmulator", this.isEmulator());
    constants.put("isTablet", this.isTablet());
    constants.put("width", this.width());
    constants.put("height", this.height());
    if (getCurrentActivity().checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
            getCurrentActivity().checkCallingOrSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED ||
            getCurrentActivity().checkCallingOrSelfPermission("android.permission.READ_PHONE_NUMBERS") == PackageManager.PERMISSION_GRANTED) {
        TelephonyManager telMgr = (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
        constants.put("phoneNumber", telMgr.getLine1Number());
    }
    return constants;
  }
}
