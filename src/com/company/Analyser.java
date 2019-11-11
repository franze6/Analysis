package com.company;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private static String P_COUNT = "100"; // Время анализа: P_COUNT*0,1

    private JSONObject jobj = new JSONObject();
    private ArrayList<String> pids = null;

    private Map<String, Boolean> runs = new HashMap<>();
    private States st;

    public Analyser(States st) {
        this.st = st;
    }

    public JSONObject getJobj() {
        return jobj;
    }

    public void start(String iterations) {

        this.st.setAnalyseFinished(false);
        if(!iterations.isEmpty())
            P_COUNT = iterations;
        if(this.pids == null)
            this.pids = findPidsForComp();
        for(String str: this.pids) {
            this.jobj.put(str, new JSONObject());
            this.runs.put(str, true);
        }
        try {
            startAnalyse();
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    public void killSission(String pid) {
        SSHManager instance = new SSHManager(SSH_USER_NAME, SSH_PASSWORD, SSH_IP, "");
        String errorMessage = instance.connect();
        if(errorMessage != null)
        {
            System.err.println(errorMessage);
            return;
        }
        ArrayList<String> res = new ArrayList<>();
        String command = "kill "+pid+"\nexit\n";
        instance.sendCommand(command);
        instance.close();
    }
    private void startAnalyse() {

        for(String str: this.pids) {
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
                this.jobj.getJSONObject(str).put("cpu_before", cpu);
                this.jobj.getJSONObject(str).put("memory_before", memory);
                this.runs.put(str, false);
                if(this.allFinished()) {
                    this.st.setAnalyseFinished(true);
                    System.out.print("\nПроцесс завершен\nНажмите Enter");
                }

                instance.close();
            });
            t.start();
        }
        startBS();
    }

    public ArrayList<String> findPidsForComp() {
        SSHManager instance = new SSHManager(SSH_USER_NAME, SSH_PASSWORD, SSH_IP, "");
        String errorMessage = instance.connect();
        if(errorMessage != null)
        {
            System.err.println(errorMessage);
            return null;
        }
        ArrayList<String> res = new ArrayList<>();
        String command = ". /u01/app/Siebel/siebsrvr/siebenv.sh\nsrvrmgr /g "+S_IP+" /e "+S_ENTERPRISE+" /u "+S_USER_NAME+" /p "+S_PASSWORD+" /s "+S_SERVER+" /c 'list procs for comp "+S_OBJMGR+" show CC_ALIAS, TK_PID' | grep "+S_OBJMGR+" | awk '{print $2}'\nexit\n";


        String result = instance.sendCommand(command);
        instance.close();

        for(String str: result.replaceAll("\r+","").split("\n"))
            if(isInteger(str))  res.add(str);
        this.pids = res;
        return res;
    }

    public void startBS() {
        SiebelBSExec bs = new SiebelBSExec(S_IP, S_PORT, S_ENTERPRISE, S_OBJMGR, S_USER_NAME, S_PASSWORD, S_LOCALE, S_BS, this.st);
        Map<String, String> input = new HashMap<>();
        input.put("iterations", "1000");
        input.put("count", "16");
        bs.setInputs(input);
        Thread bsT = new Thread(bs);
        bsT.start();
    }

    private boolean allFinished() {
        for(Map.Entry<String, Boolean> it: this.runs.entrySet()) {
            if (it.getValue()) return false;
        }
        return true;

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
