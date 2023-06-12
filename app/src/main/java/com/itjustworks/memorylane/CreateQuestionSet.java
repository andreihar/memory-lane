package com.itjustworks.memorylane;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

/*
 * CreateQuestionSet.java
 *
 * Class Description: UI to edit existing Question Sets.
 * Class Invariant: Updates the list automatically on any change in Firebase
 *
 */

public class CreateQuestionSet extends AppCompatActivity {

    private LinearLayout container;
    private DatabaseReference databaseReference;
    private int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_question_set);
        Button addQuestion = findViewById(R.id.question_set_button);
        TextView noConnectivity = findViewById(R.id.no_wifi_question_set);
        ImageView noConnectivityIcon = findViewById(R.id.no_wifi_question_set_icon);
        databaseReference = FirebaseDatabase.getInstance().getReference("questionSets");
        // we are connected to a network
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;
        if (connected) {
            noConnectivity.setVisibility(View.GONE);
            noConnectivityIcon.setVisibility(View.GONE);
            findViewById(R.id.outer_container).setVisibility(View.VISIBLE);
            addQuestion.setVisibility(View.VISIBLE);
            addQuestion.setOnClickListener(v -> {
                DatabaseReference ref = databaseReference.child("counter");
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        counter = snapshot.getValue(Integer.class);
                        databaseReference.child("counter").setValue(++counter);
                        QuestionSet set = new QuestionSet(null, null, null, null, null, "", "", 0, false, 50);
                        databaseReference.child(counter + "").setValue(set);
                        Intent moveToWriteQuestions = new Intent(CreateQuestionSet.this, CreateQuestions.class);
                        moveToWriteQuestions.putExtra("id", counter);
                        CreateQuestionSet.this.startActivity(moveToWriteQuestions);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("Database Error", "loadPost:onCancelled", error.toException());
                    }
                });
            });
            showcaseQuestions();
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
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

    // Description: A list of questions that are in User's database,
    // Precondition: Database exists
    // Postcondition: Shows all questions in the linear layout and allows user
    //                to call WriteQuestions in order to modify them
    private void showcaseQuestions() {
        container = findViewById(R.id.questions_list);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                container.removeAllViews();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // Check if dataSnapshot is not 'counter' since it is the only child
                    // that is not of class QuestionSet
                    if (!Objects.equals(dataSnapshot.getKey(), "counter")) {
                        ImageView imageView = new ImageView(CreateQuestionSet.this);
                        imageView.setAdjustViewBounds(true);
                        imageView.setImageResource(R.drawable.logo);
                        String url = "";

                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            if (Objects.equals(childSnapshot.getKey(), "image")) {
                                url=(String) childSnapshot.getValue();
                                if (url.isEmpty())
                                    url = "";
                            }
                        }

                        // Include question id into extra
                        int questionSetId = parseInt(Objects.requireNonNull(dataSnapshot.getKey()));
                        imageView.setOnClickListener(v -> {
                            Intent moveToWriteQuestions = new Intent(CreateQuestionSet.this, CreateQuestions.class);
                            moveToWriteQuestions.putExtra("id", questionSetId);
                            CreateQuestionSet.this.startActivity(moveToWriteQuestions);
                        });
                        if (!TextUtils.isEmpty(url))
                            Glide.with(CreateQuestionSet.this).load(url).into(imageView);
                        container.addView(imageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}