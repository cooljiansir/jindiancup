package com.doctor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import com.doctor.R.color;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final int DATACOMES = 1;//蓝牙设备数据传来
    /** Called when the activity is first created. */
	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();
	private BroadcastReceiver _foundReceiver = null;
	private BroadcastReceiver _finishedReceiver = null;
	private boolean foundDevice = false;
	
	//private BluetoothDevice device = null;
	private BluetoothSocket socket = null;
	private OutputStream outputStream;
	private InputStream inputStream; 
	
	private boolean connected = false;
	private TextView connectTipView;
	private Button connectBut;
	
	
	private LinearLayout connectLayout;
	private FrameLayout graphFrameLayout;
	private GraphView graphView;
	private GraphViewBorder graphViewBorder;
	private SharedPreferences spDateMonth;
	private SharedPreferences spDateDay;
	private SharedPreferences spDateHour;
	private SharedPreferences spDateMinute;
	private SharedPreferences spDateNum;
	private SharedPreferences spValueH;//收缩压
	private SharedPreferences spValueL;//舒张压
	private SharedPreferences spValueR;//心率
	private byte[] databuf;
	private int datalength = 0;
	private ListenRunnable listenRunnable = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.connectTipView = (TextView)findViewById(R.id.connectTipView);
        this.connectBut = (Button)findViewById(R.id.connecBut);
        
        this.connectLayout  = (LinearLayout)findViewById(R.id.connectLayout);
        
        this.databuf = new byte[1024];
		spDateMonth = getSharedPreferences("spDateMonth",Context.MODE_PRIVATE);
		spDateDay = getSharedPreferences("spDateDay",Context.MODE_PRIVATE);
		spDateHour = getSharedPreferences("spDateHour",Context.MODE_PRIVATE);
		spDateMinute = getSharedPreferences("spDateMinute",Context.MODE_PRIVATE);
		spDateNum = getSharedPreferences("spDateNum",Context.MODE_PRIVATE);
		spValueH = this.getSharedPreferences("spValueH",Context.MODE_PRIVATE);
		spValueL = this.getSharedPreferences("spValueL",Context.MODE_PRIVATE);
		spValueR = this.getSharedPreferences("spValueR",Context.MODE_PRIVATE);
		
        this.graphFrameLayout = (FrameLayout)findViewById(R.id.graphFrameLayout);
        this.graphView = new GraphView(this);
        //this.graphView.setBackgroundColor(getResources().getColor(R.color.background));
        this.graphViewBorder = new GraphViewBorder(this);
        //this.graphViewBorder.setBackgroundColor(getResources().getColor(R.color.background));
        this.graphFrameLayout.addView(graphViewBorder,
        		this.getWindowManager().getDefaultDisplay().getWidth(),Const.GRAPH_HEIGHT + Const.GRAPH_HEIGHT_BORDER);
        this.graphFrameLayout.addView(graphView,100000,Const.GRAPH_HEIGHT + Const.GRAPH_HEIGHT_BORDER);
        
        connectTipView.setText("尚未连接~");
        this._foundReceiver = new BroadcastReceiver(){
			public void onReceive(Context cxv,Intent intent){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				if(device.getName().equals(getResources().getString(R.string.BluetoothName))){
					connected = true;
					connectTipView.setText("连接成功!");
					connectBut.setText("已连接");
					connectBut.setClickable(false);
					connectTipView.setText("连接成功! 正在测量~");
					_bluetooth.cancelDiscovery();
					try {
						socket = device.createRfcommSocketToServiceRecord(UUID
								.fromString("00001101-0000-1000-8000-00805F9B34FB"));
						socket.connect();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Thread answerThread = new Thread(new WriteRunnable());
					answerThread.start();
					listenRunnable  = new ListenRunnable();
					Thread listenThread = new Thread(listenRunnable);
					listenThread.start();
				}
				
			}
		};
		_finishedReceiver = new BroadcastReceiver(){
			public void onReceive(Context cxt,Intent intent){
				if(!connected)connectTipView.setText("连接失败！");
			}
		};
		IntentFilter foundFilter = new IntentFilter(
				BluetoothDevice.ACTION_FOUND);
		IntentFilter finishedFilter = new IntentFilter(
				BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(_foundReceiver,foundFilter);
		this.registerReceiver (_finishedReceiver,finishedFilter);
		
        connectBut.setOnClickListener(new OnClickListener(){
        	public void onClick(View view){
        		connectTipView.setText("正在连接中，请稍后。。。。。。");
        		connect();
        	}
        });
//        for(int i  =0;i<10;i++)writeData(i*1234578%100,i*4324234%220,i*43252543%90);
//        Message msg = new Message();
//        msg.what = this.DATACOMES;
//        this.mHandler.sendMessage(msg);
    }
	private void writeData(int heart,int pressShou,int pressShu){
		SharedPreferences.Editor spDateNumEditor = spDateNum.edit();
		SharedPreferences.Editor spDateMonthEditor = spDateMonth.edit();
		SharedPreferences.Editor spDateDayEditor = spDateDay.edit();
		SharedPreferences.Editor spDateHourEditor = spDateHour.edit();
		SharedPreferences.Editor spDateMinuteEditor = spDateMinute.edit();
		SharedPreferences.Editor spValueHEditor = spValueH.edit();
		SharedPreferences.Editor spValueLEditor = spValueL.edit();
		SharedPreferences.Editor spValueREditor = spValueR.edit();
		Calendar calendar = Calendar.getInstance();
		int i = spDateNum.getInt("length",0);
		int month = calendar.get(Calendar.MONTH) + 1;//注意加上1
		int day = calendar.get(Calendar.DATE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		spDateMonthEditor.putInt(String.valueOf(i),month);
		spDateDayEditor.putInt(String.valueOf(i),day);
		spDateHourEditor.putInt(String.valueOf(i),hour);
		spDateMinuteEditor.putInt(String.valueOf(i),minute);
		
		spValueHEditor.putInt(String.valueOf(i),pressShou);
		spValueLEditor.putInt(String.valueOf(i),pressShu);
		spValueREditor.putInt(String.valueOf(i),heart);
		//记得处理非法数据凡是在（0-280）以外的全部归0
		spDateNumEditor.putInt("length",i+1);
		spDateNumEditor.commit();
		spDateMonthEditor.commit();
		spDateDayEditor.commit();
		spDateHourEditor.commit();
		spDateMinuteEditor.commit();
		spValueHEditor.commit();
		spValueLEditor.commit();
		spValueREditor.commit();	
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		this.unregisterReceiver(this._finishedReceiver);
		this.unregisterReceiver(this._foundReceiver);
		if(socket != null){
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(this.listenRunnable!=null)listenRunnable.stop();
		super.onDestroy();
	}
	public void connect(){
		if(!_bluetooth.isEnabled()){
			connectTipView.setText("请打开蓝牙设备");
			return ;
		}
		Thread thread = new Thread(new Runnable(){
			public void run(){
				_bluetooth.startDiscovery();
			}
		});
		thread.start();
	}

	private class WriteRunnable implements Runnable{
		public void run() {
			// TODO Auto-generated method stub
			try {
				OutputStream output = socket.getOutputStream();
				byte[] buffer = new byte[1];
				buffer[0] = 1;
				output.write(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	private class ListenRunnable implements Runnable{
		private boolean running = true;
		public void stop(){
			running  = false;
		}
		public void run() {
			// TODO Auto-generated method stub
			int bytes;
			byte[] buffer = new byte[1024];
			InputStream inStream = null;
			try {
				inStream = socket.getInputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			//Toast.makeText(MainActivity.this,"开始监听",Toast.LENGTH_SHORT).show();
			while(running){
				try {
					if((bytes = inStream.read(buffer))>0){
				//		Toast.makeText(MainActivity.this,"收到消息",Toast.LENGTH_SHORT).show();
						for(int i = 0;i<bytes;i++){
							databuf[datalength++] = buffer[i];
						}
						if(datalength==6){
							Message msg = new Message();
							msg.what = DATACOMES;
							mHandler.sendMessage(msg);	
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what){
			case DATACOMES:
				connectTipView.setText("测试已完成~");
				int heart = databuf[0]*128 + databuf[1];
				int pressShou = databuf[2]*128 + databuf[3];
				int pressShu = databuf[4]*128 + databuf[5];
				writeData(heart,pressShou,pressShu);
				Bundle bundle = new Bundle();
				bundle.putInt("heart",heart);
				bundle.putInt("pressShou",pressShou);
				bundle.putInt("pressShu",pressShu);
				Intent intent = new Intent(MainActivity.this,ResultActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			}
			super.handleMessage(msg);
		}		
	};
	
}