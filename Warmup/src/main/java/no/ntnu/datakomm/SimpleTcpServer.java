package main.java.no.ntnu.datakomm;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A Simple TCP client, used as a warm-up exercise for assignment A4.
 */
public class SimpleTcpServer
{
    private static final int PORT = 1234;

    public static void main(String[] args)
    {
        SimpleTcpServer server = new SimpleTcpServer();
        log("Simple TCP server starting");
        server.run();
        log("ERROR: the server should never go out of the run() method! After handling one client");
    }

    public void run()
    {
        try
        {
            ServerSocket welcomeSocket = new ServerSocket(PORT);
            System.out.println("Server started on port" + PORT);

            boolean mustRun = true;
            while (mustRun)
            {
                Socket clientSocket = welcomeSocket.accept();
                while (!clientSocket.isClosed())
                {

                    InputStreamReader reader = new InputStreamReader(clientSocket.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(reader);

                    String clientInput = bufferedReader.readLine();
                    System.out.println("client sent" + clientInput);
                    // String response = calculateString(clientInput);
                    ScriptEngineManager mgr = new ScriptEngineManager();
                    ScriptEngine engine = mgr.getEngineByName("JavaScript");
                    if (clientInput.equals("game over"))
                    {
                        clientSocket.close();
                    } else
                    {
                        try
                        {
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            writer.println(engine.eval(clientInput));
                            System.out.println("server response " + engine.eval(clientInput));

                        } catch (ScriptException e)
                        {
                            System.out.println("script exception " + e);
                            System.out.println("returns ERROR to client");
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            writer.println("ERROR");

                        }
                    }


                }
            }
            welcomeSocket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }


        // TODO - implement the logic of the server, according to the protocol.
        // Take a look at the tutorial to understand the basic blocks: creating a listening socket,
        // accepting the next client connection, sending and receiving messages and closing the connection
    }

    /**
     * Log a message to the system console.
     *
     * @param message The message to be logged (printed).
     */
    private static void log(String message)
    {
        System.out.println(message);
    }

    private String calculateString(String stringToCalculate)
    {
        String returnStatement = null;
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        try
        {
            if (engine.eval(stringToCalculate) instanceof String)
            {
                returnStatement = (String) engine.eval(stringToCalculate);
            }
        } catch (ScriptException e)
        {
            e.printStackTrace();
            returnStatement = "ERROR";
        }
        return returnStatement;
    }
}
