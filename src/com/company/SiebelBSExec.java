package com.company;

import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;
import com.siebel.data.SiebelPropertySet;
import com.siebel.data.SiebelService;

import java.util.Map;

public class SiebelBSExec implements Runnable  {

    private String ip;
    private String port;
    private String enterprise;
    private String objmgr;
    private String userName;
    private String password;
    private String locale;
    private String bs;
    private Map<String, String> inputs;
    private States st = null;

    public SiebelBSExec(String ip, String port, String enterprise, String objmgr, String userName, String password, String locale, String bs, States st) {
        this.ip = ip;
        this.port = port;
        this.enterprise = enterprise;
        this.objmgr = objmgr;
        this.userName = userName;
        this.password = password;
        this.locale = locale;
        this.bs = bs;
        this.st = st;
    }
    @Override
    public void run() {
        try {
            this.st.setBsFinished(false);
            SiebelDataBean sblConnect = new SiebelDataBean();
            sblConnect.login("Siebel://" + this.ip + ":" + this.port + "/" + this.enterprise + "/" + this.objmgr,
                    this.userName, this.password, this.locale);
            Thread.sleep(2000);
            System.out.println("Start BS...");
            SiebelService BS = sblConnect.getService(this.bs);
            SiebelPropertySet Inputs1 = sblConnect.newPropertySet();
            for(Map.Entry<String, String> it: this.inputs.entrySet())
                Inputs1.setProperty(it.getKey(), it.getValue());
            SiebelPropertySet Outputs1 = sblConnect.newPropertySet();
            BS.invokeMethod("resetVariables", Inputs1, Outputs1);

            sblConnect.logoff();
            this.st.setBsFinished(true);
            System.out.println("BS finished!");
        } catch (InterruptedException | SiebelException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }
}
