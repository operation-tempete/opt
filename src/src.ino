#include <SoftwareSerial.h>

 SoftwareSerial bt(2, 1);
#define PIN_BUZZER 0
#define PIN_GAS   A3
#define PIN_SOUND A2
#define ALCOOLISM 300
#define LOUD 250
#define GAS_IT 10

 int BluetoothData; // the data given from Computer
 char sbuf[64];
 bool force_beep = false;
 bool 

int get_alcohol(void) {
  short i = 0;
  long sum = 0;
  short average = 0;
  
  for (i = 0; i < GAS_IT; ++i)
    sum += analogRead(PIN_GAS);
  average = sum / GAS_IT;
  return average;
}

 void setup() {
   bt.begin(9600);
   pinMode(PIN_BUZZER, OUTPUT);
   pinMode(PIN_GAS, INPUT);
   pinMode(PIN_SOUND, INPUT);
 }

 void loop() {
   int gas;
   int snd;
   
    if (bt.available()) {
       BluetoothData=bt.read();
       if(BluetoothData=='1') {
          digitalWrite(PIN_BUZZER, 1);
          force_beep = true;
          //bt.println("LED  On D13 ON ! ");
       }
       if (BluetoothData=='0') {
           digitalWrite(PIN_BUZZER ,0);
           force_beep = false;
           //bt.println("LED  On D13 Off ! ");
       }
    }
    gas = get_alcohol();
    snd = analogRead(PIN_SOUND);
    if (force_beep == false)
      digitalWrite(PIN_BUZZER, (gas > ALCOOLISM || snd > LOUD));
    snprintf(sbuf, sizeof(sbuf), "%d - %d\n", gas, snd);
    bt.println(sbuf);
    delay(100);
 }

