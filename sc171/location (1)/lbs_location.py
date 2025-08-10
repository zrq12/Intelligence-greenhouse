import android
import requests
# 创建与Android设备连接的对象
droid = android.Android()
# 获取运营商编号
operator_code = droid.getNetworkOperator()
#print(operator_code)
MCCMNC = operator_code.result
if MCCMNC != "":
    #print(MCCMNC)
    #print(type(MCCMNC))
    MCC = MCCMNC[:3]
    MNC = MCCMNC[-2:]
    print("MCC:"+MCC)
    print("MCC:"+MNC)
    CellLocation = droid.getCellLocation()
    LACCID = CellLocation.result
    LAC = LACCID['lac']
    CID = LACCID['cid']
    print("LAC:"+str(LAC))
    print("CID:"+str(CID))

    #连接到基站定位接口
    data = {
    'mcc': MCC,
    'mnc': MNC,
    'lac':str(LAC),
    'ci':str(CID)
    }
    response = requests.get("http://api.cellocation.com:84/cell/get", params=data)
    #print(response.text)
    result = response.text.replace("{", "")
    result = result.replace("}", "")
    result = result.split(",")
    lat = result[1][9:-1]
    lon = result[2][9:-1]
    address = result[4][13:-1]
    print(lat)
    print(lon)
    print(address)
else:
    lat = "NA"
    lon = "NA"
    address = "NA"
    print("lat:"+lat)
    print("lon:"+lon)
    print("address:"+address)