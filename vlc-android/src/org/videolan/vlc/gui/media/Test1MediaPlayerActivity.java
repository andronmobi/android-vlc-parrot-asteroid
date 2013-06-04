package org.videolan.vlc.gui.media;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.videolan.libvlc.LibVLC;
import org.videolan.vlc.MediaServiceController;
import org.videolan.vlc.R;
import org.videolan.vlc.interfaces.IPlayerControl;
import org.videolan.vlc.interfaces.OnPlayerControlListener;
import org.videolan.vlc.widget.PlayerControlClassic;
import org.videolan.vlc.widget.PlayerControlWheel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class Test1MediaPlayerActivity extends Activity {

    private static String TAG = "VLC/MediaPlayerActivity";

    private SurfaceView mSurface;
    private SurfaceHolder mSurfaceHolder;
    private Method mSetTitleMethod;
    
    private TextView mTitle;
    private TextView mTime;
    private TextView mLength;
    private ImageButton mPlayPause;
    private ImageButton mStop;
    private ImageButton mLock;
    
    /** Overlay */
    private View mOverlayHeader;
    private View mOverlayLock;
    private View mOverlayOption;
    private View mOverlayProgress;
    private View mOverlayInterface;
    
    private MediaServiceController mAudioController;
    private List<String> mTracks = new ArrayList<String>(1);
    private boolean mIsLoaded = false;
    private boolean mShowing;
    private boolean mIsLocked = false;
    
    private IPlayerControl mControls;
    private boolean mEnableWheelbar;
    
    private int mScreenId = 0;
    
    private static final int OVERLAY_TIMEOUT = 4000;
    private static final int OVERLAY_INFINITE = 3600000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int SURFACE_SIZE = 3;
    private static final int FADE_OUT_INFO = 4;

    private String mLocation;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        
        mTitle = (TextView) findViewById(R.id.player_overlay_title);
        mTime = (TextView) findViewById(R.id.player_overlay_time);
        mLength = (TextView) findViewById(R.id.player_overlay_length);
        mPlayPause = (ImageButton) findViewById(R.id.play_pause);
        mStop = (ImageButton) findViewById(R.id.stop);

        mLock = (ImageButton) findViewById(R.id.lock_overlay_button);
        mLock.setOnClickListener(mLockListener);
        
        /** initialize Views an their Events */
        mOverlayHeader = findViewById(R.id.player_overlay_header);
        mOverlayLock = findViewById(R.id.lock_overlay);
        mOverlayOption = findViewById(R.id.option_overlay);
        mOverlayProgress = findViewById(R.id.progress_overlay);
        mOverlayInterface = findViewById(R.id.interface_overlay);
        
        mOverlayLock.setVisibility(View.VISIBLE);
        mOverlayHeader.setVisibility(View.VISIBLE);
        mOverlayOption.setVisibility(View.VISIBLE);
        mOverlayInterface.setVisibility(View.VISIBLE);
        mOverlayProgress.setVisibility(View.VISIBLE);
        
        mSurface = (SurfaceView) findViewById(R.id.player_surface);
        mSurfaceHolder = mSurface.getHolder();
        mSurfaceHolder.setFormat(ImageFormat.YV12);
        mSurfaceHolder.addCallback(mSurfaceCallback);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
     
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mEnableWheelbar = pref.getBoolean("enable_wheel_bar", false);
        
        mControls = mEnableWheelbar
                ? new PlayerControlWheel(this)
                : new PlayerControlClassic(this);
        mControls.setOnPlayerControlListener(mPlayerControlListener);
        FrameLayout mControlContainer = (FrameLayout) findViewById(R.id.player_control);
        mControlContainer.addView((View) mControls);
        
     // Use reflection to call a hidden method of SurfaceView added by Parrot.
        Class<?> params[] = new Class[1];
        params[0] = String.class;
        try {
            mSetTitleMethod = mSurface.getClass().getDeclaredMethod("setTitle", params);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.toString());
            mSetTitleMethod = null;
        }
        
//        mTracks.add("file:///mnt/sdcard/tail_toddle.mp3");
        mTracks.add("file:///mnt/sdcard/video1.3gp");
        mAudioController = MediaServiceController.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAudioController.bindMediaService(this);
//        mAudioController.addAudioPlayer(this);
    }

    public static void start(Context context, String location) {
        start(context, location, null, false, false);
    }

    public static void start(Context context, String location, Boolean fromStart) {
        start(context, location, null, false, fromStart);
    }

    public static void start(Context context, String location, String title, Boolean dontParse) {
        start(context, location, title, dontParse, false);
    }

    public static void start(Context context, String location, String title, Boolean dontParse, Boolean fromStart) {
        Intent intent = new Intent(context, MediaPlayerActivity.class);
        intent.putExtra("itemLocation", location);
        intent.putExtra("itemTitle", title);
        intent.putExtra("dontParse", dontParse);
        intent.putExtra("fromStart", fromStart);

//        if (dontParse)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
//        else {
//            // Stop the currently running audio
//            MediaServiceController asc = MediaServiceController.getInstance();
//            asc.stop();
//        }

        context.startActivity(intent);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            if (mScreenId == 2) {
                try {
                    mSetTitleMethod.invoke(mSurface, "VideoView");
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.toString());
                } catch (InvocationTargetException e) {
                    Log.e(TAG, e.toString());
                }
            }
            mAudioController.stop();
        }
//        mAudioController.removeAudioPlayer(this);
        mAudioController.unbindMediaService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    
//    public void onPlayPauseClick(View view) {
//        if (mAudioController.isPlaying()) {
//            mAudioController.pause();
//        } else {
//            mAudioController.play();
//        }
//    }
//
//    public void onStopClick(View view) {
//        mAudioController.stop();
//        finish();
//    }
    
    /**
    *
    */
   @SuppressWarnings("deprecation")
   private void load() {
       mLocation = null;
       String title = getResources().getString(R.string.title);
       boolean dontParse = false;
       boolean fromStart = false;
       String itemTitle = null;

       if (getIntent().getAction() != null
               && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
           /* Started from external application */
           if (getIntent().getData() != null
                   && getIntent().getData().getScheme() != null
                   && getIntent().getData().getScheme().equals("content")) {
               if(getIntent().getData().getHost().equals("media")) {
                   // Media URI
                   Cursor cursor = managedQuery(getIntent().getData(), new String[]{ MediaStore.Video.Media.DATA }, null, null, null);
                   int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                   if (cursor.moveToFirst())
                       mLocation = LibVLC.PathToURI(cursor.getString(column_index));
               } else {
                   // other content-based URI (probably file pickers)
                   mLocation = getIntent().getData().getPath();
               }
           } else {
               // Plain URI
               mLocation = getIntent().getDataString();
           }
       } else if(getIntent().getExtras() != null) {
           /* Started from VideoListActivity */
           mLocation = getIntent().getExtras().getString("itemLocation");
           itemTitle = getIntent().getExtras().getString("itemTitle");
           dontParse = getIntent().getExtras().getBoolean("dontParse");
           fromStart = getIntent().getExtras().getBoolean("fromStart");
       }

       Log.i(TAG, "load, Location: " + mLocation);

       //mSurface.setKeepScreenOn(true);

       mAudioController.load(mLocation, 0, false, false); // TODO use previous position

       mTitle.setText(title);
   }
   
    /**
     * attach and disattach surface to the lib
     */
    private final SurfaceHolder.Callback mSurfaceCallback = new Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged");
            mAudioController.attachSurface(holder.getSurface(), width, height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            //mAudioController.load(mTracks, 0);
            load();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            mAudioController.detachSurface();
        }
    };

    private void updateOverlayPausePlay() {
        if (mAudioController == null) {
            return;
        }

        mControls.setState(mAudioController.isPlaying());
    }

    /**
     * show overlay the the default timeout
     */
    private void showOverlay() {
        showOverlay(OVERLAY_TIMEOUT);
    }
    
    /**
     * show/hide the overlay
     */

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (mIsLocked) {
//            showOverlay();
//            return false;
//        }
//
////        DisplayMetrics screen = new DisplayMetrics();
////        getWindowManager().getDefaultDisplay().getMetrics(screen);
////
////        if (mSurfaceYDisplayRange == 0)
////            mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels);
////
////        float y_changed = event.getRawY() - mTouchY;
////        float x_changed = event.getRawX() - mTouchX;
////
////        // coef is the gradient's move to determine a neutral zone
////        float coef = Math.abs (y_changed / x_changed);
////        float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);
////
////        switch (event.getAction()) {
////
////        case MotionEvent.ACTION_DOWN:
////            // Audio
////            mTouchY = event.getRawY();
////            mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
////            mIsAudioOrBrightnessChanged = false;
////            // Seek
////            mTouchX = event.getRawX();
////            break;
////
////        case MotionEvent.ACTION_MOVE:
////            // No volume/brightness action if coef < 2
////            if (coef > 2) {
////                // Volume (Up or Down - Right side)
////                if (!mEnableBrightnessGesture || mTouchX > (screen.widthPixels / 2)){
////                    doVolumeTouch(y_changed);
////                }
////                // Brightness (Up or Down - Left side)
////                if (mEnableBrightnessGesture && mTouchX < (screen.widthPixels / 2)){
////                    doBrightnessTouch(y_changed);
////                }
////                // Extend the overlay for a little while, so that it doesn't
////                // disappear on the user if more adjustment is needed. This
////                // is because on devices with soft navigation (e.g. Galaxy
////                // Nexus), gestures can't be made without activating the UI.
////                if(Util.hasNavBar())
////                    showOverlay();
////            }
////            // Seek (Right or Left move)
////            doSeekTouch(coef, xgesturesize, false);
////            break;
////
////        case MotionEvent.ACTION_UP:
////            // Audio or Brightness
////            if (!mIsAudioOrBrightnessChanged) {
////                if (!mShowing) {
////                    showOverlay();
////                } else {
////                    hideOverlay(true);
////                }
////            }
////            // Seek
////            doSeekTouch(coef, xgesturesize, true);
////            break;
////        }
////        return mIsAudioOrBrightnessChanged;
//        return true;
//    }
    
    /**
     * Handle resize of the surface and the overlay
     */
//    private final Handler mHandler = new VideoBgPlayerHandler(this);
//
//    private static class VideoBgPlayerHandler extends WeakHandler<VideoBgPlayerActivity> {
//        public VideoBgPlayerHandler(VideoBgPlayerActivity owner) {
//            super(owner);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            VideoBgPlayerActivity activity = getOwner();
//            if(activity == null) // WeakReference could be GC'ed early
//                return;
//
//            switch (msg.what) {
//                case FADE_OUT:
//                    activity.hideOverlay(false);
//                    break;
//                case SHOW_PROGRESS:
//                    int pos = activity.setOverlayProgress();
//                    if (activity.canShowProgress()) {
//                        msg = obtainMessage(SHOW_PROGRESS);
//                        sendMessageDelayed(msg, 1000 - (pos % 1000));
//                    }
//                    break;
//                case SURFACE_SIZE:
//                    activity.changeSurfaceSize();
//                    break;
//                case FADE_OUT_INFO:
//                    activity.fadeOutInfo();
//                    break;
//            }
//        }
//    };
    
    /**
     * hider overlay
     */
    private void hideOverlay(boolean fromUser) {
        if (mShowing) {
//            mHandler.removeMessages(SHOW_PROGRESS);
            Log.i(TAG, "remove View!");
            if (!fromUser && !mIsLocked) {
                mOverlayLock.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mOverlayHeader.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mOverlayOption.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mOverlayInterface.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            }
            mOverlayLock.setVisibility(View.INVISIBLE);
            mOverlayHeader.setVisibility(View.INVISIBLE);
            mOverlayOption.setVisibility(View.INVISIBLE);
            mOverlayProgress.setVisibility(View.INVISIBLE);
            mOverlayInterface.setVisibility(View.INVISIBLE);
            mShowing = false;
//            dimStatusBar(true);
        }
    }
    
    /**
     * Lock screen rotation
     */
    private void lockScreen() {
//        if(mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
//            setRequestedOrientation(getScreenOrientation());
//        showInfo(R.string.locked, 1000);
        mLock.setBackgroundResource(R.drawable.ic_lock_glow);
        mTime.setEnabled(false);
//        mSeekbar.setEnabled(false);
        mLength.setEnabled(false);
        hideOverlay(true);
    }

    /**
     * Remove screen lock
     */
    private void unlockScreen() {
//        if(mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//        showInfo(R.string.unlocked, 1000);
        mLock.setBackgroundResource(R.drawable.ic_lock);
        mTime.setEnabled(true);
//        mSeekbar.setEnabled(true);
        mLength.setEnabled(true);
        mShowing = false;
        showOverlay();
    }
    
    /**
    *
    */
   private final OnClickListener mLockListener = new OnClickListener() {

       @Override
       public void onClick(View v) {
           if (mIsLocked) {
               mIsLocked = false;
               unlockScreen();
           } else {
               mIsLocked = true;
               lockScreen();
           }
       }
   };
   
    /**
     * show overlay
     */
    private void showOverlay(int timeout) {
//        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (!mShowing) {
            mShowing = true;
            mOverlayLock.setVisibility(View.VISIBLE);
            if (!mIsLocked) {
                mOverlayHeader.setVisibility(View.VISIBLE);
                mOverlayOption.setVisibility(View.VISIBLE);
                mOverlayInterface.setVisibility(View.VISIBLE);
//                dimStatusBar(false);
            }
            mOverlayProgress.setVisibility(View.VISIBLE);
        }
//        Message msg = mHandler.obtainMessage(FADE_OUT);
//        if (timeout != 0) {
//            mHandler.removeMessages(FADE_OUT);
//            mHandler.sendMessageDelayed(msg, timeout);
//        }
        updateOverlayPausePlay();
    }
    
    /**
     *
     */
    private void play() {
//        mLibVLC.play();
        mAudioController.play();
        mSurface.setKeepScreenOn(true);
    }

    /**
     *
     */
    private void pause() {
//        mLibVLC.pause();
        mAudioController.pause();
        mSurface.setKeepScreenOn(false);
    }

    /**
     *
     */
    private final OnPlayerControlListener mPlayerControlListener = new OnPlayerControlListener() {

        @Override
        public void onPlayPause() {
            if (mAudioController.isPlaying())
                pause();
            else
                play();
//            showOverlay();
        }

        @Override
        public void onSeek(int delta) {
            // unseekable stream
//            if(mLibVLC.getLength() <= 0) return;
//
//            long position = mLibVLC.getTime() + delta;
//            if (position < 0) position = 0;
//            mLibVLC.setTime(position);
            showOverlay();
        }

        @Override
        public void onSeekTo(long position) {
            // unseekable stream
//            if(mLibVLC.getLength() <= 0) return;
//            mLibVLC.setTime(position);
//            mTime.setText(Util.millisToString(position));
        }

        @Override
        public long onWheelStart() {
            showOverlay(OVERLAY_INFINITE);
//            return mLibVLC.getTime();
            return 0;
        }

        @Override
        public void onShowInfo(String info) {
//            if (info != null)
//                showInfo(info);
//            else {
//                hideInfo();
//                showOverlay();
//            }
        }

        @Override
        public int onScreen() {
            mScreenId = (mScreenId == 0) ? 2 : 0;
            try {
                // If a window of SurfaceView has a title VoutVideoView
                // than the surface wont be destroyed by WindowManagerService
                // while video is playing on external screen in background mode.
                mSetTitleMethod.invoke(mSurface, mScreenId == 0 ? "VideoView" : "VoutVideoView");
            } catch (IllegalAccessException e) {
                Log.e(TAG, e.toString());
            } catch (InvocationTargetException e) {
                Log.e(TAG, e.toString());
            }
            mAudioController.setScreenId(mScreenId);
            return mScreenId;
        }
    };
}
