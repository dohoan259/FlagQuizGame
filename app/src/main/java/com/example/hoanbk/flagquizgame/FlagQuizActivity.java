package com.example.hoanbk.flagquizgame;

import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class FlagQuizActivity extends AppCompatActivity {
    private static final String TAG = "FlagQuizActivity";

    private final int CHOICES_MENU_ID = Menu.FIRST;
    private final int REGIONS_MENU_ID = Menu.FIRST + 1;

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // names of countries in quiz
    private Map<String, Boolean> regionsMap; // which regions are enabled
    private String correctAnswer;
    private int totalGuesses; // number of guesses made
    private int correctAnswers;
    private int guessRows;
    private Random random;
    private Handler handler;
    private Animation shakeAnimation;

    private TextView answerTextView;
    private TextView questionNumberTextView;
    private ImageView flagImageView;
    private TableLayout buttonTableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flag_quiz);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        regionsMap = new HashMap<>();
        guessRows = 1;
        random = new Random();
        handler = new Handler();

        // load shake animation
        shakeAnimation =
                AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        // get array of world regions from string xml
        String[] regionsName = getResources().getStringArray(R.array.regionsList);

        // by default, countries are chosen from all regions
        for (String region : regionsName) {
            regionsMap.put(region, true);
        }

        // get references to GUI components
        questionNumberTextView = (TextView)findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView)findViewById(R.id.flagImageView);
        buttonTableLayout = (TableLayout)findViewById(R.id.buttonTableLayout);
        answerTextView = (TextView)findViewById(R.id.answerTextView);

        // set question
        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " 1 " +
                        getResources().getString(R.string.of) + " 10"
        );

        resetQuiz();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, R.string.choices);
        menu.add(Menu.NONE, REGIONS_MENU_ID, Menu.NONE, R.string.regions);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CHOICES_MENU_ID:
                // create a list of the possible numbers of answer choices
                final String[] possibleChoices =
                        getResources().getStringArray(R.array.guessesList);

                // create a new AlertDialog Builder and set its title
                AlertDialog.Builder choicesBuilder =
                        new AlertDialog.Builder(this);
                choicesBuilder.show();

                // add possibleChoices items to the Dialog ans set the
                // behavior when one of the items is clicked
                choicesBuilder.setItems(R.array.guessesList,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // update guessRows to match the user's choice
                                guessRows = Integer.parseInt(
                                        possibleChoices[which].toString()) / 3;
                                resetQuiz(); // reset the quiz
                            } // end anonymous inner class
                        }); // and call to setItems

                // create an AlertDialog from the Builder
                AlertDialog choicesDialog = choicesBuilder.create();
                choicesDialog.show();
                return true;
            case REGIONS_MENU_ID:
                // get array of world regions
                final String[] regionNames =
                        regionsMap.keySet().toArray(new String[regionsMap.size()]);

                // boolean array
                boolean[] regionsEnabled = new boolean[regionsMap.size()];
                for (int i = 0; i < regionsEnabled.length; ++i) {
                    regionsEnabled[i] = regionsMap.get(regionNames[i]);
                }

                // create an AlertDialog Builder
                AlertDialog.Builder regionsBuilder = new AlertDialog.Builder(this);
                regionsBuilder.setTitle(R.string.regions);

                // replace _with space in region names for display purposes
                String[] displayNames = new String[regionNames.length];
                for (int i = 0; i < regionNames.length; ++i) {

                }

                // add displayNames to the Dialog and set the behavior
                // when one of the items is clicked
                regionsBuilder.setMultiChoiceItems(displayNames, regionsEnabled,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                // include or exclude the clicked region
                                // depending on whether or not it's checked
                                regionsMap.put(
                                        regionNames[which].toString(), isChecked);
                            }
                        });

                // resets quiz when user presses the "Reset Quiz" Button
                regionsBuilder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resetQuiz();
                            }
                        });
                // create a dialog from the Builder
                AlertDialog regionsDialog = regionsBuilder.create();
                regionsDialog.show();
                return true;
        } // end switch

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            submitGuess((Button) v);
        }
    };

    private void resetQuiz() {

        // use the AssetManager to get the image flag
        AssetManager assets = getAssets();
        fileNameList.clear();

        try {
            Set<String> regions = regionsMap.keySet(); // get Set of regions

            // loop through each region
            for (String region : regions) {
                if (regionsMap.get(region)) {
                    // if region is enabled
                    String[] paths = assets.list(region);

                    for(String path : paths) {
                        fileNameList.add(path.replace(".png", ""));
                    }
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error loading image file name", ioe);
        }

        int correctAnswer = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        // add 10 random file Names
        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        while (flagCounter <= 10) {
            int randomIndex = random.nextInt(numberOfFlags);

//            get random file name
            String fileName = fileNameList.get(randomIndex);

            // if region is enabled
            if (!quizCountriesList.contains(fileName)) {
                quizCountriesList.add(fileName);
                ++ flagCounter;
            }
        }

        loadNextFlag();
    }

    private void loadNextFlag() {
        // get file name of the next flag and remove it from list
        String nextImageName = quizCountriesList.remove(0);
        correctAnswer = nextImageName; // update the correct Answer

        answerTextView.setText("");

        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " " +
                        (correctAnswer + 1) + " " +
                        getResources().getString(R.string.of) + " 10"
        );

        // extract the region from the next image' name
        String region =
                nextImageName.substring(0, nextImageName.indexOf("-"));

        // use AssetManager to load next image from assets folder
        AssetManager assets = getAssets();
        InputStream stream;

        try{
            // get an InputStream to the asset
            stream = assets.open(region + "/" + nextImageName + ".png");

            // load asset as Drawable
            Drawable flag = Drawable.createFromStream(stream, nextImageName);
            flagImageView.setImageDrawable(flag);
        } catch (IOException ioe) {
            Log.e(TAG, "Error loading", ioe);
        }

        // clear prior answer
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();
        }
            Collections.shuffle(fileNameList); // shuffle file names

            // put the correct answer at the end of fileNameList
            int correct = fileNameList.indexOf(correctAnswer);
            fileNameList.add(fileNameList.remove(correct));

            // get a reference to the
            LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

            // add 3, 6 or 9 answer Button
            for (int row = 0; row < guessRows; row++) {
                TableRow currentTableRow = getTableRow(row);

                for (int column = 0; column < 3; column++) {
                    Button newGuessButton = (Button)inflater.inflate(R.layout.guess_button, null);

                    // get country name
                    String fileName = fileNameList.get((row*3) + column);
                    newGuessButton.setText(getCountryName(fileName));

                    // register
                    newGuessButton.setOnClickListener(guessButtonListener);
                    currentTableRow.addView(newGuessButton);
                }
            }

            int row = random.nextInt(guessRows);
            int column = random.nextInt(3);
            TableRow randomTableRow = getTableRow(row);
            String countryName = getCountryName(correctAnswer);
            ((Button)randomTableRow.getChildAt(column)).setText(countryName);
    }

    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    }

    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    private void submitGuess(Button guessButton) {
        String guess = guessButton.getText().toString();
        String answer = getCountryName(correctAnswer);
        ++totalGuesses; // increment the number of guesses the user has made

        // if the guess is correct
        if (guess.equals(answer)) {
            ++correctAnswers;

            // display "Correct!"
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.correct_answer));
            disableButtons(); // disable all Button

            // if the user has correctly identified 10 flags
            if (correctAnswers == 10) {
                // create a new AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.reset_quiz); // title bar string

                // set the AlertDialog's message to display game results
                builder.setMessage(String.format("%d %s, %.02f%% %s",
                        totalGuesses, getResources().getString(R.string.guesses),
                        (1000 / (double) totalGuesses)));

                builder.setCancelable(true);

                // add "Reset Quiz" Button
                builder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resetQuiz();
                            }
                        });
                // create AlertDialog from the Builder
                AlertDialog resetDialog = builder.create();
                resetDialog.show();
            } else { // answer is correct but quiz is not over
                // load the next flag after a 1-second delay
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                loadNextFlag();
                            }
                        }, 1000
                );
            } // end else
        } else { // end if
            // play the animation
            flagImageView.startAnimation(shakeAnimation);

            // display "Incorrect!"
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer)
            );
            guessButton.setEnabled(false); // disable the incorrect answer
        } // end else
    }// end method

    // utility method that disables all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
            TableRow tableRow = (TableRow)buttonTableLayout.getChildAt(row);
            for (int i = 0; i < tableRow.getChildCount(); ++i) {
                tableRow.getChildAt(i).setEnabled(false);
            }
        }
    }
}
