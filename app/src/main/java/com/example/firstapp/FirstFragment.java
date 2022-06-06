package com.example.firstapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.firstapp.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.Random;

public class FirstFragment extends Fragment {

private FragmentFirstBinding binding;

    private TextView questionTV, questionNumberTV;
    private ImageView questionImage;
    private Button firstBtn, secondBtn, thirdBtn, fourthBtn;
    private ArrayList<QuestionBlock> quizModalArrayList;
    int tempArrayPointer = 0; // For testing purposes, forbid repetitions
    Random random;
    int score = 0, questionBlockPosition = 0, questionPosition = 0;
    boolean firstIncorrect = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View fragmentFirstLayout = inflater.inflate(R.layout.fragment_first, container, false);
        // Get the count text view
        questionTV = fragmentFirstLayout.findViewById(R.id.question);
        questionNumberTV = fragmentFirstLayout.findViewById(R.id.questionNumber);
        firstBtn = fragmentFirstLayout.findViewById(R.id.first_choice);
        secondBtn = fragmentFirstLayout.findViewById(R.id.second_choice);
        thirdBtn = fragmentFirstLayout.findViewById(R.id.third_choice);
        fourthBtn = fragmentFirstLayout.findViewById(R.id.fourth_choice);
        questionImage = fragmentFirstLayout.findViewById(R.id.imageView);
        quizModalArrayList = new ArrayList<>();
        random = new Random();
        getQuizQuestion(quizModalArrayList);
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

        return fragmentFirstLayout;
    }

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
                firstBtn.setEnabled(false);
                secondBtn.setEnabled(false);
                thirdBtn.setEnabled(false);
                fourthBtn.setEnabled(false);
                questionTV.setText(getString(R.string.answered_all_questions));
                return;
            }
            questionBlockPosition = random.nextInt(quizModalArrayList.size());
            questionPosition = 0;
        }
        questionImage.setImageResource(quizModalArrayList.get(questionBlockPosition).getImage());
        questionTV.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getQuestion());
        firstBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getFirst());
        secondBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getSecond());
        thirdBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getThird());
        fourthBtn.setText(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getFourth());
    }

    private void optionAction(ArrayList<QuestionBlock> quizModalArrayList, Button button, View v) {
        if(quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getAnswer().trim().equalsIgnoreCase(button.getText().toString().trim())) {
            ++score;
            ++questionPosition;
            setDataToViews();
        } else if (firstIncorrect == false) {
            firstIncorrect = true;
            button.setEnabled(false);
            displayToastHint(v, quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getHint());
            // TOAST HINT
            // Temporary method of delivering hints
            // Could edit style of toast and make it more dementia friendly instead of using textviews
            // Toast.makeText(getActivity(), quizModalArrayList.get(questionBlockPosition).getQuestionArray(questionPosition).getHint(), Toast.LENGTH_SHORT).show();
        } else {
            ++questionPosition;
            setDataToViews();
        }
    }

    private void displayToastHint(View v, String hint) {
        Toast.makeText(getActivity(), hint, Toast.LENGTH_SHORT).show();
    }

    private void getQuizQuestion(ArrayList<QuestionBlock> quizModalArrayList) {
        Question questionOne = new Question("Who is it?", "Mum", "Mate", "Me", "Cornershop bossman", "Me", "It's you");
        Question questionTwo = new Question("How old is he?", "16", "2", "9", "74", "74", "You're pretty old");
        Question questionThree = new Question("What's his ethnicity?", "Asian", "Caucasian", "Mongol", "Kyrgyz", "Asian", "You're from China");
        Question questionFour = new Question("His favourite UK rapper?", "Skepta", "Wiley", "Stormzy", "Himself", "Himself", "You were drilling on regs");
        Question questionFive = new Question("It's Unknown T", "Homerton B", "Gyalie on me", "Op block", "Bali on me", "Homerton B", "Homerton B");
        quizModalArrayList.add(new QuestionBlock(questionOne, questionTwo, questionThree, questionFour, questionFive, R.drawable.daboy));

        questionOne = new Question("Dat boy is who?", "Future doc", "My G", "Taiwanren", "Burnaby S boy", "Taiwanren", "He is from Taiwan");
        questionTwo = new Question("City of birth?", "Tainan", "Taipei", "Taichung", "Taoyuan", "Tainan", "He's from the South");
        questionThree = new Question("Calculus 12 grade?", "A", "A+", "A++", "A+++", "A+++", "He's really smart");
        questionFour = new Question("Most complex 漢字 in Unicode?", "\uD869\uDEA5", "\uD869\uDEA3", "\uD869\uDEA4", "\uD869\uDEA2", "\uD869\uDEA5", "Ye you can't really see them innit");
        questionFive = new Question("Anthem of Taiwan?", "San Min Zhuyi", "Yiyongjun Jinxingqu", "Gong Jin'ou", "Wuzu gonghe ge", "San Min Zhuyi", "three");
        quizModalArrayList.add(new QuestionBlock(questionOne, questionTwo, questionThree, questionFour, questionFive, R.drawable.taiwanren));

        questionOne = new Question("Who dat?", "My mate on teli", "Trump", "MP of UK", "Alien", "My mate on teli", "He's on teli");
        questionTwo = new Question("What is he famous for?", "Tallest man", "Nothing", "best nba player", "Everything", "Nothing", "Nobody knows him");
        questionThree = new Question("It's 3 am I wanna go sleep", "The last", "Questions", "Won't make", "Any sense", "Any sense", "Yes");
        questionFour = new Question("?", "=(", "=)", "=/", "=|", "=)", "Happy la");
        questionFive = new Question("Best anime?", "Boku no piku", "Initial D", "Oreimo", "Pokemon", "Initial D", "Anime about cars");
        quizModalArrayList.add(new QuestionBlock(questionOne, questionTwo, questionThree, questionFour, questionFive, R.drawable.teliman));
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}