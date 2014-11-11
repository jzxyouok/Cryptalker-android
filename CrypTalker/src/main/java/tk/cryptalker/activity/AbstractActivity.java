package tk.cryptalker.activity;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import tk.cryptalker.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class AbstractActivity extends Activity
{
    static Context context;

    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private int menu;

    public void makeLayout(int layout, int title, int menu)
    {
        context = getApplicationContext();
        setContentView(layout);

        setTitle(context.getString(title));

        this.menu = menu;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(this.menu, menu);

        getActionBar().setIcon(R.drawable.ic_drawer);

        return super.onCreateOptionsMenu(menu);
    }

    public void attachPageChange(int element, Class destination)
    {
        Button button = (Button)findViewById(element);
        button.setOnClickListener(pageChange(button, context, destination));
    }

    private OnClickListener pageChange(final Button button, final Context context, final Class destination)
    {
        return new OnClickListener() {

            public void onClick(View view) {
                if (view == button) {
                    Intent intent = new Intent(context, destination);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        };
    }

    public static Context getContext()
    {
        return context;
    }

    public boolean validation(ArrayList<TextView> textViews)
    {
        // Reset validation
        Boolean validated = true;

        for (final TextView textView : textViews) {

            // Reset Errors
            textView.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (textView.getText().length() > 0) {
                        textView.setError(null);
                    }
                }
            });


            // Check
            String content = textView.getText().toString();
            if (content == null || content.length() == 0) {
                textView.setError(errorMessage(R.string.validation_input_required));
                validated = false;
            }
        }

        return validated;
    }

    public SpannableStringBuilder errorMessage(int reference)
    {
        return errorMessage(context.getString(reference));
    }

    public SpannableStringBuilder errorMessage(String string)
    {
        int color = Color.RED;
        ForegroundColorSpan fgcspan = new ForegroundColorSpan(color);
        SpannableStringBuilder ssbuilder = new SpannableStringBuilder(string);
        ssbuilder.setSpan(fgcspan, 0, string.length(), 0);

        return ssbuilder;
    }

    public void parseJsonErrors(JSONObject errors)
    {
        JSONObject error_messages;

        try {
            error_messages = errors.getJSONObject("messages");

            if (error_messages.length() < 0) {
                return;
            }

            // Iterate errors from JSONObject
            Iterator iterator = error_messages.keys();
            while(iterator.hasNext()){
                String key = (String)iterator.next();
                JSONObject message = error_messages.getJSONObject(key);
                String value = message.getString("message");

                TextView textView = (TextView)findViewById(getResources().getIdentifier(key, "id", getPackageName()));
                textView.setError(errorMessage(value));
                textView.requestFocus();
            }

            Log.i("AbstractActivity@parseJsonErrors", error_messages.toString());
        } catch (JSONException e) {
            Log.i("AbstractActivity@parseJsonErrors", e.toString());
        }
    }

    public void parseJsonErrorsDialog(JSONObject errors, AlertDialog dialog)
    {
        JSONObject error_messages;

        try {
            error_messages = errors.getJSONObject("messages");

            if (error_messages.length() < 0) {
                return;
            }

            // Iterate errors from JSONObject
            Iterator iterator = error_messages.keys();
            while(iterator.hasNext()){
                String key = (String)iterator.next();
                JSONObject message = error_messages.getJSONObject(key);
                String value = message.getString("message");

                TextView textView = (TextView)dialog.findViewById(getResources().getIdentifier(key, "id", getPackageName()));
                textView.setError(errorMessage(value));
                textView.requestFocus();
            }

            Log.i("AbstractActivity@parseJsonErrors", error_messages.toString());
        } catch (JSONException e) {
            Log.i("AbstractActivity@parseJsonErrors", e.toString());
        }
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    public void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences();
        int appVersion = getAppVersion(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    public SharedPreferences getGcmPreferences() {

        return getSharedPreferences(AbstractActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public void storeToken(String token) {
        final SharedPreferences prefs = getGcmPreferences();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_TOKEN, token);
        editor.commit();
    }

    public String getToken() {
        final SharedPreferences prefs = getGcmPreferences();
        String token = prefs.getString(PROPERTY_TOKEN, "");

        return token;
    }
}