package com.yeleman.zerindroid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
    }

    public SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

}
