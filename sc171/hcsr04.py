import subprocess
from time import sleep
import time

GPIO3 = "488"
GPIO4 = "489"
GPIO5 = "432"
GPIO6 = "433"
GPIO7 = "436"
GPIO8 = "434"
GPIO9 = "435"
GPIO10 = "385"

def adb_cmd(cmd):
    result = subprocess.run(['adb','shell',cmd],capture_output=True,text=True)
    output = result.stdout.strip()
    return output

def gpio_init(gpio):
    adb_cmd('echo '+gpio+' > /sys/class/gpio/export') 
    
def check_gpio_direction(gpio):
    output = adb_cmd('cat /sys/class/gpio/gpio'+gpio+'/direction') 
    print(output)

def set_gpio_direction(gpio,direction):
    adb_cmd('echo '+direction+' > /sys/class/gpio/gpio'+gpio+'/direction')
    check_gpio_direction(gpio)

def check_gpio_value(gpio):
    output = adb_cmd('cat /sys/class/gpio/gpio'+gpio+'/value')
    print(output)

def set_gpio_value(gpio,value):
    adb_cmd('echo '+value+' > sys/class/gpio/gpio'+gpio+'/value')
    check_gpio_value(gpio)

def delayMicrosecond(t):    # 微秒级延时函数
    start,end=0,0           # 声明变量
    start=time.time()       # 记录开始时间
    t=(t-3)/1000000     # 将输入t的单位转换为秒，-3是时间补偿
    while end-start<t:  # 循环至时间差值大于或等于设定值时
        end=time.time()     # 记录结束时间

def main():
    trigPin = GPIO3
    echoPin = GPIO4
    gpio_init(trigPin)
    gpio_init(echoPin)
    set_gpio_direction(trigPin,"out")
    set_gpio_direction(echoPin,"in")
    
    set_gpio_value(trigPin,"0")
    delayMicrosecond(5)
    set_gpio_value(trigPin,"1")
    delayMicrosecond(10)
    set_gpio_value(trigPin,"0")

if __name__ == "__main__":
    main()