import java.awt.BorderLayout;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import interfaces.IRoomChat;
import interfaces.IServerChat;

public class ChatServer extends UnicastRemoteObject implements IServerChat {
    private ArrayList<String> roomList;
    private Map<String, IRoomChat> rooms;

    JFrame frame = new JFrame("Chatter");
    JButton closeButton = new JButton("Close Room");

    public ChatServer() throws RemoteException {
        roomList = new ArrayList<>();
        rooms = new HashMap<>();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(closeButton, BorderLayout.CENTER);

        frame.setSize(400, 250);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        closeButton.addActionListener(e -> {
            try {
                closeRoom();
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        });
    }

    public void closeRoom() throws RemoteException {
        String roomName = (String) JOptionPane.showInputDialog(frame, "Select the room to close:", "Room selection",
                JOptionPane.PLAIN_MESSAGE, null, roomList.toArray(),
                roomList.isEmpty() ? "No rooms available" : roomList.get(0));
        if (roomName != null && !roomName.isEmpty()) {
            IRoomChat room = rooms.get(roomName);
            rooms.remove(roomName);
            roomList.remove(roomName);
            room.closeRoom();
        }
    }

    @Override
    public ArrayList<String> getRooms() {
        return roomList;
    }

    @Override
    public void createRoom(String roomName) throws RemoteException {
        if (!rooms.containsKey(roomName)) {
            RoomChat newRoom = new RoomChat(roomName);
            rooms.put(roomName, newRoom);
            roomList.add(roomName);
            try {
                Naming.rebind("rmi://localhost:2020/" + roomName, newRoom);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer();
            java.rmi.registry.LocateRegistry.createRegistry(2020).rebind("Servidor", server);
            System.out.println("Server is running...");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
