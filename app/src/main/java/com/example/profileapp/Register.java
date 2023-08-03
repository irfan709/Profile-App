package com.example.profileapp;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class Register extends AppCompatActivity {
    CircleImageView reg_user_img;
    TextInputLayout reg_email, reg_username, reg_pass;
    TextInputEditText reg_input_email, reg_input_uname, reg_input_pass;
    Button reg_btn;
    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private byte[] selectedImage;
    DbHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        reg_user_img = findViewById(R.id.reg_user_img);
        reg_email = findViewById(R.id.reg_email);
        reg_username = findViewById(R.id.reg_username);
        reg_pass = findViewById(R.id.reg_pass);
        reg_btn = findViewById(R.id.reg_btn);
        reg_input_email = findViewById(R.id.reg_input_email);
        reg_input_uname = findViewById(R.id.reg_input_uname);
        reg_input_pass = findViewById(R.id.reg_input_pass);
        dbHelper = new DbHelper(this);
        reg_user_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestStoragePermission();
            }
        });
        reg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Uri selectedImageUri = data.getData();
                        if (selectedImageUri != null) {
                            Bitmap bitmap = getBitmapFromUri(selectedImageUri);
                            if (bitmap != null) {
                                reg_user_img.setImageBitmap(bitmap);
                                selectedImage = getBytesFromBitmap(bitmap);
                            } else {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
    private void registerUser() {
        String email = Objects.requireNonNull(reg_input_email.getText()).toString();
        String username = Objects.requireNonNull(reg_input_uname.getText()).toString();
        String password = Objects.requireNonNull(reg_input_pass.getText()).toString();
        if (!validateEmail() | !validateUsername() | !validatePassword()) {
            return;
        } else {
            boolean b1 = dbHelper.checkUserExists(email, username);
            if (b1) {
                Toast.makeText(Register.this, "User already exists...", Toast.LENGTH_SHORT).show();
            }
            else {
                boolean b = dbHelper.registerUserHelper(email, username, password, selectedImage);
                if (b) {
                    Toast.makeText(Register.this, "Registered successfully!!", Toast.LENGTH_SHORT).show();
                    reg_input_email.setText("");
                    reg_input_uname.setText("");
                    reg_input_pass.setText("");
                    reg_user_img.setImageResource(R.drawable.ic_launcher_background);
                }
                else {
                    Toast.makeText(Register.this, "Failed to register...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showPermissionExplanationDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            }
        } else {
            openGallery();
        }
    }

    private void showPermissionExplanationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Storage Permission")
                .setMessage("This app needs access to your device storage")
                .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
                    }
                })
                .setNegativeButton("deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) || !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showPermissionSettingsDialog();
                }
            }
        }
    }

    private void showPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Storage settings")
                .setMessage("To use this app you need to grant the storage permission")
                .setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openAppSettings();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                return image;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

private byte[] getBytesFromBitmap(Bitmap bitmap) {
    if (bitmap != null) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    } else {
        return new byte[0];
    }
}

    public boolean validateEmail() {
        String email = Objects.requireNonNull(reg_email.getEditText()).getText().toString().trim();
        if (email.isEmpty()) {
            reg_email.setError("Field can't be empty");
            reg_input_email.requestFocus();
            return false;
        }  else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            reg_email.setError("Please enter valid email");
            return false;
        }
        else {
            reg_email.setError(null);
            return true;
        }
    }
    public boolean validateUsername() {
        String username = Objects.requireNonNull(reg_username.getEditText()).getText().toString();
        if (username.isEmpty()) {
            reg_username.setError("Field can't be empty");
            reg_input_uname.requestFocus();
            return false;
        }
        else if (username.length() > 20) {
            reg_username.setError("Username length length too long");
            reg_input_uname.requestFocus();
            return false;
        }
        else {
            reg_username.setError(null);
            return true;
        }
    }
    public boolean validatePassword() {
        String pass = Objects.requireNonNull(reg_pass.getEditText()).getText().toString();
        if (pass.isEmpty()) {
            reg_pass.setError("Field can't be empty");
            reg_input_pass.requestFocus();
            return false;
        }
        else if (!pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+=~`{}\\[\\]|:;\"'<>,.?\\\\/])[^\\s]{6,}$"
        )) {
            reg_pass.setError("Password too weak");
            reg_input_pass.requestFocus();
            return false;
        }
        else {
            reg_pass.setError(null);
            return true;
        }
    }
}