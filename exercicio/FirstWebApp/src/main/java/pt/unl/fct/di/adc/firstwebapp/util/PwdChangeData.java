package pt.unl.fct.di.adc.firstwebapp.util;

public class PwdChangeData {

    public String old_pwd;
    public String new_pwd;
    public String new_pwd_conf;

    public PwdChangeData(){}

    public PwdChangeData(String old_pwd, String new_pwd, String new_pwd_conf) {
        this.old_pwd = old_pwd;
        this.new_pwd = new_pwd;
        this.new_pwd_conf = new_pwd_conf;
    }

    public String getOld_pwd() {
        return old_pwd;
    }

    public void setOld_pwd(String old_pwd) {
        this.old_pwd = old_pwd;
    }

    public String getNew_pwd() {
        return new_pwd;
    }

    public void setNew_pwd(String new_pwd) {
        this.new_pwd = new_pwd;
    }

    public String getNew_pwd_conf() {
        return new_pwd_conf;
    }

    public void setNew_pwd_conf(String new_pwd_conf) {
        this.new_pwd_conf = new_pwd_conf;
    }
}
