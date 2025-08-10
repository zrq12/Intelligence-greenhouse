import android
import datetime

droid=android.Android()
droid.startLocating(10000,30)
#print(droid.readLocation())
droid.stopLocating()
LKlocation = droid.getLastKnownLocation()
#print(type(LKlocation))
gpslocation = LKlocation.result
#print(gpslocation)
#print(type(gpslocation))
gps = gpslocation.get('gps')
#print(gps)
#print(type(gps))
if gps == None:
    latitude = "NA"
    longitude = "NA"
    altitude = "NA"
    formatted_date = "NA"
    accuracy = "NA"
    speed = "NA"
    bearing = "NA"
    print(latitude)
    print(longitude)
else:
    latitude = gps.get('latitude')
    longitude = gps.get('longitude')
    altitude = gps.get('altitude')
    time = gps.get('time')
    date_format = "%Y-%m-%d %H:%M:%S" # 设置日期格式
    # 将时间戳除以1000得到秒数
    seconds = time / 1000.0
    # 创建datetime对象并进行格式化输出
    dt = datetime.datetime.fromtimestamp(seconds)
    formatted_date = dt.strftime(date_format)
    accuracy = gps.get('accuracy')
    speed = gps.get('speed')
    bearing = gps.get('bearing')
    print("latitude:"+str(latitude))
    print("longitude:"+str(longitude))
    print("altitude:"+str(altitude))
    print("time:"+formatted_date)
    print("accuracy:"+str(accuracy))
    print("speed:"+str(speed))
    print("bearing:"+str(bearing))
    #print(type(latitude))
    #print(str(latitude)+","+str(longitude))
