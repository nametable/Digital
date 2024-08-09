from zenoh.session import Session, Subscriber, Publisher, Sample, Encoding
import zenoh
import struct
import time

def listener(sample: Sample):
    print("Encoding", sample.encoding)
    if sample.encoding == Encoding.TEXT_PLAIN():
        pass
    else:
        print(f"Received {sample.kind} ('{sample.key_expr}': '{sample.payload}')")
        # value = struct.unpack('>Q', sample.payload)[0]
        # print(f"Received {sample.kind} ('{sample.key_expr}': '{sample.payload.decode('utf-8')}')")
        # print(f"{sample.key_expr}: {value}")
        # print(f"timestamp: {time.time()}")

session: Session = zenoh.open()
# subscriber: Subscriber = session.declare_subscriber('zenoh_test/clock/**', listener)
# publisher: Publisher = session.declare_publisher('zenoh_test/clock/speed')
subscriber: Subscriber = session.declare_subscriber('**', listener)
publisher: Publisher = session.declare_publisher('test')

while True:
    new_speed = input("Enter new speed: ")
    buf = struct.pack('>Q', int(new_speed))
    print(f"timestamp: {time.time()}")
    publisher.put(buf, Encoding.APP_INTEGER())
    # publisher.put(new_speed.encode('utf-8'), Encoding.TEXT_PLAIN())
