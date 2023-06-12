package com.itjustworks.memorylane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

/*
 * WriteQuestions.java
 *
 * Class Description: Create new Question object of QuestionSet,
 *                    upload to Firebase.
 * Class Invariant: Uploads String, int, and mp3.
 *                  Toast error message if fields are empty
 *
 */

public class WriteQuestions extends AppCompatActivity {
    private int counter, questionId = 0, questionSetId, answerFromRadio;
    private final int PICK_AUDIO = 1;
    private long timeInMilliseconds = 0L, timeSwapBuff = 0L, updatedTime = 0L, startHTime = 0L;
    private boolean edit;
    private EditText question, option1, option2, option3, option4, hint;
    private RadioButton radioBtn1, radioBtn2, radioBtn3, radioBtn4;
    private TextView recordAction, recordActionInstruction, timer;
    private ImageView recordVoice, playVoice, saveRecording, restartRecording;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private final Handler customHandler = new Handler();
    private DatabaseReference databaseReferenceMain, ref;
    private StorageReference storageRef;
    private Question questionClass;
    private String questionWrittenByUser, option1WrittenByUser, option2WrittenByUser, option3WrittenByUser, option4WritenByUser, hintWrittenByUser, hintDownloadUrl;
    private Uri hintUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_questions);
        // Get the IDs
        question = findViewById(R.id.question);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        hint = findViewById(R.id.hint);
        Button saveQuestions = findViewById(R.id.save_questions);
        Button addAudioHint = findViewById(R.id.add_audio_hint);
        radioBtn1 = findViewById(R.id.radio_button_1);
        radioBtn2 = findViewById(R.id.radio_button_2);
        radioBtn3 = findViewById(R.id.radio_button_3);
        radioBtn4 = findViewById(R.id.radio_button_4);
        // Set text
        question.setHint(getString(R.string.write_the_question));
        option1.setHint(getString(R.string.option_1));
        option2.setHint(getString(R.string.option_2));
        option3.setHint(getString(R.string.option_3));
        option4.setHint(getString(R.string.option_4));
        hint.setHint(getString(R.string.write_the_hint));
        saveQuestions.setText(getString(R.string.save));
        addAudioHint.setText(getString(R.string.add_audio_hint));
        storageRef = FirebaseStorage.getInstance().getReference("createQuestions");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Pull id from Bundle
            questionId = extras.getInt("questionId");
            questionSetId = extras.getInt("questionSetId");
            databaseReferenceMain = FirebaseDatabase.getInstance().getReference("questionSets").child(questionSetId + "");
            if (questionId == 0) {
                ref = databaseReferenceMain.child("counter");
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        counter = snapshot.getValue(Integer.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("Database Error", "loadPost:onCancelled", error.toException());
                    }
                });
            } else {
                edit = true;
                ref = FirebaseDatabase.getInstance().getReference("questionSets").child(questionSetId + "").child("q" + questionId);
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        questionClass = snapshot.getValue(Question.class);
                        // set the current information
                        assert questionClass != null;
                        question.setText(questionClass.getQuestion());
                        option1.setText(questionClass.getOptions(0));
                        option2.setText(questionClass.getOptions(1));
                        option3.setText(questionClass.getOptions(2));
                        option4.setText(questionClass.getOptions(3));
                        hintDownloadUrl = questionClass.getHint();
                        hint.setText(questionClass.getHintText());
                        switch (questionClass.getAnswer()) {
                            case 0:
                                radioBtn1.setChecked(true);
                                break;
                            case 1:
                                radioBtn2.setChecked(true);
                                break;
                            case 2:
                                radioBtn3.setChecked(true);
                                break;
                            case 3:
                                radioBtn4.setChecked(true);
                                break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        addAudioHint.setOnClickListener(v -> {
            View customView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialogue_choose, null);
            AppCompatTextView leftPickText = customView.findViewById(R.id.left_pick_text), rightPickText = customView.findViewById(R.id.right_pick_text);
            AppCompatImageView leftPickIcon = customView.findViewById(R.id.left_pick_icon), rightPickIcon = customView.findViewById(R.id.right_pick_icon);
            leftPickText.setText(R.string.title_record);
            rightPickText.setText(R.string.title_files);
            leftPickIcon.setImageDrawable(AppCompatResources.getDrawable(this, android.R.drawable.ic_btn_speak_now));
            rightPickIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_photo_black_48dp));
            AlertDialog dialogue = new AlertDialog.Builder(WriteQuestions.this)
                    .setTitle(R.string.title_choose)
                    .setView(customView)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setOnCancelListener(null).create();
            dialogue.show();

            // Handle Record option click
            customView.findViewById(R.id.left_pick).setOnClickListener(v1 -> {
                getAudioByMicrophone();
                dialogue.dismiss();
            });

            // Handle Browse option click
            customView.findViewById(R.id.right_pick).setOnClickListener(v2 -> {
                getAudioByFiles();
                dialogue.dismiss();
            });
        });

        saveQuestions.setOnClickListener(v -> {
            questionWrittenByUser = question.getText().toString().trim();
            option1WrittenByUser = option1.getText().toString().trim();
            option2WrittenByUser = option2.getText().toString().trim();
            option3WrittenByUser = option3.getText().toString().trim();
            option4WritenByUser = option4.getText().toString().trim();
            hintWrittenByUser = hint.getText().toString().trim();
            // Check whether EditText and RadioButton are filled out
            if ((TextUtils.isEmpty(questionWrittenByUser) || TextUtils.isEmpty(option1WrittenByUser) || TextUtils.isEmpty(option2WrittenByUser) || TextUtils.isEmpty(option3WrittenByUser) || TextUtils.isEmpty(option4WritenByUser) || TextUtils.isEmpty(hintWrittenByUser))
                    || (!radioBtn1.isChecked() && !radioBtn2.isChecked() && !radioBtn3.isChecked() && !radioBtn4.isChecked()) || (hintUri == null && hintDownloadUrl.isEmpty())) {
                // if the text fields are empty
                // then show the below message.
                Toast.makeText(WriteQuestions.this, R.string.error_fill_out_fields, Toast.LENGTH_SHORT).show();
            } else {
                // else call the method to add
                // data to our database
                addDataToDatabase(questionWrittenByUser, option1WrittenByUser, option2WrittenByUser, option3WrittenByUser, option4WritenByUser, hintWrittenByUser);
                clearFields();
            }
        });

    }

    // Description: Adds the question to the database,
    // Precondition: Database has counter value
    // Postcondition: Increases counter by 1, adds the question at counter index
    private void addDataToDatabase(String question, String option1, String option2, String option3, String option4, String hint) {
        questionClass = new Question(question, option1, option2, option3, option4, hint, answerFromRadio, hintDownloadUrl);
        Uploader uploader = new Uploader(this, new Uploader.UploaderListener() {
            @Override
            public void success(Uri localUri, Uri uploadSessionUri) {
                if (edit)
                    finish();
            }

            @Override
            public void failed(Exception e) {

            }

            @Override
            public void onProgress(long curSize, long allSize) {

            }
        }, databaseReferenceMain, storageRef);
        if (edit) {
            Toast.makeText(WriteQuestions.this, R.string.question_saved, Toast.LENGTH_SHORT).show();
            databaseReferenceMain.child("q" + questionId).setValue(questionClass);
            uploader.uploadFile(hintUri, questionId, questionSetId);
            if (hintUri == null)
                finish();
        } else {
            if (counter >= 5) {
                Toast.makeText(WriteQuestions.this, R.string.more_than_five, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(WriteQuestions.this, R.string.question_created, Toast.LENGTH_SHORT).show();
                ref.setValue(++counter);
                databaseReferenceMain.child("q" + counter).setValue(questionClass);
                uploader.uploadFile(hintUri, counter, questionSetId);
            }
        }
    }

    //This function is called when the user click on the radio button in the create questions page
    //This functions tells which radio button is clicked, and then changes the value of the answerFromRadio to
    // that specific integer number 0,1,2,3
    public void onRadioButtonClicked(@NonNull View view) {
        RadioButton button = (RadioButton) view;
        // Check which radio button was clicked
        radioBtn1.setChecked(false);
        radioBtn2.setChecked(false);
        radioBtn3.setChecked(false);
        radioBtn4.setChecked(false);
        button.setChecked(true);
        if (button == radioBtn1)
            answerFromRadio = 0;
        if (button == radioBtn2)
            answerFromRadio = 1;
        if (button == radioBtn3)
            answerFromRadio = 2;
        if (button == radioBtn4)
            answerFromRadio = 3;
        System.out.println("answerFromRadio is " + answerFromRadio);
    }

    // Description: Records the audio through phone's microphone,
    // Precondition: Rights to record are given
    // Postcondition: Saves the mp3 for further use
    @SuppressLint("ClickableViewAccessibility")
    private void getAudioByMicrophone() {
        timeInMilliseconds = timeSwapBuff = updatedTime = startHTime = 0L;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(WriteQuestions.this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.bottom_voice_record, null);
        recordVoice = bottomSheetView.findViewById(R.id.record_voice);
        playVoice = bottomSheetView.findViewById(R.id.play_voice);
        restartRecording = bottomSheetView.findViewById(R.id.restart_recording);
        saveRecording = bottomSheetView.findViewById(R.id.save_recording);
        recordAction = bottomSheetView.findViewById(R.id.record_action);
        recordActionInstruction = bottomSheetView.findViewById(R.id.record_action_instruction);
        timer = bottomSheetView.findViewById(R.id.timer);
        TextView closeRecordPanel = bottomSheetView.findViewById(R.id.close_record_panel);
        recordAction.setText(getString(R.string.start_record));
        recordActionInstruction.setText(getString(R.string.start_record_inst));
        timer.setText("00:00");

        // Check permissions for MIC
        int MICROPHONE_PERMISSION_CODE = 200;
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE))
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            recordVoice.setEnabled(true);
            playVoice.setEnabled(true);
        }
        recordVoice.setOnTouchListener((v1, event) -> {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    try {
                        try {
                            mediaRecorder.stop();
                            mediaRecorder.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        recordVoice.setImageResource(R.drawable.ic_stop_record);
                        recordAction.setText(getString(R.string.stop_record));
                        recordActionInstruction.setText(getString(R.string.stop_record_inst));
                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mediaRecorder.setAudioEncodingBitRate(128000);
                        mediaRecorder.setAudioSamplingRate(44100);
                        mediaRecorder.setOutputFile(getRecordingFile().getPath());
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        startHTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_BUTTON_RELEASE:
                    try {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        hintUri = Uri.fromFile(getRecordingFile());
                        Uploader u = new Uploader(this, null, databaseReferenceMain, storageRef);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    timeSwapBuff += timeInMilliseconds;
                    customHandler.removeCallbacks(updateTimerThread);
                    recordVoice.setVisibility(View.GONE);
                    playVoice.setVisibility(View.VISIBLE);
                    saveRecording.setVisibility(View.VISIBLE);
                    restartRecording.setVisibility(View.VISIBLE);
                    recordAction.setText(getString(R.string.save_hint));
                    recordActionInstruction.setText(getString(R.string.save_hint_inst));
                    mediaRecorder = null;
                    return true;
            }
            return false;
        });
        // Play
        playVoice.setOnClickListener(v2 -> {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
            }
            if (mediaPlayer != null)
                mediaPlayer.stop();
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(getRecordingFile().getPath());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // Delete file and reset state to Record
        restartRecording.setOnClickListener(v3 -> {
            if (mediaPlayer != null)
                mediaPlayer.stop();
            getRecordingFile().delete();
            recordVoice.setImageResource(R.drawable.ic_start_record);
            playVoice.setVisibility(View.GONE);
            recordVoice.setVisibility(View.VISIBLE);
            restartRecording.setVisibility(View.INVISIBLE);
            saveRecording.setVisibility(View.INVISIBLE);
            recordAction.setText(getString(R.string.start_record));
            recordActionInstruction.setText(getString(R.string.start_record_inst));
            timeInMilliseconds = timeSwapBuff = updatedTime = startHTime = 0L;
            timer.setText("00:00");
        });
        // Close the Bottom Sheet Dialogue
        saveRecording.setOnClickListener(v4 -> {
            timeInMilliseconds = timeSwapBuff = updatedTime = startHTime = 0L;
            bottomSheetDialog.dismiss();
        });
        closeRecordPanel.setOnClickListener(v5 -> {
            timeInMilliseconds = timeSwapBuff = updatedTime = startHTime = 0L;
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void getAudioByFiles() {
        Intent audio = new Intent();
        audio.setType("audio/*");
        audio.setAction(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(Intent.createChooser(audio, "Select Audio"), PICK_AUDIO);
    }

    private File getRecordingFile() {
        return new File(new ContextWrapper(getApplicationContext()).getExternalFilesDir(Environment.DIRECTORY_MUSIC), "hint" + ".mp3");
    }

    // Description: Clears the fields of the template
    // Precondition: -
    // Postcondition: Fields and radio buttons are empty and set to false
    private void clearFields() {
        question.getText().clear();
        option1.getText().clear();
        option2.getText().clear();
        option3.getText().clear();
        option4.getText().clear();
        hint.getText().clear();
        radioBtn1.setChecked(false);
        radioBtn2.setChecked(false);
        radioBtn3.setChecked(false);
        radioBtn4.setChecked(false);
    }

    // Show Time
    private final Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startHTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            timer.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
        }
    };

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {
            // Audio is Picked in format of URI
            assert data != null;
            hintUri = data.getData();
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
