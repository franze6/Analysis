package com.company;

import org.json.JSONException;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        States st  = new States();

        Analyser analyser = new Analyser(st);
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
                } else if (command.equals("startBS")) {
                    analyser.startBS();
                }else if (command.contains("start")) {
                    if(st.isAnalyseFinished()) {
                        String[] arg = command.split(" ");
                        if(arg.length > 1)
                            analyser.start(arg[1]);
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
                                if (!analyser.getJobj().has(str)) {
                                    System.out.println("Нет такой инфы");
                                } else {
                                    openChart(str, analyser);
                                }
                            }
                        }
                        else {
                            if (!analyser.getJobj().has(arg[1])) {
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
                Thread.sleep(100);
            }
        } catch (InterruptedException | JSONException e) {
            e.printStackTrace();
        }


        //Analyser analyser = new Analyser();
        //analyser.start();
    }

    public static void openChart(String str, Analyser analyser) {
        try {
            File file = File.createTempFile("tmp", ".html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            String pattern = "<html>\n" +
                    "<head>\n" +
                    "  <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
                    "    <script type=\"text/javascript\">\n" +
                    "      google.charts.load('current', {'packages':['line']});\n" +
                    "      google.charts.setOnLoadCallback(drawChart);\n" +
                    "\n" +
                    "    function drawChart() {\n" +
                    "\n" +
                    "        var jsn = '" + analyser.getJobj().getJSONObject(str).toString() +
                    "';var obj = JSON.parse(jsn);\n" +
                    "\n" +
                    "      var data = new google.visualization.DataTable();\n" +
                    "      data.addColumn('number', \"Выборка\");\n" +
                    "      data.addColumn('number', 'Память после запуска');\n" +
                    "\n" +
                    "      var dataSet = [];\n" +
                    "\n" +
                    "      for( i = 0; i< obj.memory_before.length; i ++)\n" +
                    "      {\n" +
                    "        dataSet[i] = [i, parseFloat(obj.memory_before[i])];\n" +
                    "      }\n" +
                    "\n" +
                    "\n" +
                    "      data.addRows(dataSet);\n" +
                    "\n" +
                    "      var options = {\n" +
                    "        chart: {\n" +
                    "          title: \"Процесс\",\n" +
                    "          subtitle: obj.pid\n" +
                    "        },\n" +
                    "        width: 1000,\n" +
                    "        height: 500,\n" +
                    "        axes: {\n" +
                    "          x: {\n" +
                    "            0: {side: 'top'}\n" +
                    "          }\n" +
                    "        }\n" +
                    "      };\n" +
                    "\n" +
                    "      var chart = new google.charts.Line(document.getElementById('line_top_x'));\n" +
                    "\n" +
                    "      chart.draw(data, google.charts.Line.convertOptions(options));\n" +
                    "    }\n" +
                    "  </script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <div id=\"line_top_x\"></div>\n" +
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
