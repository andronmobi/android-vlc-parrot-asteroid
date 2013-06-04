package org.videolan.vlc.gui.video;

import org.videolan.vlc.gui.media.MediaPlayerActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;

public class VideoLauncherActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button btn = new Button(this);
        btn.setText("Play");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VideoLauncherActivity.this, MediaPlayerActivity.class);
                VideoLauncherActivity.this.startActivity(intent);
            }
        });
        this.addContentView(btn, new LayoutParams(120 , 80));
    }

}
