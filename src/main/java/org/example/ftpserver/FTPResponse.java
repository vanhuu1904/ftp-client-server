package org.example.ftpserver;

public class FTPResponse {

    public static final String WELCOME = "220 Service ready for new user.";
    public static final String NEED_PASSWORD = "331 User name okay, need password.";
    public static final String LOGIN_SUCCESS = "230 User logged in, proceed.";
    public static final String LOGIN_INVALID = "430 Invalid username/password.";
    public static final String BAD_SEQUENCE = "503 Bad sequence of commands.";
    public static final String QUIT_SUCCESS = "221 Service closing control connection.";


    public static final String NEED_LOGIN = "530 Not logged in.";


    public static final String SYSTEM_INFO = "215 UNIX Type: L8";


    public static final String COMMAND_OKAY = "200 Command okay.";
    public static final String TYPE_I_SUCCESS = "200 Switching to Binary mode.";
    public static final String TYPE_A_SUCCESS = "200 Switching to ASCII mode.";
    public static final String DELETE_SUCCESS = "250 Delete success.";
    public static final String CLOSING_DATA_CONN = "226 Closing data connection. Requested file action successful.";
    public static final String OPEN_DATA_CONN  = "150 Opening data connection.";

    public static final String NOT_IMPLEMENTED = "502 Command not implemented.";
    public static final String FILE_UNAVAILABLE = "550 File unavailable.";
    public static final String CANT_OPEN_DATA = "425 Can't open data connection.";
    public static final String INTERNAL_ERROR = "451 Internal server error.";
    public static final String INVALID_PARAMETER = "504 Command not implemented for that parameter.";
    public static final String SYNTAX_ERROR = "501 Syntax error in parameters or arguments.";


    public static final String ENTERING_PASV = "227 Entering Passive Mode (%s,%d,%d).";
}
