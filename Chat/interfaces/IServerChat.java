package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface IServerChat extends Remote {
    ArrayList<String> getRooms() throws RemoteException;
    void createRoom(String roomName) throws RemoteException;
}