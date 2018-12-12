package com.example.user.groceryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.SignInStateChangeListener;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserRequest;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserResult;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class LaunchActivity extends AppCompatActivity {

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        showSignIn();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pref = getSharedPreferences("preferences", MODE_PRIVATE);
        editor = pref.edit();

        IdentityManager.getDefaultIdentityManager().addSignInStateChangeListener(new SignInStateChangeListener() {
            @Override
            // Sign-in listener
            public void onUserSignedIn() {

                //Todo: Upload device token to S3
                String token = FirebaseInstanceId.getInstance().getToken();
                Log.d("Device Token:",token);

                CognitoUserPool userpool = new CognitoUserPool(getBaseContext(), new AWSConfiguration(getBaseContext()));
                String user = userpool.getCurrentUser().getUserId();
                Log.d("User ------------- :",user);

                editor.putString("user", user);
                editor.putString("token", token);
                editor.commit();

                ///
                try {
                    String message = "message";
                    FileOutputStream fos = openFileOutput("test.txt", Context.MODE_PRIVATE);
                    OutputStreamWriter outputWriter = new OutputStreamWriter(fos);
                    outputWriter.write(message);
                    outputWriter.close();
                    Toast.makeText(getApplicationContext(),"Data written to test.txt in internal storage",Toast.LENGTH_SHORT).show();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ///

                System.out.print("ruerr");
            }

            // Sign-out listener
            @Override
            public void onUserSignedOut() {

                // return to the sign-in screen upon sign-out

                //Todo: Remove device token from S3
                showSignIn();
            }
        });

        IdentityManager.getDefaultIdentityManager().signOut();

    }
public void showSignIn()
{
    AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
        @Override
        public void onComplete(AWSStartupResult awsStartupResult) {
            SignInUI signin = (SignInUI) AWSMobileClient.getInstance().getClient(
                    LaunchActivity.this,
                    SignInUI.class);
            signin.login(
                    LaunchActivity.this,
                    HomePage.class).execute();
        }
    }).execute();

}
}
