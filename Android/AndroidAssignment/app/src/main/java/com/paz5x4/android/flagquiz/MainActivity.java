package com.paz5x4.android.flagquiz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends ActionBarActivity implements QuizFragment.QuizListener
{
    // keys for reading data from SharedPreferences have all been changed to resource strings

    private boolean phoneDevice = true; // used to force portrait mode
    private boolean preferencesChanged = true; // did preferences change?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set default values in the app
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // register listener for SharedPreferences changes.
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // determine screen size
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        // if device is a tablet, set phoneDevice to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false; // not a phone-sized device

        // if running on phone-sized device, allow only portrait orientation
        if (phoneDevice)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } // end method onCreate

    // called after onCreate completes execution
    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged) {
            // now that the default preferences have been set,
            // initialize QuizFragment and start the quiz
            QuizFragment quizFragment = (QuizFragment) getFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    } // end method onStart

    // always show menu, if app is running on a phone or a portrait-oriented tablet then make menu item "settings" visible
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // get the default Display object representing the screen
        boolean ret = false;
        int orientation = getResources().getConfiguration().orientation;
        getMenuInflater().inflate(R.menu.menu_main, menu); // always inflate the menu since we have new options
        ret = true;
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                menu.findItem(R.id.action_settings).setVisible(true);//settings is invisible and disabled by default
                menu.findItem(R.id.action_settings).setEnabled(true);//make available for phone and portrait
                ret = true;
                break;
        }
        return ret;
    } // end method onCreateOptionsMenu

    //displays overall stats
    private void displayStats()
    {
        final int guesses = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt(getString(R.string.pref_globalGuesses), 1);//so we dont divide by 0
        final int correct = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt(getString(R.string.pref_globalCorrect), 0);
        final int quizzes = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt(getString(R.string.pref_quizzesCompleted), 0);

        final float percentCorrect;
        if ((100 * correct / (double) guesses) > 0)
        {
            percentCorrect = (float)(100 * correct / (double) guesses);
        }else
        {
            percentCorrect = 0.0f;
        }

        DialogFragment dialogFragment = new DialogFragment()
        {
            @Override
            public Dialog onCreateDialog(Bundle bundle)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Overall Stats");
                builder.setMessage(getResources().getString(R.string.global_results,
                        quizzes,guesses, correct, percentCorrect));
                builder.setPositiveButton(R.string.dialog_ok,new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });

                return builder.create();
            }

        };
        dialogFragment.show(getFragmentManager(), "dialogFragment");
    }

    //clears global stats out of sharedpreferences
    private void clearStats()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.pref_globalCorrect), 0);
        editor.putInt(getString(R.string.pref_globalGuesses), 0);
        editor.putInt(getString(R.string.pref_quizzesCompleted), 0);
        editor.commit();
    }

    /* displays SettingsActivity when running on a tablet handles menu clicks
    I had to read up on fragments cause I had no idea how this was initially being called,
    once I realized it always gets called if you have fragments it made sense
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                Intent preferencesIntent = new Intent(this, SettingsActivity.class);
                startActivity(preferencesIntent);
                return true;
            case R.id.action_reset_stats:
                clearStats();
                return true;
            case R.id.action_show_stats:
                displayStats();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // listener for changes to the app's SharedPreferences
    private OnSharedPreferenceChangeListener preferenceChangeListener =
            new OnSharedPreferenceChangeListener() {
                // called when the user changes the app's preferences
                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key) {
                    preferencesChanged = true; // user changed app settings

                    QuizFragment quizFragment = (QuizFragment) getFragmentManager().findFragmentById(R.id.quizFragment);

                    if (key.equals(getString(R.string.pref_numberOfChoices))) // # of choices to display changed
                    {
                            quizFragment.resetChecker();//checks for resetoption before calling resetQuiz()
                    } else if (key.equals(getString(R.string.pref_regionsToInclude))) // regions to include changed
                    {
                        Set<String> regions = sharedPreferences.getStringSet(getString(R.string.pref_regionsToInclude), null);

                        if (regions != null && regions.size() > 0)
                        {
                                quizFragment.resetChecker();//checks for resetoption before calling resetQuiz()
                        } else // must select one region--set North America as default
                        {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            regions.add(getResources().getString(R.string.default_region));
                            editor.putStringSet(getString(R.string.pref_regionsToInclude), regions);
                            editor.apply();
                            Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                        }
                    } else if(key.equals(getString(R.string.pref_forceDefaults)))//disable default loading
                    {
                        Toast.makeText(MainActivity.this, R.string.defaults_on_quiz_restart, Toast.LENGTH_SHORT).show();
                    } else if (key.equals(getString(R.string.pref_resetCheck)))
                    {
                        Toast.makeText(MainActivity.this, R.string.set_on_quiz_restart, Toast.LENGTH_SHORT).show();
                    } else if (key.equals(getString(R.string.pref_numberOfQuestions)))
                    {
                        //updates value in sharedpreferences
                        SharedPreferences.Editor spEditor = sharedPreferences.edit();
                        spEditor.putString(getString(R.string.pref_numberOfQuestions),
                                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(R.string.pref_numberOfQuestions), "5"));
                        //the change from my first submit was commenting out the line of code above.
                        quizFragment.resetChecker();
                    }


                } // end method onSharedPreferenceChanged
            }; // end anonymous inner class

    @Override
    public void callUnregister()
    {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void callRegister()
    {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
} // end class MainActivity


