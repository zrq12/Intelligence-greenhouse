"""
import serial
import time
import json
import paho.mqtt.client as mqtt

def hex_to_bytes(hex_str):
    return bytes.fromhex(hex_str)

def bytes_to_hex(byte_data):
    return byte_data.hex()

def ttl_init(port, baud):
    ser = serial.Serial(port, baud)
    ser.bytesize = serial.EIGHTBITS
    ser.parity = serial.PARITY_NONE
    ser.stopbits = serial.STOPBITS_ONE
    print("串口已设置")
    return ser

def ttl_open(ser):
    if ser:
        if not ser.isOpen():
            ser.open()
        print("串口已打开")
    else:
        print("串口未设置")

def ttl_close(ser):
    if ser:
        if ser.isOpen():
            ser.close()
        print("串口已关闭")
    else:
        print("串口未设置")

def read_soil_data(ser):
    if ser:
        send_data = "0103000000044409"
        send_bytes = hex_to_bytes(send_data)
        ser.write(send_bytes)
        time.sleep(0.1)
        data = ""
        while ser.inWaiting() > 0:
            in_byte = ser.read()
            data += bytes_to_hex(in_byte)
            data += ","
        if data:
            info = data.split(',')[:-1]
            humidity = (int(info[3], 16) * 256 + int(info[4], 16)) / 10.0
            temperature = (int(info[5], 16) * 256 + int(info[6], 16)) / 10.0
            conductivity = (int(info[7], 16) * 256 + int(info[8], 16)) / 10.0
            ph = (int(info[9], 16) * 256 + int(info[10], 16)) / 10.0
            return humidity, temperature, conductivity, ph
    return None, None, None, None

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT Broker!")
    else:
        print("Failed to connect, return code %d\n", rc)

def main():
    ser = ttl_init('/dev/ttyHS1', 4800)
    ttl_open(ser)
    
    mqtt_broker = "114.55.89.187"
    mqtt_port = 1883
    mqtt_topic = "mqtt135"

    client = mqtt.Client()
    client.on_connect = on_connect

    client.connect(mqtt_broker, mqtt_port, 60)
    client.loop_start()

    try:
        while True:
            humidity, temperature, conductivity, ph = read_soil_data(ser)
            if humidity is not None:
                print(f"Humidity: {humidity} %")
                print(f"Temperature: {temperature} °C")
                print(f"Conductivity: {conductivity} uS/cm")
                print(f"PH: {ph}")

                payload = json.dumps([
                    {"Soiltemp": str(temperature)},
                    {"Soilhum": str(humidity)},
                    {"Ph": str(ph)}
                ])
                
                client.publish(mqtt_topic, payload)
                print(f"Published to {mqtt_topic}: {payload}")
            else:
                print("无法读取土壤数据")
            time.sleep(1)
    except KeyboardInterrupt:
        ttl_close(ser)
        client.loop_stop()
        client.disconnect()
        print("程序已退出")

if __name__ == "__main__":
    main()

import subprocess
from time import sleep
import paho.mqtt.client as mqtt

GPIO5 = "432"

def adb_cmd(cmd):
    result = subprocess.run(['adb', 'shell', cmd], capture_output=True, text=True)
    output = result.stdout.strip()
    return output

def gpio_init(gpio):
    adb_cmd('echo ' + gpio + ' > /sys/class/gpio/export')

def check_gpio_direction(gpio):
    output = adb_cmd('cat /sys/class/gpio/gpio' + gpio + '/direction')
    print(output)

def set_gpio_direction(gpio, direction):
    adb_cmd('echo ' + direction + ' > /sys/class/gpio/gpio' + gpio + '/direction')
    check_gpio_direction(gpio)

def check_gpio_value(gpio):
    output = adb_cmd('cat /sys/class/gpio/gpio' + gpio + '/value')
    print(output)

def set_gpio_value(gpio, value):
    adb_cmd('echo ' + value + ' > /sys/class/gpio/gpio' + gpio + '/value')
    check_gpio_value(gpio)

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT Broker!")
        client.subscribe("Control")
    else:
        print("Failed to connect, return code %d\n", rc)

def on_message(client, userdata, msg):
    print(f"Received message: {msg.payload.decode()} from topic: {msg.topic}")
    if msg.payload.decode() == '["water_on"]':
        set_gpio_value(GPIO5, "0")
    elif msg.payload.decode() == '["water_off"]':
        set_gpio_value(GPIO5, "1")

def main():
    gpio_init(GPIO5)
    set_gpio_direction(GPIO5, "out")

    mqtt_broker = "114.55.89.187"
    mqtt_port = 1883

    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(mqtt_broker, mqtt_port, 60)
    client.loop_start()

    try:
        while True:
            sleep(1)
    except KeyboardInterrupt:
        client.loop_stop()
        client.disconnect()
        print("程序已退出")

if __name__ == "__main__":
    main()
"""
import serial
import time
import json
import paho.mqtt.client as mqtt
import subprocess
from time import sleep

# GPIO and Serial port definitions
GPIO5 = "432"
SERIAL_PORT = '/dev/ttyHS1'
BAUD_RATE = 4800

# MQTT settings
MQTT_BROKER = "114.55.89.187"
MQTT_PORT = 1883
MQTT_TOPIC = "mqtt135"
MQTT_CONTROL_TOPIC = "Control"

# GPIO control functions
def adb_cmd(cmd):
    result = subprocess.run(['adb', 'shell', cmd], capture_output=True, text=True)
    output = result.stdout.strip()
    return output

def gpio_init(gpio):
    adb_cmd('echo ' + gpio + ' > /sys/class/gpio/export')

def check_gpio_direction(gpio):
    output = adb_cmd('cat /sys/class/gpio/gpio' + gpio + '/direction')
    print(output)

def set_gpio_direction(gpio, direction):
    adb_cmd('echo ' + direction + ' > /sys/class/gpio/gpio' + gpio + '/direction')
    check_gpio_direction(gpio)

def check_gpio_value(gpio):
    output = adb_cmd('cat /sys/class/gpio/gpio' + gpio + '/value')
    print(output)

def set_gpio_value(gpio, value):
    adb_cmd('echo ' + value + ' > /sys/class/gpio/gpio' + gpio + '/value')
    check_gpio_value(gpio)

# Serial and soil data functions
def hex_to_bytes(hex_str):
    return bytes.fromhex(hex_str)

def bytes_to_hex(byte_data):
    return byte_data.hex()

def ttl_init(port, baud):
    ser = serial.Serial(port, baud)
    ser.bytesize = serial.EIGHTBITS
    ser.parity = serial.PARITY_NONE
    ser.stopbits = serial.STOPBITS_ONE
    print("串口已设置")
    return ser

def ttl_open(ser):
    if ser:
        if not ser.isOpen():
            ser.open()
        print("串口已打开")
    else:
        print("串口未设置")

def ttl_close(ser):
    if ser:
        if ser.isOpen():
            ser.close()
        print("串口已关闭")
    else:
        print("串口未设置")

def read_soil_data(ser):
    if ser:
        send_data = "0103000000044409"
        send_bytes = hex_to_bytes(send_data)
        ser.write(send_bytes)
        time.sleep(0.1)
        data = ""
        while ser.inWaiting() > 0:
            in_byte = ser.read()
            data += bytes_to_hex(in_byte)
            data += ","
        if data:
            info = data.split(',')[:-1]
            humidity = (int(info[3], 16) * 256 + int(info[4], 16)) / 10.0
            temperature = (int(info[5], 16) * 256 + int(info[6], 16)) / 10.0
            conductivity = (int(info[7], 16) * 256 + int(info[8], 16)) / 10.0
            ph = (int(info[9], 16) * 256 + int(info[10], 16)) / 10.0
            return humidity, temperature, conductivity, ph
    return None, None, None, None

# MQTT callback functions
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT Broker!")
        client.subscribe(MQTT_CONTROL_TOPIC)
    else:
        print("Failed to connect, return code %d\n", rc)

def on_message(client, userdata, msg):
    print(f"Received message: {msg.payload.decode()} from topic: {msg.topic}")
    if msg.payload.decode() == '["water_on"]':
        set_gpio_value(GPIO5, "0")
    elif msg.payload.decode() == '["water_off"]':
        set_gpio_value(GPIO5, "1")

# Main function
def main():
    ser = ttl_init(SERIAL_PORT, BAUD_RATE)
    ttl_open(ser)

    gpio_init(GPIO5)
    set_gpio_direction(GPIO5, "out")

    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_start()

    try:
        while True:
            humidity, temperature, conductivity, ph = read_soil_data(ser)
            if humidity is not None:
                print(f"Humidity: {humidity} %")
                print(f"Temperature: {temperature} °C")
                print(f"Conductivity: {conductivity} uS/cm")
                print(f"PH: {ph}")

                payload = json.dumps([
                    {"Soiltemp": str(temperature)},
                    {"Soilhum": str(humidity)},
                    {"Ph": str(ph)}
                ])
                
                client.publish(MQTT_TOPIC, payload)
                print(f"Published to {MQTT_TOPIC}: {payload}")
            else:
                print("无法读取土壤数据")
            time.sleep(1)
    except KeyboardInterrupt:
        ttl_close(ser)
        client.loop_stop()
        client.disconnect()
        print("程序已退出")

if __name__ == "__main__":
    main()
