import sys
import socket
import time
import cv2
from imutils.video import VideoStream
import imagezmq

class streamImages:

    reply = ""
    recv = False
    i = 1
    sender = None
    rpi_name = None
    jpeg_quality = None
    picam = None

    def connect_to_pc(self):
        # use either of the formats below to specifiy address of display computer
        self.sender = imagezmq.ImageSender(connect_to='tcp://192.168.17.20:5555')
        # sender = imagezmq.ImageSender(connect_to='tcp://192.168.1.190:5555')

        self.rpi_name = socket.gethostname()  # send RPi hostname with each image
        self.picam = VideoStream(usePiCamera=True, resolution=(640, 640)).start()
        time.sleep(2.0)  # allow camera sensor to warm up
        self.jpeg_quality = 95  # 0 to 100, higher is better quality, 95 is cv2 default
        #while True:  # send images as stream until Ctrl-C

    def startStreaming(self):
            self.recv = False
            image = self.picam.read()
            print("snappin " + str(self.i))
            self.i +=1
            ret_code, jpg_buffer = cv2.imencode(
                ".jpg", image, [int(cv2.IMWRITE_JPEG_QUALITY), self.jpeg_quality])
            self.reply = self.sender.send_jpg(self.rpi_name, jpg_buffer)
            print(self.reply)
            return self.reply
            #while self.recv == False:
                #print("waiting")
                #self.reply = self.sender.send_jpg(self.rpi_name, self.jpg_buffer)
                #if int(self.reply) >=0:
                    #self.recv = True

    #def keepSnapping(self):