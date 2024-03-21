from zenoh.session import Session, Subscriber, Publisher, Sample, Encoding, Reply
import zenoh
import struct
import time
import sys


base_ram_key = "ram"
# base_ram_key = "pub_test"
counter = 0

def change_listener(sample: Sample):
    # global counter
    # counter += 1
    # if counter % 10000 == 0:
    print(f"Received {sample.kind} ('{sample.key_expr}': '{sample.payload}')")

session: Session = zenoh.open()
# session.declare_subscriber(f'{base_ram_key}/changes', change_listener)
subscriber: Subscriber = session.declare_subscriber(f'{base_ram_key}/**', change_listener)

# print("Enter 'q' to quit...")
# c = '\0'
# while c != 'q':
#     c = sys.stdin.read(1)
#     if c == '':
#         time.sleep(1)

def get_response(reply: Reply):
    # if reply.is_ok:
    sample: Sample = reply.ok
    print(f"Received reply from get: {sample.kind} ('{sample.key_expr}': '{sample.payload}')")
    address = struct.unpack('>i', sample.payload[:4])[0]
    length = struct.unpack('>i', sample.payload[4:8])[0]
    data = sample.payload[8:]
    print(f"address: {address}, length: {length}, data: {data}")

def info_response(reply: Reply):
    # if reply.is_ok:
    sample: Sample = reply.ok
    print(f"Received reply from info: {sample.kind} ('{sample.key_expr}': '{sample.payload}')")
    size = struct.unpack('>i', sample.payload[:4])[0]
    data_width = struct.unpack('>i', sample.payload[4:])[0]
    print(f"size: {size}, data_width: {data_width}")

clock = False

while True:
    command = input("Enter command: ")
    command_args = command.split()

    if command_args[0] == "get":
        addr = command_args[1]
        length = command_args[2]

        # https://docs.python.org/3/library/struct.html
        buf = struct.pack('>i', int(addr)) + struct.pack('>i', int(length))
        session.get(f'{base_ram_key}/get', get_response, value=buf)

    elif command_args[0] == "info":
        session.get(f'{base_ram_key}/info', info_response)

    elif command_args[0] == "set": # set <addr> <bytes_per_word> <data>
        # ex set 0 1 01
        addr = int(command_args[1])
        bytes_per_word = int(command_args[2])

        # parse data as byte array from hex
        data = bytes.fromhex(command_args[3])

        # https://docs.python.org/3/library/struct.html
        buf = struct.pack('>i', addr) + struct.pack('>i', int(len(data) / bytes_per_word)) + data

        session.put(f'{base_ram_key}/set', buf)

    elif command_args[0] == "getr":
        key = command_args[1]

        # https://docs.python.org/3/library/struct.html
        session.get(key, lambda reply: print(f"Received reply from get: {reply.ok}"))

    elif command_args[0] == "put":
        key = command_args[1]
        data = bytes.fromhex(command_args[2])
        session.put(key, data)

    elif command_args[0] == "put8":
        key = command_args[1]
        value = int.from_bytes(bytes.fromhex(command_args[2]), byteorder='big')
        session.put(key, struct.pack('>Q', value))

    elif command_args[0] == "clock":
        clock = not clock
        key = 'clock'
        value = 0 if not clock else 1
        session.put(key, struct.pack('>Q', value))