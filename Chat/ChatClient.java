import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import interfaces.IRoomChat;
import interfaces.IServerChat;
import interfaces.IUserChat;

public class ChatClient extends UnicastRemoteObject implements IUserChat {
    IServerChat server;
    IRoomChat room;
    String serverAddress;
    String userName;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextPane messageArea = new JTextPane();
    JButton leaveButton = new JButton("Leave Room");
    JButton createRoomButton = new JButton("Create New Room");
    Random random = new Random();
    Map<String, Color> colorMap = new HashMap<>();

    public ChatClient(String serverAddress) throws RemoteException, MalformedURLException, NotBoundException {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        messageArea.setPreferredSize(new Dimension(400, 300));
        messageArea.setEditable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        panel.add(textField, BorderLayout.SOUTH);
        panel.add(leaveButton, BorderLayout.NORTH);

        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (room != null) {
                        room.sendMsg(userName, textField.getText());
                        textField.setText("");
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });

        leaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    leaveRoom();
                } catch (MalformedURLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (NotBoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        connectToServer();
        selectRoom();
    }

    private void connectToServer() {
        try {
            server = (IServerChat) Naming.lookup("rmi://" + serverAddress + ":2020/Servidor");
            userName = JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                    JOptionPane.PLAIN_MESSAGE);
            if (userName != null && !userName.isEmpty()) {
                textField.setEditable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectRoom() throws MalformedURLException, NotBoundException {
        try {
            ArrayList<String> rooms = server.getRooms();
            rooms.add("Create New Room"); // Add the option to create a new room

            String roomName = (String) JOptionPane.showInputDialog(frame, "Select or create a room:", "Room selection",
                    JOptionPane.PLAIN_MESSAGE, null, rooms.toArray(), rooms.isEmpty() ? "No rooms available" : rooms.get(0));
            if (roomName != null && !roomName.isEmpty()) {
                if (roomName.equals("Create New Room")) {
                    String newRoomName = JOptionPane.showInputDialog(frame, "Enter the name for the new room:", "New Room Creation",
                            JOptionPane.PLAIN_MESSAGE);
                    if (newRoomName != null && !newRoomName.isEmpty()) {
                        server.createRoom(newRoomName);
                        rooms = server.getRooms(); // Refresh the rooms list
                        roomName = newRoomName; // Set the newly created room as the selected room
                    } else {
                        JOptionPane.showMessageDialog(frame, "Room creation canceled.");
                        selectRoom(); // Retry room selection
                        return;
                    }
                }

                try {
                    room = (IRoomChat) Naming.lookup("rmi://" + serverAddress + ":2020/" + roomName);
                    room.joinRoom(userName, this);
                    frame.setTitle("Chatter - " + roomName);
                    textField.setEditable(true);
                } catch (NotBoundException e) {
                    JOptionPane.showMessageDialog(frame, "Room not found. Please try again.");
                    selectRoom(); // Retry room selection
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void leaveRoom() throws MalformedURLException, NotBoundException {
        try {
            if (room != null) {
                room.leaveRoom(userName);
                room = null;
                textField.setEditable(false);
                frame.setTitle("Chatter");
                messageArea.setText(""); // Clear message area when leaving the room
                selectRoom();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliverMsg(String senderName, String msg) throws RemoteException {
        StyledDocument doc = messageArea.getStyledDocument();
        Style style = messageArea.addStyle("Style", null);
        Color color = colorMap.get(senderName);
        if (color == null) {
            color = getRandomColor();
            colorMap.put(senderName, color);
        }
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), senderName + ": " + msg + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private Color getRandomColor() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return new Color(r, g, b);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        var client = new ChatClient(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
    }
}
