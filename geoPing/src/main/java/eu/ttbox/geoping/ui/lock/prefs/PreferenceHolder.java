package eu.ttbox.geoping.ui.lock.prefs;


import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

/**
        * Helper interface for using same functionalities provided by
        * {@link PreferenceActivity} and {@link PreferenceFragment}.
        *
        * @author Hai Bison
        *
        */
public interface PreferenceHolder {

    /**
     * Wrapper for {@link PreferenceActivity#findPreference(CharSequence)} and
     * {@link PreferenceFragment#findPreference(CharSequence)}.
     *
     * @param key
     *            the preference's key.
     * @return the {@link Preference} object, or {@code null} if not found.
     * @see PreferenceActivity#findPreference(CharSequence)
     * @see PreferenceFragment#findPreference(CharSequence)
     */
    Preference findPreference(CharSequence key);

}
