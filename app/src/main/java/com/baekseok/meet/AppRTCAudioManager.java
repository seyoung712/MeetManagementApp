package com.baekseok.meet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.baekseok.meet.util.AppRTCUtils;

public class AppRTCAudioManager {
  private static final String TAG = "AppRTCAudioManager";
  private static final String SPEAKERPHONE_AUTO = "auto";
  private static final String SPEAKERPHONE_TRUE = "true";
  private static final String SPEAKERPHONE_FALSE = "false";

  public enum AudioDevice { SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, BLUETOOTH, NONE }

  public enum AudioManagerState {
    UNINITIALIZED,
    PREINITIALIZED,
    RUNNING,
  }

  public interface AudioManagerEvents {
    void onAudioDeviceChanged(
        AudioDevice selectedAudioDevice, Set<AudioDevice> availableAudioDevices);
  }

  private final Context apprtcContext;
  @Nullable
  private AudioManager audioManager;

  @Nullable
  private AudioManagerEvents audioManagerEvents;
  private AudioManagerState amState;
  private int savedAudioMode = AudioManager.MODE_INVALID;
  private boolean savedIsSpeakerPhoneOn;
  private boolean savedIsMicrophoneMute;
  private boolean hasWiredHeadset;


  private AudioDevice defaultAudioDevice;
  private AudioDevice selectedAudioDevice;
  private AudioDevice userSelectedAudioDevice;
  @Nullable private final String useSpeakerphone;
  private final AppRTCBluetoothManager bluetoothManager;
  private Set<AudioDevice> audioDevices = new HashSet<>();
  private BroadcastReceiver wiredHeadsetReceiver;

  @Nullable
  private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

  private void onProximitySensorChangedState() {
    if (!useSpeakerphone.equals(SPEAKERPHONE_AUTO)) {
      return;
    }

    if (audioDevices.size() == 2 && audioDevices.contains(AppRTCAudioManager.AudioDevice.EARPIECE)
        && audioDevices.contains(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)) {
    }
  }

  private class WiredHeadsetReceiver extends BroadcastReceiver {
    private static final int STATE_UNPLUGGED = 0;
    private static final int STATE_PLUGGED = 1;
    private static final int HAS_NO_MIC = 0;
    private static final int HAS_MIC = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
      int state = intent.getIntExtra("state", STATE_UNPLUGGED);
      int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);
      String name = intent.getStringExtra("name");
      Log.d(TAG, "WiredHeadsetReceiver.onReceive" + AppRTCUtils.getThreadInfo() + ": "
              + "a=" + intent.getAction() + ", s="
              + (state == STATE_UNPLUGGED ? "unplugged" : "plugged") + ", m="
              + (microphone == HAS_MIC ? "mic" : "no mic") + ", n=" + name + ", sb="
              + isInitialStickyBroadcast());
      hasWiredHeadset = (state == STATE_PLUGGED);
      updateAudioDeviceState();
    }
  }

  static AppRTCAudioManager create(Context context) {
    return new AppRTCAudioManager(context);
  }

  private AppRTCAudioManager(Context context) {
    apprtcContext = context;
    audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    bluetoothManager = AppRTCBluetoothManager.create(context, this);
    wiredHeadsetReceiver = new WiredHeadsetReceiver();
    amState = AudioManagerState.UNINITIALIZED;

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    useSpeakerphone = sharedPreferences.getString(context.getString(R.string.pref_speakerphone_key),
        context.getString(R.string.pref_speakerphone_default));
    Log.d(TAG, "스피커폰 사용여부: " + useSpeakerphone);
    if (useSpeakerphone.equals(SPEAKERPHONE_FALSE)) {
      defaultAudioDevice = AudioDevice.EARPIECE;
    } else {
      defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
    }
    Log.d(TAG, "오디오 설정 default: " + defaultAudioDevice);
    AppRTCUtils.logDeviceInfo(TAG);
  }

  public void start(AudioManagerEvents audioManagerEvents) {
    Log.d(TAG, "start");
    if (amState == AudioManagerState.RUNNING) {
      Log.e(TAG, "AudioManager 실행");
      return;
    }

    Log.d(TAG, "AudioManager 실행중");
    this.audioManagerEvents = audioManagerEvents;
    amState = AudioManagerState.RUNNING;

    savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
    savedIsMicrophoneMute = audioManager.isMicrophoneMute();
    hasWiredHeadset = hasWiredHeadset();

    audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

      @Override
      public void onAudioFocusChange(int focusChange) {
        final String typeOfChange;
        switch (focusChange) {
          case AudioManager.AUDIOFOCUS_GAIN:
            typeOfChange = "AUDIOFOCUS_GAIN";
            break;
          case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT";
            break;
          case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
            typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE";
            break;
          case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
            typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK";
            break;
          case AudioManager.AUDIOFOCUS_LOSS:
            typeOfChange = "AUDIOFOCUS_LOSS";
            break;
          case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT";
            break;
          case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
            break;
          default:
            typeOfChange = "AUDIOFOCUS_INVALID";
            break;
        }
        Log.d(TAG, "onAudioFocusChange: " + typeOfChange);
      }
    };

    // audio focus 요청 없이 focus 변경.
    int result = audioManager.requestAudioFocus(audioFocusChangeListener,
        AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      Log.d(TAG, "Audio focus request granted for VOICE_CALL streams");
    } else {
      Log.e(TAG, "Audio focus request failed");
    }

  // audioManager 내 MODE_IN_COMMUNICATION 설정. VoIP급 성능 설정.
    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

  // 오디오 Mute Default 설정
    setMicrophoneMute(false);

    // Set initial device states.
    userSelectedAudioDevice = AudioDevice.NONE;
    selectedAudioDevice = AudioDevice.NONE;
    audioDevices.clear();

    // 블루투스 매니저를 실행하여 블루투스 장치 감지 및 초기화.
    bluetoothManager.start();

    // 오디오 장치 초기선택 수행. Bluetooth 또는 유선 등.
    updateAudioDeviceState();

    registerReceiver(wiredHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    Log.d(TAG, "AudioManager 실행");
  }

  @SuppressWarnings("deprecation")
  public void stop() {
    Log.d(TAG, "stop");
    if (amState != AudioManagerState.RUNNING) {
      Log.e(TAG, "오디오 매니저 문제 발생하여 중단 시도중 : " + amState);
      return;
    }
    amState = AudioManagerState.UNINITIALIZED;

    unregisterReceiver(wiredHeadsetReceiver);

    bluetoothManager.stop();

    // 저장된 오디오 상태 복원(오류 발생시 실행됨)
    setSpeakerphoneOn(savedIsSpeakerPhoneOn);
    setMicrophoneMute(savedIsMicrophoneMute);

    audioManager.abandonAudioFocus(audioFocusChangeListener);
    audioFocusChangeListener = null;
    Log.d(TAG, "VOICE_CALL Stream에 대한 audio focus를 찾을 수 없음.");

    audioManagerEvents = null;
    Log.d(TAG, "AudioManager 중단.");
  }

  /** Changes selection of the currently active audio device. */
  private void setAudioDeviceInternal(AudioDevice device) {
    Log.d(TAG, "setAudioDeviceInternal(device=" + device + ")");
    AppRTCUtils.assertIsTrue(audioDevices.contains(device));

    switch (device) {
      case SPEAKER_PHONE:
        setSpeakerphoneOn(true);
        break;
      case EARPIECE:
        setSpeakerphoneOn(false);
        break;
      case WIRED_HEADSET:
        setSpeakerphoneOn(false);
        break;
      case BLUETOOTH:
        setSpeakerphoneOn(false);
        break;
      default:
        Log.e(TAG, "오디오 장치 설정 오류 : 잘못된 장치 선택됨.");
        break;
    }
    selectedAudioDevice = device;
  }

  // default audio 설정.
  public void setDefaultAudioDevice(AudioDevice defaultDevice) {
    switch (defaultDevice) {
      case SPEAKER_PHONE:
        defaultAudioDevice = defaultDevice;
        break;
      case EARPIECE:
        if (hasEarpiece()) {
          defaultAudioDevice = defaultDevice;
        } else {
          defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
        }
        break;
      default:
        Log.e(TAG, "오디오 장치 설정 오류 : 잘못된 장치 선택됨.");
        break;
    }
    Log.d(TAG, "기본설정 오디오 장치(장치=" + defaultAudioDevice + ")");
    updateAudioDeviceState();
  }

  public void selectAudioDevice(AudioDevice device) {
    if (!audioDevices.contains(device)) {
      Log.e(TAG, "해당 장치 선택불가 : " + device + " 사용 가능 장치 : " + audioDevices);
    }
    userSelectedAudioDevice = device;
    updateAudioDeviceState();
  }

  public Set<AudioDevice> getAudioDevices() {
    return Collections.unmodifiableSet(new HashSet<>(audioDevices));
  }

  public AudioDevice getSelectedAudioDevice() {
    return selectedAudioDevice;
  }

  private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    apprtcContext.registerReceiver(receiver, filter);
  }

  private void unregisterReceiver(BroadcastReceiver receiver) {
    apprtcContext.unregisterReceiver(receiver);
  }

  private void setSpeakerphoneOn(boolean on) {
    boolean wasOn = audioManager.isSpeakerphoneOn();
    if (wasOn == on) {
      return;
    }
    audioManager.setSpeakerphoneOn(on);
  }

  private void setMicrophoneMute(boolean on) {
    boolean wasMuted = audioManager.isMicrophoneMute();
    if (wasMuted == on) {
      return;
    }
    audioManager.setMicrophoneMute(on);
  }

  private boolean hasEarpiece() {
    return apprtcContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
  }

  @Deprecated
  private boolean hasWiredHeadset() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return audioManager.isWiredHeadsetOn();
    }
    return false;
  }

  public void updateAudioDeviceState() {
    Log.d(TAG, "--- 오디오 장치 업데이트 : "
            + "유선 장치=" + hasWiredHeadset + ", "
            + "블루투스 상태=" + bluetoothManager.getState());
    Log.d(TAG, "장치 상태: "
            + "이용가능 장치=" + audioDevices + ", "
            + "선택한 장치=" + selectedAudioDevice + ", "
            + "사용자가 선택한 장치=" + userSelectedAudioDevice);

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_DISCONNECTING) {
      bluetoothManager.updateDevice();
    }

    Set<AudioDevice> newAudioDevices = new HashSet<>();

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE) {
      newAudioDevices.add(AudioDevice.BLUETOOTH);
    }

    if (hasWiredHeadset) {
      newAudioDevices.add(AudioDevice.WIRED_HEADSET);
    } else {
      newAudioDevices.add(AudioDevice.SPEAKER_PHONE);
      if (hasEarpiece()) {
        newAudioDevices.add(AudioDevice.EARPIECE);
      }
    }
    boolean audioDeviceSetUpdated = !audioDevices.equals(newAudioDevices);
    audioDevices = newAudioDevices;
    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
        && userSelectedAudioDevice == AudioDevice.BLUETOOTH) {
      userSelectedAudioDevice = AudioDevice.NONE;
    }
    if (hasWiredHeadset && userSelectedAudioDevice == AudioDevice.SPEAKER_PHONE) {
      userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
    }
    if (!hasWiredHeadset && userSelectedAudioDevice == AudioDevice.WIRED_HEADSET) {
      userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
    }

    boolean needBluetoothAudioStart =
        bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
        && (userSelectedAudioDevice == AudioDevice.NONE
               || userSelectedAudioDevice == AudioDevice.BLUETOOTH);

    boolean needBluetoothAudioStop =
        (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
            || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING)
        && (userSelectedAudioDevice != AudioDevice.NONE
               && userSelectedAudioDevice != AudioDevice.BLUETOOTH);

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING
        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
      Log.d(TAG, "요청된 블루투스 오디오 시작여부: " + needBluetoothAudioStart + ", "
              + "정지 상태=" + needBluetoothAudioStop + ", "
              + "현재 상태=" + bluetoothManager.getState());
    }

    if (needBluetoothAudioStop) {
      bluetoothManager.stopScoAudio();
      bluetoothManager.updateDevice();
    }

    if (needBluetoothAudioStart && !needBluetoothAudioStop) {
      if (!bluetoothManager.startScoAudio()) {
        audioDevices.remove(AudioDevice.BLUETOOTH);
        audioDeviceSetUpdated = true;
      }
    }

    final AudioDevice newAudioDevice;

    if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
      newAudioDevice = AudioDevice.BLUETOOTH;
    } else if (hasWiredHeadset) {
      newAudioDevice = AudioDevice.WIRED_HEADSET;
    } else {
      newAudioDevice = defaultAudioDevice;
    }
    if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
      setAudioDeviceInternal(newAudioDevice);
      Log.d(TAG, "새 장치 상태: "
              + "이용 가능 장치=" + audioDevices + ", "
              + "선택된 장치=" + newAudioDevice);
      if (audioManagerEvents != null) {
        audioManagerEvents.onAudioDeviceChanged(selectedAudioDevice, audioDevices);
      }
    }
    Log.d(TAG, "--- updateAudioDeviceState");
  }
}
