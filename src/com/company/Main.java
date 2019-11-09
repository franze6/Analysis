package com.company;

import org.json.JSONException;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Analyser analyser = new Analyser();
        Scanner in = new Scanner(System.in);
        try {


            while (true) {
                if (analyser.wait) {
                    Thread.sleep(1000);
                    continue;
                }

                System.out.print('>');
                String command = in.nextLine();
                if (command.equals("getpids")) {
                    for (String str : analyser.findPidsForComp())
                        System.out.println(str);
                } else if (command.contains("start")) {
                    String[] arg = command.split(" ");
                    analyser.start(arg[1]);
                } else if (command.contains("kill")) {
                    String[] arg = command.split(" ");
                    analyser.killSission(arg[1]);
                } else if (command.contains("get")) {
                    String[] arg = command.split(" ");
                    if (!analyser.getJobj().has(arg[1])) {
                        System.out.println("Нет такой инфы");
                    } else {
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
                                "        var jsn = '" + analyser.getJobj().getJSONObject(arg[1]).toString() +
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
                    }
                } else if (command.equals("exit")) System.exit(0);
            }
        } catch (InterruptedException | IOException | JSONException e) {
            e.printStackTrace();
        }


        //Analyser analyser = new Analyser();
        //analyser.start();
    }
}
