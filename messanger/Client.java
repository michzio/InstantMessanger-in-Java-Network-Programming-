package messanger;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.lang.Runnable;
import java.util.prefs.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import model.Contact;
import model.Message;


public class Client extends JFrame implements ActionListener, ListSelectionListener, Runnable, AuthorizationCompletedListener, ContactAddedListener, ArchiveListener
{
    public static final String PREFERENCES_CLIENT_ID_KEY = "client_id";
    public static final String PREFERENCES_LAST_STATUS = "last_status";
    
    private final String host = "localhost"; //localhost
    private final int port = 5454;
    
    //identyfikator clienta przekazany podczas wywołania programu
    private int clientId;
    private Status status;
    
    //obiekty do komunikacji sieciowej
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    
    //lista kontaktów i mapa statusów
    private JList<Contact> contactList;
    private DefaultListModel<Contact> listModel;
    private Map<Integer, Status> contactStatuses;
    private static Map<Integer, Contact> contacts;
    
    private Map<Integer, ClientMessageFrame> msgFrames;
    
    //obiekty związane z Menu aplikacji
    private JMenuBar menuBar;
    private JMenu menu;
    private JMenuItem exitItem;
    private JMenuItem signoutItem;
    private JMenuItem addContactItem;
    //podmenu z wyborem statusu dostępności
    private JMenu statusMenu;
    private JMenuItem onlineStatusItem;
    private JMenuItem offlineStatusItem;
    private JMenuItem idleStatusItem;
    private JMenuItem invisibleStatusItem;
    
    //panel z buttonami
    private JButton addContactButton;
    private JButton removeContactButton;
    private JButton archiveButton;
    
    //obiekt otwartego okna dialogowego wyszukiwania kontaktów
    AddContactDialog addContactDialog;
    ArchiveDialog archiveDialog;
    
    //obiekty związane ze periodycznym sprawdzaniem statusu kontaktów
    private java.util.Timer statusTimer; //scheduler zadań
    private TimerTask statusTask; //zadanie sprawdzania statusu kontaktów
    
    /**
     * Klasa reprezentująca poszczególne komórki na JList kontaków
     * głównego okna programu. Obiekty komórek muszą zawierać
     * Imię, Nazwisko oraz Status (Dostępny/Niedostępny)
     **/
    public class ContactCellRenderer extends JPanel implements ListCellRenderer<Object>
    {
        
        public ContactCellRenderer()
        {
            setOpaque(true);
        }
        
        /**
         * Przeciążana metoda, która customizuje renderowanie komórki
         * na liście kontaktów
         **/
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setComponentOrientation(
                                          ComponentOrientation.LEFT_TO_RIGHT);
    
            //pobranie obiektu Kontaktu
            Contact contact = (Contact) value;
            
            //Sprawdzenie statusu w HashMapie<Integer, Status> z dostępnością kontaktów
            Status contactStatus = contactStatuses.get(contact.getId());
            
            //Dodanie ikonki statusu
            ImageIcon statusIcon = null;
            
            //załadowanie odpowiedniej ikonki dla danego statusu
            switch(contactStatus) {
                case INVISIBLE:
                case OFFLINE:
                    statusIcon = new ImageIcon(Status.OFFLINE_IMG);
                    break;
                case ONLINE:
                    statusIcon = new ImageIcon(Status.ONLINE_IMG);
                    break;
                case IDLE:
                    statusIcon = new ImageIcon(Status.IDLE_IMG);
                    break;
                default:
                    statusIcon = new ImageIcon(Status.OFFLINE_IMG);
                    break;
            }
            
            //Dodanie labelki z imieniem i nazwiskiem
            panel.add(new JLabel(statusIcon));
            panel.add(new JLabel(contact.getFirstName()) );
            panel.add(new JLabel(contact.getLastName()) );
            
            if(isSelected) {
                panel.setBackground(Color.LIGHT_GRAY);
            } else {
                panel.setBackground(new Color(247,247,247));
            }
            
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            
            return panel;
            
        }
    };
    
    public Client()
    {
        super();
        
        //mapa identyfikator klienta -> okno rozmowy
        //maksymalnie tylko jedno okno rozmowy otwarte dla każdego klienta
        msgFrames = new HashMap<Integer, ClientMessageFrame>();
        //alokowanie listy kontaktów
        contacts = new HashMap<Integer, Contact>();
        //utworzenie widoku okna głównego
        createView();
        
        try {
            //utworzenie gniazda sieciowego
            socket = new Socket(java.net.InetAddress.getByName(host).getHostName(), port);
        
            //utworzenie strumieni wejsciowych i wyjsciowych
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            
        } catch(IOException e)
        {
            System.out.println("Przechwycono IOException w konstruktorze Client");
            //wyświetlenie alertu z błędem połączenia sieciowego
            JOptionPane.showMessageDialog(this, "Brak połączenia z siecią...", "Błąd sieci!", JOptionPane.WARNING_MESSAGE);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
        
        //1. MOŻNA POBRAĆ ID ZALOGOWANEGO KLIENTA JEŻELI JEST PERSYSTOWANE
        //   gdy klient zaznaczył opcje by pozostać zalogowanym na tym komputerze
        
        //uzyskanie dostępu do preferencji aplikacji
        Preferences preferences = Preferences.userNodeForPackage(messanger.Client.class);
        //pobranie ID klienta, domyślne ustawienie id klienta na 0 - brak danych klienta wymaga uwierzytelnienia
        clientId = preferences.getInt(PREFERENCES_CLIENT_ID_KEY, 0);
        
        //2. Wyświetlenie nowego okna dialogowego, które umożliwi zalogowanie lub rejestrację nowego użytkownika
        if(clientId < 1) {
            try {
                //brak identyfikatora użytkownika -> wymagane uwierzytelnienie
                SignInDialog dialog = new SignInDialog(ois, oos);
                dialog.addAuthorizationCompletedListener(this);
                
            } catch( UnknownHostException e) {
                System.out.println("Nieznany host podczas tworzenia okna uwierzytelniania klienta.");
            } catch( IOException e) {
                System.out.println("Błąd wejścia/wyjścia podczas tworzenia okna uwierzytelniania klienta.");
            }
        }
        
        //dodanie listenera zdarzenia zamknięcia okna
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                appClosing();
            }
        } );
        
    }
    
    /**
     * Funkcja wczytuje listę kontaktów z serwera dla bieżącego klienta
     **/
    @SuppressWarnings("unchecked")
    private void loadContactList()
    {
        System.out.println("Wczytywanie kontaktów.");
        
        //1. ZAPYTANIE O LISTĘ KONTAKTÓW BIEŻĄCEGO KLIENTA
        try {
            
            //utworzenie pakietu z zapytaniem
            Packet<Integer> request = new Packet<Integer>(
                Packet.PacketCommand.CONTACT_LIST_REQUEST, clientId);
            
            //wysłanie zapytania o listę kontaktów
            oos.writeObject(request);
            
            //odebranie pakietu odpowiedzi z listą kontaktów
            Packet response = (Packet) ois.readObject();
            
            if(response.command() == Packet.PacketCommand.CONTACT_LIST_RESPONSE)
            {
                //odczytanie listy kontaktów z pakietu odpowiedzi jako
                //mapy: integer -> Contact object
                contacts = (Map<Integer, Contact>) response.object();
                
            } else {
                System.out.println("Błędny pakiet odpowiedzi podczas pobierania listy kontaktów.");
            }
            
        } catch(IOException e) {
            System.out.println("Błąd podczas połączenia sieciowego przesyłającego listę kontaktów.");
        } catch(ClassNotFoundException e) {
            System.out.println("Obiekt nieznanej klasy otrzymany z sieciowego strumienia obiektowego.");
        }
        
        //2. ZAŁADOWANIE KONTAKTÓW Z MAPY NA JLISTE
        for(Contact c : contacts.values()) {
            listModel.addElement(c);
        }

    }
    
    /**
     * Metoda tworząca widok okna aplikajci
     **/
    private void createView()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Witaj!");
        setSize(300, 700);
        setLocationRelativeTo(null);
        //ustawienie GridBagLayout jako menadżera layoutu
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        
        //pobranie nazw użytkowników w postaci tablicy stringów
        System.out.println(contacts);
        
        //utworzenie górnego Menu aplikacji
        createMenuBar();
        
        //dodanie przycisków ikon głównego okna aplikacji
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.setComponentOrientation(
                                             ComponentOrientation.LEFT_TO_RIGHT);
        
        addContactButton = new JButton(new ImageIcon("messanger/add-icon.png"));
        addContactButton.addActionListener(this);
        buttonsPanel.add(addContactButton);
        
        archiveButton = new JButton(new ImageIcon("messanger/archive.png"));
        archiveButton.addActionListener(this);
        buttonsPanel.add(archiveButton);
        
        removeContactButton = new JButton(new ImageIcon("messanger/remove.png"));
        removeContactButton.addActionListener(this);
        buttonsPanel.add(removeContactButton);
        
        buttonsPanel.setMaximumSize(new Dimension(150, 40));
        getContentPane().add(buttonsPanel);
        
        
        //utworzenie kontrolki JList wyświetlajacej liste kontaktów do wyboru
        listModel = new DefaultListModel<Contact>();
        contactList = new JList<Contact>(listModel);
        contactList.setCellRenderer( new ContactCellRenderer());
        getContentPane().add(new JScrollPane(contactList));
        
        //list.element.addActionListener(ActionListener l);
        contactList.addListSelectionListener(this);
        
        //wyswietlenie komponentów
        pack();
        setSize(300,700);
        setVisible(true);

        
    }
    
    /**
     * Metoda dodająca do okna aplikacji Menu umożliwiające np. wylogowanie się
     **/
    private void createMenuBar()
    {
       //utworzenie paska menu
        menuBar = new JMenuBar();
        
        //utworzenie listy menu
        menu = new JMenu("Plik");
        menu.setMnemonic(KeyEvent.VK_M);
        menu.getAccessibleContext().setAccessibleDescription("Menu Aplikacji");
        menuBar.add(menu);
        
        //dodanie itemów do menu
        
        //item dodawania/wyszukiwania kontaktu
        addContactItem = new JMenuItem("Dodaj kontakt");
        addContactItem.setMnemonic(KeyEvent.VK_A);
        addContactItem.addActionListener(this);
        menu.add(addContactItem);
        
        //utworzenie itemu z podmenu wyboru statusu dostępności
        statusMenu = new JMenu("Ustaw status");
        statusMenu.setMnemonic(KeyEvent.VK_S);
        
        //itemy podmenu statusów dostępności
        onlineStatusItem = new JMenuItem("Dostępny", new ImageIcon("messanger/online.png"));
        idleStatusItem = new JMenuItem("Zaraz wracam", new ImageIcon("messanger/idle.png"));
        invisibleStatusItem = new JMenuItem("Niewidoczny", new ImageIcon("messanger/invisible.png"));
        offlineStatusItem = new JMenuItem("Niedostępny", new ImageIcon("messanger/offline.png"));
        onlineStatusItem.addActionListener(this);
        offlineStatusItem.addActionListener(this);
        idleStatusItem.addActionListener(this);
        invisibleStatusItem.addActionListener(this);
        statusMenu.add(onlineStatusItem);
        statusMenu.add(offlineStatusItem);
        statusMenu.add(idleStatusItem);
        statusMenu.add(invisibleStatusItem);
        
        menu.add(statusMenu);
        
        //wylogowanie użytkownika
        signoutItem = new JMenuItem("Wyloguj się");
        signoutItem.setMnemonic(KeyEvent.VK_O);
        signoutItem.addActionListener(this);
        menu.add(signoutItem);
        
        menu.addSeparator();
        
        //wyjście za aplikacji
        exitItem = new JMenuItem("Wyjdź", new ImageIcon("messanger/exit.png"));
        exitItem.setMnemonic(KeyEvent.VK_E);
        exitItem.addActionListener(this);
        menu.add(exitItem);
        
        
        setJMenuBar(menuBar);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void run()
    {
        System.out.println("Rozpoczęcie wątku run() w obiekcie Client o id: " + clientId);
        
        try {
            
            while(true) {
                //odbieranie pakietów (wiadomości, odpowiedzi) z serwera
                System.out.println("Klient " + clientId + " odbieranie pakietów (odpowiedzi, wiadomości)...");
                
                Packet packet = (Packet) ois.readObject();
                
                switch( packet.command() )
                {
                    case MESSAGE:
                        //obsługa odebranej wiadomości
                        handleReceivedMessage(packet);
                        break;
                    
                    case SUGGESTED_CONTACTS_RESPONSE:
                        //obsługa wyników wyszukiwania kontaktów
                        //przekazanie pakietu do okna dialogowego
                        if(addContactDialog != null)
                            addContactDialog.updateSuggestedContacts(packet);
                        break;
                    case ARCHIVE_MESSAGES:
                        if(archiveDialog != null)
                            archiveDialog.updateArchiveMessagesList(packet);
                        break;
                    case CONTACTS_STATUSES_RESPONSE:
                        //aktualizacja statusów kontaktów na podstawie otrzymanej odpowiedzi
                        contactsStatusesReceived(packet);
                        break;
                    case ADD_CONTACT_RESPONSE:
                        //obsługa potwierdzenia dodania kontaktu na liste w SQL
                        contactSaved(packet);
                        break;
                    case REMOVE_CONTACT_RESPONSE:
                        contactRemoved(packet);
                        break;
                    default:
                        System.out.println("Odebrano z servera nieznany pakiet!");
                        break;
                };
            }
            
        } catch(IOException e)
        {
            System.out.println("Przechwycono IOException w metodzi run() Clienta");
        } catch(ClassNotFoundException e)
        {
            System.out.println("Przechwycono ClassNotFoundException w metodzi run() Clienta");
        }

        
    }
    
    /**
     * Funkcja obsługująca odbieranie wiadomości z serwera od innych klientów
     * Po odebraniu wiadomości opakownej w pakiec Packet<Message> odpowiednio 
     * przetwarza tę wiadomość i wyświetla ją w oknie aplikacji.
     **/
    private synchronized void handleReceivedMessage(Packet<Message> packet)
    {
        //odczytanie obiektu wiadomości z pakietu
        Message m = (Message) packet.object();
        
        System.out.println("Odebrano od " + m.getSenderId() + ": " + m);
        
        //Jeżeli okno rozmowy z nadawcą tej wiadomości zostało już wczęsniej otwarte to uzyskujemy tylko dostęp
        if(msgFrames.containsKey(m.getSenderId())) {
            
            System.out.println("Okienko rozmowy z tą osobą jest już otwarte");
            
            //pobieramy obiekt okienka i dodajemy nową wiadomość do kontrolki JList
            ClientMessageFrame frame = msgFrames.get(m.getSenderId());
            frame.addMessage(m);
            
        }
        //W przeciwnym wypadku musimy otworzyć nowe okno
        else {
            
            //utworzenie nowego okienka komunikacji z osoba od której dostaliśmy wiadomość
            ClientMessageFrame frame = new ClientMessageFrame(this, m.getSenderId());
            
            //wstawienie okienka do hashmap zawierajace liste otwartych okienek
            msgFrames.put(m.getSenderId(), frame);
            
            //dodaje do JList utworzonego okienka nowa otrzymana wiadmosc
            frame.addMessage(m);
        }
        
        
        //odesłanie potwierdzenia odbioru wiadomości
        String readTimeStamp =
            new SimpleDateFormat("YYYY-MM-dd HH:MM:ss.SSS").format(
                                        Calendar.getInstance().getTime());
        
        m.setReadDate(readTimeStamp);
        
        //utworzenie pakietu zwrotnego
        Packet<Message> response = new Packet<Message>(
            Packet.PacketCommand.MESSAGE_RECEIVED_CONFIRMATION, m);
        
        try {
            //odesłanie potwierdzenia odbioru wiadomości
            oos.writeObject(response);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania potwierdzenia odbioru wiadomości.");
        }
    }
    
    public void closeFrameFor(int reciever_id)
    {
        //usuwanie obiektu okna z HashMap po jego zamknieciu
        msgFrames.remove(reciever_id);
    }
    
    public int getClientId()
    {
        return clientId;
    }
    
    public Socket getSocket()
    {
        return socket;
    }
    
    public ObjectOutputStream getObjectOutputStream(){
        return oos;
    }
    
    public ObjectInputStream getObjectInputStream() {
        return ois;
    }
    
    @Override
    public void actionPerformed(ActionEvent evt)
    {
            //obsluga zdarzenia
            if(evt.getSource() == addContactItem)
            {
                System.out.println("Dodawanie Kontaktu Menu Item Kliknięto...");
                
                //utworzenie okna dialogowego wyszukiwania kontaktów
                try {
                    addContactDialog = new AddContactDialog(oos);
                    addContactDialog.addContactAddedListener(this);
                } catch( UnknownHostException ex) {
                    System.out.println("Nieznany host podczas tworzenia okna wyszukiwania kontaktów.");
                } catch( IOException ex) {
                    System.out.println("Błąd wejścia/wyjścia podczas tworzenia okna wyszukiwania kontaktów.");
                }
                
            } else if(evt.getSource() == exitItem) {
                
                System.out.println("Wyjdź Menu Item Kliknięto...");
                //zamykanie aplikacji
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                
                
            } else if(evt.getSource() == signoutItem) {
                
                System.out.println("Wyloguj się Menu Item Kliknięto...");
                
                //1. wylogowanie poprzez usunięcie identyfikatora z preferencji
                
                //uzyskanie dostępu do preferencji aplikacji
                Preferences preferences = Preferences.userNodeForPackage(messanger.Client.class);
                //usunięcie ID klienta
                preferences.remove(PREFERENCES_CLIENT_ID_KEY);
                
                //2. zamykanie aplikacji (można by ją też restartować)
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                
                
            } else if(evt.getSource() == onlineStatusItem) {
                
                System.out.println("Status Online Menu Item Kliknięto...");
                //zmiana statusu i powiadomienie o tym fakcie server'a
                status = Status.ONLINE;
                sendClientStatus();
    
            } else if(evt.getSource() == offlineStatusItem) {
                
                System.out.println("Status Offline Menu Item Kliknięto...");
                //zmiana statusu i powiadomienie o tym fakcie server'a
                status = Status.OFFLINE;
                sendClientStatus();
                
            } else if(evt.getSource() == idleStatusItem) {
                
                System.out.println("Statu Idle Menu Item Kliknięto...");
                //zmiana statusu i powiadomienie o tym fakcie server'a
                status = Status.IDLE;
                sendClientStatus();
                
            } else if(evt.getSource() == invisibleStatusItem) {
                
                System.out.println("Status Invisible Menu Item Kliknięto...");
                //zmiana statusu i powiadomienie o tym fakcie server'a
                status = Status.INVISIBLE;
                sendClientStatus();
            } else if(evt.getSource() == addContactButton)
            {
                //utworzenie okna dialogowego wyszukiwania kontaktów
                try {
                    addContactDialog = new AddContactDialog(oos);
                    addContactDialog.addContactAddedListener(this);
                } catch( UnknownHostException ex) {
                    System.out.println("Nieznany host podczas tworzenia okna wyszukiwania kontaktów.");
                } catch( IOException ex) {
                    System.out.println("Błąd wejścia/wyjścia podczas tworzenia okna wyszukiwania kontaktów.");
                }

            } else if(evt.getSource() == archiveButton) {
                //utworzenie okna dialogowego archiwum rozmów
                try {
                    Contact c = contactList.getSelectedValue();
                    if(c == null) return;
                    archiveDialog = new ArchiveDialog(c, oos);
                    archiveDialog.addArchiveListener(this);
                    
                } catch( UnknownHostException ex) {
                    System.out.println("Nieznany host podczas tworzenia okna archiwum rozmów.");
                } catch( IOException ex) {
                    System.out.println("Błąd wejścia/wyjścia podczas tworzenia okna archiwum rozmów.");
                }
            } else if(evt.getSource() == removeContactButton) {
                
                //usuwanie konraktu
                Contact c = contactList.getSelectedValue();
                
                if(c == null) return;
                
                Packet<Contact> removeContact = new Packet<Contact>(
                    Packet.PacketCommand.REMOVE_CONTACT_REQUEST, c);
                
                try {
                    oos.writeObject(removeContact);
                    
                    deletedContact = c;
                    
                } catch(IOException e) {
                    System.out.println("Błąd I/O przy usuwaniu kontaktu.");
                }
            }
    }
    
    private static Contact deletedContact = null;
    
    private synchronized void contactRemoved(Packet<Boolean> packet)
    {
        //odczytanie potwierdzenia usuniecia kontaktu
        boolean hasBeenRemoved = (Boolean) packet.object();
        
        //jeżeli usunieto poprawnie z bazy danych to spoko usuwamy kontakt
        //z listy w programie
        if(hasBeenRemoved)
        {
            contactStatuses.remove(deletedContact.getId());
            contacts.remove(deletedContact.getId());
            listModel.removeElement(deletedContact);
            deletedContact = null;
        }
        //jeżeli błąd to nie dodajemy kontatku do listy i wywalamy WARNING
        else {
            JOptionPane.showMessageDialog(this, "Błąd podczas usuwania kontaktu z listy po stronie serwera...", "Błąd usuwania kontaktu!", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * Metoda wywoływana podczas wybrania kontaktu z listy kontaktów oknie głównym 
     * programu. Lista ma postać kontenera JList<Contact>
     **/
    @Override
    public void valueChanged(ListSelectionEvent event) {
        
        boolean adjusting = event.getValueIsAdjusting();
        System.out.println("Dostosowywanie: " + adjusting);
        if(adjusting) return;
        
        @SuppressWarnings("unchecked")
        Contact  selectedContact =
                ((JList<Contact>)event.getSource()).getSelectedValue();
        
        if(selectedContact == null) return;
        
        System.out.println("Wybrano: " + selectedContact.getNickname());
        
        //pobranie identyfikatora kontaktu który został wybrany z listy kontaktów
        int contactId = selectedContact.getId();
        
        System.out.println("Otwieranie okna rozmowy z odbiorcą: " + contactId);
        
        if(msgFrames.containsKey(contactId)) {
            System.out.println("Okienko rozmowy z tą osobą jest już otwarte");
        } else {
            ClientMessageFrame frame = new ClientMessageFrame(this, contactId);
            msgFrames.put(contactId, frame);
        }
        
    }
    
    /**
     * Implementacja metod interfejsu listenera: AuthorizationCompletedListener
     **/
    @Override
    public void grantedContactId(int contact_id)
    {
        System.out.println("Klient został poprawnie uwierzytelniony.");
        System.out.println("Przyznano ci identyfikator klienta: " + contact_id);
        
        //zapisanie przyznanego contact_id jako identyfikator bieżącego klienta
        clientId = contact_id;
        
        //załadowanie listy kontaktów
        loadContactList();
        
        //wczytanie zapamiętanego statusu z persystowanych preferencji
        Preferences preferences = Preferences.userNodeForPackage(messanger.Client.class);
        //pobranie statusu, domyślne ustawienie Status.ONLINE
        int statusValue = preferences.getInt(PREFERENCES_LAST_STATUS, Status.ONLINE.getValue());

        status = Status.values()[statusValue];
        
        
        //wysłanie bieżącego statusu i pobranie  statusów dla listy kontaktów
        sendClientStatus();
        updateContactsStatuses();
        
        receiveUnreadedMessages();
        
        //uruchomienie wątku odbierania wiadomości
        new Thread(this).start();
        
        //zaplanowanie zadania periodycznego sprawdzania statusów listy
        //kontaktów i wysyłania bieżącego statusu klienta
        statusTimer = new java.util.Timer();
        statusTask = new TimerTask() {
            
            @Override
            public void run() {
                //wywołanie funkcji wysyłającej na serwer info o bieżącym statusie aplikajci
                sendClientStatus();
                //wywołanie funkcji sprawdzającej statusy
                updateContactsStatuses();
            }
        };
        
        statusTimer.schedule(statusTask, 5000,5000);
    }
    
    /**
     * Metoda wysyła na serwer zapytanie z prośbą o przesłanie wszystkich 
     * nieprzeczytancyh wiadomości.
     **/
    public synchronized void receiveUnreadedMessages()
    {
        Packet<Integer> request = new Packet<Integer>(
                Packet.PacketCommand.RECEIVE_UNREADED_MESSAGES, clientId);
        
        try {
            //wysłanie zapytania o nieprzeczytane wiadomości 
            oos.writeObject(request);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas wysyłania zapytania z prośbą o dostarczenie nieprzeczytanych wiadomości.");
        }
    }
    
    static class LackOfClientIdException extends Exception
    {
        private String msg;
        
        LackOfClientIdException(String msg) {
            this.msg = msg;
        }
        
        @Override
        public String getMessage() {
            return msg;
        }
    }
    
    /**
     * Implementacja metod interfejsu listenera: ContactAddedListener
     **/
    @Override
    public void contactAdded(Contact contact)
    {
        
        System.out.println("Dodawanie kontaktu " + contact.getNickname() + " na listę kontaktów...");
        
        //1. zapisanie dodania kontaktu do listy w bazie danych na serwerze
        
        //tworzenie pakietu Packet<Contact> zapytania ADD_CONTACT_REQUEST
        Packet<Contact> request = new Packet<Contact>(
            Packet.PacketCommand.ADD_CONTACT_REQUEST, contact);
        
        try {
        
            //wysłanie zapytania o dodanie kontaktu na serwer
            oos.writeObject(request);
            
            //przechowanie w lokalnej zmiennej dodawanego kontaktu
            //az do momentu uzyskania potwierdzenia z serwera
            newContact = contact;
            
        } catch(IOException e) {
            System.out.println("Błąd I/O sieciowego strumienia obiektowego podczas zapisywania kontaktu na liście kontaktów na serwerze.");
        }
        
    }
    
    //zmienna w której przechowywan są kontakty podczas ich dodawania
    //na liste kontaktów pomiędzy wysłaniem zapytania na serwer
    //a odebraniem odpowiedzi zwrotnej
    private static Contact newContact = null;
    
    /**
     * Metoda przyjmująca odpowiedź na zapytanie o dodanie kontaktu 
     * i zapisanie go w bazie danych na serwerze
     **/
    private synchronized void contactSaved(Packet<Boolean> response)
    {
        boolean hasBeenAdded = false;
        
        //odczytanie potwierdzenia doania kontaktu
        hasBeenAdded = (Boolean) response.object();
        
        //jeżeli dodano poprawnie do bazy danych to spoko dodajemy kontakt
        //na listę w programie
        if(hasBeenAdded)
        {
            contactStatuses.put(newContact.getId(), Status.OFFLINE);
            contacts.put(newContact.getId(), newContact);
            listModel.addElement(newContact);
            newContact = null;
        }
        //jeżeli błąd to nie dodajemy kontatku do listy i wywalamy WARNING
        else {
            JOptionPane.showMessageDialog(this, "Błąd podczas dodawania kontaktu do list po stronie serwera...", "Błąd dodawania kontaktu!", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * Obsługa zdarzenia zamknięcie okna wyszukiwania kontaktów
     **/
    @Override
    public void addContactDialogClosed()
    {
        addContactDialog = null;
    }
    
    @Override
    public void archiveDialogClosed()
    {
        archiveDialog = null;
    }
    
    
    /**
     * Metoda wysyła aktualny status bieżącego klienta na serwer
     * Umożliwia to periodyczne powiadamianie serwera o stanie dostępności tego klienta 
     * jak również powiadamianie w przypadku pewnych eventów (zmiana statusu, zamykwanie aplikacji)
     **/
    
    public synchronized void sendClientStatus() {
        
        // Wysłanie bieżącego statusu klienta
        Packet<Status> currentStatus = new Packet<Status>(
                                                          Packet.PacketCommand.STATUS_CHANGED, status);
        
        try {
            
            //wysłanie pakietu z info o statusie bieżącego klienta
            oos.writeObject(currentStatus);
            
        } catch(IOException e) {
            System.out.println("Błąd podczas pierodyczego wysyłania informacji o statusie klienta.");
            e.printStackTrace();
        }
    }
    
    /**
     * Metoda aktualizująca statusy kontaktów
     * Wysyła na serwer listę kontaktów dla których chce sprawdzić statusy
     **/
    public synchronized void updateContactsStatuses() {
        
       
        
        // Przesłanie listy kontaktów z requestem sprawdzenia statusów
        java.util.List<Integer> contactIdsList = new ArrayList<Integer>(contacts.keySet());
        Packet< java.util.List<Integer> > contactsStatusesRequest = new Packet< java.util.List<Integer> >(
                        Packet.PacketCommand.CONTACTS_STATUSES_REQUEST, contactIdsList);
        
        try {
            
            //wysłanie prośby o zaktualizowanie statusów na liście kontaktów
            oos.writeObject(contactsStatusesRequest);
            
        } catch(IOException e) {
            System.out.println("Błąd podczas pierodyczego wysyłania zapytania celem sprawdzania statusu kontaktów z listy kontaktów.");
            e.printStackTrace();
        }
    }
    
    /**
     * Aktualizacja statusów na liście kontaktów na podstawe otrzymanej z serwera mapy statusów
     **/
    public synchronized void contactsStatusesReceived(Packet< Map<Integer, Status> > packet)
    {
        contactStatuses = packet.object();
        
    }
    
    
    public void appClosing()
    {
        System.out.println("Funkcja obługujaca zamykanie aplikacji.");
        
        //persystowanie statusu dostępności
        Preferences preferences = Preferences.userNodeForPackage(messanger.Client.class);
        preferences.putInt(PREFERENCES_LAST_STATUS, status.getValue());
        
        //po wyłączeniu aplikacji bieżący kontakt musi przejść
        //w stan niedostępny
        status = Status.OFFLINE;
        //przesyłamy info o tym na serwer
        sendClientStatus();
        
        //zamykanie połączenia
        Packet<Integer> closePacket = new Packet<Integer>(
                            Packet.PacketCommand.CLOSE_CONNECTION, clientId);
        
        try {
            
            oos.writeObject(closePacket);
            
            socket.close();
            oos.close();
            ois.close();
            
        } catch(IOException e)
        {
            System.out.println("Błąd podczas zamykania połączenia z " + clientId);
        }
        
       
    }
    
    /**
     * Funkcja wejściowa do programu klienta
     **/
    public static void main(String[] args)
    {
        
          new Client();
        
    }
    
   
}

