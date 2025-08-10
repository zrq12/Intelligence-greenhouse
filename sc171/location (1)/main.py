import requests
from gnss_location import longitude,latitude
import wifi_location


def geocode(location):
    # 参数内容 可以写成字典的格式
    parameters = {'output': 'json', 'key': '33e1bce41f85b8969f71e66132b35a43', 'location': location,
                  'extensions': 'all'}
    # 问号以前的内容
    base = 'http://restapi.amap.com/v3/geocode/regeo'
    response = requests.get(base, parameters)
    #print('HTTP 请求的状态: %s' % response.status_code)
    return response.json()


#loc = '113.5629337,22.3435249'
if longitude == "NA":
    formatted_address = "NA"
    print(formatted_address)
else:
    loc = str(longitude) + ',' + str(latitude)
    data = geocode(loc)  # 获取的数据类型为dict
    formatted_address = data['regeocode']['formatted_address']
    print(formatted_address)

if wifi_location.longitude == "NA":
    wifi_formatted_address = "NA"
    print(wifi_formatted_address)
else:
    loc = str(wifi_location.longitude) + ',' + str(wifi_location.latitude)
    data = geocode(loc)  # 获取的数据类型为dict
    wifi_formatted_address = data['regeocode']['formatted_address']
    print(wifi_formatted_address)

