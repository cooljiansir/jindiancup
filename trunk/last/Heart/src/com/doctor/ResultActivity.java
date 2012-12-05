package com.doctor;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ResultActivity extends Activity {
	private Button sendMesBut;
	private int heart;
	private int pressShou;
	private TextView heartView;
	private TextView pressShouView;
	private TextView pressShuView;
	private int pressShu;
	private TextView tipView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.reusltlayout);
		this.sendMesBut = (Button)findViewById(R.id.sendBut);
		this.heartView = (TextView)findViewById(R.id.heartRateView);
		this.pressShouView = (TextView)findViewById(R.id.ShouPressView);
		this.pressShuView = (TextView)findViewById(R.id.ShuPressView);
		this.tipView = (TextView)findViewById(R.id.resultTipView);
		Bundle bundle = this.getIntent().getExtras();
		this.heart = bundle.getInt("heart");
		this.pressShou = bundle.getInt("pressShou");
		this.pressShu = bundle.getInt("pressShu");
		this.heartView.setText("����--"+heart+"��/ÿ����");
		this.pressShouView.setText("����ѹ--"+pressShou+"mmhg");
		this.pressShuView.setText("����ѹ--"+pressShu+"mmhg");
		sendMesBut.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				onSendMesBut();
			}
        });
		int level = 5;//������ѹ������ѹ�����и��ߵ���һ������Ϊ���
		for(int i = 5;i>=0;i--){
			if(this.pressShou>=Const.PRESSSHOU[i]){
				level = i;
				break;
			}
		}
		for(int i = 5;i>=0;i--){
			if(this.pressShu>=Const.PRESSSHU[i]){
				if(level<i)level = i;
				break;
			}
		}
		this.tipView.setText(Const.TIPSTR[level]);
	}
	public void onSendMesBut(){
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;//ע�����1
		int day = calendar.get(Calendar.DATE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		String mes = "����"+year+"��"+month+"��"+day+"��"+hour+":"+minute
			+"��õĽ��Ϊ������--"+heart
			+"��/����,����ѹ--"+pressShou
			+"mmhg,����ѹ--"+pressShu+"mmhg��";
		Uri uri = Uri.parse("smsto:");
		Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
		intent.putExtra("sms_body",mes);
		startActivity(intent);
	}

}
