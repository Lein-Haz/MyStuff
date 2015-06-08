// SettingsActivity.java
// Activity to display SettingsFragment on a phone
package com.paz5x4.android.flagquiz;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity
{
   // use FragmentManager to display SettingsFragment
   @Override
   protected void onCreate(Bundle savedInstanceState) 
   {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.fragment_settings);
   }
} // end class SettingsActivity