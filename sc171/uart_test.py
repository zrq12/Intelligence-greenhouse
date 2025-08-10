import serial

# 打开串口
ser = serial.Serial('/dev/ttyHS1', 9600)
print("串口已打开")

# 设置串口参数
ser.baudrate = 9600
ser.bytesize = serial.EIGHTBITS
ser.parity = serial.PARITY_NONE
ser.stopbits = serial.STOPBITS_ONE
print("串口参数已设置")

'''
input_data = input("请输入要发送的数据：")
ser.write(input_data.encode())
print("成功发送数据")
data = ser.read()    #读取一个字符，若data = ser.read(20)则是读取20个字符
print("接收到数据:", data.decode())
'''

# 循环接收数据
while True:

    # 从键盘读取输入
    input_data = input("请输入要发送的数据：")+'\n'
    # 向串口发送数据
    ser.write(input_data.encode())

    data = ser.readline()
    if data:
        print("接收到数据:", data.decode())

    # 如果输入q，则退出循环
    if input_data == 'q\n':
        break 

# 关闭串口
ser.close()
print("串口已关闭")
