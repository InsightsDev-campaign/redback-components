package org.apache.archiva.redback.components.modello.jpox;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.modello.AbstractModelloGeneratorTest;
import org.codehaus.modello.AbstractModelloJavaGeneratorTest;
import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.model.Model;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.dom4j.Attribute;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.AssertionFailedError;

public abstract class AbstractJpoxGeneratorTestCase
    extends AbstractModelloJavaGeneratorTest
{
    protected ModelloCore modello;

    protected AbstractJpoxGeneratorTestCase( String name )
    {
        super( name );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        modello = (ModelloCore) container.lookup( ModelloCore.ROLE );
    }

    protected void verifyModel( Model model, String className )
        throws IOException, ModelloException, CompilerException, CommandLineException
    {
        verifyModel( model, className, null );
    }

    protected void verifyModel( Model model, String className, String[] versions )
        throws IOException, ModelloException, CompilerException, CommandLineException
    {
        File generatedSources = new File( getTestPath( "target/" + getName() + "/sources" ) );

        File classes = new File( getTestPath( "target/" + getName() + "/classes" ) );

        FileUtils.deleteDirectory( generatedSources );

        FileUtils.deleteDirectory( classes );

        generatedSources.mkdirs();

        classes.mkdirs();

        Properties parameters = new Properties();

        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, generatedSources.getAbsolutePath() );

        parameters.setProperty( ModelloParameterConstants.VERSION, "1.0.0" );

        parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( false ) );

        modello.generate( model, "java", parameters );

        modello.generate( model, "jpox-store", parameters );

        modello.generate( model, "jpox-metadata-class", parameters );

        parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, classes.getAbsolutePath() );
        modello.generate( model, "jpox-jdo-mapping", parameters );

        if ( versions != null && versions.length > 0 )
        {
            parameters.setProperty( ModelloParameterConstants.ALL_VERSIONS, StringUtils.join( versions, "," ) );

            for ( int i = 0; i < versions.length; i++ )
            {
                parameters.setProperty( ModelloParameterConstants.VERSION, versions[i] );

                parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY,
                                        generatedSources.getAbsolutePath() );

                parameters.setProperty( ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString( true ) );

                modello.generate( model, "java", parameters );

                modello.generate( model, "jpox-store", parameters );

                parameters.setProperty( ModelloParameterConstants.OUTPUT_DIRECTORY, classes.getAbsolutePath() );
                modello.generate( model, "jpox-jdo-mapping", parameters );
            }
        }

        addDependency( "org.codehaus.modello", "modello-core" );

        addDependency( "jpox", "jpox" );
        addDependency( "javax.jdo", "jdo2-api" );
        addDependency( "org.apache.derby", "derby" );
        addDependency( "log4j", "log4j" );

        compileGeneratedSources( true );

        enhance( classes );
        
        //verifyCompiledGeneratedSources( className, getName() );
    }

    private void enhance( File classes )
        throws CommandLineException, ModelloException, IOException
    {
        Properties loggingProperties = new Properties();
        loggingProperties.setProperty( "log4j.appender.root", "org.apache.log4j.ConsoleAppender" );
        loggingProperties.setProperty( "log4j.appender.root.layout", "org.apache.log4j.PatternLayout" );
        loggingProperties.setProperty( "log4j.appender.root.layout.ConversionPattern", "%-5p [%c] - %m%n" );
        loggingProperties.setProperty( "log4j.category.JPOX", "INFO, root" );
        File logFile = new File( classes, "log4j.properties" );
        loggingProperties.store( new FileOutputStream( logFile ), "logging" );

        Commandline cl = new Commandline();

        cl.setExecutable( "java" );

        StringBuffer cpBuffer = new StringBuffer();

        cpBuffer.append( classes.getAbsolutePath() );

        for ( Iterator it = getClassPathElements().iterator(); it.hasNext(); )
        {
            cpBuffer.append( File.pathSeparator );

            cpBuffer.append( it.next() );
        }

        File enhancerJar = getDependencyFile( "jpox", "jpox-enhancer" );
        cpBuffer.append( File.pathSeparator + enhancerJar.getAbsolutePath() );
        File bcelJar = getDependencyFile( "org.apache.bcel", "bcel" );
        cpBuffer.append( File.pathSeparator + bcelJar.getAbsolutePath() );

        cl.createArgument().setValue( "-cp" );

        cl.createArgument().setValue( cpBuffer.toString() );

        cl.createArgument().setValue( "-Dlog4j.configuration=" + logFile.toURL() );

        cl.createArgument().setValue( "org.jpox.enhancer.JPOXEnhancer" );

        cl.createArgument().setValue( "-v" );

        for ( Iterator i = FileUtils.getFiles( classes, "**/*.jdo", null ).iterator(); i.hasNext(); )
        {
            cl.createArgument().setFile( (File) i.next() );
        }

        CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();

        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        System.out.println( cl );
        int exitCode = CommandLineUtils.executeCommandLine( cl, stdout, stderr );

        String stream = stderr.getOutput();

        if ( stream.trim().length() > 0 )
        {
            System.err.println( stderr.getOutput() );
        }

        stream = stdout.getOutput();

        if ( stream.trim().length() > 0 )
        {
            System.out.println( stdout.getOutput() );
        }

        if ( exitCode != 0 )
        {
            throw new ModelloException( "The JPox enhancer tool exited with a non-null exit code." );
        }
    }
    
    protected void assertAttributeEquals( Document doc, String xpathToNode, String attributeKey, String expectedValue )
    {
        if ( expectedValue == null )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using a null expected value." );
        }
    
        Attribute attribute = findAttribute( doc, xpathToNode, attributeKey );
    
        if ( attribute == null )
        {
            throw new AssertionFailedError( "Element at '" + xpathToNode + "' is missing the '" + attributeKey
                            + "' attribute." );
        }
    
        assertEquals( "Attribute value for '" + xpathToNode + "'", expectedValue, attribute.getValue() );
    }

    protected void assertElementExists( Document doc, String xpathToNode )
    {
        findElement( doc, xpathToNode );
    }

    protected void assertElementNotExists( Document doc, String xpathToNode )
    {
        if ( StringUtils.isEmpty( xpathToNode ) )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using an empty xpath." );
        }
    
        if ( doc == null )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using a null document." );
        }
    
        XPath xpath = doc.createXPath( xpathToNode );
    
        Node node = xpath.selectSingleNode( doc );
    
        if ( node != null )
        {
            throw new AssertionFailedError( "Element at '" + xpathToNode + "' should not exist." );
        }
    
        // In case node returns something other than an element.
    }

    private Element findElement( Document doc, String xpathToNode )
    {
        if ( StringUtils.isEmpty( xpathToNode ) )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using an empty xpath." );
        }
    
        if ( doc == null )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using a null document." );
        }
    
        XPath xpath = doc.createXPath( xpathToNode );
    
        Node node = xpath.selectSingleNode( doc );
    
        if ( node == null )
        {
            throw new AssertionFailedError( "Expected Node at '" + xpathToNode + "', but was not found." );
        }
    
        if ( node.getNodeType() != Node.ELEMENT_NODE )
        {
            throw new AssertionFailedError( "Node at '" + xpathToNode + "' is not an xml element." );
        }
    
        return (Element) node;
    }

    private Attribute findAttribute( Document doc, String xpathToNode, String attributeKey ) throws AssertionFailedError
    {
        if ( StringUtils.isEmpty( attributeKey ) )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using an empty attribute key." );
        }
    
        Element elem = findElement( doc, xpathToNode );
    
        Attribute attribute = elem.attribute( attributeKey );
        return attribute;
    }

    protected void assertAttributeMissing( Document doc, String xpathToNode, String attributeKey )
    {
        Attribute attribute = findAttribute( doc, xpathToNode, attributeKey );
    
        if ( attribute != null )
        {
            throw new AssertionFailedError( "Node at '" + xpathToNode + "' should not have the attribute named '"
                            + attributeKey + "'." );
        }
    }

    protected void assertNoTextNodes( Document doc, String xpathToParentNode, boolean recursive )
    {
        if ( StringUtils.isEmpty( xpathToParentNode ) )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using an empty xpath." );
        }
    
        if ( doc == null )
        {
            throw new AssertionFailedError( "Unable to assert an attribute using a null document." );
        }
    
        XPath xpath = doc.createXPath( xpathToParentNode );
    
        List nodes = xpath.selectNodes( doc );
    
        if ( ( nodes == null ) || nodes.isEmpty() )
        {
            throw new AssertionFailedError( "Expected Node(s) at '" + xpathToParentNode + "', but was not found." );
        }
    
        Iterator it = nodes.iterator();
        while ( it.hasNext() )
        {
            Node node = (Node) it.next();
    
            assertNoTextNode( "No Text should exist in '" + xpathToParentNode + "'", node, recursive );
        }
    }

    private boolean assertNoTextNode( String message, Node node, boolean recursive )
    {
        if ( node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE )
        {
            // Double check that it isn't just whitespace.
            String text = StringUtils.trim( node.getText() );
    
            if ( StringUtils.isNotEmpty( text ) )
            {
                throw new AssertionFailedError( message + " found <" + text + ">" );
            }
        }
    
        if ( recursive )
        {
            if ( node instanceof Branch )
            {
                Iterator it = ( (Branch) node ).nodeIterator();
                while ( it.hasNext() )
                {
                    Node child = (Node) it.next();
                    assertNoTextNode( message, child, recursive );
                }
            }
        }
    
        return false;
    }
}
