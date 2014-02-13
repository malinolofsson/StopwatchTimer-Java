package se.skuggla.ui;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

public class TimerService extends Service implements Handler.Callback {

	private static final int NOTIFICATION_ID = 5005;
	private static final String TAG = "Timer";
	private static final int COUNTDOWN_TIMER_MSG = 2000;
	private static final int TIMER_START_VALUE = 10000;
	private final LocalBinder mLocalBinder = new LocalBinder();
	private TimerCallback mTimerCallback;

	private boolean mTimerIsRunning = false;
	NotificationCompat.Builder builder;
	private long mTimerMsLeft;
	private long mStartTime;
	private Handler mHandler;
	Ringtone mRingtone;
	Uri notification;
	private boolean isAlarming = false;

	public TimerService() {

	}

	public class LocalBinder extends Binder {
		public TimerService getService() {
			return TimerService.this;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (msg.what == COUNTDOWN_TIMER_MSG) {
			long now = SystemClock.elapsedRealtime();
			mTimerMsLeft = TIMER_START_VALUE - (now - mStartTime);
			if (mTimerMsLeft > 0 && mTimerIsRunning) {
				notifyTimerCallback();

				builder.setContentText(getString(R.string.notification_timer_text, mTimerMsLeft / 1000));

				startForeground(NOTIFICATION_ID, builder.build());

				mHandler.sendEmptyMessageDelayed(COUNTDOWN_TIMER_MSG, 100);
			}
			else {
				startAlarm();
			}
		}
		return false;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mHandler = new Handler(getMainLooper(), this);

		notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		mRingtone = RingtoneManager.getRingtone(getApplicationContext(), notification);

		createNotification();
	}

	private void createNotification() {

		builder = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.timer_notifications_title))
				.setContentText(getString(R.string.notification_timer_title))
				.setSmallIcon(R.drawable.ic_launcher);

		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("sectionNumber", 1);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// Creates the PendingIntent
		PendingIntent notifyIntent =
				PendingIntent.getActivity(
						this,
						1,
						intent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		// Puts the PendingIntent into the notification builder
		builder.setContentIntent(notifyIntent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mLocalBinder;
	}

	public void startMyTimer() {
		startService(new Intent(this, getClass()));

		if (!mTimerIsRunning) {
			mTimerIsRunning = true;
			startTimerInForeground();
		}
	}

	public void startAlarm() {
		mRingtone.play();

		//Send to MainActivity that the alarm is ringing
		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("alarmIsRinging", true);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);

		//Change text in Notification when the alarm is ringing
		builder.setContentText(getString(R.string.timer_finnished_text, mTimerMsLeft / 1000));
		startForeground(NOTIFICATION_ID, builder.build());

		isAlarming = true;
	}

	public void stopAlarm() {

		if (isAlarming) {
			mRingtone.stop();
			isAlarming = false;
			mTimerIsRunning = false;
			stopForeground(true);
			stopSelf();
		}
		else {
			mTimerIsRunning = true;
		}
	}

	private void startTimerInForeground() {
		mStartTime = SystemClock.elapsedRealtime();
		mTimerMsLeft = TIMER_START_VALUE;
		mHandler.sendEmptyMessage(COUNTDOWN_TIMER_MSG);
	}

	public long getTimerValue() {
		return mTimerMsLeft;
	}

	private void notifyTimerCallback() {
		if (mTimerCallback != null) {
			mTimerCallback.onTimerValueChanged(getTimerValue());
		}
	}

	public void setTimerCallback(TimerCallback timerCallback) {
		mTimerCallback = timerCallback;
	}

	//Callback interface
	public interface TimerCallback {
		void onTimerValueChanged(long timerValue);
	}
}
