import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import interfaces.IRoomChat;
import interfaces.IServerChat;

public class ChatServer extends UnicastRemoteObject implements IServerChat {
    private ArrayList<String> roomList;
    private Map<String, IRoomChat> rooms;

    public ChatServer() throws RemoteException {
        roomList = new ArrayList<>();
        rooms = new HashMap<>();
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
