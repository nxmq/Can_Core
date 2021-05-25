#!/usr/bin/env python3
from wbdbgbus import *
import sys

if len(sys.argv) < 4:
    print("Usage: {} (serial port) (baud) (addr) [value]".format(sys.argv[0]))
    sys.exit(1)

port = sys.argv[1]
baud = int(sys.argv[2])

if len(sys.argv) > 4:
    readback = not ("no-readback" in sys.argv[4:])
    ascii_mode = "ascii-big" in sys.argv[4:] or "ascii-little" in sys.argv[4:]
    ascii_endian = "ascii-big" in sys.argv[4:]
else:
    readback = True
    ascii_mode = False

with DebugBus(port, baud, fifo_size=1, timeout=0) as fpga:
    addr = sys.argv[3].replace("_", "")

    try:
        value = int(sys.argv[4].replace("_", ""), 0) if len(sys.argv) > 4 else None
    except:
        value = None

    if addr == "reset":
        fpga.reset()
    else:
        addr = int(addr, 0)
        if value is None:
            if ascii_mode:
                data = fpga.read(addr)[0]
                data = "{:08x}".format(data)
                data = bytes.fromhex(data).decode("ascii")
                if not ascii_endian:
                    data = data[::-1]
                print("0x{:08x} = {}".format(addr, data))
            else:
                print("0x{:08x} = 0x{:08x}".format(addr, fpga.read(addr)[0]))
        else:
            #print("Writing (to 0x{:08x}) : 0x{:08x}".format(addr, value))
            fpga.write(addr, value)
            if readback:
                print("0x{:08x} = 0x{:08x}".format(addr, fpga.read(addr)[0]))
            else:
                print("0x{:08x} = 0x{:08x} (as written)".format(addr, value))

