#define PIN_LED    0
#define PIN_GAS   A3
#define PIN_SOUND A2


void setup() {
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_GAS, INPUT);
  pinMode(PIN_SOUND, INPUT);
}

int get_sound(void) {
    return analogRead(PIN_SOUND);
}

int get_alcohol(void) {
    return analogRead(PIN_GAS);
}

void loop() {
  digitalWrite(PIN_LED, get_sound() > 200);
}
