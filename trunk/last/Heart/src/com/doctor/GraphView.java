package com.doctor;

import java.util.Calendar;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class GraphView extends View {
	private Bitmap backGroundBitmap;
	private float nowX;
	private float beforeX; 
	private float touchxstill = -1;
	private float touchx;
	private SharedPreferences spDateMonth;
	private SharedPreferences spDateDay;
	private SharedPreferences spDateHour;
	private SharedPreferences spDateMinute;
	private SharedPreferences spDateNum;
	private SharedPreferences spValueH;//收缩压
	private SharedPreferences spValueL;//舒张压
	private SharedPreferences spValueR;//心率
	private int dataNum;
	private int screenWidth;
	private Context context;
	private boolean moving = false;
	private MoveHandler moveHandler;
	private float speedX;
	private int flagX;//运动速度的方向
	private Thread moveThread;
	private boolean isDown;//是否屏幕被按下
	public GraphView(Context context) {
		super(context);
		this.context = context;
		// TODO Auto-generated constructor stub
		backGroundBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.village);
		spDateMonth = context.getSharedPreferences("spDateMonth",Context.MODE_PRIVATE);
		spDateDay = context.getSharedPreferences("spDateDay",Context.MODE_PRIVATE);
		spDateHour = context.getSharedPreferences("spDateHour",Context.MODE_PRIVATE);
		spDateMinute = context.getSharedPreferences("spDateMinute",Context.MODE_PRIVATE);
		spDateNum = context.getSharedPreferences("spDateNum",Context.MODE_PRIVATE);
		spValueH = context.getSharedPreferences("spValueH",Context.MODE_PRIVATE);
		spValueL = context.getSharedPreferences("spValueL",Context.MODE_PRIVATE);
		spValueR = context.getSharedPreferences("spValueR",Context.MODE_PRIVATE);
		dataNum = spDateNum.getInt("length",0);
		screenWidth = ((MainActivity)context).getWindowManager().getDefaultDisplay().getWidth();
		if(dataNum*Const.DERTA+Const.MARGIN_LEFT>=screenWidth){
			beforeX = -1*Const.DERTA*dataNum+screenWidth;
		}
		else {
			beforeX = Const.MARGIN_LEFT;
		}
		nowX = beforeX; 
		moveHandler = new MoveHandler();
		moving = true;
		isDown = false;
		
		moveThread = new Thread(new MoveRunnable());
		moveThread.start();
	}
	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.draw(canvas);
		canvas.translate(Const.MARGIN_LEFT,0);
		//canvas.drawBitmap(backGroundBitmap,0,0,null);
		if(screenWidth-dataNum*Const.DERTA<0)canvas.translate(nowX,0);
		else canvas.translate(Const.MARGIN_LEFT*2,0);
		drawChange(0,spValueH,canvas);
		drawChange(1,spValueL,canvas);
		drawChange(2,spValueR,canvas);
	}
	private void drawChange(int arg,SharedPreferences sp,Canvas canvas){
		Paint paint = new Paint();
		//画点
		int centerxPre = 0;
		int centeryPre = 0;
		for(int i = 0;i<dataNum;i++){
			//画收缩压
//			if(arg==0)paint.setColor(Color.BLUE);
//			else if(arg==1)paint.setColor(Color.GREEN);
//			else if(arg==2)paint.setColor(Color.CYAN);
			if(arg==0)paint.setColor(Const.COLOR_PRESSSHOU);
			else if(arg==1)paint.setColor(Const.COLOR_PRESSSHU);
			else if(arg==2)paint.setColor(Const.COLOR_HEART);
			int centerx = i*Const.DERTA;
			int centery = sp.getInt(String.valueOf(i),-1);
			if(centery==-1)break;
			centery = Const.GRAPH_HEIGHT - centery;
			canvas.drawRect(centerx-2,centery-2,centerx+2,centery+2, paint);
			if(arg==0){
				//日期
				paint.setColor(Color.BLACK);
				canvas.drawText(spDateMonth.getInt(String.valueOf(i),0) + "."+ spDateDay.getInt(String.valueOf(i),0),centerx-15,Const.GRAPH_HEIGHT+Const.GRAPH_HEIGHT_BORDER-15,paint);
				canvas.drawText(spDateHour.getInt(String.valueOf(i),0) + ":"+spDateMinute.getInt(String.valueOf(i),0),centerx-15,Const.GRAPH_HEIGHT+Const.GRAPH_HEIGHT_BORDER,paint);
			}
			if(arg==0)paint.setColor(Const.COLOR_PRESSSHOU);
			else if(arg==1)paint.setColor(Const.COLOR_PRESSSHU);
			else if(arg==2)paint.setColor(Const.COLOR_HEART);
			//文字标记
			String txt = String.valueOf(Const.GRAPH_HEIGHT - centery);
			if(arg==0)canvas.drawText(txt,centerx+5, centery - 10, paint);
			if(arg==1)canvas.drawText(txt,centerx-10, centery - 10, paint);
			if(arg==2)canvas.drawText(txt,centerx-25, centery - 10, paint);
			//画折线
			
			if(i>0){
				canvas.drawLine(centerxPre,centeryPre,centerx,centery, paint);
			}
			centerxPre = centerx;
			centeryPre = centery;
		}
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if(screenWidth-dataNum*Const.DERTA>0)return true;//长度没有超过
		touchx = event.getX();
		speedX = 0;
		//moving  = false;
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			isDown = true;
			touchxstill = touchx;
			return true;
		case MotionEvent.ACTION_MOVE:
			if(touchxstill!=-1){
				nowX = beforeX + touchx - touchxstill;
				if(nowX>Const.MARGIN_LEFT)nowX = Const.MARGIN_LEFT;
				if(nowX<-1*Const.DERTA*dataNum+screenWidth)
					nowX = -1*Const.DERTA*dataNum+screenWidth; 
				this.invalidate();
			}
			return true;
		case MotionEvent.ACTION_UP:
			speedX = nowX - beforeX;
			if(speedX>0)flagX = 1;
			else flagX = -1;
			beforeX = nowX;
			moving = true;
			//if(!moveThread.isAlive())moveThread.start();
			isDown  = false;
			return true;
		}
		return super.onTouchEvent(event);
	}
	private class MoveHandler extends Handler{

		@Override
		public void handleMessage(Message msg1) {
			// TODO Auto-generated method stub
			super.handleMessage(msg1);
			speedX -= 10*flagX;
			if(speedX*flagX<0)speedX = 0;
			nowX += speedX/3;
			if(nowX>Const.MARGIN_LEFT){
				nowX = Const.MARGIN_LEFT;
				speedX = 0;
			}
			if(nowX<-1*Const.DERTA*dataNum+screenWidth)
			{
				nowX = -1*Const.DERTA*dataNum+screenWidth;
				speedX = 0;
			}
			beforeX = nowX;
			invalidate();
		}
	}
	private class MoveRunnable implements Runnable{
//		Message msg;
//		public MoveRunnable(){
//			msg = new Message();
//		}
		public void run() {
			// TODO Auto-generated method stub
			while(moving){
				if(isDown)continue;
				Message msg = new Message();
				moveHandler.sendMessage(msg);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
}
