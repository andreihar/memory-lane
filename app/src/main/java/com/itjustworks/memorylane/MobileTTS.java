package com.itjustworks.memorylane;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

/*
 * MobileTTS.java
 *
 * Class Description: Converts text of the view into speech.
 * Class Invariant: Language of the TTS depends on the default language and default country.
 *                  zh-TW for Chinese (Taiwan).
 *                  yue for Chinese (Hong Kong) and Chinese (Macau).
 *                  zh for Chinese.
 *                  pa for Punjabi
 *                  fr for French
 *                  en else
 *
 */

public class MobileTTS  {
    static class SwitchBtnColour extends UtteranceProgressListener {
        private final Handler handler = new Handler(message -> {
            Button btn=(Button) message.obj;
            if (message.what == 1)
                btn.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.highlight_button));
            else if (message.what == 0)
                btn.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.button_background));
            return true;
        });
        public SwitchBtnColour() {
        }

        private final Queue<Button> btnQueue=new LinkedList<>();
        public synchronized void AddButton(Button _btn) {
            synchronized (btnQueue) {
                btnQueue.add(_btn);
            }
        }

        @Override
        public void onStart(String s) {
            synchronized (btnQueue) {
                if (!btnQueue.isEmpty()) {
                    Message message = new Message();
                    message.what=1;
                    message.obj=btnQueue.peek();
                    handler.sendMessage(message);
                }
            }
        }

        @Override
        public void onDone(String s) {
            synchronized (btnQueue) {
                if (!btnQueue.isEmpty()) {
                    Message message = new Message();
                    message.what=0;
                    message.obj=btnQueue.poll();
                    //if (btnQueue.peek()!=null)
                    handler.sendMessage(message);
                }
            }
        }

        @Override
        public void onError(String s) {

        }
    }

    private final Activity activity;
    private final Context context;
    private final boolean TTSOn;//, hintOn;
    private final ArrayList<QuestionSet> quizModalArrayList;
    private final Button firstBtn, secondBtn, thirdBtn, fourthBtn;
    public TextToSpeech mobileTextToSpeech;
    private final Romanisation r;
    private float pitch, speed;
    public SwitchBtnColour switchBtnColour;

    public MobileTTS(Activity activity, Context context, ArrayList<QuestionSet> quizModalArrayList,
                     Button firstBtn, Button secondBtn, Button thirdBtn, Button fourthBtn, Romanisation r) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        pitch = (float)prefs.getInt("seek_bar_pitch", 50) / 50;
        speed = (float)prefs.getInt("seek_bar_speed", 50) / 50;
        TTSOn = prefs.getBoolean("tts_on", true);
        //hintOn = prefs.getBoolean("hint_text", true);
        if (pitch < 0.1)
            pitch = 0.1f;
        if (speed < 0.1)
            speed = 0.1f;
        switchBtnColour  = new SwitchBtnColour();
        this.activity = activity;
        this.context = context;
        this.quizModalArrayList = quizModalArrayList;
        this.firstBtn = firstBtn;
        this.secondBtn = secondBtn;
        this.thirdBtn = thirdBtn;
        this.fourthBtn = fourthBtn;
        this.r = r;
    }

    // Description: TTS of Question and Options on first iteration,
    //              Hint and remaining Options after incorrect answer.
    // Precondition: TTS option is ON in settings
    // Postcondition: TTS appropriate text strings
    public void TTSQuestion(TextToSpeech mTTS, HintPlayer player, int questionSetPosition, int questionPosition, boolean firstIncorrect, boolean secondIncorrect, int[] optionNumber) {
        if (player != null)
            player.stopPlayer();
        if (!firstIncorrect && TTSOn) {
            mTTS.speak(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getQuestion(), TextToSpeech.QUEUE_ADD, null, null);
        }
        if (firstIncorrect || secondIncorrect) {
            player.displayToastHint(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getHintText(), activity), secondIncorrect);
            int duration = sayHint(mTTS);
            // Stall TTS
            new Handler().postDelayed(() -> { // Less wasteful than Thread.sleep()
                MobileTTS newMTTS = new MobileTTS(activity, context, quizModalArrayList, firstBtn, secondBtn, thirdBtn, fourthBtn, r);
                player.startPlayer(newMTTS, mTTS, questionSetPosition, questionPosition, firstBtn, secondBtn, thirdBtn, fourthBtn, optionNumber);
            }, duration);
        } else {
            switchBtnColour=new SwitchBtnColour();
            mTTS.setOnUtteranceProgressListener(switchBtnColour);
            TTSQuestionButton(mTTS, firstBtn, 1, questionSetPosition, questionPosition, optionNumber);
            TTSQuestionButton(mTTS, secondBtn, 2, questionSetPosition, questionPosition, optionNumber);
            TTSQuestionButton(mTTS, thirdBtn, 3, questionSetPosition, questionPosition, optionNumber);
            TTSQuestionButton(mTTS, fourthBtn, 4, questionSetPosition, questionPosition, optionNumber);
        }
    }

    // Description: TTS of Options, indicates Options of text string.
    // Precondition: Button is enabled, TTS option is ON in settings
    // Postcondition: TTS appropriate text strings, highlighting Option
    //                of text string
    public void TTSQuestionButton(TextToSpeech mTTS, @NonNull Button button, int i, int questionSetPosition, int questionPosition, int[] optionNumber) {
        if (i == 1) {
            switchBtnColour = new SwitchBtnColour();
            mTTS.setOnUtteranceProgressListener(switchBtnColour);
        }
        if (TTSOn) {
            if (button.isEnabled() && button.getVisibility() == View.VISIBLE) {
                switchBtnColour.AddButton(button);
                mTTS.speak(context.getString(R.string.option_tts) + optionNumber[i - 1] + ":" + quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getOptions(i - 1), TextToSpeech.QUEUE_ADD, null,"id");
            } else {
                if (optionNumber[i-1] == 0)
                    return;
                optionNumber[i-1] = 0;
                for (int j = i; j < 4; j++)
                    --optionNumber[j];
            }
        }
    }

    // Description: TTS of Option of Button
    // Precondition: TTS option is ON in settings
    // Postcondition: TTS appropriate text strings, highlighting Button
    public void TTSQuestionButtonLongClick(TextToSpeech mTTS, @NonNull Button button, int i, int questionSetPosition, int questionPosition, int[] optionNumber) {
        // setOnUtteranceProgressListener is used to follow the progress of voice playback
        if (TTSOn) {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(i));
            mTTS.speak(context.getString(R.string.option_tts) + optionNumber[i - 1] + ":" + quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getOptions(i - 1), TextToSpeech.QUEUE_ADD, params);
            mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    // runOnUiThread is used for updating the UI
                    activity.runOnUiThread(() -> button.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.highlight_button)));
                }

                @Override
                public void onDone(String s) {
                    activity.runOnUiThread(() -> button.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.button_background)));
                }

                @Override
                public void onError(String s) {
                    Log.e("ddd", "onError: " );
                }
            });
        }
    }

    // Description: Says "Hint" and stalls hint player
    // Precondition: TTS option is ON in settings
    // Postcondition: Stalls hint player for enough time to say "Hint" for all 5 languages
    public int sayHint(TextToSpeech mTTS) {
        if (TTSOn) {
            mTTS.speak(context.getString(R.string.hint_tts), TextToSpeech.QUEUE_ADD, null, null);
            return (int)(500*(2-speed));
        }
        return 0;
    }

    // Description: Initialises TTS for appropriate language
    // Precondition: Android system has a valid language, googleTTSPackage is working
    // Postcondition: Connected TTS to googleTTS
    public TextToSpeech initialiseTextToSpeechEngine(int questionSetPosition, int questionPosition) {
        String googleTtsPackage = "com.google.android.tts";
        mobileTextToSpeech = new TextToSpeech(activity, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result;
                switch(Locale.getDefault().getLanguage()) {
                    case "zh":
                        if (Locale.getDefault().getCountry().equals("TW"))
                            result = mobileTextToSpeech.setLanguage(new Locale("zh", "TW"));
                        else if (Locale.getDefault().getCountry().equals("HK")
                                || Locale.getDefault().getCountry().equals("MO"))
                            result = mobileTextToSpeech.setLanguage(new Locale("yue", "HK"));
                        else
                            result = mobileTextToSpeech.setLanguage(new Locale("zh", "CN"));
                        break;
                    case "pa":
                        result = mobileTextToSpeech.setLanguage(new Locale("pa", "IN"));
                        break;
                    case "fr":
                        result = mobileTextToSpeech.setLanguage(new Locale("fr"));
                        break;
                    default:
                        result = mobileTextToSpeech.setLanguage(new Locale("en", "GB"));
                        break;
                }
                if (result == TextToSpeech.LANG_MISSING_DATA // Check for supported languages
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported, set to default");
                    mobileTextToSpeech.setLanguage(new Locale("en", "GB"));
                }
                if (quizModalArrayList != null) {
                    TTSQuestion(mobileTextToSpeech, null, questionSetPosition, questionPosition, false, false, new int[]{1, 2, 3, 4});
                }
            } else {
                Log.e("TTS", "Initialisation failed");
            }
        }, googleTtsPackage);
        mobileTextToSpeech.setPitch(pitch);
        mobileTextToSpeech.setSpeechRate(speed);
        return mobileTextToSpeech;
    }

    // Description: Plays the
    // Precondition: TTSOn = true
    // Postcondition: The toast is displayed
    public void speak(TextToSpeech mTTS, String str) {
        if (TTSOn) {
            mTTS.speak(str, TextToSpeech.QUEUE_ADD, null, null);
        }
    }
}
