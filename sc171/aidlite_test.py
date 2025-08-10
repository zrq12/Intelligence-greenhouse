
import cv2
import numpy as np
import paho.mqtt.client as mqtt
import base64
import aidlite
from datetime import datetime

def preprocess(source_img):
    shape = source_img.shape[:2]
    r = min(640 / shape[0], 640 / shape[1])
    r = min(r, 1.0)
    new_unpad = int(round(shape[1] * r)), int(round(shape[0] * r))
    dw, dh = 640 - new_unpad[0], 640 - new_unpad[1]
    dw /= 2
    dh /= 2
    img = source_img
    if shape[::-1] != new_unpad:
        img = cv2.resize(source_img, new_unpad, interpolation=cv2.INTER_LINEAR)
    top, bottom = int(round(dh - 0.1)), int(round(dh + 0.1))
    left, right = int(round(dw - 0.1)), int(round(dw + 0.1))
    img = cv2.copyMakeBorder(img, top, bottom, left, right, cv2.BORDER_CONSTANT, value=(114, 114, 114))
    image = [img]
    image = np.stack(image)
    image = image[..., ::-1].transpose((0, 3, 1, 2))
    image = np.ascontiguousarray(image)
    image = image.astype(np.float32)
    image = image / 255
    image = image.transpose((0, 2, 3, 1))
    return image

def aidlite_invoke(model_path, model_class, image):
    model = aidlite.Model.create_instance(model_path)
    input_shapes = [[1,640,640,3]]
    output_shapes = [[1,4+model_class,8400]]
    model.set_model_properties(input_shapes, aidlite.DataType.TYPE_FLOAT32, output_shapes, aidlite.DataType.TYPE_FLOAT32)
    config = aidlite.Config.create_instance()
    config.framework_type = aidlite.FrameworkType.TYPE_TFLITE
    config.accelerate_type = aidlite.AccelerateType.TYPE_GPU
    fast_interpreter = aidlite.InterpreterBuilder.build_interpretper_from_model_and_config(model, config)
    fast_interpreter.init()
    fast_interpreter.load_model()
    fast_interpreter.set_input_tensor(0, image)
    fast_interpreter.invoke()
    output_data = fast_interpreter.get_output_tensor(0)
    output = output_data.copy()
    output = output.reshape(1,4+model_class,8400)
    output[:,:4] *=640
    fast_interpreter.destory()
    return output

def postprocess(source_img, output, conf, iou, draw, classes):
    shape = source_img.shape[:2]
    boxes, scores, class_ids = [], [], []
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
    if len(indices) > 0:
        indices = indices.flatten()
    for i in indices:
        box = boxes[i]
        gain = min(640 / shape[1], 640 / shape[0])
        pad = (round((640 - shape[1] * gain) / 2 - 0.1), round((640 - shape[0] * gain) / 2 - 0.1))
        box[0] = (box[0] - pad[0]) / gain
        box[1] = (box[1] - pad[1]) / gain
        box[2] = box[2] / gain
        box[3] = box[3] / gain
        score = scores[i]
        class_id = class_ids[i]
        if draw:
            draw_detections(source_img, box, score, class_id, classes)
    return boxes, scores, class_ids, source_img

def draw_detections(source_img, box, score, class_id, classes):
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
    class_names = ['normal_corn', 'broken_corn']
    model_path = 'best_float32.tflite'
    mqtt_client = mqtt.Client()
    mqtt_client.connect("114.55.89.187", 1883, 60)
    cap = cv2.VideoCapture(2)

    while True:
        ret, frame = cap.read()
        if not ret:
            break
        pred_input = preprocess(frame)
        output = aidlite_invoke(model_path=model_path, model_class=len(class_names), image=pred_input)
        boxes, scores, class_ids, result_img = postprocess(frame, output, conf=0.3, iou=0.5, draw=True, classes=class_names)

        ret, buffer = cv2.imencode('.jpg', result_img)
        frame_bytes = buffer.tobytes()
        frame_base64 = base64.b64encode(frame_bytes).decode('utf-8')

        mqtt_client.publish("camera/stream", frame_base64)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()
    mqtt_client.disconnect()

if __name__ == "__main__":
    main()
