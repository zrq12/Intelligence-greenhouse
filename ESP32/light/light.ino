
//光：两个遮阳帘电机控制（也可以1个遮阳帘），LED补光灯（12V，两根线），光传感器（排针5）
#include <Arduino.h>
#include <ESP8266WiFi.h>  //esp8266;
#include <PubSubClient.h>
#include <string.h>
#include <stdio.h>
#include "DHT.h"
#include <U8g2lib.h>
#include <math.h>
#include <Wire.h>
#include <Ticker.h>

#define BH1750_ADDRESS_LOW      0x23
#define BH1750_ADDRESS_HIGH     0x5C
#define BH1750_ADDRESS          BH1750_ADDRESS_LOW
#define ADDR 0b0100011    // I2C 设备地址
 
int i=0;

U8G2_SSD1306_128X64_NONAME_F_SW_I2C u8g2(U8G2_R0, 14, 2, U8X8_PIN_NONE);



String clientId = "esp8266-" + WiFi.macAddress();
const char *ssid = "wdf";  // 输入你的WiFi名称
const char *password = "12345678";  // 输入你的WiFi密码
const char *mqtt_broker = "114.55.89.187";  //输入你的无线局域网适配器IPv4地址
const int mqtt_port = 1883;  //默认为1883
const char *a="123";

// I2C 引脚定义
const int BH1750_SDA = 5;
const int BH1750_SCL = 4;

// u8g2显示屏定义
const int u8g2_SDA = 2;
const int u8g2_SCL = 14;
//电机定义
const int AIN1 = 12;
const int AIN2 = 13;
const int PWMA =16;
const int led = 15;
// 查询码

// 函数声明
void BH1750_Init();
int Read_BH1750();
void updateu8g2(const String &val);
void callback(char *topic, byte *payload, unsigned int length);
WiFiClient espClient;
PubSubClient client(espClient);


void setup() {
  pinMode(AIN1,OUTPUT);
  pinMode(AIN2,OUTPUT);
  pinMode(PWMA,OUTPUT);
  pinMode(led,OUTPUT);
    digitalWrite(AIN1, LOW);          
    digitalWrite(AIN2, LOW); 
    digitalWrite(PWMA, HIGH);
    digitalWrite(led, LOW);
  Serial.begin(4800); // 频段
  WiFi.begin(ssid, password);
  u8g2.begin();
  BH1750_Init();  // 初始化BH1750光照传感器
  
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.println("Connecting to WiFi..");
    }
    Serial.println("Connected to the WiFi network");
    client.setServer(mqtt_broker, mqtt_port);
//将内置的回调函数指向我们的回调函数进行处理
    client.setCallback(callback);
        while (!client.connected()) {
        Serial.println("Connecting to public emqx mqtt broker.....");
        if (client.connect(clientId.c_str())) {
            Serial.println("Public emqx mqtt broker connected");
        } else {
            Serial.print("failed with state ");
            Serial.print(client.state());
            delay(2000);
        }
    }
    //subscribe 订阅
    client.subscribe("Control");
    
     
     
}
 

 
void loop() {
  

   
char publish1[128];

// 读取光照度数值

String val = String(Read_BH1750()); // 假设 Read_BH1750 返回的是整数值

// 使用 snprintf 构建 JSON 字符串
snprintf(publish1, sizeof(publish1), 
          "{\"Light\":\"%s\"}", 
           val.c_str());



       
  client.publish("mqtt135", publish1);
  Serial.println(publish1);
   Serial.println("------------------------------");
    
    Serial.print("Light Intensity: ");
    Serial.println(val);
    Serial.print(" lx");
    // 在u8g2屏幕上显示数据
  updateu8g2(val);
//清空数组
//    memset(sensorData, 0, sizeof(sensorData));
//    memset(publish1, 0, sizeof(publish1));
    
  
  client.loop();     
   delay(200);
}



// updateu8g2 函数，现在接受额外的参数来显示温度、湿度、pH和电导率
void updateu8g2(const String &val) {
    u8g2.firstPage();
    u8g2.setFont(u8g2_font_unifont_t_symbols);
    do {
        u8g2.clearBuffer(); // 清除当前缓冲区
        u8g2.setCursor(0, 15); // 根据需要设置文本起始位置
        u8g2.print("Light: ");
        u8g2.print(val);
        u8g2.print(" lx");
        // 继续添加其他数据的显示逻辑
    } while (u8g2.nextPage());
}

// BH1750_Init 初始化BH1750光照传感器
void BH1750_Init() {
    // 发送初始化命令到BH1750
    Wire.begin(BH1750_SDA,BH1750_SCL);  // Wire.begin(SDA_PIN, SCL_PIN); // 初始化 I2C 通信并指定 SDA 和 SCL 引脚
    Wire.beginTransmission(ADDR);
    Wire.write(0b00000001);
  
    Wire.endTransmission();
}

// Read_BH1750 从BH1750读取光照度
int Read_BH1750() {
    int val = 0;
    
    Wire.beginTransmission(ADDR);
    Wire.write(0x10);
    Wire.endTransmission();
 
    Wire.beginTransmission(ADDR);
    Wire.write(0b00100000);
    Wire.endTransmission();
    delay(120);
    /*计算光照*/
    Wire.requestFrom(ADDR, 2);      //每次 2byte
    for (val = 0; Wire.available() >= 1; ) {
        char c = Wire.read();
        val = (val << 8) + (c & 0xFF);
    }
    val = val / 1.2;

    return val;
}
void callback(char *topic, byte *payload, unsigned int length) {
  
    Serial.print("Message arrived in topic: ");
    Serial.println(topic);
    Serial.print("Message:");
    String message;
    for (int i = 0; i < length; i++) {
        message = message + (char) payload[i];  // convert *byte to string
    }
    Serial.print(message);
//以下为点灯判断

if (message == "[\"led_off\"]") {
            digitalWrite(led, LOW);// 灯 off
            
      
    }
    if (message == "[\"led_on\"]") {
            digitalWrite(led, HIGH);// 灯 on
    
      
    }
    if (message == "[\"sun_off\"]"&&i==1) {
            digitalWrite(AIN1, HIGH);// 电机 off
            digitalWrite(AIN2, LOW);// 电机 off
            delay(2000);
            digitalWrite(AIN1, LOW);// 电机 off
            digitalWrite(AIN1, LOW);// 电机 off
            i=0;
      
    }
      
    if (message == "[\"sun_on\"]"&&i==0){ 
      
       digitalWrite(AIN2, HIGH);// 电机 on
       digitalWrite(AIN1, LOW);// 电机 on
       delay(2000);
        digitalWrite(AIN2, LOW);// 电机 on
        digitalWrite(AIN1, LOW);// 电机 on
       i=1;
     
      } 
    
    Serial.println();
    Serial.println("-----------------------");
}
