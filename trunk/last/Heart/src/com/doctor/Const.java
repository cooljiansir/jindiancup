package com.doctor;

import android.graphics.Color;

public class Const {
	public static final int MARGIN_LEFT = 10;
	public static final int DERTA = 40;//x方向上的间距
	public static final int DERTAY = 20;//y方向上的间距
	public static final int GRAPH_HEIGHT = 300;
	public static final int GRAPH_HEIGHT_BORDER = 30;
	public static final int MARGIN_TEXT_LEFT = 3;//y坐标上的刻度数字的左边距
	public static final int MARGIN_TEXT_BOTTOM = 3;//y坐标上的刻度数字的下边距
	public static final int COLOR_HEART = Color.RED;
	public static final int COLOR_PRESSSHOU = Color.BLACK;
	public static final int COLOR_PRESSSHU = Color.YELLOW;
	public static final int PRESSSHU[] = {0,80,85,90,100,110};//理想、正常、正常高压、轻度高压、中度高压、重度高压
	public static final int PRESSSHOU[] = {0,120,130,140,160,180};
	public static final String TIPSTR[] = {
		"您的测试结果水平为--理想血压",
		"您的测试结果水平为--正常血压",
		"您的测试结果水平为--正常高血压",
		"您的测试结果水平为--轻度高血压",
		"您的测试结果水平为--中度高血压",
		"您的测试结果水平为--重度高血压"
	};
}
