// General
#define PIN_LED    0
#define PIN_GAS   A3
#define PIN_SOUND A2

// Breath analyzer related
#define SIZE 10


void setup() {
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_GAS, INPUT);
  pinMode(PIN_SOUND, INPUT);
}

int get_sound(void) {
    return analogRead(PIN_SOUND);
}

unsigned short update_sum(short* array, short current_index, short old_sum) {
  old_sum -= array[SIZE -current_index - 1];
  return old_sum + array[current_index];
}

int get_alcohol(void) {
  static short values[SIZE];
  static short i = 0;
  static short sum = 0;
  static short average = 0;
  
  values[i % SIZE] = analogRead(PIN_GAS);
  if (i > SIZE) {
    sum = update_sum(values, i % SIZE, sum);
    average = sum / SIZE; 
  }
  ++i;
  digitalWrite(PIN_LED, average > 250);
}

void loop() {
  static short read_alcohol = 0;
  if (++read_alcohol == 50) {
    read_alcohol = 0;
    get_alcohol();
  }
  digitalWrite(PIN_LED, get_sound() > 300);
}
