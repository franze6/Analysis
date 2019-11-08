package com.company;

import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;
import com.siebel.data.SiebelPropertySet;
import com.siebel.data.SiebelService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class Analyser {
    private static final String SSH_USER_NAME="siebel";
    private static final String SSH_PASSWORD="siebel";
    private static final String SSH_IP="bcvm496";

    private static final String S_USER_NAME="SADMIN";
    private static final String S_PASSWORD="SADMIN";
    private static final String S_IP="bcvm496";
    private static final String S_PORT="2321";
    private static final String S_OBJMGR="FINSObjMgr_rus";
    private static final String S_ENTERPRISE="SBA_81";
    private static final String S_LOCALE="rus";
    private static final String S_SERVER="bcvm496";
    private static final String S_BS="TSC Dev Nightmare";

    private static final int P_COUNT = 100; // Время анализа: P_COUNT*0,1

    private JSONObject jobj = new JSONObject();

    public void start() {
        SSHManager instance = new SSHManager(SSH_USER_NAME, SSH_PASSWORD, SSH_IP, "");
        String errorMessage = instance.connect();

        if(errorMessage != null)
        {
            System.err.println(errorMessage);
            return;
        }

        ArrayList<String> res = findPidsForComp(S_OBJMGR, instance);
        for(String str: res) this.jobj.put(str, new JSONObject());
        try {
            startAnalyse(res, false);
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }

        instance.close();

    }

    private void startAnalyse(ArrayList<String> res, boolean withBS) throws InterruptedException {

        for(String str: res) {
            Thread t = new Thread(() -> {
                SSHManager instance = new SSHManager(SSH_USER_NAME, SSH_PASSWORD, SSH_IP, "");
                String errorMessage = instance.connect();

                if(errorMessage != null)
                {
                    System.err.println(errorMessage);
                    return;
                }
                StatPid stat = new StatPid(str);
                ArrayList<StatPid.State> st = stat.start(0.1f, P_COUNT, instance);

                JSONArray cpu = new JSONArray();
                JSONArray memory = new JSONArray();



                for(StatPid.State s: st) {
                    cpu.put(s.cpu);
                    memory.put(s.memory);

                }

                if(withBS) {
                    this.jobj.getJSONObject(str).put("cpu_after", cpu);
                    this.jobj.getJSONObject(str).put("memory_after", memory);
                }
                else {
                    this.jobj.getJSONObject(str).put("cpu_before", cpu);
                    this.jobj.getJSONObject(str).put("memory_before", memory);
                }
                instance.close();
                if(withBS) {
                    System.out.println(this.jobj.getJSONObject(str).toString());
                    return;
                }
                try {
                    startAnalyse(res, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            t.start();
            if(!withBS) return;


            Thread thread = new Thread(() -> {
                try {
                    SiebelDataBean sblConnect = new SiebelDataBean();
                    sblConnect.login("Siebel://"+S_IP+":"+S_PORT+"/"+S_ENTERPRISE+"/"+S_OBJMGR, S_USER_NAME, S_PASSWORD, S_LOCALE);

                    SiebelService BS = sblConnect.getService(S_BS);
                    SiebelPropertySet Inputs1 = sblConnect.newPropertySet();
                    Inputs1.setProperty("iterations", "1000");
                    Inputs1.setProperty("count", "16");
                    SiebelPropertySet Outputs1 = sblConnect.newPropertySet();
                    BS.invokeMethod("resetVariables", Inputs1, Outputs1);
                    // println 'Result: ' + Outputs1.getProperty("result");

                    sblConnect.logoff();

                }
                catch (SiebelException e){}
            });
            thread.start();


        }
    }

    private ArrayList<String> findPidsForComp(String comp, SSHManager instance) {
        ArrayList<String> res = new ArrayList<>();
        String command = ". /u01/app/Siebel/siebsrvr/siebenv.sh\nsrvrmgr /g "+S_IP+" /e "+S_ENTERPRISE+" /u "+S_USER_NAME+" /p "+S_PASSWORD+" /s "+S_SERVER+" /c 'list procs for comp "+comp+" show CC_ALIAS, TK_PID' | grep "+S_OBJMGR+" | awk '{print $2}'\nexit\n";


        String result = instance.sendCommand(command);
        for(String str: result.replaceAll("\r+","").split("\n"))
            if(isInteger(str))  res.add(str);

        return res;
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }
}