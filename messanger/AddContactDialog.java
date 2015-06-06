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
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import model.Contact;


public class AddContactDialog extends JDialog implements ActionListener, FocusListener, ListSelectionListener, DocumentListener
{
    
    private Socket socket;
    private ObjectOutputStream oos;
    
    //obiekt delegaty
    private ContactAddedListener contactAddedListener;
    
    //kontrolki formularza wyszukiwania kontaktów
    private JTextField nicknameField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JButton searchButton;
    
    //kontrolki zawierające listę sugestii
    private JList<Contact> suggestedContactsList;
    private DefaultListModel<Contact> listModel;
    
    //kontrolka dodawania kontaktu
    private JButton addButton;
    
    //obiekty planowania zadania pobierania sugerownych kontaktów
    private java.util.Timer timer; //scheduler zadań
    private TimerTask task; //planowane zadanie
    
    /**
     * Klasa renderująca komórki w JList sugerowanych kontaktów
     * Obiekty komórek zawierają nick, Imię, Nazwisko
     **/
     public class SuggestedContactCellRenderer extends JPanel implements ListCellRenderer<Object>
    {
        public SuggestedContactCellRenderer()
        {
            //setOpaque(true);
        }
        
        /**
         * Przeciążona metoda, która customizuje renderowane komórki listy 
         * sugerowanych kontaktów będących wynikami wyszukiwania w bazie danych na serwerze
         **/
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setComponentOrientation(
                                ComponentOrientation.LEFT_TO_RIGHT);
            
            //pobranie obiektu sugerowanego Kontaktu
            Contact contact = (Contact) value;
            
            JLabel nickLabel = new JLabel(contact.getNickname());
            nickLabel.setPreferredSize(new Dimension(150, 20));
            nickLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));
            panel.add(nickLabel);
            
            JLabel fnLabel = new JLabel(contact.getFirstName());
            fnLabel.setPreferredSize(new Dimension(150, 20));
            fnLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));
            panel.add(fnLabel);
            
            JLabel lnLabel = new JLabel(contact.getLastName());
            lnLabel.setPreferredSize(new Dimension(150, 20));
            lnLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));
            panel.add(lnLabel);
            
            if(cellHasFocus) {
               panel.setBackground(Color.LIGHT_GRAY);
            } else {
               panel.setBackground(Color.white);

            }
            
            if(isSelected) {
                panel.setBackground(Color.LIGHT_GRAY);
            } else {
                panel.setBackground(Color.white);
            }
            
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
            
            return panel;
        }
    }
    
    
    
    public AddContactDialog(ObjectOutputStream oos) throws UnknownHostException, IOException
    {
        super();
        
        //przypisanie obiektowych strumieni wejściowych i wyjściowych
        //z gniazda sieciowego podłączonego do serwera
        this.oos = oos;
       
        createView();
        
        //dodanie listenera zdarzenia zamknięcia okna
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                contactAddedListener.addContactDialogClosed();
            }
        } );
    }
    
    /**
     * Metoda ustawiająca na obiekcie okna dialogowego obiekt nasłuchujący 
     * momentu dodania nowego kontaktu do listy kontaktów bieżącego klienta
     **/
    public void addContactAddedListener( ContactAddedListener listener)
    {
        contactAddedListener = listener;
    }
    
    /**
     * Tworzenie widoku okna dialogowego wyszukiwania kontaktów
     **/
    private void createView()
    {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setTitle("Wyszukaj kontakt...");
        setSize(500, 400);
        setLocationRelativeTo(null);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        
        //utworzenie kontrolek formularza
        JLabel nicknameLabel = new JLabel("Nick:");
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.ipadx = 0;
        constraints.insets= new Insets(2,5,5,2);
        add(nicknameLabel, constraints);
        
        nicknameField = new JTextField();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.ipadx = 40;
        constraints.insets= new Insets(2,5,5,5);
        add(nicknameField, constraints);
        nicknameField.addFocusListener(this); //dodanie focus listenera
        nicknameField.getDocument().addDocumentListener(this);
       
        JLabel firstNameLabel = new JLabel("Imię:");
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.ipadx = 0;
        constraints.insets= new Insets(2,5,5,2);
        add(firstNameLabel, constraints);
        
        firstNameField = new JTextField();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.insets= new Insets(2,5,5,5);
        constraints.gridwidth = 2;
        constraints.ipadx = 40;
        add(firstNameField, constraints);
        firstNameField.addFocusListener(this); //dodanie focus listenera
        firstNameField.getDocument().addDocumentListener(this);
        
        JLabel lastNameLabel = new JLabel("Nazwisko: ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets = new Insets(2,5,5,2);
        constraints.gridwidth = 1;
        constraints.ipadx = 0;
        add(lastNameLabel, constraints);
        
        lastNameField = new JTextField();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(2,5,5,5);
        constraints.gridwidth = 2;
        constraints.ipadx = 40;
        add(lastNameField, constraints);
        lastNameField.addFocusListener(this); //dodanie focus listenera
        lastNameField.getDocument().addDocumentListener(this);
        
        //dodanie listy sugesti kontaktów
        listModel = new DefaultListModel<Contact>();
        suggestedContactsList = new JList<Contact>(listModel);
        suggestedContactsList.setCellRenderer( new SuggestedContactCellRenderer());
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 3;
        constraints.ipadx = 0;
        constraints.ipady = 40;
        add( new JScrollPane(suggestedContactsList), constraints);
        
        //dodanie listenera selekcji kontaktu na liście sugerowanych kontaktów
        suggestedContactsList.addListSelectionListener(this);
        
        //utworzenie kontrolki JButton
        searchButton = new JButton("Szukaj kontaktu");
        searchButton.addActionListener(this);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.ipadx = 0;
        constraints.ipady = 0;
        constraints.gridwidth = 1;
        add(searchButton, constraints);
        
        //dodanie przycisku dodawania kontaktów
        addButton = new JButton("Dodaj Kontakt");
        addButton.addActionListener(this);
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        add(addButton, constraints);
        
        
        //wyswietlenie komponentów
        pack();
        setSize(500,400);
        setVisible(true);

        
    }
    
    
    /**
     * Funkcja obsługi zdarzeń kliknięcia przycisku Wyszukaj/Dodaj Kontakt
     **/
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == searchButton) {
            
            System.out.println("Search Button kliknięty...");
            
            //anulowanie ewentualnego wcześniej zaplanowanego
            //zadania pobierania listy sugerowanych kontaktów
            if(timer != null && task != null)
            {
                task.cancel();
                timer.cancel();
            }
            
            //wywołanie metody pobierającej sugesite kontatów
            getContactsSuggestionFor(nicknameField.getText(),
                                     firstNameField.getText(),
                                     lastNameField.getText());
            
        } else if(e.getSource() == addButton) {
            
            System.out.println("Add Button kliknięty...");
            
            Contact c =  suggestedContactsList.getSelectedValue();
            
            if(c == null) {
                JOptionPane.showMessageDialog(this, "Nie wybrano kontaktu...",
                                              "Błąd dodawania kontaktu!",
                                              JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            contactAddedListener.contactAdded(c);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            
        }
    }
    
    /**
     * FocusListener - zdarzenie gdy poszczególne pola uzyskują lub pracą focus
     **/
    @Override
    public void focusGained(FocusEvent e) {
        //ignorowane
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        if(e.getSource() == nicknameField) {
            System.out.println("Nick Field Lost Focus...");
            
        } else if(e.getSource() == firstNameField) {
            
            System.out.println("First Name Field Lost Focus...");
            
        } else if(e.getSource() == lastNameField) {
            
            System.out.println("Last Name Field Lost Focus...");
            
        }
    }
    
    /**
     * ListSelectionListener - zdarzenia listy sugerowanych kontaktów
     **/
    @Override
    public void valueChanged(ListSelectionEvent event) {
        //to do...
    }
    
    /**
     * DocumentListener - zdarzenie pól tekstowych wpisywania i usuwania
     * znaków. Pozwala na schedulowanie taksa pobierania listy kontaktów
     * jeżeli użytkownik przez 3 sek nie modyfikuje pól tekstowych
     **/
    @Override
    public void changedUpdate(DocumentEvent e) {
        //ignorujemy ten event
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        //wywołanie metody reagującej na zmianę danych wyszukiwania
        searchDataChanged();
    }
    
    @Override
    public void removeUpdate(DocumentEvent e) {
        //analogiczne wywołanie metody reagującej na zmianę danych wyszukiwania
        searchDataChanged();
    }
    
    /**
     * Korzystająć z Timera schedulujemy TimerTask pobierania 
     * sugerowanych kontaktów na podstawie wprowadzonych danych.
     * Zadanie jest zaplanowane do wykonania po upływie 3000 milisekund.
     * Jeżeli bieżąca metoda zostanie wywołana ponownie przed upływem
     * wyznaczonych 3000 milisekund to dotychczasowe zadanie zostanie
     * anulowane i zaplanowane zostanie nowe zadanie z nowym inputem.
     **/
    private void searchDataChanged() {
        System.out.println("Użytkownik edytował pola danych wyszukiwania...");
        
        //Jeżeli zadanie wyszukiwania sugestii zostało zaplanowane
        //to anulujemy związany z nim obiekt zadani i timer
        if(timer != null && task != null) {
            task.cancel();
            timer.cancel();
        }
        
        //Tworzenie nowego timera i zadania
        timer = new java.util.Timer();
        task = new TimerTask() {
            
            @Override
            public void run() {
                //zaplanowane zadanie pobierania sugesii kontaktów
                //na podstawie wpisanych danych inputowych
                getContactsSuggestionFor(nicknameField.getText(),
                                         firstNameField.getText(),
                                         lastNameField.getText());
                
                
            }
        };
        
        timer.schedule(task, 3000);
    }
    
    /**
     * Metoda wykonywana w wyniku kliknięcia przycisku "Szukaj Kontaktu"
     * lub zaplanowanego TimerTaska który podczas dłuższej bezczynności
     * uaktualnia listę sugerowanych kontaktów na podstawie danych 
     * wprowadzonych w pola inputowe wyszukiwarki
     **/
    private synchronized void getContactsSuggestionFor(String nick,
                                                     String firstName,
                                                     String lastName)
    {
        System.out.println("Pobieranie sugestii kontaktów dla:");
        System.out.println("Nick: " + nick);
        System.out.println("Imię: " + firstName);
        System.out.println("Nazwisko: " + lastName);
        
        //opakowanie danych wyszukiwania kontaktów w obiekt Contact
        Contact searchData = new Contact(null, 0,
                                         nick, null,
                                         firstName, lastName);
        
        //opakowanie obiektu Contact w pakiet Packet<Contact> zapytania
        //o listę kontaktów zgodnych z atrybutami opakowywanego kontaktu
        Packet<Contact> request = new Packet<Contact>(
           Packet.PacketCommand.SEARCH_CONTACTS_REQUEST, searchData);
        
        try {
            
            //przesłanie pakietu z danymi poszukiwanego kontaktu
            oos.writeObject(request);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas połączenia sieciowego przesyłającego listę wyszukanych kontaktów.");
        }

    }
    
    /**
     * Metoda przetwarzająca odpowiedź z serwera z wynikami wyszukiwania 
     * tj. listą sugerowanych kontaktów pasujących do patternu. 
     * Zadaniem metody jest przetworzenie i wyświetlenie kontaktów.
     **/
    public void updateSuggestedContacts(
                                Packet<java.util.List<Contact>> response)
    {
        //pobranie kontaktów
        java.util.List<Contact> suggestedContacts =
        (java.util.List<Contact>) response.object();
        
        //usunięcie dotychczasowych sugestii z listy
        listModel.clear();
        
        //wypełnieie listy nowymi sugestiami
        for(Contact c : suggestedContacts)
            listModel.addElement(c);
    }
}

