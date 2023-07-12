#!/bin/bash

echo "nrf52840"

while true; do
	if [ "$1" = "cooja" ]; then
		make TARGET=cooja connect-router-cooja
	else
		make TARGET=$1 BOARD=dongle PORT=$2 connect-router
	fi
	sleep 2.5
done
