package com.inventrax.falconsl_new.logout;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;


import com.inventrax.falconsl_new.activities.LoginActivity;
import com.inventrax.falconsl_new.interfaces.MainActivityView;
import com.inventrax.falconsl_new.util.ProgressDialogUtils;
import com.inventrax.falconsl_new.util.SharedPreferencesUtils;


public class LogoutUtil {

    private FragmentActivity fragmentActivity;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private Activity activity;
    private MainActivityView mainActivityView;



    public void setFragmentActivity(FragmentActivity fragmentActivity) {
        this.fragmentActivity = fragmentActivity;
    }

    public void setSharedPreferencesUtils(SharedPreferencesUtils sharedPreferencesUtils) {
        this.sharedPreferencesUtils = sharedPreferencesUtils;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        new ProgressDialogUtils(activity);
    }



    public void setMainActivityView(MainActivityView mainActivityView) {
        this.mainActivityView = mainActivityView;
    }











    public void doLogout(){

        try
        {
            Intent loginIntent = new Intent(activity, LoginActivity.class);
            activity.startActivity(loginIntent);
            sharedPreferencesUtils.removePreferences("url");

            //Toast.makeText(activity, "You have successfully logged out", Toast.LENGTH_LONG).show();
            activity.finish();

        }catch (Exception ex){

        }

    }


}
