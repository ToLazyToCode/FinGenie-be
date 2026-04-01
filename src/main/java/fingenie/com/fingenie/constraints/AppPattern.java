package fingenie.com.fingenie.constraints;

public class AppPattern {
    public static final String EMAIL = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$"; // Minimum eight characters, at least one letter and one number
    public static final String PHONE_PATTERN = "^[0-9]{10}$";
    public static final String DATE_PATTERN = "^\\d{4}[-/]\\d{2}[-/]\\d{2}$"; // YYYY-MM-DD
}
