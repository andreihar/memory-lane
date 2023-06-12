package com.itjustworks.memorylane;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.itjustworks.memorylane.FirstFragmentArgs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

/*
 * FirstFragment.java
 *
 * Class Description: Application quiz system.
 * Class Invariant: Biased randomised ordering
 *                  Firebase can't be empty
 *
 */

public class FirstFragment extends Fragment {

    private TextView questionTV, noConnectivity;
    private ImageView questionImage, noConnectivityIcon;
    private Button firstBtn, secondBtn, thirdBtn, fourthBtn, menuBtn, nextQuestionBtn, nextQuestionSetBtn;
    private String quizType;
    private FloatingActionButton playHint;
    private ArrayList<QuestionSet> quizModalArrayList, changesInWeights;
    private TextToSpeech mobileTTS;
    private MobileTTS mTTS;
    private HintPlayer player;
    private Romanisation r;
    private int questionSetPosition = 0, questionPosition = 0;
    private boolean firstIncorrect = false, secondIncorrect = false, firstFetch = true, connected = false;
    private int[] optionNumber = new int[]{1, 2, 3, 4};

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the Layout for this Fragment
        View fragmentFirstLayout = inflater.inflate(R.layout.fragment_first, container, false);
        // Check the type of the quiz
        if (getArguments() != null) {
            FirstFragmentArgs args = FirstFragmentArgs.fromBundle(getArguments());
            quizType = args.getQuizType();
        }
        // we are connected to a network
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;
        // Get the Count and Question TextView
        questionTV = fragmentFirstLayout.findViewById(R.id.question);
        noConnectivity = fragmentFirstLayout.findViewById(R.id.no_wifi_quiz);
        noConnectivityIcon = fragmentFirstLayout.findViewById(R.id.no_wifi_quiz_icon);
        // Get the Buttons
        firstBtn = fragmentFirstLayout.findViewById(R.id.first_choice);
        secondBtn = fragmentFirstLayout.findViewById(R.id.second_choice);
        thirdBtn = fragmentFirstLayout.findViewById(R.id.third_choice);
        fourthBtn = fragmentFirstLayout.findViewById(R.id.fourth_choice);
        menuBtn = fragmentFirstLayout.findViewById(R.id.back_to_menu);
        nextQuestionBtn = fragmentFirstLayout.findViewById(R.id.get_next_question);
        nextQuestionSetBtn = fragmentFirstLayout.findViewById(R.id.get_next_question_set);
        playHint = fragmentFirstLayout.findViewById(R.id.play_hint);
        // Get the Image and set resolution
        questionImage = fragmentFirstLayout.findViewById(R.id.imageView);
        questionImage.getLayoutParams().height = Resources.getSystem().getDisplayMetrics().widthPixels;
        // Initialise romanisation
        r = new Romanisation(MyApplication.getAppContext());
        // Get the Questions
        quizModalArrayList = new ArrayList<>();
        changesInWeights = new ArrayList<>();
        questionTV.setText(r.input(getString(R.string.loading), getActivity()));
        noConnectivity.setText(r.input(getString(R.string.no_wifi), getActivity()));
        fetchQuestions();
        // Give buttons optionAction
        firstBtn.setOnClickListener(v -> optionAction(quizModalArrayList, firstBtn));
        secondBtn.setOnClickListener(v -> optionAction(quizModalArrayList, secondBtn));
        thirdBtn.setOnClickListener(v -> optionAction(quizModalArrayList, thirdBtn));
        fourthBtn.setOnClickListener(v -> optionAction(quizModalArrayList, fourthBtn));

        // Set colour for all buttons (temporary fix, need to change colours in selector)
        firstBtn.setTextColor(Color.parseColor("#ffffff"));
        secondBtn.setTextColor(Color.parseColor("#ffffff"));
        thirdBtn.setTextColor(Color.parseColor("#ffffff"));
        fourthBtn.setTextColor(Color.parseColor("#ffffff"));
        // Call mTTS on Long Click
        firstBtn.setOnLongClickListener(v -> {
            player.stopPlayer();
            mobileTTS.stop();
            replaceColours();
            mTTS.TTSQuestionButtonLongClick(mobileTTS, firstBtn, 1, questionSetPosition, questionPosition, optionNumber);
            return true;
        });
        secondBtn.setOnLongClickListener(v -> {
            player.stopPlayer();
            mobileTTS.stop();
            replaceColours();
            mTTS.TTSQuestionButtonLongClick(mobileTTS, secondBtn, 2, questionSetPosition, questionPosition, optionNumber);
            return true;
        });
        thirdBtn.setOnLongClickListener(v -> {
            player.stopPlayer();
            mobileTTS.stop();
            replaceColours();
            mTTS.TTSQuestionButtonLongClick(mobileTTS, thirdBtn, 3, questionSetPosition, questionPosition, optionNumber);
            return true;
        });
        fourthBtn.setOnLongClickListener(v -> {
            player.stopPlayer();
            mobileTTS.stop();
            replaceColours();
            mTTS.TTSQuestionButtonLongClick(mobileTTS, fourthBtn, 4, questionSetPosition, questionPosition, optionNumber);
            return true;
        });
        // Go back to MainMenu
        menuBtn.setOnClickListener(v -> NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_MainMenuFragment));
        // Enable all option buttons after getting next question
        nextQuestionBtn.setOnClickListener(v -> {
            firstBtn.setEnabled(true);
            secondBtn.setEnabled(true);
            thirdBtn.setEnabled(true);
            fourthBtn.setEnabled(true);
            setDataToViews();
        });

        nextQuestionSetBtn.setOnClickListener(v -> setDataToViews());

        questionTV.setOnClickListener(v -> {
            if (mobileTTS != null) {
                mobileTTS.stop();
                if (quizModalArrayList.size() == 0)
                    mTTS.speak(mobileTTS, getString(R.string.answered_all_questions));
                else
                    mTTS.speak(mobileTTS, quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getQuestion());
            }
        });

        playHint.setOnClickListener(v -> {
            if (player != null)
                player.stopPlayer();
            mobileTTS.stop();
            replaceColours();
            int duration = mTTS.sayHint(mobileTTS);
            new Handler().postDelayed(() -> { // Less wasteful than Thread.sleep()
                assert player != null;
                player.startHint(questionSetPosition, questionPosition);
            }, duration);
            player.displayToastHint(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getHintText(), this.getActivity()), secondIncorrect);
        });

        return fragmentFirstLayout;
    }

    // Description: Updates Image, Options, QuestionTV after new Question is chosen.
    // Precondition: -
    // Postcondition: TextViews and Image are updated.
    private void setDataToViews() {
        optionNumber = new int[]{1,2,3,4};
        nextQuestionSetBtn.setVisibility(View.GONE);
        nextQuestionBtn.setVisibility(View.GONE);
        firstBtn.setVisibility(View.VISIBLE);
        secondBtn.setVisibility(View.VISIBLE);
        thirdBtn.setVisibility(View.VISIBLE);
        fourthBtn.setVisibility(View.VISIBLE);
        playHint.setVisibility(View.VISIBLE);
        questionTV.setVisibility(View.VISIBLE);
        questionImage.setVisibility(View.VISIBLE);
        firstIncorrect = false;
        secondIncorrect = false;
        if (questionPosition == 5 || quizModalArrayList.size() == 0) {
            if (quizModalArrayList.size() == 0) {
                firstBtn.setVisibility(View.GONE);
                secondBtn.setVisibility(View.GONE);
                thirdBtn.setVisibility(View.GONE);
                fourthBtn.setVisibility(View.GONE);
                playHint.setVisibility(View.GONE);
                questionImage.setVisibility(View.INVISIBLE);
                questionTV.setText(r.input(getString(R.string.answered_all_questions), getActivity()));
                mTTS.speak(mobileTTS, getString(R.string.answered_all_questions));
                menuBtn.setText(r.input(getString(R.string.back_to_menu), getActivity()));
                menuBtn.setVisibility(View.VISIBLE);
                return;
            }
            finishedQuestionSet();
            return;
        }

        // Fetch image from the Storage
        if (!TextUtils.isEmpty(quizModalArrayList.get(questionSetPosition).getImage()))
            Glide.with(FirstFragment.this).load(quizModalArrayList.get(questionSetPosition).getImage()).into(questionImage);

        firstBtn.setText(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getOptions(0), getActivity()));
        secondBtn.setText(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getOptions(1), getActivity()));
        thirdBtn.setText(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getOptions(2), getActivity()));
        fourthBtn.setText(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getOptions(3), getActivity()));
        questionTV.setText(r.input(quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getQuestion(), getActivity()));
        mTTS.TTSQuestion(mobileTTS, player, questionSetPosition, questionPosition, firstIncorrect, secondIncorrect, optionNumber);
    }

    // Description: Reduces number of Options, provides Hint,
    //              sets next Question.
    // Precondition: -
    // Postcondition: Fam I don't even know what to write here atm
    private void optionAction(@NonNull ArrayList<QuestionSet> quizModalArrayList, @NonNull Button button) {
        player.stopPlayer();
        mobileTTS.stop();
        replaceColours();
        if (quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getAnswer() == getButtonPosition(button)) {
            addErrTimeQuestion(false);
            displayCorrectAnswer();
        } else if (!firstIncorrect && !secondIncorrect) {
            addErrTimeQuestion(true);
            firstIncorrect = true;
            button.setVisibility(View.GONE);
            mTTS.TTSQuestion(mobileTTS, player, questionSetPosition, questionPosition, firstIncorrect, secondIncorrect, optionNumber);
        } else if (firstIncorrect && !secondIncorrect) {
            addErrTimeQuestion(true);
            secondIncorrect = true;
            button.setVisibility(View.GONE);
            mTTS.TTSQuestion(mobileTTS, player, questionSetPosition, questionPosition, firstIncorrect, secondIncorrect, optionNumber);
        } else {
            addErrTimeQuestion(true);
            displayCorrectAnswer();
        }
    }

    // Description: Returns the index of button's text in Question
    //              Helper function to compare Integers instead of Strings
    // Precondition: -
    // Postcondition: Index is returned
    private int getButtonPosition(Button button) {
        if (button == firstBtn) return 0;
        if (button == secondBtn) return 1;
        if (button == thirdBtn) return 2;
        else return 3;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // Description: Displays the correct answer to the user
    // Precondition: Answered correctly or 2 times incorrectly
    // Postcondition: Sets visibility of incorrect buttons to GONE
    //                Disables ClickListener of the buttons
    //                Sets visibility of the nextQuestionBtn to VISIBLE
    private void displayCorrectAnswer() {
        firstBtn.setEnabled(false);
        secondBtn.setEnabled(false);
        thirdBtn.setEnabled(false);
        fourthBtn.setEnabled(false);
        if (quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getAnswer() != getButtonPosition(firstBtn))
            firstBtn.setVisibility(View.GONE);
        if (quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getAnswer() != getButtonPosition(secondBtn))
            secondBtn.setVisibility(View.GONE);
        if (quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getAnswer() != getButtonPosition(thirdBtn))
            thirdBtn.setVisibility(View.GONE);
        if (quizModalArrayList.get(questionSetPosition).getQuestionArray(questionPosition).getAnswer() != getButtonPosition(fourthBtn))
            fourthBtn.setVisibility(View.GONE);
        nextQuestionBtn.setVisibility(View.VISIBLE);
        nextQuestionBtn.setText(r.input(getString(R.string.tap_to_continue), getActivity()));
        ++questionPosition;
    }

    // Description: Displays the question video at the end of the question set
    // Precondition: All questions of the question set have been answered
    // Postcondition: Option buttons, question textview, question image disappear
    //                nextQuestion button appears
    //                plays the question video on loop until the nextQuestion is pressed
    private void finishedQuestionSet() {
        String video = quizModalArrayList.get(questionSetPosition).getVideo();
        Intent intent = new Intent(getActivity(), QuestionVideo.class);
        intent.putExtra("video", video);
        startActivity(intent);
        firstBtn.setVisibility(View.GONE);
        secondBtn.setVisibility(View.GONE);
        thirdBtn.setVisibility(View.GONE);
        fourthBtn.setVisibility(View.GONE);
        playHint.setVisibility(View.GONE);
        questionTV.setVisibility(View.GONE);
        questionImage.setVisibility(View.GONE);
        nextQuestionSetBtn.setVisibility(View.VISIBLE);
        nextQuestionSetBtn.setText(r.input(getString(R.string.next_question), getActivity()));
        changesInWeights.add(quizModalArrayList.remove(questionSetPosition));
        if (quizModalArrayList.size() != 0)
            questionSetPosition = getRandomQuestion();
        questionPosition = 0;
    }

    // Description: Uses biased randomisation to get next question set
    // Precondition: quizModalArrayList is not empty
    // Postcondition: Next question set's index is returned
    private int getRandomQuestion() {
        RandomAlgorithm<Integer> rc = new RandomAlgorithm<>();
        for (int i = 0; i < quizModalArrayList.size(); i++)
            rc.add(quizModalArrayList.get(i).getWeight(), i);
        return rc.next();
    }

    // Description: Adjusts the weights of the question sets
    // Precondition: The option button is pressed
    // Postcondition: If correct option decreases weight by 5
    //                Else increases weight by 5
    private void addErrTimeQuestion(boolean increase) {
        int weight = 5;
        if (increase)
            quizModalArrayList.get(questionSetPosition).setWeight(quizModalArrayList.get(questionSetPosition).getWeight()+weight);
        else
            quizModalArrayList.get(questionSetPosition).setWeight(quizModalArrayList.get(questionSetPosition).getWeight()-weight);
    }

    // Description: Populates quizModalArray with complete QuestionSets from database
    // Precondition: -
    // Postcondition: The QuestionSets' weights in database are replaced with new ones
    private void fetchQuestions() {
        if (connected) {
            questionTV.setVisibility(View.VISIBLE);
            questionImage.setVisibility(View.VISIBLE);
            noConnectivity.setVisibility(View.GONE);
            noConnectivityIcon.setVisibility(View.GONE);
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(quizType);
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (firstFetch) {
                        firstFetch = false;
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            // Check if dataSnapshot is not 'counter' since it is the only child
                            // that is not of class QuestionSet
                            if (!Objects.equals(dataSnapshot.getKey(), "counter")) {
                                QuestionSet questionSet = dataSnapshot.getValue(QuestionSet.class);
                                assert questionSet != null;
                                if (questionSet.getComplete()) {
                                    questionSet.setId(dataSnapshot.getKey());
                                    quizModalArrayList.add(questionSet);
                                }
                            }
                        }
                        if (quizModalArrayList.size() == 0)
                            questionTV.setText(r.input(getString(R.string.no_questions), getActivity()));
                        else {
                            firstBtn.setVisibility(View.VISIBLE);
                            secondBtn.setVisibility(View.VISIBLE);
                            thirdBtn.setVisibility(View.VISIBLE);
                            fourthBtn.setVisibility(View.VISIBLE);
                            playHint.setVisibility(View.VISIBLE);
                            player = new HintPlayer(getActivity(), quizModalArrayList);
                            // Initialise TextToSpeech
                            mTTS = new MobileTTS(getActivity(), MyApplication.getAppContext(), quizModalArrayList, firstBtn, secondBtn, thirdBtn, fourthBtn, r);
                            questionSetPosition = getRandomQuestion();
                            mobileTTS = mTTS.initialiseTextToSpeechEngine(questionSetPosition, questionPosition);
                            setDataToViews();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    // Description: Resets the colours of the buttons after TTS showcase
    // Precondition: -
    // Postcondition: Buttons' colours are returned to their original state
    // Exception: Avoid the presence of other buttons that have not yet finished playing
    //            or whose colors have not yet returned to their original colors
    private void replaceColours() {
        firstBtn.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.button_background));
        secondBtn.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.button_background));
        thirdBtn.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.button_background));
        fourthBtn.setBackgroundColor(ContextCompat.getColor(MyApplication.getAppContext(), R.color.button_background));
    }

    // Description: Disconnects from TTS engine
    // Precondition: mTTS exists
    // Postcondition: App disconnected from TTS engine
    @Override
    public void onDestroyView() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(quizType);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                while (!changesInWeights.isEmpty()) {
                    DatabaseReference addQuestionSet = ref.child(changesInWeights.get(0).getId()).child("weight");
                    addQuestionSet.setValue(changesInWeights.remove(0).getWeight());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("Database Error", "loadPost:onCancelled", error.toException());
            }
        });
        if (mobileTTS != null) {
            mobileTTS.stop();
            mobileTTS.shutdown();
        }
        if (player != null)
            player.stopPlayer();
        super.onDestroyView();
    }
}
