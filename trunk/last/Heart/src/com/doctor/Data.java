package com.doctor;

public class Data {
	//注意java 中的byte 是有符号的
	public final int STATUS_TESTING = 1;//正在测量中
	public final int STATUS_FINISHED = 2;//测量完毕
	public int getStatus(byte[] buffer){
		return buffer[1];//从第一个字节开始
	}
	public int getHeartRate(byte[] buffer){//得到心率,两个字节
		return buffer[2]<<8 + buffer[3];
	}
	public int getPresureShou(byte[] buffer){//得到舒张压
		return buffer[4]<<8+buffer[5];
	}
	public int getPresureShu(byte[] buffer){//得到收缩压
		return buffer[6]<<8+buffer[7];
	}
}
