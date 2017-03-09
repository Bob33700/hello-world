package com.bluegecko.sos.send;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.SeekBarHint;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SOSActivity extends Activity {

	public static Context mContext;
	int COUNT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);
		mContext = this;

        this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

		Intent intent = getIntent();
	    if (intent!=null){
		    int pos_top = intent.getIntExtra("TOP", 0);
		    Window window = getWindow();
		    WindowManager.LayoutParams wlp = window.getAttributes();
		    wlp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		    wlp.y = pos_top-120;
		    window.setAttributes(wlp);
	    }

	    final SeekBarHint seekBar = (SeekBarHint) findViewById(R.id.seekBar);
	    if (seekBar != null) {
		    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			    int originalProgress;
			    @Override
			    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
				    if(fromTouch){
					    // only allow changes by 1 up or down
					    if ((progress > (originalProgress+24))
							    || (progress < (originalProgress-24))) {
						    seekBar.setProgress( originalProgress);
					    } else {
						    originalProgress = progress;
					    }
				    }
			    }
			    @Override
			    public void onStartTrackingTouch(SeekBar seekBar) {
				    originalProgress=seekBar.getProgress();
			    }
					@Override
					public void onStopTrackingTouch(final SeekBar seekBar) {
						if (seekBar.getProgress() > 95) {
							SendSOS sender = new SendSOS(mContext, 0);
							sender.execute();                                       // sand SOS
							finish();
						} else {
							ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), 0);
							anim.setDuration(200);
							anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
								@Override
								public void onAnimationUpdate(ValueAnimator animation) {
									int animProgress = (Integer) animation.getAnimatedValue();
									seekBar.setProgress(animProgress);
								}
							});
							anim.start();
						}
					}
				});
	    }

	    Timer t = new Timer();
	    t.schedule(new TimerTask() {
		    @Override
		    public void run() {
				    finish();
		    }
	    }, COUNT * 1000);
	    final List<ImageView> arrows = new ArrayList<>();
	    arrows.add((ImageView) findViewById(R.id.arrow1));
	    arrows.add((ImageView) findViewById(R.id.arrow2));
	    arrows.add((ImageView) findViewById(R.id.arrow3));
	    arrows.add((ImageView) findViewById(R.id.arrow4));
	    arrows.add((ImageView) findViewById(R.id.arrow5));
	    arrows.add((ImageView) findViewById(R.id.arrow6));
	    arrows.add((ImageView) findViewById(R.id.arrow7));

	    final double offset = -1d/arrows.size();
	    for (ImageView arrow : arrows){
		    arrow.setAlpha(0f);
	    }

	    ValueAnimator animator = new ValueAnimator();
	    animator.setObjectValues(0, 100 * COUNT);
	    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		    public void onAnimationUpdate(ValueAnimator animation) {
			    if (seekBar != null) {
				    seekBar.Hint = String.valueOf(COUNT - (int)animation.getAnimatedValue()/100);
				    seekBar.invalidate();
			    }
				for (int i=0; i<arrows.size(); i++){
					double a = (((int)animation.getAnimatedValue() /100f) % 1 + offset*i);
					arrows.get(i).setAlpha((float)(a*(1-a))*2-.2f);
				}
		    }
	    });
	    animator.setInterpolator(new LinearInterpolator());
	    animator.setEvaluator(new TypeEvaluator<Integer>() {
		    @Override
		    public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
			    return Math.round((endValue - startValue) * fraction);
		    }
	    });
	    animator.setDuration(COUNT * 1000);
	    animator.start();
    }

	@Override
	public void onResume(){
		super.onResume();

	}

}

