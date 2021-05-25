#!/usr/bin/env sh

# reset IC
./scripts/wb /dev/tty.usbserial-00000000 115200 reset
# program in quanta
./scripts/wb /dev/tty.usbserial-00000000 115200 6 0x01
./scripts/wb /dev/tty.usbserial-00000000 115200 7 0x1c
# set clkdiv register
./scripts/wb /dev/tty.usbserial-00000000 115200 31 0
# program acceptance code/mask
./scripts/wb /dev/tty.usbserial-00000000 115200 4 0xe8
./scripts/wb /dev/tty.usbserial-00000000 115200 5 0x0f
# enter resetmode
sleep 1
./scripts/wb /dev/tty.usbserial-00000000 115200 0 0x1
sleep 1
# program masks for tx
./scripts/wb /dev/tty.usbserial-00000000 115200 4 0x28
./scripts/wb /dev/tty.usbserial-00000000 115200 5 0xff
# exit resetmode
./scripts/wb /dev/tty.usbserial-00000000 115200 0 0
# enable IRQs
./scripts/wb /dev/tty.usbserial-00000000 115200 1 0
# load tx data
./scripts/wb /dev/tty.usbserial-00000000 115200 10 0xea
./scripts/wb /dev/tty.usbserial-00000000 115200 11 0x28
./scripts/wb /dev/tty.usbserial-00000000 115200 12 0x56
./scripts/wb /dev/tty.usbserial-00000000 115200 13 0x78
./scripts/wb /dev/tty.usbserial-00000000 115200 14 0x9a
./scripts/wb /dev/tty.usbserial-00000000 115200 15 0xbc
./scripts/wb /dev/tty.usbserial-00000000 115200 16 0xde
./scripts/wb /dev/tty.usbserial-00000000 115200 17 0xf0
./scripts/wb /dev/tty.usbserial-00000000 115200 18 0x0f
./scripts/wb /dev/tty.usbserial-00000000 115200 19 0xed
# request tx
./scripts/wb /dev/tty.usbserial-00000000 115200 0 0x0
./scripts/wb /dev/tty.usbserial-00000000 115200 1 0x1

