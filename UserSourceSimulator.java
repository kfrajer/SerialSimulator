
/*
  UserSourceSimulator - Class to generate data based on user definition
 
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

class UserSourceSimulator {

  protected PApplet p;

  UserSourceSimulator(PApplet parent) { 
    p=parent;
  }

  /**
   *  Can be overriden by user
   */
  public byte[] dataGenerator() {
    String ch = p.nfs(p.random(2), 0, 2)+"\n";
    return ch.getBytes();
  }
}
