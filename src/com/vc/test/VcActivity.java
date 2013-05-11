package com.vc.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ffcut.FFCut;

public class VcActivity extends Activity implements View.OnClickListener {	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        FFCut.init(this);
       
        
        ((Button)findViewById(R.id.runbtn)).setOnClickListener(this);
    }
    
    public void onClick(View v) {
        String srcVideo = ((EditText)findViewById(R.id.editText1)).getText().toString();
        String srcAudio = ((EditText)findViewById(R.id.editText2)).getText().toString();
        if (srcAudio.length() == 0)
        	srcAudio = null;
        String destPath =  ((EditText)findViewById(R.id.editText3)).getText().toString();
        double start = Double.parseDouble(((EditText)findViewById(R.id.editText4)).getText().toString());
        double end = Double.parseDouble(((EditText)findViewById(R.id.editText5)).getText().toString());
        
        
        FFCut.process(VcActivity.this, srcVideo, srcAudio, destPath, start, end, new FFCut.Listener() {
			
			public void onProgress(double progress) {
				Log.d("VcActivity", "progress: " + progress);
				((TextView)findViewById(R.id.textView6)).setText("progress: " + progress);
			}
			
			public void onFinish() {
				Log.d("VcActivity", "finished");
				((TextView)findViewById(R.id.textView6)).setText("finished");
			}
			
			public void onFail() {
				Log.d("VcActivity", "failed");
				((TextView)findViewById(R.id.textView6)).setText("failed");
			}
        });

    }

}
