package com.itjustworks.memorylane;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.itjustworks.memorylane.MainMenuFragmentDirections;
import com.itjustworks.memorylane.databinding.FragmentMainMenuBinding;

/*
 * MainMenuFragment.java
 *
 * Class Description: Main menu of the application,
 *                    having redirection to Quiz and Settings on top.
 * Class Invariant: -
 *
 */

public class MainMenuFragment extends Fragment {

    private FragmentMainMenuBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Romanisation r = new Romanisation(MyApplication.getAppContext());
        AppCompatTextView leftPickText = view.findViewById(R.id.left_pick_text), rightPickText = view.findViewById(R.id.right_pick_text), createPickText = view.findViewById(R.id.question_set_pick_text);
        leftPickText.setText(r.input(getString(R.string.start_quiz), getActivity()));
        rightPickText.setText(r.input(getString(R.string.start_premade_quiz), getActivity()));
        createPickText.setText(r.input(getString(R.string.create_quiz), getActivity()));
        view.findViewById(R.id.user_quiz).setOnClickListener(view1 -> {
            MainMenuFragmentDirections.ActionMainMenuFragmentToFirstFragment action = MainMenuFragmentDirections.actionMainMenuFragmentToFirstFragment();
            action.setQuizType("questionSets");
            NavHostFragment.findNavController(MainMenuFragment.this).navigate(action);
        });
        view.findViewById(R.id.pre_made_quiz).setOnClickListener(view1 -> {
            MainMenuFragmentDirections.ActionMainMenuFragmentToFirstFragment action = MainMenuFragmentDirections.actionMainMenuFragmentToFirstFragment();
            action.setQuizType("premadeQuiz");
            NavHostFragment.findNavController(MainMenuFragment.this).navigate(action);
        });
        view.findViewById(R.id.create_question_set).setOnClickListener(view1 -> startActivity(new Intent(getActivity(), CreateQuestionSet.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}