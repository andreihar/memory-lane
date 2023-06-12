package com.itjustworks.memorylane;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;

/*
 * HintPlayer.java
 *
 * Class Description: Plays the audio hint and displays the text hint.
 * Class Invariant: Uses MediaPlayer to play the mp3 hint file.
 *                  Uses Toast to show the hint given as a String.
 *
 */

public class HintPlayer {
    private final Activity activity;
    private MediaPlayer player; // Takes a lot of resources, create only where necessary
    private final boolean hintOn;
    private final ArrayList<QuestionSet> quizModalArrayList;

    public HintPlayer(Activity activity, ArrayList<QuestionSet> quizModalArrayList) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getAppContext());
        this.activity = activity;
        hintOn = prefs.getBoolean("hint_text", true);
        this.quizModalArrayList = quizModalArrayList;
    }

    // Description: Plays hint along with remaining options
    // Precondition: -
    // Postcondition: Hint and remaning options are played
    public void startPlayer(MobileTTS mobileTTS, TextToSpeech mTTS, int questionSetPosition, int questionPosition, Button firstBtn, Button secondBtn, Button thirdBtn, Button fourthBtn, int[] optionNumber) {
        String hint = quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getHint();
        Uri uri = Uri.parse(hint);
        player = MediaPlayer.create(activity,uri);
        player.start();
        player.setOnCompletionListener(mp -> {
            stopPlayer();
            mobileTTS.TTSQuestionButton(mTTS, firstBtn, 1, questionSetPosition, questionPosition, optionNumber);
            mobileTTS.TTSQuestionButton(mTTS, secondBtn, 2, questionSetPosition, questionPosition, optionNumber);
            mobileTTS.TTSQuestionButton(mTTS, thirdBtn, 3, questionSetPosition, questionPosition, optionNumber);
            mobileTTS.TTSQuestionButton(mTTS, fourthBtn, 4, questionSetPosition, questionPosition, optionNumber);
        });
    }

    // Description: Displays the Toast with the hint
    // Precondition: The user answered incorrectly,
    // Postcondition: The toast is displayed
    public void displayToastHint(String hint, boolean secondIncorrect) {
        if (hintOn) {
            if (secondIncorrect)
                Toast.makeText(activity, hint, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(activity, hint, Toast.LENGTH_SHORT).show();
        }
    }

    public void startHint(int questionSetPosition, int questionPosition) {
        String hint = quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getHint();
        Uri uri = Uri.parse(hint);
        player = MediaPlayer.create(activity,uri);
        player.start();
    }

    // Description: Deallocates resources allocated for mediaPlayer
    // Precondition: mediaPlayer exists
    // Postcondition: Resources are released
    public void stopPlayer() {
        if (player != null)
            player.release();
    }
}
