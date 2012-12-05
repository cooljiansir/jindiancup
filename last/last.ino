/*
LCD  Arduino
 RS = 17; Analog Pin3
 RW = 16; Analog Pin2
 EN = 18; Analog Pin4
 D0  = 8; 
 D1  = 9;
 D2  = 10; 
 D3  = 11; 
 D4  = 4;
 D5  = 5; 
 D6  = 6; 
 D7  = 7;
 PIN15 PSB = 5V;
 */

#include "LCD12864R.h"
#include <MsTimer2.h>

#define DEBUG
#define DEBUG2
#define AR_SIZE( a ) sizeof( a ) / sizeof( a[0] )
#define LVBOC 12
#define LVBOC2 3
#define DC 512 //
#define RATE 0.1
#define SCANT 5
#define LEDTIME 1
#define DIS 0
#define MAXRATE 350//the max frequency of people's heart rate's time
#define JIAOZHENG 115 //每117个单位为100mmHG
#define DCMAXD 2//直流分量限幅
#define ACMAXD 8//交流分量限幅
#define STOPBUMP 200//判断放气的静压起始点
#define STOPBUMPDERTA 65
#define MAXBUFFER 80//缓冲区长度 
#define JUDGE 1//出现几次小幅度就停止充气（避免偶然）
#define SHOURATE 0.55 //计算收缩压的比率
#define SHURATE 0.70 //计算舒张压的比率
#define SHURATE2 0.65 //结束测量的的比率
#define RANGE 5//在多少个峰值内最大即为峰值
#define FENGZHIMIN 180
#define FENGZHIDELAY 4//放气一定延时之后才开始分析数据 
#define DCMAXERROR 300//充气气压大于此值为异常值
#define MINDOT 30 //记录的最小点数
//#define MAXDERTA 25 //相邻两个点差值的最大值，超过即为异常值
#define FROM 11//从第几个心跳开始指示
#define MAXDERTADERTA 150 //幅值限幅，大于前几个点平均值一定数额为异常值
#define LVBO3 4//限幅用到的均值滤波

unsigned char showXin[12]={
  0xD0,0xC4,0xC2,0xCA,0xA3,0xBA,0x20,0x20,0x20,0x20};//,0xB4,0xCE};//心率
unsigned char showShu[16]={
  0xCA,0xE6,0xD5,0xC5,0xD1,0xB9,0xA3,0xBA,0x20,0x20,0x20,0x20,'m','m','H','g'};//舒张压
unsigned char showShou[16]={
  0xCA,0xD5,0xCB,0xF5,0xD1,0xB9,0xA3,0xBA,0x20,0x20,0x20,0x20,'m','m','H','g'};//收缩压
unsigned char showIng[16]={
  0xD5,0xFD,0xD4,0xDA,0xB2,0xE2,0xC1,0xBF,':',0x20,0x20,0x20,'m','m','H','g'};//"正在测试";
unsigned char showError2[]={
  0xD3,0xF6,0xB5,0xBD,0xB4,0xED,0xCE,0xF3,0xA3,0xAC,0xC7,0xEB,0xD6,0xD8,0xC6,0xF4} ;//遇到错误，请重启
unsigned char showError1[]={
  0xB1,0xA7,0xC7,0xB8,0xA3,0xA1};  //抱歉!
unsigned char showBluetooth1[]={
  0xCE,0xB4,0xC1,0xAC,0xD6,0xC1,0xC6,0xE4,0xCB,0xFB,0xC9,0xE8,0xB1,0xB8};  //未连至其他设备
unsigned char showBluetooth2[]={
  0xD2,0xD1,0xC1,0xAC,0xD6,0xC1,0xC6,0xE4,0xCB,0xFB,0xC9,0xE8,0xB1,0xB8};  //已连至其他设备
unsigned char showBluetooth3[]={
  0xD2,0xD1,0xB7,0xA2,0xD6,0xC1,0xC6,0xE4,0xCB,0xFB,0xC9,0xE8,0xB1,0xB8};//已发至其他设备
unsigned char showPrepare[]={
  0xD7,0xBC,0xB1,0xB8,0xD6,0xD0,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20};//0xA1,0xA3};  //准备中。。。。。
int analogpin = A1;
int analogpinDC = A0;
int ledPin = 13;
int bumpPin = 2;
int valvePin = 3;

int heart;// 

int heartvaluepre;
int countTime;
int countDelay;

int lvbo2[LVBOC2];
int lvbo[LVBOC];

int sumLvbo;
int index;
boolean xielv;//true ;go up,false :go down 

boolean timerOn;//定时器是否开着（没有开定时器而关掉会导致死机）
int heartPre;
int ledTime;
int nAC;
int nACPre;
int nDC;
int nDCPre;
int pressureMaxPre;//上一个波峰
int pressureMinPre;//上一个波谷

int mPressureBegin;//0压力值

struct CounterHeartRate
{
  CounterHeartRate()
  {
    count = 0;
    counttime = 0;
    isRunning = false;
  }
  int count;
  int counttime;
  boolean isRunning;
  void countstart()
  {
    isRunning  = true;
  }
  void countstop()
  {
    isRunning = false;
  }
  void timer()
  {
    if(!isRunning)return ;
    counttime ++;
  }  
  void addone()
  {
    if(!isRunning)return ;
    count++;
  }
  int getHeart()
  {
    float temp = 60000.0/SCANT/counttime;
    return count * temp;
  }
} 
counterHeart;

struct MainDealer//电机控制和气阀控制
{
  unsigned char  mstatus;//所处状态（充气中1,放气中2,完成3,错误4）
  int bufferDC[MAXBUFFER];
  int bufferRear;
  int bufferDerta[MAXBUFFER];
  int countSmallTime;//较小峰值（通过这个来判断是否可以停止充气）的次数（为了避免偶然）
  int maxDerta;//最大峰峰值
  int maxDertaIndex;//最大峰峰值的索引值
  boolean bigger;//峰值在上升(true)还是在减小
  unsigned char countDelay;//一定范围内检测不到比较大的才是峰值
  unsigned char countFengZhiDelay;
  int resultPressShu;//最终求得的舒张压结果
  int resultPressShou;//最终求得的收缩压结果
  int lvbo3[LVBO3];
  unsigned char lvbo3index;
  int sumLvbo3;
  MainDealer()
  {
    mstatus = 0;
    countSmallTime = 0;
    bufferRear = 0;
    maxDerta = 0;
    maxDertaIndex = 0;
    bigger = true;
    countDelay = 0;
    countFengZhiDelay = FENGZHIDELAY;
    
    sumLvbo3 = 0;
    for(lvbo3index = 0;lvbo3index<LVBO3;lvbo3index++)
    {
      lvbo3[lvbo3index]=0;
    }
    lvbo3index = 0;
  }
  void turnBump(boolean b)
  {
    if(!b) digitalWrite(bumpPin,LOW);    
    else digitalWrite(bumpPin,HIGH);    
  }
  void turnValve(boolean b) 
  {
    if(!b)digitalWrite(valvePin,HIGH);
    else digitalWrite(valvePin,LOW);
  }
  void getBeginPressure()
  {
    mPressureBegin = nDC;//获得0压力值
  }
  void mStart()
  {
   turnBump(true);
   turnValve(true);
   mstatus = 1;
  }
  void dealDerta(int derta)
  {
    if(mstatus==0)//不进行任何处理（还没有开始）
    {
      return ;
    }
    if(mstatus==1)
    {
      if(mPressureBegin-nDC>DCMAXERROR)
      {
        timerOn = false;
        mstatus = 4;
        turnBump(false);
        turnValve(false);
        return ;
      }
      if(mPressureBegin-nDC>STOPBUMP&&derta<STOPBUMPDERTA)//达到停止充气的条件
      {
        countSmallTime ++;
        if(countSmallTime<=JUDGE)return ;        
        turnBump(false);
        mstatus = 2;//进入缓慢放气状态
        counterHeart.countstart();//开始数心跳数        
        return ;
      }      
    }
    else if(mstatus==2)
    {
      if(countFengZhiDelay>0)
      {
        countFengZhiDelay--;
        return ;
      }
      /***************************幅值限幅*******************************************/
      if(derta-sumLvbo3/LVBO3>MAXDERTADERTA)
      {
        return ;//抛弃这个数据
      }
      sumLvbo3 -= lvbo3[lvbo3index];
      lvbo3[lvbo3index] = derta;
      sumLvbo3 += derta;
      lvbo3index++;
      if(lvbo3index>=LVBO3)lvbo3index = 0;
      /***************************幅值限幅*******************************************/
      
      if(bufferRear>=MAXBUFFER)
      {
        mstatus = 4;
        turnBump(false);
        turnValve(false);
        timerOn = false;
        return ;
      }
      if(derta>maxDerta)
      {
        maxDerta=derta;
        maxDertaIndex = bufferRear;
        countDelay = 0;
        return ;
      }
      bufferDC[bufferRear] = nDC;
      bufferDerta[bufferRear] = derta;
      bufferRear++;
      
      countDelay++;
      if(bigger&&(derta<maxDerta)&&(countDelay>RANGE))
      { 
        bigger=false;//由上升期进入下降期
        return ;
      }
      if((!bigger)&&(derta<maxDerta*SHURATE2)&&(maxDerta>FENGZHIMIN)&&bufferRear>MINDOT)
      {
        mstatus = 3;
        turnValve(false);//测试完毕，放气
        if(timerOn)
        {
          timerOn = false;
         // MsTimer2::stop();
        }
        counterHeart.countstop();
        return ;
      }
    }
  }
  void calResult()
  {
    #ifdef DEBUG2
    Serial.print('\n');
    Serial.print('\n');
    Serial.print('\n');
    for(int i = 0;i<bufferRear;i++)
    {
        Serial.print(bufferDerta[i]);
        Serial.print('\n');
    }
    #endif  
    resultPressShou = (mPressureBegin-bufferDC[0])*100/JIAOZHENG;//意外情况，下面的程序没有找到值
    for(int i = maxDertaIndex;i>=0;i--)
    {
      if(bufferDerta[i]<maxDerta*SHOURATE)
      {
        resultPressShou = (mPressureBegin-bufferDC[i])*100/JIAOZHENG;
        break;
      }
    }
    resultPressShu = (mPressureBegin-bufferDC[bufferRear-1])*100/JIAOZHENG;//意外情况，下面的程序没有找到值
    for(int i = maxDertaIndex;i<bufferRear;i++)
    {
      if(bufferDerta[i]<maxDerta*SHURATE)
      {
        resultPressShu = (mPressureBegin-bufferDC[i])*100/JIAOZHENG;
        break;
      }      
    }
    
  }
  void pinHua(char arg)
  {
    char cou = 0;
    char inde = 0;
    int sumtemp = 0;
//    for(int i = maxDertaIndex;i>=0;i--)
    int i = maxDertaIndex;
    while(1)
    {
      if(cou<LVBOC2)
      {
        sumtemp += bufferDerta[i];
        lvbo2[cou] = bufferDerta[i];
        cou++;
        bufferDerta[i] = sumtemp/cou;
      }
      else 
      {
        sumtemp -= lvbo2[inde];
        sumtemp += bufferDerta[i];
        lvbo2[inde] = bufferDerta[i];
        bufferDerta[i] = sumtemp/LVBOC2;
      }
      inde++;
      if(inde>=LVBOC2)inde = 0;
      if(arg==1)i--;
      else i++;
      if(i<0||i>=bufferRear)break;
    }
  }
} mainDealer;

struct ResultDealer
{
  unsigned char  haveSend;//0为没有连接，1为已连接，2为已发送结果信息
  ResultDealer()
  {
    haveSend = 0;
  } 
 void  showheart(int n)
  {
    if(n>999)return ;
    unsigned char i = 8;
    while(n)
    {
      showXin[i--] = n%10 + '0';
      n/=10;
    }
    showXin[10] = 0xB4;
    showXin[11] = 0xCE;
    LCDA.DisplayString(0,1,showXin,AR_SIZE(showXin));//  
  }
  void showPress(int n,char arg)//arg=1,showShouPress,arg=2,showShuPress
  {
    if(n<0)
    {
      n = -n;
      if(arg==1)showShou[7] = '-';
      else if(arg==2) showShu[7] = '-';
      else showIng[7] = '-';
    }
    int i = 11;
    while(i>7)
    {
      if(n>0)
      {
        if(arg==1)showShou[i--] = n%10 + '0';
        else if(arg==2) showShu[i--] = n%10 + '0';
        else showIng[i--] = n%10 + '0';
        n/=10;
      }
      else 
      {
        if(arg==1)showShou[i--] = 0x20;
        else if(arg==2)showShu[i--] = 0x20;
        else showIng[i--] = 0x20;
      }
    }
    if(arg==1)LCDA.DisplayString(1,0,showShou,AR_SIZE(showShou));//  
    else if(arg==2)LCDA.DisplayString(2,0,showShu,AR_SIZE(showShu));//  
    else LCDA.DisplayString(1,0,showIng,AR_SIZE(showShu));//  
  }
  void showTesting()
  {
    int v = (mPressureBegin-nDC)*100/JIAOZHENG;
    if(v<0)v=0;
    showPress(v,3);
    if(haveSend==0)
    {
      LCDA.DisplayString(3,0,showBluetooth1,AR_SIZE(showBluetooth1));//  
      if(Serial.available()>0)
      {
        if(Serial.read()==1)
        haveSend = 1;
      }
    }
    else if(haveSend==1)
    {
      LCDA.DisplayString(3,0,showBluetooth2,AR_SIZE(showBluetooth2));//  
    }    
  }
  void showBegin()
  {
    for(char a = 0;a<8;a+=2)
    {
      showPrepare[6+a]=0xA1;
      showPrepare[7+a]=0xA3;
      LCDA.DisplayString(1,0,showPrepare,AR_SIZE(showPrepare));//准备中。。。。。
      delay(500);     
    }
  }
  void showErr()
  {
    if(timerOn)
    {
      MsTimer2::stop();
      timerOn = false;
    }
    LCDA.CLEAR();//����
    LCDA.DisplayString(0,3,showError1,AR_SIZE(showError1));//抱歉!      
    LCDA.DisplayString(2,0,showError2,AR_SIZE(showError2));//遇到错误，请重启      
    digitalWrite(ledPin,HIGH);
  }
  void showLed()
  {
    
    if(heart>heartPre)
    {
      //ledTime = LEDTIME;      
      if(mainDealer.mstatus==2&&mainDealer.bufferRear>FROM)digitalWrite(ledPin,HIGH);
      delay(100);
      heartPre = heart;
    }
/*    delay(100);
    if(ledTime>=0)
    {
      ledTime --;
    }*/
    digitalWrite(ledPin,LOW);
  }
  void showResult()
  {
    digitalWrite(ledPin,LOW);
    mainDealer.pinHua(1);
     mainDealer.calResult();
    mainDealer.pinHua(2);
    mainDealer.calResult();
    showheart(counterHeart.getHeart());
    showPress(mainDealer.resultPressShou,1);
    showPress(mainDealer.resultPressShu,2);
    if(haveSend==0)
    {
      LCDA.DisplayString(3,0,showBluetooth1,AR_SIZE(showBluetooth1));
    }
    else if(haveSend==1)
    {
      LCDA.DisplayString(3,0,showBluetooth2,AR_SIZE(showBluetooth2));      
    }
    while(1)
    {
      if(Serial.available()>0)
      {
        if(Serial.read()==1)
        {
          haveSend = 1;
        }
      }
      if(haveSend==1)break;
    }
    char sendData = counterHeart.getHeart()/128;
    Serial.write(sendData);
    sendData = counterHeart.getHeart()%128;
    Serial.write(sendData);
    sendData = mainDealer.resultPressShou/128;
    Serial.write(sendData);    
    sendData = mainDealer.resultPressShou%128;
    Serial.write(sendData);
    sendData = mainDealer.resultPressShu/128;
    Serial.write(sendData);    
    sendData = mainDealer.resultPressShu%128;
    Serial.write(sendData);
    LCDA.DisplayString(3,0,showBluetooth3,AR_SIZE(showBluetooth3));
  }
} resultDealer;
void setup()
{
  Serial.begin(9600);//手机蓝牙的波特率是9600
#ifdef DEBUG
  Serial.begin(115200);
#endif
#ifdef DEBUG2
  Serial.begin(115200);
#endif
  pinMode(ledPin, OUTPUT);
  pinMode(bumpPin, OUTPUT);
  pinMode(valvePin, OUTPUT);
  
  heartPre = 0;  
  LCDA.Initialise(); // INIT SCREEN
  delay(100);
  LCDA.CLEAR();//����
  delay(100);
  LCDA.DisplaySig(0,0,0x20);//
  delay(100);
  waveInit();
  
  mainDealer.turnBump(false);
  mainDealer.turnValve(false);
  //delay(3000);//暂停，等待开始
  resultDealer.showBegin();
  mainDealer.getBeginPressure();
  delay(200);
  mainDealer.mStart();
}
void waveInit()
{
  heart = 1;
  index = 0;
  xielv  = true;
  heartvaluepre = 0;
  for(char i = 0;i<LVBOC;i++)
  {
    lvbo[i] = 0;
  }
  MsTimer2::set(SCANT,intFunc);
  MsTimer2::start();
  timerOn = true;
  countTime = 0;
  countDelay = 0;
  nDCPre = -1;
  nACPre = -1;
}
void loop()
{
  while(1)
  {
    resultDealer.showLed();
    if(mainDealer.mstatus==1||mainDealer.mstatus==2)resultDealer.showTesting();
    if(mainDealer.mstatus==4)
    {
      resultDealer.showErr();
      break;
    }
    else if(mainDealer.mstatus==3)  
    {
      resultDealer.showResult();
      break;
    }
  }
  while(1);//什么也不做
}
void intFunc()
{
  if(!timerOn)return ;
  countTime++;
  countDelay ++;
  counterHeart.timer();
  /***********直限幅********************/
  nAC = analogRead(analogpin);
  nDC = analogRead(analogpinDC);  
  /****************************限幅********************/
//  if(mainDealer.mstatus==2)
//  {
//    if(nAC-heartvaluepre>MAXDERTA)nAC = heartvaluepre + MAXDERTA;
//    if(heartvaluepre-nAC>MAXDERTA)nAC = heartvaluepre - MAXDERTA;  
//  }
  
  /****************************限幅********************/
  
  
  /*减少不必要的耗时操作
   if(nDCPre!=-1)
   {
   if(nDC>nDCPre+DCMAXD)nDC = nDCPre+DCMAXD;
   else if(nDC<nDCPre-DCMAXD)nDC = nDCPre-DCMAXD;
   }
   if(nACPre!=-1)
   {
   if(n>nACPre+ACMAXD)nDC = nACPre+ACMAXD;
   else if(n<nACPre-ACMAXD)nDC = nACPre-ACMAXD;
   }
   */
  /***********直限幅********************/

//  Serial.print(nDC);
//  Serial.print(" ");

  /***********均值滤波********************/
  int valueNow;
  boolean xielvpre = xielv;
  sumLvbo -= lvbo[index];
  lvbo[index] = nAC;
  sumLvbo += nAC;
  index++;
  if(index>=LVBOC)index = 0;
  valueNow = sumLvbo/LVBOC;
  /***********均值滤波********************/
#ifdef DEBUG
  Serial.print(valueNow);
  Serial.print("\n");
#endif
  /***********判断波峰和波谷********************/

  /*在时间紧急的情况下(为了减少中断处理时间),在这个实例中用不到  
   if(valueNow>heartvaluepre+DIS)xielv = true;
   else if(valueNow<heartvaluepre-DIS)xielv = false;
   */
   
  if(valueNow>heartvaluepre)xielv = true;
  else if(valueNow<heartvaluepre)xielv = false;
  else xielv = xielvpre;
  if(xielvpre==true&&xielv==false)//peak
  {
    if(countDelay>MAXRATE/SCANT)
    {
      heart ++;
      counterHeart.addone();      
      countDelay = 0;
      pressureMaxPre = valueNow;
      mainDealer.dealDerta(pressureMaxPre - pressureMinPre);
    }
  }
  else if(xielvpre==false&&xielv==true)//valley
  {
      pressureMinPre = valueNow;
  }
  /***********判断波峰和波谷********************/
  heartvaluepre = valueNow;
}
