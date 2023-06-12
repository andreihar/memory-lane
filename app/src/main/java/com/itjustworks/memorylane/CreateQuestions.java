package com.itjustworks.memorylane;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

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
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/*
 * CreateQuestions.java
 *
 * Class Description: UI to create new Question Set in the Firebase.
 * Class Invariant: Deletes Question Set if none of the necessary information is given
 *
 */

public class CreateQuestions extends AppCompatActivity implements View.OnClickListener {
    private final int PICK_VIDEO = 1, VIDEO_RECORD_CODE = 101;
    private VideoView videoView;
    private LinearLayout container;
    private DatabaseReference databaseReference;
    private Uploader uploader;
    private Button addVideo;
    private QuestionSet questionSet;
    private int questionSetId = 0, counter;
    private Boolean isUploadImage = false, isUploadVideo = false;
    ImageView imageCover;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_questions);
        // Get the IDs
        videoView = findViewById(R.id.video_view);
        videoView.setVisibility(View.GONE);
        imageCover = findViewById(R.id.image_cover);
        imageCover.setImageResource(R.drawable.logo);
        FloatingActionButton addImage = findViewById(R.id.add_image);
        Button addQuestions = findViewById(R.id.add_questions);
        addVideo = findViewById(R.id.add_video);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            questionSetId = extras.getInt("id");
        }
        // Set text
        databaseReference = FirebaseDatabase.getInstance().getReference("questionSets").child(questionSetId + "");
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("createQuestions");
        showcaseQuestions();
        uploader = new Uploader(this, new Uploader.UploaderListener() {
            @Override
            public void success(Uri localUri, Uri uploadSessionUri) {
                if (isUploadImage) {
                    imageCover.setImageURI(localUri);
                }
                else if (isUploadVideo) {
                    videoView.setVisibility(View.VISIBLE);
                    addVideo.setVisibility(View.GONE);
                    videoView.setVideoURI(localUri);
                    videoView.requestFocus();
                    videoView.setOnPreparedListener(mp -> {
                        mp.setLooping(true);
                        videoView.start();
                    });
                }
                hideUploading();
            }

            @Override
            public void failed(Exception e) {
                hideUploading();
            }

            @Override
            public void onProgress(long curSize, long allSize) {
                showUploading(curSize, allSize);
            }
        }, databaseReference, storageRef);
        addQuestions.setOnClickListener(v -> {
            Intent moveToWriteQuestions = new Intent(CreateQuestions.this, WriteQuestions.class);
            moveToWriteQuestions.putExtra("questionSetId", questionSetId);
            CreateQuestions.this.startActivity(moveToWriteQuestions);
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        imageCover.setOnClickListener(this);
        addImage.setOnClickListener(this);

        // Select Video
        addVideo.setOnClickListener(v -> {
            View customView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialogue_choose, null);
            AppCompatTextView leftPickText = customView.findViewById(R.id.left_pick_text), rightPickText = customView.findViewById(R.id.right_pick_text);
            AppCompatImageView leftPickIcon = customView.findViewById(R.id.left_pick_icon), rightPickIcon = customView.findViewById(R.id.right_pick_icon);
            leftPickText.setText(R.string.title_camera);
            rightPickText.setText(R.string.title_gallery);
            leftPickIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_photo_camera_black_48dp));
            rightPickIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_photo_black_48dp));
            AlertDialog dialogue = new AlertDialog.Builder(CreateQuestions.this)
                    .setTitle(R.string.title_choose)
                    .setView(customView)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setOnCancelListener(null).create();
            dialogue.show();

            // Handle Record option click
            customView.findViewById(R.id.left_pick).setOnClickListener(v1 -> {
                if (!checkPermissions(NEEDED_PERMISSIONS)) {
                    ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
                } else {
                    isExternalStorageManager();
                    callSysCameraAppToRecordVideo();
                }
                dialogue.dismiss();
            });

            // Handle Browse option click
            customView.findViewById(R.id.right_pick).setOnClickListener(v2 -> {
                getVideoByGallery();
                dialogue.dismiss();
            });
        });
    }
    private void callSysCameraAppToRecordVideo() {
        File videoFile = createImageFile();
        Uri videoUri;
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);  // 表示跳转至相机的录视频界面
        if (intent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            if (null != videoFile) {
                String AUTH_FILE_PROVIDER = "com.itjustworks.memorylane.fileprovider";
                videoUri = FileProvider.getUriForFile(this, AUTH_FILE_PROVIDER, videoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);    // 表示录制完后保存的录制，如果不写，则会保存到默认的路径，在onActivityResult()的回调，通过intent.getData中返回保存的路径
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 600);   // 设置视频录制的最长时间
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);    // MediaStore.EXTRA_VIDEO_QUALITY 表示录制视频的质量，从 0-1，越大表示质量越好，同时视频也越大
                startActivityForResult(intent, VIDEO_RECORD_CODE);//启动相机
            }
        }
    }

    private void isExternalStorageManager(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent =new  Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent,1001);
            }
        }
    }
    private File createImageFile() {
        String filePath = getFilePath(this) +System.currentTimeMillis()+".mp4";

        File file = new File(filePath);
        File dirFile = file.getParentFile();
        if (null != dirFile) {
            if (!dirFile.exists())
                Log.e("jcy-TAG", "callSysCameraAppToRecordVideo() isMkdirs " + dirFile.mkdirs());
        }

        try {
            Log.e("jcy-TAG", "callSysCameraAppToRecordVideo() isCreate " + file.createNewFile());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("jcy-TAG", "callSysCameraAppToRecordVideo() IOException "+e);
            file = null;
        }

        return file;
    }

    // Return to previous Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (ImagePicker.REQUEST_CODE == requestCode) {
                isUploadImage = true;
                isUploadVideo = false;
            } else if (PICK_VIDEO == requestCode || VIDEO_RECORD_CODE == requestCode) {
                isUploadImage = false;
                isUploadVideo = true;
            }
            assert data != null;
            uploader.uploadFile(data.getData(), 0, questionSetId);
        }
    }

    // Description: A list of questions that are in User's database,
    // Precondition: Database exists
    // Postcondition: Shows all questions in the linear layout and allows user
    //                to call WriteQuestions in order to modify them
    ValueEventListener valueEventListener = new ValueEventListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            container.removeAllViews();
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                // Check if dataSnapshot is not 'counter' since it is the only child
                // that is not of class Question
                if (Objects.equals(dataSnapshot.getKey(), "image")) {
                    String url = (String) dataSnapshot.getValue();
                    if (!TextUtils.isEmpty(url) && !isUploadImage) {
                        System.out.println("Stuff is called");
                        Glide.with(CreateQuestions.this).load(url).into(imageCover);
                    }
                } else if (Objects.equals(dataSnapshot.getKey(), "video")) {
                    String url = (String) dataSnapshot.getValue();
                    if (!TextUtils.isEmpty(url)) {
                        videoView.setVisibility(View.VISIBLE);
                        addVideo.setText(R.string.reupload_video);
                        videoView.setVideoPath(url);
                        videoView.requestFocus();
                        videoView.setOnPreparedListener(mp -> {
                            mp.setLooping(true);
                            videoView.start();
                        });
                    }
                }
                if (!Objects.equals(dataSnapshot.getKey(), "counter") && !Objects.equals(dataSnapshot.getKey(), "image")
                        && !Objects.equals(dataSnapshot.getKey(), "video") && !Objects.equals(dataSnapshot.getKey(), "complete")
                        && !Objects.equals(dataSnapshot.getKey(), "weight") && !Objects.equals(dataSnapshot.getKey(), "audio")) {
                    Question question = dataSnapshot.getValue(Question.class);
                    Button button = new Button(CreateQuestions.this);
                    assert question != null;
                    button.setText(dataSnapshot.getKey() + ". " + question.getQuestion());
                    button.setGravity(Gravity.CENTER);
                    // Include question id into extra
                    int ting = Integer.parseInt(Objects.requireNonNull(dataSnapshot.getKey()).substring(1));
                    button.setOnClickListener(v -> {
                        Intent moveToWriteQuestions = new Intent(CreateQuestions.this, WriteQuestions.class);
                        moveToWriteQuestions.putExtra("questionId", ting);
                        moveToWriteQuestions.putExtra("questionSetId", questionSetId);
                        CreateQuestions.this.startActivity(moveToWriteQuestions);
                    });
                    container.addView(button);
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };
    private String getFilePath(Context mContext) {
        if (mContext == null)
            return "";
        return mContext.getFilesDir().getAbsolutePath();
    }
    private void showcaseQuestions() {
        container = findViewById(R.id.questions_list);
        databaseReference.addValueEventListener(valueEventListener);
    }

    private void getVideoByGallery() {
        Intent video = new Intent();
        video.setType("video/*");
        video.setAction(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(Intent.createChooser(video, "Select Video"), PICK_VIDEO);
    }

    ProgressDialog mUploading;

    private void showUploading(long curSize, long allSize) {
        if (mUploading == null) {
            mUploading = new ProgressDialog(this);
            mUploading.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mUploading.setCancelable(true);
            mUploading.setCanceledOnTouchOutside(false);
            mUploading.setIcon(R.drawable.logo);
            mUploading.setTitle(getString(R.string.upload_wait));
            mUploading.setMax(100);
            mUploading.setMessage(getString(R.string.uploading));
            mUploading.show();
        } else {
            int progress = ((int) (((double) curSize) / allSize * 100));
            mUploading.incrementProgressBy(progress);
        }
    }

    private void hideUploading() {
        if (mUploading != null) {
            mUploading.dismiss();
            mUploading = null;
        }
    }

    @Override
    public void onClick(View v) {
        View customView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialogue_choose, null);
        AppCompatTextView leftPickText = customView.findViewById(R.id.left_pick_text), rightPickText = customView.findViewById(R.id.right_pick_text);
        AppCompatImageView leftPickIcon = customView.findViewById(R.id.left_pick_icon), rightPickIcon = customView.findViewById(R.id.right_pick_icon);
        leftPickText.setText(R.string.title_camera);
        rightPickText.setText(R.string.title_gallery);
        leftPickIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_photo_camera_black_48dp));
        rightPickIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_photo_black_48dp));
        AlertDialog dialogue = new AlertDialog.Builder(CreateQuestions.this)
                .setTitle(R.string.title_choose)
                .setView(customView)
                .setNegativeButton(R.string.action_cancel, null)
                .setOnCancelListener(null).create();
        dialogue.show();

        // Handle Record option click
        customView.findViewById(R.id.left_pick).setOnClickListener(v1 -> {
            ImagePicker.Companion.with(CreateQuestions.this).cropSquare().cameraOnly().start();
            dialogue.dismiss();
        });

        // Handle Browse option click
        customView.findViewById(R.id.right_pick).setOnClickListener(v2 -> {
            ImagePicker.Companion.with(CreateQuestions.this).cropSquare().galleryOnly().start();
            dialogue.dismiss();
        });
    }

    public void onDestroy() {
        super.onDestroy();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionSet = snapshot.getValue(QuestionSet.class);
                assert questionSet != null;
                if (questionSet.getCounter() == 5 && !questionSet.getImage().isEmpty()
                        && !questionSet.getVideo().isEmpty()) {
                    databaseReference.child("complete").setValue(true);
                    Toast.makeText(CreateQuestions.this, R.string.question_set_saved, Toast.LENGTH_SHORT).show();
                } else if (questionSet.getCounter() == 0 && questionSet.getImage().isEmpty()
                        && questionSet.getVideo().isEmpty()) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("questionSets").child("counter");
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            counter = snapshot.getValue(Integer.class);
                            DatabaseReference remove = FirebaseDatabase.getInstance().getReference("questionSets").child(counter + "");
                            remove.child("complete").removeValue();
                            remove.child("counter").removeValue();
                            remove.child("image").removeValue();
                            remove.child("video").removeValue();
                            remove.child("weight").removeValue();
                            ref.setValue(--counter);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w("Database Error", "loadPost:onCancelled", error.toException());
                        }
                    });
                } else {
                    Toast.makeText(CreateQuestions.this, R.string.question_set_draft, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("Database Error", "loadPost:onCancelled", error.toException());
            }
        });
    }

    protected boolean checkPermissions(String[] neededPermissions) {
        if (CreateQuestions.NEEDED_PERMISSIONS == null || CreateQuestions.NEEDED_PERMISSIONS.length == 0)
            return true;
        boolean allGranted = true;
        for (String neededPermission : CreateQuestions.NEEDED_PERMISSIONS)
            allGranted = allGranted && ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isAllGranted = true;
        for (int grantResult : grantResults)
            isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
        afterRequestPermission(requestCode, isAllGranted);
    }

    private void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                isExternalStorageManager();
                callSysCameraAppToRecordVideo();
            } else
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
        }
    }
}