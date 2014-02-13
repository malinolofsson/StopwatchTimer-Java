package se.skuggla.ui;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class StopwatchService extends Service implements Handler.Callback {

    private static final int NOTIFICATION_ID = 3001;
    private static final String TAG = "Stopwatch";
    private final LocalBinder mLocalBinder = new LocalBinder();
    private static final int UPDATE_STOPWATCH_VALUE = 1001;
    private static final long FIVEHUNDRED_MILLIS = 500;

    private StopwatchCallback mStopwatchCallback;
    NotificationCompat.Builder builder;

    //Stopwatch
    private long mStartTimeStopwatch = 0;
    private Handler mStopwatchHandler;
    private boolean mStopwatchIsRunning = false;
    private long mLastTime;
    private boolean mIsReset = false;
    private long elapsed;
    private String timeString;


    public StopwatchService() {

    }

    public class LocalBinder extends Binder {
        public StopwatchService getService() {
            return StopwatchService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //En handler för att räkna ut tiden med hjälp av tråd
        mStopwatchHandler = new Handler(getMainLooper(), this);

        startStopwatchNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    public void startStopwatch() {
        startService(new Intent(this, getClass()));

        startTimerInForeground();
    }

    private void startTimerInForeground() {

        if (!mStopwatchIsRunning) {

            mStopwatchIsRunning = true;

            if (mStartTimeStopwatch == 0) {
                mStartTimeStopwatch = SystemClock.elapsedRealtime();
            } else if (mIsReset) {
                mStartTimeStopwatch = SystemClock.elapsedRealtime();
                mIsReset = false;
            } else if (mStartTimeStopwatch != 0) {
                mStartTimeStopwatch = SystemClock.elapsedRealtime() - mLastTime;
            }
            mStopwatchHandler.sendEmptyMessageDelayed(UPDATE_STOPWATCH_VALUE, FIVEHUNDRED_MILLIS);
        }
    }

    private void updateTimeAndNotify() {
        if (mStopwatchIsRunning) {
            elapsed = SystemClock.elapsedRealtime() - mStartTimeStopwatch;
            timeString = formatDateString(elapsed);

            builder.setContentText(getString(R.string.notification_timer_text, timeString));
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    private void startStopwatchNotification() {
        builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.stopwatch_notification_title))
                .setContentText(getString(R.string.notification_stopwatch_text, timeString))
                .setSmallIcon(R.drawable.ic_launcher);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("sectionNumber", 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Creates the PendingIntent
        PendingIntent notifyIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        builder.setContentIntent(notifyIntent);
    }

    public void stopStopwatch() {
        stopForeground(true);
        stopSelf();

        mLastTime = SystemClock.elapsedRealtime() - mStartTimeStopwatch;
        mStopwatchIsRunning = false;
    }

    public void resetStopwatch() {
         mStartTimeStopwatch = SystemClock.elapsedRealtime();

        if (!mStopwatchIsRunning) {
            mStartTimeStopwatch = SystemClock.elapsedRealtime();
            mIsReset = true;
        }
    }

    public static String formatDateString(long elapsed) {
        //Formatera tiden
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(new Date(elapsed));
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == UPDATE_STOPWATCH_VALUE) {
            if (mStopwatchIsRunning) {
                updateTimeAndNotify();
                notifyStopwatchCallback();
                mStopwatchHandler.sendEmptyMessageDelayed(UPDATE_STOPWATCH_VALUE, FIVEHUNDRED_MILLIS);
            }
        }
        return false;
    }


    public long getStopwatchValue() {
        return elapsed;
    }

    private void notifyStopwatchCallback() {
        if (mStopwatchCallback != null) {
            mStopwatchCallback.onStopwatchValueChanged(getStopwatchValue());
        }
    }

    public void setStopwatchCallback(StopwatchCallback stopwatchCallback) {
        mStopwatchCallback = stopwatchCallback;
    }

    //Callback interface
    public interface StopwatchCallback {
        void onStopwatchValueChanged(long stopwatchValue);
    }
}
