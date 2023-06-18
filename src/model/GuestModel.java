/*
model:
case 0: s- 0|a,5.d,8.l,1......
        c - gets seven tiles at the beginning of the game sends nothing back

case 1: c - 1|word(not full)|row|col|v/h|name for the word it wants to enter or 1|xxx|name if wants to pass

case 2:s - there are three options for response: a - query and try... were good: "2|true|score|a,1.b2....|name|word(not full)|row|col|v\h"
                                                  b - only query was good: "2|true|0|name"
                                                  c - query returned false: "2|false|name"
       c - if query false challenge option presented:for challenge sends  3|c|word(not full)|row|col|v/h|name for passing sends 3|xxx|word(not full)|row|col|v/h|name to host

       *** in case 2 we get from host words that were entered and put onto the board by other users in order to put into other players board and maintain an updated board for all

case 3: s - challenge request answer, again three options: a - challenge and try... were good: "3|true|score|a,1.b2....|name|word(not full)|row|col|v\h"
                                                  b - only challenge was good: "3|true|0|name"
                                                  c - challenge returned false: "3|false|name"
case 4 : winner announcement
 */


package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class GuestModel extends Observable implements GameModel{

    private Player guest_player;
    private String[][] board;
    private Map<String,String> letterToScore;
    private String request_to_server;
    private String message;
    boolean gameRunning;
    private List<Observer> myObservers;

    public GuestModel(String name, String ip, int port){
        this.myObservers = new ArrayList<>();
        this.guest_player = new Player();
        this.setName(name);
        this.board = new String[15][15];
        this.message = null;
        this.gameRunning = true;
        this.request_to_server = null;
        this.letterToScore = new HashMap<>();
        letterToScore.put("A","1");
        letterToScore.put("B","3");
        letterToScore.put("C","3");
        letterToScore.put("D","2");
        letterToScore.put("E","1");
        letterToScore.put("F","4");
        letterToScore.put("G","2");
        letterToScore.put("H","4");
        letterToScore.put("I","1");
        letterToScore.put("J","8");
        letterToScore.put("K","5");
        letterToScore.put("L","1");
        letterToScore.put("M","3");
        letterToScore.put("N","1");
        letterToScore.put("O","1");
        letterToScore.put("P","3");
        letterToScore.put("Q","10");
        letterToScore.put("R","1");
        letterToScore.put("S","1");
        letterToScore.put("T","1");
        letterToScore.put("U","1");
        letterToScore.put("V","4");
        letterToScore.put("W","4");
        letterToScore.put("X","8");
        letterToScore.put("Y","4");
        letterToScore.put("Z","10");
        new Thread(()-> {
            this.connectToServer(ip,port);
        }).start();
    }

    @Override
    public String getName() {
        System.out.println("guest name from getName: " + this.guest_player.name);
        return this.guest_player.name;
    }

    @Override
    public int getScore() {
        return this.guest_player.getScore();
    }

    @Override
    public List<String> getTiles() {
        return this.guest_player.strTiles;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public void setUserQueryInput(String word, String row, String col, String vertical) {
        this.guest_player.wordDetails[0] = word;
        if(!word.equals("xxx") && !word.equals("XXX")) {
            this.guest_player.wordDetails[1] = row;
            this.guest_player.wordDetails[2] = col;
            this.guest_player.wordDetails[3] = vertical;
        }
        else {
            this.setMessage("turn over");
            this.guest_player.wordDetails[1] = "null";
            this.guest_player.wordDetails[2] = "null";
            this.guest_player.wordDetails[3] = "null";
        }

        request_to_server = "1|" + this.guest_player.wordDetails[0] + "|" +  this.guest_player.wordDetails[1] + "|" + this.guest_player.wordDetails[2] + "|" + this.guest_player.wordDetails[3] + "|" + this.getName();
        write_to_server(this.request_to_server,this.getMySocket());
    }

    @Override
    public void setUserChallengeInput(String request) {
        if(request.equals("c") || request.equals("C"))
        {
            this.request_to_server = "3" + "|" + "C" + "|" + this.guest_player.wordDetails[0] + "|"+  this.guest_player.wordDetails[1] + "|" + this.guest_player.wordDetails[2] + "|" + this.guest_player.wordDetails[3] + "|" + this.getName();
            write_to_server(request_to_server,this.getMySocket()); // request challenge or not
        }
        else
        {
            this.setMessage("turn over");
            write_to_server(request_to_server,this.getMySocket()); // request challenge or not
        }
    }

    @Override
    public String[][] getBoard() {
        return this.board;
    }

    public void setMessage(String msg)
    {
        this.message = msg; // messages to the user and requests for input
        this.notifyObserver("message");
    }

    public void setName(String name)
    {
        this.guest_player.setName(name);
    }

    public Socket getMySocket() {

        return this.guest_player.socket;
    }

    public void setMySocket(Socket socket) {

        this.guest_player.socket = socket;
    }

    public void decreaseScore(int num)
    {
        this.guest_player.decreaseScore(num);
        this.notifyObserver("score");
    }

    public void addScore(int num)
    {
        this.guest_player.addScore(num);
        this.notifyObserver("score");
    }

    private void notifyObserver(String change) {
        setChanged();
        for(Observer obz: myObservers)
        {
            obz.update(this, change);
        }
    }
    @Override
    public void addObserver(Observer obz)
    {
        this.myObservers.add(obz);
    }

    public void addTiles(String addedTiles) // received all 7 tiles
    {
        this.guest_player.strTiles.clear();
        String[] moreTiles = addedTiles.split("[.]");
        for(String s : moreTiles)
        {
            if(!s.equals("."))
                this.guest_player.strTiles.add(s);
        }
        this.notifyObserver("tiles");
        for(String s : guest_player.strTiles)
            System.out.println(s);
    }

    public void updateMatrixBoard(String word, String row, String col, String vertical) {
        if(vertical.equals("vertical"))
        {
            for(int i = 0; i < word.length(); i++)
            {
                if(word.charAt(i) != '_')
                {
                    this.board[Integer.parseInt(row) + i][Integer.parseInt(col)] = String.valueOf(word.charAt(i)) + "," + letterToScore.get(String.valueOf(word.charAt(i)));
                }
            }
        }
        else // "h"
        {
            for(int i = 0; i < word.length(); i++)
            {
                if(word.charAt(i) != '_')
                {
                    this.board[Integer.parseInt(row)][Integer.parseInt(col) + i] = String.valueOf(word.charAt(i)) + "," + letterToScore.get(String.valueOf(word.charAt(i)));
                }
            }
        }
        this.notifyObserver("board");
    }


    public void write_to_server(String str, Socket server_socket){
        try {
            PrintWriter outToServer = new PrintWriter(server_socket.getOutputStream());
            outToServer.println(str);
            outToServer.flush();
            //outToServer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String read_from_server(Socket server_socket) {
        try {
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
            String serverResponse = inFromServer.readLine();
            //inFromServer.close();
            return serverResponse;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectToServer(String ip, int port){
        try {
            Socket hostServer =new Socket(ip,port);
            this.setMessage("Connection established successfully!");
            this.setMySocket(hostServer);
            this.startGuestGame();
        } catch (IOException e) {
            this.setMessage("connection to remote game failed");
            throw new RuntimeException(e);
        }
    }
    public void case0(String fromHost) // got 7 tiles
    {
        this.notifyObserver("name");
        this.setMessage("game started, you got seven tiles, wait for your turn");
        addTiles(fromHost);
    }

    public void serverWordResponse(String[] fromHost) //my word failed"2|true|0|null|name"
    {
        for(String s : fromHost)
            System.out.println(s);
        if(fromHost[1].equals("true") && !fromHost[4].equals(this.guest_player.name)) // other users word was placed on board
        {
            wordEnteredByOtherUser(fromHost);
            //this.notifyObserver("board");
            this.setMessage(fromHost[4] + " put a new word in the board");
        }

        else if(fromHost[1].equals("true") && fromHost[4].equals(this.guest_player.name)) // received:"2|true|score|a,1^b2^...|name|word(not full)|row|col|v\h"
        {
            wordEnteredByMe(fromHost);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.setMessage("turn over");
        }
        else // my query request returned false received:"2|false|null|null|name"
        {
            this.setMessage("challenge?");
        }
    }

    public void challengeResponse(String[] fromHost)
    {
        if(fromHost[1].equals("true"))
        {
            challengeTrue(fromHost);
        }
        else // challenge wasn't correct - deduce points received: "3|false|name"
        {
            this.setMessage("challenge returned false, you lose 10 points");
            this.decreaseScore(10);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        this.setMessage("turn over");
    }

    public void gameOver(String fromHost)
    {
        // TODO - make into a popup screen
        this.setMessage(fromHost + ", Game Over"); // winner message
        this.gameRunning = false;

        try{
            this.guest_player.socket.close();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void wordEnteredByOtherUser(String[] fromHost)
    {
        System.out.println("updating word by other user");
        updateMatrixBoard(fromHost[5],fromHost[6],fromHost[7],fromHost[8]);
    }

    public void wordEnteredByMe(String[] fromHost)
    {
        if(!fromHost[2].equals("0")) // my word was put into board
        {
            this.setMessage( "your word was placed on the board, you get " + fromHost[2] + " points");
            updateMatrixBoard(fromHost[5],fromHost[6],fromHost[7],fromHost[8]);
            this.addScore(Integer.parseInt(fromHost[2]));

            this.addTiles(fromHost[3]); // add tiles received from the server replacing those used in the word add to board (received all of my tiles)
        }
        else //"2|true|0|name"
            this.setMessage("wrong word or placement, you get 0 points, turn over");
    }

    public void challengeTrue(String[] fromHost)
    {
        System.out.println("challenge true string:");
        for(String s : fromHost)
        {
            System.out.println(s);
        }
        if(!fromHost[2].equals("0")) // challenge and tryplaceword correct, received: "3|true|score|a,1^b2^...|name|word(not full)|row|col|v\h"
        {
            int tmp_score = Integer.parseInt(fromHost[2]) + 10;
            this.setMessage( "your word was placed on the board, you get " + tmp_score + " points");
            updateMatrixBoard(fromHost[5],fromHost[6],fromHost[7],fromHost[8]);
            this.addScore(tmp_score);
            this.addTiles(fromHost[3]);
        }
        else // only challenge correct received: "3|true|0|name"
        {
            this.setMessage("challenge returned true but word couldn't be put into board. turn over");
        }
    }

    public void printboard() {
        System.out.println("the board is: ");
        System.out.print("  ");
        for (int k = 0; k < 15; k++) {
            System.out.print(" " + k + " ");
        }
        System.out.println("");
        for (int i = 0; i < 15; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < 15; j++) {
                if (this.board[i][j] != null) {
                    if (j < 11) {
                        System.out.print(" " + this.board[i][j] + " ");
                    } else {
                        System.out.print("  " + this.board[i][j] + " ");
                    }

                } else {
                    if (j < 11) {
                        System.out.print(" _ ");
                    } else {
                        System.out.print("  _ ");
                    }
                }
            }
            System.out.println("");
        }
    }

    public void startGuestGame() {
        this.setMessage("please wait for Host to start the game");
        while (gameRunning){
            // reading from server
            System.out.println("guest model loop");
            String readfromHost = this.read_from_server(this.getMySocket());
            //if(readfromHost != null)
            //  {
            String[] fromHost = readfromHost.split("[|]");
            switch (fromHost[0]){
                case "0": // seven tiles at the start of the game
                    case0(fromHost[1]);
                    break;
                case "1": // my turn
                    this.setMessage("your turn");
                    break;//send: 1|word(not full)|row|col|v/h|name or 1|xxx|name

                case "2": // host + server response to query request ++ updated board for any entered word
                    System.out.println("guest model - entered query case");
                    serverWordResponse(fromHost);
                    break; // if user wants to challenge or not, send: 3|c/xxx|word(not full)|row|col|v/h|name

                case "3": // host + server response to challenge request
                    System.out.println("guest model - entered challenge case");
                    challengeResponse(fromHost);
                    break;

                case "4": // game over
                    gameOver(fromHost[1]);
                    break;
            }
            // }
        }
    }
}