package AddressBookDB;

public class AddressBookException extends Exception {
	public enum ExceptionType {
        SQL_EXCEPTION
    }

    public ExceptionType type;

    public AddressBookException(String message, ExceptionType type) {
        super(message);
        this.type = type;
    }

    public AddressBookException(String message, ExceptionType type, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
    
    public AddressBookException(String message, String name) {
		super(message);
		this.type = ExceptionType.valueOf(name);
	}
}
