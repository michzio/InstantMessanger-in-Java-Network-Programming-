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
import java.util.regex.*;
import model.Contact;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.prefs.*;


public class SignInDialog extends JDialog implements ActionListener, DocumentListener, FocusListener
{
    //wyrażenie regularne do testowania Imienia/Nazwiska
    public static final String NAME_REGEX = "^[\\p{L} .'-]+$";
    /**
     * wyrażenie regularne do testowania hasła:
     * ^ - początek wyrażenia
     * (?=.*[0-9]) - co najmniej jedna cyfra
     * (?=.*[a-z]) - co najmniej jedna mała litera
     * (?=.*[A-Z]) - co najmniej jedna duża litera
     * (?=.*[@#$%^&+=!]) - co najmniej jeden znak specjalny
     * (?=[\\S]+$) - nie dozwolone spacje
     * .{4,10} - co najmniej 5 znaków
     * $ - koniec wyrażenia regularnego
     **/
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=[\\S]+$).{4,10}$";
    
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    
    //obiekt delegaty
    AuthorizationCompletedListener authCompletedListener;
    
    //kontrolki formularza logowania
    JPanel signInPanel;
    private JButton signInButton;
    JTextField loginField;
    JPasswordField passwordField;
    Checkbox checkbox;
    
    //kontrolki formularza rejestracji
    JPanel signUpPanel;
    private JButton signUpButton;
    JTextField nickField;
    JTextField firstNameField;
    JTextField lastNameField;
    JPasswordField passField;
    JPasswordField confirmPassField;
    
    
    //flagi sprawdzane przed wysłaniem formularza rejestracji
    boolean isNickCorrect = false;
    boolean isFirstNameCorrect = false;
    boolean isLastNameCorrect = false;
    boolean isPassCorrect = false;
    boolean arePassMatch = false;
    
    public SignInDialog(ObjectInputStream ois, ObjectOutputStream oos) throws UnknownHostException, IOException
    {
        super();
        
        //przypisanie obiektowych strumieni wejściowych i wyjściowych
        //z gniazda sieciowego podłączonego do serwera
        this.ois = ois;
        this.oos = oos;
       
        createView();
    }
    
    /**
     * Metoda ustawiająca na obiekcie okna dialogowego obiekt nasłuchujący 
     * moment poprawnej autoryzacji użytkownika (klienta)
     **/
    public void addAuthorizationCompletedListener( AuthorizationCompletedListener listener)
    {
        authCompletedListener = listener;
    }
    
    private void createView()
    {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setTitle("Zalguj się...");
        setSize(400, 400);
        setLocationRelativeTo(null);
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridx = 0;
        
        //kontener zakładek umożliwia przełączanie screenu
        //logowania i rejestracji użytkownika
        JTabbedPane tabbedPane = new JTabbedPane();
        
        //panel z forumlarzem logowania
        signInPanel = new JPanel(new GridBagLayout());
        
        //utworzenie kontrolek formularza
        JLabel loginLabel = new JLabel("Podaj login:");
        constraints.gridy = 0;
        constraints.insets= new Insets(2,10,2,10);
        signInPanel.add(loginLabel, constraints);
        loginField = new JTextField();
        constraints.gridy = 1;
        constraints.insets= new Insets(2,10,20,10);
        signInPanel.add(loginField, constraints);
        
        JLabel passwordLabel = new JLabel("Podaj hasło:");
        constraints.gridy = 2;
        constraints.insets= new Insets(2,10,2,10);
        signInPanel.add(passwordLabel, constraints);
        passwordField = new JPasswordField();
        constraints.gridy = 3;
        constraints.insets= new Insets(2,10,20,10);
        signInPanel.add(passwordField, constraints);
        
        //utworzenie checkboxa
        checkbox = new Checkbox("Pozostań zalogowany na tym komputerze.");
        constraints.gridy = 4;
        signInPanel.add(checkbox, constraints);
        
        //utworzenie kontrolki JButton
        constraints.insets= new Insets(20,10,20,10);
        signInButton = new JButton("Zaloguj się");
        signInButton.addActionListener(this);
        constraints.gridy = 5;
        signInPanel.add(signInButton, constraints);
        
        tabbedPane.addTab("Zaloguj się", null, signInPanel,
                          "Podaj login i hasło aby zalogować się...");
        
        //panel z formularzem rejestracji
        signUpPanel = new JPanel(new GridBagLayout());
        
        //utworzenie kontrolek formularza
        JLabel nickLabel = new JLabel("Podaj login:");
        constraints.gridy = 0;
        constraints.insets = new Insets(2, 10, 2, 10);
        signUpPanel.add(nickLabel, constraints);
        nickField = new JTextField();
        nickField.getDocument().addDocumentListener(this);
        nickField.addFocusListener(this);
        constraints.gridy = 1;
        //constraints.insets = new Insets(2, 10, 10, 10);
        signUpPanel.add(nickField, constraints);
        
        JLabel firstNameLabel = new JLabel("Imię:");
        constraints.gridy = 2;
        //constraints.insets = new Insets(2, 10, 2, 10);
        signUpPanel.add(firstNameLabel, constraints);
        firstNameField = new JTextField();
        firstNameField.getDocument().addDocumentListener(this);
        firstNameField.addFocusListener(this);
        constraints.gridy = 3;
        //constraints.insets = new Insets(2, 10, 10, 10);
        signUpPanel.add(firstNameField, constraints);
        
        JLabel lastNameLabel = new JLabel("Nazwisko:");
        constraints.gridy = 4;
        //constraints.insets = new Insets(2, 10, 2, 10);
        signUpPanel.add(lastNameLabel, constraints);
        lastNameField = new JTextField();
        lastNameField.getDocument().addDocumentListener(this);
        lastNameField.addFocusListener(this);
        constraints.gridy = 5;
        //constraints.insets = new Insets(2, 10, 10, 10);
        signUpPanel.add(lastNameField, constraints);
        
        JLabel passLabel = new JLabel("Hasło:");
        constraints.gridy = 6;
        //constraints.insets = new Insets(2, 10, 2, 10);
        signUpPanel.add(passLabel, constraints);
        passField = new JPasswordField();
        passField.getDocument().addDocumentListener(this);
        passField.addFocusListener(this);
        constraints.gridy = 7;
        //constraints.insets = new Insets(2, 10, 10, 10);
        signUpPanel.add(passField, constraints);
        
        JLabel confirmPassLabel = new JLabel("Potwierdź hasło:");
        constraints.gridy = 8;
        //constraints.insets = new Insets(2, 10, 2, 10);
        signUpPanel.add(confirmPassLabel, constraints);
        confirmPassField = new JPasswordField();
        confirmPassField.getDocument().addDocumentListener(this);
        confirmPassField.addFocusListener(this);
        constraints.gridy = 9;
        //constraints.insets = new Insets(2, 10, 10, 10);
        signUpPanel.add(confirmPassField, constraints);
        
        signUpButton = new JButton("Utwórz konto");
        signUpButton.addActionListener(this);
        constraints.gridy = 10;
        signUpPanel.add(signUpButton, constraints);
        
        tabbedPane.addTab("Utwórz konto", null, signUpPanel,
                          "Zarejestruj nowe konto w Messangerze!");
        add(tabbedPane);
        
        
        //wyswietlenie komponentów
        pack();
        setSize(400,400);
        setVisible(true);

        
    }
    
    
    /**
     * Funkcja obsługi zdarzeń kliknięcia przycisku Zaloguj/Zarejestruj się
     **/
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == signInButton) {
            
            System.out.println("Sign In Button kliknięty...");
            trySignIn();
            
        } else if(e.getSource() == signUpButton) {
            
            System.out.println("Sign Up Button kliknięty...");
            trySignUpNewUser();
            
        }
    }
    
    /**
     * Metoda podejmuje próbę zalogowania użytkownika
     * Pobiera z odpowiednich pól: login i hasło 
     * Następnie wysyła odpowiedni pakiet z tymi danymi 
     * na serwer i odbiera odpowiedź z potwierdzeniem poprawności logowania
     * Otrzymuje ID użytkownika z bazy danych lub 0 - niepoprawne logowanie
     **/
    private void trySignIn() {
        
        //1. SPRAWDZENIE POPRAWNEGO WYPEŁNIENIA FORMULARZU
        
        if(loginField.getText().length() < 1)
        {
            JOptionPane.showMessageDialog(this, "Nie podano loginu!",
                                          "Błąd Logowania", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if(passwordField.getText().length() < 1)
        {
            JOptionPane.showMessageDialog(this, "Nie podano hasła!",
                                          "Błąd Logowania", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        //2. LOGOWANIE

        //utworzenie obiektu Contact z loginem i hasłem przeznaczonym do autoryzacji klienta
        Contact authUser = null;
        
        try {
            
            authUser = new Contact(null, 0,
                                           loginField.getText(),
                                           Contact.SHA1(passwordField.getText()),
                                           null, null);
            
        } catch(NoSuchAlgorithmException e) {
            System.out.println("Błąd podczas enkodowania hasła algorytmem SHA1 podczas logowania.");
        } catch(UnsupportedEncodingException e) {
            System.out.println("Błąd podczas enkodowania hasła algorytmem SHA1 podczas logowania.");
        }
        
        if(authUser == null) {
            JOptionPane.showMessageDialog(null, "Nieoczekiwany błąd podczas logowania!", "Błąd Logowania!", JOptionPane.WARNING_MESSAGE);
        }
        
        //opakowanie obiektu Contact (danych logowania) w pakiet
        //Packet<Contact> z komendą SIGN_IN_REQUEST
        Packet<Contact> request = new Packet<Contact>(
                    Packet.PacketCommand.SIGN_IN_REQUEST, authUser);
        
        try {
            
            //przesłanie pakietu z danymi logowania na serwer
            oos.writeObject(request);
            
            //odbieranie odpowiedzi z serwera z potwierdzeniem uwierzytelnienia
            //albo ID > 0 zalogowanego klienta albo ID = 0 -> ERROR
            Packet response = (Packet) ois.readObject();
            
            //sprawdzenie poprawności pakietu
            if(response.command() == Packet.PacketCommand.SIGN_IN_RESPONSE)
            {
                Integer contact_id = (Integer) response.object();
                
                if(contact_id < 1) {
                    JOptionPane.showMessageDialog(this, "Podany login lub hasło są niepoprawne.", "Błąd Logowania!", JOptionPane.WARNING_MESSAGE);
                } else {
                    
                    if(checkbox.getState())
                    {
                        //persystowanie identyfikatora klienta lokalnie
                        //pobranie preferencji użytkownika dla bieżącego pakietu
                        Preferences preferences = Preferences.userNodeForPackage(messanger.Client.class);
                        //zapisanie identyfikatora użytkownika
                        preferences.putInt(Client.PREFERENCES_CLIENT_ID_KEY, contact_id);
                    
                        System.out.println("Identyfikator klienta został persystowany lokalnie.");
                    }
                    
                    authCompletedListener.grantedContactId(contact_id);
                    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                }
                
            } else {
                System.out.println("Odebrano niepoprawny pakiet podczas próby logowania.");
            }

            
            
        } catch(IOException e) {
            System.out.println("Błąd podczas komunikacji przez sieć podczas logowania.");
        } catch(ClassNotFoundException e) {
            System.out.println("Błąd castowania obiektu zwróconego w odpowiedzi na próbę logowania.");
        }

    
    }
    
    /**
     * Metoda podejmująca próbę zarejestrowania użytkownika
     * Najpierw sprawdza poprawnośc wypełnienia formularzu
     * Następnie jeżeli wszystko OK to podejmuje próbę rejestracji 
     * wysyłając odpowiedni pakiet z danymi formularza na serwer 
     * Otrzymuje odpowiedź w postaci ID użytkownika lub 0 - niepoprawna rejestracja
     **/
    private void trySignUpNewUser() {
        
       //1. SPRAWDZENIE BŁĘDÓW W FORMULARZU
        if(!isNickCorrect) {
            
            JOptionPane.showMessageDialog(this, "Podany login jest już zajęty!", "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if(!isFirstNameCorrect) {
            JOptionPane.showMessageDialog(this, "Niepoprawne imię!", "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
            return;

        }
        
        if(!isLastNameCorrect) {
            JOptionPane.showMessageDialog(this, "Niepoprawne nazwisko!", "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
            return;
            
        }
        
        if(!isPassCorrect) {
            JOptionPane.showMessageDialog(this, "Hasło musi zawierać: mała literę, dużą literę, znak specjalny, cyfrę!", "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
            return;
            
        }
        
        if(!arePassMatch) {
            JOptionPane.showMessageDialog(this, "Podane hasła nie pasują do siebie!", "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        //2. REJESTRACJA
        
        //utworzenie obiektu Contact przekazując jako pierwszy argument
        //(Connection) null, ponieważ klient nie ma dostępu do bazy danych
        Contact newUser = null;
        
        try {
            
            newUser = new Contact(null, 0,
                                  nickField.getText(),
                                  Contact.SHA1(passField.getText()),
                                  firstNameField.getText(),
                                  lastNameField.getText());
        
            System.out.println("Zakodowane hasło: "
                           + Contact.SHA1(passField.getText()) );
            
        } catch(NoSuchAlgorithmException e) {
            System.out.println("Błąd podczas enkodowania hasła algorytmem SHA1 przy rejestracji.");
        } catch(UnsupportedEncodingException e) {
            System.out.println("Błąd podczas enkodowania hasła algorytmem SHA1 przy rejestracji.");
        }
        
        if(newUser == null) {
            JOptionPane.showMessageDialog(this, "Niespodziewany błąd podczas rejestracji!",
                                          "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        //opakowanie obiektu Contact (nowego użytkownika) w pakiet
        //Packet<Contact> z komendą SIGN_UP_REQUEST
        Packet<Contact> request = new Packet<Contact>(
                                Packet.PacketCommand.SIGN_UP_REQUEST, newUser);
        
        try {
            //przesłanie pakietu z nowym userem do serweru
            oos.writeObject(request);
                           
            //odebranie pakietu zwrotnego z ID przypisanym
            //klientowi lub ID=0 -> ERROR
            Packet response = (Packet) ois.readObject();
            
            //sprawdzenie poprawności pakietu
            if(response.command() ==
               Packet.PacketCommand.SIGN_UP_RESPONSE)
            {
                Integer contect_id = (Integer) response.object();
                
                if(contect_id < 1) {
                    JOptionPane.showMessageDialog(this, "Próba rejestracji zakończona niepowodzeniem po stronie serwera!", "Błąd Rejestracji!", JOptionPane.WARNING_MESSAGE);
                } else {
                    authCompletedListener.grantedContactId(contect_id);
                    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                }
                
            } else {
                System.out.println("Odebrano niepoprawny pakiet podczas próby rejestracji.");
            }

            
        } catch(IOException e) {
            System.out.println("Błąd podczas komunikacji przez sieć podczas próby rejesracji.");
        } catch(ClassNotFoundException e) {
            System.out.println("Błąd castowania obiektu zwróconego w odpowiedzi podczas próby rejestracji.");
        }
    }
    
    /**
     * DocumentListener - zdarzenia TextFiledów, wpisywanie i usuwanie poszczególnych
     * znaków w sumie mało przydatne do weryfikacji wprowadzonych danych 
     * lepiej użyć FocusListener.focusLost()
     **/
    @Override
    public void changedUpdate(DocumentEvent e) {
        //System.out.println("Text Field Document Changed Update...");
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        //System.out.println("Text Field Document Insert Update...");
    }
    
    @Override
    public void removeUpdate(DocumentEvent e) {
        //System.out.println("Text Field Document Remove Update...");
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
        if(e.getSource() == nickField) {
            System.out.println("Nick Field Lost Focus...");
            //sprawdzenie zajętości nicku
            //callback do serwera
            checkNickIsFree(nickField.getText());
            
        } else if(e.getSource() == firstNameField) {
            
            System.out.println("First Name Field Lost Focus...");
            checkNameCorrect(firstNameField);
            
        } else if(e.getSource() == lastNameField) {
            
            System.out.println("Last Name Field Lost Focus...");
            checkNameCorrect(lastNameField);
            
        } else if(e.getSource() == passField) {
            System.out.println("Pass Field Lost Focus...");
            checkPassCorrect(passField);
            checkConfirmPassCorrect(confirmPassField);
                             
        } else if(e.getSource() == confirmPassField) {
            
            System.out.println("Confirm Pass Field Lost Focus...");
            checkConfirmPassCorrect(confirmPassField);
        }
    }
    
    
    /**
     * Dodaje flage określającą, że pole jest poprawne
     **/
    private void flagFieldAsCorrect(JTextField field)
    {
        Border border = BorderFactory.createLineBorder(new Color(0.2f,1.0f,0.2f));
        field.setBorder(border);
    }
    
    /**
     * Dodaje flage określającą, że pole jest niepoprawne
     **/
    private void flagFieldAsIncorrect(JTextField field)
    {
        Border border = BorderFactory.createLineBorder(Color.red);
        field.setBorder(border);
    }
    
    private void checkNickIsFree(String newNickName) {
        
        //utworzenie pakietu do przesłania przez sieć
        //opakowującego String'ową nazwę nicku
        Packet<String> request = new Packet<String>(Packet.PacketCommand.IS_NICK_OCCUPIED_REQUEST,
                                                   newNickName);
        try {
            //wysłanie pakietu z zapytaniem
            oos.writeObject(request);
        
            //odebranie pakietu
            Packet response = (Packet) ois.readObject();
            //sprawdzenie poprawności pakietu
            if(response.command() ==
                                Packet.PacketCommand.IS_NICK_OCCUPIED_RESPONSE)
            {
                
                //konwersja opakowanego obiektu na typ Boolean
                //zwrócona flaga oznacza IS_NICK_OCCUPIED:
                // true - nick zajęty, false - nick wolny
                //stąd musimy przypisać zaprzeczenie odebranej flagi
                isNickCorrect = !((Boolean) response.object());
            } else {
                System.out.println("Odebrano niepoprawny pakiet przy sprawdzaniu zajętości nicku!");
            }
        } catch(IOException e) {
           
            System.out.println("Błąd podczas komunikacji przez sieć przy sprawdzaniu zajętości nicku!");
            
        } catch(ClassNotFoundException e) {
            
            System.out.println("Niepoprawne castowanie z obiektowego strumienia wejściowego (sprawdzanie nicku)!");
        }
        
        
        if(isNickCorrect) {
            flagFieldAsCorrect(nickField);
        } else {
            flagFieldAsIncorrect(nickField);
        }
        
    }
    
    /**
     * Metoda sprawdza poprawność pola z imieniem lub nazwiskiem
     **/
    private void checkNameCorrect(JTextField field) {
        //Utworzenie wzroca wyrażeń regularnych
        Pattern pattern = Pattern.compile(NAME_REGEX,
                                          Pattern.CASE_INSENSITIVE);
        //Sprawdzenie zawartości pola z imieniem
        Matcher matcher = pattern.matcher(field.getText());
        if(matcher.find()) {
            if(field == firstNameField) {
                isFirstNameCorrect = true;
            } else if(field == lastNameField) {
                isLastNameCorrect = true;
            }
             flagFieldAsCorrect(field);
        } else {
            if(field == firstNameField) {
                isFirstNameCorrect = false;
            } else if(field == lastNameField) {
                isLastNameCorrect = false;
            }
            flagFieldAsIncorrect(field);
        }

    }
    
    /**
     * Metoda sprawdza poprawność pola z hasłem
     **/
    private void checkPassCorrect(JTextField field)
    {
        //użycie uproszczonej metody testowania ciągu znaków względem
        //wyrażenia regularnego -> funkcja String.matches()
        //deleguje ona wykonanie do obiektu Matcher jak w checkNameCorrect()
        //używanie Pattern + Matcher lepsze gdy tym samym skompilowanym
        //wyrażeniem regularnym sprawdzamy wiele stringów
        if(field.getText().matches(PASSWORD_REGEX)) {
            isPassCorrect = true;
            flagFieldAsCorrect(field);
        } else {
            isPassCorrect = false;
            flagFieldAsIncorrect(field);
        }
    }
    
    /**
     * Metoda sprawdzajaca czy potwierdzenie hasła zgadza sie z wcześniej podanym hasłem
     **/
    private void checkConfirmPassCorrect(JTextField field)
    {
        if(field.getText().equals(passField.getText())) {
            //hasła zgodne
            arePassMatch = true;
            flagFieldAsCorrect(field);
        } else {
            //hasła niezgodne
            arePassMatch = false;
            flagFieldAsIncorrect(field);
        }
    }
}

