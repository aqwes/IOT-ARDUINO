#include <FreeSixIMU.h>
#include <FIMU_ADXL345.h>
#include <FIMU_ITG3200.h>

#define DEBUG
#ifdef DEBUG
#include "DebugUtils.h"
#endif

#include "CommunicationUtils.h"
#include "FreeSixIMU.h"
#include <Wire.h>
int sen = 18000; // you can change it to make it more or less sensitive
int win = 30; // this is your window size for sliding window
int q[6]; //hold q values
int p[6]; //hold q values
int t = 0;
int accX, accY, accZ, gyrX, gyrY, gyrZ = 0;
int counter = 0;
boolean gesture = false;
boolean sending = false;
boolean stoping = false;
// Set the FreeIMU object
FreeSixIMU my3IMU = FreeSixIMU();

void setup() {
Serial.begin(9600);
Wire.begin();

delay(5);
my3IMU.init();
delay(5);
}

void loop() { 
  
my3IMU.getRawValues(q);
accX = p[0]-q[0];
accY = p[1]-q[1];
accZ = p[2]-q[2];
gyrX = p[3]-q[3];
gyrY = p[4]-q[4];
gyrZ = p[5]-q[5];

// first classification based on activation of accelerometer: detects if gesture is happening or not
if (!gesture) {
  t = accX*accX+accY*accY+accZ*accZ;
  if(abs(t) > sen) {
  gesture = true;
  sending = true;
  stoping = true;
  }
}


if (counter < win && gesture) {
  if(sending == true){
  Serial.print("#"); // send a header character
  }
  sending =false; 
Serial.print(",");
Serial.print(accX);
Serial.print(",");
Serial.print(accY);
Serial.print(",");  
Serial.print(accZ);
Serial.print(",");
Serial.print(gyrX);
Serial.print(",");
Serial.print(gyrY);
Serial.print(",");  
Serial.print(gyrZ); 
 
for (int k=0; k<=5; k++){
  p[k] = q[k];
}
 //Serial.print("~");
delay(60);
 counter ++;
} else { // reset counter and gesture
  counter = 0;
  gesture = false;
  if(stoping == true){
    Serial.print(",");  
    Serial.print("~");
  }stoping = false;
  }
  
}

