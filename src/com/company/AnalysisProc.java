package com.company;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class AnalysisProc extends Thread {

    private SshConnectionData connectionData;
    private String pid;
    private JSONObject jobj;
    private boolean run = false;
    private float freq;
    private String iterations;
    private StatPid stat;

    public AnalysisProc(SshConnectionData connectionData, String pid, JSONObject jobj, float freq, String iterations) {
        this.connectionData = connectionData;
        this.pid = pid;
        this.jobj = jobj;
        this.freq = freq;
        this.iterations = iterations;
    }



    @Override
    public void run() {
        this.run = true;
        SSHManager instance = new SSHManager(this.connectionData.getUserName(), this.connectionData.getPassword(), this.connectionData.getIp(), "");
        String errorMessage = instance.connect();

        if(errorMessage != null)
        {
            System.err.println(errorMessage);
            return;
        }
        this.stat = new StatPid(this.pid);
        ArrayList<StatPid.State> st;
        if(this.iterations.equals("0")) st = stat.start(this.freq, instance);
        else st = stat.start(this.freq, this.iterations, instance);

        JSONArray cpu = new JSONArray();
        JSONArray memory = new JSONArray();

        for(StatPid.State s: st) {
            cpu.add(s.cpu);
            memory.add(s.memory);

        }

        this.jobj.put("cpu_before", cpu);

        this.jobj.put("memory_before", memory);
        this.jobj.put("pid", this.pid);
        this.run = false;

        instance.close();
    }

    public void stopT() {
        this.stat.stop();
    }

    public boolean isRun() {
        return run;
    }
}
