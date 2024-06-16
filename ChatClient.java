import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Scanner;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.text.StyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import interfaces.IServerChat;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the following Chat Protocol. When the server sends
 * "SUBMITNAME" the client replies with the desired screen name. The server will
 * keep sending "SUBMITNAME" requests as long as the client submits screen names
 * that are already in use. When the server sends a line beginning with
 * "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */
public class ChatClient implements interfaces.IUserChat {
    /* Novo */
    IServerChat server;

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextPane messageArea = new JTextPane();
    Random random = new Random();
    Map<Integer, Color> colorMap = new HashMap<Integer, Color>();

    public void deliverMsg(String senderName, String msg) {
        out.println("MESSAGE:" + senderName + ":" + msg);
    }

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Return in the listener sends the textfield
     * contents to the server. Note however that the textfield is initially NOT
     * editable, and only becomes editable AFTER the client receives the
     * NAMEACCEPTED message from the server.
     */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        messageArea.setPreferredSize(new Dimension(400, 300));
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private Color getRandomColor() {
        int r = random.nextInt(256); // Valor entre 0 e 255 para o componente vermelho
        int g = random.nextInt(256); // Valor entre 0 e 255 para o componente verde
        int b = random.nextInt(256); // Valor entre 0 e 255 para o componente azul
        return new Color(r, g, b);
    }

    private Color addNewClientColor(Integer key) {
        Color newColor = getRandomColor();

        while (colorMap.containsValue(newColor)) {
            newColor = getRandomColor();
        }

        if (!colorMap.containsKey(key)) {
            colorMap.put(key, newColor);
        }

        return newColor;
    }

    private void run() throws IOException {
        try {
            var socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            try {
                server = (IServerChat) Naming.lookup("rmi://" + serverAddress + "/ChatServer");
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Apenas para testar se chamada remota está funcionando
            this.listRooms();

            StyledDocument doc = messageArea.getStyledDocument();
            Style style = messageArea.addStyle("Style", null);

            while (in.hasNextLine()) {
                var line = in.nextLine();
                int firstColonIndex = line.indexOf(":");
                int secondColonIndex = line.indexOf(":", line.indexOf(":") + 1);
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);

                    Integer key = Integer.parseInt(line.substring(firstColonIndex + 1, secondColonIndex));
                    addNewClientColor(key);
                } else if (line.startsWith("MESSAGE")) {
                    try {
                        Integer key = Integer.parseInt(line.substring(firstColonIndex + 1, secondColonIndex));
                        Color clientColor = colorMap.get(key);
                        if (clientColor == null) {
                            clientColor = addNewClientColor(key);
                        }

                        StyleConstants.setForeground(style, clientColor);
                        doc.insertString(doc.getLength(), line.substring(secondColonIndex + 1) + "\n", style);
                        StyleConstants.setForeground(style, Color.BLACK);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                } else if (line.startsWith("SYSTEM_MESSAGE_JOIN")) {
                    try {
                        StyleConstants.setForeground(style, Color.GREEN);
                        doc.insertString(doc.getLength(), line.substring(secondColonIndex + 1) + "\n", style);
                        StyleConstants.setForeground(style, Color.BLACK);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                } else if (line.startsWith("SYSTEM_MESSAGE_LEAVE")) {
                    try {
                        StyleConstants.setForeground(style, Color.RED);
                        doc.insertString(doc.getLength(), line.substring(20) + "\n", style);
                        StyleConstants.setForeground(style, Color.BLACK);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    /* Para testar a chamada remota */
    public void listRooms() throws RemoteException {
        ArrayList<String> rooms = server.getRooms();
        rooms.forEach(room -> System.out.println(room));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        var client = new ChatClient(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}