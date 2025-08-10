import aidlite
import cv2
import numpy as np

def preprocess(source_img):
    # 缩放填充至640*640
    shape = source_img.shape[:2]
    r = min(640 / shape[0], 640 / shape[1])
    r = min(r, 1.0)
    ratio = r, r
    new_unpad = int(round(shape[1] * r)), int(round(shape[0] * r))
    dw, dh = 640 - new_unpad[0], 640 - new_unpad[1]
    dw /= 2
    dh /= 2
    if shape[::-1] != new_unpad:  # resize
        img = cv2.resize(source_img, new_unpad, interpolation=cv2.INTER_LINEAR)
    top, bottom = int(round(dh - 0.1)) , int(round(dh + 0.1))
    left, right = int(round(dw - 0.1)) , int(round(dw + 0.1))
    img = cv2.copyMakeBorder(img, top, bottom, left, right, cv2.BORDER_CONSTANT, value=(114, 114, 114))
    # 转为输入向量
    image = [img]
    image = np.stack(image)
    image = image[..., ::-1].transpose((0, 3, 1, 2))
    image = np.ascontiguousarray(image)
    image = image.astype(np.float32)
    image = image / 255
    image = image.transpose((0, 2, 3, 1))
    return image

def aidlite_invoke(model_path, model_class, image):
    # 初始化模型
    model = aidlite.Model.create_instance(model_path)
    input_shapes = [[1,640,640,3]]
    output_shapes = [[1,4+model_class,8400]]
    model.set_model_properties(input_shapes, aidlite.DataType.TYPE_FLOAT32, output_shapes, aidlite.DataType.TYPE_FLOAT32)
    config = aidlite.Config.create_instance()
    config.framework_type = aidlite.FrameworkType.TYPE_TFLITE
    config.accelerate_type = aidlite.AccelerateType.TYPE_GPU
    fast_interpreter = aidlite.InterpreterBuilder.build_interpretper_from_model_and_config(model, config)
    # 模型推理
    result = fast_interpreter.init()
    result = fast_interpreter.load_model()
    result = fast_interpreter.set_input_tensor(0, image)
    result = fast_interpreter.invoke()
    output_data = fast_interpreter.get_output_tensor(0)
    output = output_data.copy()
    output = output.reshape(1,4+model_class,8400)
    output[:,:4] *=640
    result = fast_interpreter.destory()
    return output

def postprocess(source_img,output,conf,iou,draw,classes):
    shape = source_img.shape[:2]
    boxes = []
    scores = []
    class_ids = []
    for pred in output:
        pred = np.transpose(pred)
        for box in pred:
            x, y, w, h = box[:4]
            x1 = x - w / 2
            y1 = y - h / 2
            boxes.append([x1, y1, w, h])
            idx = np.argmax(box[4:])
            scores.append(box[idx + 4])
            class_ids.append(idx)
    indices = cv2.dnn.NMSBoxes(boxes, scores, conf, iou)
    for i in indices:
        box = boxes[i]
        gain = min(640 / shape[1], 640 / shape[0])
        pad = (
            round((640 - shape[1] * gain) / 2 - 0.1),
            round((640 - shape[0] * gain) / 2 - 0.1),
        )
        box[0] = (box[0] - pad[0]) / gain
        box[1] = (box[1] - pad[1]) / gain
        box[2] = box[2] / gain
        box[3] = box[3] / gain
        score = scores[i]
        class_id = class_ids[i]
        print(box, score, class_id)
        if draw is True:
            draw_detections(source_img,box,score,class_id,classes)
    cv2.imwrite('test1.jpg',source_img)
    return boxes,scores,class_ids,source_img
            
def draw_detections(source_img,box,score,class_id,classes):            
    color_palette = np.random.uniform(0, 255, size=(len(classes), 3))
    x1, y1, w, h = box
    color = color_palette[class_id]
    cv2.rectangle(source_img, (int(x1), int(y1)), (int(x1 + w), int(y1 + h)), color, 2)
    label = f"{classes[class_id]}: {score:.2f}"
    (label_width, label_height), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1)
    label_x = x1
    label_y = y1 - 10 if y1 - 10 > label_height else y1 + 10
    cv2.rectangle(
        source_img,
        (int(label_x), int(label_y - label_height)),
        (int(label_x + label_width), int(label_y + label_height)),
        color,
        cv2.FILLED,
    )
    cv2.putText(source_img, label, (int(label_x), int(label_y)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1, cv2.LINE_AA)

def main():
    class_name = ['normal_corn','broken_corn']
    img = cv2.imread('22.jpg')
    pred_input = preprocess(img)
    output = aidlite_invoke(model_path='best_float32.tflite',model_class=len(class_name),image=pred_input)
    boxes,scores,class_ids,result_img = postprocess(img,output,conf=0.3,iou=0.5,draw=True,classes=class_name)

if __name__ == "__main__":
    main()
