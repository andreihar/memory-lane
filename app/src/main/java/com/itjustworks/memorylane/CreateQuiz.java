package com.itjustworks.memorylane;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

/*
 * CreateQuiz.java
 *
 * Class Description: UI redirecting the user to CreateQuestionSet.
 * Class Invariant: -
 *
 */

public class CreateQuiz extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);
        // Get IDs
        Button createQuiz = findViewById(R.id.createQuiz);
        // Set text
        createQuiz.setText(getString(R.string.create_quiz));

        createQuiz.setOnClickListener(v -> {
            Intent moveToNextStep = new Intent(CreateQuiz.this, CreateQuestionSet.class);
            CreateQuiz.this.startActivity(moveToNextStep);
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // Return to previous Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}