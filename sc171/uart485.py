import serial
import time

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

def main():
    ser = ttl_init('/dev/ttyHS1', 4800)
    ttl_open(ser)
    
    try:
        while True:
            humidity, temperature, conductivity, ph = read_soil_data(ser)
            if humidity is not None:
                print(f"Humidity: {humidity} %")
                print(f"Temperature: {temperature} °C")
                print(f"Conductivity: {conductivity} uS/cm")
                print(f"PH: {ph}")
            else:
                print("无法读取土壤数据")
            time.sleep(1)
    except KeyboardInterrupt:
        ttl_close(ser)
        print("程序已退出")

if __name__ == "__main__":
    main()
