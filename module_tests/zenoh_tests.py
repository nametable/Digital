import unittest
import zenoh
import struct
import time

# runs Digital.jar with the given circuit file (.dig)
def start_digital_with_circuit(circuit_path: str):
    import subprocess
    return subprocess.Popen(["java", "-cp", "../target/Digital.jar", "CLI", "run", "-dig", circuit_path], stdout=subprocess.DEVNULL)

class TestZenohPublisher(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/pub_circuit.dig")

    def test_receive(self):
        sub = self.session.declare_subscriber("test/pub1")
        sample = sub.recv()

        # value of 1 packed into 8 bytes
        self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", 1))

        sample = sub.recv()

        # value of 0 packed into 8 bytes
        self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", 0))

    def tearDown(self):
        self.session.close()
        self.process.terminate()
        self.process.wait()

class TestZenohSubscriber(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/sub_circuit.dig")

    def test_publish(self):
        pub1 = self.session.declare_publisher("test/sub1")
        pub2 = self.session.declare_publisher("test/sub2")
        sub1 = self.session.declare_subscriber("test/pub1")
        sub2 = self.session.declare_subscriber("test/pub2")

        time.sleep(1)
        sub1.try_recv()
        sub2.try_recv()
        # print("Sending 5 to test/sub1")
        pub1.put(struct.pack(">Q", 5))

        sample = sub1.recv()
        self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", 5))

        pub2.put(struct.pack(">Q", 5))

        sample = sub2.recv()
        self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", 10))

    def tearDown(self):
        self.session.close()
        self.process.terminate()
        self.process.wait()

class TestZenohSubscriberSynchronous(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/subsync_circuit.dig")

    def test_subscribe_sync(self):
        clock = self.session.declare_publisher("test/clock")
        sync_sub_pub = self.session.declare_publisher("test/sub_sync")
        out_sub = self.session.declare_subscriber("test/output")

        out_sub.try_recv()

        values = [0x8888, 0x6666, 0x5555, 0x4444, 0x3333]

        time.sleep(1)
        for value in values:
            sync_sub_pub.put(struct.pack(">Q", value))

        clock.put(struct.pack(">Q", 1))

        for value in values:
            clock.put(struct.pack(">Q", 0))

            sample = out_sub.recv()
            self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", value))

            clock.put(struct.pack(">Q", 1))
            time.sleep(0.1)

    def tearDown(self):
        self.session.close()
        self.process.terminate()
        self.process.wait()

class TestZenohRegister(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/reg_circuit.dig")

    def test_register(self):
        enable = self.session.declare_publisher("test/enable")
        changes = self.session.declare_subscriber("test/reg/changes")
        set_reg = self.session.declare_publisher("test/reg/set")

        time.sleep(1)
        changes.try_recv()

        set_reg.put(struct.pack(">Q", 0))
        enable.put(struct.pack(">Q", 1))
        for i in range(10):
            val = struct.unpack(">Q", changes.recv().payload.deserialize(bytes))[0]
            self.assertEqual(val, (i + 1) * 2)

        enable.put(struct.pack(">Q", 0))
        set_reg.put(struct.pack(">Q", 1))
        val = struct.unpack(">Q", changes.recv().payload.deserialize(bytes))[0]
        self.assertEqual(val, 1)
        changes.try_recv()

        enable.put(struct.pack(">Q", 1))
        for i in range(10):
            val = struct.unpack(">Q", changes.recv().payload.deserialize(bytes))[0]
            self.assertEqual(val, (i + 1) * 2 + 1)

        enable.put(struct.pack(">Q", 0))

    def tearDown(self):
        self.session.close()
        self.process.terminate()
        self.process.wait()

def bytes_per_word(bits: int) -> int:
    bytes = (bits - 1) // 8 + 1
    rounded_bytes = 1
    while rounded_bytes < bytes:
        rounded_bytes *= 2
    return rounded_bytes


class RamRange:
    def __init__(self, address, length, data, word_size):
        self.address = address
        self.length = length
        self.data: list[int] = data
        self.word_size = word_size

    def from_bytes(data: bytes, word_size: int):
        address = struct.unpack(">I", data[0:4])[0]
        words = struct.unpack(">I", data[4:8])[0]

        match word_size:
            case 8:
                unpacked_data = list(struct.unpack(">" + "Q" * words, data[8:]))
            case 4:
                unpacked_data = list(struct.unpack(">" + "I" * words, data[8:]))
            case 2:
                unpacked_data = list(struct.unpack(">" + "H" * words, data[8:]))
            case 1:
                unpacked_data = list(struct.unpack(">" + "B" * words, data[8:]))

        return RamRange(address, words, unpacked_data, word_size)
    
    def to_bytes(self) -> bytes:
        buffer = bytes()
        buffer += struct.pack(">I", self.address)
        buffer += struct.pack(">I", self.length)
        buffer += struct.pack(">" + "I" * len(self.data), *self.data)
        return buffer

    def __eq__(self, other):
        return self.address == other.address and self.length == other.length and self.data == other.data and self.word_size == other.word_size

class TestRAMDualAccess(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/ram_circuit.dig")

    def test_ram(self):
        store = self.session.declare_publisher("ram1/str")
        load = self.session.declare_publisher("ram1/ld")
        out_data1 = self.session.declare_subscriber("ram1/out_d1")
        out_data2 = self.session.declare_subscriber("ram1/out_d2")

        ram_info = self.session.declare_subscriber("ram1/ram/info")
        ram_set = self.session.declare_publisher("ram1/ram/set")
        ram_changes = self.session.declare_subscriber("ram1/ram/changes")

        in_addr1 = self.session.declare_publisher("ram1/addr1")
        in_addr2 = self.session.declare_publisher("ram1/addr2")
        in_data = self.session.declare_publisher("ram1/in_d1")

        clock = self.session.declare_publisher("ram1/clock")


        time.sleep(3)
        
        # check that info is correct (8 bit addr, 32 bit data)
        info = ram_info.recv().payload.deserialize(bytes)
        # size is a 4 bytes integer
        size = struct.unpack(">I", info[0:4])[0]
        # bits is a 4 byte integer
        bits = struct.unpack(">I", info[4:8])[0]

        bytes_per_word_num = bytes_per_word(bits)

        self.assertEqual(size, 256)
        self.assertEqual(bits, 32)

        time.sleep(1)

        # write 0x12345678 to address 0x10
        in_addr1.put(struct.pack(">Q", 0x10))
        in_data.put(struct.pack(">Q", 0x12345678))
        store.put(struct.pack(">Q", 1))
        clock.put(struct.pack(">Q", 1))
        time.sleep(0.1)
        clock.put(struct.pack(">Q", 0))

        # read from address 0x10
        in_addr1.put(struct.pack(">Q", 0x10))
        load.put(struct.pack(">Q", 1))

        # check changes
        changes: RamRange = RamRange.from_bytes(ram_changes.recv().payload.deserialize(bytes), bytes_per_word_num)
        self.assertEqual(changes.address, 0x10)
        self.assertEqual(changes.length, 1)
        self.assertEqual(changes.data, [0x12345678])

        data = out_data1.recv().payload.deserialize(bytes)
        self.assertEqual(data, struct.pack(">Q", 0x12345678))

        # write 0x87654321 to address 0x20
        in_addr1.put(struct.pack(">Q", 0x20))
        in_data.put(struct.pack(">Q", 0x87654321))
        # store.put(struct.pack(">Q", 1))
        clock.put(struct.pack(">Q", 1))
        time.sleep(0.1)
        clock.put(struct.pack(">Q", 0))

        # read from address 0x20
        in_addr2.put(struct.pack(">Q", 0x20))
        data = out_data2.recv().payload.deserialize(bytes)
        self.assertEqual(data, struct.pack(">Q", 0x87654321))

        # check changes
        changes = RamRange.from_bytes(ram_changes.recv().payload.deserialize(bytes), bytes_per_word_num)
        self.assertEqual(changes.address, 0x20)
        self.assertEqual(changes.length, 1)
        self.assertEqual(changes.data, [0x87654321])

        # write increasing values to all addresses
        data_to_write = bytes()
        for i in range(size):
            data_to_write += struct.pack(">I", i)
        buffer = bytes()
        buffer += struct.pack(">I", 0)
        buffer += struct.pack(">I", size)
        buffer += data_to_write
        ram_set.put(buffer)

        # read changes
        changes = ram_changes.recv().payload.deserialize(bytes)
        self.assertEqual(changes, buffer)

        # read 4 words from address 0x10
        request_payload = bytes()
        request_payload += struct.pack(">I", 0x10)
        request_payload += struct.pack(">I", 4)
        some_words = RamRange.from_bytes(self.session.get("ram1/ram/get", payload=request_payload).recv().ok.payload.deserialize(bytes), bytes_per_word_num)
        self.assertEqual(some_words.address, 0x10)
        self.assertEqual(some_words.length, 4)
        self.assertEqual(some_words.data, [0x10, 0x11, 0x12, 0x13])


    def tearDown(self):
        self.session.close()
        self.process.terminate()
        self.process.wait()

class TestRAMSeparatedPorts(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/ram_circuit.dig")

    def test_ram(self):
        store = self.session.declare_publisher("ram2/str")
        load = self.session.declare_publisher("ram2/ld")
        out_data = self.session.declare_subscriber("ram2/out_data")

        ram_info = self.session.declare_subscriber("ram2/ram/info")
        ram_set = self.session.declare_publisher("ram2/ram/set")
        ram_changes = self.session.declare_subscriber("ram2/ram/changes")

        in_addr = self.session.declare_publisher("ram2/addr")
        in_data = self.session.declare_publisher("ram2/in_data")

        clock = self.session.declare_publisher("ram2/clock")


        time.sleep(3)
        
        # check that info is correct (4 bit addr, 7 bit data)
        info = ram_info.recv().payload.deserialize(bytes)
        # size is a 4 bytes integer
        size = struct.unpack(">I", info[0:4])[0]
        # bits is a 4 byte integer
        bits = struct.unpack(">I", info[4:8])[0]

        bytes_per_word_num = bytes_per_word(bits)

        self.assertEqual(size, 16)
        self.assertEqual(bits, 7)

        time.sleep(1)

        # write 0x12 to address 0x2
        in_addr.put(struct.pack(">Q", 0x2))
        in_data.put(struct.pack(">Q", 0x12))
        store.put(struct.pack(">Q", 1))
        clock.put(struct.pack(">Q", 1))
        time.sleep(0.1)
        clock.put(struct.pack(">Q", 0))

        # read from address 0x2
        load.put(struct.pack(">Q", 1))

        data = out_data.recv().payload.deserialize(bytes)
        self.assertEqual(data, struct.pack(">Q", 0x12))

        # check changes
        changes: RamRange = RamRange.from_bytes(ram_changes.recv().payload.deserialize(bytes), bytes_per_word_num)
        self.assertEqual(changes.address, 0x2)
        self.assertEqual(changes.length, 1)
        self.assertEqual(changes.data, [0x12])

        # write increasing values to all addresses
        data_to_write = bytes()
        for i in range(size):
            data_to_write += struct.pack(">B", i)
        buffer = bytes()
        buffer += struct.pack(">I", 0)
        buffer += struct.pack(">I", size)
        buffer += data_to_write
        ram_set.put(buffer)

        # read changes
        changes = ram_changes.recv().payload.deserialize(bytes)
        self.assertEqual(changes, buffer)

        # read 5 words from address 0x7
        request_payload = bytes()
        request_payload += struct.pack(">I", 0x7)
        request_payload += struct.pack(">I", 5)
        some_words = RamRange.from_bytes(self.session.get("ram2/ram/get", payload=request_payload).recv().ok.payload.deserialize(bytes), bytes_per_word_num)
        self.assertEqual(some_words.address, 0x7)
        self.assertEqual(some_words.length, 5)
        self.assertEqual(some_words.data, [0x7, 0x8, 0x9, 0xa, 0xb])

    def tearDown(self):
        self.session.close()
        self.process.terminate()
        self.process.wait()

if __name__ == '__main__':
    unittest.main()