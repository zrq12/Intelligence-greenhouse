import subprocess
from time import sleep

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

def main():
    
        gpio_init(GPIO5)
        set_gpio_direction(GPIO5,"out")
        sleep(1)
        set_gpio_value(GPIO5,"0")
        sleep(10)
        set_gpio_value(GPIO5,"1")

if __name__ == "__main__":
    main()