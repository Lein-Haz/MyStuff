package com.paz5x4.android.flagquiz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class QuizFragment extends Fragment {
    // String used when logging error messages
    private static final String TAG = "FlagQuiz Activity";
    private int quizzesCompleted;

    private int totalQuestions;
    private int globalTotalGuesses; // number of guesses made
    private int globalCorrectAnswers; // number of correct guesses

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // countries in current quiz
    private Set<String> regionsSet; // world regions in current quiz
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // number of rows displaying guess Buttons
    private Random random; // used to randomize the quiz
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess

    private TextView questionNumberTextView; // shows current question #
    private ImageView flagImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView; // displays Correct! or Incorrect!

    private QuizListener listener;

    public interface QuizListener
    {
        void callUnregister();
        void callRegister();
    }


    @Override
    public void onDetach()
    {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        listener = (QuizListener) activity;
    }

    // configures the QuizFragment when its View is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        //called onCreateView so that we have header label info
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
        //something weird happened when I cast it right to an int and shared pref weren't created yet
        String temp = sharedPreferences.getString(getString(R.string.pref_numberOfQuestions), "5");
        totalQuestions = Integer.valueOf(temp);


        fileNameList = new ArrayList<String>();
        quizCountriesList = new ArrayList<String>();
        random = new Random();
        handler = new Handler();

        // load the shake animation that's used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // animation repeats 3 times

        // get references to GUI components
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[3];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }


        // set questionNumberTextView's text
        questionNumberTextView.setText(getResources().getString(R.string.question, 1, totalQuestions));
        return view; // returns the fragment's view for display
    } // end method onCreateView

    // update guessRows based on value in SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get the number of guess buttons that should be displayed
        String choices =
                sharedPreferences.getString(getString(R.string.pref_numberOfChoices), null);
        guessRows = Integer.parseInt(choices) / 3;

        // hide all guess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.INVISIBLE);

        // display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    // update world regions for quiz based on values in SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences) {
        regionsSet =
                sharedPreferences.getStringSet(getString(R.string.pref_regionsToInclude), null);
    }

    // set up and start the next quiz
    public void resetQuiz() {
        // use AssetManager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // empty list of image file names


        //handles resetting quiz to defaults
        boolean forceDefaults = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        forceDefaults = sharedPreferences.getBoolean(getString(R.string.pref_forceDefaults), false);
        if (forceDefaults)
        {
            listener.callUnregister();
            /*spEditor.remove(getString(R.string.pref_numberOfChoices));
            spEditor.remove(getString(R.string.pref_numberOfQuestions));
            spEditor.remove(getString(R.string.pref_regionsToInclude));*/
            spEditor.remove(getString(R.string.pref_numberOfChoices));
            spEditor.remove(getString(R.string.pref_numberOfQuestions));
            spEditor.remove(getString(R.string.pref_regionsToInclude));
            spEditor.apply();
            PreferenceManager.setDefaultValues(this.getActivity().getApplicationContext(), R.xml.preferences, true);
            listener.callRegister();
            //spEditor.clear().apply();
        }
        totalQuestions = Integer.valueOf(sharedPreferences.getString(getString(R.string.pref_numberOfQuestions), "3"));


        try {
            // loop through each region
            for (String region : regionsSet) {
                // get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        } catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

        correctAnswers = 0; // reset the number of correct answers made
        totalGuesses = 0; // reset the total number of guesses the user made
        quizCountriesList.clear(); // clear prior list of quiz countries

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // add totalQuestions random file names to the quizCountriesList
        while (flagCounter <= totalQuestions) {
            int randomIndex = random.nextInt(numberOfFlags);

            // get the random file name
            String fileName = fileNameList.get(randomIndex);

            // if the region is enabled and it hasn't already been chosen
            if (!quizCountriesList.contains(fileName)) {
                quizCountriesList.add(fileName); // add the file to the list
                ++flagCounter;
            }
        }

        loadNextFlag(); // start the quiz by loading the first flag
    } // end method resetQuiz

    // after the user guesses a correct flag, load the next flag
    private void loadNextFlag() {
        // get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; // update the correct answer
        answerTextView.setText(""); // clear answerTextView

        // display current question number
        questionNumberTextView.setText(
                getResources().getString(R.string.question,
                        (correctAnswers + 1), totalQuestions));

        // extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        // use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        try {
            // get an InputStream to the asset representing the next flag
            InputStream stream =
                    assets.open(region + "/" + nextImage + ".png");

            // load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
        } catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        // put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 3, 6, or 9 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            // place Buttons in currentTableRow
            for (int column = 0;
                 column < guessLinearLayouts[row].getChildCount(); column++) {
                // get reference to Button to configure
                Button newGuessButton =
                        (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getCountryName(fileName));
            }
        }

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(3); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    } // end method loadNextFlag

    // parses the country flag file name and returns the country name
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    // called when a guess Button is touched
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //for incrementing global stats in sharedprefs
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            SharedPreferences.Editor editor= sharedPreferences.edit();



            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses; // increment number of guesses the user has made
            globalTotalGuesses++; // increment the running total and commit
            editor.putInt(getString(R.string.pref_globalGuesses), globalTotalGuesses).commit();

            if (guess.equals(answer)) // if the guess is correct
            {
                ++correctAnswers; // increment the number of correct answers
                globalCorrectAnswers++; // increment running correct answers and commit
                editor.putInt(getString(R.string.pref_globalCorrect), globalCorrectAnswers).commit();

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(
                        getResources().getColor(R.color.correct_answer));

                disableButtons(); // disable all guess Buttons

                // if the user has correctly identified totalQuestions flags
                if (correctAnswers == totalQuestions) {
                    // DialogFragment to display quiz stats and start new quiz
                    DialogFragment quizResults =
                            new DialogFragment() {
                                // create an AlertDialog and return it
                                @Override
                                public Dialog onCreateDialog(Bundle bundle) {
                                    AlertDialog.Builder builder =
                                            new AlertDialog.Builder(getActivity());
                                    builder.setCancelable(false);

                                    builder.setMessage(
                                            getResources().getString(R.string.results,
                                                    totalGuesses, (100 * totalQuestions / (double) totalGuesses)));

                                    // "Reset Quiz" Button
                                    builder.setPositiveButton(R.string.reset_quiz,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id)
                                                {
                                                    //update and then reset
                                                    updateRegions(PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()));
                                                    updateGuessRows(PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()));
                                                    quizzesCompleted++;//increment completed quizzes
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putInt(getString(R.string.pref_quizzesCompleted), quizzesCompleted).commit();
                                                    resetQuiz();
                                                }
                                            } // end anonymous inner class
                                    ); // end call to setPositiveButton

                                    return builder.create(); // return the AlertDialog
                                } // end method onCreateDialog
                            }; // end DialogFragment anonymous inner class

                    // use FragmentManager to display the DialogFragment
                    quizResults.show(getFragmentManager(), "quiz results");
                    quizzesCompleted++;
                } else // answer is correct but quiz is not over
                {
                    // load the next flag after a n-second delay
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    loadNextFlag();
                                }
                            }, 2000); // 2000 milliseconds for 2-second delay
                }
            } else // guess was incorrect
            {
                flagImageView.startAnimation(shakeAnimation); // play shake

                // display "Incorrect!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(
                        getResources().getColor(R.color.incorrect_answer));
                guessButton.setEnabled(false); // disable incorrect answer
            }
        } // end method onClick
    }; // end answerButtonListener

    // utility method that disables all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
    //sees if restart should happen immediately
    public void resetChecker()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
        boolean resetCheck = sharedPreferences.getBoolean(getString(R.string.pref_resetCheck), false);
        if(!resetCheck)
        {
            updateRegions(sharedPreferences);
            updateGuessRows(sharedPreferences);
            resetQuiz();
            Toast.makeText(this.getActivity().getApplicationContext(), R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }else //Gives message that settings changes will be applied on restart instead
        {
            Toast.makeText(this.getActivity().getApplicationContext(), R.string.set_on_quiz_restart, Toast.LENGTH_SHORT).show();
        }
    }

    public int getGlobalTotalGuesses()
    {
        return globalTotalGuesses;
    }

    public int getGlobalCorrectAnswers()
    {
        return globalCorrectAnswers;
    }
}
