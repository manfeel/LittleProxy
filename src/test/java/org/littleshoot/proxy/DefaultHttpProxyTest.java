package org.lastbamboo.common.http.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.junit.Test;
import org.lastbamboo.common.util.UriUtils;

/**
 * Tests the default HTTP proxy.
 */
public class DefaultHttpProxyTest
    {

    @Test public void testProxyChunkAndNo() throws Exception 
        {
        final String url = 
            "http://i.i.com.com/cnwk.1d/html/rb/js/tron/oreo.moo.rb.combined.js";
        final org.apache.commons.httpclient.HttpClient client = 
            new org.apache.commons.httpclient.HttpClient();
        client.getParams().setConnectionManagerTimeout(10);
        
        System.out.println("starting proxy");
        startHttpProxy();
        System.out.println("started proxy");
        Thread.sleep(2000);

        final byte[] baseResponse = rawResponse("i.i.com.com", 80, true);
        final byte[] proxyResponse = rawResponse("127.0.0.1", 8080, false);
        final ChannelBuffer wrappedBase = ChannelBuffers.wrappedBuffer(baseResponse);
        final ChannelBuffer wrappedProxy = ChannelBuffers.wrappedBuffer(proxyResponse);
        
        assertEquals("Lengths not equal", wrappedBase.capacity(), wrappedProxy.capacity());
        assertEquals("Not equal:\n"+
            ChannelBuffers.hexDump(wrappedBase)+"\n\n\n"+
            ChannelBuffers.hexDump(wrappedProxy), wrappedBase, wrappedProxy);
        
        final ByteArrayInputStream baseBais = new ByteArrayInputStream(baseResponse);
        final String baseStr = IOUtils.toString(new GZIPInputStream(baseBais));
        final FileWriter baseFileWriter = new FileWriter(new File("base.js"));
        baseFileWriter.write(baseStr);
        baseFileWriter.close();
        System.out.println("RESPONSE:\n"+baseStr);
        
        final ByteArrayInputStream proxyBais = new ByteArrayInputStream(proxyResponse);
        final String proxyStr = IOUtils.toString(new GZIPInputStream(proxyBais));
        final FileWriter proxyFileWriter = new FileWriter(new File("proxy.js"));
        proxyFileWriter.write(proxyStr);
        proxyFileWriter.close();
        System.out.println("RESPONSE:\n"+proxyStr);
        
        /*
        assertEquals("Not equal:\n"+
            wrappedBase.toString("UTF-8")+"\n\n\n"+
            wrappedProxy.toString("UTF-8"), wrappedBase, wrappedProxy);
        */
        
        //final String unzipped = IoUtils.inflateString(baseResponse);
        //System.out.println(unzipped);
        //assertEquals("Expected:\n"+baseResponse.subList(0, 20)+"\n but was:\n"+proxyResponse.subList(0, 20), baseResponse, proxyResponse);
        //assertEquals("Response lengths not equal!!", response.length, proxiedResponse.length);
        //assertTrue("Responses not equal!!", Arrays.equals(response, proxiedResponse));
        
        System.out.println("ALL PASSED!!");
        }
    
    private byte[] rawResponse(final String url, final int port, 
        final boolean simulateProxy) throws UnknownHostException, IOException
        {
        //final InetSocketAddress isa = new InetSocketAddress("127.0.0.1", 8080);
        final Socket sock = new Socket(url, port);
        System.out.println("Connected...");
        final OutputStream os = sock.getOutputStream();
        final Writer writer = new OutputStreamWriter(os);
        if (simulateProxy)
            {
            final String uri = "http://i.i.com.com/cnwk.1d/html/rb/js/tron/oreo.moo.rb.combined.js";
            //final String uri = "http://i.i.com.com/cnwk.1d/html/rb/js/tron/oreo.moo.rb.combined.js";
            /*
            final String uri = "http://www.google.com";
            */
            final String noHostUri = UriUtils.stripHost(uri);
            System.out.println("Using host: "+noHostUri);
            writeHeader(writer, "GET "+noHostUri+" HTTP/1.1\r\n");
            }
        else
            {
            writeHeader(writer, "GET http://i.i.com.com/cnwk.1d/html/rb/js/tron/oreo.moo.rb.combined.js HTTP/1.1\r\n");
            }
        writeHeader(writer, "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n");
        writeHeader(writer, "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\r\n");
        writeHeader(writer, "Accept-Encoding: gzip,deflate\r\n");
        writeHeader(writer, "Accept-Language: en-us,en;q=0.5\r\n");
        //writeHeader(writer, "Cookie: XCLGFbrowser=Cg8ILkmHQruNAAAAeAs; globid=1.1WJrGuYpPuQP4SL3\r\n");
        
        writeHeader(writer, "Cookie: [XCLGFbrowser=Cg8ILkmHQruNAAAAeAs; globid=1.1WJrGuYpPuQP4SL3]\r\n");
        writeHeader(writer, "Host: i.i.com.com\r\n");
        //writeHeader(writer, "Host: www.google.com\r\n");
        writeHeader(writer, "Keep-Alive: 300\r\n");
        //writeHeader(writer, "Proxy-Authorization: Basic Ym9wOmJvcA==\r\n");
        if (simulateProxy)
            {
            writeHeader(writer, "Connection: keep-alive\r\n");
            }
        else
            {
            writeHeader(writer, "Proxy-Connection: keep-alive\r\n");
            }
        writeHeader(writer, "User-Agent: Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.0.14) Gecko/2009082706 Firefox/3.0.14\r\n");
        if (simulateProxy)
            {
            final InetAddress address = InetAddress.getLocalHost();
            final String host = address.getHostName();
            final String via = "1.1 " + host;
            writeHeader(writer, "Via: "+via+"\r\n");
            }
        writeHeader(writer, "\r\n");
        writer.flush();
        
        System.out.println("READING RESPONSE HEADERS");
        final Map<String, String> headers = new HashMap<String, String>();
        StringBuilder curLine = new StringBuilder();
        final InputStream is = sock.getInputStream();
        boolean lastCr = false;
        boolean haveCrLn = false;
        while (true) 
            {
            final char curChar = (char) is.read();
            if (lastCr && curChar == '\n')
                {
                if (haveCrLn) 
                    {
                    System.out.println("GOT END OF HEADERS!!");
                    break;
                    }
                else
                    {
                    final String headerLine = curLine.toString();
                    System.out.println("READ HEADER: "+headerLine);
                    if (!headerLine.startsWith("HTTP"))
                        {
                        headers.put(
                            StringUtils.substringBefore(headerLine, ":").trim(), 
                            StringUtils.substringAfter(headerLine, ":").trim());
                        }
                    curLine = new StringBuilder();
                    haveCrLn = true;
                    }
                }
            else if (curChar == '\r')
                {
                lastCr = true;
                }
            else 
                {
                lastCr = false;
                haveCrLn = false;
                curLine.append(curChar);
                }
            }
        
        final File file = new File("test.html");
        if (file.isFile()) file.delete();
        final FileChannel fc = 
            new FileOutputStream(file).getChannel();
        
        final ReadableByteChannel src = Channels.newChannel(is);
        
        final int limit;
        if (headers.containsKey("Content-Length") && 
            !headers.containsKey("Transfer-Encoding"))
            {
            limit = Integer.parseInt(headers.get("Content-Length").trim());
            }
        else if (headers.containsKey("Transfer-Encoding"))
            {
            final String encoding = headers.get("Transfer-Encoding");
            if (encoding.trim().equalsIgnoreCase("chunked"))
                {
                return readAllChunks(is, file);
                }
            else
                {
                fail("Weird encoding: "+encoding);
                throw new RuntimeException("Weird encoding: "+encoding);
                }
            }
        else
            {
            throw new RuntimeException("Weird headers");
            }
        
        //System.out.println("READING BODY WITH LENGTH "+limit+"!");
        //final ByteArrayOutputStream bodyOs = new ByteArrayOutputStream();

        int remaining = limit;
        System.out.println("Reading body of length: "+limit);
        while (remaining > 0)
            {
            System.out.println("Remaining: "+remaining);
            final long transferred = fc.transferFrom(src, 0, remaining);
            System.out.println("Read: "+transferred);
            remaining -= transferred;
            }
        System.out.println("CLOSING CHANNEL");
        fc.close();
        //copy(is, bodyOs, limit);
        
        System.out.println("READ BODY!");
        //final byte[] bodyBytes = new byte[limit];
        //final int read = is.read(bodyBytes);
        //if (read != limit)
        //    throw new Error();
        //return bodyOs.toByteArray();
        //System.out.println("\n\n\n\n\n\n\n\n");
        //System.out.println(new GZIPInputStream(new FileInputStream(file)));
        //System.out.println(IOUtils.toString(new FileInputStream(file)));
        //System.out.println("\n\n\n\n\n\n\n\n");
        return IOUtils.toByteArray(new FileInputStream(file));
        }

    private byte[] readAllChunks(final InputStream is, final File file) throws IOException
        {
        final FileChannel fc = new FileOutputStream(file).getChannel();
        int totalTransferred = 0;
        int index = 0;
        while (true)
            {
            final int length = readChunkLength(is);
            if (length == 0)
                {
                System.out.println("GOT CHUNK LENGTH 0!!!");
                readCrLf(is);
                break;
                }
            final ReadableByteChannel src = Channels.newChannel(is);
            final long transferred = fc.transferFrom(src, index, length);
            if (transferred != length)
                {
                throw new RuntimeException("Could not read expected length!!");
                }
            index += transferred;
            totalTransferred += transferred;
            System.out.println("READ: "+transferred);
            System.out.println("TOTAL: "+totalTransferred);
            readCrLf(is);
            }
        //fc.close();
        return IOUtils.toByteArray(new FileInputStream(file));
        }

    private void readCrLf(final InputStream is) throws IOException
        {
        final char cr = (char) is.read();
        final char lf = (char) is.read();
        if (cr != '\r' || lf != '\n')
            {
            final byte[] crlf = new byte[2];
            crlf[0] = (byte) cr;
            crlf[1] = (byte) lf;
            final ChannelBuffer buf = ChannelBuffers.wrappedBuffer(crlf);
            throw new Error("Did not get expected CRLF!! Instead got hex: "+
                ChannelBuffers.hexDump(buf)+" and str: "+buf.toString("US-ASCII"));
            }
        }

    private int readChunkLength(final InputStream is) throws IOException
        {
        final StringBuilder curLine = new StringBuilder(8);
        boolean lastCr = false;
        int count = 0;
        while (true && count < 20) 
            {
            final char curChar = (char) is.read();
            count++;
            if (lastCr && curChar == '\n')
                {
                final String line = curLine.toString();
                final byte[] bytes = line.getBytes();
                final ChannelBuffer buf = ChannelBuffers.wrappedBuffer(bytes);
                System.out.println("BUF IN HEX: "+ChannelBuffers.hexDump(buf));
                if (StringUtils.isBlank(line))
                    {
                    return 0;
                    }
                final int length = Integer.parseInt(line, 16);
                System.out.println("CHUNK LENGTH: "+length);
                return length;
                //return Integer.parseInt(line);
                }
            else if (curChar == '\r')
                {
                lastCr = true;
                }
            else 
                {
                lastCr = false;
                curLine.append(curChar);
                }
            
            }
        
        throw new IOException("Reached count with current read: "+curLine.toString());
        }

    private void writeHeader(final Writer writer, final String header) 
        throws IOException
        {
        System.out.print("WRITING HEADER: "+header);
        writer.write(header);
        }

    /**
     * Tests the proxy.
     * 
     * @throws Exception If any unexpected error occurs.
     */
    public void testProxy() throws Exception
        {
        //final HttpProxy proxy = new DefaultHttpProxy();
        //proxy.start("127.0.0.7", 8999);
        final org.apache.commons.httpclient.HttpClient client = 
            new org.apache.commons.httpclient.HttpClient();
        client.getParams().setConnectionManagerTimeout(10);
        
        System.out.println("starting proxy");
        startHttpProxy();
        System.out.println("started proxy");
        Thread.sleep(2000);
        //final String response = method.getResponseBodyAsString();
        //System.out.println(response);
        final byte[] response = getResponse(client);
        
        final org.apache.commons.httpclient.HttpClient proxiedClient = new org.apache.commons.httpclient.HttpClient();
        //proxiedClient.getHostConfiguration().setProxy("127.0.0.1", 8080);
        final byte[] proxiedResponse = getResponse(proxiedClient);
        
        assertEquals("Response lengths not equal!!", response.length, proxiedResponse.length);
        assertTrue("Responses not equal!!", Arrays.equals(response, proxiedResponse));
        
        System.out.println("ALL PASSED!!");
        }

    private byte[] getResponse(final HttpClient client) throws HttpException, IOException
        {
        final String url =
            "http://littleshootimages.appspot.com/doesnotexist";
            //"http://admin.brightcove.com/viewer/us1.21.02.03/federatedVideoUI/BrightcovePlayer.swf?isUI=true&isVid=true&%40playlistTabs=ref%3A1194811622188&autoStart=&bgcolor=%23FFFFFF&flashID=myPlayer&height=225&playerID=1803302900&publisherID=1749339200&width=312&wmode=transparent";
        //final GetMethod method = new GetMethod("http://www.google.com");
        //final GetMethod method = new GetMethod(url);
        return getResponse(client, url);
        }
    
    private byte[] getResponse(final HttpClient client, final String url) throws HttpException, IOException
        {
        final GetMethod method = new GetMethod(url);
        method.getParams().setSoTimeout(20*1000);
        
        method.setRequestHeader("Accept-Encoding", "gzip,deflate");
        method.setRequestHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.0.14) Gecko/2009082706 Firefox/3.0.14");
        method.setRequestHeader("Accept", "*/*");
        method.setRequestHeader("Accept-Language", "en-us,en;q=0.5");
        method.setRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
    
        System.out.println("Executing method...");
        client.executeMethod(method);
        System.out.println(method.getStatusLine());
        final Header[] responseHeaders = method.getResponseHeaders();
        for (final Header header : responseHeaders)
            {
            System.out.print(header);
            }
        final Header ce = method.getResponseHeader("Content-Encoding");
        final InputStream is;
        if (ce != null && ce.getValue().contains("gzip"))
            {
            //is = new GZIPInputStream(method.getResponseBodyAsStream());
            is = method.getResponseBodyAsStream();
            }
        else 
            {
            is = method.getResponseBodyAsStream();
            }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(is, baos);
        //final byte[] response = IOUtils.toByteArray(); 
        
        method.releaseConnection();
        final byte[] bodyBytes = baos.toByteArray();
        final String bodyStr = new String(bodyBytes, "UTF-8");
        System.out.println(bodyStr+"'");
        return bodyBytes;
        }

    private void startHttpProxy()
        {
        // Configure the server.
        final ServerBootstrap bootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
        

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new HttpServerPipelineFactory());

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(8080));
        }

    /** 
     * Copies the {@link InputStream} to the specified {@link OutputStream}
     * for the specified number of bytes or until EOF or exception.
     * 
     * @param in The {@link InputStream} to copy from. 
     * @param out The {@link OutputStream} to copy to.
     * @param originalByteCount The number of bytes to copy.
     * @return The number of bytes written.
     * @throws IOException If there's an IO error copying the bytes.
     */
    private static long copy(final InputStream in, final OutputStream out,
        final long originalByteCount) throws IOException 
        {
        final int DEFAULT_BUFFER_SIZE = (int) originalByteCount;
        if (originalByteCount < 0)
            {
            throw new IllegalArgumentException("Invalid byte count: " + 
                    originalByteCount);
            }
        final byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
        int len = 0;
        long written = 0;
        long byteCount = originalByteCount;
        try
            {
            while (byteCount > 0)
                {
                if (byteCount < DEFAULT_BUFFER_SIZE)
                    {
                    System.out.println("Reading bytes: "+byteCount);
                    System.out.println("Available: "+in.available());
                    len = in.read(buffer, 0, (int)byteCount);
                    }
                else
                    {
                    len = in.read(buffer, 0, DEFAULT_BUFFER_SIZE);
                    }
                
                if (len == -1)
                    {
                    break;
                    }

                byteCount -= len;
                out.write(buffer, 0, len);
                written += len;
                
                System.out.println("Read: "+len+" byte count "+byteCount);
                //final String unzippedBody = IOUtils.toString(new GZIPInputStream(out));
                //System.out.println("Body so far:\n"+unzippedBody);
                if (byteCount == 0)
                    {
                    return written;
                    }
                //LOG.debug("IoUtils now written: "+written);
                }
            return written;
            }
        catch (final IOException e)
            {
            throw e;
            }
        finally
            {
            out.flush();
            }
        }
    }

