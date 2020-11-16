/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.lineageparts.statusbar;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import lineageos.preference.LineageSystemSettingListPreference;
import lineageos.preference.SecureSettingSwitchPreference;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.utils.DeviceUtils;

import java.util.Set;

public class StatusBarSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {

    private static final String CATEGORY_BATTERY = "status_bar_battery_key";
    private static final String CATEGORY_CLOCK = "status_bar_clock_key";

    private static final String ICON_BLACKLIST = "icon_blacklist";

    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String STATUS_BAR_AM_PM = "clock_am_pm";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";

    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;
	private static final int PULLDOWN_DIR_BOTH = 3;
	
	private static final int HOLE_PUNCH_CAMERA_POSITION_RIGHT = 1;
    private static final int HOLE_PUNCH_CAMERA_POSITION_CENTER = 2;
	private static final int HOLE_PUNCH_CAMERA_POSITION_LEFT = 3;

    private LineageSystemSettingListPreference mQuickPulldown;
    private LineageSystemSettingListPreference mStatusBarClock;
    private SecureSettingSwitchPreference mStatusBarAmPm;
    private LineageSystemSettingListPreference mStatusBarBattery;

    private PreferenceCategory mStatusBarBatteryCategory;
    private PreferenceCategory mStatusBarClockCategory;

    private boolean mHasNotch;
	private int mHolePunchCameraPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.status_bar_settings);

        mHasNotch = getResources().getBoolean(
                org.lineageos.platform.internal.R.bool.config_haveNotch);
		mHolePunchCameraPosition = getResources().getInteger(
                org.lineageos.platform.internal.R.integer.config_holePunchCameraPosition);

        mStatusBarAmPm = findPreference(STATUS_BAR_AM_PM);
        mStatusBarClock = findPreference(STATUS_BAR_CLOCK_STYLE);
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mStatusBarClockCategory = getPreferenceScreen().findPreference(CATEGORY_CLOCK);

        mStatusBarBattery = findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mStatusBarBatteryCategory = getPreferenceScreen().findPreference(CATEGORY_BATTERY);

        mQuickPulldown = findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        mQuickPulldown.setOnPreferenceChangeListener(this);
        updateQuickPulldownSummary(mQuickPulldown.getIntValue(0));
    }

    @Override
    public void onResume() {
        super.onResume();

        final String curIconBlacklist = Settings.Secure.getString(getContext().getContentResolver(),
                ICON_BLACKLIST);

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "clock")) {
            getPreferenceScreen().removePreference(mStatusBarClockCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarClockCategory);
        }

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "battery")) {
            getPreferenceScreen().removePreference(mStatusBarBatteryCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarBatteryCategory);
        }
		
		final boolean disallowAMPM = mHasNotch;
		
        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
		} else if (disallowAMPM) {
			getPreferenceScreen().removePreference(mStatusBarAmPm);
        } else {
			mStatusBarAmPm.setEnabled(true);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_summary);
		}

        final boolean disallowCenteredClock = mHasNotch || HasCenterPunch();

        // Adjust status bar preferences for RTL
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            if (disallowCenteredClock || HasCenterPunch()) {
                	mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch_rtl);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch_rtl);
            } else {
				if (mHolePunchCameraPosition == HOLE_PUNCH_CAMERA_POSITION_RIGHT) {
					mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_hole_punch_right_rtl);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_hole_punch_right_rtl);
				} else if (mHolePunchCameraPosition == HOLE_PUNCH_CAMERA_POSITION_LEFT) {
					mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_hole_punch_left_rtl);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_hole_punch_left_rtl);
				} else {
                	mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_rtl);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_rtl);
				}
            }
            mQuickPulldown.setEntries(R.array.status_bar_quick_qs_pulldown_entries_rtl);
            mQuickPulldown.setEntryValues(R.array.status_bar_quick_qs_pulldown_values_rtl);
        } else if (disallowCenteredClock || HasCenterPunch()) {
			mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch);
            mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch);
        } else {
            	if (mHolePunchCameraPosition == HOLE_PUNCH_CAMERA_POSITION_RIGHT) {
					mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_hole_punch_right);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_hole_punch_right);
				} else if (mHolePunchCameraPosition == HOLE_PUNCH_CAMERA_POSITION_LEFT) {
					mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_hole_punch_left);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_hole_punch_left);
				} else {
                	mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries);
                	mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values);
				}
        }

    }
	
	public boolean HasCenterPunch() {
		if (mHolePunchCameraPosition == HOLE_PUNCH_CAMERA_POSITION_CENTER) {
			return true;
		} else {
			return false;
		}
	}
	
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        String key = preference.getKey();
        switch (key) {
            case STATUS_BAR_QUICK_QS_PULLDOWN:
                updateQuickPulldownSummary(value);
                break;
        }
        return true;
    }

    private void updateQuickPulldownSummary(int value) {
        String summary="";
        switch (value) {
            case PULLDOWN_DIR_NONE:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;

            case PULLDOWN_DIR_LEFT:
            case PULLDOWN_DIR_RIGHT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary,
                    getResources().getString(value == PULLDOWN_DIR_LEFT
                        ? R.string.status_bar_quick_qs_pulldown_summary_left
                        : R.string.status_bar_quick_qs_pulldown_summary_right));
                break;
			case PULLDOWN_DIR_BOTH:
				summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_both);
                break;
        }
        mQuickPulldown.setSummary(summary);
    }
}
