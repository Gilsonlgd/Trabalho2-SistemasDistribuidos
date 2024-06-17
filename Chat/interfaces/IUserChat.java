package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IUserChat extends Remote {
    void deliverMsg(String senderName, String msg) throws RemoteException;
}
