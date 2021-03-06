package in.icomputercoding.folkchat.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import in.icomputercoding.folkchat.Model.User;
import in.icomputercoding.folkchat.databinding.ActivityProfileScreenBinding;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileScreenBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri imageUri;
    ProgressDialog dialog;
    FirebaseUser firebaseUser;
    ActivityResultLauncher<Intent> launcher;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(ProfileActivity.this);

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Profile Updating");
        dialog.setMessage("Please Wait...");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        uid = auth.getUid();

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    if (data.getData() != null) {
                        Uri uri = data.getData(); // filepath
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        long time = new Date().getTime();
                        StorageReference storageReference = storage.getReference().child("Profiles").child(time + "");
                        storageReference.putFile(uri).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                storageReference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                    String filePath = uri1.toString();
                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("image", filePath);
                                    database.getReference().child("users")
                                            .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                                            .updateChildren(obj).addOnSuccessListener(aVoid -> {

                                    });
                                });
                            }
                        });


                        binding.ProfileImage.setImageURI(data.getData());
                        imageUri = data.getData();
                    }
                }
            }
        });


        binding.ProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            launcher.launch(intent);
        });

        binding.SubmitBtn.setOnClickListener(v -> {
            String name = Objects.requireNonNull(binding.name.getEditText()).getText().toString();
            String bio = Objects.requireNonNull(binding.bio.getEditText()).getText().toString();

            if (name.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.show();
            if (imageUri != null) {
                StorageReference storageReference = storage.getReference().child("Profiles").child(Objects.requireNonNull(auth.getUid()));
                storageReference.putFile(imageUri).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            String phone = Objects.requireNonNull(auth.getCurrentUser()).getPhoneNumber();
                            User user = new User(uid, name, phone, imageUrl, bio);

                            database.getReference()
                                    .child("users")
                                    .child(uid)
                                    .setValue(user)
                                    .addOnSuccessListener(aVoid -> {
                                        dialog.dismiss();
                                        Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish();
                                    });
                        });
                    }
                });
            } else {
                String phone = Objects.requireNonNull(auth.getCurrentUser()).getPhoneNumber();
                User user = new User(uid, name, phone, "No Image",bio);
                database.getReference()
                        .child("users")
                        .child(Objects.requireNonNull(uid))
                        .setValue(user)
                        .addOnSuccessListener(aVoid -> {
                            dialog.dismiss();
                            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        });
            }


        });
    }
}