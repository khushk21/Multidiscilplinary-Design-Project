# This is a sample Python script.
#import socket
import time
import numpy as np
from matplotlib import pyplot as plt
from numpy import asarray
import os
import imagezmq
import cv2
import pandas as pd
import torch
from PIL import Image
# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.

# def stream():
#     image_hub = imagezmq.ImageHub()
#     while True:  # show streamed images until Ctrl-C
#         rpi_name, image = image_hub.recv_image()
#         cv2.imshow(rpi_name, image)  # 1 window for each RPi
#         cv2.waitKey(1)
#         image_hub.send_reply(b'OK')

class ImageRec:

    blank = cv2.imread("C:/yolov5/yolov5/detected_images/blank.jpg")
    blank = cv2.resize(blank, dsize=(0, 0),
                        fx=0.5, fy=0.5)
    image_list = [blank] * 6
    counter = 0

    def concat_vh(self, list_2d):
        # return final image
        return cv2.vconcat([cv2.hconcat(list_h)
                            for list_h in list_2d])

    def test(self):
        img_tiles = None
        img_dict = {}
        img_path = "C:/yolov5/yolov5/detected_images/"
        image_hub = imagezmq.ImageHub()
        model = torch.hub.load('C:/yolov5/yolov5/', 'custom', path='C:/yolov5/yolov5/yyds_promax.pt',
                               source='local', force_reload=True)  # or yolov5m, yolov5l, yolov5x, custom
        model.conf = 0.8
        model.max_det = 1
        counter = 0
        # print(str(self.image_list))
        while True:  # show streamed images until Ctrl-C
            rpi_name, jpg_buffer = image_hub.recv_jpg()
            image = cv2.imdecode(np.frombuffer(jpg_buffer, dtype='uint8'), -1)
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            # Inference
            results = model(image)
            # Results
            results.print()  # or .show(), .save(), .crop(), .pandas(), etc.

            res_list = pd.read_json(results.pandas().xyxy[0].to_json(orient="columns", index="false"))
            #print(res_list.values.to_list())
            print(res_list)
            if len(res_list):

                pic_id = str(res_list["name"][0])

                for i in range(len(results.xyxy[0])):
                    #print(res_list["confidence"][i])
                    conf = float(res_list["confidence"][i])
                    #print(conf)
                    print(res_list['ymax'][i])
                    print(res_list['ymin'][i])
                    picture_id = int(res_list["name"][i])
                    if picture_id not in img_dict:
                        img_dict[picture_id] = conf
                        #image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                        #roi = image[int(res_list['ymin'][i]):int(res_list['ymax'][i]), int(res_list['xmin'][i]):int(res_list['xmax'][i])]   #for bounding box only, comment out if want whole picture
                        #cv2.imwrite(img_path + str(picture_id) +".jpg", image)
                        results.save("C:/yolov5/yolov5/detected_images")
                        #s.remove(img_path + str(picture_id) + ".jpg")
                        os.rename(img_path + "image0.jpg", img_path + str(picture_id) +".jpg")
                        #print(int(res_list['xmax'][i])-int(res_list['xmin'][i]))
                    elif conf > img_dict[picture_id]:
                        pass
                        #image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                        #roi = image[int(res_list['ymin'][i]):int(res_list['ymax'][i]), int(res_list['xmin'][i]):int(res_list['xmax'][i])]   #for bounding box only, comment out if want whole picture
                        #cv2.imwrite(img_path + str(picture_id) +".jpg", image)
                        # results.save("C:/yolov5/yolov5/detected_images")
                        # os.remove(img_path + str(picture_id) + ".jpg")
                        # os.rename(img_path + "image0.jpg", img_path + str(picture_id) + ".jpg")
                        #print(int(res_list['xmax'][i]) - int(res_list['xmin'][i]))
                    print(int(res_list['xmax'][i]) - int(res_list['xmin'][i]))
                    # if int(220-(int(res_list['ymax'][i]) - int(res_list['ymin'][i]))) >220:
                    #     print("dist is <=15")
                    # else:
                    #     print("distance is " + str(int((220-(int(res_list['ymax'][i]) - int(res_list['ymin'][i]))-5)/3)))
                    dist = int(2280/(int(res_list['xmax'][i]) - int(res_list['xmin'][i])))
                    print(conf)
                    print(picture_id)
                    print("Dist is " + str(dist))



            else:
                pic_id = "0"
                results.save("C:/yolov5/yolov5/detected_images")
                if os.path.isfile(img_path + str(pic_id) + ".jpg"):
                    os.remove(img_path + str(pic_id) + ".jpg")
                os.rename(img_path + "image0.jpg", img_path + str(pic_id) + ".jpg")
            results2 = cv2.imread(img_path + str(pic_id) + ".jpg")
            result2 = cv2.resize(results2, dsize=(0, 0),
                                 fx=0.5, fy=0.5)
            self.image_list[counter] = result2

            img_tile = self.concat_vh([[self.image_list[0], self.image_list[1], self.image_list[2]],
                                       [self.image_list[3], self.image_list[4], self.image_list[5]]])

            cv2.imwrite("C:/yolov5/yolov5/detected_images/result.png", img_tile)
            img_tile = cv2.cvtColor(img_tile, cv2.COLOR_BGR2RGB)
            img_tiles = Image.fromarray(img_tile)

            img_tiles.show()
            counter += 1

            image_hub.send_reply(pic_id.encode('utf-8'))

        cv2.waitKey(1)


    def main(self):

        self.test()


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    start = ImageRec()
    start.main()

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
