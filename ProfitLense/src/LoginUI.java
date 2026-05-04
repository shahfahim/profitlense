import java.util.Scanner;

public class LoginUI {
    private static final String[] emailadd = {
        "talha@gmail.com",
        "fahim@gmail.com",
        "sanjida@gmail.com"
    };

    private static final String password = "admin123";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        System.out.print("Enter Password: ");
        String passInput = scanner.nextLine();

        try {
            String pass = passInput;
            boolean existmail = false;
            for (String validmail : emailadd) {
                if (email.equals(validmail)) {
                    existmail = true;
                    break;
                }
            }

            if (existmail && pass.equals(password)) {
                System.out.println("✅ Successfully logged in!");
                System.out.println("Email: " + email);
                System.out.println("Password: " + pass);
            } else if (!existmail&& pass.equals(password)) {
                System.out.println("❌ Email is wrong!");
            } else if (existmail && !pass.equals(password)) {
                System.out.println("❌ Password is wrong!");
            } else {
                System.out.println("❌ Email and Password are wrong!");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Something went wrong!");
        }
    }
}
