from bluetooth import *
from STM_connection import STMConnection

class fastpath:
    def main(self, andr):
        stm = STMConnection()
        while True:
            try:
                data = andr.recv(2048).strip()
                print(data)
                if data.decode() == "START":
                    print(data)
                    stm.thread_send("n".encode())



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

if __name__ == '__main__':
    conn = fastpath()
    andr = conn.connect_android()
    conn.main(andr)