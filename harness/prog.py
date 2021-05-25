#!/usr/bin/env python3

import serial
import sys

ACK = b'\x06'
NAK = b'\x15'

ser = serial.Serial('/dev/cu.usbmodem1412401', 115200, timeout=1)

# read and evaluate response
def readResponse():
    d = ser.read(1)
    print(d)
    if len(d) == 0:
        raise Exception('response timeout')
    if d == ACK:
        return
    if d == NAK:
        raise Exception('NAK received')
    raise Exception('unkown response')

# send a command    
def sendCommand(cmd):
    ser.write(cmd.encode())
    ser.flush()
    readResponse()

# send a block
def sendBlock(data):
    ser.write('b'.encode())
    print("sent block!")
    print(data)
    for d in data:
        ser.write([d])
    ser.flush()
    readResponse()

# read file and add some bytes at the end for the required 49 clocks for configuration
inputFile = open(sys.argv[1], 'rb')
bufSize = 128
data = bytearray(inputFile.read()) + bytearray([0] * (bufSize * 2))

# reset FPGA
ser.write('r'.encode())
ser.flush()
d = ser.read(1)
print(d)
ser.write('r'.encode())
ser.flush()
d = ser.read(1)
print(d)
ser.write('r'.encode())
ser.flush()
d = ser.read()
print(d)
sendCommand('r')
print("Sent reset!")

# send data to FPGA
while len(data) > bufSize:
    block = data[:bufSize]
    data = data[bufSize:]
    sendBlock(block)

