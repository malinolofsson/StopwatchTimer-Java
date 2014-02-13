package se.skuggla.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Created by Malin on 2014-02-08.
 */
public class AlarmActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_layout);
	}

	public void onAlarmStopButton(View view) {

	}
}
