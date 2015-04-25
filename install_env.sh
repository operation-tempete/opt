#! /bin/sh

wget http://downloads.arduino.cc/arduino-1.6.3-linux64.tar.xz
tar xf arduino-1.6.3-linux64.tar.xz
rm arduino-1.6.3-linux64.tar.xz
git clone https://github.com/damellis/attiny.git
cd attiny
git checkout ide-1.5.x
cd ..
cp -r attiny/attiny arduino-1.6.3/hardware/
rm -rf attiny
