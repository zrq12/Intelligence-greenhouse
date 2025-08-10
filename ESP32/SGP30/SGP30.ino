//2.空气：co2传感器（排针4），dht11（排针3），排气扇（12v，两根线），加热器（12v，两根线，不一定带动），加湿器（可不用）
#include <stdio.h>
#include <Arduino.h>
#include <ESP8266WiFi.h>  //esp8266
#include <PubSubClient.h>
#include <string.h>
#include <Adafruit_SGP30.h>
#include <U8g2lib.h>
#include <math.h>
#include <Wire.h>
#include "DHT.h"
#define DHTPIN 5
#define DHTTYPE DHT11
 // sgp气体定义
const int sgp30_SDA = 12;
const int sgp30_SCL = 13;
//排气扇定义
const int pai = 4;
//加热器
const int warm = 5;
Adafruit_SGP30 sgp;

U8G2_SSD1306_128X64_NONAME_F_SW_I2C u8g2(U8G2_R0, 14, 2, U8X8_PIN_NONE);

DHT dht(DHTPIN,DHTTYPE);



String clientId = "esp8266-" + WiFi.macAddress();
const char *ssid = "wdf";  // 输入你的WiFi名称
const char *password = "12345678";  // 输入你的WiFi密码
const char *mqtt_broker = "114.55.89.187";  //输入你的无线局域网适配器IPv4地址
const int mqtt_port = 1883;  //默认为1883
const char *a="123";

// u8g2显示屏定义
const int u8g2_SDA = 2;
const int u8g2_SCL = 14;



// 函数声明
void  updateu8g2(const String &temperature, const String &humidity,const String &TVOC, const String &eCO2);
double *readAndRecordData();
void callback(char *topic, byte *payload, unsigned int length);
void sgp_Init();
 
WiFiClient espClient;
PubSubClient client(espClient);


void setup() {
  
  Serial.begin(4800); // 频段
 pinMode(pai,OUTPUT);
 pinMode(warm,OUTPUT);
 digitalWrite(pai, LOW);
 digitalWrite(warm, LOW);
  WiFi.begin(ssid, password);
  u8g2.begin();
  sgp_Init();
  dht.begin(); //DHT开始工作
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
String TVOC = String(sgp.TVOC, DEC);
String eCO2 = String(sgp.eCO2, DEC);
float hum = dht.readHumidity(); // 获取湿度
float tem = dht.readTemperature(); // 读取温度
// 将变量转换为字符串格式
String h = String(hum, 2);
String t = String(tem, 2);

//// 使用 snprintf 构建 JSON 字符串
snprintf(publish1, sizeof(publish1), 
          "{\"TVO2\":\"%s\",\"Co2\":\"%s\",\"Humi\":\"%s\",\"Temp\":\"%s\"}", 
       TVOC.c_str(), eCO2.c_str(), h.c_str(), t.c_str());

if (! sgp.IAQmeasure()) {
    Serial.println("Measurement failed");
    return;
  }
  
       
  client.publish("mqtt135", publish1);
  Serial.println(publish1);
   Serial.println("------------------------------");
    Serial.print("TVOC "); Serial.print(sgp.TVOC); Serial.println(" ppb\t");
  Serial.print("eCO2 "); Serial.print(sgp.eCO2); Serial.println(" ppm");
 Serial.print("Temperature: ");
    Serial.print(t);
    Serial.println(" °C");
    Serial.print("Humidity: ");
    Serial.print(h);
    Serial.println(" %");
    Serial.print(hum);
    Serial.print(tem);
    
    // 在u8g2屏幕上显示数据
  updateu8g2(t,h,TVOC, eCO2);
//清空数组
//    memset(sensorData, 0, sizeof(sensorData));
//    memset(publish1, 0, sizeof(publish1));
    
  delay(500);
  client.loop();     
                   
}



// updateu8g2 函数，现在接受额外的参数来显示温度、湿度、pH和电导率
void updateu8g2(const String &temperature, const String &humidity,const String &TVOC, const String &eCO2) {
    u8g2.firstPage();
    u8g2.setFont(u8g2_font_unifont_t_symbols);
    do {
        u8g2.clearBuffer(); // 清除当前缓冲区
        u8g2.setCursor(0, 15); // 根据需要设置文本起始位置
        u8g2.print("TVOC: ");
        u8g2.print(TVOC);
        u8g2.print(" ppb\t");
        u8g2.setCursor(0, 30);
        u8g2.print("eCO2: ");
        u8g2.print(eCO2);
        u8g2.print(" ppm");
        u8g2.setCursor(0, 45); // 根据需要设置文本起始位置
        u8g2.print("Temp: ");
        u8g2.print(temperature);
        u8g2.print(" °C");
        u8g2.setCursor(0, 60);
        u8g2.print("Humi: ");
        u8g2.print(humidity);
        u8g2.print(" %");
        // 继续添加其他数据的显示逻辑
    } while (u8g2.nextPage());
}

void sgp_Init(){
Wire.begin(sgp30_SDA,sgp30_SCL);
  Serial.println("SGP30 test");
 
  if (! sgp.begin()){
    Serial.println("Sensor not found :(");
    while (1);
  }
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
    if (message == "[\"cloud_on\"]") {
            
      digitalWrite(pai, HIGH);
      
    }
      
    if (message == "[\"cloud_off\"]"){ 
      
      digitalWrite(pai,LOW);
     
      } 
       if (message == "[\"warm_on\"]"){ 
      
      digitalWrite(warm,HIGH);
     
      } 
       if (message == "[\"warm_off\"]"){ 
      
      digitalWrite(warm,LOW);
     
      } 
    
    Serial.println();
    Serial.println("-----------------------");
}
