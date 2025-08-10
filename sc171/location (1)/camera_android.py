#*拍摄需要识别的数字照片*
import android   #调用安卓库
#import cv2

#调用安卓的相机，并拍摄照片存储至位置：/sdcard/myphoto.jpg
droid = android.Android()
imageFn = '/sdcard/myphoto.jpg'
droid.cameraInteractiveCapturePicture(imageFn)
#img = cv2.imread(imageFn)
