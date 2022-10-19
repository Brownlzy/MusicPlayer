package com.liux.musicplayer.ui;

import com.liux.musicplayer.R;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class UserGuide extends AppCompatActivity {

    private int pageId;
    private Button pageUp;
    private Button pageDown;
    private Button over;

    private ImageView imageGuide;
    private int[] imageId = new int[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);
        initView();
    }

    private void initView() {
        imageId[0] = R.drawable.ic_baseline_music_note_24;
        imageId[1] = R.drawable.ic_baseline_create_24;
        imageId[2] = R.drawable.ic_baseline_done_all_24;
        pageId = 0;
        imageGuide =findViewById(R.id.ImageGuide);
        pageUp = findViewById(R.id.PageUp);
        pageDown = findViewById(R.id.PageDown);
        over = findViewById(R.id.PageOver);


        pageUp.setEnabled(false);
        imageGuide.setImageResource(imageId[0]);
        pageUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageId--;
                imageGuide.setImageResource(imageId[pageId]);
                if (pageId == 0)
                    pageUp.setEnabled(false);
                if(pageId!=2)
                    pageDown.setEnabled(true);
            }
        });
        pageDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageId++;
                imageGuide.setImageResource(imageId[pageId]);
                if (pageId == 2)
                    pageDown.setEnabled(false);
                if(pageId!=0)
                    pageUp.setEnabled(true);
            }
        });
        over.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}