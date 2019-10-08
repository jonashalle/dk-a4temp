package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;

import java.util.LinkedList;
import java.util.List;

public class TCPClient
{
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port)
    {
        boolean connected = false;
        try
        {

            connection = new Socket(host, port);
            toServer = new PrintWriter(connection.getOutputStream(), true);
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connected = true;
            System.out.println("Socket successfully connected");
        } catch (IOException e)
        {
            connection = null;
            System.out.println("Socket error " + e.getMessage());
        }
        //  Step 1: implement this method
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        return connected;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect()
    {
        if (isConnectionActive())
        {
            try
            {
                connection.close();
                onDisconnect();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        //  Step 4: implement this method
        // Hint: remember to check if connection is active
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive()
    {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd)
    {

        boolean returnStatement = false;
        if (isConnectionActive())
        {


            toServer.println(cmd);
            returnStatement = true;


        }

        return returnStatement;

        // Step 2: Implement this method
        // Hint: Remember to check if connection is active
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message)
    {
        boolean returnStatement = sendCommand("msg " + message);

        // Step 2: implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        return returnStatement;

    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username)
    {
        sendCommand("login " + username);
        //  Step 3: implement this method
        // Hint: Reuse sendCommand() method
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList()
    {
        sendCommand("users ");
        //  Step 5: implement this method
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message)
    {
        boolean returnStatement = sendCommand("privmsg " + recipient + " " + message);
        //  Step 6: Implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        return returnStatement;
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands()
    {
        sendCommand("help ");
        //  Step 8: Implement this method
        // Hint: Reuse sendCommand() method
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse()
    {
        String serverResponse = null;
        try
        {
            serverResponse = fromServer.readLine();
        } catch (IOException e)
        {
            System.out.println("waitServerResponse error" + e);
            disconnect();
        }
        if (serverResponse == null)
        {
            disconnect();
        }
        //  Step 3: Implement this method
        // Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.

        return serverResponse;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError()
    {
        if (lastError != null)
        {
            return lastError;
        } else
        {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread()
    {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() ->
        {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands()
    {
        while (isConnectionActive())
        {

            String[] serverResponse = waitServerResponse().toLowerCase().split(" ", 2);
            String commandWord = serverResponse[0];
            String[] serverResponseMsg;
            String sender = "";
            String message = "";
            if (serverResponse.length > 1)
            {
                serverResponseMsg = serverResponse[1].split(" ", 2);
                sender = serverResponseMsg[0];
                message = serverResponseMsg[1];
            }
            System.out.println(commandWord);
            switch (commandWord)
            {
                case "loginok":
                    onLoginResult(true, null);
                    break;
                case "loginerr":
                    onLoginResult(false, serverResponse[1]);
                    break;
                case "users":
                    onUsersList(serverResponse[1].split(" "));
                    break;

                case "supported":
                    onSupported(serverResponse[1].split(" "));
                    break;

                case "msg":

                    onMsgReceived(false, sender, message);
                    break;

                case "privmsg":

                    onMsgReceived(true, sender, message);
                    break;

                case "msgerror":
                    onMsgError(serverResponse[1]);
                    break;

                case "cmderr":
                    onCmdError(serverResponse[1]);
                    break;


            }
            // TStep 3: Implement this method
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method

            // T Step 5: update this method, handle user-list response from the server
            // Hint: In Step 5 reuse onUserList() method

            // Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
            // Step 7: add support for incoming message errors (type: msgerr)
            // Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners

            //  Step 8: add support for incoming supported command list (type: supported)

        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener)
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener)
    {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg)
    {
        for (ChatListener l : listeners)
        {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect()
    {
        for (ChatListener l : listeners)
        {
            l.onDisconnect();
        }
        // Step 4: Implement this method
        // Hint: all the onXXX() methods will be similar to onLoginResult()
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users)
    {
        for (ChatListener l : listeners)
        {
            l.onUserList(users);
        }
        // Step 5: Implement this method
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text)
    {
        TextMessage textMessage = new TextMessage(sender, priv, text);
        for (ChatListener l : listeners)
        {
            l.onMessageReceived(textMessage);
        }
        //  Step 7: Implement this method
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg)
    {
        for (ChatListener l : listeners)
        {
            l.onMessageError(errMsg);
        }
        //  Step 7: Implement this method
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg)
    {
        for (ChatListener l : listeners)
        {
            l.onCommandError(errMsg);
        }
        //  Step 7: Implement this method
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands)
    {
        for (ChatListener l : listeners)
        {
            l.onSupportedCommands(commands);
        }
        //  Step 8: Implement this method
    }
}
