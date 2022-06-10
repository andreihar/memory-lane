package com.example.firstapp;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.firstapp.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class FirstFragment extends Fragment {

private FragmentFirstBinding binding;

    private TextView questionTV, questionNumberTV;
    private ImageView questionImage;
    private Button firstBtn, secondBtn, thirdBtn, fourthBtn, menuBtn;
    private ArrayList<QuestionBlock> quizModalArrayList;
    private TextToSpeech mTTS;
    private Random random;
    private int score = 0, questionBlockPosition = 0, questionPosition = 0;
    private boolean firstIncorrect = false, TTSOn = false;
    private MediaPlayer player; // Takes a lot of resources, create only where necessary

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the Layout for this Fragment
        View fragmentFirstLayout = inflater.inflate(R.layout.fragment_first, container, false);
        // Initialise TextToSpeech
        mTTS = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.UK);
                    if (result == TextToSpeech.LANG_MISSING_DATA // Check for supported languages
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        TTSOn = true;
                        TTSQuestion();
                    }
                } else {
                    Log.e("TTS", "Initialisation failed");
                }
            }
        });

        // Get the Count and Question TextView
        questionTV = fragmentFirstLayout.findViewById(R.id.question);
        questionNumberTV = fragmentFirstLayout.findViewById(R.id.questionNumber);
        // Get the Option Buttons
        firstBtn = fragmentFirstLayout.findViewById(R.id.first_choice);
        secondBtn = fragmentFirstLayout.findViewById(R.id.second_choice);
        thirdBtn = fragmentFirstLayout.findViewById(R.id.third_choice);
        fourthBtn = fragmentFirstLayout.findViewById(R.id.fourth_choice);
        menuBtn = fragmentFirstLayout.findViewById(R.id.back_to_menu);
        // Get the Image
        questionImage = fragmentFirstLayout.findViewById(R.id.imageView);

        // Get the Questions
        quizModalArrayList = new ArrayList<>();
        random = new Random();
        getQuizQuestion(quizModalArrayList); // Populate ArrayList with questions
        questionBlockPosition = random.nextInt(quizModalArrayList.size());
        setDataToViews();
        firstBtn.setOnClickListener(v -> {
            optionAction(quizModalArrayList, firstBtn, v);
        });
        secondBtn.setOnClickListener(v -> {
            optionAction(quizModalArrayList, secondBtn, v);
        });
        thirdBtn.setOnClickListener(v -> {
            optionAction(quizModalArrayList, thirdBtn, v);
        });
        fourthBtn.setOnClickListener(v -> {
            optionAction(quizModalArrayList, fourthBtn, v);
        });
        menuBtn.setOnClickListener(v -> {
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_MainMenuFragment);
        });

        return fragmentFirstLayout;
    }

    // Description: Updates Image, Options, QuestionTV after new Question is chosen.
    // Precondition: -
    // Postcondition: TextViews and Image are updated.
    private void setDataToViews() {
        firstBtn.setEnabled(true);
        secondBtn.setEnabled(true);
        thirdBtn.setEnabled(true);
        fourthBtn.setEnabled(true);
        firstIncorrect = false;
        questionNumberTV.setText(getString(R.string.answered_correctly, score));
        if (questionPosition == 5) {
            quizModalArrayList.remove(questionBlockPosition);
            if (quizModalArrayList.size() == 0) {
                firstBtn.setVisibility(View.GONE);
                secondBtn.setVisibility(View.GONE);
                thirdBtn.setVisibility(View.GONE);
                fourthBtn.setVisibility(View.GONE);
                questionTV.setText(getString(R.string.answered_all_questions));
                TTSQuestion();
                menuBtn.setVisibility(View.VISIBLE);
                return;
            }
            questionBlockPosition = random.nextInt(quizModalArrayList.size());
            questionPosition = 0;
        }
        questionImage.setImageResource(quizModalArrayList.get(questionBlockPosition).getImage());
        firstBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getFirst());
        secondBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getSecond());
        thirdBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getThird());
        fourthBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getFourth());
        questionTV.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getQuestion());
        TTSQuestion();
    }

    // Description: Increases Score, reduces number of Options, provides Hint,
    //              sets next Question.
    // Precondition: -
    // Postcondition: Fam I don't even know what to write here atm
    private void optionAction(@NonNull ArrayList<QuestionBlock> quizModalArrayList, @NonNull Button button, View v) {
        stopPlayer(player);
        mTTS.stop();
        if(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getAnswer().trim().equalsIgnoreCase(button.getText().toString().trim())) {
            ++score;
            ++questionPosition;
            setDataToViews();
        } else if (!firstIncorrect) {
            firstIncorrect = true;
            button.setEnabled(false);
            TTSQuestion();
        } else {
            ++questionPosition;
            setDataToViews();
        }
    }

    // Description: TTS of Question and Options on first iteration,
    //              Hint and remaining Options after incorrect answer.
    // Precondition: TTS option is ON in settings
    // Postcondition: TTS appropriate text strings
    private void TTSQuestion() {
        stopPlayer(player);
        System.out.println("The value of TTSOn = " + TTSOn);
        if (!TTSOn) {
            return;
        }
        mTTS.setPitch((float)1);
        mTTS.setSpeechRate((float)1);
        if (!firstIncorrect)
            mTTS.speak(questionTV.getText().toString(), TextToSpeech.QUEUE_ADD, null);
        if (firstIncorrect) {
            mTTS.speak("Hint: ", TextToSpeech.QUEUE_ADD, null);
            player = MediaPlayer.create(getActivity(), quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getHint());
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopPlayer(mediaPlayer);
                    TTSQuestionButton(firstBtn, 1);
                    TTSQuestionButton(secondBtn, 2);
                    TTSQuestionButton(thirdBtn, 3);
                    TTSQuestionButton(fourthBtn, 4);
                }
            });
        }
        else {
            TTSQuestionButton(firstBtn, 1);
            TTSQuestionButton(secondBtn, 2);
            TTSQuestionButton(thirdBtn, 3);
            TTSQuestionButton(fourthBtn, 4);
        }
    }

    // Description: TTS Options, indicates Options of text string.
    // Precondition: button is enabled
    // Postcondition: TTS appropriate text strings, highlighting Option
    //                of text string
    // Exceptions: Throws TTSSleepInterruptedException if sleep() got interrupted.
    private void TTSQuestionButton(@NonNull Button button, int i) {
        // BUG: Colour changes despite button being disabled
        // IMPLEMENT: Highlight button while speak() is in progress
        System.out.println(i + "is active: " + button.isEnabled());
        if (button.isEnabled() && button.getVisibility() == View.VISIBLE) {
            //button.setTextColor(getResources().getColor(R.color.highlight_button));
            mTTS.speak("Option" + i + ":" + button.getText().toString(), TextToSpeech.QUEUE_ADD, null);
            //button.setTextColor(getResources().getColor(R.color.white));
        }
    }

    // Description: Populates quizModalArrayList with questions.
    // Precondition: -
    // Postcondition: quizModalArray populated with questions
    private void getQuizQuestion(ArrayList<QuestionBlock> quizModalArrayList) {
        Question questionOne = new Question("Who is it?", "Mum", "Mate", "Me", "Cornershop bossman", "Me", R.raw.hint1);
        Question questionTwo = new Question("How old is he?", "16", "2", "9", "74", "74", R.raw.hint2);
        Question questionThree = new Question("What's his ethnicity?", "Asian", "Caucasian", "Mongol", "Kyrgyz", "Asian", R.raw.hint3);
        Question questionFour = new Question("His favourite UK rapper?", "Skepta", "Wiley", "Stormzy", "Himself", "Himself", R.raw.hint4);
        Question questionFive = new Question("It's Unknown T", "Homerton B", "Gyalie on me", "Op block", "Bali on me", "Homerton B", R.raw.hint5);
        quizModalArrayList.add(new QuestionBlock(questionOne, questionTwo, questionThree, questionFour, questionFive, R.drawable.daboy));

        questionOne = new Question("Dat boy is who?", "Future doc", "My G", "Taiwanren", "Burnaby S boy", "Taiwanren", R.raw.hint1);
        questionTwo = new Question("City of birth?", "Tainan", "Taipei", "Taichung", "Taoyuan", "Tainan", R.raw.hint2);
        questionThree = new Question("Calculus 12 grade?", "A", "A+", "A++", "A+++", "A+++", R.raw.hint3);
        questionFour = new Question("Most complex 漢字 in Unicode?", "\uD869\uDEA5", "\uD869\uDEA3", "\uD869\uDEA4", "\uD869\uDEA2", "\uD869\uDEA5", R.raw.hint4);
        questionFive = new Question("Anthem of Taiwan?", "San Min Zhuyi", "Yiyongjun Jinxingqu", "Gong Jin'ou", "Wuzu gonghe ge", "San Min Zhuyi", R.raw.hint5);
        quizModalArrayList.add(new QuestionBlock(questionOne, questionTwo, questionThree, questionFour, questionFive, R.drawable.taiwanren));

        questionOne = new Question("Who dat?", "My mate on teli", "Trump", "MP of UK", "Alien", "My mate on teli", R.raw.hint1);
        questionTwo = new Question("What is he famous for?", "Tallest man", "Nothing", "best nba player", "Everything", "Nothing", R.raw.hint2);
        questionThree = new Question("It's 3 am I wanna go sleep", "The last", "Questions", "Won't make", "Any sense", "Any sense", R.raw.hint3);
        questionFour = new Question("?", "=(", "=)", "=/", "=|", "=)", R.raw.hint4);
        questionFive = new Question("Best anime?", "Boku no piku", "Initial D", "Oreimo", "Pokemon", "Initial D", R.raw.hint5);
        quizModalArrayList.add(new QuestionBlock(questionOne, questionTwo, questionThree, questionFour, questionFive, R.drawable.teliman));

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // Description: Deallocates resources allocated for mediaPlayer
    // Precondition: mediaPlayer exists
    // Postcondition: Resources are released
    private void stopPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    // Description: Disconnects from TTS engine
    // Precondition: mTTS exists
    // Postcondition: App disconnected from TTS engine
@Override
    public void onDestroyView() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroyView();
        binding = null;
    }
}