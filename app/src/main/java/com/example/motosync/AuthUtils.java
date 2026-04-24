package com.example.motosync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;

public class AuthUtils {

    public static void logoutUser(Activity activity) {
        // 1. Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // 2. Clear locally stored user data
        SharedPreferences prefs = activity.getSharedPreferences("MotoSyncPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // 3. Redirect to Login and clear history
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);

        // 4. Finish the current activity
        activity.finish();
    }
}