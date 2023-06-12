package com.itjustworks.memorylane;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Objects;

/*
 * Uploader.java
 *
 * Class Description: Uploads media files with certain naming convention based on extension.
 * Class Invariant: Images,
 *                  Videos,
 *                  Audio.
 *
 */

public class Uploader {
    private String typeOfFile;
    private final Context context;
    private final DatabaseReference databaseRef;
    private final StorageReference storageRef;
    private final UploaderListener mUploaderListener;


    public Uploader(Context context, UploaderListener mUploaderListener, DatabaseReference databaseRef, StorageReference storageRef) {
        this.context = context;
        this.mUploaderListener = mUploaderListener;
        this.databaseRef = databaseRef;
        this.storageRef = storageRef;
    }

    public void uploadFile(Uri uri, int questionId, int questionSetId) {
        if (uri != null) {
            // Determine whether video, audio, or image
            switch (getFileExtension(uri)) {
                case "png":
                case "jpg":
                case "jpeg":
                    typeOfFile = "image";
                    break;
                case "mp4":
                    typeOfFile = "video";
                    break;
                case "mp3":
                    typeOfFile = "audio";
                    break;
                default:
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show();
            }

            StorageReference fileReference;
            if (Objects.equals(typeOfFile, "audio"))
                fileReference = storageRef.child("hint" + questionSetId + questionId + "." + getFileExtension(uri));
            else
                fileReference = storageRef.child(typeOfFile + questionSetId + "." + getFileExtension(uri));
            UploadTask uploadTask = fileReference.putFile(uri);

            Task<Uri> urlTask = uploadTask.addOnProgressListener(snapshot -> {
                if (mUploaderListener != null) {
                    mUploaderListener.onProgress(snapshot.getBytesTransferred(), snapshot.getTotalByteCount());
                } else {
                    Toast.makeText(context, R.string.uploading, Toast.LENGTH_SHORT).show();
                }

            }).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }

                // Continue with the task to get the download URL
                return fileReference.getDownloadUrl();
            });

            urlTask.addOnSuccessListener(uri1 -> {
                        Toast.makeText(context, R.string.uploaded, Toast.LENGTH_SHORT).show();
                        databaseRef.child(typeOfFile).setValue(uri1.toString());
                        if (mUploaderListener != null) {
                            mUploaderListener.success(uri, uri1);
                        }
                        if (Objects.equals(typeOfFile, "image")) {

                        }
                        if (Objects.equals(typeOfFile, "audio")) {
                            databaseRef.child("q" + questionId).child("hint").setValue(uri1.toString());
                        } else {
                            databaseRef.child(typeOfFile).setValue(uri1.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (mUploaderListener != null) {
                            mUploaderListener.failed(e);
                        }
                        Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public String getFileExtension(Uri uri) {
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT))
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(uri));
        else
            return MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
    }

    //上传回调
    public interface UploaderListener {
        void success(Uri localUri, Uri uploadSessionUri);

        void failed(Exception e);

        void onProgress(long curSize, long allSize);
    }
}
