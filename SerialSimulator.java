/*
  SerialSimulator - Class to simulate receiving data from serial ports
 
 Copyright (c) 2004-05 Ben Fry & Casey Reas
 Reworked by Gottfried Haider as part of GSOC 2013
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General
 Public License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 Boston, MA  02111-1307  USA
 */

//package com.kfrajer.serialsim;


import processing.core.*;
import java.util.ArrayList;
import java.lang.reflect.*;


/**
 * Class for simulation of receiving data via serial communication protocol.
 * 
 * ( end auto-generated )
 * @webref serial
 * @brief Class for simulating a serial port - receiving only 
 * @usage Application
 */
public class SerialSimulator implements Runnable, PConstants {


  final int WAIT_MS=10;
  final String PORT_NAME="SimulationPort";

  //----------------------
  PApplet p;
  Method serialEventMethod;
  Method serialAvailableMethod;

  volatile boolean invokeSerialAvailable = false;

  //----------------------
  boolean verbosity;

  //----------------------
  ArrayList<Byte> buf;
  ArrayList<Byte> rxbuf;

  int bufferUntilSize = 1;
  byte bufferUntilByte = 0;

  //----------------------
  Thread main;
  float rate;
  protected int duration;
  protected int endLapsedTime=-1;
  UserSourceSimulator simPort;



  /**
   * @param rate Fake event generation. Units in Hz
   */
  public SerialSimulator(PApplet parent, float rate) {

    p=parent;
    p.registerMethod("pre", this);
    p.registerMethod("dispose", this);

    verbosity=false;

    buf = new ArrayList<Byte>();
    rxbuf = new ArrayList<Byte>();
    this.rate=rate;  

    Float fob= new Float((1.0f/rate)*1000);
    duration=rate>0 ? fob.intValue() : 1000; //in msecs

    simPort = new UserSourceSimulator(p);

    serialEventMethod=findCallback("serialEvent");
    serialAvailableMethod=findCallback("serialAvailable");  //Needed????    

    p.println("serialEventMethod", serialEventMethod);

    (main=new Thread(this)).start();
  }

  /**
   * 
   */
  public void dispose() {

    stop();

    p.unregisterMethod("pre", this);
    p.unregisterMethod("dispose", this);


    //Documentation suggest to shut down threads here. 
    //Some concepts here: https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
  }

  /**
   * 
   */
  public boolean active() {
    return main.isAlive();
  }

  /**
   * 
   */
  int available() {    
    return buf.size();
  }

  /**
   * 
   */
  public void buffer(int size) {
    bufferUntilSize = size;
  }

  /**
   * 
   */
  public void bufferUntil(int inByte) {
    bufferUntilSize = 0;
    bufferUntilByte = (byte)inByte;
  }

  /**
   * 
   */
  public void clear() {
    synchronized (buf) {
      buf.clear();
    }
  }

  //NOT-IMPLEMENTED:   public boolean getCTS() { }
  //NOT-IMPLEMENTED:   public boolean getDSR() {}
  //NOT-IMPLEMENTED:   public static Map<String, String> getProperties(String portName) { }


  /** 
   * <h3>Advanced</h3>
   * Same as read() but returns the very last value received
   * and clears the buffer. Useful when you just want the most
   * recent value sent over the port.
   * @webref serial:serial
   * @usage web_application
   */
  public int last() {
    if (!dataReady()) {
      return -1;
    }

    synchronized (buf) {
      int ret = buf.get(buf.size()-1) & 0xFF;
      buf.clear();
      return ret;
    }
  }

  /**
   * 
   */
  public char lastChar() {
    return (char)last();
  }


  //NOT-IMPLEMENTED:  public static String[] list() { }


  /**
   * 
   */
  public int read() {
    if (!dataReady()) {
      return -1;
    }

    synchronized (buf) {
      int ret = buf.get(0) & 0xFF;
      buf.remove(0);

      return ret;
    }
  }


  /**
   * @generate Serial_readBytes.xml
   * @webref serial:serial
   * @usage web_application
   */
  public byte[] readBytes() {
    if (!dataReady()) {
      return null;
    }

    synchronized (buf) {
      int n=buf.size();
      byte[] ret = new byte[n];

      getArray(n, ret);

      buf.clear();

      return ret;
    }
  }


  /**
   * <h3>Advanced</h3>
   * Return a byte array of anything that's in the serial buffer
   * up to the specified maximum number of bytes.
   * Not particularly memory/speed efficient, because it creates
   * a byte array on each read, but it's easier to use than
   * readBytes(byte b[]) (see below).
   *
   * @param max the maximum number of bytes to read
   */
  public byte[] readBytes(int max) {
    if (!dataReady()) {
      return null;
    }

    synchronized (buf) {
      int length = buf.size();
      if (length > max) length = max;
      byte[] ret = new byte[length];

      int n=length;
      getArray(n, ret);

      return ret;
    }
  }

  /**
   * <h3>Advanced</h3>
   * Grab whatever is in the serial buffer, and stuff it into a
   * byte buffer passed in by the user. This is more memory/time
   * efficient than readBytes() returning a byte[] array.
   *
   * Returns an int for how many bytes were read. If more bytes
   * are available than can fit into the byte array, only those
   * that will fit are read.
   */
  public int readBytes(byte[] dest) {
    if (!dataReady()) {
      return 0;
    }

    synchronized (buf) {
      int toCopy = buf.size();
      if (dest.length < toCopy) {
        toCopy = dest.length;
      }

      getArray(toCopy, dest);

      return toCopy;
    }
  }

  /**
   * @generate Serial_readBytesUntil.xml
   * @webref serial:serial
   * @usage web_application
   * @param inByte character designated to mark the end of the data
   */
  public byte[] readBytesUntil(int inByte) {

    p.println("readBytesUntil", dataReady());

    if (!dataReady()) 
      return null;


    synchronized (buf) {

      //Integer iob= new Integer(inByte);
      //Byte anchor = new Byte(iob.byteValue());  
      Byte anchor = new Byte((byte)inByte);  

      if (buf.contains(anchor)) {
        int idx=buf.indexOf(anchor);

        //Build returning array
        int n = idx+1;
        byte[] dest = new byte[n];

        getArray(n, dest);        

        return dest;
      } else
        return null;
    }
  }

  /**
   * <h3>Advanced</h3>
   * If dest[] is not big enough, then -1 is returned,
   *   and an error message is printed on the console.
   * If nothing is in the buffer, zero is returned.
   * If 'interesting' byte is not in the buffer, then 0 is returned.
   * @param dest passed in byte array to be altered
   */
  public int readBytesUntil(int inByte, byte[] dest) {
    if (!dataReady()) {
      return 0;
    }

    synchronized (buf) {

      //Integer iob= new Integer(inByte);      
      //Byte anchor = new Byte(iob.byteValue());
      Byte anchor = new Byte((byte)inByte); 
      int n = -1;
      if (buf.contains(anchor)) {
        int idx=buf.indexOf(anchor);
        n=idx+1;
      } else
        return 0;

      // check if bytes to copy fit in dest
      int toCopy = n;
      if (dest.length < toCopy) {
        System.err.println( "The buffer passed to readBytesUntil() is to small " +
          "to contain " + toCopy + " bytes up to and including " +
          "char " + (byte)inByte);
        return -1;
      }

      getArray(toCopy, dest);

      return toCopy;
    }
  }


  void getArray(int n, byte[] dest) {

    if (dest==null || dest.length<n)
      dest = new byte[n];

    for (int i=0; i<n; i++) {
      dest[i]=buf.get(i);
    }

    //Remove elements in array
    for (int i=n-1; i>=0; i--) {
      buf.remove(i);
    }
  }

  /**
   * 
   */
  boolean dataReady() {
    return buf.size()>0;
  }

  /**
   * 
   */
  public char readChar() {
    return (char) read();
  }

  /**
   * 
   */
  public String readString() {
    if (!dataReady()) {
      return null;
    }
    return new String(readBytes());
  }


  /**
   * @generate Serial_readStringUntil.xml
   *<h3>Advanced</h3>
   * If you want to move Unicode data, you can first convert the
   * String to a byte stream in the representation of your choice
   * (i.e. UTF8 or two-byte Unicode data), and send it as a byte array.
   * 
   * @param inByte character designated to mark the end of the data
   */
  public String readStringUntil(int inByte) {

    byte temp[] = readBytesUntil(inByte);

    if (temp == null) {
      return null;
    } else {
      return new String(temp);
    }
  }

  /**
   * 
   */
  public void serialEvent(SerialSimulator s) {
    int toRead;

    //p.println("CLASS serial event invoked");
    p.println(rxbuf.size() + " => " + buf.size());

    while (0 < (toRead = rxbuf.size())) {

      // this method can be called from the context of another thread
      synchronized (buf) {
        // read one byte at a time if the sketch is using serialEvent
        if (serialEventMethod != null) {
          toRead = 1;
        }

        //NEXT is an expensive operations. Instead of ArraList, use some FIFO structure
        buf.add(rxbuf.get(0));
        rxbuf.remove(0);

        p.println(rxbuf.size() + " => " + buf.size());
      }
      if (serialEventMethod != null) {
        if ((0 < bufferUntilSize && bufferUntilSize <= buf.size()) ||
          (0 == bufferUntilSize && bufferUntilByte == buf.get(buf.size()-1) )) {
          try {
            // serialEvent() is invoked in the context of the current (serial) thread
            // which means that serialization and atomic variables need to be used to
            // guarantee reliable operation (and better not draw() etc..)
            // serialAvailable() does not provide any real benefits over using
            // available() and read() inside draw - but this function has no
            // thread-safety issues since it's being invoked during pre in the context
            // of the Processing applet
            serialEventMethod.invoke(p, this);
          } 
          catch (Exception e) {
            System.err.println("Error, disabling serialEvent() for "+PORT_NAME);
            System.err.println(e.getLocalizedMessage());
            serialEventMethod = null;
          }
        }
      }
      invokeSerialAvailable = true;
    }
  }


  //NOT-IMPLEMENTED:   public void setDTR(boolean state) { }
  //NOT-IMPLEMENTED:   public void setRTS(boolean state) { }

  /**
   * 
   */
  public void stop() {

    clear();
  }

  //NOT-IMPLEMENTED:   public void write(byte[] src) { }
  //NOT-IMPLEMENTED:   public void write(int src) { }
  //NOT-IMPLEMENTED:   public void write(String src) { }


  // ========================================================================================
  //
  //                   \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
  //
  //                   /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
  //
  // ========================================================================================


  /**
   * 
   */
  public void run() {

    while (true) {

      if (p.millis()>endLapsedTime) {
        endLapsedTime=p.millis()+duration;

        //Add to simulation stream
        receiveNewIncomingData();

        if (verbosity) {
          p.println(p.millis() + " Thread called " + rxbuf.size() + " +++ " + buf.size());
          //p.printArray(rxbuf.toArray());
        }
      }

      try {
        Thread.sleep(WAIT_MS);
      }
      catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    } //WHILE
  }


  /**
   * 
   */
  public synchronized void receiveNewIncomingData() {

    byte[] data=simPort.dataGenerator();    

    synchronized (rxbuf) {

      if (verbosity) 
        p.println("New data", data.length);

      for (int c=0; c<data.length; c++)       
        rxbuf.add(data[c]);
    }
  }

  /**
   * 
   */
  void setDataSource(UserSourceSimulator port) {
    simPort=port;
  }

  /**
   * 
   */
  public void pre() {
    if (serialAvailableMethod != null && invokeSerialAvailable) {
      invokeSerialAvailable = false;
      try {
        serialAvailableMethod.invoke(p, this);
      } 
      catch (Exception e) {
        System.err.println("Error, disabling serialAvailable() for "+PORT_NAME);
        System.err.println(e.getLocalizedMessage());
        serialAvailableMethod = null;
      }
    }
    
    serialEvent(this);
    
  }

  /**
   * 
   */
  private Method findCallback(final String name) {

    try {
      //return p.getClass().getMethod(name, this.getClass());
      //return p.getClass().getMethod(name, new Class[] {SerialSimulator.class});
      return p.getClass().getMethod(name, SerialSimulator.class);
    } 
    catch (Exception e) {
    }

    // Permit callback(Object) as alternative to callback(Serial).
    try {
      //return p.getClass().getMethod(name, this.getClass());
      //return this.getClass().getMethod(name, new Class[] {SerialSimulator.class});
      return p.getClass().getMethod(name, Object.class);
    } 
    catch (Exception e) {
    }   

    return null;
  }
}
