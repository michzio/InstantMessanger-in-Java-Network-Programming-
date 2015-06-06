package messanger;

import model.DataBase;
import model.Contact;
import model.Message;
import model.ContactList; 
import java.sql.Connection;

import java.net.ServerSocket;
import java.net.Socket;
import java.lang.Runnable;
import java.lang.Integer;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import java.util.*;
import java.lang.Class;


public class Server implements Runnable
{
    private static final int PORT = 5454;
    private static final int MAX_CONNECTIONS = 50;
    
    //obiekty przypisane do klasy dostępne z poszczególnych wątków
    private static Map<Integer, Socket> sockets;
    private static Map<Integer, ObjectOutputStream> outputs;
    private static Connection connection;
    //mapa statusów aktualnie podłączonych klientów
    //offline -> usuwanie kontaktu z kontenera niepotrzebne zajmowanie miejsca
    //otrzymano status -> nie znaleziono statusu to dodajemy dla niego status
    //podczas zamykania aplikacji klienckiej przesyłany status offline
    private static Map<Integer, Status> contactsStatuses;
    
    //identyfikator klienta z którym server uzyskał połączenie
    private int clientId;
    
    //obiekty umożliwiające komunikację przez sieć dla bieżącego połączenia z programem klienta
    private Socket sock;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    
    public Server(Socket sock)
    {
        this.sock = sock;
        
        //tworzymy strumień wejściowy obiektów z gniazda klienta
        try {
            oos = new ObjectOutputStream(sock.getOutputStream());
            ois = new ObjectInputStream(sock.getInputStream());
            
            
        }  catch(IOException e)
        {
            System.out.println("Przechwycono IOException w konstruktorze Servera");
        }
        
    }
    /*
    private synchronized void setupConnectionStreams()
    {
        //pobranie identyfikatora int podłączonego klienta
        //i zapisanie w statycznej mapie dla tego identyfikatora
        //obiekt gniazda
        try {
            //wysyłanie klientowi listy kontaktów
            oos.writeObject(contacts);
            oos.flush();
            //odczytywanie identyfikatora podłączanego klienta
            int client_id = ois.readInt();
            sockets.put(client_id, sock);
            outputs.put(client_id, oos);

        } catch(IOException e) {
            System.out.println("Przechwycono IOException w konstruktorze Severa");
        }
    }*/
    
    @Override
    @SuppressWarnings("unchecked")
    public void run()
    {
        
      try {
          
        //odebranie wiadomości od klienta
        //odczytyjemy wiadomość
        while(true) {
        
            System.out.println("Odczytywanie pakietów na serwerze...");
        
            //Przyjmowanie pakietu od klienta
            Packet request = (Packet) ois.readObject();
            
            switch( request.command() )
            {
                case MESSAGE:
                    //obsługa przekazywania wiadomości między nadawcą i adresatem
                    passMessage(request);
                    break;
                case MESSAGE_RECEIVED_CONFIRMATION:
                    //obsługa potwierdzenia odbioru wiadomości
                    confirmMessageReceived(request);
                    break;
                case IS_NICK_OCCUPIED_REQUEST:
                    //obsługa zapytania o zajętość nicku
                    handleIsNickOccupiedRequest(request);
                    break;
                case SEARCH_CONTACTS_REQUEST:
                    //obsługa zapytania z patternem kontaktu do wyszukania
                    handleContactsSearch(request);
                    break;
                    
                case ADD_CONTACT_REQUEST:
                    //obsługa dodawania kontaktu do listy kontaktów
                    addContactToList(request);
                    break;
                case REMOVE_CONTACT_REQUEST:
                    removeContact(request);
                    break;
                case SIGN_IN_REQUEST:
                    //obsługa procesu logowania klienta
                    signInClient(request);
                    break;
                    
                case SIGN_UP_REQUEST:
                    //obsługa procesu rejestracji klienta
                    signUpNewAccount(request);
                    break;
                    
                case CONTACT_LIST_REQUEST:
                    //obsługa żądania listy kontaktów
                    sendContactList(request);
                    break;
                case STATUS_CHANGED:
                    //obsługa aktualizacji statusu jednego z klientów
                    updateStatus(request);
                    break;
                    
                case CONTACTS_STATUSES_REQUEST:
                    //utworzenie i odesłanie odpowiedzi ze statusami
                    //dla otrzymanej listy kontaktów
                    sendContactsStatusesFor(request);
                    break;
                case RECEIVE_UNREADED_MESSAGES:
                    //obsługa zapytania z prośba o przesłanie
                    //klientowi nieprzeczytanych wiadomości
                    sendUnreadedMessages();
                    break;
                case GET_ARCHIVE_MESSAGES:
                    //obsługa żądania wiadomości archiwalnych
                    sendArchiveMessages(request);
                    break; 
                case CLOSE_CONNECTION:
                    closeConnectionWithClient(request);
                    return;
                default:
                    System.out.println("Nierozpoznane zapytanie!");
            };
        
    /*
            Message m = (Message)ois.readObject();
            //wysyłamy wiadomość do odbiorcy
            //gniazdo wyjściowe do drugiego klienta (odbiorcy wiadomości)
            int reciever_id = m.getReceiverId();
            /*Socket outSock = sockets.get(reciever_id);
            if(outSock == null) {
                System.out.println("null");
            }*/
    /*
            ObjectOutputStream outputToReciever = outputs.get(reciever_id);
            if(outputToReciever == null) {
                System.out.println(outputs);
                System.out.println("ObjectOutputStream to massege Receiver is null!");
            }
            outputToReciever.writeObject(m);
    */
            
        }
      } catch(IOException e)
      {
          System.out.println("Przechwycono IOException w metodzi run() Servera");
      } catch(ClassNotFoundException e)
      {
          System.out.println("Przechwycono ClassNotFoundException w metodzi run() Servera");
      }
    }
    
    /**
     * Metoda obsługuje zapytanie sprawdzające zajętość nicku
     * Po sprawdzeniu w bazie danych czy nick (opakowany w packet)
     * znajduje się czy też nie w bazie danych na serwerze odsyłamy 
     * odpowiedź.
     **/
    private synchronized void handleIsNickOccupiedRequest(Packet<String> packet)
    {
        //pobranie nicka
        String nickName = packet.object();
        //sprawdzenie czy istnieje kontakt dla tego nicka w bazie danych
        Contact c = Contact.getContactWithNickname(connection, nickName);
        
        //sprawdzenie zajętości i ustawienie flagi
        Boolean isNickOccupied = false;
        if(c != null)
        {
            isNickOccupied = true;
        }
        
        //skontruowanie odpowiedzi
        Packet<Boolean> response = new Packet<Boolean>(
            Packet.PacketCommand.IS_NICK_OCCUPIED_RESPONSE, isNickOccupied);
       
        try {
            //odesłanie odpowiedzi
            oos.writeObject(response);
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas odsyłania odpowiedzi na zapytanie o zajętość nicku!");
        }
        
    }
    
    /**
     * Metoda obsługuje proces rejestracji nowego konta użytkownika
     * Pobieramy z pakietu opakowany w nim obiekt kontakt
     * Ustawiamy na nim obiekt Connection połączenia z bazą danych 
     * i zapisujemy użytkownika do bazy danych metosą .save()
     * Odsyłamy nadany użytkownikowi identyfikator w pakiecie Packet<Integer>
     **/
    private synchronized void signUpNewAccount(Packet<Contact> packet)
    {
        //pobranie obiektu Contact
        Contact c = packet.object();
        
        //ustawienie obiektu Connection na obiekcie Contact
        c.setConnection(connection);
        
        //zapisanie kontaktu w bazie danych (true jeżeli z sukcesem)
        if(!c.save())
            System.out.println("Błąd podczas rejestracji użytkownika po stronie serwera!");
        //Pobranie identyfikatora klienta: ID > 0 Dodano poprawnie, ID == 0 Bład
        clientId = c.getId();
        
        //utworzenie odpowiedzi zwrotnej
        Packet<Integer> response = new Packet<Integer>(
                  Packet.PacketCommand.SIGN_UP_RESPONSE, clientId);
        
        try {
            //odesłanie odpowiedzi
            oos.writeObject(response);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas odsyłania odpowiedzi na zapytanie rejestracji nowego użytkownika.");
        }
        
        //zapisanie obiektów gniazda i strumienia wyjścowego w ogólnie
        //dostępnych HashMapach, tak by pozostałe wątki połączeń z
        //klientami miały dostęp nich dostęp
        sockets.put(clientId, sock);
        outputs.put(clientId, oos);
        
    }
    
    /**
     * Metoda obsługuje proces logowania użytkownika na podstawie 
     * loginu i hasła (enkodowanego algorytmem SHA1) opakowanych w
     * obiekt Contact. Ustawiamy na tym obieckie obiekt połączenia z
     * bazą danych Connection i wykonujemy metodę .authorizeClient()
     * Otrzymujemy obiekt pełnoprawny klienta z ID > 0 lub 
     * brak klienta gdy ID = 0.  Odsyłamy programowi klienckiemu 
     * identyfikator contact_id opakowany w Packet<Integer>
     **/
    public synchronized void signInClient(Packet<Contact> packet)
    {
        //pobranie obiektu Contact
        Contact c = packet.object();
        
        //ustawienie obiektu Connection na obiekcie Contact
        c.setConnection(connection);
        
        //wywołanie metody weryfikującej dane uwierzytelniające z bazą danych
        if(!c.authorizeClient())
            System.out.println("Błąd podczas uwierzytelniania klienta względem bazy danych!");
        
        //pobranie identyfikatora klienta: ID > 0 Uwierzytelniono poprawnie
        //                                 ID = 0 Błąd uwierzytelniania
        clientId = c.getId();
        
        //utworzenie odpowiedzi zwrotnej
        Packet<Integer> response = new Packet<Integer>(
                Packet.PacketCommand.SIGN_IN_RESPONSE, clientId);
        
        try {
            //odesłanie wiadomości
            oos.writeObject(response);
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas odsyłania odpowiedzi na zapytanie uwierzytelnienia klienta.");
        }
        
        //zapisanie obiektów gniazda i strumienia wyjścowego w ogólnie
        //dostępnych HashMapach, tak by pozostałe wątki połączeń z
        //klientami miały dostęp nich dostęp
        sockets.put(clientId, sock);
        outputs.put(clientId, oos);
        
    }
    
    /**
     * Metoda umożliwiająca odesłanie listy kontaktów w odpowiedzi na 
     * żądanie programu klienckiego
     **/
    public synchronized void sendContactList(Packet<Integer> packet)
    {
        //pobranie identyfkikatora klienta dla którego żądano listy kontaktów
        Integer clientId = (Integer) packet.object();
        
        //pobranie list kontaktów z bazy danych SQL
        List<Contact> contactList = ContactList.getForOwnerId(connection,
                                                              clientId);
        
        //opakowanie kontaktów w HashMap<Integer, Contact>
        //gdzie kluczem jest identyfikator kontaktu
        Map<Integer, Contact> contactMap = new HashMap<>();
        
        for(Contact c : contactList)
            contactMap.put(c.getId(), c);
        
        //opakowanie mapty kontaktów w obiekt pakietu Packet<...>
        Packet< Map<Integer,Contact> > response =
            new Packet<>(Packet.PacketCommand.CONTACT_LIST_RESPONSE,
                         contactMap);
        try {
            //odesłanie pakietu z listą kontaktów
            oos.writeObject(response);
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania listy kontaktów po stronie serwera.");
        }
    }
    
    /**
     * Metoda obsługująca proces wyszukiwania kontaktów zgodnych ze 
     * wzorcem przesłanym w obiekcie Contact opakowanym w pakiet Packet<Contact>
     * Na podstawie przesłanego patternu tworzone jest zapytanie do bazy 
     * danych SQL i zwracana jako odpowiedź lista dopasowanych do wzroca kontaktów
     * List<Contact>
     **/
    public synchronized void handleContactsSearch(Packet<Contact> packet)
    {
        //pobranie obiektu Contact z patternem kontaktu
        Contact pattern = (Contact) packet.object();
        
        //dołączenie obiektu połączenia z bazą danych
        pattern.setConnection(connection);
        
        //wywołanie metody wyszukującej kontakty pasujące do paternu
        List<Contact> matchedContacts = pattern.getMatchedContacts();
        
        //opakowanie listy dopasowanych kontaktów w nowy pakiet zwrotny
        Packet< List<Contact> > response =
            new Packet<>(Packet.PacketCommand.SUGGESTED_CONTACTS_RESPONSE,
                         matchedContacts);
        
        try {
            //odesłanie pakietu z dopasowanymi kontaktami
            oos.writeObject(response);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania wyników wyszukiwania kontaktu po stronie serwera.");
        }
    }
    
    public synchronized void addContactToList(Packet<Contact> packet)
    {
        Contact c = packet.object();
        
        
        //utworzenie wpisu na listę kontaktów
        //dodajemy do listy należącej do clientId
        //identyfikator pobrany przy autoryzacji i przechowywany lokalnie
        //dla wątku serwera
        ContactList contactEntry = new ContactList(connection,
                                                   clientId,
                                                   c.getId());
        
        //zapisanie kontaktu na listę kontaktów bieżącego usera w bazie danych
        boolean result = contactEntry.save();
        
        //opakowanie resultatu w pakiet zwrotny
        Packet<Boolean> response = new Packet<Boolean>(
            Packet.PacketCommand.ADD_CONTACT_RESPONSE, result);
        
        try {
            //odesłanie odpowiedzi
            oos.writeObject(response);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania wiadomości zwrotnej z potwierdzeniem wstawienia kontaktu na listę kontaków w bazie danych");
        }
        
    }
    
    
    public synchronized void passMessage(Packet<Message> packet)
    {
        //pobranie wiadomości
        Message msg = (Message) packet.object();
        
        //zapisanie w bazie danych w celu archiwizacji
        //lub przechowania gdy nie można aktualnie dostarczyć
        msg.setConnection(connection);
        msg.save();
        
        //pobranie identyfikatora odbiorcy wiadomości
        int receiver_id = msg.getReceiverId();
        
        //przesłanie wiadomości do odbiorcy jeżeli jest dostępny
        ObjectOutputStream outputToReciever = outputs.get(receiver_id);
        if(outputToReciever == null) {
            System.out.println(outputs);
            System.out.println("ObjectOutputStream do odbiorcy wiadomości jest null!");
            return;
        }
        //utworzenie nowego pakietu z wiadomością uaktualnioną o message_id
        //z bazy danych
        Packet<Message> msgPacket = new Packet<Message>(
                            Packet.PacketCommand.MESSAGE, msg);
        
        try {
            //wypisanie wiadomości do strumienia wyjściowego
            outputToReciever.writeObject(msgPacket);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania pakitu z wiadomością do odbiorcy z serwera poprzez strumień obiektowy.");
        }
        
        
    }
    
    /**
     * Metoda pozwala potwierdzić odbiór wiadomości przez adresata.
     * Po otrzymaniu wiadomości klient odsyła wiadomość uzupełnioną
     * o date/czas odczytania wiadomości jako pakiet 
     * MESSAGE_RECEIVED_CONFIRMATION. Pozwala to na uaktualnienie 
     * wpisu wiadomości w bazie danych jako odczytana.
     **/
    public synchronized void confirmMessageReceived(Packet<Message> packet)
    {
        Message msg = packet.object();
        
        //ustawienie połączenia z bazą danych i aktualizacja wpisu
        msg.setConnection(connection);
        msg.save();
    }
    
    /**
     * Metoda aktualizuje status bieżącego klienta na ten znajdujący 
     * się w przesłanym pakiecie od klienta.
     **/
    public synchronized void updateStatus(Packet<Status> packet)
    {
        //pobranie otrzymanego statusu
        Status currentClientStatus = (Status) packet.object();
        
        //zaktualizowanie statusu w HashMap'ie
        contactsStatuses.put(clientId, currentClientStatus);
        
        System.out.println("Zaktualizowano status klienta: " + clientId);
    }
    
    /**
     * Metoda dla przesłanego w pakiecie zbioru kontaktów sprawdza 
     * ich bieżący status w HashMap'ie i tworzy odpowiednia mapę
     * będącą jej podzbiorem, a następnie odsyła ją do bieążcego klienta
     * który przesłał to zapytanie.
     **/
    public synchronized void sendContactsStatusesFor(Packet< java.util.List<Integer> > packet) {
        
        //odczytanie zbioru kontaktów
        java.util.List<Integer> contactIdsList = packet.object();
        
        //utworzenie nowej mapy ze statusami dla otrzymanego zbioru kontaktów
        Map<Integer, Status> statuses =
                    new HashMap<Integer, Status>(contactIdsList.size());
        
        for(Integer contactId : contactIdsList)
        {
            //sprawdzanie czy istnieje status dla kontaktu contactId
            if(contactsStatuses.containsKey(contactId))
            {
                //jeżeli tak to dodajemy bieżący status przechowywany na serwrze
                statuses.put(contactId, contactsStatuses.get(contactId));
            } else {
                //jeżeli nie to zwracamy dla tego kontaktu statu OFFLINE
                statuses.put(contactId, Status.OFFLINE);
            }
        }
        
        //opakowanie mapy statusów dla zbioru kontaktów w pakiet zwrotny
        Packet< Map<Integer, Status> > response =
            new Packet<>(Packet.PacketCommand.CONTACTS_STATUSES_RESPONSE,
                         statuses);
        
        try {
            //odesłanie pakietu z odpowiedzią (statusy kontaktów)
            oos.writeObject(response);
        } catch(IOException e)
        {
            System.out.println("Błąd I/O podczas odsyłania pakietu ze statusami dla otrzymanej listy kontaktów.");
        }
        
    }
    
    public synchronized void closeConnectionWithClient(Packet<Integer> packet)
    {
        Integer clientId = packet.object();
        
        outputs.remove(clientId);
        sockets.remove(clientId);
        contactsStatuses.remove(clientId);
        
        try {
            sock.close();
            oos.close();
            ois.close();
        } catch(IOException e) {
            System.out.println("Błąd podczas zamykania połączenia z klientem: "+ clientId); 
        }
        
    }
    
    /**
     * Metoda sprawdzająca w bazie danych SQL czy użytkownik
     * posiada nieodczytane wiadomości. Jeżeli tak
     * to wysyła klientowi zaległe wiadomości.
     **/
    public synchronized void sendUnreadedMessages()
    {
        //jeżeli klient nie został poprawni uwierzytelniony to
        //pomijamy ten krok
        if(clientId < 1) return;
        
        //pobranie nieprzeczytanych wiadomości z bazy danych
        java.util.List<Message> unreadedMessages =
                    Message.getUnreadMessagesFor(connection, clientId);
        
        //wysłanie ich do klienta
        try {
            
            for(Message msg : unreadedMessages)
            {
                //utworzenie pakietu dla każdej wiadomości
                Packet<Message> msgPacket = new Packet<Message>(
                                        Packet.PacketCommand.MESSAGE, msg);
                
                oos.writeObject(msgPacket);
                
            }
            
            System.out.println("Wysłano nieprzeczytane wiadomości do: " + clientId);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania klientowi nieodebranych wiadomości."); 
        }
    }
    
    public synchronized void sendArchiveMessages(Packet<Contact> packet)
    {
        Contact c = packet.object();
        
        System.out.println("Archiwum widomości dla użytkowników: " + clientId
                           + " i " + c.getId() );
        
        java.util.List<Message> archiveMessages =
                    Message.getArchiveMessagesFor(connection,
                                                  clientId,
                                                  c.getId() );
        
        //utworzenie pakietu zwrotnego
        Packet< java.util.List<Message> > response = new
            Packet<>(Packet.PacketCommand.ARCHIVE_MESSAGES, archiveMessages);
        
        try {
            oos.writeObject(response);
        }
        catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania archiwalnych wiadomości.");
        }
    }
    
    public synchronized void removeContact(Packet<Contact> packet)
    {
        Contact c = packet.object();
        
        ContactList entry = new ContactList(connection,
                                            clientId,
                                            c.getId());
        
        boolean result = entry.remove();
        
        Packet<Boolean> response = new Packet<Boolean>(
                Packet.PacketCommand.REMOVE_CONTACT_RESPONSE, result);
        
        try {
            oos.writeObject(response);
            
        } catch(IOException e)
        {
            System.out.println("Błąd I/O podczas wysyłania odpowiedzi na usunięcie kontaktu");
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        //utworzenie obiektu bazy danych
        DataBase database = new DataBase();
        //pobranie obiektu połączenia z bazą danych
        connection = database.connection();
        
        ServerSocket serverSocket = new ServerSocket(PORT, MAX_CONNECTIONS);
        //utworzenie statycznego HashMap<Integer, String>
        //w k†órym będą przechowywane gniazda sieciowe z którymi
        //serwer nawiązał połączenie
        sockets = new HashMap<Integer, Socket>();
        //utworzenie statycznego HashMap<Integer, String>
        //w którym będą przechowywane obiekty ObjectOutputStream
        //odpowiadające poszczególnym klientą
        outputs = new HashMap<Integer, ObjectOutputStream>();
        //utworzenie statycznej HashMap<Integer, Status>
        //ze statusami podłączonych klientów
        contactsStatuses = new HashMap<Integer, Status>();
        
        //pętla nieskończona akceptująca podłączające się programy klienckie
        while(true) {
            Socket sock = serverSocket.accept();
            if(sock != null) {
                //dla każdego klienta tworzymy nowy wątek do jego obsługi
                new Thread(new Server(sock)).start();
            }
        }
    }

}