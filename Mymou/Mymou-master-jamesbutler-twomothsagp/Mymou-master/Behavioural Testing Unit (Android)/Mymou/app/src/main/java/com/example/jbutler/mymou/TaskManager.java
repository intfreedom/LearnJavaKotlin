package com.example.jbutler.mymou;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Calendar;

public class TaskManager extends Activity implements Thread.UncaughtExceptionHandler {

    public static String TAG = "TaskManager";

    // Task you want to run goes here
    private static TaskExample task = new TaskExample(); //TODO AS says static context classes are a memory leak...
    //private static TaskFromPaper task = new TaskFromPaper();
    private static String taskId = "001";  // Unique string prefixed to all log entries

    public static int faceRecogPrediction = -1;
    private static int monkeyButtonPressed = -1;
    private static boolean faceRecogRunning = false;
    private static int trialCounter = 0;
    public static RewardSystem rewardSystem;

    private static FaceRecog faceRecog;
    private static ArrayList<String> trialData;
    public static String photoTimestamp;
    public static String message;
    private static Handler logHandler;
    private static HandlerThread logThread;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private static Context mContext;
    private static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        activity = (Activity) this;

        initialiseScreenSettings();

        if (MainMenu.useFaceRecognition) {
            // Load facerecog off the main thread as takes a while
            Thread t = new Thread(new Runnable() {
                public void run() {
                    faceRecog = new FaceRecog();
                }
            });
            t.start();

        }

        registerPowerReceivers();

        initialiseLogHandler();

        //only lock if we aren't in testing mode
        if (!MainMenu.testingMode) {
            this.startLockTask();
        }

//            // Enable this if you want task to automatically restart on crash
//            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//                @Override
//                public void uncaughtException(Thread thread, Throwable throwable) {
//                    logHandler.post(new CrashReport(throwable));
//                    quitBt();
//                    restartApp();
//                }
//            });

        startTask();

        // This is last as it interacts with objects in the task
        initaliseRewardSystem();

    }

    private void initialiseLogHandler() {
        logThread = new HandlerThread("LogBackground");
        logThread.start();
        logHandler = new Handler(logThread.getLooper());
    }

    private void initaliseRewardSystem() {
        boolean successfullyEstablished = false;
        rewardSystem.quitBt();
        rewardSystem = new RewardSystem(this);
        if (rewardSystem.bluetoothConnection | !MainMenu.useBluetooth) {
            successfullyEstablished = enableApp(true);
        }

        // Repeat if either couldn't connect or couldn't enable app
        if (!successfullyEstablished){
            Handler handlerOne = new Handler();
            handlerOne.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initaliseRewardSystem();
                }
            }, 5000);
        }
    }

    public void onValueChanged(int newValue) {
        // The int got a new value! Update the text
        Log.d(TAG, "Integer updated");

    }

    private void startTask() {
        Log.d(TAG, "startTask() called");
        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        setContentView(R.layout.activity_all_tasks);
        if (MainMenu.useCamera) {
            Log.d(TAG, "startTask() using CameraMain");
            CameraMain cM = new CameraMain();
            fragmentTransaction.add(R.id.container, cM);
        }
        fragmentTransaction.add(R.id.container, task);
        fragmentTransaction.commit();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logHandler.post(new CrashReport(throwable));
        rewardSystem.quitBt();
        restartApp();
    }

    private void restartApp() {
        Intent intent=new Intent(getApplicationContext(), TaskManager.class);
        intent.putExtra("restart",true);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
        System.exit(2);
    }


    public static void deliverReward(int juiceChoice, int rewardAmount) {
        rewardSystem.activateChannel(juiceChoice, rewardAmount);
    }

    private void registerPowerReceivers() {
        IntentFilter unplugIntent = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        IntentFilter plugIntent = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        registerReceiver(powerPlugReceiver, plugIntent);
        registerReceiver(powerUnplugReceiver, unplugIntent);
    }

    private final BroadcastReceiver powerPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
                // Do something when power connected
                //task.enableApp();
            }
        }
    };

    private final BroadcastReceiver powerUnplugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                // Do something when power disconnected
                //task.hideApplication();
            }
        }
    };


    public static void setFaceRecogPrediction(int[] intArray) {
        if (faceRecog != null) {

            faceRecogPrediction = faceRecog.idImage(intArray);

            if (faceRecogPrediction == monkeyButtonPressed) {

                // If monkey clicked it's designated button
                Log.d(TAG, "Monkey pressed correct cue");
                task.resultMonkeyPressedTheirCue(true);

            } else {

                // If monkey clicked wrong button
                Log.d(TAG, "Monkey pressed wrong cue");
                task.resultMonkeyPressedTheirCue(false);

            }

        } else {

            Log.d(TAG, "Error: FaceRecog not instantiated");

        }

        // Release facerecog as now ready to process another image
        faceRecogRunning = false;
    }

    public static void commitTrialData(int overallTrialOutcome) {
        if (dateHasChanged()) {
            trialCounter = 0;
        } else {
            trialCounter++;
        }
        if (trialData != null) {
            int length = trialData.size();
            for (int i = 0; i < length; i++) {
                String s = trialData.get(i);
                // Prefix variables that were constant throughout trial (trial result, which monkey, etc)
                s = taskId +"," + trialCounter +"," + faceRecogPrediction + "," + overallTrialOutcome + "," + s;
                logHandler.post(new LogEvent(s));
                Log.d(TAG, "commitTrialData: " + s);
            }
        }
    }

    private void initialiseScreenSettings() {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final View decorView = TaskManager.this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public static void shutdownLoop() {
        Log.d(TAG, "shutdownLoop() called");
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int AMPM = c.get(Calendar.AM_PM);
        if (hour >= 7 && AMPM == Calendar.PM) {
            enableApp(false);
            boolean restartNextDay = true;
            if(restartNextDay) {

                // Only restart on certain days of the week
                int day = c.get(Calendar.DAY_OF_WEEK);
                if (day == Calendar.THURSDAY | day == Calendar.FRIDAY | day == Calendar.SATURDAY) {
                    startupLoop();
                }

            }
        } else {
            Handler handlerOne = new Handler();
            handlerOne.postDelayed(new Runnable() {
                @Override
                public void run() {
                    shutdownLoop();
                }
            }, 60000);
        }
    }

    private static void startupLoop() {
        Log.d(TAG, "startupLoop() called");
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int AMPM = c.get(Calendar.AM_PM);
        if (hour >= 7 && AMPM == Calendar.AM) {
                enableApp(true);
                shutdownLoop();
        } else {
            Handler handlerOne = new Handler();
            handlerOne.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startupLoop();
                }
            }, 60000);
        }
    }

    public static boolean enableApp(boolean bool) {
        if (task.hideApplication != null) {
            if (bool) {
                task.hideApplication.setEnabled(false);
                task.hideApplication.setVisibility(View.INVISIBLE);
            } else {
                task.hideApplication.setEnabled(true);
                task.hideApplication.setVisibility(View.VISIBLE);
                setBrightness(1);
            }
            return true;
        } else {
            Log.d(TAG, "hideApplication object not instantiated");
            return false;
        }
    }

    public static void setBrightness(int brightness) {
        if (Settings.System.canWrite(mContext)) {
            if (brightness > 255) {
                brightness = 255;
            } else if (brightness < 0) {
                brightness = 0;
            }
            ContentResolver cResolver = mContext.getContentResolver();
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        }
    }

    public static void logEvent(String data) {
        String timestamp = MainMenu.folderManager.getTimestamp();
        String msg = TaskManager.photoTimestamp + "," + timestamp + "," + data;
        trialData.add(msg);
    }

    public static void resetTrialData() {
        trialData = new ArrayList<String>();
    }

    // Takes selfie and checks to see if it matches with which monkey it should be
    public static boolean checkMonkey(int monkId) {
        if (MainMenu.useFaceRecognition && faceRecogRunning) {
            // Previous face recog still running
            return false;
        }

        boolean photoTaken = takePhoto();

        if (photoTaken && MainMenu.useFaceRecognition) {
            // Photo taken, now we wait for faceRecog to return prediction
            faceRecogRunning = true;
            monkeyButtonPressed = monkId;
        }

        return photoTaken;

    }

    public static boolean takePhoto() {
        if (MainMenu.useCamera) {
            Log.d(TAG, "takePhoto() called");
            photoTimestamp = MainMenu.folderManager.getTimestamp();
            boolean photoTaken = CameraMain.captureStillPicture(photoTimestamp);
            return photoTaken;
        } else {
            return true;
        }
    }

    //Checks if todays date is the same as the last time function was called
    public static boolean dateHasChanged() {
        String todaysDate = MainMenu.folderManager.getBaseDate();
        SharedPreferences sharedPref = activity.getPreferences(mContext.MODE_PRIVATE);
        String lastRecordedDate = sharedPref.getString("lastdate", "null");
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("lastdate", todaysDate);
        editor.commit();
        if (todaysDate.equals(lastRecordedDate)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() called");
        rewardSystem.quitBt();
        unregisterReceivers();
        quitThreads();
        this.stopLockTask();
    }

    private void quitThreads() {
        try {
            logThread.quitSafely();
            logThread.join();
            logThread = null;
            logHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {

        }
    }

    private void unregisterReceivers() {
        try {
            unregisterReceiver(powerPlugReceiver);
            unregisterReceiver(powerUnplugReceiver);
        } catch(IllegalArgumentException e) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        return false;
    }

}
