package com.anteour.shivchalisa;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public class SettingsPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        loadSummary();
    }

    @Override
    public void onResume() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Preference preference = findPreference(s);
        switch (s) {
            case "setting_lang":
                preference.setSummary(sharedPreferences.getString(s, "English"));
                break;
            case "setting_size":
                preference.setSummary(sharedPreferences.getString(s, "16") + " sp");
                break;
            case "setting_color":
                preference.setSummary(sharedPreferences.getString(s, "Green"));
                break;
        }
    }

    private void loadSummary() throws NullPointerException{
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String[] keys = getResources().getStringArray(R.array.settings_key);
        for (String key : keys) {
            Preference preference = findPreference(key);
            String pref_val = sharedPreferences.getString(key, null);
            switch (key) {
                case "setting_lang":
                    preference.setSummary(pref_val);
                    break;
                case "setting_size":
                    preference.setSummary(pref_val + " sp");
                    break;
                case "setting_color":
                    preference.setSummary(pref_val);
                    break;
            }
        }
    }
}
