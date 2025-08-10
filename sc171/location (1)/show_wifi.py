import cv2
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from cvs import *
from PIL import Image
from camera_android import *
from main import wifi_formatted_address
import textwrap
import os
from wifi_location import latitude,longitude

 
def cv2ImgAddText(img, text, left, top, textColor=(255, 255, 255), textSize=20):
    if (isinstance(img, np.ndarray)):  # 判断是否OpenCV图片类型
        img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    # 创建一个可以在给定图像上绘图的对象
    draw = ImageDraw.Draw(img)
    # 字体的格式
    fontStyle = ImageFont.truetype(
        "FZCuDengXian.ttf", textSize, encoding="utf-8")
    # 绘制文本
    draw.text((left, top), text, textColor, font=fontStyle)
    # 转换回OpenCV格式
    return cv2.cvtColor(np.asarray(img), cv2.COLOR_RGB2BGR)

#WIFI定位数据
img = cv2ImgAddText(cv2.imread(imageFn), "WIFI定位", 200, 200, ( 0 , 0 , 255), 150)
img = cv2ImgAddText(img, "经度:"+str(longitude), 200, 400, ( 255 , 255 , 255), 150)
img = cv2ImgAddText(img, "纬度:"+str(latitude), 200, 600, (255 , 255 , 255), 150)

img = cv2ImgAddText(img, "GIS:", 200, 800, (255 , 255 , 255), 150)
# 获取用户输入的值
text1 = wifi_formatted_address
# 指定每行的宽度
width = 18
# 调用wrap方法进行自动换行
wrapped_text = textwrap.wrap(text1, width)
# 打印自动换行后的结果
top1 = 1000
for line in wrapped_text:
    img = cv2ImgAddText(img, line, 200, top1, (255 , 255 , 255), 150)
    top1 = top1 + 200
    print(line)
#img = cv2ImgAddText(img, wrapped_text, 200, 1000, (255 , 255 , 255), 200)
#cv2.imshow('show', img)
#cv2.waitkey(0)

#保存图片并展示
cv2.imwrite("blended1.jpg",img)                          
cap = cvs.VideoCapture("blended1.jpg")
blended1 = cap.read()
cap.imshow(blended1)
os.remove("blended1.jpg")