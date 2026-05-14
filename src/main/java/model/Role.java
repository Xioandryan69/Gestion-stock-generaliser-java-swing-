package model;

public enum Role
{
    ADMINISTRATEUR ,
    CLIENT,
    INVITE ;

    public static Role getADMINISTRATEUR() {
        return ADMINISTRATEUR;
    }

    public static Role getCLIENT() {
        return CLIENT;
    }

    public static Role getINVITE() {
        return INVITE;
    }
}