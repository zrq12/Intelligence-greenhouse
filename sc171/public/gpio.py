#-*- coding: utf-8 -*-
from ctypes import *
import platform
from librockmong import *
from usb_device import *

#初始化引脚工作模式
#SerialNumber: 设备序号
#Pin：引脚编号。0，P0. 1, P1...
#Mode：输入输出模式。0，输入。1，输出。2，开漏
#Pull：上拉下拉电阻。0，无。1，使能内部上拉。2，使能内部下拉
#函数返回：0，正常；<0，异常
def IO_InitPin(SerialNumber, Pin, Mode, Pull):
    return librockmong.IO_InitPin(SerialNumber, Pin, Mode, Pull)
    
#读取引脚状态
#SerialNumber: 设备序号
#Pin：引脚编号。0，P0. 1, P1...
#PinState：返回引脚状态。0，低电平。1，高电平
#函数返回：0，正常；<0，异常
def IO_ReadPin(SerialNumber, Pin, PinState):
    return librockmong.IO_ReadPin(SerialNumber, Pin, PinState)

#控制引脚输出状态
#SerialNumber: 设备序号
#Pin：引脚编号。0，P0. 1, P1...
#PinState：引脚状态。0，低电平。1，高电平
#函数返回：0，正常；<0，异常
def IO_WritePin(SerialNumber, Pin, PinState):
    return librockmong.IO_WritePin(SerialNumber, Pin, PinState)

class IO_InitMulti_TxStruct_t(Structure):  
	_fields_ = [
		("Pin", c_ubyte),	# 引脚编号
		("Mode", c_ubyte),	# 模式：0，输入；1，输出
		("Pull", c_ubyte)
	]

class IO_InitMulti_RxStruct_t(Structure):  
	_fields_ = [
		("Ret", c_ubyte),	# 返回
	]
    
def IO_InitMultiPin(SerialNumber, TxStruct, RxStruct, Number):
    return librockmong.IO_InitMultiPin(SerialNumber, TxStruct, RxStruct, Number)


class IO_ReadMulti_TxStruct_t(Structure):  
	_fields_ = [
		("Pin", c_ubyte),	# 引脚编号
	]

class IO_ReadMulti_RxStruct_t(Structure):  
	_fields_ = [
		("Ret", c_ubyte),		# 返回
		("PinState", c_ubyte),	# 引脚状态
	]
    
def IO_ReadMultiPin(SerialNumber, TxStruct, RxStruct, Number):
    return librockmong.IO_ReadMultiPin(SerialNumber, TxStruct, RxStruct, Number)


class IO_WriteMulti_TxStruct_t(Structure):  
	_fields_ = [
		("Pin", c_ubyte),		# 引脚编号
		("PinState", c_ubyte),	# 引脚状态
	]

class IO_WriteMulti_RxStruct_t(Structure):  
	_fields_ = [
		("Ret", c_ubyte),	#返回
	]
    
def IO_WriteMultiPin(SerialNumber, TxStruct, RxStruct, Number):
    return librockmong.IO_WriteMultiPin(SerialNumber, TxStruct, RxStruct, Number)

#自己测试用
if __name__ == '__main__':
    SerialNumbers = 240713014
    sn = 0
    # Scan device
    ret = UsbDevice_Scan(byref(SerialNumbers))
    if (0 > ret):
        print("Error: %d"%ret)
        exit()
    elif(ret == 0):
        print("No device!")
        exit()
    else:
        for i in range(ret):
            print("Dev%d SN: %d"%(i, SerialNumbers[i]))
    
    sn = SerialNumbers[0]#选择设备0

    #初始化P0为输出模式
    ret = IO_InitPin(sn, 0, 1, 0)
    if (0 > ret):
        print("error: %d"%ret)
    
    #控制P0输出高电平
    ret = IO_WritePin(sn, 0, 1)
    if (0 > ret):
        print("error: %d"%ret)
    
    #控制P0输出低电平
    ret = IO_WritePin(sn, 0, 0)
    if (0 > ret):
        print("error: %d"%ret)

    #初始化P0为输入模式
    ret = IO_InitPin(sn, 0, 0, 0)
    if (0 > ret):
        print("error: %d"%ret)
        
    #读取P0电平状态
    PinState = (c_int)()
    ret = IO_ReadPin(sn, 0, byref(PinState))
    if (0 > ret):
        print("error: %d"%ret)
    print("Pin state: %d"%PinState.value)
