//import processing.serial.*;
//Serial heartbeat; 
FakeExtDataStreamer heartbeat; 

String val = "0.0"; // Wert von Arduino 


float bg = 20;
float s = 255;

float xoff = 0;
float yoff = 1000;

float fval = 0;
float fvalBuffer;

float u(float n) {
  return width/100 * n; //50
}

void setup() {

  //String portName = Serial.list()[7];
  //heartbeat = new Serial(this, portName, 9600);

  heartbeat = new FakeExtDataStreamer(this, 1);

  size(1000, 1000);
  //pixelDensity(displayDensity());
  background(bg);
  strokeWeight(3);
  stroke(s);
  smooth();
}

void draw() {


  //arduino
  if (heartbeat.available() >0) 
  { 
    val=heartbeat.readStringUntil('\n');

    if (val != null && val.equals("")==false) {
      fval = float(val);
      //langsamer 
      if (fval > 1) {
        fval = 0.5;
      }

      if (fval > fvalBuffer) {
        fvalBuffer = fval;
        fval = fval * 8;
      }

      if (fval < fvalBuffer) {
        fvalBuffer -= 0.005f;
        fval = fvalBuffer * 8;
      }
      println("fval", nfs(fval, 0, 2), nfs(fvalBuffer, 0, 2));
    }
  }
  fval=8;

  background(bg);
  for (float y = height*0.3; y < height*0.9; y += u(1.5)) { //height*0.1 
    pushMatrix();
    translate(20, y);
    noFill();
    beginShape();
    for (float x = width*0.1; x < width*0.9; x++) {

      float ypos = map(noise(x/100 + xoff, y/30 + yoff), 0, 1, -100, 100);
      float magnitude = x < width*0.5 ? map(x, width*0.1, width*0.5, 0, 1) : map(x, width*0.5, width*0.9, 1, 0) ;
      ypos *= magnitude *fval;

      if (ypos > 0) ypos = 0;

      vertex(x, ypos);
    }
    endShape();
    popMatrix();
  }

  xoff += 0.01;
  yoff += -0.01;
}

void keyPressed() {
  if (key=='s'||key=='S') 
    saveFrame();
}

void serialEvent(SerialSimulator s) {
}

class FakeExtDataStreamer extends SerialSimulator{
  
  
  FakeExtDataStreamer(){
    super(this,2);
  }
}
