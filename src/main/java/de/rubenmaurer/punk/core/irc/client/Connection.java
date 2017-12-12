package de.rubenmaurer.punk.core.irc.client;

import akka.actor.ActorRef;
import akka.io.TcpMessage;
import de.rubenmaurer.punk.util.Notification;
import de.rubenmaurer.punk.util.Settings;
import de.rubenmaurer.punk.util.Template;

/**
 * Connection class for storing all needed information about a connection.
 *
 * @author Ruben Maurer
 * @version 1.0
 * @since 1.0
 */
public class Connection {

    /**
     * The used connection actor.
     */
    private final ActorRef connection;

    /**
     * The nickname of this connection.
     */
    private String nickname = "";

    /**
     * The real-name of this connection.
     */
    private String realname = "";

    /**
     * Is connection logged in?
     */
    private boolean login = false;

    /**
     * Get connection nickname.
     *
     * @return the nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Set connection nickname.
     *
     * @param nickname the new nickname
     */
    public void setNickname(String nickname) {
        if (!ConnectionManager.hasNickname(nickname)) {
            if (login) {
                write(Notification.get(Notification.Reply.RPL_NICKCHANGE,
                        new String[] { this.nickname, nickname }));
            }

            this.nickname = nickname;
            tryLogin();
            return;
        }

        write(Notification.get(Notification.Error.ERR_NICKNAMEINUSE, nickname));
    }

    /**
     * Get connection real-name.
     *
     * @return the real-name
     */
    public String getRealname() {
        return realname;
    }

    /**
     * Set connection real-name
     *
     * @param realname the new real-name
     */
    public void setRealname(String realname) {
        if(this.realname.isEmpty()) {
            this.realname = realname;
            tryLogin();
            return;
        }

        write(Notification.get(Notification.Error.ERR_ALREADYREGISTRED, Settings.hostname()));
    }

    /**
     * Get the connection
     *
     * @return the connection
     */
    public ActorRef getConnection() {
        return connection;
    }

    /**
     * Create new connection.
     *
     * @param connection the used connection actor
     */
    private Connection(ActorRef connection) {
        this.connection = connection;
    }

    /**
     * Write a string on the connection.
     *
     * @param message the message to write
     */
    public void write(String message) {
        connection.tell(message, ActorRef.noSender());
    }

    /**
     * Write a string to a specific connection.

     * @param message the message to write
     */
    private void write(Object message) {
        connection.tell(message, ActorRef.noSender());
    }

    /**
     * Send message to another user.
     *
     * @param nickname the user to chat with
     * @param message the message to send
     */
    public void chat(String nickname, String message) {
        for (Connection con : ConnectionManager.connections) {
            if (con.nickname.equals(nickname)) {
                write(Message.create(con.getConnection(), nickname, message, this.nickname));
                return;
            }
        }

        write(Notification.get(Notification.Error.ERR_NOSUCHNICK, nickname));
    }

    /**
     * Logout a connection.
     *
     * @param message the quit message
     */
    public void logout(String message) {
        ConnectionManager.connections.remove(this);

        write(Notification.get(Notification.Error.ERROR, message));
        connection.tell(TcpMessage.close(), ActorRef.noSender());
    }

    /**
     * Try to login the connection.
     */
    private void tryLogin() {
        if (!isLogged() && (!nickname.isEmpty() && !realname.isEmpty())) {
            connection.tell(Notification.get(Notification.Reply.RPL_WELCOME,
                    new String[] { nickname, realname, Settings.hostname()}), ActorRef.noSender());

            write(Notification.get(Notification.Reply.RPL_YOURHOST));
            write(Notification.get(Notification.Reply.RPL_CREATED));
            write(Notification.get(Notification.Reply.RPL_MYINFO));

            login = true;
        }
    }

    /**
     * Is connection logged in
     *
     * @return is logged in?
     */
    private boolean isLogged() {
        return login;
    }

    /**
     * Plays ping pong...
     */
    public void pong() {
        write(Template.get("pong").toString());
    }

    /**
     * Create a new connection object.
     *
     * @param connection the used connection actor
     * @return the created connection object
     */
    public static Connection create(ActorRef connection) {
        return new Connection(connection);
    }
}
