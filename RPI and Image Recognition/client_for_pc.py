import socket
import time
from STM_connection import STMConnection
from test_send_img import streamImages


class PC_Comm:

    imgDet = streamImages()
    stm = None
    android = None
    obstacle_list = []

    def connect_PC(self):
        self.imgDet.connect_to_pc()
        self.s = socket.socket()
        host = '192.168.17.28'# ip 192.168.17.20
        port = 5005
        self.s.connect((host, port))
        time.sleep(3)
        print('Connection established')
        return self.s

    def write_to_PC(self, message, pc):
        try:
            print('Transmitted to PC:')
            print('\t %s' % message)
            pc.send(message)
        except Exception as e:
            print('[write to PC ERROR] %s' % str(e))
            raise e

    def read_from_PC(self, pc,andr):
        stm = STMConnection()
        try:
            data = pc.recv(2048).strip()
            print('Transmission from PC:')
            print('\t %s' % data)

            if len(data) > 0:
                movements = data.decode().split(",")

                if movements[0] == "obstacles":
                    self.obstacle_list = movements[1:]
                    #obstacle_info = "<"+ str(self.obstacle_list) + ">"
                    #print(str(self.obstacle_list))
                    #andr.send(obstacle_info)
                    self.write_to_PC("Movement Done".encode(),pc)
                else:
                    print(len(movements))
                    while movements:
                        curr_movement = movements.pop(0)
                        print(movements)
                        print("length now " + str(len(movements)))
                        print(curr_movement)
                        #self.stm.thread_send(data)
                        stm.thread_send(curr_movement.encode('utf-8'))
                        #reply = stm.thread_recv()
                        while True:
                            reply = stm.thread_recv()
                            if reply == "k":
                                print("reply: ", reply)
                                break

                        if curr_movement == 'x':
                            curr_obs = self.obstacle_list.pop(0)
                            #imgDet = streamImages()
                            #imgDet.connect_to_pc()
                            img_id = self.imgDet.startStreaming()
                            #img_id = '0'
                            #write to android
                            info = "<target," + str(curr_obs) + "," + img_id.decode() +">"
                            print("obstacle id: " + curr_obs)
                            print("image id: " + img_id.decode())
                            andr.send(info)
                            self.write_to_PC("Movement Done".encode(),pc)
                        else:
                            #pass
                            andr.send("<move," + str(curr_movement) +">")
                #return data

            #return None

        except Exception as e:
            print('[error] %s' % str(e))
            raise e

    def startComms(self):
        ser = PC_Comm()
        ser.connect_PC()
        time.sleep(3)
        print('Connection to RPI established')
        self.stm = STMConnection()

    def listen_and_write(self, pc ,andr):
        while True:
            try:
                self.read_from_PC(pc,andr)
                #self.write_to_PC('Received input!'.encode('utf-8'), pc)
                #self.stm.thread_recv()

            except KeyboardInterrupt:
                print('Communication interrupted')
                pc.close()
                break