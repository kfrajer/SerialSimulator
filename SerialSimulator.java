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
  int inBuffer = 0;
  int readOffset = 0;

  int bufferUntilSize = 1;
  byte bufferUntilByte = 0;

  //----------------------
  Thread main;
  float rate;
  protected int duration;
  protected int endLapsedTime=-1;



  /**
   * @param rate Fake event generation. Units in Hz
   */
  public SerialSimulator(PApplet parent, float rate) {

    p=parent;
    p.registerMethod("dispose", this);
    p.registerMethod("pre", this);
    verbosity=false;

    buf = new ArrayList<Byte>();
    rxbuf = new ArrayList<Byte>();
    this.rate=rate;  

    Float fob= new Float((1.0f/rate)*1000);
    duration=rate>0 ? fob.intValue() : 1000; //in msecs

    serialEventMethod=findCallback("serialEvent");
    serialAvailableMethod=findCallback("serialAvailable");  //Needed????    

    (main=new Thread(this)).start();
  }

  /**
   * 
   */
  public void dispose() {

    stop();

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
  }

  /**
   * 
   */
  private Method findCallback(final String name) {
    try {
      return p.getClass().getMethod(name, this.getClass());
    } 
    catch (Exception e) {
    }
    // Permit callback(Object) as alternative to callback(Serial).
    try {
      return p.getClass().getMethod(name, Object.class);
    } 
    catch (Exception e) {
    }
    return null;
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
      inBuffer = 0;
      readOffset = 0;
    }
  }

  //public boolean getCTS() { }
  //public boolean getDSR() {}
  // public static Map<String, String> getProperties(String portName) { }


  /** 
   * <h3>Advanced</h3>
   * Same as read() but returns the very last value received
   * and clears the buffer. Useful when you just want the most
   * recent value sent over the port.
   * @webref serial:serial
   * @usage web_application
   */
  public int last() {
    if (available()==0) {
      return -1;
    }

    synchronized (buf) {
      int ret = buf.get(buf.size()-1) & 0xFF;
      buf.clear();
      inBuffer = 0;
      readOffset = 0;
      return ret;
    }
  }

  /**
   * 
   */
  public char lastChar() {
    return (char)last();
  }

  ///**
  // * Returns current simmulator
  // */
  //public static String[] list() {

  //  String[] simports = {PORT_NAME};    
  //  return simports;
  //}

  /**
   * 
   */
  public int read() {
    if (available()==0) {
      return -1;
    }

    synchronized (buf) {
      int ret = buf.get(0) & 0xFF;
      buf.remove(0);

      readOffset++;
      if (available()==0) {
        buf.clear(); //Not needed as elements are being already removed
        inBuffer = 0;
        readOffset = 0;
      }
      return ret;
    }
  }


  /**
   * @generate Serial_readBytes.xml
   * @webref serial:serial
   * @usage web_application
   */
  public byte[] readBytes() {
    if (available()==0) {
      return null;
    }

    synchronized (buf) {
      int n=buf.size();
      byte[] ret = new byte[n];

      getArray(n, ret);
      //for (int i=0; i<n; i++) {
      //  ret[i]=buf.get(i).byteValue();
      //}

      ////Remove elements in array
      //for (int i=n-1; i>=0; i--) {
      //  buf.remove(i);
      //}

      buf.clear();
      inBuffer = 0;
      readOffset = 0;

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
    if (available()==0) {
      return null;
    }

    synchronized (buf) {
      int length = buf.size();
      if (length > max) length = max;
      byte[] ret = new byte[length];

      int n=length;
      getArray(n, ret);
      //for (int i=0; i<n; i++) {
      //  ret[i]=buf.get(i).byteValue();
      //}

      ////Remove elements in array
      //for (int i=n-1; i>=0; i--) {
      //  buf.remove(i);
      //}

      readOffset += length;
      if (available()==0) {
        buf.clear();
        inBuffer = 0;
        readOffset = 0;
      }
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
    if (available()==0) {
      return 0;
    }

    synchronized (buf) {
      int toCopy = buf.size();
      if (dest.length < toCopy) {
        toCopy = dest.length;
      }

      getArray(toCopy, dest);
      //int n=toCopy;
      //for (int i=0; i<n; i++) {
      //  ret[i]=buf.get(i).byteValue();
      //}

      ////Remove elements in array
      //for (int i=n-1; i>=0; i--) {
      //  buf.remove(i);
      //}

      readOffset += toCopy;      
      if (available()==0) {
        buf.clear();
        inBuffer = 0;
        readOffset = 0;
      }
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
    if (available()==0) {
      return null;
    }

    synchronized (buf) {
      //// look for needle in buffer
      //int found = -1;
      //for (int i=readOffset; i < inBuffer; i++) {
      //  if (buffer[i] == (byte)inByte) {
      //    found = i;
      //    break;
      //  }
      //}
      //if (found == -1) {
      //  return null;
      //}
      Integer iob= new Integer(inByte);

      Byte anchor = new Byte(iob.byteValue());  //PApplet.byte(inByte));
      if (buf.contains(anchor)) {
        int idx=buf.indexOf(anchor);

        //Build returning array
        int n = idx+1;
        byte[] dest = new byte[n];

        getArray(n, dest);

        //for (int i=0; i<n; i++) {
        //  dest[i]=buf.get(i);
        //}

        ////Remove elements in array
        //for (int i=n-1; i>=0; i--) {
        //  buf.remove(i);
        //}

        readOffset += n;
        if (available()==0) {
          buf.clear();
          inBuffer = 0;
          readOffset = 0;
        }

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
    if (available()==0) {
      return 0;
    }

    synchronized (buf) {
      //// look for needle in buffer
      //int found = -1;
      //for (int i=readOffset; i < inBuffer; i++) {
      //  if (buffer[i] == (byte)inByte) {
      //    found = i;
      //    break;
      //  }
      //}
      //if (found == -1) {
      //  return 0;
      //}

      Integer iob= new Integer(inByte);

      int n = -1;
      Byte anchor = new Byte(iob.byteValue());
      if (buf.contains(anchor)) {
        int idx=buf.indexOf(anchor);
        n=idx+1;

        //Build returning array
        n = idx+1;
        //byte[] dest = new byte[toCopy];        

        //for (int i=0; i<n; i++) {
        //  dest[i]=buf.get(i);
        //}

        ////Remove elements in array
        //for (int i=n-1; i>=0; i--) {
        //  buf.remove(i);
        //}
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
      //System.arraycopy(buffer, readOffset, dest, 0, toCopy);

      readOffset += toCopy;
      if (available()==0) {
        buf.clear();
        inBuffer = 0;
        readOffset = 0;
      }
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
  public char readChar() {
    return (char) read();
  }

  /**
   * 
   */
  public String readString() {
    if (available()==0) {
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

    while (0 < (toRead = rxbuf.size())) {
      // this method can be called from the context of another thread
      synchronized (buf) {
        // read one byte at a time if the sketch is using serialEvent
        if (serialEventMethod != null) {
          toRead = 1;
        }

        //// read an array of bytes and copy it into our buffer
        //byte[] read = port.readBytes(toRead);
        //System.arraycopy(read, 0, buffer, inBuffer, read.length);
        //inBuffer += read.length;

        //NEXT is an expensive operations. Instead of ArraList, use some FIFO structure
        buf.add(rxbuf.get(0));
        rxbuf.remove(0);
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


  // public void setDTR(boolean state) { }
  // public void setRTS(boolean state) { }

  /**
   * 
   */
  public void stop() {

    buf.clear();
    inBuffer = 0;
    readOffset = 0;
  }

  // public void write(byte[] src) { }
  // public void write(int src) { }
  // public void write(String src) { }


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
          p.println(p.millis() + " Thread called " + rxbuf.size());
          p.printArray(rxbuf.toArray());
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

  public synchronized void receiveNewIncomingData() {
    customSimulationDataGen();
  }


  /**
   *  Can be overriden by user
   */
  public void customSimulationDataGen() {

    String ch = p.nfs(p.random(2), 0, 2)+"\n";

    for (int c=0; c<ch.length(); c++) {
      byte b = (byte) ch.charAt(c);
      rxbuf.add(new Byte(b));
    }
  }








  ///**
  // * 
  // */
  //public String readStringUntil(char c) {

  //  String anchor=str(c);
  //  String out;

  //  if (buf.contains(anchor)) {
  //    int idx=buf.indexOf(anchor);

  //    //Build returning array
  //    out = "";
  //    int n=idx+1;
  //    for (int i=0; i<n; i++) {
  //      out+=buf.get(i);
  //    }

  //    //Remove elements in array
  //    for (int i=n-1; i>=0; i--) {
  //      buf.remove(i);
  //    }
  //  } else
  //    return null;

  //  return out;
  //}
}
