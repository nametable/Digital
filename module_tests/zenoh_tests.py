import unittest
import zenoh
import struct

# runs Digital.jar with the given circuit file (.dig)
def start_digital_with_circuit(circuit_path: str):
    import subprocess
    return subprocess.Popen(["java", "-cp", "../target/Digital.jar", "CLI", "run", "-dig", circuit_path])

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
        # self.process.terminate()
        self.process.kill()

class TestZenohSubscriber(unittest.TestCase):
    def setUp(self):
        self.session = zenoh.open(zenoh.Config.peer())
        self.process = start_digital_with_circuit("./resources/sub_circuit.dig")

    def test_publish(self):
        pub1 = self.session.declare_publisher("test/sub1")
        pub2 = self.session.declare_publisher("test/sub2")
        sub1 = self.session.declare_subscriber("test/pub1")
        sub2 = self.session.declare_subscriber("test/pub2")
        pub1.put(struct.pack(">Q", 123))

        sample = sub1.recv()
        self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", 123))

        pub2.put(struct.pack(">Q", 123))

        sample = sub2.recv()
        self.assertEqual(sample.payload.deserialize(bytes), struct.pack(">Q", 128))

    def tearDown(self):
        self.session.close()
        # self.process.terminate()
        self.process.kill()


if __name__ == '__main__':
    unittest.main()