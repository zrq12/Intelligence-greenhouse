//土壤三合一传感器（485，5-30V，排针4，485要一根线供电），水泵（继电器），
#include <Arduino.h>
#include <ESP8266WiFi.h>  //esp8266
#include <PubSubClient.h>
#include <string.h>
#include <stdio.h>
#include <U8g2lib.h>
#include <math.h>
#include <Wire.h>

#define BH1750_ADDRESS_LOW      0x23
#define BH1750_ADDRESS_HIGH     0x5C
#define BH1750_ADDRESS          BH1750_ADDRESS_LOW
#define ADDR 0b0100011    // I2C 设备地址
#define LED 2//定义了2号引脚连接led
U8G2_SSD1306_128X64_NONAME_F_SW_I2C u8g2(U8G2_R0, 14, 2, U8X8_PIN_NONE);



String clientId = "esp8266-" + WiFi.macAddress();
const char *ssid = "wdf";  // 输入你的WiFi名称
const char *password = "12345678";  // 输入你的WiFi密码
const char *mqtt_broker = "114.55.89.187";  //输入你的无线局域网适配器IPv4地址
const int mqtt_port = 1883;  //默认为1883
const char *a="123";


// u8g2显示屏定义
const int u8g2_SDA = 2;
const int u8g2_SCL = 14;

int relay_pin = 13; //继电器引脚


// 查询码
unsigned char item[8] = {0x01, 0x03, 0x00, 0x00, 0x00, 0x04, 0x44, 0x09};
int i = 0; // 初始化样本计数器

// 函数声明

void updateu8g2(const String &temperature, const String &humidity, const String &ph);
double *readAndRecordData();
void callback(char *topic, byte *payload, unsigned int length);

 
WiFiClient espClient;
PubSubClient client(espClient);


void setup() {
  pinMode(relay_pin, OUTPUT);

  digitalWrite(relay_pin, LOW);
  Serial.begin(4800); // 频段
  WiFi.begin(ssid, password);
  u8g2.begin();
  
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

// 从函数获取温湿度等数据
double *sensorData = readAndRecordData(); 

// 将数组数据转换为字符串格式
String temperature = String(sensorData[1], 2);
String humidity = String(sensorData[0], 2);
String conductivity = String(sensorData[2], 2);
String ph = String(sensorData[3], 2);



// 使用 snprintf 构建 JSON 字符串
if(sensorData[0]!=0){
snprintf(publish1, sizeof(publish1), 
          "{\"Soilhum\":\"%s\",\"Soiltemp\":\"%s\",\"Ph\":\"%s\"}", 
          humidity.c_str(), temperature.c_str(), ph.c_str());
}


       
  client.publish("mqtt135", publish1);
  Serial.println(publish1);
   Serial.println("------------------------------");
    Serial.print("Temperature: ");
    Serial.print(temperature);
    Serial.println(" °C");
    Serial.print("Humidity: ");
    Serial.print(humidity);
    Serial.println(" %");
    Serial.print("Conductivity: ");
    Serial.print(conductivity);
    Serial.println(" uS/cm");
    Serial.print("PH: ");
    Serial.print(ph);
    Serial.println();
    // 在u8g2屏幕上显示数据
  updateu8g2(temperature, humidity, ph);

  
//清空数组
//    memset(sensorData, 0, sizeof(sensorData));
//    memset(publish1, 0, sizeof(publish1));
//    Serial.println(mqttClient.state());
  delay(500);//必须延时
  client.loop();     
}



// updateu8g2 函数，现在接受额外的参数来显示温度、湿度、pH和电导率
void updateu8g2(const String &temperature, const String &humidity, const String &ph){
    u8g2.firstPage();
    u8g2.setFont(u8g2_font_unifont_t_symbols);
    do {
        u8g2.clearBuffer(); // 清除当前缓冲区
        u8g2.setCursor(0, 15); // 根据需要设置文本起始位置
        u8g2.print("Temp: ");
        u8g2.print(temperature);
        u8g2.print(" °C");
        u8g2.setCursor(0, 30);
        u8g2.print("Humidity: ");
        u8g2.print(humidity);
        u8g2.print(" %");
        u8g2.setCursor(0, 45);
        u8g2.print("PH: ");
        u8g2.print(ph);
        // 继续添加其他数据的显示逻辑
    } while (u8g2.nextPage());
}


// readAndRecordData 读取温湿度等数据的函数
double *readAndRecordData() {
    // 此处应添加读取温湿度等数据的代码
    // ...
    static double sensorData[4];
    String data = "";
    String info[11];
    
    for (int i = 0; i < 8; i++) {  // 发送测温命令
        Serial.write(item[i]);   // write输出
    }
    delay(100);  // 等待测温数据返回
    
    while (Serial.available()) {  // 从串口中读取数据
        unsigned char in = (unsigned char)Serial.read();  // read读取
        Serial.print(in, HEX); // 16进制
        Serial.print(" ");
        data += String(in);
        data += ',';
    }
    
    if (data.length() > 0) {
        Serial.println("Data length: " + String(data.length())); // 打印数据长度并转换为字符串
        Serial.println(data);
        int commaPosition = -1;
        for (int i = 0; i < 11; i++) {
            commaPosition = data.indexOf(',');
            if (commaPosition != -1) {
                info[i] = data.substring(0, commaPosition);
                data = data.substring(commaPosition + 1);
            } 
            else {
                if (data.length() > 0) 
                    info[i] = data.substring(0, data.length());
                
                break; // 没有逗号了，退出循环
            }
        }
    }
    

   
    sensorData[0] = (info[3].toInt() * 256 + info[4].toInt()) / 10.0;//湿度
    sensorData[1] = (info[5].toInt() * 256 + info[6].toInt()) / 10.0;//温度
    sensorData[2] = (info[7].toInt() * 256 + info[8].toInt()) / 10.0;//电导率
    sensorData[3] = (info[9].toInt() * 256 + info[10].toInt()) / 10.0;//PH
    
    return sensorData;
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
    if (message == "[\"water_on\"]") {
           
            digitalWrite(relay_pin, HIGH);
      Serial.print(" OK");
    }
      
    if (message == "[\"water_off\"]"){ 
     
      digitalWrite(relay_pin, LOW);
      Serial.print(" OK");
      } 
    
    Serial.println();
    Serial.println("-----------------------");
}
