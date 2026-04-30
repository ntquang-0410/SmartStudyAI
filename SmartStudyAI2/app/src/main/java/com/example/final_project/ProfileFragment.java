package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvUserEmail;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);

        MaterialCardView cvReminders = view.findViewById(R.id.cvReminders);
        MaterialCardView cvPassword = view.findViewById(R.id.cvPassword);
        MaterialCardView cvLogout = view.findViewById(R.id.cvLogout);

        loadUserProfile();

        // Implement click listeners
        cvReminders.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng nhắc nhở đang phát triển", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to Study Reminders screen
        });

        cvPassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng đổi mật khẩu đang phát triển", Toast.LENGTH_SHORT).show();
            // TODO: Implement change password flow
        });

        cvLogout.setOnClickListener(v -> logoutUser());

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();

            if (email != null) {
                tvUserEmail.setText(email);
            }

            mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User userProfile = snapshot.getValue(User.class);
                    if (userProfile != null && userProfile.fullName != null) {
                        tvUserName.setText(userProfile.fullName);
                    } else {
                        tvUserName.setText("Người dùng");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvUserName.setText("Lỗi tải thông tin");
                }
            });
        } else {
            tvUserName.setText("Khách");
            tvUserEmail.setText("Chưa đăng nhập");
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), WelcomeActivity.class);
        // Clear all previous activities
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
