package com.divyansh.musicalex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView songListView;

    TextView currentSong;

    Button btnPlay, btnPause, btnNext, btnPrev;

    EditText searchBar;

    ImageView albumArt;

    RotateAnimation rotateAnimation;

    ArrayList<String> songNames = new ArrayList<>();

    ArrayList<Uri> songUris = new ArrayList<>();

    ArrayAdapter<String> adapter;

    MediaPlayer mediaPlayer;

    int currentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // Connect Views
        songListView = findViewById(R.id.songListView);

        currentSong = findViewById(R.id.currentSong);

        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);

        albumArt = findViewById(R.id.albumArt);

        searchBar = findViewById(R.id.searchBar);

        // Adapter
        adapter = new ArrayAdapter<>(
                this,
                R.layout.song_item,
                songNames
        );

        songListView.setAdapter(adapter);

        // Search Songs
        searchBar.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Favorite Song
        songListView.setOnItemLongClickListener((parent, view, position, id) -> {

            Toast.makeText(
                    MainActivity.this,
                    songNames.get(position) + " added to Favorites ❤️",
                    Toast.LENGTH_SHORT
            ).show();

            return true;
        });

        // Song Click
        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                currentIndex = position;

                playSong(position);
            }
        });

        // Play Button
        btnPlay.setOnClickListener(v -> {

            animateButton(v);

            if (mediaPlayer != null) {

                mediaPlayer.start();
            }
        });

        // Pause Button
        btnPause.setOnClickListener(v -> {

            animateButton(v);

            if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                mediaPlayer.pause();
            }
        });

        // Next Button
        btnNext.setOnClickListener(v -> {

            animateButton(v);

            if (currentIndex < songUris.size() - 1) {

                currentIndex++;

                playSong(currentIndex);
            }
        });

        // Previous Button
        btnPrev.setOnClickListener(v -> {

            animateButton(v);

            if (currentIndex > 0) {

                currentIndex--;

                playSong(currentIndex);
            }
        });

        // Permission Check
        checkPermissionAndLoadSongs();
    }

    // Check Permission
    private void checkPermissionAndLoadSongs() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        1
                );

            } else {

                loadSongs();
            }

        } else {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1
                );

            } else {

                loadSongs();
            }
        }
    }

    // Load Songs
    private void loadSongs() {

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = getContentResolver().query(
                uri,
                null,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        );

        if (cursor != null) {

            while (cursor.moveToNext()) {

                String songName = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.TITLE
                        )
                );

                long songId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media._ID
                        )
                );

                Uri songUri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(songId)
                );

                songNames.add(songName);

                songUris.add(songUri);
            }

            cursor.close();

            adapter.notifyDataSetChanged();
        }
    }

    // Play Song
    private void playSong(int position) {

        if (mediaPlayer != null) {

            mediaPlayer.stop();

            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();

        try {

            mediaPlayer.setDataSource(
                    MainActivity.this,
                    songUris.get(position)
            );

            mediaPlayer.prepare();

            mediaPlayer.start();

            currentSong.setText(songNames.get(position));

            // Rotate Album Art
            rotateAnimation = new RotateAnimation(
                    0,
                    360,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            );

            rotateAnimation.setDuration(8000);

            rotateAnimation.setRepeatCount(Animation.INFINITE);

            rotateAnimation.setInterpolator(new LinearInterpolator());

            albumArt.startAnimation(rotateAnimation);

            // Auto Next Song
            mediaPlayer.setOnCompletionListener(mp -> {

                if (currentIndex < songUris.size() - 1) {

                    currentIndex++;

                    playSong(currentIndex);
                }
            });

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Unable to play this song",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Button Animation
    private void animateButton(View v) {

        v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100));
    }

    // Permission Result
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == 1
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            loadSongs();
        }
    }

    // Release MediaPlayer
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {

            mediaPlayer.release();
        }
    }
}