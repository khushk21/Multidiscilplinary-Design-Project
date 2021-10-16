import serial
import time
import threading
global ser

class STMConnection():

    def thread_recv(self):
        global ser
        ser=serial.Serial('/dev/ttyUSB0',115200,timeout=0.5)
        read = ser.readall()
        if len(read) > 0:
            #print(read)
            return(read.decode())

    def thread_send(self,text):
        global ser
        #while True:
        ser=serial.Serial('/dev/ttyUSB0',115200,timeout=0.5)
        ser.write(text)
        #print(ser.readline())
        time.sleep(0.5)

    def connect_STM(self):
        #global ser
        ser=serial.Serial('/dev/ttyUSB0',115200,timeout=0.5)
        print("Connected to STM")
        #recv_data = threading.Thread(target=thread_recv)
        #send_data = threading.Thread(target=thread_send)
        return ser
        #send_data.start()

    def listen_and_write(self):
        while True:
            self.thread_recv()
            #self.thread_send(input(),stm)