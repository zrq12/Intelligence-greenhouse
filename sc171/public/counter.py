#-*- coding: utf-8 -*-
from ctypes import *
import platform
from librockmong import *

#计数器初始化
#SerialNumber: 设备序号
#Channel：通道编号。0，1，...
#Mode：计数模式。0，上升沿计数。1，下降沿。2，双边沿
#Dirction：计数模式。0，计数加加。1，计数减。
#Pull：引脚上拉下拉电阻。0，无。1，使能内部上拉。2，使能内部下拉
#函数返回：0，正常；<0，异常
def Counter_Init(SerialNumber, Channel, Mode, Dirction, Pull):
    return librockmong.Counter_Init(SerialNumber, Channel, Mode, Dirction, Pull)

#计数器开始计数
#SerialNumber: 设备序号
#Channel：通道编号。0，1，...
#函数返回：0，正常；<0，异常
def Counter_Start(SerialNumber, Channel):
    return librockmong.Counter_Start(SerialNumber, Channel)

#计数器停止计数
#SerialNumber: 设备序号
#Channel：通道编号。0，1，...
#函数返回：0，正常；<0，异常
def Counter_Stop(SerialNumber, Channel):
    return librockmong.Counter_Stop(SerialNumber, Channel)

#计数器读数值
#SerialNumber: 设备序号
#Channel：通道编号。0，1，...
#Value：返回读取的数值。
#函数返回：0，正常；<0，异常
def Counter_Read(SerialNumber, Channel, Value):
    return librockmong.Counter_Read(SerialNumber, Channel, Value)

#计数器写数值
#SerialNumber: 设备序号
#Channel：通道编号。0，1，...
#Value：写入的数值。
#函数返回：0，正常；<0，异常
def Counter_Write(SerialNumber, Channel, Value):
    return librockmong.Counter_Write(SerialNumber, Channel, Value)
    

