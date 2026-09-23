// Administrator.java (Старший оператор / Админ)
package helpdesk.model;

public class Administrator extends User {
    public Administrator(long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public void performAction() {
        System.out.println("Старший оператор " + getName() + " управляет правилами поддержки и возвратами.");
    }
}