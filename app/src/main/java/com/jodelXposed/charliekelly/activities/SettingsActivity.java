package com.jodelXposed.charliekelly.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.iml.ActionProcessButton;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.jodelXposed.R;
import com.jodelXposed.charliekelly.asynctasks.GeocoderAsync;
import com.jodelXposed.krokofant.utils.Settings;
import com.spazedog.lib.rootfw4.RootFW;
import com.spazedog.lib.rootfw4.utils.Device;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;

import static com.jodelXposed.krokofant.utils.Log.xlog;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, GeocoderAsync.OnGeoListener {

    private int PLACEPICKER_REQUEST = 0;
    private Settings mSettings = Settings.getInstance();
    private SwitchCompat chkIsActive;
    private ActionProcessButton btnSelectPosition;
    private ActionProcessButton btnResetDefaults;
    private ActionProcessButton btnRestartJodel;
    private static final int REQUEST_CODE_PERMISSIONS = 200;
    static Boolean isTouched = false;
    public static String currentlocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }

        this.btnSelectPosition = (ActionProcessButton) findViewById(R.id.btn_select_position);
        this.btnResetDefaults = (ActionProcessButton) findViewById(R.id.btn_reset_defaults);
        this.btnRestartJodel = (ActionProcessButton) findViewById(R.id.btn_restart_jodel);

        assert this.btnSelectPosition != null;
        assert this.btnResetDefaults != null;
        this.btnSelectPosition.setOnClickListener(this);
        this.btnResetDefaults.setOnClickListener(this);
        this.btnRestartJodel.setOnClickListener(this);

        this.btnSelectPosition.setMode(ActionProcessButton.Mode.ENDLESS);

        try {
            mSettings.load();
        } catch (JSONException | IOException e) {
            xlog(e.getMessage());
        }
        setInformation();

        RootFW.connect();
    }

    private void setOnClickListener() {
        chkIsActive.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                isTouched = true;
                return false;
            }
        });

        chkIsActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    mSettings.setActive(chkIsActive.isChecked());
                    Log.d("isactive", String.valueOf(chkIsActive.isChecked()));
                    mSettings.save();
                    setInformation();

                    Toast.makeText(getApplicationContext(), "Saved settings to file\n" + mSettings.toJson(), Toast.LENGTH_LONG).show();

                } catch (JSONException | IOException e) {
                    xlog(e.getMessage());
                }
            }
        });
    }

    private void setInformation() {
        TextView tvLat = (TextView) findViewById(R.id.tvLat);
        TextView tvLng = (TextView) findViewById(R.id.tvLng);
        TextView tvCity = (TextView) findViewById(R.id.tvCity);
        TextView tvCountry = (TextView) findViewById(R.id.tvCountry);
        TextView tvCountrycode = (TextView) findViewById(R.id.tvCountrycode);
        assert tvLat != null;
        tvLat.setText("Lat: " + String.valueOf(mSettings.getLat()));
        assert tvLng != null;
        tvLng.setText("Lng: " + String.valueOf(mSettings.getLng()));
        assert tvCity != null;
        tvCity.setText("City: " + String.valueOf(mSettings.getCity()));
        assert tvCountry != null;
        tvCountry.setText("Country: " + String.valueOf(mSettings.getCountry()));
        assert tvCountrycode != null;
        tvCountrycode.setText("Countrycode: " + String.valueOf(mSettings.getCountryCode()));
    }

    @Override
    public void onClick(View v) {
        int viewID = v.getId();

        if (viewID == this.btnSelectPosition.getId())
            this.pickLocation();

        if (viewID == this.btnResetDefaults.getId()) {
            mSettings.createDefaultFile(new File(Settings.settingsPath));
        }

        if (viewID == this.btnRestartJodel.getId()) {
            Device.Process process = RootFW.getProcess("com.tellm.android.app");
            if (process.kill())
                openApp(this, "com.tellm.android.app");
            Log.d("Killed", "Jodel!");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.toggleservice);
        this.chkIsActive = (SwitchCompat) MenuItemCompat.getActionView(item);
        chkIsActive.setChecked(mSettings.isActive());
        setOnClickListener();
        return super.onCreateOptionsMenu(menu);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        List<String> permissions = new ArrayList<String>();
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Permissions", "Permission Granted: " + permissions[i]);
                        setInformation();
                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.scrollview), "Permission denied, this app wont work properly!", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Check that", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //open up app settings page
                                    startActivity(
                                        new Intent().setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null)));
                                }
                            });
                        snackbar.show();
                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    /**
     * Open a Place picker, zoomed in on coordinates from settings
     */
    private void pickLocation() {
        //Start place picker with these coordinates in the center
        double lat = mSettings.getLat();
        double lng = mSettings.getLng();

        xlog(String.format("Loaded latlng from Settings:Lat: %s, Lng: %s", lat, lng));

        PlacePicker.IntentBuilder i = new PlacePicker.IntentBuilder();
        i.setLatLngBounds(new LatLngBounds(
            new LatLng((lat - 0.20), (lng - 0.20)),
            new LatLng((lat + 0.20), (lng + 0.20))
        ));


        try {
            xlog("Opening maps app");
            //Intent in = i.build((Activity)context);
            startActivityForResult(i.build(this), PLACEPICKER_REQUEST);
        } catch (Exception ex) {
            xlog("Error opening maps:");
            xlog(ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        xlog("onActivityResult requestcode: " + requestCode);

        if (resultCode != RESULT_OK) {
            xlog("Error, resultCode: " + String.valueOf(resultCode));
            return;
        }

        if (requestCode == PLACEPICKER_REQUEST) {
            xlog("Recieved data from placepicker activity");

            if (data == null) {
                xlog("data was null");
                return;
            }
            this.btnSelectPosition.setProgress(1);
            Place place = PlacePicker.getPlace(this, data);
            this.setSettingsFromPlace(place);

        }
    }

    /**
     * Extract location properties from place object and insert to the Settings-object, then save settings to file
     *
     * @param place
     */
    private void setSettingsFromPlace(Place place) {
        LatLng latlng = place.getLatLng();
        double lat = latlng.latitude;
        double lng = latlng.longitude;

        mSettings.setLat(lat);
        mSettings.setLng(lng);

        //Get location data on seperate thread, then call onGeoFinished
        new GeocoderAsync(lat, lng, this, this).execute();
    }

    @Override
    public void onGeoFinished(List<Address> addresses) {
        if (addresses == null) {
            Toast.makeText(this, "No addresses nearby, unable to set location", Toast.LENGTH_SHORT).show();
            return;
        }

        int length = addresses.size();
        boolean save = false;

        //Find an adress that has all needed indexes
        for (int i = 0; i < length; i++) {
            Address a = addresses.get(i);
            String locality = a.getLocality();
            String country = a.getCountryName();
            String countryCode = a.getCountryCode();

            if (locality == null) {
                xlog("Locality was null");
                this.btnSelectPosition.setProgress(-1); //error in geofetching
                resetProgress();
                continue;
            }

            if (country == null) {
                xlog("Country was null");
                this.btnSelectPosition.setProgress(-1);
                resetProgress();
                continue;
            }

            if (countryCode == null) {
                xlog("CountryCode was null");
                this.btnSelectPosition.setProgress(-1);
                resetProgress();
                continue;
            }

            mSettings.setCity(locality);
            mSettings.setCountry(country);
            mSettings.setCountryCode(countryCode);
            save = true;
            setInformation();
            this.btnSelectPosition.setProgress(100);
            resetProgress();
            Snackbar snackbar = Snackbar
                .make(findViewById(R.id.scrollview), "For changes to take effect please restart Jodel!", Snackbar.LENGTH_LONG)
                .setAction("Okay", null);
            snackbar.show();
            break;
        }

        //If we found an adress with all indexes, save to file
        if (save) {
            try {
                mSettings.save();
                Toast.makeText(getApplicationContext(), "Location saved", Toast.LENGTH_SHORT).show();
            } catch (JSONException | IOException e) {
                xlog(e.getLocalizedMessage());
            }

        } else {
            Toast.makeText(getApplicationContext(), "Could not save location, try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetProgress() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                btnSelectPosition.setProgress(0); //reset progress after 2,5sec
            }
        }, 2500);
    }

    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                throw new PackageManager.NameNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
