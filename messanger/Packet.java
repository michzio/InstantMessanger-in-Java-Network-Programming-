package messanger;

import java.io.Serializable;

/**
 * Typ generyczny pakietu przesyłanego pomiędzy 
 * klientem, a serwerem za pośrednictwem 
 * ObjectOutputStream, ObjectInputStream.
 * Typ jest parametryzowany typem T co pozwala 
 * na opakowywanie różnego rodzaju obiektów takich 
 * jak: Contact, Message, Lista Kontaktów, String
 *       Integer, etc. co pozwala na odpowiednie 
 * reagowanie Serwera/Klienta na otrzymany pakiet
 * Każdy obiekt Packet<T> posiada pole command 
 * zawierające jeden z rozkazów (request) lub odpowiedzi (response)
 * opisujących przesłyłany pakiet i jego cel
 **/
public class Packet<T> implements Serializable
{
    public enum PacketCommand {
        MESSAGE,
        MESSAGE_RECEIVED_CONFIRMATION,
        RECEIVE_UNREADED_MESSAGES,
        CONTACT_LIST_REQUEST,
        CONTACT_LIST_RESPONSE,
        SEARCH_CONTACTS_REQUEST,
        SUGGESTED_CONTACTS_RESPONSE, 
        ADD_CONTACT_REQUEST,
        ADD_CONTACT_RESPONSE,
        SIGN_IN_REQUEST,
        SIGN_IN_RESPONSE,
        SIGN_UP_REQUEST,
        SIGN_UP_RESPONSE,
        STATUS_CHANGED,
        CONTACTS_STATUSES_REQUEST,
        CONTACTS_STATUSES_RESPONSE,
        IS_NICK_OCCUPIED_REQUEST,
        IS_NICK_OCCUPIED_RESPONSE,
        CLOSE_CONNECTION,
        GET_ARCHIVE_MESSAGES,
        ARCHIVE_MESSAGES,
        REMOVE_CONTACT_REQUEST,
        REMOVE_CONTACT_RESPONSE
    };
    
    //komenda przesłana z pakietem
    private PacketCommand command;
    
    //obiekt opakowany przez pakiet: Contact, Message, String, Integer, List<Contact>
    private T object;
    
    public Packet(PacketCommand command, T object) {
        
        this.command = command;
        this.object = object;
    }
    
    public PacketCommand command()
    {
        return command;
    }
    
    public T object()
    {
        return object;
    }
    
   
};

/**
 * 1) Obiekty Packet<T> mozna wysyłać do strumienia -> ObjectOutputStream
 * Przesyłanie obiektu przez sieć:
 * ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream()); 
 * oos.writeObject(packet);
 *
 * 2) Odczytywanie przesyłanego obiektu Packet<T> ze strumienia -> ObjectInputStream
 * ObjectInputStream ois = new ObjectInputStream(sock.getInputStream()); 
 * packet = (Packet) ois.readObject(); 
 **/