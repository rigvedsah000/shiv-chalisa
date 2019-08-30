package com.anteour.shivchalisa;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, AppCompatSeekBar.OnSeekBarChangeListener,
        FloatingActionButton.OnClickListener, NavigationView.OnNavigationItemSelectedListener, onPlayStateListener {

    private static final String CONTENT_ID = "content_id";
    private static final String AUDIO_ID = "audio_id";
    private static final String PLAY_STATE = "play_state";

    private static final String TITLE_SHARE_DIALOG = "Share via";
    private static final String MIME_TEXT = "text/plain";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private int contentID = R.id.shiv_chalisa;
    private int audioID = R.raw.shiv_chalisa;
    private boolean playState = false;
    private boolean orientationChanged = false;

    private MediaWorker mediaWorker = null;

    private FloatingActionButton floatingActionButton;
    private AppCompatSeekBar seekbar;
    private LinearLayout linearLayout;

    private TextView textView;
    private int contentResID;
    private int contentTitleID;

    private Timer timer = null;

    private InterstitialAd interstitialAd;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        setContentView(R.layout.activity_home);
        initializeAds();
        loadUI();
        initializeMediaWorker();
        onLoadInstanceState(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) drawerLayout.closeDrawers();
        else super.onBackPressed();
    }

    private void setContent(int contentID) {
        textView = findViewById(R.id.content_view);
        switch (contentID) {
            case R.id.shiv_chalisa:
                contentResID = R.string.content_shiv_chalisa;
                contentTitleID = R.string.title_shiv_chalisa;
                audioID = R.raw.shiv_chalisa;
                break;
            case R.id.mm_mantra:
                contentResID = R.string.content_mm_mantra;
                contentTitleID = R.string.title_mm_mantra;
                audioID = R.raw.mm_mantra;
                break;
            case R.id.lingashtakam:
                contentResID = R.string.content_lingashtakam;
                contentTitleID = R.string.title_lingashtakam;
                audioID = R.raw.lingashtakam;
                break;
            case R.id.shivashtakam:
                contentResID = R.string.content_shivashtakam;
                contentTitleID = R.string.title_shivashtakam;
                audioID = R.raw.shivashtakam;
                break;
            case R.id.panchakshara_strotam:
                contentResID = R.string.content_panchakshara_strotam;
                contentTitleID = R.string.title_panchakshara_strotam;
                audioID = R.raw.panchakshara_strotam;
                break;
        }
        toolbar.setTitle(contentTitleID);
        textView.setText(contentResID);
        this.contentID = contentID;
        if (!orientationChanged) {
            mediaWorker.resetWorker(this, audioID);
            this.playState = false;
            interruptSeekUpdateTask();
        } else orientationChanged = false;
        if (!playState) linearLayout.setVisibility(View.GONE);
        navigationView.getMenu().findItem(R.id.shiv_chalisa).setChecked(false);
        navigationView.getMenu().findItem(contentID).setChecked(true);
        if (interstitialAd.isLoaded()) {
            interstitialAd.show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(HomeActivity.CONTENT_ID, contentID);
        outState.putInt(HomeActivity.AUDIO_ID, audioID);
        outState.putBoolean(HomeActivity.PLAY_STATE, playState);
        super.onSaveInstanceState(outState);
    }

    private void onLoadInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.contentID = savedInstanceState.getInt(HomeActivity.CONTENT_ID, R.id.shiv_chalisa);
            this.audioID = savedInstanceState.getInt(HomeActivity.AUDIO_ID, R.raw.shiv_chalisa);
            this.playState = savedInstanceState.getBoolean(HomeActivity.PLAY_STATE, false);
            this.orientationChanged = true;
        }
        setContent(contentID);
    }

    @Override
    protected void onPause() {
        if (playState) mediaWorker.pause(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (playState) {
            mediaWorker.play(this);
            linearLayout.setVisibility(View.VISIBLE);
        } else
            floatingActionButton.setImageResource(R.drawable.ic_play);
        try {
            applyPreferences();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mediaWorker.destroy(this);
        super.onDestroy();
    }

    private void initializeToolbar() throws NullPointerException {
        toolbar = findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        Objects.requireNonNull(actionbar).setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_hamburger);
    }

    private void initializeContent() {
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(HomeActivity.this);
        seekbar = findViewById(R.id.seekBar);
        seekbar.setMax(100);
        seekbar.setOnSeekBarChangeListener(this);
    }

    private void initializeNaviagtionDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initializeMediaWorker() {
        mediaWorker = MediaWorker.getInstance(this);
    }

    private void loadUI() {
        initializeToolbar();
        initializeNaviagtionDrawer();
        initializeContent();
        linearLayout = findViewById(R.id.seekbar_layout);
    }

    private void applyPreferences() throws NullPointerException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String[] keys = getResources().getStringArray(R.array.settings_key);
        for (String key : keys) {
            String pref_val = sharedPreferences.getString(key, null);
            switch (key) {
                case "setting_lang":
                    Locale locale;
                    if (Objects.requireNonNull(pref_val).equals("English"))
                        locale = new Locale("en");
                    else locale = new Locale("hi");
                    attachLocaleToConfig(locale);
                    invalidateUI();
                    break;
                case "setting_size":
                    textView.setTextSize(Integer.parseInt(pref_val));
                    break;
                case "setting_color":
                    textView.setTextColor(Color.parseColor(pref_val));
                    break;
            }
        }

    }

    private void attachLocaleToConfig(Locale locale) {
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private void invalidateUI() {
        textView.setText(contentResID);
        for (int i = 0; i < 5; i++) {
            navigationView.getMenu().getItem(0).
                    getSubMenu().getItem(i).setTitle(
                    getResources().getStringArray(R.array.title_menu)[i]);
            toolbar.setTitle(contentTitleID);
        }
    }

    @Override
    public void onClick(View view) {
        if (playState) {
            mediaWorker.pause(this);
            playState = false;
        } else {
            mediaWorker.play(this);
            linearLayout.setVisibility(View.VISIBLE);
            playState = true;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b) {
            if (!playState) {
                try {
                    seekBar.setProgress(mediaWorker.getProgress());
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                return;
            }
            mediaWorker.jumpTo(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaWorker.stop(this);
        this.playState = false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.isCheckable() && !item.isChecked()) {
            ScrollView scrollView = findViewById(R.id.scroll_view);
            scrollView.smoothScrollTo(0, 0);
            setContent(item.getItemId());
        } else {
            switch (item.getItemId()) {
                case R.id.settings:
                    startActivity(launchSetingsActivity());
                    if (interstitialAd.isLoaded())
                        findViewById(R.id.ad_layout).setVisibility(View.VISIBLE);
                    break;
                case R.id.share:
                    startActivity(Intent.createChooser(launchShareDialog(), HomeActivity.TITLE_SHARE_DIALOG));
                    break;
                case R.id.rate_us:
                    launchRateDialog();
                    break;
                case R.id.about:
                    launchAboutDialog();
                    break;
            }
        }
        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onStateChanged(Boolean play) throws NullPointerException {
        if (play) {
            floatingActionButton.setImageResource(R.drawable.ic_pause);
            TimerTask seekbarUpdateTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        seekbar.setProgress(mediaWorker.getProgress());
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer = new Timer(true);
            timer.schedule(seekbarUpdateTask, 0, 1000);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_play);
            interruptSeekUpdateTask();
        }
    }

    private Intent launchShareDialog() {
        Intent share = new Intent();
        share.setType(HomeActivity.MIME_TEXT);
        share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.extra_sub));
        share.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.extra_text));
        return share;
    }

    private Intent launchSetingsActivity() {
        Intent settings = new Intent();
        settings.setClass(getApplicationContext(), SettingsActivity.class);
        return settings;
    }

    private void launchAboutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle(getResources().getString(R.string.title_about_dialog));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_about);
        dialog.show();
    }

    private void launchRateDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(getResources().getString(R.string.title_rate_dialog));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_rate);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final RatingBar ratingBar = dialog.findViewById(R.id.rating_bar);
        if (sharedPreferences.contains("rating")) {
            float ratings = sharedPreferences.getFloat("rating", 0);
            ratingBar.setRating(ratings);
        }
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.findViewById(R.id.btn_rate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("rating", ratingBar.getRating()).apply();
                dialog.cancel();
                startActivity(launchRateIntent());
            }
        });
        dialog.show();
    }

    private void interruptSeekUpdateTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            try {
                seekbar.setProgress(mediaWorker.getProgress());
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    private Intent launchRateIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.extra_text)));
    }

    private void initializeAds() {
        MobileAds.initialize(this, "ca-app-pub-7535962018598855~1317429603");
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId("ca-app-pub-7535962018598855/9377050314");
        interstitialAd.loadAd(new AdRequest.Builder().build());
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                interstitialAd.loadAd(new AdRequest.Builder().build());
                super.onAdClosed();
            }
        });

        final AdView mAdView = findViewById(R.id.adView);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                findViewById(R.id.ad_layout).setVisibility(View.VISIBLE);
                super.onAdLoaded();
            }
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
}

