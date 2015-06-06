package messanger;

import model.Contact;

/**
 * Interfejs listenera zdarzeń okna dodawania kontaktów
 **/
public interface ContactAddedListener
{
    public void contactAdded(Contact contact);
    public void addContactDialogClosed(); 
};