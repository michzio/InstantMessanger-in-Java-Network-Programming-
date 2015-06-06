package messanger;

/**
 * Interfejs listenera zdarzeń procesu autoryzacji użytkownika
 **/
public interface AuthorizationCompletedListener
{
    public void grantedContactId(int contact_id); 
};