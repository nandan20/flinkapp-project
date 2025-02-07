import socket
import time
import random

HOST = ''   # Listen on all interfaces
PORT = 9999

sensors = ['sensor1', 'sensor2', 'sensor3']

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen(1)
    print("Sensor Data Generator: Listening on port", PORT, flush=True)
    while True:
        # Accept a connection (blocking)
        conn, addr = s.accept()
        print("Sensor Data Generator: Connected by", addr, flush=True)
        with conn:
            while True:
                sensor_id = random.choice(sensors)
                temperature = round(random.uniform(20, 30), 2)
                message = sensor_id + "," + str(temperature) + "\n"
                try:
                    conn.sendall(message.encode())
                    print("Sent:", message.strip(), flush=True)  # Debug log for each message sent
                except BrokenPipeError:
                    print("Client disconnected. Waiting for a new connection...", flush=True)
                    break  # Break inner loop to accept a new connection
                time.sleep(1)
