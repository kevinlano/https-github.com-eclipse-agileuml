import java.util.Vector; 

/******************************
* Copyright (c) 2003,2019 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/
/* Package: B AMN */ 

public class BBasicStatement extends BStatement
{ private String text = "skip";  // default

  public BBasicStatement(String code)
  { text = code; }

  public String toString() { return text; }

  public BStatement substituteEq(String oldE, BExpression newE)
  { return this; } 

  public BStatement simplify()
  { return this; } 

  public BStatement normalise() 
  { return this; } 

  public BStatement seq2parallel() 
  { return this; } 

  public Vector wr()
  { return new Vector(); } 

  public Vector rd()
  { return new Vector(); } 
}

