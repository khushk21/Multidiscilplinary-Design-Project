from bluetooth import *
from client_for_pc import PC_Comm
from STM_connection import STMConnection

class bluetoothAndroid:

    def write_to_android(self, text, android):
        try:
            android.send(text.encode('utf-8'))
        except Exception as e:
            print('[write to RPI ERROR] %s' % str(e))
            raise e

    def listen_to_android_to_pc(self, pc, android):
        PC_comms = PC_Comm()
        i=0
        XnYnFacing = []
        try:
            data = android.recv(2048).strip()
            if len(data) > 0:
                print(data)
                data = data.decode().split(',')
                data = data[0:-1]
                print("length of data =" + str(len(data)))
                if len(data) <= 4:
                    PC_comms.write_to_PC(data.encode(), pc)
                else:
                    while i < len(data):
                        XnYnFacing.append(data[i+1]) #obstacle id
                        XnYnFacing.append(data[i+2])
                        XnYnFacing.append(data[i+3])
                        XnYnFacing.append(data[i+6])
                        i+=7
                        print("curr i = " + str(i))

                    obstacles = "obstacles," + ",".join(XnYnFacing)
                    print(obstacles)
                    PC_comms.write_to_PC(obstacles.encode(), pc)

        except Exception as e:
            print('[write to RPI ERROR] %s' % str(e))
            raise e

    def listen_to_android_to_stm(self, android):
        #PC_comms = PC_Comm()
        stm = STMConnection()
        try:
            data = android.recv(2048).strip()
            if len(data) > 0:
                if data.decode() =='stop':
                    stm.thread_send('x'.encode())
                else:
                    stm.thread_send(data)
                print(data)
                #PC_comms.write_to_PC(data, pc)

        except Exception as e:
            print('[write to RPI ERROR] %s' % str(e))
            raise e

    def connect_android(self):
        os.system('sudo hciconfig hci0 piscan')
        server_sock = BluetoothSocket(RFCOMM)
        server_sock.bind(("",PORT_ANY))
        server_sock.listen(1)
        port = server_sock.getsockname()[1]
        uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"
        advertise_service( server_sock, "MDPGrp17",
         service_id = uuid,
         service_classes = [ uuid, SERIAL_PORT_CLASS ],
         profiles = [ SERIAL_PORT_PROFILE ],
        # protocols = [ OBEX_UUID ]
         )
        print("Waiting for connection on RFCOMM channel %d" % port)
        self.c, self.client_info = server_sock.accept()
        print("Accepted connection from ", self.client_info)
        return self.c

    def listen_and_write_to_stm(self, android):
        try:
            while True:
                self.listen_to_android_to_stm(android)
                self.write_to_android("RECEIVED", android)

        except IOError:
            pass
        print("Disconnected")
        self.c.close()
        server_sock.close()
        print("All closed")

    def listen_and_write_to_pc(self, pc, android):
        try:
            while True:
                self.listen_to_android_to_pc(pc, android)
                self.write_to_android("RECEIVED", android)

        except IOError:
            pass
        print("Disconnected")
        self.c.close()
        server_sock.close()
        print("All closed")

    def startComms(self):
        conn = bluetoothAndroid()
        conn.connect_android()