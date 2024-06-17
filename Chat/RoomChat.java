import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import interfaces.IRoomChat;
import interfaces.IUserChat;

public class RoomChat extends UnicastRemoteObject implements IRoomChat {
    private String roomName;
    private Map<String, IUserChat> userList;

    public RoomChat(String roomName) throws RemoteException {
        this.roomName = roomName;
        this.userList = new HashMap<>();
    }

    @Override
    public void sendMsg(String usrName, String msg) throws RemoteException {
        for (IUserChat user : userList.values()) {
            user.deliverMsg(usrName, msg);
        }
    }

    @Override
    public void joinRoom(String usrName, IUserChat user) throws RemoteException {
        userList.put(usrName, user);
        sendMsg("SYSTEM", usrName + " has joined the room.");
    }

    @Override
    public void leaveRoom(String usrName) throws RemoteException {
        userList.remove(usrName);
        sendMsg("SYSTEM", usrName + " has left the room.");
    }

    @Override
    public void closeRoom() throws RemoteException {
        sendMsg("SYSTEM", "Sala fechada pelo servidor.");
        userList.clear();
    }

    @Override
    public String getRoomName() throws RemoteException {
        return roomName;
    }
}
