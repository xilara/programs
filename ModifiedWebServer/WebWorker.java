/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable{

   private Socket socket;

   /**
   * Constructor: must have a valid open socket
   **/
   public WebWorker(Socket s){
   
      socket = s;

   }

   /**
   * Worker thread starting point. Each worker handles just one HTTP 
   * request and then returns, which destroys the thread. This method
   * assumes that whoever created the worker created it with a valid
   * open socket object.
   **/
   
   public void run(){
   
      // website address 
      String address = "";

      // text or jpg etc. 
      String argType = "";
      
      System.err.println("Handling connection...");
      
      try {
         InputStream  is = socket.getInputStream();
         OutputStream os = socket.getOutputStream();
         
         address = readHTTPRequest(is);
         
         if (address.contains(".jpg")) 
            argType = "image/jpeg";
         else if (address.contains(".png")) 
            argType = "image/png";
         else if (address.contains(".gif")) 
            argType = "image/gif";
         else if (address.contains(".ico")) 
            argType = "image/x-icon";
         else 
            argType = "text/html";

         // send the address to the address
         writeHTTPHeader(os, argType, address);
         writeContent(os, argType, address);
         os.flush();
         socket.close();
      
      } catch (Exception e) {
         System.err.println("Output error: " + e);
      }
      
      System.err.println("Done handling connection.");
      
      return;
      
   } // of run

   /**
   * Read the HTTP request header.
   * @return address String 
   **/
   private String readHTTPRequest(InputStream is){
      
      String line;
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      
      String address = "";
      
      while (true) {
         try {
            while (!r.ready()) Thread.sleep(1);
            line = r.readLine();
            
            if(line.contains("GET ")){
               address = line.substring(4);
               for(int i = 0; i < address.length(); i++){
                  if(address.charAt(i) == ' '){
                     address = address.substring(0, i);
                  } // of if 
               } // of for 
            } // of if 
            
            System.err.println("Request line: (" + line + ")");
            if (line.length() == 0) 
               break;
         
         } catch (Exception e) {
            System.err.println("Request error: " + e);
            break;
         }
      }
      return address;
      
   } // of readHTTPRequest

   /**
   * Write the HTTP header lines to the client network connection.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   * @param address is the website address
   **/
   private void writeHTTPHeader(OutputStream os, String contentType, String address) throws Exception{
      
      Date d = new Date();
      DateFormat df = DateFormat.getDateTimeInstance();
      df.setTimeZone(TimeZone.getTimeZone("GMT-6"));
      
      String copy = '.' + address;
      File f1 = new File(copy);
      
      try{
         FileReader file = new FileReader(f1);
         BufferedReader r = new BufferedReader(file);
      }catch(FileNotFoundException e){
         System.out.println("File not found: " + address);
         os.write("HTTP/1.1 404 Error: Not Found\n".getBytes());
      }
      
      os.write("HTTP/1.1 200 OK\n".getBytes());
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Xiana's very own server\n".getBytes());
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      return;
      
   } // of writeHTTPHeader

   /**
   * Write the data content to the client network connection. This MUST
   * be done after the HTTP header has been written out.
   * @param os is the OutputStream object to write to
   * @param contentType 
   * @param address is website address
   **/
   private void writeContent(OutputStream os, String contentType, String address) throws Exception{
      
      // date info
      Date d = new Date();
      DateFormat dformat = DateFormat.getDateTimeInstance();
      dformat.setTimeZone(TimeZone.getTimeZone("GMT-6"));

	   // file contents and address copy
      String fcont = "";
      String copy = "." + address.substring(0, address.length());
      String date = dformat.format(d);
      File f1 = new File(copy);

      if (contentType.equals("text/html")) {
         try{
            
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(f);
            
            while((fcont = br.readLine()) != null) {
               os.write(fcont.getBytes());
               os.write("\n".getBytes());

               if (fcont.contains("<cs371date>")) {
                  os.write(date.getBytes());
               } // of if
               
               if (fileContent.contains("<cs371server>")) {
                  os.write("Server Name\n".getBytes());
               } // of if

             } // of while

         } // of try 

         catch(FileNotFoundException e) {
                 System.err.println("File not found: " + address);
                 os.write("<h1>Error: 404 Not found<h1>\n".getBytes());
         }//end catch
      }// end if
      
      else if (contentType.contains("image")) {

         try {
            FileInputStream imgIS = new FileInputStream(f1);
            byte img[] = new byte [(int) f1.length()];
            imgIS.read(img);
            DataOutputStream imgOS = new DataOutputStream(os);
            imgOS.write(img);
         }//end try

         catch(FileNotFoundException e) {
                 System.err.println("File not found: " + address);
                 os.write("<h1>Error: 404 Not found<h1>\n".getBytes());
         }//end catch
      
      }//end if
      
   }  // of writeContent

} // end class
