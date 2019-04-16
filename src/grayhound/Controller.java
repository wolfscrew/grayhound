package grayhound;

import java.io.*;
import java.util.*;
import java.net.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FileList {
    static String[] getList(String filename) {

        Scanner sc = null;
        URL url = FileList.class.getResource(filename);
        File location = new File(url.getPath());

        try {
            sc = new Scanner(location);
        } catch (FileNotFoundException e) {
            System.out.println("Error opening file");
        }

        ArrayList<String> list = new ArrayList<>();
        String s;
        if (sc != null) {
            while (sc.hasNextLine()) {
                s = sc.nextLine();
                list.add(s);
            }
        }

        String[] subs;
        subs = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            subs[i] = list.get(i);
        }

        return subs;
    }
}

public class Controller {

    @FXML
    private Button scanButton;
    @FXML
    private TextField targetText;
    @FXML
    private TextArea outputText;
    @FXML
    private Label statusText;

    @FXML
    private void initialize() {

        scanButton.setText("Start");
        scanButton.setStyle("-fx-background-color: #457ecd; -fx-text-fill: #ffffff;");

        // Handle Button event.
        scanButton.setOnAction((event) -> {
            if (targetText.getText().length() > 0) {

                statusText.setText("Scanning");

                outputText.appendText("Scan domain " + targetText.getText() + "\n");

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Runnable runnableTask = () -> bruteDomain(targetText.getText());

                executor.submit(runnableTask);

                statusText.setText("Done");
            }
        });
    }

    private static String[] DNSLookup(String hostname) throws UnknownHostException {
        String[] output;

        InetAddress[] addresses = InetAddress.getAllByName(hostname);
        output = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            output[i] = addresses[i].toString();
        }
        return output;
    }

    private int THREAD_COUNT = 18;

    private int bruteDomain(String target) {

        // get the list of sub-domains
        String[] subs = FileList.getList("subs-domain.txt");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (String sub : subs) {
            final String domain = sub + "." + target;

            Callable<String> callableTask = () -> {
                String[] result;

                try {
                    result = DNSLookup(domain);
                } catch (UnknownHostException he) {
                    result = new String[0];
                }
                return result[0];
            };

            Future<String> future = executor.submit(callableTask);

            String result;

            try {
                result = future.get();
                outputText.appendText("[+] found " + result + "\n");

            } catch (InterruptedException | ExecutionException e) {
                //e.printStackTrace();
            }
        }

        executor.shutdown();

        while (true) {
            if (executor.isTerminated()) break;
        }
        return 0;
    }

}

