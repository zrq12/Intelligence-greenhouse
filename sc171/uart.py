import serial

def hex_to_bytes(hex_str):              # 将16进制字符串转换为字节
    return bytes.fromhex(hex_str)

def bytes_to_hex(byte_data):            # 将字节数据转换为16进制字符串
    return byte_data.hex()

def ttl_init(port, baud):
    ser = serial.Serial(port, baud)
    ser.bytesize = serial.EIGHTBITS
    ser.parity = serial.PARITY_NONE
    ser.stopbits = serial.STOPBITS_ONE
    print("串口已设置/n")
    return ser

def ttl_open(ser):
    if ser:
        if not ser.isOpen():
            ser.open()
        print("串口已打开/n")
    else:
        print("串口未设置/n")

def ttl_close(ser):
    if ser:
        if ser.isOpen():
            ser.close()
        print("串口已关闭/n")
    else:
        print("串口未设置/n")
def ttl_decode(rtx):
    hex_values = []      
    for i in range(len(rtx)):
        byte = rtx[i:i+1]
        byte_hex = bytes_to_hex(byte)
        hex_values.append(byte_hex)
    return hex_values

def check(msg):
    data_bits = int(msg[1],16)+int(msg[2],16)+int(msg[3],16)+int(msg[4],16)+int(msg[5],16)+int(msg[6],16)
    check_bits = 256*int(msg[7],16)+int(msg[8],16)
    if data_bits==check_bits:
        weight = 65536*int(msg[4],16)+256*int(msg[5],16)+int(msg[6],16)
        if int(msg[3],16)==0:
            return weight
        else:
            return -weight
    else:
        return "notpass" 

def zero_calibration(ser):               #零点校验
    if ser:    
        while True:
            send_data = "AA00A9ABA8"
            send_bytes = hex_to_bytes(send_data)
            ser.write(send_bytes)
            rtx = ser.read(10)
            msg = ttl_decode(rtx)
            weight = check(msg)
            if weight!="notpass":
                return weight
                break
            else:
                print("校验不通过/n")
    else:
        return "notset"
def weight_calibration(ser,weight0):     #砝码校验
    if ser: 
        dat1 = weight0*100//256
        dat2 = weight0*100%256
        yy = int("AD",16)^int("00",16)^dat1^dat2
        hex_dat1 = format(dat1,'02x')
        hex_dat2 = format(dat2,'02x')
        hex_yy = format(yy,'02x')
        while True:
            send_data = "AD00"+hex_dat1+hex_dat2+hex_yy
            send_bytes = hex_to_bytes(send_data)
            ser.write(send_bytes)
            rtx = ser.read(10)
            msg = ttl_decode(rtx)
            weight = check(msg)
            if weight!="notpass":
                return weight
                break
            else:
                print("校验不通过/n")
    else:
        return "notset"

def temp_clear(ser):                     #去皮
    if ser:     
        while True:
            send_data = "AB00AAACAD"
            send_bytes = hex_to_bytes(send_data)
            ser.write(send_bytes)
            rtx = ser.read(10)
            msg = ttl_decode(rtx)
            weight = check(msg)
            if weight!="notpass":
                return weight
                break
            else:
                print("校验不通过/n")
    else:
        return "notset"

def temp_clear_recover(ser):             #取消去皮
    if ser: 
        while True:
            send_data = "AC00ABADAA"
            send_bytes = hex_to_bytes(send_data)
            ser.write(send_bytes)
            rtx = ser.read(10)
            msg = ttl_decode(rtx)
            weight = check(msg)
            if weight!="notpass":
                return weight
                break
            else:
                print("校验不通过/n")
    else:
        return "notset"
def read_weight(ser):
    if ser:
        while True:
            send_data = "A300A2A4A5"
            send_bytes = hex_to_bytes(send_data)
            ser.write(send_bytes)
            rtx = ser.read(10)
            msg = ttl_decode(rtx)
            weight = check(msg)
            if weight!="notpass":
                return weight
                break
            else:
                print("校验不通过/n")
    else:
        return "notset"

def main():
    ser = None
    weight = None
    while True:
        command = input("请输入指令:")
        if command == "设置串口":
            ser = ttl_init('/dev/ttyHS1',9600)
        elif command == "打开串口":
            ttl_open(ser)
        elif command == "关闭串口":
            ttl_close(ser)
        elif command == "零点校验":
            weight = zero_calibration(ser)
        elif command == "砝码校验":
            weight0 = input("请输入砝码重量(g):")
            weight = weight_calibration(ser,int(weight0))
        elif command == "去皮":
            weight = temp_clear(ser)
        elif command == "去皮恢复":
            weight = temp_clear_recover(ser)
        elif command == "称重":
            weight = read_weight(ser)
        elif command == "退出":
            ttl_close(ser)
            break
        else:
            print("无此指令/n")
        if weight is not None:
            if weight!="notset":
                print("重量为"+str(weight/100)+"g/n")
            else:
                print("串口未设置/n")
            weight = None

if __name__ == "__main__":
    main()