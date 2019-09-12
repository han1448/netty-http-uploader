package com.oppa.woodpecker;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

public class HttpSender {

    private static String CHARSET = "UTF-8";
    private static String CRLF = "\r\n";
    private String host = "http://localhost";
    private String port = "8080";
    private String boundary = Long.toHexString(System.currentTimeMillis());
    private File file;

    public HttpSender(String file) {
        this.file = new File(file);
    }

    public HttpSender(String host, String port, String file) {
        this.host = host;
        this.port = port;
        this.file = new File(file);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Please input host name and port and file path. ex) http://localhost 8080 /usr/local/file.txt");
            return;
        }
        HttpSender httpSender = new HttpSender(args[0], args[1], args[2]);
        httpSender.send();
    }

    public void send() throws Exception {
        URLConnection connection = getConnection(host, port);
        OutputStream output = connection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, CHARSET), true);

        writeContentAndFlush(writer, output);
        writeEndContentAndFlush(writer);
        output.flush();
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        System.out.println("response code: " + responseCode);
    }

    private void writeEndContentAndFlush(PrintWriter writer) {
        writer.append(CRLF).flush();
        writer.append("--" + boundary + "--").append(CRLF).flush();
    }

    private void writeContentAndFlush(PrintWriter writer, OutputStream output) throws Exception {
        writer.append("--" + boundary).append(CRLF);
        writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + file.getName() + "\"").append(CRLF);
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(CRLF);
        writer.append("Content-Transfer-Encoding: binary").append(CRLF);
        writer.append(CRLF).flush();
        Files.copy(file.toPath(), output);
    }

    private URLConnection getConnection(String host, String port) throws IOException {
        String url = host.concat(":").concat(port);
        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        return connection;
    }


}
