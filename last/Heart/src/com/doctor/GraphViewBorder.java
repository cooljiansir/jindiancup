package com.doctor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.os.Handler;
import android.os.Message;
import android.view.View;

public class GraphViewBorder extends View{

	private int screenWidth;
	public GraphViewBorder(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		screenWidth = ((MainActivity)context)
		.getWindowManager().getDefaultDisplay().getWidth();
	}

	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.draw(canvas);
//		canvas.drawColor(Color.BLACK);
//		canvas.drawColor(getResources().getColor(R.color.background));
		Paint paint = new Paint();
		//paint.setColor(Color.argb(255,23,42,221));
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		PathEffect effects = new DashPathEffect(new float[] { 5, 5, 5, 5 }, 1);
        paint.setPathEffect(effects);
		canvas.drawLine(Const.MARGIN_LEFT,0,
				Const.MARGIN_LEFT,Const.GRAPH_HEIGHT, paint);
		Paint painttext = new Paint();
		painttext.setColor(Color.argb(255,23,42,221));
		int thelast = 0;
		for(int i = 0;(i*Const.DERTAY)<Const.GRAPH_HEIGHT;i++){
			//画刻度数字
			canvas.drawText(String.valueOf(i*Const.DERTAY),
					Const.MARGIN_LEFT + Const.MARGIN_TEXT_LEFT,
					Const.GRAPH_HEIGHT - i*Const.DERTAY - Const.MARGIN_TEXT_BOTTOM,
					painttext);
			//画刻度虚线
			canvas.drawLine(Const.MARGIN_LEFT,
					Const.GRAPH_HEIGHT - i*Const.DERTAY,
					screenWidth,Const.GRAPH_HEIGHT - i*Const.DERTAY, paint);
			thelast = i;
		}
		thelast++;
		Paint paint2 = new Paint();
		paint2.setColor(Const.COLOR_HEART);
		canvas.drawLine(Const.MARGIN_LEFT + 50
				,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,Const.MARGIN_LEFT+70,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,paint2);
		canvas.drawText("心率",Const.MARGIN_LEFT+70
				,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,paint2);
		
		paint2.setColor(Const.COLOR_PRESSSHOU);
		canvas.drawLine(Const.MARGIN_LEFT + 100
				,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,Const.MARGIN_LEFT+120,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,paint2);
		canvas.drawText("收缩压",Const.MARGIN_LEFT+120
				,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,paint2);
		
		paint2.setColor(Const.COLOR_PRESSSHU);
		canvas.drawLine(Const.MARGIN_LEFT +160
				,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,Const.MARGIN_LEFT+180,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,paint2);
		canvas.drawText("舒张压",Const.MARGIN_LEFT+180
				,Const.GRAPH_HEIGHT - thelast*Const.DERTAY+Const.DERTAY/2
				,paint2);
	}

}
