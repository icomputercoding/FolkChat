package in.icomputercoding.folkchat.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import in.icomputercoding.folkchat.databinding.ActivityPhoneScreenBinding;

public class PhoneActivity extends AppCompatActivity {

    ActivityPhoneScreenBinding binding;
    private long pressedTime;
    FirebaseAuth auth;
    String phone;
    String MobilePattern = "[0-9]{10}";
    ProgressDialog dialog;

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Tap Again To Exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(PhoneActivity.this);

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("OTP Sending");
        dialog.setMessage("Please Wait...");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        binding.NextBtn.setOnClickListener(v -> {
            dialog.show();
            phone = Objects.requireNonNull(binding.phoneNumber.getEditText()).getText().toString();
            String phoneNo = "+" + binding.countryCode.getSelectedCountryCode() + phone;
            if (phone.isEmpty()  || !phone.matches(MobilePattern)) {
                binding.phoneNumber.setError("Enter a valid mobile number");
            } else {
                PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNo)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(PhoneActivity.this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                dialog.dismiss();
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                dialog.dismiss();
                                Toast.makeText(PhoneActivity.this,e.getMessage(), Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onCodeSent(@NonNull String CodeOTP, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(CodeOTP, forceResendingToken);
                                Intent intent = new Intent(PhoneActivity.this, OTPVerifyActivity.class);
                                intent.putExtra("phoneNumber", phoneNo);
                                intent.putExtra("CodeOTP",CodeOTP);
                                startActivity(intent);
                                finishAffinity();
                            }

                        }).build();

                PhoneAuthProvider.verifyPhoneNumber(options);


            }


        });
    }


}