package com.danielqueiroz.drinkwater;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button btnNotify;
    private EditText editMinutes;
    private TimePicker timePicker;

    private boolean activated;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("db_drinkwater", Context.MODE_PRIVATE);

        btnNotify = findViewById(R.id.btn_notify);
        editMinutes = findViewById(R.id.edit_txt_number_interval);
        timePicker = findViewById(R.id.time_picker);
        activated = preferences.getBoolean("activated", false);

        setupUI(activated, preferences);
        timePicker.setIs24HourView(true);
    }

    public void notifyClick(View view) {
        if (!activated){
            if (!intervalIsValid()) return;

            int hour = timePicker.getCurrentHour();
            int minute = timePicker.getCurrentMinute();
            int interval = Integer.parseInt(editMinutes.getText().toString());

            updateStorage(true, interval, hour, minute);
            setupUI(true, preferences);
            setupNotification(true,interval, hour, minute );
            alert(R.string.notified);

            activated = true;
        } else {
            updateStorage(false, 0,0,0);
            setupUI(false, preferences);
            setupNotification(false,0,0,0 );
            alert(R.string.notified_pause);

            activated = false;
        }

    }

    private boolean intervalIsValid(){
        String sInterval = editMinutes.getText().toString();
        if (sInterval.isEmpty() || sInterval.equals("0")){
            alert(R.string.not_valid);
            return false;
        }
        return true;
    }

    private void alert(int resId){
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void setupUI(boolean activated, SharedPreferences preferences){
        if (activated) {
            btnNotify.setText(R.string.pause);
            btnNotify.setBackgroundResource(R.drawable.bg_button_background);
            editMinutes.setText(String.valueOf(preferences.getInt("interval", 0)));
            timePicker.setCurrentHour(preferences.getInt("hour", timePicker.getCurrentHour()));
            timePicker.setCurrentMinute(preferences.getInt("minute", timePicker.getCurrentMinute()));
        } else {
            btnNotify.setText(R.string.notify);
            btnNotify.setBackgroundResource(R.drawable.bg_button_background_accent);
        }
    }

    private void updateStorage(boolean added, int interval, int hour, int minute){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("activated", added);

        if (added){
            editor.putInt("interval", interval);
            editor.putInt("hour", hour);
            editor.putInt("minute", minute);
        } else  {
            editor.remove("interval");
            editor.remove("hour");
            editor.remove("minute");
        }

        editor.apply();
    }

    private void setupNotification(boolean added, int interval, int hour, int minute){
        Intent notificationIntent = new Intent(MainActivity.this, NotificationPublisher.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (added){
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            notificationIntent.putExtra(NotificationPublisher.KEY_NOTIFICATION, "Hora de beber água");
            notificationIntent.putExtra(NotificationPublisher.KEY_NOTIFICATION_ID, 1);

            PendingIntent broadcast = PendingIntent.getBroadcast(MainActivity.this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval * 60 * 1000, broadcast);
        } else {
            PendingIntent broadcast = PendingIntent.getBroadcast(MainActivity.this, 0, notificationIntent, 0);
            alarmManager.cancel(broadcast);
        }

    }

}