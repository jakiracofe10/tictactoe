package com.example.tictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TTTActivity extends ActionBarActivity {

    // TAG for logging
    private static final String TAG = "TTTActivity";
    public String groupName;

    // server to connect to
    protected static final int GROUPCAST_PORT = 22220;
    protected static final String GROUPCAST_SERVER = "ec2-52-32-115-14.us-west-2.compute.amazonaws.com";

    // networking
    Socket socket = null;
    BufferedReader in = null;
    PrintWriter out = null;
    boolean connected = false;

    // UI elements
    Button board[][] = new Button[3][3];
    Button bConnect = null;
    EditText etName = null;
    TextView connecting = null;
    boolean Player1 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ttt);

        // find UI elements defined in xml
        bConnect = (Button) this.findViewById(R.id.bConnect);
        etName = (EditText) this.findViewById(R.id.etName);
        board[0][0] = (Button) this.findViewById(R.id.b00);
        board[0][1] = (Button) this.findViewById(R.id.b01);
        board[0][2] = (Button) this.findViewById(R.id.b02);
        board[1][0] = (Button) this.findViewById(R.id.b10);
        board[1][1] = (Button) this.findViewById(R.id.b11);
        board[1][2] = (Button) this.findViewById(R.id.b12);
        board[2][0] = (Button) this.findViewById(R.id.b20);
        board[2][1] = (Button) this.findViewById(R.id.b21);
        board[2][2] = (Button) this.findViewById(R.id.b22);
        connecting = (TextView) findViewById(R.id.tvConnecting);

        // hide login controls
        hideLoginControls();

        // make the board non-clickable
        disableBoardClick();

        // hide the board
        hideBoard();

        // assign OnClickListener to connect button
        bConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String name = etName.getText().toString();
                // sanitity check: make sure that the name does not start with an @ character
                if (name == null || name.startsWith("@")) {
                    Toast.makeText(getApplicationContext(), "Invalid name",
                            Toast.LENGTH_SHORT).show();
                } else {
                    send("NAME,"+etName.getText());
                }
            }
        });


        // assign a common OnClickListener to all board buttons
        View.OnClickListener boardClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int x, y;
                switch (v.getId()) {
                    case R.id.b00:
                        x = 0;
                        y = 0;
                        ifClicked(x,y);
                        ifTurn(Player1);
                        break;
                    case R.id.b01:
                        x = 0;
                        y = 1;
                        ifClicked(x, y);
                        ifTurn(Player1);
                        break;
                    case R.id.b02:
                        x = 0;
                        y = 2;
                        ifClicked(x, y);
                        ifTurn(Player1);
                        break;
                    case R.id.b10:
                        x = 1;
                        y = 0;
                        ifClicked(x, y);
                        ifTurn(Player1);
                        break;
                    case R.id.b11:
                        x = 1;
                        y = 1;
                        ifClicked(x, y);
                        ifTurn(Player1);
                        break;
                    case R.id.b12:
                        x = 1;
                        y = 2;
                        ifClicked(x,y);
                        ifTurn(Player1);
                        break;
                    case R.id.b20:
                        x = 2;
                        y = 0;
                        ifClicked(x,y);
                        ifTurn(Player1);
                        break;
                    case R.id.b21:
                        x = 2;
                        y = 1;
                        ifClicked(x, y);
                        ifTurn(Player1);
                        break;
                    case R.id.b22:
                        x = 2;
                        y = 2;
                        ifClicked(x, y);
                        ifTurn(Player1);
                        break;

                    default:
                        break;

                }
            }
        };


        // assign OnClickListeners to board buttons
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                board[x][y].setOnClickListener(boardClickListener);


        // start the AsyncTask that connects to the server
        // and listens to whatever the server is sending to us
        connect();
    }

    public void ifClicked(int x, int y) {
        int totalX = 0;
        if (board[x][y].getText()=="") {
            board[x][y].setText("X");
            send("MSG," + groupName + "," + String.valueOf(x) + String.valueOf(y));}
        if (isFull()) {
            connecting.setVisibility(View.VISIBLE);
            connecting.setText("GAME OVER. \n Disconnecting...");
            send("BYE");
            disconnect();}
        else{
            return;}}



    public boolean ifMarked(int x, int y) {
        if (board[x][y].getText().toString()=="X" || board[x][y].getText().toString()=="O") {
            return true;}
        else {
            return false;}}


    public void ifTurn(boolean Player1) {
        int totalMarked= 0;
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                if (ifMarked(a,b)) {
                    totalMarked = totalMarked + 1;}}}
        if (Player1) {
            if (totalMarked%2 == 1) {
                enableBoardClick();
                return;}
            else {
                disableBoardClick();
                return;}}
        else {
            if (totalMarked%2 == 0) {
                enableBoardClick();}
            else {
                disableBoardClick();}}}


    public boolean isFull() {
        int totalMarked = 0;
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                if (ifMarked(a,b)) {
                    totalMarked = totalMarked + 1;}}}
        if (totalMarked == 9) {
            return true;}
        else {
            return false;}}



    public void markBoard(String xy) {
        Log.i(TAG,"Is player1: "+Player1);
        int x = Integer.valueOf(xy.substring(0, 1));
        int y = Integer.valueOf(xy.substring(1));
        if (!isFull() && board[x][y].getText().toString()==""){
            Log.i(TAG,"is full:"+isFull());
            Log.i(TAG,"is marked:"+(board[x][y].getText().toString()==""));
            board[x][y].setText("O");}
        ifTurn(Player1);}




    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy called");
        disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle menu click events
        if (item.getItemId() == R.id.exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ttt, menu);
        return true;
    }




    /***************************************************************************/
    /********* Networking ******************************************************/
    /***************************************************************************/

    /**
     * Connect to the server. This method is safe to call from the UI thread.
     */
    void connect() {

        new AsyncTask<Void, Void, String>() {

            String errorMsg = null;

            @Override
            protected String doInBackground(Void... args) {
                Log.i(TAG, "Connect task started");
                try {
                    connected = false;
                    socket = new Socket(GROUPCAST_SERVER, GROUPCAST_PORT);
                    Log.i(TAG, "Socket created");
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream());

                    connected = true;
                    Log.i(TAG, "Input and output streams ready");

                } catch (UnknownHostException e1) {
                    errorMsg = e1.getMessage();
                } catch (IOException e1) {
                    errorMsg = e1.getMessage();
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
                Log.i(TAG, "Connect task finished");
                return errorMsg;
            }

            @Override
            protected void onPostExecute(String errorMsg) {
                if (errorMsg == null) {
                    Toast.makeText(getApplicationContext(),
                            "Connected to server", Toast.LENGTH_SHORT).show();

                    hideConnectingText();
                    showLoginControls();

                    // start receiving
                    receive();

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                    // can't connect: close the activity
                    finish();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Start receiving one-line messages over the TCP connection. Received lines are
     * handled in the onProgressUpdate method which runs on the UI thread.
     * This method is automatically called after a connection has been established.
     */

    void receive() {
        AsyncTask<Void, String, Void> voidStringVoidAsyncTask = new AsyncTask<Void, String, Void>() {

            @Override
            protected Void doInBackground(Void... args) {
                Log.i(TAG, "Receive task started");
                try {
                    while (connected) {

                        String msg = in.readLine();

                        if (msg == null) { // other side closed the
                            // connection
                            break;
                        }
                        publishProgress(msg);
                    }

                } catch (UnknownHostException e1) {
                    Log.i(TAG, "UnknownHostException in receive task");
                } catch (IOException e1) {
                    Log.i(TAG, "IOException in receive task");
                } finally {
                    connected = false;
                    try {
                        if (out != null)
                            out.close();
                        if (socket != null)
                            socket.close();
                    } catch (IOException e) {
                    }
                }
                Log.i(TAG, "Receive task finished");
                return null;
            }

            @Override
            protected void onProgressUpdate(String... lines) {
                // the message received from the server is
                // guaranteed to be not null
                String msg = lines[0];

                // TODO: act on messages received from the server
                if (msg.startsWith("+OK,NAME")) {
                    hideLoginControls();
                    showBoard();
                    send("LIST,GROUPS");
                    return;
                }

                if (msg.startsWith("+ERROR,NAME")) {
                    Toast.makeText(getApplicationContext(), msg.substring("+ERROR,NAME,".length()), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (msg.startsWith("+OK,JOIN")) {
                    hideLoginControls();
                    showBoard();
                    return;
                }

                if (msg.startsWith("+ERROR,JOIN")) {
                    Toast.makeText(getApplicationContext(), msg.substring("+ERROR,JOIN,".length()), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (msg.startsWith("+OK,QUIT")) {
                    return;
                }

                if (msg.startsWith("+ERROR,QUIT")) {
                    Toast.makeText(getApplicationContext(), msg.substring("+ERROR,QUIT,".length()), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (msg.startsWith("+MSG,")) {
                    String msg2 = msg.substring(msg.indexOf("@"));
                    String msg3 = msg2.substring(msg2.indexOf(",")+1);
                    markBoard(msg3);
                    if (isFull()) {
                        connecting.setVisibility(View.VISIBLE);
                        connecting.setText("GAME OVER. \n Disconnecting...");
                        disconnect();
                        send("BYE");
                    }
                    return;
                }

                if (msg.startsWith("+OK,MSG")) {
                    return;
                }

                if (msg.startsWith("+ERROR,MSG")) {
                    Toast.makeText(getApplicationContext(), msg.substring("+ERROR,MSG,".length()), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (msg.startsWith("+OK,VERSION")) {
                    return;
                }


                if (msg.startsWith("+OK,LIST,USERS")) {
                    return;
                }

                if (msg.startsWith("+ERROR,LIST,USERS")) {
                    Toast.makeText(getApplicationContext(), msg.substring("+ERROR,LIST,USERS,".length()), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (msg.startsWith("+OK,LIST,GROUPS")) {
                    String groups = msg.substring(16);
                    if (groups.isEmpty()) {
                        Random rand = new Random();
                        int n = rand.nextInt(10000);
                        groupName = "@group_" + n;
                        send("JOIN," + groupName + ",2");
                        Player1 = true;
                        return;
                    } else {
                        String groups2 = groups.substring(0, groups.indexOf(")"));
                        while (!groups2.isEmpty()) {
                            groupName = groups2.substring(0, groups2.indexOf("("));
                            String num = groups2.substring(groups2.indexOf("(")+1);
                            String num1 = num.substring(0, num.indexOf("/"));
                            String num2 = num.substring(num.indexOf("/")+1);
                            int current = Integer.parseInt(num1);
                            int max = Integer.parseInt(num2);
                            if (current!=max) {
                                send("JOIN," + groupName + "," + max);
                                if (current==0) {
                                    Player1 = true;}
                                else {
                                    enableBoardClick();
                                    Player1 = false;}
                                return;}
                            else {
                                if (groups.contains(",")) {
                                    groups = groups.substring(groups.indexOf(",")+1);}
                                else {
                                    Random rand = new Random();
                                    int n = rand.nextInt(10000);
                                    groupName = "@group_" + n;
                                    send("JOIN," + groupName + ",2");
                                    Player1 = true;
                                    return;}}}}
                    return;
                }


                if (msg.startsWith("+OK,LIST,MYGROUPS")) {
                    return;
                }


                if (msg.startsWith("+OK,BYE")) {
                    return;
                }

                // [ ... and so on for other kinds of messages]

                // if we haven't returned yet, tell the user that we have an unhandled message
                Toast.makeText(getApplicationContext(), "Unhandled message: " + msg, Toast.LENGTH_SHORT).show();
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    /**
     * Disconnect from the server
     */
    void disconnect() {
        new Thread() {
            @Override
            public void run() {
                if (connected) {
                    connected = false;
                }
                // make sure that we close the output, not the input
                if (out != null) {
                    out.print("BYE");
                    out.flush();
                    out.close();
                }
                // in some rare cases, out can be null, so we need to close the socket itself
                if (socket != null)
                    try { socket.close();} catch(IOException ignored) {}

                Log.i(TAG, "Disconnect task finished");
            }
        }.start();
    }

    /**
     * Send a one-line message to the server over the TCP connection. This
     * method is safe to call from the UI thread.
     *
     * @param msg
     *            The message to be sent.
     * @return true if sending was successful, false otherwise
     */
    boolean send(String msg) {
        if (!connected) {
            Log.i(TAG, "can't send: not connected");
            return false;
        }

        new AsyncTask<String, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(String... msg) {
                Log.i(TAG, "sending: " + msg[0]);
                out.println(msg[0]);
                return out.checkError();
            }

            @Override
            protected void onPostExecute(Boolean error) {
                if (!error) {
                    Toast.makeText(getApplicationContext(),
                            "Message sent to server", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error sending message to server",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);

        return true;
    }

    /***************************************************************************/
    /***** UI related methods **************************************************/
    /***************************************************************************/

    /**
     * Hide the "connecting to server" text
     */
    void hideConnectingText() {
        findViewById(R.id.tvConnecting).setVisibility(View.GONE);
    }

    /**
     * Show the "connecting to server" text
     */
    void showConnectingText() {
        findViewById(R.id.tvConnecting).setVisibility(View.VISIBLE);
    }

    /**
     * Hide the login controls
     */
    void hideLoginControls() {
        findViewById(R.id.llLoginControls).setVisibility(View.GONE);
    }

    /**
     * Show the login controls
     */
    void showLoginControls() {
        findViewById(R.id.llLoginControls).setVisibility(View.VISIBLE);
    }

    /**
     * Hide the tictactoe board
     */
    void hideBoard() {
        findViewById(R.id.llBoard).setVisibility(View.GONE);
    }

    /**
     * Show the tictactoe board
     */
    void showBoard() {
        findViewById(R.id.llBoard).setVisibility(View.VISIBLE);
    }


    /**
     * Make the buttons of the tictactoe board clickable if they are not marked yet
     */
    void enableBoardClick() {
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                if ("".equals(board[x][y].getText().toString()))
                    board[x][y].setEnabled(true);
    }

    /**
     * Make the tictactoe board non-clickable
     */
    void disableBoardClick() {
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                board[x][y].setEnabled(false);
    }


}
