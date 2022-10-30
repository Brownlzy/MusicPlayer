package com.liux.musicplayer.ui;

import com.liux.musicplayer.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
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
    private int[] imageId = new int[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);
        initView();
    }
/**
 * 初始化引导页
 * @param
 * @return void
 */
    private void initView() {
        imageId[0] = R.mipmap.guide_1;
        imageId[1] = R.mipmap.guide_2;
        imageId[2] = R.mipmap.guide_3;
        imageId[3] = R.mipmap.guide_4;
        imageId[4] = R.mipmap.guide_6;
        imageId[5] = R.mipmap.guide_5;
        pageId = 0;
        imageGuide =findViewById(R.id.ImageGuide);
        pageUp = findViewById(R.id.PageUp);
        pageDown = findViewById(R.id.PageDown);
        over = findViewById(R.id.PageOver);
        over.setVisibility(View.GONE);


        pageUp.setEnabled(false);
        imageGuide.setImageResource(imageId[0]);
        pageUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageId--;
                imageGuide.setImageResource(imageId[pageId]);
                if (pageId == 0)
                    pageUp.setEnabled(false);
                if(pageId!=5)
                    pageDown.setEnabled(true);
            }
        });
        pageDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageId++;
                if(pageId==4) over.setVisibility(View.VISIBLE);
                imageGuide.setImageResource(imageId[pageId]);
                if (pageId == 5)
                    pageDown.setEnabled(false);
                if(pageId!=0)
                    pageUp.setEnabled(true);
            }
        });
        over.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(UserGuide.this);
                sp.edit().putBoolean("isFirstStart",false).apply();
                finish();
            }
        });
    }
}