//*****************************************************************************
//*                    Licensed Materials - Property of IBM
//*
//* XXXX-XXX (C) Copyright by IBM Corp. 1997.  All Rights Reserved.
//*
//* File, Component, Release: COM/ibm/sdwb/bps/subsystem/build/packaging/parserGenerator/EntityTreeRootException.java, parserGenerator, sdwb2.2, sdwb2.2_b37
//*
//* Version: 1.3
//* 
//* Date and Time File was last checked in:       98/01/22 18:14:23
//* Date and Time File was extracted/checked out: 99/04/25 09:13:25
//* 
//*
//* Author   Defect (D) or Feature (F) and Number
//* ------   ------------------------------------
//* KS       F 821
//*
//*****************************************************************************

package com.ibm.ode.pkg.parserGenerator;

/**
 * A class representing an exception
 *
 * @version     1.3 98/01/22
 * @author      Kurt Shah
**/

public class EntityTreeRootException extends Exception
{
  private String theMessage_;

  public EntityTreeRootException()
  {
    super();
    theMessage_ = "No message provided.";
    
  }

  public EntityTreeRootException( String s )
  {
    super( s );
    theMessage_ = s;
  }

  public String toString()
  {
    return theMessage_;
  }
}
