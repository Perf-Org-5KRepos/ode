/*******************************************************************************
 *
 *                    Licensed Materials - Property of IBM
 *
 * XXXX-XXX (C) Copyright by IBM Corp. 2002.  All Rights Reserved.
 *
 * Version: 1.1
 *
 * Date and Time File was last checked in: 5/10/03 00:35:37
 * Date and Time File was extracted/checked out: 06/04/13 16:45:20
 ******************************************************************************/
package com.ibm.ode.pkg.cmfgen;

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.File;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.xerces.parsers.SAXParser;

/**
 * This class implements the methods to handle various events generated by
 * the SAX parser while parsing the metadata files
 *
 * @author Kiran Lingutla
 * @version 1.1
 */
public class MetaDataContentHandler extends DefaultHandler {

  //holds all the information for a metadata file in the CMF format
  public StringBuffer metaDataBuffer;
  //holds the list of known CMF elements of the type "list of requisites"
  //currently there is only one such type "partInfo"
  public static Vector KNOWN_LIST_OF_REQUISITE_TYPES;

  private Locator locator;
  private CMFFileStanza fileStanza;
  private StringBuffer cmfPackageDataBuffer;
  private String elementName;
  private String attributes;
  private boolean isListOfRequisitesType;
  private static String ELEMENT_TYPE = "cmfType";

  static 
  {
    KNOWN_LIST_OF_REQUISITE_TYPES = new Vector();
    //any new list of requisite types added to the CMF should be added here also
    KNOWN_LIST_OF_REQUISITE_TYPES.addElement( new String("partInfo") );
  }

  public void setDocumentLocator(Locator locator)
  {
    this.locator = locator;
  }

  public void startDocument() throws SAXException
  {
    metaDataBuffer = new StringBuffer();
    cmfPackageDataBuffer = new StringBuffer();
    isListOfRequisitesType = false;
    elementName = "";
    attributes = "";
  }

  public void startElement(String namespaceURI, String cmfKey, 
                           String rawName, Attributes cmfAttributes)
    throws SAXException
  {
    //reset the attributes for each element
    elementName = cmfKey;
    attributes = "";
    cmfPackageDataBuffer = new StringBuffer("");
    if (cmfKey.equalsIgnoreCase( "filestanza" ) )
    {
      //Attribute sourceFile is optional, it will be null when not specified
      //but sourceDir is required
      if ((cmfAttributes.getValue( "sourceDir" ) == null) ||
          (cmfAttributes.getValue( "sourceDir" ).trim().length() == 0))
        VariablesAndMessageHandler.printErrorMessage( "MetaDataContentHandler",
                "A required value \"sourceDir\" is missing or has an invalid " +
                "value in the metadata file" );

      fileStanza = new CMFFileStanza( cmfAttributes.getValue( "sourceFile" ),
                                      cmfAttributes.getValue( "sourceDir" ) );
      // each source file can have multiple parents, the parent list
      // will be a space separated set of strings
      String parentList = cmfAttributes.getValue( "parent" );
      if ((parentList == null) || (parentList.trim().length() == 0))
        VariablesAndMessageHandler.printErrorMessage( "MetaDataContentHandler",
                "A required value \"parent\" is missing or has an invalid " +
                "value in the metadata file" );

      StringTokenizer st = new StringTokenizer( parentList, " ");
      while (st.hasMoreTokens())
      {
        insertChildAndParentIntoHashtable( st.nextToken(), 
                                     cmfAttributes.getValue( "sourceFile" ),
                                     cmfAttributes.getValue( "sourceDir" ) );
      }
    }
    else if (cmfKey.equalsIgnoreCase( "packagedata" ) )
    {
      fileStanza.createPackageDataStanza();
    }
    if (!cmfKey.equalsIgnoreCase( "filemetadata" ) &&
        !cmfKey.equalsIgnoreCase( "filestanza" ) &&
        !cmfKey.equalsIgnoreCase( "packagedata" ))
    {
      if ((cmfAttributes != null) && (cmfAttributes.getLength() != 0))
      {
        // for now, there will be only one attribute ie. cmfType for
        // each CMF entry but it has been coded this way so as to
        // allow more than one attribute in the future.
        // Each such attribute will be ; separated in the list
        String datatype = cmfAttributes.getValue( ELEMENT_TYPE );
        if ((datatype != null) && 
            (datatype.equalsIgnoreCase( "listofreqtypes" )))
        {
          isListOfRequisitesType = true;
          fileStanza.initialiseListOfReqTypesInPackageData( cmfKey );
        }
        else
        {
          for (int i=0; i<cmfAttributes.getLength(); i++)
          {
            attributes += cmfAttributes.getLocalName(i) + "=" +
                          cmfAttributes.getValue(i) + ";";
          }
        }
      }
    }
  }

  public void endElement(String namespaceURI, String localName, String rawName)
    throws SAXException
  {
    fileStanza.signalEndOfElement( localName );
    //if this is the end of an element of type listOfRequisites, then set
    //the corresponding flag to false
    if (KNOWN_LIST_OF_REQUISITE_TYPES.contains( localName ))
    {
      isListOfRequisitesType = false;
    }
    else
    {
      //do not do this if this is the end of an element of type 
      //"listOfRequisites" because it is not a simple element but a complex
      //element with several other elements. It is taken care of in
      //the CMFPackageData file
      fileStanza.insertValuesIntoPackageData( localName, attributes, 
                                              cmfPackageDataBuffer.toString(), 
                                              isListOfRequisitesType );
      //at the end of each file stanza, append the info to metaDataBuffer
      //because all the other buffers are going to be reset
      if (localName.equalsIgnoreCase( "filestanza") )
      {
        metaDataBuffer.append(fileStanza.formatToCMF());
      }
    }
  }

  public void characters(char[] ch, int start, int end)
    throws SAXException
  {
    cmfPackageDataBuffer.append( new String(ch, start, end) );
  }

  /**
   * Inserts the entityName, sourceFile and sourceDir into the hashtable
   * with each entityName mapped to a vector consisting of all its children.
   * Each such children is a combination of the sourceFile and the sourceDir
   * This is needed so as to populate immChildFiles in the product info
   * later while creating the CMF
   *
   * @param A string representing the parent which is the entityName
   * @param A string representing the sourceFile
   * @param A string representing the sourceDir
   */

  private void insertChildAndParentIntoHashtable( String parent, 
                                           String sourceFile, String sourceDir )
  {
    if (sourceFile != null)
    {
      if (sourceDir.trim().endsWith( File.separator ))
        sourceFile = sourceDir.trim() + sourceFile.trim();
      else
        sourceFile = sourceDir.trim() + File.separator + sourceFile.trim();
    }
    else
      sourceFile = sourceDir.trim();

    Vector sourceFileList;

    if (MetaDataParser.ParentToChildMapping.containsKey( parent ))
      sourceFileList = (Vector)MetaDataParser.ParentToChildMapping.get( 
                                                               parent.trim() );
    else
      sourceFileList = new Vector();

    sourceFileList.addElement( sourceFile );
    MetaDataParser.ParentToChildMapping.put( parent.trim(), sourceFileList );

  }

  public void warning(SAXParseException e)
    throws SAXException
  {
    System.out.println( "MetaDataContentHandler: " + e);
  }

  public void error(SAXParseException e)
    throws SAXException
  {
    VariablesAndMessageHandler.printException( e, "MetaDataContentHandler",
                       "An error occured during parsing");
  }

  public void fatalError(SAXParseException e)
    throws SAXException
  {
    VariablesAndMessageHandler.printException( e, "MetaDataContentHandler",
                       "A fatal error occured during parsing");
  }
}
