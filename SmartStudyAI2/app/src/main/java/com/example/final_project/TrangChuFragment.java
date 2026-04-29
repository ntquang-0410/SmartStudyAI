package com.example.final_project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TrangChuFragment extends Fragment {
    private TextView tvGreeting;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trang_chu, container, false);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (tvGreeting != null) {
            loadUserProfile();
        }

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User userProfile = snapshot.getValue(User.class);
                    if (userProfile != null) {
                        String name = userProfile.fullName;
                        if (name != null) {
                            String[] nameParts = name.trim().split(" ");
                            String firstName = nameParts[nameParts.length - 1]; // Lấy tên hiển thị
                            tvGreeting.setText("Chào buổi sáng, " + firstName + "!");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvGreeting.setText("Chào buổi sáng!");
                }
            });
        }
    }
}
