package com.doctor;

public class Data {
	//ע��java �е�byte ���з��ŵ�
	public final int STATUS_TESTING = 1;//���ڲ�����
	public final int STATUS_FINISHED = 2;//�������
	public int getStatus(byte[] buffer){
		return buffer[1];//�ӵ�һ���ֽڿ�ʼ
	}
	public int getHeartRate(byte[] buffer){//�õ�����,�����ֽ�
		return buffer[2]<<8 + buffer[3];
	}
	public int getPresureShou(byte[] buffer){//�õ�����ѹ
		return buffer[4]<<8+buffer[5];
	}
	public int getPresureShu(byte[] buffer){//�õ�����ѹ
		return buffer[6]<<8+buffer[7];
	}
}
