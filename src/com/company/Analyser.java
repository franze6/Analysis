package com.company;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.util.*;

public class Analyser {
    private static String P_COUNT = "100"; // Время анализа: P_COUNT*0,1

    private JSONObject jobj = new JSONObject();
    private ArrayList<String> pids = null;

    private Map<String, Boolean> runs = new HashMap<>();
    private States st;
    private SiebelConnectionData siebelConnectionData;
    private SshConnectionData sshConnectionData;
    private SiebelBSExec.BS bs;
    private float freq;

    ArrayList<AnalysisProc> threads = new ArrayList<>();

    public Analyser(States st, float freq) {
        this.st = st;
        this.freq = freq;
    }

    public JSONObject getJobj() {
        return jobj;
    }

    public void start(String iterations, boolean withBS) {

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
            startAnalyse(withBS);
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    public void killSission(String pid) {
        SSHManager instance = new SSHManager(this.sshConnectionData.getUserName(),
                this.sshConnectionData.getPassword(), this.sshConnectionData.getIp(), "");
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
    private void startAnalyse(boolean withBS) {

        for(String str: this.pids) {
            AnalysisProc t = new AnalysisProc(this.sshConnectionData, str, (JSONObject)this.jobj.get(str), this.freq, P_COUNT);
            this.threads.add(t);
            t.start();
        }
        if(withBS) startBS((String) this.bs.getMethods().keySet().toArray()[0]);
        new Thread(()-> {
           while(!this.st.isAnalyseFinished()) {
               if(allFinished()) {
                   this.st.setAnalyseFinished(true);
                   System.out.println("Процесс завершен\nНажмите Enter...");
               }
           }
        }).start();
    }

    public void stopAnalyse() {
        for(AnalysisProc t:this.threads)
            t.stopT();
    }



    public ArrayList<String> findPidsForComp() {
        SSHManager instance = new SSHManager(this.sshConnectionData.getUserName(),
                this.sshConnectionData.getPassword(), this.sshConnectionData.getIp(), "");
        String errorMessage = instance.connect();
        if(errorMessage != null)
        {
            System.err.println(errorMessage);
            return null;
        }
        ArrayList<String> res = new ArrayList<>();
        String command = ". /u01/app/Siebel/siebsrvr/siebenv.sh\nsrvrmgr /g "+this.siebelConnectionData.getIp()+
                " /e "+this.siebelConnectionData.getEnterprise()+" /u "+this.siebelConnectionData.getUserName()+
                " /p "+this.siebelConnectionData.getPassword()+" /s "+this.siebelConnectionData.getServer()+
                " /c 'list procs for comp "+this.siebelConnectionData.getObjmgr()+" show CC_ALIAS, TK_PID' | grep "+
                this.siebelConnectionData.getObjmgr()+" | awk '{print $2}'\nexit\n";

        instance.sendCommand(command);
        String result = instance.getOutBuff().toString();
        instance.close();

        for(String str: result.replaceAll("\r+","").split("\n"))
            if(isInteger(str))  res.add(str);
        this.pids = res;
        return res;
    }

    public void startBS(String method) {
        if(!this.bs.getMethods().containsKey(method)) {
            System.out.println("Метод не существует!");
            return;
        }
        SiebelBSExec bsExec = new SiebelBSExec(this.siebelConnectionData, this.bs, method,this.st);
        Thread bsT = new Thread(bsExec);
        bsT.start();
    }

    private boolean allFinished() {
        try {
            for (AnalysisProc it : this.threads) {
                Thread.sleep(10);
                if (it.isRun()) return false;
            }
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void setConfig(String file) {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(file))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONObject configList = (JSONObject) obj;
            JSONObject connection = (JSONObject) configList.get("connection");
            JSONObject sshConnection = (JSONObject) connection.get("ssh");
            JSONObject siebelConnection = (JSONObject) connection.get("siebel");
            this.siebelConnectionData = new SiebelConnectionData(siebelConnection.get("ip").toString(),
                    siebelConnection.get("port").toString(),
                    siebelConnection.get("enterprise").toString(),
                    siebelConnection.get("objmgr").toString(),
                    siebelConnection.get("user").toString(),
                    siebelConnection.get("server").toString(),
                    siebelConnection.get("password").toString(),
                    siebelConnection.get("locale").toString());
            this.sshConnectionData = new SshConnectionData(sshConnection.get("ip").toString(),
                    sshConnection.get("port").toString(),
                    sshConnection.get("user").toString(),
                    sshConnection.get("password").toString());

            JSONObject bsConfig = (JSONObject) configList.get("BS");
            JSONArray mList = (JSONArray) bsConfig.get("methods");
            Map<String, Map<String, String>> methods = new HashMap<>();
            mList.forEach( method -> {
                JSONObject mObj = (JSONObject) bsConfig.get(method.toString());
                JSONObject inputs = (JSONObject) mObj.get("inputs");

                Map<String, String> inputsMap = new HashMap<>();
                for (Object it:inputs.keySet()) {
                    inputsMap.put(it.toString(), inputs.get(it).toString());
                }
                methods.put(method.toString(), inputsMap);
            });
            this.bs = new SiebelBSExec.BS(bsConfig.get("name").toString(), methods);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void setBSConfig(String str) {
        try {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(str);
            JSONObject bsConfig = (JSONObject) obj;
            Map<String, Map<String, String>> methods = new HashMap<>();
            for(Object it:bsConfig.keySet()) {
                if(it.toString().equals("name")) continue;
                JSONObject method = (JSONObject)bsConfig.get(it);
                JSONObject inputs = (JSONObject) method.get("inputs");
                Map<String, String> inputsMap = new HashMap<>();
                for (Object it2 : inputs.keySet()) {
                    inputsMap.put(it2.toString(), inputs.get(it2).toString());
                }
                methods.put(it.toString(), inputsMap);
            }
            this.bs = new SiebelBSExec.BS(bsConfig.get("name").toString(), methods);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setFreq(float freq) {
        this.freq = freq;
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
