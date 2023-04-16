package pt.unl.fct.di.adc.firstwebapp.util;


//codifica informacoes relativas a autor
public class LoginData {

    public static final String INDEFINIDO = "INDEFINIDO";
    public static final String INATIVO = "inactive";

    public String user_name; // unique username
    public String user_email;
    public String user_pwd;


    public String user_name_full; // unique username
    public boolean isPrivate;
    public String phoneNumber;
    public String cellPhoneNumber;
    public String address;
    public String nif;
    public int user_role; // 0 == USER; 1 == GBO; 2 == GS; 3 == SU
    public String state;


    public LoginData(){ }

    public LoginData(String email, String name, String password, String user_name_full) {
        this.user_name = name;
        this.user_name_full = user_name_full;
        this.user_email = email;
        this.user_pwd = password;
        this.isPrivate = true;
        this.phoneNumber = INDEFINIDO;
        this.cellPhoneNumber = INDEFINIDO;
        this.address = INDEFINIDO;
        this.nif = INDEFINIDO;
        this.user_role = 0;
        this.state = INATIVO;
    }

    public LoginData(String email, String name, String password, String user_name_full,
                     String isPrivate, String phoneNumber,
                     String cellPhoneNumber, String address,
                     String nif, String role, String state
    ) {
        this(email, name, password, user_name_full);
        this.isPrivate = Boolean.parseBoolean(isPrivate);
        this.phoneNumber = phoneNumber;
        this.cellPhoneNumber = cellPhoneNumber;
        this.address = address;
        this.nif = nif;
        this.user_role = Integer.parseInt(role);
        this.state = state;
    }


    public boolean isValid(){
        return this.user_name != null && this.user_name.length() > 0;
    }


    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_pwd() {
        return user_pwd;
    }

    public void setUser_pwd(String user_pwd) {
        this.user_pwd = user_pwd;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCellPhoneNumber() {
        return cellPhoneNumber;
    }

    public void setCellPhoneNumber(String cellPhoneNumber) {
        this.cellPhoneNumber = cellPhoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public int getUser_role() {
        return user_role;
    }

    public void setUser_role(int user_role) {
        this.user_role = user_role;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUser_name_full() {
        return user_name_full;
    }

    public void setUser_name_full(String user_name_full) {
        this.user_name_full = user_name_full;
    }

}
