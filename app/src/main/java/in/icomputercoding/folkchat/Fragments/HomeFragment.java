package in.icomputercoding.folkchat.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import in.icomputercoding.folkchat.Adapters.TopStatusAdapter;
import in.icomputercoding.folkchat.Adapters.UsersAdapter;
import in.icomputercoding.folkchat.Chats.ChatFragment;
import in.icomputercoding.folkchat.Model.Post;
import in.icomputercoding.folkchat.Model.Status;
import in.icomputercoding.folkchat.Model.User;
import in.icomputercoding.folkchat.Model.UserStatus;
import in.icomputercoding.folkchat.databinding.FragmentHomeBinding;


public class HomeFragment extends Fragment {

    FragmentHomeBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog dialog;
    User user;
    FirebaseApp app;
    ActivityResultLauncher<Intent> story;
    RecyclerView storyRV, dashboardRv;
    ArrayList<StoryModel> storyList;
    ArrayList<Post> postList;
    ImageView addStory;
    FirebaseDatabase database;
    FirebaseAuth auth;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
         database = FirebaseDatabase.getInstance()
         auth = FirebaseDatabase.getInstance()
        binding = FragmentHomeBinding.inflate(inflater,container,false);



        binding.chats.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), ChatFragment.class);
            startActivity(i);
        });

         app = FirebaseApp.initializeApp(requireContext());
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);


        database = FirebaseDatabase.getInstance(app);

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("token", token);
                    database.getReference()
                            .child("users")
                            .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                            .updateChildren(map);
                });


        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);


        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Post post = dataSnapshot.getValue(Post.class);
                    postList.add(post);
                }
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        database.getReference().child("users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        usersAdapter = new UsersAdapter(getContext(), users);
        statusAdapter = new TopStatusAdapter(getContext(), userStatuses);
        UserStatus users = new UserStatus();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);

        binding.statusList.showShimmerAdapter();

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userStatuses.clear();
                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        status.setLastUpdated(Objects.requireNonNull(storySnapshot.child("lastUpdated").getValue(Long.class)));

                        ArrayList<Status> statuses = new ArrayList<>();

                        for (DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {
                            Status sampleStatus = statusSnapshot.getValue(Status.class);
                            statuses.add(sampleStatus);
                        }

                        status.setStatuses(statuses);
                        userStatuses.add(status);
                    }
                    binding.statusList.hideShimmerAdapter();
                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        story = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    if (data.getData() != null) {
                        dialog.show();
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        Date date = new Date();
                        StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");

                        reference.putFile(data.getData()).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getName());
                                    userStatus.setProfileImage(user.getProfileImage());
                                    userStatus.setLastUpdated(date.getTime());

                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("name", userStatus.getName());
                                    obj.put("profileImage", userStatus.getProfileImage());
                                    obj.put("lastUpdated", userStatus.getLastUpdated());

                                    String imageUrl = uri.toString();
                                    Status status = new Status(imageUrl, userStatus.getLastUpdated());

                                    database.getReference()
                                            .child("stories")
                                            .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                                            .updateChildren(obj);

                                    database.getReference().child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses")
                                            .push()
                                            .setValue(status);

                                    dialog.dismiss();
                                });
                            }
                        });
                    }
                }
            }
        });



        //Dashboard Recycler View
      /*  posts = new ArrayList<>();
        posts.add(new Posts(R.drawable.hipster,R.drawable.new_hope,"Shubham Wable","Bank Manager","150","10","33"));
        posts.add(new Posts(R.drawable.dennis,R.drawable.dennis_kane,"Vaibhav Wable","Android Developer","250","50","50"));
        posts.add(new Posts(R.drawable.nature1,R.drawable.nature,"Sanket Nachankar","Owner Of Nachankar","444","200","20"));
        posts.add(new Posts(R.drawable.art,R.drawable.nature_dordogne,"Siddesh Bhosale","Bike Rider","1K","150","10")); */
      //  PostsAdapter postsAdapter = new PostsAdapter(posts,getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.dashboardRV.setLayoutManager(linearLayoutManager);
        binding.dashboardRV.addItemDecoration(new DividerItemDecoration(binding.dashboardRV.getContext(), DividerItemDecoration.VERTICAL));
        binding.dashboardRV.setNestedScrollingEnabled(false);
       // binding.dashboardRV.setAdapter(postsAdapter);

        binding.addStoryImg.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            story.launch(intent);
        });

        return binding.getRoot();

    }
}