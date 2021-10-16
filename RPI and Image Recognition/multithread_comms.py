import threading
from client_for_pc import PC_Comm
from android_bluetooth import bluetoothAndroid
from test_send_img import streamImages


class multithreadComm:


    def __init__(self):
        self.pc_comms = PC_Comm()
        self.android_comms = bluetoothAndroid()
        self.image_detection = streamImages()

    def startPC_Comm(self):
        self.pc = self.pc_comms.connect_PC()

    def startAndroid_Comm(self):
        self.android = self.android_comms.connect_android()

    def startPC_listen(self, pc, android):
        self.pc_comms.listen_and_write(pc,android)

    def startAndroid_to_pc_listen(self, pc, android):
        self.android_comms.listen_and_write_to_pc(pc, android)

    def startAndroid_to_stm_listen(self, android):
        self.android_comms.listen_and_write_to_stm(android)

    def startImageDetection(self):
        self.image_detection.startStreaming()

    def main(self):
        #start_pc_comms = threading.Thread(target=self.startPC_Comm)
        start_android_comms = threading.Thread(target=self.startAndroid_Comm)
        #start_pc_comms.start()
        start_android_comms.start()
        #start_pc_comms.join()
        start_android_comms.join()
        #start_pc_listen = threading.Thread(target=self.startPC_listen, args=(self.pc,self.android))
        #start_android_to_pc_listen = threading.Thread(target=self.startAndroid_to_pc_listen, args=(self.pc, self.android))
        start_android_to_stm_listen = threading.Thread(target=self.startAndroid_to_stm_listen, args=(self.android,))
        #start_image_detection = threading.Thread(target=self.startImageDetection)
        #start_pc_listen.start()
        #start_android_to_pc_listen.start()
        start_android_to_stm_listen.start()
        #start_image_detection.start()

if __name__ == '__main__':
    conn = multithreadComm()
    conn.__init__()
    conn.main()