package com.company;

import java.util.*;

public class StatPid {

    SSHManager instance;

    public String pid = "";

    public StatPid(String pid) {
        this.pid = pid;
    }

    //count - Кол-во выборок состояния процесса
    //delay - Задержка между каждой следующей выборкой

    public ArrayList<State> start(float delay, String iterations, SSHManager instance) {
        this.instance = instance;
        ArrayList<State> result = new ArrayList<>();
        String cmd = "top -p " + this.pid + " -d "+delay+" -b -n "+iterations+" | grep " + this.pid + " |  sed -r -e \"s;\\s\\s*; ;g\" -e \"s;^ *;;\"  |cut -d ' '  -f '6 9' | tr ' ' -\nexit\n";
        this.instance.sendCommand(cmd);
        String res = this.instance.getOutBuff().toString();

        String[] arr = res.replaceAll("\r+","").split("\n");
        for(int i = 4; i < arr.length-2; i++)
        {
            try {
                String tmp = arr[i].split("-")[0];
                if(tmp.contains("m")) tmp = tmp.replaceAll("m", "");
                else if(tmp.contains("g")) tmp = String.valueOf(Double.parseDouble(tmp.replaceAll("g", "")) * 1024);
                result.add(new State(arr[i].split("-")[1], tmp, this.pid));
            }catch (ArrayIndexOutOfBoundsException e){}
        }

        return result;

    }

    public ArrayList<State> start(float delay, SSHManager instance) {
        this.instance = instance;
        ArrayList<State> result = new ArrayList<>();
        String cmd = "top -p " + this.pid + " -d "+delay+" -b | grep " + this.pid + " |  sed -r -e \"s;\\s\\s*; ;g\" -e \"s;^ *;;\"  |cut -d ' '  -f '6 9' | tr ' ' -\n";

        this.instance.sendCommand(cmd);
        String res = this.instance.getOutBuff().toString();

                String[] arr = res.replaceAll("\r+","").split("\n");
        for(int i = 4; i < arr.length-2; i++)
        {
            try {
                String tmp = arr[i].split("-")[0];
                if(tmp.contains("m")) tmp = tmp.replaceAll("m", "");
                else if(tmp.contains("g")) tmp = String.valueOf(Double.parseDouble(tmp.replaceAll("g", "")) * 1024);
                result.add(new State(arr[i].split("-")[1], tmp, this.pid));
            }catch (ArrayIndexOutOfBoundsException e){}
        }

        return result;

    }

    public void stop() {
        this.instance.doStop();
    }

    class State {
        String cpu = "";
        String memory = "";
        String pid = "";

        public State(String cpu, String memory, String pid) {
            this.pid = pid;
            this.cpu = cpu;
            this.memory = memory;
        }
    }
}