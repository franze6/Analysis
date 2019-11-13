package com.company;


import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        File config = new File("config\\config.json");
        float freq =0.1f;

        States st  = new States();
        Analyser analyser = new Analyser(st, freq);
        if(config.exists()) {
            analyser.setConfig(config.getAbsolutePath());
        }
        Scanner in = new Scanner(System.in);
        try {
            while (true) {
                if (!st.isBsFinished()) {
                    Thread.sleep(1000);
                    continue;
                }
                if(st.isAnalyseFinished())
                    System.out.print('>');
                else
                    System.out.print('#');
                String command = in.nextLine();
                if (command.equals("getpids")) {
                    for (String str : analyser.findPidsForComp())
                        System.out.println(str);
                } else if (command.contains("setfreq")) {
                    String[] arg = command.split(" ");
                    if(arg.length > 1) {
                        freq = Float.parseFloat(arg[1]);
                        analyser.setFreq(freq);
                    }
                    else System.out.println("Не верный синтаксис");
                }else if (command.contains("startBS")) {
                    String[] arg = command.split(" ");
                    if(arg.length > 1)
                        analyser.startBS(arg[1]);
                    else System.out.println("Не верный синтаксис");
                }else if (command.contains("startUI")) {
                    if (st.isAnalyseFinished()) {
                        String[] arg = command.split(" ");
                        if (arg.length > 1)
                            analyser.start(arg[1], false);
                        else System.out.println("Не верный синтаксис");
                    } else {
                        System.out.println("Старый процесс еще не завершен, подожди немного");
                    }
                }else if (command.contains("start")) {
                    if(st.isAnalyseFinished()) {
                        String[] arg = command.split(" ");
                        if(arg.length > 1)
                            analyser.start(arg[1], true);
                        else System.out.println("Не верный синтаксис");
                    }
                    else {
                        System.out.println("Старый процесс еще не завершен, подожди немного");
                    }
                } else if (command.contains("kill")) {
                    String[] arg = command.split(" ");
                    analyser.killSission(arg[1]);
                } else if (command.contains("get")) {
                    String[] arg = command.split(" ");
                    if(arg.length > 1) {
                        if(arg[1].equals("all")) {
                            for (String str : analyser.findPidsForComp()) {
                                if (!analyser.getJobj().containsKey(str)) {
                                    System.out.println("Нет такой инфы");
                                } else {
                                    openChart(str, analyser);
                                }
                            }
                        }
                        else {
                            if (!analyser.getJobj().containsKey(arg[1])) {
                                System.out.println("Нет такой инфы");
                            } else {
                                openChart(arg[1], analyser);
                            }
                        }
                    }
                }
                else if (command.equals("exit")) System.exit(0);
                else if (command.equals("help")) {
                    System.out.println(
                            "\tgetpids - получает пиды процессов\n" +
                            "\tkill <pid> - Убивает процесс <pid>\n" +
                            "\tstart <inerations> - Начинает процесс анализа с <interations> измерений\n" +
                            "\tget <pid> - Получает график по <pid>(Вызывать после start)\n" +
                            "\texit - Выход\n" +
                            "\tstartBS - запуск бизнес-сервиса\n"

                    );
                }
                //else if (command.equals("stop")) analyser.stopAnalyse();
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //Analyser analyser = new Analyser();
        //analyser.start();
    }

    public void setConfig(File file) {

    }

    public void createDefaultConfig(File file) {

    }

    public static void openChart(String str, Analyser analyser) {
        try {
            File file = File.createTempFile("tmp", ".html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            String pattern = "<html class=\"wf-roboto-n4-active wf-roboto-n5-active wf-active\"><head>\n" +
                    "  <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
                    "  <script type=\"text/javascript\" charset=\"utf-8\">\n" +
                    "          google.charts.load('current', {\n" +
                    "        'packages': ['corechart']\n" +
                    "      });\n" +
                    "      google.charts.setOnLoadCallback(drawChart);\n" +
                    "\n" +
                    "      function drawChart() {\n" +
                    "\n" +
                    "        var jsn = '"+ analyser.getJobj().get(str).toString() +"';\n" +
                    "      var obj = JSON.parse(jsn);\n" +
                    "\n" +
                    "      var data = new google.visualization.DataTable();\n" +
                    "      data.addColumn('number', \"Выборка\");\n" +
                    "      data.addColumn('number', 'Память после запуска');\n" +
                    "\n" +
                    "      var dataSet = [];\n" +
                    "        \n" +
                    "\n" +
                    "        for( i = 0; i< obj.memory_before.length; i ++)\n" +
                    "        {\n" +
                    "          dataSet[i] = [i, parseFloat(obj.memory_before[i])];\n" +
                    "        }\n" +
                    "        data.addRows(dataSet);\n" +
                    "\n" +
                    "        var options = {\n" +
                    "          title: \"Процесс\",\n" +
                    "          subtitle: obj.pid,\n" +
                    "          hAxis: {\n" +
                    "            title: 'Выборка',\n" +
                    "            titleTextStyle: {\n" +
                    "              color: '#333'\n" +
                    "            },\n" +
                    "            slantedText: true,\n" +
                    "            slantedTextAngle: 180\n" +
                    "          },\n" +
                    "          explorer: {\n" +
                    "            axis: 'horizontal',\n" +
                    "            keepInBounds: true,\n" +
                    "            maxZoomIn: 160.0\n" +
                    "          },\n" +
                    "        };\n" +
                    "\n" +
                    "        var chart = new google.visualization.LineChart(document.getElementById('chart_div'));\n" +
                    "        chart.draw(data, options);\n" +
                    "      }\n" +
                    "  </script>\n" +
                    "<body>\n" +
                    "<div id=\"chart_div\" style=\"width: 1000px; height: 800px;\"></div>\n" +
                    "</body>\n" +
                    "</html>";
            writer.write(pattern);
            writer.close();
            Desktop.getDesktop().open(file.getAbsoluteFile());
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
