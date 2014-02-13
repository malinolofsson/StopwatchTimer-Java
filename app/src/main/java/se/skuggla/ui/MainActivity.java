package se.skuggla.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity implements ActionBar.TabListener, StopwatchService.StopwatchCallback, TimerService.TimerCallback {

	private static final String TAG = "Stopwatch";
	public static final int STOPWATCH_POSITION = 0;
	public static final int TIMER_POSITION = 1;
	public static int lap_counter = 1;
	SectionsPagerAdapter mSectionsPagerAdapter;

	ViewPager mViewPager;
	private MainFragment mStopwatchFragment;
	private MainFragment mTimerFragment;

	private TimerServiceConnection mTimerServiceConnection;
	private StopwatchServiceConnection mStopwatchServiceConnection;
	private StopwatchService mStopwatchService;
	private TimerService mTimerService;
	private boolean mStopwatchBound = false;
	private boolean mTimerBound = false;

	private String stopwatchTimeString;
	private TextView stopwatchValueView;


	//public int mCurCheckPosition;
	private boolean mTimerIsRunning = false;
	private int sectionNumber = -1;
	private boolean mTimerAlarmIsRinging = false;
	private int mMyCurrentPosition;

	public MainActivity() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*if (savedInstanceState != null) {
			// Restore last state for checked position.
			mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
		}*/

		mStopwatchFragment = MainFragment.newInstance(0);
		mTimerFragment = MainFragment.newInstance(1);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(
					actionBar.newTab()
							.setText(mSectionsPagerAdapter.getPageTitle(i))
							.setTabListener(this));
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mMyCurrentPosition = savedInstanceState.getInt("mMyCurrentPosition");
		// where mMyCurrentPosition should be a public value in your activity.
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		sectionNumber = intent.getIntExtra("sectionNumber", -1);
		mTimerAlarmIsRinging = intent.getBooleanExtra("alarmIsRinging", false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(mMyCurrentPosition != 0){
			mViewPager.setCurrentItem(mMyCurrentPosition);
		}
		if (mTimerAlarmIsRinging) {
			mViewPager.setCurrentItem(TIMER_POSITION);
			mTimerFragment.mTimerLabel.setText(getString(R.string.timer_finnished_text));
		}
		if(sectionNumber != -1) {
			mViewPager.setCurrentItem(sectionNumber);
		}

		mTimerServiceConnection = new TimerServiceConnection();
		bindService(new Intent(this, TimerService.class), mTimerServiceConnection, BIND_AUTO_CREATE);

		mStopwatchServiceConnection = new StopwatchServiceConnection();
		bindService(new Intent(this, StopwatchService.class), mStopwatchServiceConnection, BIND_AUTO_CREATE);
	}


	@Override
	protected void onPause() {
		super.onPause();

		if (mStopwatchBound) {
			mStopwatchService.setStopwatchCallback(null);
			unbindService(mStopwatchServiceConnection);
			mStopwatchBound = false;
		}
		if (mTimerBound) {
			mTimerService.setTimerCallback(null);
			unbindService(mTimerServiceConnection);
			mTimerBound = false;
		}

	}

	class TimerServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mTimerService = ((TimerService.LocalBinder) service).getService();
			mTimerService.setTimerCallback(MainActivity.this);
			mTimerBound = true;
			//	mTimerFragment.setTimerButtonEnabled(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mTimerService = null;
			mTimerBound = false;
			//mTimerFragment.setTimerButtonEnabled(false);
		}
	}

	class StopwatchServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mStopwatchService = ((StopwatchService.LocalBinder) service).getService();
			mStopwatchService.setStopwatchCallback(MainActivity.this);
			mStopwatchBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mStopwatchService = null;
			mStopwatchBound = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {

			Intent intent = new Intent(SettingsActivity.ACTION_SETTINGS);
			startActivity(intent);

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	public void startTimer(View view) {
		if (mTimerService != null && !mTimerIsRunning) {

			mTimerFragment.stopTimerButton.setVisibility(View.VISIBLE);
			mTimerFragment.startTimerButton.setVisibility(View.INVISIBLE);

			mTimerService.startMyTimer();
			mTimerIsRunning = true;
		}

	}

	public void stopTimer(View view) {

		if (mTimerAlarmIsRinging) {
			mTimerService.stopAlarm();
			mTimerIsRunning = false;
			mTimerAlarmIsRinging = false;

			mTimerFragment.stopTimerButton.setVisibility(View.INVISIBLE);
			mTimerFragment.startTimerButton.setVisibility(View.VISIBLE);

			mTimerFragment.mTimerLabel.setText(getString(R.string.timer_text_default));
		}
		else if (mTimerIsRunning && !mTimerAlarmIsRinging) {
			//DO NOTHING when the button is clicked and the alarm isn't ringing!!
		}
	}

	public void startStopStopwatch(View view) {
		if (mStopwatchBound) {
			mStopwatchService.startStopwatch();
		}
	}

	public void stopStopwatch(View view) {
		if (mStopwatchBound) {
			mStopwatchService.stopStopwatch();
		}
	}

	public void resetStopwatch(View view) {
		if (mStopwatchBound) {
			mStopwatchService.resetStopwatch();
		}
	}

	public void saveLapStopwatch(View view) {
		String dateString = getString(R.string.varv_label, lap_counter) + "              " + getString(R.string.lap_time_label, stopwatchTimeString);
		mStopwatchFragment.addLapTime(new Lap(lap_counter++, dateString));
	}

	public static String formatDateString(long elapsed) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return simpleDateFormat.format(new Date(elapsed));
	}

	@Override
	public void onStopwatchValueChanged(long stopwatchValue) {
		//Sätter den nya tiden i textView Från Service!!!
		stopwatchTimeString = formatDateString(stopwatchValue);
		stopwatchValueView = (TextView) findViewById(R.id.stopwatch_value);
		stopwatchValueView.setText(stopwatchTimeString);
	}

	@Override
	public void onTimerValueChanged(long timerValue) {
		mTimerFragment.updateTimerValue(timerValue);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a MainFragment (defined as a static inner class below).
			switch (position) {
				case STOPWATCH_POSITION:
					return mStopwatchFragment;
				case TIMER_POSITION:
					return mTimerFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
				case STOPWATCH_POSITION:
					return getString(R.string.title_section1).toUpperCase(l);
				case TIMER_POSITION:
					return getString(R.string.title_section2).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class MainFragment extends Fragment {

		private List<Lap> mLaps;
		private LapAdapter mLapAdapter;
		private TextView mTimerLabel;
		//	private Button mTimerButton;


		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";
		private int mCurCheckPosition;
		private Button stopTimerButton;
		private Button startTimerButton;

		/**
		 * Returns a new instance of this fragment for the given section
		 * number.
		 */
		public static MainFragment newInstance(int sectionNumber) {
			MainFragment fragment = new MainFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public MainFragment() {
			mLaps = new LinkedList<Lap>();
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			mLapAdapter = new LapAdapter(activity, mLaps);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		                         Bundle savedInstanceState) {
			int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);

			switch (sectionNumber) {
				case STOPWATCH_POSITION:
					View stopwatchView = inflater.inflate(R.layout.fragment_main, container, false);
					ListView listView = (ListView) stopwatchView.findViewById(R.id.lap_list);
					listView.setAdapter(mLapAdapter);
					mCurCheckPosition = STOPWATCH_POSITION;
					return stopwatchView;

				case TIMER_POSITION:
					View timerView = inflater.inflate(R.layout.timer_layout, container, false);
					mTimerLabel = (TextView) timerView.findViewById(R.id.timer_textview);
					mCurCheckPosition = TIMER_POSITION;

					stopTimerButton = (Button) timerView.findViewById(R.id.timer_stop_button);
					startTimerButton = (Button) timerView.findViewById(R.id.timer_start_button);

					stopTimerButton.setVisibility(View.INVISIBLE);
					startTimerButton.setVisibility(View.VISIBLE);

					return timerView;
			}
			return null;
		}


		public void addLapTime(Lap lap) {
			mLaps.add(lap);
			mLapAdapter.notifyDataSetChanged();
		}

		public void updateTimerValue(long timerValue) {
			if (mTimerLabel != null) {
				mTimerLabel.setText(getString(R.string.timer_text_value, timerValue / 1000));
				/*if (timerValue / 1000 == 0) {
					Log.d(TAG, "LARMET går!!!");
				}*/
			}
		}

		@Override
		public void onSaveInstanceState(Bundle savedInstanceState) {
			super.onSaveInstanceState(savedInstanceState);
			savedInstanceState.putInt("mMyCurrentPosition", mCurCheckPosition);
		}

/*
		public void setTimerButtonEnabled(boolean enabled) {
			if (mTimerButton != null) {
				mTimerButton.setEnabled(enabled);
			}
		}*/
	}

	static class LapAdapter extends BaseAdapter {


		private Activity mActivity;
		private List<Lap> mLaps;

		public LapAdapter(Activity activity, List<Lap> laps) {
			mActivity = activity;
			mLaps = laps;
		}

		@Override
		public int getCount() {
			return mLaps.size();
		}

		@Override
		public Object getItem(int position) {
			return mLaps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mLaps.get(position).getmId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(mActivity, R.layout.lap_item, null);
			}
			Lap lap = mLaps.get(position);
			((TextView) convertView.findViewById(R.id.lap_time)).setText(lap.getLapTime());
			return convertView;
		}
	}


}
