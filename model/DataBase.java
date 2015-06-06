package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Klasa tworząca obiekt odpowiedzialny za tworzenie 
 * bazy danych oraz za późniejsze nawiązywanie połączenia 
 * z bazą danych
 **/
public class DataBase
{
    //stała określająca sterownik bazy danych SQLite
    public static final String DATABASE_DRIVER = "org.sqlite.JDBC";
    //stała określająca adres Url bazy danych SQLite
    public static final String DATABASE_URL = "jdbc:sqlite:database.db";
    
    // Data format: "YYYY-MM-DD HH:MM:SS.SSS".
    // wyrażenie tworzące tablicę z wiadomościami
    public static final String CREATE_MESSAGE_TABLE =
    "CREATE TABLE IF NOT EXISTS message (message_id INTEGER PRIMARY KEY AUTOINCREMENT, receiver_id INTEGER, sender_id INTEGER, message TEXT, created_date TEXT, read_date TEXT)";
   
    // wyrażenie tworzące tablicę z kontaktami
    public static final String CREATE_CONTACT_TABLE =
    "CREATE TABLE IF NOT EXISTS contact (contact_id INTEGER PRIMARY KEY AUTOINCREMENT, nickname VARCHAR(255), sha1password CHARACTER(40), first_name VARCHAR(255), last_name VARCHAR(255))";
    
    //wyrażenie tworzące tablicę z wpisami list kontaktów
    public static final String CREATE_CONTACT_LIST_TABLE =
    "CREATE TABLE IF NOT EXISTS contact_list (list_owner_id INTEGER, contact_id INTEGER, PRIMARY KEY(list_owner_id, contact_id))";

   
    //obiekt połączenia z bazą danych
    private Connection connection;
    //obiekt zapytań do bazy danych
    private Statement statement;
    
    //konstruktor bazy danych
    public DataBase()
    {
        //rejestrowanie sterownika bazy danych
        try {
            Class.forName(DATABASE_DRIVER);
        } catch(ClassNotFoundException e) {
            System.out.println("Nie odnaleziono sterownika bazdy danych SQLite.");
            e.printStackTrace();
        }
        
        //utworzenie obiektu połączenia z bazą danych
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
            statement = connection.createStatement();
        } catch(SQLException e) {
            System.out.println("Błąd podczas nawiązywania połączenia z bazą SQLite.");
            e.printStackTrace();
        }
        
        System.out.println("Baza danych otwarta z powodzeniem...");
        
        //wywołanie funkcji tworzących tablice w bazie danych jeżeli
        //te nie istnieją
        createContactTable();
        createMessageTable();
        createContactListTable();
    }
    
    /**
     * Funkcja pobierająca obiekt połączenia z bazą danych
     **/
    public Connection connection()
    {
        return connection;
    }
    
    /**
     * Metoda tworzy nową tablicę do przechowywania kontaktów 
     * o ile taka jeszcze nie istnieje. 
     * Pola Tablicy: @contact
     * - contact_id
     * - nickname
     * - sha1password
     * - first_name
     * - last_name
     **/
    private boolean createContactTable()
    {
        try {
            statement.execute(CREATE_CONTACT_TABLE);
        } catch(SQLException e)
        {
            System.out.println("Błąd przy tworzeniu tabeli @contact w bazie danych");
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Metoda tworzy nową tablicę do przechowywania list kontaktów
     * o ile taka tablica jeszcze nie istnieje w bazie danych.
     * Pola Tablicy: @contact_list
     * - list_owner_id
     * - contact_id
     **/
    private boolean createContactListTable()
    {
        try {
            statement.execute(CREATE_CONTACT_LIST_TABLE);
        } catch(SQLException e)
        {
            System.out.println("Błąd przy tworzeniu tabeli @contact_list w bazie danych");
            e.printStackTrace();
            return false;
            
        }
        
        return true;
    }
    
    /**
     * Metoda tworzy nową tablicę do przechowywania wiadomości 
     * o ile taka tablica jeszcze nie istnieje w bazie danych.
     * Pola tablicy:  @message
     * - message_id
     * - receiver_id
     * - sender_id
     * - message 
     * - created_date 
     * - read_date
     **/
    private boolean createMessageTable()
    {
        try {
            statement.execute(CREATE_MESSAGE_TABLE);
        } catch(SQLException e)
        {
            System.out.println("Błąd przy tworzeniu tabeli @message w bazie danych");
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Funkcja zamyka otwarte połączenia z bazą danych 
     **/
    public void close() {
        if(statement != null) {
            try {
                statement.close();
            } catch(SQLException e)
            {
            }
            statement = null;
        }
        
        if(connection != null)
        {
            try {
                connection.close();
            } catch(SQLException e) {
            }
            connection = null;
        }
    }
}
